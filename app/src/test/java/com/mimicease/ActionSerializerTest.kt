package com.mimicease

import com.mimicease.data.model.ActionSerializer
import com.mimicease.domain.model.Action
import org.junit.Assert.*
import org.junit.Test

class ActionSerializerTest {

    private fun roundTrip(action: Action): Action {
        val (type, params) = ActionSerializer.serialize(action)
        return ActionSerializer.deserialize(type, params)
    }

    @Test
    fun `GlobalHome 직렬화 후 역직렬화`() {
        assertEquals(Action.GlobalHome, roundTrip(Action.GlobalHome))
    }

    @Test
    fun `GlobalBack 직렬화 후 역직렬화`() {
        assertEquals(Action.GlobalBack, roundTrip(Action.GlobalBack))
    }

    @Test
    fun `GlobalRecents 직렬화 후 역직렬화`() {
        assertEquals(Action.GlobalRecents, roundTrip(Action.GlobalRecents))
    }

    @Test
    fun `TapCustom 좌표 보존`() {
        val action = Action.TapCustom(0.3f, 0.7f)
        val result = roundTrip(action) as Action.TapCustom
        assertEquals(0.3f, result.x, 0.001f)
        assertEquals(0.7f, result.y, 0.001f)
    }

    @Test
    fun `SwipeUp duration 보존`() {
        val action = Action.SwipeUp(500L)
        val result = roundTrip(action) as Action.SwipeUp
        assertEquals(500L, result.duration)
    }

    @Test
    fun `Drag 모든 파라미터 보존`() {
        val action = Action.Drag(0.2f, 0.5f, 0.8f, 0.5f, 600L)
        val result = roundTrip(action) as Action.Drag
        assertEquals(0.2f, result.startX, 0.001f)
        assertEquals(0.5f, result.startY, 0.001f)
        assertEquals(0.8f, result.endX, 0.001f)
        assertEquals(0.5f, result.endY, 0.001f)
        assertEquals(600L, result.duration)
    }

    @Test
    fun `OpenApp packageName 보존`() {
        val action = Action.OpenApp("com.android.chrome")
        val result = roundTrip(action) as Action.OpenApp
        assertEquals("com.android.chrome", result.packageName)
    }

    @Test
    fun `PinchIn 직렬화 후 역직렬화`() {
        assertEquals(Action.PinchIn, roundTrip(Action.PinchIn))
    }

    @Test
    fun `MediaPlayPause 직렬화 후 역직렬화`() {
        assertEquals(Action.MediaPlayPause, roundTrip(Action.MediaPlayPause))
    }

    @Test
    fun `VolumeUp 직렬화 후 역직렬화`() {
        assertEquals(Action.VolumeUp, roundTrip(Action.VolumeUp))
    }

    @Test
    fun `MimicPause 직렬화 후 역직렬화`() {
        assertEquals(Action.MimicPause, roundTrip(Action.MimicPause))
    }

    @Test
    fun `모든 파라미터 없는 액션 타입 직렬화`() {
        val noParamActions = listOf(
            Action.GlobalHome, Action.GlobalBack, Action.GlobalRecents,
            Action.GlobalNotifications, Action.GlobalQuickSettings,
            Action.ScreenLock, Action.TakeScreenshot, Action.PowerDialog,
            Action.TapCenter, Action.ScrollUp, Action.ScrollDown,
            Action.PinchIn, Action.PinchOut,
            Action.MediaPlayPause, Action.MediaNext, Action.MediaPrev,
            Action.VolumeUp, Action.VolumeDown, Action.MimicPause
        )
        noParamActions.forEach { action ->
            val result = roundTrip(action)
            assertEquals("${action::class.simpleName} 타입 불일치", action, result)
        }
    }
}
