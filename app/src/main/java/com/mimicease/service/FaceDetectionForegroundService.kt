package com.mimicease.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.mimicease.MainActivity
import com.mimicease.domain.model.Action
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.mimicease.gameface.FaceLandmarkerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.mimicease.domain.model.InteractionMode
import com.mimicease.domain.model.ModeManager
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class FaceDetectionForegroundService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mimic_ease_service_channel"

        const val ACTION_PAUSE = "com.mimicease.ACTION_PAUSE"
        const val ACTION_RESUME = "com.mimicease.ACTION_RESUME"
        const val ACTION_STOP = "com.mimicease.ACTION_STOP"

        // SharedFlow exposed for UI screens to observe real-time blendshapes
        private val _blendShapeFlow = MutableSharedFlow<Map<String, Float>>(replay = 1)
        val blendShapeFlow: SharedFlow<Map<String, Float>> = _blendShapeFlow.asSharedFlow()

        // SharedFlow for face mesh landmark coordinates (for FaceMeshOverlay)
        private val _faceLandmarksFlow = MutableSharedFlow<List<NormalizedLandmark>>(replay = 1)
        val faceLandmarksFlow: SharedFlow<List<NormalizedLandmark>> = _faceLandmarksFlow.asSharedFlow()

        // MediaPipe 처리 이미지 크기 — FaceMeshOverlay 좌표 보정에 사용
        private val _imageSizeFlow = MutableStateFlow(Pair(0, 0))
        val imageSizeFlow: StateFlow<Pair<Int, Int>> = _imageSizeFlow.asStateFlow()

        private val _inferenceTimeMs = MutableStateFlow(0L)
        val inferenceTimeMs: StateFlow<Long> = _inferenceTimeMs.asStateFlow()

        private val _isFaceVisible = MutableStateFlow(false)
        val isFaceVisible: StateFlow<Boolean> = _isFaceVisible.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        // Service instance for direct Preview SurfaceProvider attachment (test screen only)
        private var _instance: FaceDetectionForegroundService? = null

        fun attachPreviewSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
            _instance?.previewUseCase?.setSurfaceProvider(surfaceProvider)
        }

        fun detachPreviewSurfaceProvider() {
            _instance?.previewUseCase?.setSurfaceProvider(null)
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
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@FaceDetectionForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private lateinit var expressionAnalyzer: ExpressionAnalyzer
    private lateinit var triggerMatcher: TriggerMatcher
    private lateinit var actionExecutor: ActionExecutor

    // Phase 3: Head Tracking components
    private lateinit var headTracker: HeadTracker
    private lateinit var cursorOverlayView: CursorOverlayView
    private lateinit var dwellClickController: DwellClickController

    private var isAnalyzing = true
    private var activeProfileName: String? = null
    private var currentMode: InteractionMode = InteractionMode.EXPRESSION_ONLY
    private var globalToggleController: GlobalToggleController? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Preview UseCase: SurfaceProvider가 없으면 프리뷰 미표시, 테스트 화면에서 연결 가능
    private val previewUseCase: Preview = Preview.Builder().build()

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

        // startForeground() MUST be called early in onCreate() to satisfy the 5-second
        // timeout on Android 12+. Calling it after slow initialization (like MediaPipe
        // GPU model loading) causes ForegroundServiceDidNotStartInTimeException crash.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasCameraPermission) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        expressionAnalyzer = ExpressionAnalyzer()
        
        // Initialize Phase 3 components
        headTracker = HeadTracker(this)
        cursorOverlayView = CursorOverlayView(this)
        // actionExecutor is injected later via bind, so dwell controller is initialized in setActionExecutor
        
        initFaceLandmarker()
        observeSettingsAndProfile()
        registerScreenStateReceiver()
        if (hasCameraPermission) {
            setupCamera()
        } else {
            Timber.w("Camera permission not granted — skipping camera setup")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PAUSE -> {
                pauseAnalysis()
            }
            ACTION_RESUME -> {
                resumeAnalysis()
            }
            ACTION_STOP -> {
                // 카메라 해제 후 서비스 자체 종료 (BIND_AUTO_CREATE 바인딩이 해제되면 완전 종료)
                unbindCamera()
                isAnalyzing = false
                _isPaused.value = true
                faceLandmarkerHelper.pauseThread()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> updateNotification()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun setAccessibilityService(service: MimicAccessibilityService) {
        actionExecutor = ActionExecutor(service)
        dwellClickController = DwellClickController(actionExecutor)
    }

    fun setGlobalToggleController(controller: GlobalToggleController) {
        globalToggleController = controller
    }

    private fun initFaceLandmarker() {
        faceLandmarkerHelper = FaceLandmarkerHelper()
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
                serviceScope.launch { _blendShapeFlow.emit(blendshapes) }
                if (landmarks.isNotEmpty()) {
                    serviceScope.launch { _faceLandmarksFlow.emit(landmarks) }
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
            } catch (t: Throwable) {
                Timber.e(t, "FaceLandmarkerHelper init failed on HandlerThread")
            }
        }
    }

    private fun processResults(rawValues: Map<String, Float>, yaw: Float = 0f, pitch: Float = 0f) {
        if (!isAnalyzing || !::triggerMatcher.isInitialized || !::actionExecutor.isInitialized) return

        val smoothed = expressionAnalyzer.processSmoothed(rawValues)

        // 글로벌 토글: 표정 채널 검사 (트리거 매칭보다 먼저)
        if (globalToggleController?.checkExpressionToggle(smoothed) == true) {
            return  // 토글이 발동되면 이번 프레임의 다른 트리거는 무시
        }

        // Phase 3: Head Tracking
        if (currentMode == InteractionMode.HEAD_MOUSE) {
            // Update logical cursor position
            val (cx, cy) = headTracker.updatePosition(yaw, pitch)

            // ActionExecutor의 커서 액션(TapAtCursor 등)이 HeadTracker 위치를 참조하도록 동기화
            MimicAccessibilityService.instance?.cursorTracker?.updateFromHeadTracker(cx, cy)

            // Update Dwell progress
            val progress = if (::dwellClickController.isInitialized) {
                dwellClickController.update(cx, cy, System.currentTimeMillis())
            } else 0f

            // Draw overlay — SYSTEM_ALERT_WINDOW 권한이 런타임에 허용된 경우에만 오버레이 표시
            // 미허용 시 windowManager.addView()가 SecurityException을 던져 앱이 꺼짐
            serviceScope.launch(Dispatchers.Main) {
                if (Settings.canDrawOverlays(this@FaceDetectionForegroundService)) {
                    if (cursorOverlayView.parent == null) {
                        cursorOverlayView.show()
                    }
                    cursorOverlayView.update(cx, cy, progress)
                } else {
                    Timber.w("Overlay permission not granted — cursor overlay skipped. Go to Settings > Apps > Special app access > Display over other apps")
                }
            }
        } else {
            // Hide overlay if not in mode
            serviceScope.launch(Dispatchers.Main) {
                cursorOverlayView.hide()
            }
        }

        val actions = triggerMatcher.match(smoothed)

        if (actions.isNotEmpty()) {
            serviceScope.launch(Dispatchers.Main) {
                actions.forEach { action ->
                    // 모드별 Action 필터링
                     if (ModeManager.isActionAllowed(currentMode, action)) {
                        actionExecutor.execute(action)
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
                .collect { (settings, profile) ->
                    // Update EMA analyzer with settings
                    expressionAnalyzer.updateSettings(
                        emaAlpha = settings.emaAlpha,
                        consecutiveFrames = settings.consecutiveFrames
                    )

                    // Update camera facing if needed (requires restart)
                    // 모드 업데이트
                    currentMode = settings.activeMode

                    // 글로벌 토글 설정 업데이트
                    globalToggleController?.updateSettings(settings)

                    // Phase 3: Head Mouse settings
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

                    // Profile triggers and cooldown
                    profile?.let { p ->
                        activeProfileName = p.name
                        triggerMatcher = TriggerMatcher(
                            triggers = p.triggers.filter { t -> t.isEnabled },
                            globalCooldownMs = p.globalCooldownMs
                        )
                        updateNotification()
                    }
                }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            // detectLiveStream calls postProcessLandmarks via MediaPipe async callback
                            // which then calls our FaceResultListener
                            faceLandmarkerHelper.detectLiveStream(imageProxy)
                        }
                    }

                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis,
                    previewUseCase
                )
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
                Intent.ACTION_SCREEN_ON  -> if (!_isPaused.value) resumeAnalysis()
            }
        }
    }

    fun pauseAnalysis() {
        isAnalyzing = false
        _isPaused.value = true
        faceLandmarkerHelper.pauseThread()
        // CameraX 언바인드: 카메라 하드웨어를 실제로 해제해야 다른 앱(잠금 해제 등)이 카메라 사용 가능
        unbindCamera()
        serviceScope.launch(Dispatchers.Main) {
            cursorOverlayView.hide()
        }
        updateNotification()
    }

    fun resumeAnalysis() {
        isAnalyzing = true
        _isPaused.value = false
        faceLandmarkerHelper.resumeThread()
        // CameraX 재바인드: 카메라를 다시 열어 감지 재개
        setupCamera()
        updateNotification()
    }

    fun togglePause() {
        if (_isPaused.value) resumeAnalysis() else pauseAnalysis()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification() = run {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val paused = _isPaused.value
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
        serviceScope.cancel()
        cameraExecutor.shutdown()
        unregisterReceiver(screenStateReceiver)
        
        serviceScope.launch(Dispatchers.Main) {
            if (::cursorOverlayView.isInitialized) {
                cursorOverlayView.hide()
            }
        }
        
        faceLandmarkerHelper.destroy()
    }
}
