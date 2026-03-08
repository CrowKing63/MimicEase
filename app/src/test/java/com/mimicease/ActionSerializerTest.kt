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
            Action.VolumeUp, Action.VolumeDown, Action.MimicPause,
            Action.TapAtCursor, Action.DoubleTapAtCursor, Action.LongPressAtCursor,
            Action.DragToggleAtCursor
        )
        noParamActions.forEach { action ->
            val result = roundTrip(action)
            assertEquals("${action::class.simpleName} 타입 불일치", action, result)
        }
    }

    @Test
    fun `DragToggleAtCursor 직렬화 후 역직렬화`() {
        assertEquals(Action.DragToggleAtCursor, roundTrip(Action.DragToggleAtCursor))
    }

    @Test
    fun `레거시 DragStartAtCursor → DragToggleAtCursor 마이그레이션`() {
        val result = ActionSerializer.deserialize("DragStartAtCursor", "{}")
        assertEquals(Action.DragToggleAtCursor, result)
    }

    @Test
    fun `레거시 DragEndAtCursor → DragToggleAtCursor 마이그레이션`() {
        val result = ActionSerializer.deserialize("DragEndAtCursor", "{}")
        assertEquals(Action.DragToggleAtCursor, result)
    }

    @Test
    fun `NoOp 직렬화 후 역직렬화`() {
        assertEquals(Action.NoOp, roundTrip(Action.NoOp))
    }

    @Test
    fun `알 수 없는 액션 타입 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("UnknownActionType", "{}")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `손상된 JSON - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("TapCustom", "{not-valid-json")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `TapCustom 좌표 누락 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("TapCustom", "{}")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `TapCustom 좌표 범위 초과 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("TapCustom", """{"x": 1.5, "y": 0.5}""")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `SwipeUp 잘못된 duration - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("SwipeUp", """{"duration": "oops"}""")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `Drag 파라미터 일부 누락 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("Drag", """{"startX": 0.1}""")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `SwitchKey 잘못된 keyCode - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("SwitchKey", """{"keyCode": 0, "label": "invalid"}""")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `손상된 액션 타입 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("CorruptedAction123", "{}")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `빈 액션 타입 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("", "{}")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `알 수 없는 타입이지만 파라미터가 있는 경우 - NoOp으로 안전 폴백`() {
        val result = ActionSerializer.deserialize("FutureAction", """{"x": 0.5, "y": 0.5}""")
        assertEquals(Action.NoOp, result)
    }

    @Test
    fun `레거시 마이그레이션은 여전히 동작 - DragStartAtCursor`() {
        val result = ActionSerializer.deserialize("DragStartAtCursor", "{}")
        assertEquals(Action.DragToggleAtCursor, result)
    }

    @Test
    fun `레거시 마이그레이션은 여전히 동작 - DragEndAtCursor`() {
        val result = ActionSerializer.deserialize("DragEndAtCursor", "{}")
        assertEquals(Action.DragToggleAtCursor, result)
    }
}
