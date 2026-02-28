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
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.mimicease.MainActivity
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
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

        // SharedFlow exposed for UI screens to observe real-time blendshapes
        private val _blendShapeFlow = MutableSharedFlow<Map<String, Float>>(replay = 1)
        val blendShapeFlow: SharedFlow<Map<String, Float>> = _blendShapeFlow.asSharedFlow()

        private val _inferenceTimeMs = MutableStateFlow(0L)
        val inferenceTimeMs: StateFlow<Long> = _inferenceTimeMs.asStateFlow()

        private val _isFaceVisible = MutableStateFlow(false)
        val isFaceVisible: StateFlow<Boolean> = _isFaceVisible.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

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

    private var isAnalyzing = true
    private var activeProfileName: String? = null

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
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
                isAnalyzing = false
                _isPaused.value = true
                faceLandmarkerHelper.pauseThread()
                updateNotification()
            }
            ACTION_RESUME -> {
                isAnalyzing = true
                _isPaused.value = false
                faceLandmarkerHelper.resumeThread()
                updateNotification()
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
    }

    private fun initFaceLandmarker() {
        faceLandmarkerHelper = FaceLandmarkerHelper()
        faceLandmarkerHelper.start()  // Start HandlerThread
        faceLandmarkerHelper.setFaceResultListener { blendshapes, mediapipeMs, faceVisible ->
            _isFaceVisible.value = faceVisible
            _inferenceTimeMs.value = mediapipeMs
            if (faceVisible && blendshapes.isNotEmpty()) {
                serviceScope.launch { _blendShapeFlow.emit(blendshapes) }
                processResults(blendshapes)
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

    private fun processResults(rawValues: Map<String, Float>) {
        if (!isAnalyzing || !::triggerMatcher.isInitialized || !::actionExecutor.isInitialized) return

        val smoothed = expressionAnalyzer.processSmoothed(rawValues)
        val actions = triggerMatcher.match(smoothed)

        if (actions.isNotEmpty()) {
            serviceScope.launch(Dispatchers.Main) {
                actions.forEach { actionExecutor.execute(it) }
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
                val cameraProvider = cameraProviderFuture.get()

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

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis
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
        updateNotification()
    }

    fun resumeAnalysis() {
        isAnalyzing = true
        _isPaused.value = false
        faceLandmarkerHelper.resumeThread()
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
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        cameraExecutor.shutdown()
        unregisterReceiver(screenStateReceiver)
        faceLandmarkerHelper.destroy()
    }
}
