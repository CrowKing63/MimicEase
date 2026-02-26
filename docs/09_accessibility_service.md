> **[MimicEase 사양서 — 09/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 09. 접근성 서비스 구현

## 9.1 서비스 선언 (AndroidManifest.xml)

```xml
<service
    android:name=".service.MimicAccessibilityService"
    android:exported="true"
    android:label="@string/accessibility_service_label"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

<service
    android:name=".service.FaceDetectionForegroundService"
    android:exported="false"
    android:foregroundServiceType="camera" />
```

---

## 9.2 접근성 서비스 설정 (res/xml/accessibility_service_config.xml)

```xml
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeViewFocused"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity=".presentation.ui.settings.SettingsActivity" />
```

**주요 플래그 설명**:
- `canPerformGestures="true"` — `dispatchGesture()` 사용에 필수
- `canRetrieveWindowContent="true"` — `rootInActiveWindow` 접근, 스크롤 노드 탐색에 필요
- `flagRetrieveInteractiveWindows` — 다중 윈도우 환경에서 모든 창에 접근

---

## 9.3 MimicAccessibilityService

```kotlin
// service/MimicAccessibilityService.kt
@AndroidEntryPoint
class MimicAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MimicAccessibilityService? = null
            private set
    }

    private var faceDetectionServiceConnection: ServiceConnection? = null
    private var faceDetectionService: FaceDetectionForegroundService? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // FaceDetectionForegroundService 시작 및 바인딩
        val intent = Intent(this, FaceDetectionForegroundService::class.java)
        startForegroundService(intent)
        bindFaceDetectionService()
    }

    private fun bindFaceDetectionService() {
        faceDetectionServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val localBinder = binder as FaceDetectionForegroundService.LocalBinder
                faceDetectionService = localBinder.getService()
                // ActionExecutor에 this(AccessibilityService) 참조 전달
                faceDetectionService?.setAccessibilityService(this@MimicAccessibilityService)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                faceDetectionService = null
            }
        }
        bindService(
            Intent(this, FaceDetectionForegroundService::class.java),
            faceDetectionServiceConnection!!,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 현재 포커스 창 정보 업데이트 (필요 시 활용)
        // MimicEase 자체 이벤트는 소비하지 않음 (TalkBack과의 공존)
    }

    override fun onInterrupt() {
        faceDetectionService?.pauseAnalysis()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceScope.cancel()
        faceDetectionServiceConnection?.let { unbindService(it) }
        faceDetectionService = null
        instance = null
        return super.onUnbind(intent)
    }
}
```

---

## 9.4 FaceDetectionForegroundService

```kotlin
// service/FaceDetectionForegroundService.kt
class FaceDetectionForegroundService : Service() {

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

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        expressionAnalyzer = ExpressionAnalyzer()
        initFaceLandmarker()
        observeActiveProfile()
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(null, false))
        return START_STICKY  // 시스템에 의해 종료되면 자동 재시작
    }

    override fun onBind(intent: Intent) = binder

    fun setAccessibilityService(service: MimicAccessibilityService) {
        actionExecutor = ActionExecutor(service)
    }

    private fun initFaceLandmarker() {
        faceLandmarkerHelper = FaceLandmarkerHelper(
            context = this,
            faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
                override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
                    if (!isAnalyzing) return
                    processResults(resultBundle)
                }
                override fun onError(error: String, errorCode: Int) {
                    Timber.e("FaceLandmarker error: $error (code=$errorCode)")
                }
            }
        )
    }

    private fun processResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        // 1. 블렌드쉐이프 추출 → Map 변환
        val rawValues: Map<String, Float> = resultBundle.result.blendshapes()
            .firstOrNull()
            ?.associate { it.categoryName() to it.score() }
            ?: return

        // 2. EMA 필터 적용
        val smoothed = expressionAnalyzer.process(rawValues)

        // 3. 트리거 매칭 → 액션 결정
        val actions = triggerMatcher.match(smoothed)

        // 4. 액션 실행 (Main 스레드에서)
        if (actions.isNotEmpty()) {
            serviceScope.launch(Dispatchers.Main) {
                actions.forEach { actionExecutor.execute(it) }
            }
        }
    }

    private fun observeActiveProfile() {
        serviceScope.launch {
            profileRepository.getActiveProfileWithTriggers()
                .collect { profile ->
                    profile?.let {
                        triggerMatcher = TriggerMatcher(
                            triggers = it.triggers.filter { t -> t.isEnabled },
                            globalCooldownMs = it.globalCooldownMs
                        )
                        updateNotification(it, false)
                    }
                }
        }
    }

    // 화면 꺼짐/켜짐 브로드캐스트 수신
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
        faceLandmarkerHelper.clearFaceLandmarker()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mimic_ease_service_channel"
    }
}
```

---

## 9.5 CameraX 통합

```kotlin
// service/FaceDetectionForegroundService.kt (카메라 설정 부분)
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
                    faceLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = true
                    )
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,   // Service가 LifecycleOwner 구현 필요
                cameraSelector,
                imageAnalysis
            )
        } catch (e: Exception) {
            Timber.e(e, "CameraX 바인딩 실패")
        }
    }, ContextCompat.getMainExecutor(this))
}
```

> **주의**: `ForegroundService`는 기본적으로 `LifecycleOwner`가 아닙니다.
> `LifecycleService`를 상속하거나, `ServiceLifecycleDispatcher`를 사용하여 CameraX와 통합하세요.
> 또는 별도의 `ProcessLifecycleOwner`를 활용할 수 있습니다.

---

## 9.6 ExpressionAnalyzer 완전 구현

```kotlin
// service/ExpressionAnalyzer.kt
class ExpressionAnalyzer(private var alpha: Float = 0.5f) {

    private val smoothedValues = mutableMapOf<String, Float>()
    private val frameCounters = mutableMapOf<String, Int>()
    private var requiredFrames: Int = 3

    fun updateSettings(emaAlpha: Float, consecutiveFrames: Int) {
        alpha = emaAlpha.coerceIn(0.1f, 0.9f)
        requiredFrames = consecutiveFrames.coerceIn(1, 10)
    }

    // EMA 필터만 적용한 값 반환 (TriggerMatcher용)
    fun processSmoothed(rawValues: Map<String, Float>): Map<String, Float> {
        rawValues.forEach { (key, newValue) ->
            val prev = smoothedValues[key] ?: newValue
            smoothedValues[key] = alpha * newValue + (1 - alpha) * prev
        }
        return smoothedValues.toMap()
    }

    fun reset() {
        smoothedValues.clear()
        frameCounters.clear()
    }
}
```

---

## 9.7 포그라운드 알림 채널 설정

```kotlin
// di/ServiceModule.kt 또는 Application.onCreate()
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            FaceDetectionForegroundService.CHANNEL_ID,
            "MimicEase 감지 서비스",
            NotificationManager.IMPORTANCE_LOW  // 소리/팝업 없음
        ).apply {
            description = "MimicEase가 표정을 감지하는 동안 표시됩니다."
            setShowBadge(false)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
```
