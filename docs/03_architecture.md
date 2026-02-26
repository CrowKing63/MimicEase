> **[MimicEase 사양서 — 03/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 03. 시스템 아키텍처

## 3.1 전체 데이터 흐름

```
┌─────────────────────────────────────────────────────┐
│  FaceDetectionForegroundService  (Foreground Service) │
│                                                       │
│  CameraX (전면 카메라)                                │
│    ↓ ImageProxy (매 프레임)                           │
│  FaceLandmarkerHelper  ← GameFace 모듈                │
│    ↓ FaceLandmarkerResult                             │
│      (blendshapes: List<List<Category>>, 52개 값)     │
│  ExpressionAnalyzer                                   │
│    ↓ Map<String, Float>  (EMA 필터 + 연속 프레임 확정) │
│  TriggerMatcher  ← 활성 Profile의 Trigger 목록 (Flow) │
│    ↓ List<Action>  (쿨다운·우선순위 적용 후)           │
│  ActionExecutor                                       │
└─────────────────────────────────────────────────────┘
         ↓                           ↓
  MimicAccessibilityService      Intent / AudioManager
  (performGlobalAction,          (앱 실행, 미디어 제어)
   dispatchGesture,
   AccessibilityNodeInfo)
         ↓
  실제 액션 수행
  (홈, 뒤로가기, 탭, 스와이프, 드래그, 앱 열기 등)
```

## 3.2 두 서비스 구조

| 서비스 | 클래스 | 역할 | 실행 방식 |
|--------|--------|------|-----------|
| **접근성 서비스** | `MimicAccessibilityService` | 시스템 전체 컨트롤 진입점. `dispatchGesture()`, `performGlobalAction()` 실행. | 시스템 접근성 서비스 — 사용자가 설정에서 직접 활성화 |
| **포그라운드 서비스** | `FaceDetectionForegroundService` | 카메라 스트림 유지 + GameFace 엔진 실행. 포그라운드 알림 표시. | `startForegroundService()` — 알림 없으면 시스템이 종료 |

두 서비스는 **바인딩(Bind)**으로 통신합니다:
- `MimicAccessibilityService`가 `FaceDetectionForegroundService`에 바인딩
- `ActionExecutor`는 `MimicAccessibilityService`의 참조를 받아 제스처 실행

## 3.3 컴포넌트 상세

### 3.3.1 FaceLandmarkerHelper (GameFace 모듈)

**역할**: MediaPipe Face Landmarker 모델 로드 + 카메라 프레임 처리

```kotlin
// 초기화 예시
val helper = FaceLandmarkerHelper(
    context = context,
    faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
        override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
            val blendshapes = resultBundle.result.blendshapes()
            // blendshapes[0] = 첫 번째 얼굴의 Category 리스트
            // Category.categoryName() = 블렌드쉐이프 ID (String)
            // Category.score() = 0.0 ~ 1.0 (Float)
        }
        override fun onError(error: String, errorCode: Int) { }
    }
)

// 카메라 프레임 처리 (ImageAnalysis.Analyzer 내부에서 호출)
helper.detectLiveStream(imageProxy, isFrontCamera = true)
```

**주요 설정값**:
- `minFaceDetectionConfidence`: 0.5 (기본)
- `minFaceTrackingConfidence`: 0.5 (기본)
- `minFacePresenceConfidence`: 0.5 (기본)
- `maxNumFaces`: 1 (단일 사용자)
- `runningMode`: `RunningMode.LIVE_STREAM`
- `delegate`: GPU 우선, 실패 시 CPU 폴백

---

### 3.3.2 ExpressionAnalyzer

**역할**: 원시 블렌드쉐이프 값 → 안정적인 표정 상태 변환

**처리 파이프라인**:

```
rawValues: Map<String, Float>
  → [1단계] EMA 필터 (노이즈 제거, 떨림 방지)
  → smoothedValues: Map<String, Float>
  → [2단계] 임계값 기준 활성 표정 Set 추출
  → activeShapes: Set<String>
  → [3단계] 연속 프레임 확정 (단발성 오트리거 방지)
  → confirmedShapes: Set<String>  ← TriggerMatcher로 전달
```

**EMA 필터 구현**:
```kotlin
class ExpressionAnalyzer(private val alpha: Float = 0.5f) {
    private val smoothedValues = mutableMapOf<String, Float>()

    fun process(rawValues: Map<String, Float>): Map<String, Float> {
        rawValues.forEach { (key, newValue) ->
            val prev = smoothedValues[key] ?: newValue
            smoothedValues[key] = alpha * newValue + (1 - alpha) * prev
        }
        return smoothedValues.toMap()
    }
}
// alpha: 0.3(부드러움/느림) ~ 0.7(빠름/거침). 사용자 설정으로 조절.
```

**연속 프레임 확정 구현**:
```kotlin
class ConsecutiveFrameChecker(private val requiredFrames: Int = 3) {
    private val frameCounters = mutableMapOf<String, Int>()

    // 반환: 이번 프레임에서 처음으로 N회 연속 달성한 표정 Set
    fun check(activeShapes: Set<String>): Set<String> {
        val confirmed = mutableSetOf<String>()
        (frameCounters.keys + activeShapes).toSet().forEach { key ->
            if (key in activeShapes) {
                val count = (frameCounters[key] ?: 0) + 1
                frameCounters[key] = count
                if (count == requiredFrames) confirmed.add(key)
            } else {
                frameCounters.remove(key)
            }
        }
        return confirmed
    }
}
```

---

### 3.3.3 TriggerMatcher

**역할**: 표정 상태 + 트리거 목록 → 실행할 Action 결정

**입력**: `smoothedValues: Map<String, Float>` (EMA 적용 완료값, 연속 프레임 확정 전)

> ⚠️ TriggerMatcher는 **연속 프레임 확정 이전** 값을 받습니다. 홀드 시간(holdDurationMs) 계산을 내부에서 직접 처리하기 때문입니다.

**처리 로직**:

```kotlin
class TriggerMatcher(
    private val triggers: List<Trigger>,
    private val globalCooldownMs: Int
) {
    private val lastFiredTime = mutableMapOf<String, Long>()  // triggerId → timestamp
    private var lastAnyFiredTime = 0L
    private val holdStartTime = mutableMapOf<String, Long>()  // triggerId → hold 시작 시각

    fun match(smoothedValues: Map<String, Float>): List<Action> {
        val now = SystemClock.elapsedRealtime()
        val actions = mutableListOf<Action>()

        // 1. 전역 쿨다운 체크
        if (now - lastAnyFiredTime < globalCooldownMs) return emptyList()

        // 2. 활성화된 트리거만 우선순위 순으로 처리
        triggers.filter { it.isEnabled }
            .sortedBy { it.priority }
            .forEach { trigger ->
                val value = smoothedValues[trigger.blendShape.id] ?: 0f

                if (value >= trigger.threshold) {
                    // 홀드 시간 추적
                    val holdStart = holdStartTime.getOrPut(trigger.id) { now }
                    val holdElapsed = now - holdStart

                    // 개별 쿨다운 체크 + 홀드 시간 충족 시 발동
                    val lastFired = lastFiredTime[trigger.id] ?: 0L
                    if (holdElapsed >= trigger.holdDurationMs &&
                        now - lastFired >= trigger.cooldownMs) {

                        actions.add(trigger.action)
                        lastFiredTime[trigger.id] = now
                        lastAnyFiredTime = now
                        holdStartTime.remove(trigger.id)
                    }
                } else {
                    holdStartTime.remove(trigger.id)  // 표정 해제 시 홀드 리셋
                }
            }

        return actions
    }
}
```

---

### 3.3.4 ActionExecutor

**역할**: `Action` → 실제 Android 시스템 동작

**의존성**: `MimicAccessibilityService` 인스턴스 (바인딩으로 전달)

```kotlin
class ActionExecutor(private val service: MimicAccessibilityService) {

    fun execute(action: Action) {
        when (action) {
            is Action.GlobalHome ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            is Action.GlobalBack ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            is Action.TapCustom -> {
                val (absX, absY) = relToAbs(action.x, action.y)
                executeTap(absX, absY)
            }
            is Action.SwipeUp -> executeSwipe(0.5f, 0.7f, 0.5f, 0.3f, action.duration)
            is Action.SwipeDown -> executeSwipe(0.5f, 0.3f, 0.5f, 0.7f, action.duration)
            is Action.SwipeLeft -> executeSwipe(0.7f, 0.5f, 0.3f, 0.5f, action.duration)
            is Action.SwipeRight -> executeSwipe(0.3f, 0.5f, 0.7f, 0.5f, action.duration)
            is Action.OpenApp -> openApp(action.packageName)
            is Action.MediaPlayPause -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            is Action.MimicPause -> pauseService()
            // ... 기타 액션
        }
    }

    private fun relToAbs(relX: Float, relY: Float): Pair<Float, Float> {
        val dm = service.resources.displayMetrics
        return relX * dm.widthPixels to relY * dm.heightPixels
    }

    private fun executeTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), null, null
        )
    }

    private fun executeSwipe(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long) {
        val (asx, asy) = relToAbs(sx, sy)
        val (aex, aey) = relToAbs(ex, ey)
        val path = Path().apply { moveTo(asx, asy); lineTo(aex, aey) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), null, null
        )
    }
}
```

## 3.4 컴포넌트 간 통신 요약

```
ViewModel ──StateFlow──→ Composable UI (Presentation Layer)
   ↕ UseCase
Repository ──Flow──→ Room DB / DataStore

FaceDetectionForegroundService (코루틴 스코프)
   ├── CameraX Analyzer → FaceLandmarkerHelper
   ├── ExpressionAnalyzer (IO Dispatcher)
   ├── TriggerMatcher (IO Dispatcher)
   └── ActionExecutor → MimicAccessibilityService (Main Dispatcher)

MimicAccessibilityService
   ├── onServiceConnected() → bindService(FaceDetectionForegroundService)
   └── dispatchGesture() / performGlobalAction() → 시스템 API
```
