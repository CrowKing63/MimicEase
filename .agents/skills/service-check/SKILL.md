---
name: service-check
description: MimicEase 서비스 레이어(service/) 코드를 작성하거나 수정할 때 자동으로 참조되는 컨텍스트. 서비스 생명주기, 스레딩, 카메라 관리, 접근성 서비스 공존 규칙을 상기시킨다.
user-invocable: false
---

## 서비스 레이어 핵심 규칙

### ❌ 절대 금지
- `android.os.SystemClock` 사용 → `System.currentTimeMillis()` 사용
- `onAccessibilityEvent()`에서 이벤트 consume (TalkBack 체인 깨짐)
- `fillMaxHeight(fraction)` 내부에서 `weight()` 사용 (ModalBottomSheet 크래시)
- Domain 레이어에 Android 클래스 import

### ✅ 서비스 생명주기 체크리스트

**ForegroundService**:
- `onCreate()` 초반에 `startForeground()` 호출 (5초 타임아웃 방지)
- `onStartCommand()`에서 `intent`가 null일 수 있음 (START_STICKY 재시작 시)
- `ACTION_STOP` 인텐트 처리: `unbindCamera()` → `stopForeground()` → `stopSelf()`

**카메라 관리**:
- `pauseAnalysis()` 호출 시 반드시 `unbindCamera()` 포함
- `resumeAnalysis()` 호출 시 `setupCamera()` 재호출
- `CameraState.ERROR_CAMERA_IN_USE` 감지 후 `pauseAnalysis()` 처리 필수

**MediaPipe 초기화**:
```kotlin
Handler(faceLandmarkerHelper.looper).post { init() }  // HandlerThread에서 비동기
```

**FaceDetectionForegroundService processResults() 구조**:
```
HEAD_MOUSE 커서 코드 (가드 전에 실행)
↓
triggerMatcher.isInitialized && actionExecutor.isInitialized 가드
↓
ExpressionAnalyzer → TriggerMatcher → ActionExecutor
```

### 스레드 안전성
- `_instance` companion static 메서드는 메인 스레드에서만 접근
- `faceLandmarksFlow`, `blendShapeFlow` StateFlow emit은 카메라 스레드 (자동 스레드 전환)
- `CursorOverlayView` UI 업데이트는 메인 스레드에서

### SwitchKey 주의
`Action.SwitchKey`는 Android 스위치 제어 서비스와 직접 통신하지 않음.
`AccessibilityNodeInfo` API로 독립 구현 (`SwitchAccessBridge.kt` 참조).

### 관련 파일 목록 (service/ 레이어)
| 파일 | 역할 |
|------|------|
| `FaceDetectionForegroundService.kt` | 포그라운드 서비스 (카메라 + 얼굴 감지) |
| `MimicAccessibilityService.kt` | 접근성 서비스 (액션 실행) |
| `ExpressionAnalyzer.kt` | EMA 필터 (alpha 설정 가능) |
| `TriggerMatcher.kt` | 임계값 + holdDuration + 쿨다운 |
| `ActionExecutor.kt` | GestureDescription, Intent, AudioManager |
| `GlobalToggleController.kt` | 다중 채널 토글 + TTS + 진동 |
| `HeadTracker.kt` | yaw/pitch → 화면 좌표 (데드존 + 가속) |
| `DwellClickController.kt` | 드웰 클릭 진행도 |
