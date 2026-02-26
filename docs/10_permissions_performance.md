> **[MimicEase 사양서 — 10/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 10. 권한 요구사항 & 성능 최적화

## 10.1 AndroidManifest.xml 권한 선언

```xml
<!-- 카메라 (런타임 권한 필요) -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 포그라운드 서비스 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- API 34+ : 카메라를 사용하는 포그라운드 서비스 타입 선언 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />

<!-- 부팅 후 자동 시작 (선택사항) -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 진동 피드백 -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- 화면 켜짐 유지 (선택사항, 사용자 옵션으로 제공) -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- 배터리 최적화 제외 요청 -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- 카메라 기능 요구 선언 -->
<uses-feature android:name="android.hardware.camera.front" android:required="true" />
```

---

## 10.2 런타임 권한 처리 전략

### 10.2.1 카메라 권한 흐름

```kotlin
// 온보딩 Step 2에서 사용 (Accompanist Permissions)
@Composable
fun CameraPermissionStep(onGranted: () -> Unit, onDenied: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status) {
        when {
            cameraPermissionState.status.isGranted -> onGranted()
            cameraPermissionState.status.shouldShowRationale -> { /* 이유 설명 UI 표시 */ }
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            // 다음 단계로 자동 진행
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // 설명 다이얼로그 표시 후 재요청
            CameraPermissionRationaleDialog(
                onConfirm = { cameraPermissionState.launchPermissionRequest() },
                onDismiss = onDenied
            )
        }
        else -> {
            // 최초 요청
            SideEffect { cameraPermissionState.launchPermissionRequest() }
        }
    }
}
```

### 10.2.2 접근성 서비스 활성화 유도

```kotlin
// 접근성 서비스 활성화 여부 확인
fun Context.isAccessibilityServiceEnabled(): Boolean {
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == packageName }
}

// 설정 화면으로 이동
fun Context.navigateToAccessibilitySettings() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

// 온보딩 Step 3: 돌아왔을 때 상태 재확인
// → Activity.onResume() 또는 LaunchedEffect(lifecycle) 에서 체크
```

### 10.2.3 배터리 최적화 제외

```kotlin
// 설정 화면 Toggle에서 사용
fun Context.requestIgnoreBatteryOptimization() {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

fun Context.isIgnoringBatteryOptimization(): Boolean {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}
```

### 10.2.4 부팅 후 자동 시작 (선택사항)

```kotlin
// BootReceiver.kt
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // DataStore에서 서비스 활성화 여부 확인 후 시작
            // (DataStore는 비동기이므로 WorkManager 또는 코루틴 사용)
        }
    }
}
```

---

## 10.3 성능 목표

| 지표 | 목표값 | 측정 도구 |
|------|--------|-----------|
| 표정 감지 지연 (end-to-end) | < 100ms | 수동 타이머 측정 |
| MediaPipe 추론 시간 | < 50ms (GPU 사용 시) | `FaceLandmarkerResult.inferenceTime()` |
| CPU 사용률 (백그라운드 감지 중) | < 15% | Android Studio CPU Profiler |
| 메모리 사용량 | < 150MB | Android Studio Memory Profiler |
| 배터리 소모 | < 5%/시간 | Battery Historian 또는 실기기 측정 |
| 앱 Cold Start | < 2초 | Jetpack Macrobenchmark |
| 포그라운드 서비스 유지율 | > 99% | 장시간 사용 안정성 테스트 |

---

## 10.4 성능 최적화 전략

### 10.4.1 GPU 델리게이트 활성화 (최우선)

```kotlin
// FaceLandmarkerHelper 초기화 시
val baseOptionsBuilder = BaseOptions.builder()
    .setModelAssetPath("face_landmarker.task")

// GPU 우선, 실패 시 CPU 폴백
try {
    baseOptionsBuilder.setDelegate(Delegate.GPU)
    // GPU 추론 시간: ~20~30ms
} catch (e: Exception) {
    baseOptionsBuilder.setDelegate(Delegate.CPU)
    // CPU 추론 시간: ~50~80ms
}
```

### 10.4.2 카메라 해상도 및 FPS 제한

```kotlin
// 480x640으로 제한 (얼굴 감지에 충분)
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(480, 640))
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)  // 처리 못한 프레임 드롭
    .build()

// 필요 시 분석 FPS 추가 제한 (API 33+)
// imageAnalysis.setAnalyzerWithScheduler(cameraExecutor, 15, TimeUnit.FPS) { ... }
// 또는 수동으로 타임스탬프 기반 프레임 스킵
```

### 10.4.3 분석 프레임 스킵 (쿨다운 중)

```kotlin
// FaceDetectionForegroundService 내부
private fun processResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
    if (!isAnalyzing) return

    // 전역 쿨다운 중이면 분석 자체를 스킵
    if (SystemClock.elapsedRealtime() - lastGlobalActionTime < MIN_ANALYSIS_GAP_MS) return

    // ... 이하 정상 처리
}

companion object {
    const val MIN_ANALYSIS_GAP_MS = 100L  // 최소 100ms 간격으로 분석
}
```

### 10.4.4 화면 꺼짐 시 카메라 분석 중단

```kotlin
// 화면 꺼지면 isAnalyzing = false → processResults 즉시 리턴
// 카메라 바인딩은 유지 (재시작 지연 방지)
// 화면 켜지면 isAnalyzing = true → 즉시 재개
```

### 10.4.5 코루틴 디스패처 분리

```kotlin
// 카메라 분석 스레드: cameraExecutor (Executors.newSingleThreadExecutor)
// EMA 계산 / TriggerMatcher: Dispatchers.Default (CPU 연산)
// Action 실행: Dispatchers.Main (UI 스레드 / 접근성 API)
// DB 읽기/쓰기: Dispatchers.IO

serviceScope.launch(Dispatchers.Default) {
    val smoothed = expressionAnalyzer.processSmoothed(rawValues)
    val actions = triggerMatcher.match(smoothed)
    if (actions.isNotEmpty()) {
        withContext(Dispatchers.Main) {
            actions.forEach { actionExecutor.execute(it) }
        }
    }
}
```

### 10.4.6 메모리 최적화

```kotlin
// ImageProxy는 반드시 close() 호출
analysis.setAnalyzer(cameraExecutor) { imageProxy ->
    try {
        faceLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera = true)
    } finally {
        imageProxy.close()  // ← 누락 시 메모리 누수 + 카메라 중단
    }
}

// FaceLandmarkerHelper는 서비스 종료 시 반드시 정리
override fun onDestroy() {
    faceLandmarkerHelper.clearFaceLandmarker()  // MediaPipe 리소스 해제
    super.onDestroy()
}
```

---

## 10.5 권한별 UX 처리 요약

| 권한/설정 | 상태 확인 방법 | 미충족 시 UX |
|-----------|--------------|--------------|
| CAMERA | `ContextCompat.checkSelfPermission()` | 온보딩 Step 2에서 요청 다이얼로그 |
| 접근성 서비스 | `AccessibilityManager.getEnabledAccessibilityServiceList()` | 설정으로 이동 버튼 + 복귀 시 재확인 |
| 배터리 최적화 제외 | `PowerManager.isIgnoringBatteryOptimizations()` | 설정 화면 Toggle → 시스템 설정으로 이동 |
| FOREGROUND_SERVICE_CAMERA | 선언만으로 충분 (API 34+) | 없음 (자동 처리) |
