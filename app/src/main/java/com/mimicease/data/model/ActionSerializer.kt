package com.mimicease.data.model

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
            is Action.Drag       -> gson.toJson(
                mapOf(
                    "startX" to action.startX,
                    "startY" to action.startY,
                    "endX" to action.endX,
                    "endY" to action.endY,
                    "duration" to action.duration
                )
            )
            is Action.OpenApp    -> gson.toJson(mapOf("packageName" to action.packageName))
            is Action.SwitchKey  -> gson.toJson(mapOf("keyCode" to action.keyCode, "label" to action.label))
            is Action.NoOp       -> "{}"
            else                 -> "{}"
        }
        return type to params
    }

    fun deserialize(type: String, paramsJson: String): Action {
        val params: Map<String, Any?> = try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(paramsJson, Map::class.java) as? Map<String, Any?>
                ?: emptyMap()
        } catch (_: JsonSyntaxException) {
            return Action.NoOp
        } catch (_: Exception) {
            return Action.NoOp
        }

        return when (type) {
            "GlobalHome"          -> Action.GlobalHome
            "GlobalBack"          -> Action.GlobalBack
            "GlobalRecents"       -> Action.GlobalRecents
            "GlobalNotifications" -> Action.GlobalNotifications
            "GlobalQuickSettings" -> Action.GlobalQuickSettings
            "ScreenLock"          -> Action.ScreenLock
            "TakeScreenshot"      -> Action.TakeScreenshot
            "PowerDialog"         -> Action.PowerDialog
            "TapCenter"           -> Action.TapCenter

            "TapCustom" -> {
                val x = params.getNormalizedFloat("x") ?: return Action.NoOp
                val y = params.getNormalizedFloat("y") ?: return Action.NoOp
                Action.TapCustom(x = x, y = y)
            }

            "DoubleTap" -> {
                val x = params.getNormalizedFloat("x") ?: return Action.NoOp
                val y = params.getNormalizedFloat("y") ?: return Action.NoOp
                Action.DoubleTap(x = x, y = y)
            }

            "LongPress" -> {
                val x = params.getNormalizedFloat("x") ?: return Action.NoOp
                val y = params.getNormalizedFloat("y") ?: return Action.NoOp
                Action.LongPress(x = x, y = y)
            }

            "SwipeUp" -> {
                val duration = params.getDuration("duration") ?: return Action.NoOp
                Action.SwipeUp(duration)
            }

            "SwipeDown" -> {
                val duration = params.getDuration("duration") ?: return Action.NoOp
                Action.SwipeDown(duration)
            }

            "SwipeLeft" -> {
                val duration = params.getDuration("duration") ?: return Action.NoOp
                Action.SwipeLeft(duration)
            }

            "SwipeRight" -> {
                val duration = params.getDuration("duration") ?: return Action.NoOp
                Action.SwipeRight(duration)
            }

            "ScrollUp"   -> Action.ScrollUp
            "ScrollDown" -> Action.ScrollDown

            "Drag" -> {
                val startX = params.getNormalizedFloat("startX") ?: return Action.NoOp
                val startY = params.getNormalizedFloat("startY") ?: return Action.NoOp
                val endX = params.getNormalizedFloat("endX") ?: return Action.NoOp
                val endY = params.getNormalizedFloat("endY") ?: return Action.NoOp
                val duration = params.getDuration("duration") ?: return Action.NoOp
                Action.Drag(
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY,
                    duration = duration
                )
            }

            "PinchIn"  -> Action.PinchIn
            "PinchOut" -> Action.PinchOut

            "OpenApp" -> {
                val packageName = params["packageName"] as? String ?: return Action.NoOp
                if (packageName.isBlank()) return Action.NoOp
                Action.OpenApp(packageName)
            }

            "MediaPlayPause" -> Action.MediaPlayPause
            "MediaNext"      -> Action.MediaNext
            "MediaPrev"      -> Action.MediaPrev
            "VolumeUp"       -> Action.VolumeUp
            "VolumeDown"     -> Action.VolumeDown
            "MimicPause"     -> Action.MimicPause

            "TapAtCursor"        -> Action.TapAtCursor
            "DoubleTapAtCursor"  -> Action.DoubleTapAtCursor
            "LongPressAtCursor"  -> Action.LongPressAtCursor
            "DragToggleAtCursor" -> Action.DragToggleAtCursor
            "RecenterCursor"     -> Action.RecenterCursor

            // 하위 호환: 구 타입 → DragToggleAtCursor로 자동 마이그레이션
            "DragStartAtCursor",
            "DragEndAtCursor" -> Action.DragToggleAtCursor

            "SwitchKey" -> {
                val keyCode = (params["keyCode"] as? Number)?.toInt() ?: return Action.NoOp
                if (keyCode <= 0) return Action.NoOp
                val label = params["label"] as? String ?: ""
                Action.SwitchKey(
                    keyCode = keyCode,
                    label = label
                )
            }

            "NoOp" -> Action.NoOp

            else -> Action.NoOp  // 알 수 없는 타입 안전 폴백 (액션 실행 안 함)
        }
    }

    private fun Map<String, Any?>.getNormalizedFloat(key: String): Float? {
        val raw = this[key] ?: return null
        val number = (raw as? Number)?.toFloat() ?: return null
        return if (number in 0f..1f) number else null
    }

    private fun Map<String, Any?>.getDuration(
        key: String,
        minMs: Long = 1L,
        maxMs: Long = 10_000L
    ): Long? {
        val raw = this[key] ?: return null
        val value = (raw as? Number)?.toLong() ?: return null
        return if (value in minMs..maxMs) value else null
    }
}
