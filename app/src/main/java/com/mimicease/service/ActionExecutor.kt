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

class ActionExecutor(private val service: MimicAccessibilityService) {

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
                MimicAccessibilityService.instance?.let { svc ->
                    svc.faceDetectionService?.togglePause()
                }
            }
            else -> {}
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

    private fun executeDoubleTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0L, 50L)
        val stroke2 = GestureDescription.StrokeDescription(path, 150L, 50L)
        service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(stroke1)
                .addStroke(stroke2)
                .build(),
            null, null
        )
    }

    private fun executeLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 800L)
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

        service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(stroke1)
                .addStroke(stroke2)
                .build(),
            null, null
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
