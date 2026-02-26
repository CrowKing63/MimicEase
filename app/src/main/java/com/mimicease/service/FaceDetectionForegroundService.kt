package com.mimicease.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.gameface.FaceLandmarkerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class FaceDetectionForegroundService : LifecycleService() {

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

    @Inject lateinit var profileRepository: ProfileRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        expressionAnalyzer = ExpressionAnalyzer()
        initFaceLandmarker()
        observeActiveProfile()
        registerScreenStateReceiver()
        setupCamera()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildNotification())
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
        // Assume FaceLandmarkerHelper has appropriate constructors or init methods
        faceLandmarkerHelper = FaceLandmarkerHelper()
        faceLandmarkerHelper.init(this)
        // Set listener manually if unsupported in constructor based on Gameface java spec
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

    private fun observeActiveProfile() {
        serviceScope.launch {
            profileRepository.getActiveProfile()
                .collect { profile ->
                    profile?.let {
                        triggerMatcher = TriggerMatcher(
                            triggers = it.triggers.filter { t -> t.isEnabled },
                            globalCooldownMs = it.globalCooldownMs
                        )
                        expressionAnalyzer.updateSettings(it.sensitivity, 3) 
                        // Update Notification if needed
                    }
                }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
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
                        faceLandmarkerHelper.detectLiveStream(imageProxy)
                        
                        // We extract results here manually since our GameFace java module
                        // stores results inside the helper class (or we need to wire up the callback)
                        val blendshapes = faceLandmarkerHelper.blendshapes
                        val blendMap = mutableMapOf<String, Float>()
                        // We map float[] to map here based on indices roughly - TODO mapping
                        processResults(blendMap)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis
                )
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
                Intent.ACTION_SCREEN_ON  -> resumeAnalysis()
            }
        }
    }

    fun pauseAnalysis() { isAnalyzing = false }
    fun resumeAnalysis() { isAnalyzing = true }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        cameraExecutor.shutdown()
        unregisterReceiver(screenStateReceiver)
        faceLandmarkerHelper.destroy()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("MimicEase Running")
        .setContentText("Detecting facial expressions...")
        .build() // add required icons when resource is available

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mimic_ease_service_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "MimicEase Detection Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when MimicEase is detecting expressions"
                    setShowBadge(false)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }
}
