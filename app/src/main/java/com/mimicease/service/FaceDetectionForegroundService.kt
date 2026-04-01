package com.mimicease.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.service.quicksettings.TileService
import android.hardware.camera2.CaptureRequest
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.mimicease.MainActivity
import com.mimicease.domain.model.Action
import com.mimicease.domain.model.InteractionMode
import com.mimicease.domain.model.ModeManager
import com.mimicease.domain.model.ServiceState
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.mimicease.gameface.FaceLandmarkerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class FaceDetectionForegroundService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mimic_ease_service_channel"

        const val ACTION_START = "com.mimicease.ACTION_START"
        const val ACTION_PAUSE = "com.mimicease.ACTION_PAUSE"
        const val ACTION_RESUME = "com.mimicease.ACTION_RESUME"
        const val ACTION_STOP = "com.mimicease.ACTION_STOP"
        private const val EXTRA_TARGET_STATE = "extra_target_state"

        // SharedFlow exposed for UI screens to observe real-time blendshapes
        // extraBufferCapacity: UI 구독자가 느릴 때 emit이 블록되지 않도록, DROP_OLDEST로 최신값 유지
        private val _blendShapeFlow = MutableSharedFlow<Map<String, Float>>(
            replay = 1, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val blendShapeFlow: SharedFlow<Map<String, Float>> = _blendShapeFlow.asSharedFlow()

        // SharedFlow for face mesh landmark coordinates (for FaceMeshOverlay)
        private val _faceLandmarksFlow = MutableSharedFlow<List<NormalizedLandmark>>(
            replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val faceLandmarksFlow: SharedFlow<List<NormalizedLandmark>> = _faceLandmarksFlow.asSharedFlow()

        // MediaPipe 처리 이미지 크기 — FaceMeshOverlay 좌표 보정에 사용
        private val _imageSizeFlow = MutableStateFlow(Pair(0, 0))
        val imageSizeFlow: StateFlow<Pair<Int, Int>> = _imageSizeFlow.asStateFlow()

        private val _inferenceTimeMs = MutableStateFlow(0L)
        val inferenceTimeMs: StateFlow<Long> = _inferenceTimeMs.asStateFlow()

        private val _isFaceVisible = MutableStateFlow(false)
        val isFaceVisible: StateFlow<Boolean> = _isFaceVisible.asStateFlow()

        private val _serviceState = MutableStateFlow(ServiceState.Stopped)
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        // Service instance for direct Preview SurfaceProvider attachment (test screen only)
        private var _instance: FaceDetectionForegroundService? = null

        /**
         * 서비스 실행 상태.
         * - true: 서비스가 실행 중 (일시정지 여부와 무관)
         * - false: 서비스가 완전히 정지됨
         */

        fun attachPreviewSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
            val svc = _instance ?: return
            svc.activeSurfaceProvider = surfaceProvider
            svc.previewUseCase.setSurfaceProvider(surfaceProvider)
            // cameraProvider가 준비된 경우 Preview UseCase를 동적으로 추가 바인딩
            val provider = svc.cameraProvider ?: return
            if (!provider.isBound(svc.previewUseCase)) {
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                try {
                    provider.bindToLifecycle(svc, cameraSelector, svc.previewUseCase)
                } catch (e: Exception) {
                    Timber.w(e, "Preview UseCase 바인딩 실패")
                }
            }
        }

        fun detachPreviewSurfaceProvider() {
            val svc = _instance ?: return
            svc.activeSurfaceProvider = null
            svc.previewUseCase.setSurfaceProvider(null)
            svc.cameraProvider?.unbind(svc.previewUseCase)
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "MimicEase 감지 서비스",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "표정 감지 중일 때 표시됩니다"
                    setShowBadge(false)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }

        fun createStartIntent(context: Context, targetState: ServiceState): Intent {
            return Intent(context, FaceDetectionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_STATE, targetState.name)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@FaceDetectionForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private lateinit var expressionAnalyzer: ExpressionAnalyzer
    private var triggerMatcher: TriggerMatcher? = null
    private lateinit var actionExecutor: ActionExecutor

    // Phase 3: Head Tracking components
    private lateinit var headTracker: HeadTracker
    private lateinit var cursorOverlayView: CursorOverlayView
    private lateinit var dwellClickController: DwellClickController

    private var isAnalyzing = false
    // 빅스비 활성화 중 카메라를 일시 정지했는지 추적 — 사용자가 수동 정지한 것과 구별
    private var cameraPausedForBixby = false
    // 빅스비 활성화 중 트리거 액션 억제 플래그 — processResults()는 HandlerThread에서 호출되므로 @Volatile
    @Volatile private var isBixbyActive = false

    /**
     * 빅스비 활성화 시 카메라·MediaPipe를 일시 정지합니다.
     * updateRuntimeState()를 호출하지 않아 사용자가 보는 서비스 상태(알림/홈화면)는 그대로 유지됩니다.
     */
    private fun pauseCameraForBixby() {
        if (isAnalyzing && !cameraPausedForBixby) {
            cameraPausedForBixby = true
            isAnalyzing = false
            faceLandmarkerHelper.pauseThread()
            unbindCamera()
            Timber.i("빅스비용 카메라 일시 정지 — MediaPipe CPU 해제")
        }
    }

    /**
     * 빅스비 비활성화 시 카메라·MediaPipe를 재시작합니다.
     * pauseCameraForBixby()로 정지된 경우에만 재시작하며, 사용자가 수동 정지한 상태는 유지합니다.
     */
    private fun resumeCameraForBixby() {
        if (cameraPausedForBixby) {
            cameraPausedForBixby = false
            if (targetServiceState == ServiceState.Running) {
                faceLandmarkerHelper.resumeThread()
                isAnalyzing = true
                setupCamera()
                Timber.i("빅스비용 카메라 재시작")
            }
        }
    }

    private fun deactivateBixby() {
        isBixbyActive = false
        expressionAnalyzer.reset()
        triggerMatcher?.clearHoldTimers()
        resumeCameraForBixby()
        Timber.i("빅스비 비활성화 — 카메라 재시작, EMA/홀드타이머 초기화")
    }

    // 빅스비 비활성화 지연 해제 — 명령 실행 완료 대기 후 카메라 재시작
    private val bixbyDeactivateRunnable = Runnable { deactivateBixby() }

    private val bixbyResumeTimeoutRunnable = Runnable {
        if (isBixbyActive) {
            Timber.w("빅스비 활성화 타임아웃(30s) — 자동 해제")
            mainHandler.removeCallbacks(bixbyDeactivateRunnable)
            deactivateBixby()
        }
    }
    private var activeProfileName: String? = null
    private var currentMode: InteractionMode = InteractionMode.EXPRESSION_ONLY
    private var targetServiceState: ServiceState = ServiceState.Stopped
    // CURSOR_CLICK 모드에서 마우스 인터셉트 활성화 여부 (일시정지/재개 시 복원용)
    private var currentShouldIntercept = false
    private var globalToggleController: GlobalToggleController? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var dwellClickEnabled = true
    private lateinit var actionFeedbackController: ActionFeedbackController

    // Preview UseCase: 기본적으로 바인딩 안 됨 — 테스트 화면 진입 시에만 동적 바인딩
    private val previewUseCase: Preview = Preview.Builder().build()
    // 테스트 화면이 열려있는 동안 유지되는 SurfaceProvider (pause/resume 후 자동 재바인딩용)
    private var activeSurfaceProvider: Preview.SurfaceProvider? = null
    // 매 프레임 코루틴 생성 오버헤드 제거: UI 업데이트는 mainHandler로 직접 post
    private val mainHandler = Handler(Looper.getMainLooper())

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        _instance = this
        createNotificationChannel(this)

        // On Android 14+ (targetSdk >= 34), startForeground() with FOREGROUND_SERVICE_TYPE_CAMERA
        // throws SecurityException if the CAMERA runtime permission is not yet granted.
        // Check permission first and fall back to a basic foreground start if not granted.
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        // 카메라 권한 없이 CameraX를 시작하면 SecurityException 크래시 발생.
        // startForeground()는 5초 타임아웃 때문에 권한 여부와 무관하게 먼저 호출해야 하지만,
        // 권한이 없으면 서비스를 즉시 종료한다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasCameraPermission) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (!hasCameraPermission) {
            Timber.w("카메라 권한 없음 — FaceDetectionForegroundService 즉시 종료")
            MimicServiceStateStore.persistRuntimeStateBlocking(this, ServiceState.Stopped)
            stopSelf()
            return
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        expressionAnalyzer = ExpressionAnalyzer()

        // Initialize Phase 3 components
        headTracker = HeadTracker(this)
        cursorOverlayView = CursorOverlayView(this)
        actionFeedbackController = ActionFeedbackController(this)
        // actionExecutor is injected later via bind, so dwell controller is initialized in setActionExecutor

        initFaceLandmarker()
        observeSettingsAndProfile()
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val targetState = intent?.getStringExtra(EXTRA_TARGET_STATE)?.let(ServiceState::fromStorage)
            ?: targetServiceState
        when (intent?.action) {
            ACTION_START, null -> {
                applyTargetState(targetState)
            }
            ACTION_PAUSE -> {
                pauseAnalysis()
            }
            ACTION_RESUME -> {
                resumeAnalysis()
            }
            ACTION_STOP -> {
                stopServiceRuntime()
                // 카메라 해제 후 서비스 자체 종료 (BIND_AUTO_CREATE 바인딩이 해제되면 완전 종료)
                return START_NOT_STICKY
            }
            else -> updateNotification()
        }
        return if (_serviceState.value == ServiceState.Stopped) START_NOT_STICKY else START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun setAccessibilityService(service: MimicAccessibilityService) {
        actionExecutor = ActionExecutor(service)
        dwellClickController = DwellClickController(actionExecutor)
        // setMotionEventSources는 onServiceConnected()에서 항상 ON — 여기서 별도 호출 불필요
    }

    /** 진행 중인 제스처를 취소합니다. MimicAccessibilityService의 onInterrupt 등에서 호출됩니다. */
    fun cancelCurrentGesture() {
        if (::actionExecutor.isInitialized) actionExecutor.cancelCurrentGesture()
    }

    /**
     * 표정 트리거 제스처 진행 여부를 반환합니다.
     * MimicAccessibilityService.onMotionEvent()에서 BT 마우스 클릭 재주입 충돌 방지에 사용됩니다.
     */
    fun isGestureDispatching(): Boolean =
        if (::actionExecutor.isInitialized) actionExecutor.isGestureDispatching() else false

    fun setGlobalToggleController(controller: GlobalToggleController) {
        globalToggleController = controller
    }

    /**
     * 빅스비 활성화 상태를 설정합니다.
     * 활성화 시 표정 트리거 실행을 억제하여 빅스비가 의도치 않게 종료되는 문제를 방지합니다.
     * 빅스비 활성화 시 카메라와 MediaPipe를 완전히 정지하여 CPU/GPU 리소스 경합을 제거합니다.
     * @param active true → 빅스비 활성화(카메라 정지 + 트리거 억제), false → 빅스비 비활성화(카메라 재시작)
     */
    fun setBixbyActive(active: Boolean) {
        if (active) {
            // 빅스비 이벤트 수신: 항상 비활성화 타이머를 취소하여 억제 상태 유지
            mainHandler.removeCallbacks(bixbyDeactivateRunnable)
            if (!isBixbyActive) {
                // 비활성 → 활성 전환 시에만 플래그 설정 및 카메라 정지
                mainHandler.removeCallbacks(bixbyResumeTimeoutRunnable)
                isBixbyActive = true
                Timber.i("빅스비 활성화 — 카메라 일시 정지")
                pauseCameraForBixby()
                mainHandler.postDelayed(bixbyResumeTimeoutRunnable, 30_000L)
            }
        } else {
            // 비-빅스비 창 전환: 5초 후 비활성화 예약
            // - 5초로 설정한 이유: 빅스비 명령 실행(앱 열기 등) 중 중간 window event가
            //   발생해도 명령 완료 전에 트리거가 재활성화되지 않도록 충분한 여유를 확보
            // - 빅스비 이벤트가 5초 이내에 다시 오면 removeCallbacks로 타이머가 취소됨
            mainHandler.removeCallbacks(bixbyDeactivateRunnable)
            mainHandler.postDelayed(bixbyDeactivateRunnable, 5_000L)
        }
    }

    /** HEAD_MOUSE 커서를 화면 중앙으로 리셋합니다. */
    fun recenterCursor() {
        if (::headTracker.isInitialized) headTracker.recenter()
    }

    private fun initFaceLandmarker() {
        try {
            faceLandmarkerHelper = FaceLandmarkerHelper()
        } catch (t: Throwable) {
            Timber.e(t, "FaceLandmarkerHelper instantiation failed — native library missing?")
            stopSelf()
            return
        }
        faceLandmarkerHelper.start()  // Start HandlerThread
        faceLandmarkerHelper.setFaceResultListener { blendshapes, landmarks, transformMatrix, mediapipeMs, faceVisible ->
            _isFaceVisible.value = faceVisible
            _inferenceTimeMs.value = mediapipeMs
            // MediaPipe 처리 이미지 크기 업데이트 (랜드마크 오버레이 좌표 보정용)
            val iw = faceLandmarkerHelper.mpInputWidth
            val ih = faceLandmarkerHelper.mpInputHeight
            if (iw > 0 && ih > 0) {
                _imageSizeFlow.value = Pair(iw, ih)
            }
            if (faceVisible && blendshapes.isNotEmpty()) {
                // DROP_OLDEST 버퍼 설정으로 tryEmit은 항상 성공 — 코루틴 생성 불필요
                _blendShapeFlow.tryEmit(blendshapes)
                if (landmarks.isNotEmpty()) {
                    _faceLandmarksFlow.tryEmit(landmarks)
                }

                var yaw = 0f
                var pitch = 0f
                if (transformMatrix != null) {
                    yaw = Math.atan2(transformMatrix[2].toDouble(), transformMatrix[10].toDouble()).toFloat()
                    pitch = Math.asin(-transformMatrix[6].toDouble()).toFloat()
                }

                processResults(blendshapes, yaw, pitch)
            }
        }
        // Post init() to the HandlerThread to avoid blocking the main thread.
        // GPU model loading can take several seconds; running it on the HandlerThread
        // keeps the main thread free and prevents the 5-second startForeground() timeout.
        // The try-catch is critical: an uncaught Throwable in a HandlerThread propagates
        // to the default UncaughtExceptionHandler and kills the entire process.
        Handler(faceLandmarkerHelper.looper).post {
            try {
                faceLandmarkerHelper.init(this@FaceDetectionForegroundService)
                // 최초 설치 직후 GPU/CPU 델리게이트가 모두 실패하는 경우,
                // faceLandmarker가 null로 남아 얼굴 감지가 안 될 수 있음.
                // 동일 HandlerThread에서 3초 뒤 재시도 → 서비스 재시작 없이 자동 복구.
                if (!faceLandmarkerHelper.isModelLoaded()) {
                    Timber.w("MediaPipe: 모델 로드 실패, 3초 후 재시도")
                    Handler(faceLandmarkerHelper.looper).postDelayed({
                        if (!faceLandmarkerHelper.isModelLoaded()) {
                            try {
                                Timber.i("MediaPipe 초기화 재시도...")
                                faceLandmarkerHelper.init(this@FaceDetectionForegroundService)
                            } catch (t: Throwable) {
                                Timber.e(t, "FaceLandmarkerHelper 재시도 실패")
                            }
                        }
                    }, 3000L)
                }
            } catch (t: Throwable) {
                Timber.e(t, "FaceLandmarkerHelper init failed on HandlerThread")
            }
        }
    }

    private fun processResults(rawValues: Map<String, Float>, yaw: Float = 0f, pitch: Float = 0f) {
        if (!isAnalyzing) return

        // HEAD_MOUSE 커서 오버레이: triggerMatcher/actionExecutor 초기화와 독립적으로 동작
        // (커서는 프로필 미설정 상태나 접근성 서비스 바인딩 전에도 표시되어야 함)
        if (currentMode == InteractionMode.HEAD_MOUSE) {
            headTracker.updatePosition(yaw, pitch)
            val cx = headTracker.currentX
            val cy = headTracker.currentY
            MimicAccessibilityService.instance?.cursorTracker?.updateFromHeadTracker(cx, cy)
            // Dwell click은 설정이 활성화된 경우에만 동작. 빅스비 활성 중에는 억제
            val progress = if (dwellClickEnabled && ::dwellClickController.isInitialized && !isBixbyActive) {
                dwellClickController.update(cx, cy, System.currentTimeMillis())
            } else 0f
            mainHandler.post {
                if (Settings.canDrawOverlays(this@FaceDetectionForegroundService)) {
                    if (cursorOverlayView.parent == null) cursorOverlayView.show()
                    cursorOverlayView.update(cx, cy, progress)
                } else {
                    Timber.w("Overlay permission not granted — cursor overlay skipped.")
                }
            }
        } else {
            mainHandler.post { cursorOverlayView.hide() }
        }

        // 트리거 매칭/액션 실행은 triggerMatcher + actionExecutor 초기화 후에만 가능
        val matcher = triggerMatcher
        if (matcher == null || !::actionExecutor.isInitialized) return

        // 빅스비 활성화 중: EMA 처리 포함 모든 표정 분석 건너뜀
        // - EMA를 빅스비 활성 중 갱신하지 않으면 비활성화 시 bixbyDeactivateRunnable에서
        //   reset()이 호출되어 사전 빅스비 상태(중립)로 돌아감 → 즉발 트리거 방지
        // - 글로벌 토글도 억제하여 빅스비 음성 인식 중 의도치 않은 서비스 상태 전환 방지
        if (isBixbyActive) return

        val smoothed = expressionAnalyzer.processSmoothed(rawValues)

        // 글로벌 토글: 표정 채널 검사 (트리거 매칭보다 먼저)
        if (globalToggleController?.checkExpressionToggle(smoothed) == true) {
            return  // 토글이 발동되면 이번 프레임의 다른 트리거는 무시
        }

        val actions = matcher.match(smoothed)

        if (actions.isNotEmpty()) {
            mainHandler.post {
                actions.forEach { action ->
                    // 모드별 Action 필터링
                     if (ModeManager.isActionAllowed(currentMode, action)) {
                        actionExecutor.execute(action)
                        if (::actionFeedbackController.isInitialized) {
                            actionFeedbackController.onActionTriggered()
                        }
                        // Manual gesture tap in Head Mouse mode should reset the dwell timer
                        if (currentMode == InteractionMode.HEAD_MOUSE && action is Action.TapAtCursor) {
                            if (::dwellClickController.isInitialized) {
                                dwellClickController.reset(System.currentTimeMillis())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeSettingsAndProfile() {
        serviceScope.launch {
            combine(
                settingsRepository.getSettings(),
                profileRepository.getActiveProfile()
            ) { settings, profile -> Pair(settings, profile) }
                .conflate()  // 설정 변경이 빠르게 연속될 때 중간값 건너뜀 — 최신값만 처리
                .collect { (settings, profile) ->
                    // Update EMA analyzer with settings
                    expressionAnalyzer.updateSettings(emaAlpha = settings.emaAlpha)

                    // Update camera facing if needed (requires restart)
                    // 모드 업데이트
                    currentMode = settings.activeMode
                    targetServiceState = settings.targetServiceState

                    // 글로벌 토글 설정 업데이트
                    globalToggleController?.updateSettings(settings)

                    // Phase 3: Head Mouse settings
                    dwellClickEnabled = settings.dwellClickEnabled
                    if (::headTracker.isInitialized) {
                        // Base sensitivity 2500f * multiplier
                        headTracker.sensitivityX = 2500f * settings.headMouseSensitivity
                        headTracker.sensitivityY = 2500f * settings.headMouseSensitivity
                        headTracker.deadzone = settings.headMouseDeadZone
                    }
                    if (::dwellClickController.isInitialized) {
                        dwellClickController.dwellDurationMs = settings.dwellClickTimeMs
                        dwellClickController.thresholdPixel = settings.dwellClickRadiusPx
                    }

                    // 액션 피드백 설정 업데이트
                    if (::actionFeedbackController.isInitialized) {
                        actionFeedbackController.updateSettings(
                            visual = settings.actionFeedbackVisual,
                            audio = settings.actionFeedbackAudio,
                            vibrate = settings.actionFeedbackVibrate
                        )
                    }

                    // BT 마우스 패스스루 모드: CURSOR_CLICK 모드일 때만 적용
                    // 패스스루 ON → setMotionEventSources(0) → 네이티브 마우스 유지
                    // 패스스루 OFF → setMotionEventSources(SOURCE_MOUSE) → 앱이 인터셉트
                    val shouldIntercept = currentMode == InteractionMode.CURSOR_CLICK && !settings.btMousePassthrough
                    currentShouldIntercept = shouldIntercept
                    // 현재 분석 중인 경우에만 인터셉트 적용 (일시정지/중지 상태에서는 인터셉트 OFF 유지)
                    MimicAccessibilityService.instance?.updateMouseInterceptMode(shouldIntercept && isAnalyzing)

                    // Profile triggers and cooldown
                    if (profile != null) {
                        activeProfileName = profile.name
                        triggerMatcher = TriggerMatcher(
                            triggers = profile.triggers.filter { t -> t.isEnabled },
                            globalCooldownMs = profile.globalCooldownMs,
                            requiredFrames = settings.consecutiveFrames
                        )
                    } else {
                        // 활성 프로필이 없으면 트리거 매칭 비활성화
                        activeProfileName = null
                        triggerMatcher = null
                    }
                    updateNotification()
                }
        }
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                // 해상도 320×240: MediaPipe 내부 처리(213×160)보다 충분히 크면서 Bitmap 메모리/GC 부담 최소화
                // FPS: HEAD_MOUSE는 커서 부드러움을 위해 12~20, 그 외 표정 인식은 8~10으로 충분
                val targetFps = if (currentMode == InteractionMode.HEAD_MOUSE) Range(12, 20) else Range(8, 10)
                val imageAnalysisBuilder = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                Camera2Interop.Extender(imageAnalysisBuilder)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetFps)
                val imageAnalysis = imageAnalysisBuilder.build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            // detectLiveStream calls postProcessLandmarks via MediaPipe async callback
                            // which then calls our FaceResultListener
                            faceLandmarkerHelper.detectLiveStream(imageProxy)
                        }
                    }

                provider.unbindAll()
                // Preview UseCase는 기본적으로 바인딩하지 않음 — 테스트 화면 열릴 때만 동적 추가
                // ImageAnalysis(320×240) 단독 바인딩으로 카메라가 낮은 해상도로 동작
                val camera = provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis
                )
                // 테스트 화면이 열려있는 동안 pause/resume 시 Preview UseCase 자동 재바인딩
                val sp = activeSurfaceProvider
                if (sp != null) {
                    previewUseCase.setSurfaceProvider(sp)
                    try {
                        provider.bindToLifecycle(this, cameraSelector, previewUseCase)
                    } catch (e: Exception) {
                        Timber.w(e, "Preview UseCase 재바인딩 실패")
                    }
                }
                // Monitor camera state errors (e.g. another app using camera)
                camera.cameraInfo.cameraState.observe(this) { state ->
                    state.error?.let { error ->
                        Timber.w("Camera state error: ${error.code}")
                        pauseAnalysis()
                        updateNotification()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "CameraX binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** CameraX 언바인드: 카메라 하드웨어를 실제로 해제하여 다른 앱이 사용할 수 있도록 합니다. */
    private fun unbindCamera() {
        cameraProvider?.unbindAll()
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> pauseAnalysis()
                Intent.ACTION_SCREEN_ON  -> if (ServiceStatePolicy.shouldResumeAfterScreenOn(targetServiceState)) {
                    resumeAnalysis()
                }
            }
        }
    }

    fun pauseAnalysis() {
        if (::actionExecutor.isInitialized) actionExecutor.cancelCurrentGesture()
        isAnalyzing = false
        // 일시정지 시 마우스 인터셉트 해제 → 네이티브 마우스 동작 복원
        MimicAccessibilityService.instance?.updateMouseInterceptMode(false)
        faceLandmarkerHelper.pauseThread()
        // CameraX 언바인드: 카메라 하드웨어를 실제로 해제해야 다른 앱(잠금 해제 등)이 카메라 사용 가능
        unbindCamera()
        mainHandler.post { cursorOverlayView.hide() }
        updateRuntimeState(ServiceState.Paused)
    }

    fun resumeAnalysis() {
        isAnalyzing = true
        // 재개 시 인터셉트 상태 복원 (설정에 따라 저장된 currentShouldIntercept 값 사용)
        MimicAccessibilityService.instance?.updateMouseInterceptMode(currentShouldIntercept)
        faceLandmarkerHelper.resumeThread()
        // CameraX 재바인드: 카메라를 다시 열어 감지 재개
        setupCamera()
        updateRuntimeState(ServiceState.Running)
    }

    /** QS 타일에 상태 변경을 알려 Bixby 등 외부 도구가 최신 상태를 읽을 수 있게 함. */
    private fun requestTileUpdate() {
        try {
            TileService.requestListeningState(
                this,
                ComponentName(this, MimicToggleTileService::class.java)
            )
        } catch (e: Exception) {
            Timber.w(e, "QS 타일 갱신 요청 실패")
        }
    }

    private fun applyTargetState(targetState: ServiceState) {
        targetServiceState = targetState
        when (targetState) {
            ServiceState.Running -> resumeAnalysis()
            ServiceState.Paused -> pauseAnalysis()
            ServiceState.Stopped -> stopServiceRuntime()
        }
    }

    private fun updateRuntimeState(state: ServiceState) {
        _serviceState.value = state
        serviceScope.launch {
            MimicServiceStateStore.persistRuntimeState(this@FaceDetectionForegroundService, state)
        }
        updateNotification()
        requestTileUpdate()
    }

    private fun stopServiceRuntime() {
        if (::actionExecutor.isInitialized) actionExecutor.cancelCurrentGesture()
        isAnalyzing = false
        // 중지 시 마우스 인터셉트 해제 → 네이티브 마우스 동작 복원
        MimicAccessibilityService.instance?.updateMouseInterceptMode(false)
        faceLandmarkerHelper.pauseThread()
        unbindCamera()
        // BIND_AUTO_CREATE 바인딩이 살아있는 한 stopSelf()는 무효 — 먼저 언바인딩.
        MimicAccessibilityService.instance?.unbindFaceDetectionService()
        // targetServiceState 저장은 호출측(HomeViewModel / MimicAccessibilityService)에서
        // ACTION_STOP 발송 전에 이미 처리한다. 여기서 비동기로 다시 저장하면 빠른 stop→start
        // 시퀀스에서 코루틴이 늦게 실행되어 Stopped 상태를 덮어쓰는 경쟁 조건이 발생한다.
        _serviceState.value = ServiceState.Stopped
        serviceScope.launch {
            MimicServiceStateStore.persistRuntimeState(this@FaceDetectionForegroundService, ServiceState.Stopped)
        }
        requestTileUpdate()
        // stopForeground 전에 알림을 명시적으로 제거: updateNotification() 없이 직접 처리하여
        // 코루틴이 stopForeground 이후 nm.notify()로 알림을 재생성하는 타이밍 버그를 방지
        stopForeground(STOP_FOREGROUND_REMOVE)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
        stopSelf()
    }

    fun togglePause() {
        if (_serviceState.value == ServiceState.Paused) resumeAnalysis() else pauseAnalysis()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (_serviceState.value == ServiceState.Stopped) {
            // Stopped 상태에서는 알림을 재생성하지 말고 명시적으로 제거
            nm.cancel(NOTIFICATION_ID)
            return
        }
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification() = run {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val paused = _serviceState.value == ServiceState.Paused
        val pauseResumeIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FaceDetectionForegroundService::class.java).apply {
                action = if (paused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, FaceDetectionForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("MimicEase")
            .setContentText(
                if (paused) "일시정지됨"
                else "표정 감지 중 — ${activeProfileName ?: "프로필 없음"}"
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(
                if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (paused) "재개" else "일시정지",
                pauseResumeIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "앱 열기",
                openIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "정지",
                stopIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_instance == this) _instance = null
        _serviceState.value = ServiceState.Stopped
        CoroutineScope(Dispatchers.IO).launch {
            MimicServiceStateStore.persistRuntimeState(this@FaceDetectionForegroundService, ServiceState.Stopped)
        }
        // 서비스 종료 → QS 타일을 "미실행" 상태로 갱신
        requestTileUpdate()
        // cursorOverlayView는 Main thread에서 해제 — serviceScope.cancel() 이전에 처리해야 합니다.
        // cancel 이후 serviceScope.launch()는 즉시 CancellationException으로 실패합니다.
        mainHandler.removeCallbacks(bixbyResumeTimeoutRunnable)
        mainHandler.removeCallbacks(bixbyDeactivateRunnable)
        if (::cursorOverlayView.isInitialized) {
            ContextCompat.getMainExecutor(this).execute { cursorOverlayView.hide() }
        }
        if (::actionFeedbackController.isInitialized) actionFeedbackController.destroy()
        serviceScope.cancel()
        cameraExecutor.shutdown()
        unregisterReceiver(screenStateReceiver)
        faceLandmarkerHelper.destroy()
    }
}
