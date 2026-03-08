package com.mimicease.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import com.mimicease.domain.model.Action
import timber.log.Timber

class ActionExecutor(private val service: MimicAccessibilityService) {

    private var dragStartX: Float? = null
    private var dragStartY: Float? = null
    private var isDragging: Boolean = false

    // 진행 중인 dispatchGesture() 완료 상태 추적.
    // Android는 제스처 dispatch 중 실제 터치/마우스 이벤트를 차단할 수 있으므로
    // 콜백으로 완료 시점을 명시적으로 처리해야 한다.
    @Volatile private var isDispatchingGesture = false

    private val gestureCallback = object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
            isDispatchingGesture = false
        }
        override fun onCancelled(gestureDescription: GestureDescription) {
            isDispatchingGesture = false
            Timber.w("dispatchGesture cancelled by system")
        }
    }

    /**
     * 드래그 상태를 초기화하고 다음 제스처 실행을 허용합니다.
     * 앱 정지·일시정지·onInterrupt 등 서비스 전환 시 호출합니다.
     *
     * Android AccessibilityService는 진행 중인 dispatchGesture()를 조기 취소하는
     * 공식 API를 제공하지 않습니다. 제스처는 지정된 duration 후 자동으로 완료되며,
     * GestureResultCallback(onCompleted/onCancelled)이 호출되어 isDispatchingGesture가
     * 정상화됩니다. 이 메서드는 isDragging 등 앱 내부 상태만 초기화합니다.
     */
    fun cancelCurrentGesture() {
        isDispatchingGesture = false
        dragStartX = null
        dragStartY = null
        isDragging = false
    }

    /** MimicAccessibilityService의 onMotionEvent에서 표정 제스처 충돌 방지용 */
    fun isGestureDispatching(): Boolean = isDispatchingGesture

    fun execute(action: Action) {
        when (action) {
            is Action.GlobalHome ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            is Action.GlobalBack ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            is Action.GlobalRecents ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            is Action.GlobalNotifications ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            is Action.GlobalQuickSettings ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            is Action.ScreenLock -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            is Action.TakeScreenshot -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            is Action.PowerDialog ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            is Action.TapCenter -> {
                val (absX, absY) = relToAbs(0.5f, 0.5f)
                executeTap(absX, absY)
            }
            is Action.TapCustom -> {
                val (absX, absY) = relToAbs(action.x, action.y)
                executeTap(absX, absY)
            }
            is Action.DoubleTap -> {
                val (absX, absY) = relToAbs(action.x, action.y)
                executeDoubleTap(absX, absY)
            }
            is Action.LongPress -> {
                val (absX, absY) = relToAbs(action.x, action.y)
                executeLongPress(absX, absY)
            }
            is Action.SwipeUp -> executeSwipe(0.5f, 0.7f, 0.5f, 0.3f, action.duration)
            is Action.SwipeDown -> executeSwipe(0.5f, 0.3f, 0.5f, 0.7f, action.duration)
            is Action.SwipeLeft -> executeSwipe(0.7f, 0.5f, 0.3f, 0.5f, action.duration)
            is Action.SwipeRight -> executeSwipe(0.3f, 0.5f, 0.7f, 0.5f, action.duration)
            is Action.ScrollUp -> executeSwipe(0.5f, 0.3f, 0.5f, 0.7f, 300L)
            is Action.ScrollDown -> executeSwipe(0.5f, 0.7f, 0.5f, 0.3f, 300L)
            is Action.Drag -> executeSwipe(action.startX, action.startY, action.endX, action.endY, action.duration)
            is Action.PinchIn -> executePinch(isZoomIn = false)
            is Action.PinchOut -> executePinch(isZoomIn = true)
            is Action.OpenApp -> openApp(action.packageName)
            is Action.MediaPlayPause -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            is Action.MediaNext -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            is Action.MediaPrev -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            is Action.VolumeUp -> adjustVolume(AudioManager.ADJUST_RAISE)
            is Action.VolumeDown -> adjustVolume(AudioManager.ADJUST_LOWER)
            is Action.MimicPause -> {
                MimicAccessibilityService.instance?.globalToggleController?.handleBroadcastToggle()
            }
            is Action.RecenterCursor -> {
                MimicAccessibilityService.instance?.faceDetectionService?.recenterCursor()
            }
            // ── 커서 위치 액션 (CursorTracker 연동) ──
            is Action.TapAtCursor -> {
                service.cursorTracker.getCurrentPosition()?.let { (x, y) ->
                    executeTap(x, y)
                } ?: Timber.w("TapAtCursor: no cursor position available")
            }
            is Action.DoubleTapAtCursor -> {
                service.cursorTracker.getCurrentPosition()?.let { (x, y) ->
                    executeDoubleTap(x, y)
                } ?: Timber.w("DoubleTapAtCursor: no cursor position available")
            }
            is Action.LongPressAtCursor -> {
                service.cursorTracker.getCurrentPosition()?.let { (x, y) ->
                    executeLongPress(x, y)
                } ?: Timber.w("LongPressAtCursor: no cursor position available")
            }
            is Action.DragToggleAtCursor -> {
                if (!isDragging) {
                    // 드래그 시작: 현재 커서 위치 저장
                    service.cursorTracker.getCurrentPosition()?.let { (x, y) ->
                        dragStartX = x
                        dragStartY = y
                        isDragging = true
                        Timber.d("DragToggleAtCursor: drag START saved at ($x, $y)")
                    } ?: Timber.w("DragToggleAtCursor: no cursor position for drag start")
                } else {
                    // 드래그 종료: 저장된 시작 위치 → 현재 커서까지 스와이프
                    val sx = dragStartX
                    val sy = dragStartY
                    service.cursorTracker.getCurrentPosition()?.let { (ex, ey) ->
                        if (sx != null && sy != null) {
                            executeAbsoluteSwipe(sx, sy, ex, ey, 500L)
                            Timber.d("DragToggleAtCursor: drag END swipe ($sx,$sy) → ($ex,$ey)")
                        } else {
                            Timber.w("DragToggleAtCursor: drag start position missing")
                        }
                    } ?: Timber.w("DragToggleAtCursor: no cursor position for drag end")
                    // 상태 초기화 (성공/실패 무관)
                    dragStartX = null
                    dragStartY = null
                    isDragging = false
                }
            }
            // ── 스위치 제어 ──
            is Action.SwitchKey -> {
                Timber.d("SwitchKey: keyCode=${action.keyCode}, label=${action.label}")
                service.switchAccessBridge.injectKeyEvent(action.keyCode)
            }
            else -> {}
        }
    }

    private fun relToAbs(relX: Float, relY: Float): Pair<Float, Float> {
        val dm = service.resources.displayMetrics
        return relX * dm.widthPixels to relY * dm.heightPixels
    }

    private fun executeTap(x: Float, y: Float) {
        if (isDispatchingGesture) { Timber.d("tap skipped: gesture dispatching"); return }
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        isDispatchingGesture = true
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), gestureCallback, null
        )
    }

    private fun executeDoubleTap(x: Float, y: Float) {
        if (isDispatchingGesture) { Timber.d("doubleTap skipped: gesture dispatching"); return }
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0L, 50L)
        val stroke2 = GestureDescription.StrokeDescription(path, 150L, 50L)
        isDispatchingGesture = true
        service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(stroke1)
                .addStroke(stroke2)
                .build(),
            gestureCallback, null
        )
    }

    private fun executeLongPress(x: Float, y: Float) {
        if (isDispatchingGesture) { Timber.d("longPress skipped: gesture dispatching"); return }
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 800L)
        isDispatchingGesture = true
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), gestureCallback, null
        )
    }

    private fun executeSwipe(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long) {
        val (asx, asy) = relToAbs(sx, sy)
        val (aex, aey) = relToAbs(ex, ey)
        executeAbsoluteSwipe(asx, asy, aex, aey, durationMs)
    }

    private fun executeAbsoluteSwipe(asx: Float, asy: Float, aex: Float, aey: Float, durationMs: Long) {
        if (isDispatchingGesture) { Timber.d("swipe skipped: gesture dispatching"); return }
        val path = Path().apply { moveTo(asx, asy); lineTo(aex, aey) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        isDispatchingGesture = true
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), gestureCallback, null
        )
    }

    /**
     * Pinch gesture using two simultaneous strokes moving toward/away from center.
     * isZoomIn=false → pinch in (fingers toward center)
     * isZoomIn=true  → pinch out (fingers away from center)
     */
    private fun executePinch(isZoomIn: Boolean) {
        val dm = service.resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels / 2f
        val offset = dm.widthPixels * 0.2f
        val duration = 400L

        // Two finger paths: one from left to right of center and vice versa
        val (startOff, endOff) = if (isZoomIn) {
            Pair(offset, offset * 0.3f)   // fingers start far, end near center
        } else {
            Pair(offset * 0.3f, offset)   // fingers start near, end far from center
        }

        val path1 = Path().apply { moveTo(cx - startOff, cy); lineTo(cx - endOff, cy) }
        val path2 = Path().apply { moveTo(cx + startOff, cy); lineTo(cx + endOff, cy) }

        val stroke1 = GestureDescription.StrokeDescription(path1, 0L, duration)
        val stroke2 = GestureDescription.StrokeDescription(path2, 0L, duration)

        if (isDispatchingGesture) { Timber.d("pinch skipped: gesture dispatching"); return }
        isDispatchingGesture = true
        service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(stroke1)
                .addStroke(stroke2)
                .build(),
            gestureCallback, null
        )
    }

    private fun openApp(packageName: String) {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
        }
    }

    /**
     * Send media key via AudioManager. Works without root for media control.
     */
    private fun sendMediaKey(keyCode: Int) {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }
}
