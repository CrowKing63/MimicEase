package com.mimicease.data.model

import com.google.gson.Gson
import com.mimicease.domain.model.Action

object ActionSerializer {
    private val gson = Gson()

    fun serialize(action: Action): Pair<String, String> {
        val type = action::class.simpleName ?: "Unknown"
        val params = when (action) {
            is Action.TapCustom  -> gson.toJson(mapOf("x" to action.x, "y" to action.y))
            is Action.DoubleTap  -> gson.toJson(mapOf("x" to action.x, "y" to action.y))
            is Action.LongPress  -> gson.toJson(mapOf("x" to action.x, "y" to action.y))
            is Action.SwipeUp    -> gson.toJson(mapOf("duration" to action.duration))
            is Action.SwipeDown  -> gson.toJson(mapOf("duration" to action.duration))
            is Action.SwipeLeft  -> gson.toJson(mapOf("duration" to action.duration))
            is Action.SwipeRight -> gson.toJson(mapOf("duration" to action.duration))
            is Action.Drag       -> gson.toJson(mapOf(
                "startX" to action.startX, "startY" to action.startY,
                "endX" to action.endX, "endY" to action.endY, "duration" to action.duration
            ))
            is Action.OpenApp    -> gson.toJson(mapOf("packageName" to action.packageName))
            else                 -> "{}"
        }
        return type to params
    }

    fun deserialize(type: String, paramsJson: String): Action {
        val map = gson.fromJson(paramsJson, Map::class.java) ?: emptyMap<String, Any>()
        return when (type) {
            "GlobalHome"       -> Action.GlobalHome
            "GlobalBack"       -> Action.GlobalBack
            "GlobalRecents"    -> Action.GlobalRecents
            "GlobalNotifications" -> Action.GlobalNotifications
            "GlobalQuickSettings" -> Action.GlobalQuickSettings
            "ScreenLock"       -> Action.ScreenLock
            "TakeScreenshot"   -> Action.TakeScreenshot
            "PowerDialog"      -> Action.PowerDialog
            "TapCenter"        -> Action.TapCenter
            "TapCustom"        -> Action.TapCustom(
                x = (map["x"] as? Double)?.toFloat() ?: 0.5f,
                y = (map["y"] as? Double)?.toFloat() ?: 0.5f
            )
            "DoubleTap"        -> Action.DoubleTap(
                x = (map["x"] as? Double)?.toFloat() ?: 0.5f,
                y = (map["y"] as? Double)?.toFloat() ?: 0.5f
            )
            "LongPress"        -> Action.LongPress(
                x = (map["x"] as? Double)?.toFloat() ?: 0.5f,
                y = (map["y"] as? Double)?.toFloat() ?: 0.5f
            )
            "SwipeUp"          -> Action.SwipeUp((map["duration"] as? Double)?.toLong() ?: 300L)
            "SwipeDown"        -> Action.SwipeDown((map["duration"] as? Double)?.toLong() ?: 300L)
            "SwipeLeft"        -> Action.SwipeLeft((map["duration"] as? Double)?.toLong() ?: 300L)
            "SwipeRight"       -> Action.SwipeRight((map["duration"] as? Double)?.toLong() ?: 300L)
            "ScrollUp"         -> Action.ScrollUp
            "ScrollDown"       -> Action.ScrollDown
            "Drag"             -> Action.Drag(
                startX = (map["startX"] as? Double)?.toFloat() ?: 0f,
                startY = (map["startY"] as? Double)?.toFloat() ?: 0f,
                endX   = (map["endX"] as? Double)?.toFloat() ?: 0f,
                endY   = (map["endY"] as? Double)?.toFloat() ?: 0f,
                duration = (map["duration"] as? Double)?.toLong() ?: 500L
            )
            "PinchIn"          -> Action.PinchIn
            "PinchOut"         -> Action.PinchOut
            "OpenApp"          -> Action.OpenApp((map["packageName"] as? String) ?: "")
            "MediaPlayPause"   -> Action.MediaPlayPause
            "MediaNext"        -> Action.MediaNext
            "MediaPrev"        -> Action.MediaPrev
            "VolumeUp"         -> Action.VolumeUp
            "VolumeDown"       -> Action.VolumeDown
            "MimicPause"       -> Action.MimicPause
            else               -> Action.GlobalHome  // 알 수 없는 타입 폴백
        }
    }
}
