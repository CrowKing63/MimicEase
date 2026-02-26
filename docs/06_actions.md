> **[MimicEase 사양서 — 06/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 06. 액션 설정 (Action Configuration)

트리거에 연결할 수 있는 액션의 전체 목록과 구현 명세입니다.
액션은 `Action` sealed class로 정의됩니다 (전체 Domain 모델은 `08_data_model.md` 참조).

액션 UI는 **시스템 / 제스처 / 앱·미디어** 3개 탭으로 분류됩니다.

---

## 6.1 시스템 액션 (System Actions)

`AccessibilityService.performGlobalAction()`으로 실행합니다.

| 액션 ID (sealed class) | UI 이름 | performGlobalAction 상수 | 최소 API |
|------------------------|---------|--------------------------|---------|
| `GlobalHome` | 홈 버튼 | `GLOBAL_ACTION_HOME` | 16 |
| `GlobalBack` | 뒤로가기 | `GLOBAL_ACTION_BACK` | 16 |
| `GlobalRecents` | 최근 앱 | `GLOBAL_ACTION_RECENTS` | 16 |
| `GlobalNotifications` | 알림 패널 열기 | `GLOBAL_ACTION_NOTIFICATIONS` | 16 |
| `GlobalQuickSettings` | 빠른 설정 열기 | `GLOBAL_ACTION_QUICK_SETTINGS` | 16 |
| `ScreenLock` | 화면 잠금 | `GLOBAL_ACTION_LOCK_SCREEN` | 28 |
| `TakeScreenshot` | 스크린샷 | `GLOBAL_ACTION_TAKE_SCREENSHOT` | 28 |
| `PowerDialog` | 전원 메뉴 | `GLOBAL_ACTION_POWER_DIALOG` | 21 |

```kotlin
// ActionExecutor.kt 예시
is Action.GlobalHome ->
    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

is Action.ScreenLock -> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }
}
```

---

## 6.2 제스처 액션 (Gesture Actions)

`GestureDescription` API로 실행합니다. 좌표는 화면 상대값(0.0~1.0)을 사용하며, 실행 시 픽셀 절대값으로 변환합니다.

### 6.2.1 탭 계열

| 액션 ID | UI 이름 | 파라미터 | 설명 |
|---------|---------|---------|------|
| `TapCenter` | 화면 중앙 탭 | 없음 | 화면 정중앙 (0.5, 0.5) 탭 |
| `TapCustom(x, y)` | 커스텀 위치 탭 | `x: Float, y: Float` (0.0~1.0) | 사용자 지정 위치 탭 |
| `DoubleTap(x, y)` | 두 번 탭 | `x: Float, y: Float` | 동일 위치 연속 두 번 탭 |
| `LongPress(x, y)` | 길게 누르기 | `x: Float, y: Float` | 1000ms 길게 탭 |

```kotlin
// 탭 실행
fun executeTap(x: Float, y: Float, durationMs: Long = 50L) {
    val (absX, absY) = relToAbs(x, y)
    val path = Path().apply { moveTo(absX, absY) }
    val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
    service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
}

// 두 번 탭 (100ms 간격)
fun executeDoubleTap(x: Float, y: Float) {
    val (absX, absY) = relToAbs(x, y)
    val path = Path().apply { moveTo(absX, absY) }
    val builder = GestureDescription.Builder()
    builder.addStroke(GestureDescription.StrokeDescription(path, 0L, 50L, true))   // continueStroke=true
    builder.addStroke(GestureDescription.StrokeDescription(path, 150L, 50L, false))
    service.dispatchGesture(builder.build(), null, null)
}
```

### 6.2.2 스와이프 계열

| 액션 ID | UI 이름 | 기본 좌표 (startX,Y → endX,Y) | duration |
|---------|---------|-------------------------------|---------|
| `SwipeUp` | 위로 스와이프 | (0.5, 0.7) → (0.5, 0.3) | 300ms |
| `SwipeDown` | 아래로 스와이프 | (0.5, 0.3) → (0.5, 0.7) | 300ms |
| `SwipeLeft` | 왼쪽 스와이프 | (0.7, 0.5) → (0.3, 0.5) | 300ms |
| `SwipeRight` | 오른쪽 스와이프 | (0.3, 0.5) → (0.7, 0.5) | 300ms |
| `ScrollUp` | 위로 스크롤 | AccessibilityNodeInfo 방식 | — |
| `ScrollDown` | 아래로 스크롤 | AccessibilityNodeInfo 방식 | — |

```kotlin
// 스와이프 실행
fun executeSwipe(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long = 300L) {
    val (asx, asy) = relToAbs(sx, sy)
    val (aex, aey) = relToAbs(ex, ey)
    val path = Path().apply { moveTo(asx, asy); lineTo(aex, aey) }
    val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
    service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
}

// AccessibilityNodeInfo 기반 스크롤 (ScrollUp/ScrollDown)
fun executeScroll(isUp: Boolean) {
    val action = if (isUp) AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                 else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
    service.rootInActiveWindow?.let { root ->
        // 스크롤 가능한 첫 번째 노드 찾기
        findScrollableNode(root)?.performAction(action)
    }
}
```

### 6.2.3 드래그 및 핀치

| 액션 ID | UI 이름 | 파라미터 | 설명 |
|---------|---------|---------|------|
| `Drag(sx,sy,ex,ey,duration)` | 드래그 | 시작/끝 좌표, 지속시간 | A→B 드래그 |
| `PinchIn` | 핀치 인 (축소) | 없음 | 화면 중앙 기준 핀치 인 |
| `PinchOut` | 핀치 아웃 (확대) | 없음 | 화면 중앙 기준 핀치 아웃 |

```kotlin
// 드래그 실행
fun executeDrag(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long = 500L) {
    val (asx, asy) = relToAbs(sx, sy)
    val (aex, aey) = relToAbs(ex, ey)
    val path = Path().apply {
        moveTo(asx, asy)
        // 잠시 멈춤 효과: 첫 50ms는 시작점 유지
        lineTo(asx, asy)  // pause point
        lineTo(aex, aey)
    }
    val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
    service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
}

// 핀치 아웃 (두 손가락 멀어지기)
fun executePinchOut() {
    val dm = service.resources.displayMetrics
    val cx = dm.widthPixels / 2f
    val cy = dm.heightPixels / 2f
    val offset = 200f

    val path1 = Path().apply { moveTo(cx, cy); lineTo(cx - offset, cy) }
    val path2 = Path().apply { moveTo(cx, cy); lineTo(cx + offset, cy) }

    val stroke1 = GestureDescription.StrokeDescription(path1, 0L, 400L)
    val stroke2 = GestureDescription.StrokeDescription(path2, 0L, 400L)

    service.dispatchGesture(
        GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build(),
        null, null
    )
}
```

---

## 6.3 앱·미디어 액션 (App & Media Actions)

| 액션 ID | UI 이름 | 구현 방법 | 비고 |
|---------|---------|-----------|------|
| `OpenApp(packageName)` | 앱 열기 | `context.packageManager.getLaunchIntentForPackage()` | 설치된 앱 목록에서 선택 |
| `MediaPlayPause` | 재생/일시정지 | `AudioManager` + `KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE` | |
| `MediaNext` | 다음 트랙 | `KeyEvent.KEYCODE_MEDIA_NEXT` | |
| `MediaPrev` | 이전 트랙 | `KeyEvent.KEYCODE_MEDIA_PREVIOUS` | |
| `VolumeUp` | 볼륨 업 | `AudioManager.adjustVolume(ADJUST_RAISE, FLAG_SHOW_UI)` | |
| `VolumeDown` | 볼륨 다운 | `AudioManager.adjustVolume(ADJUST_LOWER, FLAG_SHOW_UI)` | |
| `MimicPause` | MimicEase 일시정지 | 내부 서비스 플래그 토글 | 실수로 발동 시 빠른 정지 용도 |

```kotlin
// 앱 열기
fun openApp(packageName: String) {
    val intent = service.packageManager.getLaunchIntentForPackage(packageName)
        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    intent?.let { service.startActivity(it) }
}

// 미디어 키 전송
fun sendMediaKey(keyCode: Int) {
    val am = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

// MimicEase 일시정지 토글
fun pauseService() {
    // FaceDetectionForegroundService의 isAnalyzing 플래그를 false로
    // 알림에서 '재개' 버튼으로 복귀
    ServiceBridge.togglePause()
}
```

---

## 6.4 커스텀 좌표 설정 UI (CoordinatePickerScreen)

`TapCustom`, `DoubleTap`, `LongPress`, `Drag` 액션을 선택했을 때 진입하는 화면입니다.

### 6.4.1 화면 동작

```
1. 현재 화면 스크린샷을 배경으로 표시 (반투명 오버레이)
2. 16x16 격자선 오버레이
3. 사용자가 터치한 위치에 마커(●) 표시
4. 좌표를 퍼센트로 즉시 표시: "위치: 52%, 38%"
5. Drag 액션의 경우: 첫 탭 = 시작점(🟢), 두 번째 탭 = 끝점(🔴)

         [초기화]                    [확인]
```

### 6.4.2 좌표 변환

```kotlin
// 터치 이벤트 좌표 → 상대 좌표 (0.0~1.0)
fun toRelativeCoordinate(touchX: Float, touchY: Float, screenWidth: Int, screenHeight: Int): Pair<Float, Float> {
    return (touchX / screenWidth) to (touchY / screenHeight)
}

// 상대 좌표 → 퍼센트 문자열 (UI 표시용)
fun toPercentString(relX: Float, relY: Float): String {
    return "위치: ${"%.0f".format(relX * 100)}%, ${"%.0f".format(relY * 100)}%"
}
```

---

## 6.5 액션 선택 UI 흐름

```
TriggerEditScreen
  ↓ '액션 선택' 버튼 탭
ActionPickerBottomSheet
  ├── [시스템] 탭: 홈, 뒤로가기, 최근 앱, 알림, 빠른 설정, 화면 잠금, 스크린샷
  ├── [제스처] 탭: 탭, 두 번 탭, 길게 누르기, 스와이프 4방향, 스크롤, 드래그, 핀치
  └── [앱·미디어] 탭: 앱 목록 (설치된 앱), 미디어 제어, 볼륨, 일시정지
      ↓ 액션 선택
  (추가 파라미터가 필요한 액션: TapCustom, Drag, OpenApp)
      ↓
  CoordinatePickerScreen  (TapCustom / Drag)
  또는
  AppPickerScreen  (OpenApp — 설치된 앱 목록)
      ↓ 완료
  TriggerEditScreen (액션 파라미터 자동 채워짐)
```

### AppPickerScreen

```kotlin
// 설치된 앱 목록 가져오기
fun getInstalledApps(context: Context): List<AppInfo> {
    return context.packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }  // 시스템 앱 제외
        .map { AppInfo(
            packageName = it.packageName,
            appName = context.packageManager.getApplicationLabel(it).toString(),
            icon = context.packageManager.getApplicationIcon(it)
        )}
        .sortedBy { it.appName }
}
```
