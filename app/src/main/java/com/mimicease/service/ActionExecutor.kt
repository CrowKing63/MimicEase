package com.mimicease.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.KeyEvent
import android.content.Intent
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
            is Action.ScreenLock ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            is Action.TakeScreenshot ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
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
            is Action.PinchIn -> {} // TODO: implement pinch gesture if needed
            is Action.PinchOut -> {} // TODO: implement pinch gesture if needed
            is Action.OpenApp -> openApp(action.packageName)
            is Action.MediaPlayPause -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            is Action.MediaNext -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            is Action.MediaPrev -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            is Action.VolumeUp -> sendMediaKey(KeyEvent.KEYCODE_VOLUME_UP)
            is Action.VolumeDown -> sendMediaKey(KeyEvent.KEYCODE_VOLUME_DOWN)
            is Action.MimicPause -> {} // Handled elsewhere or implement intent
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

    private fun openApp(packageName: String) {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        // AccessibilityService does not have direct media key injection usually without root,
        // but we can try using AudioManager or media sessions later.
        // For now, this is a placeholder.
    }
}
