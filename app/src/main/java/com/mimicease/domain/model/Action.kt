package com.mimicease.domain.model

sealed class Action {

    // ── 시스템 액션 ──────────────────────────────
    object GlobalHome : Action()
    object GlobalBack : Action()
    object GlobalRecents : Action()
    object GlobalNotifications : Action()
    object GlobalQuickSettings : Action()
    object ScreenLock : Action()
    object TakeScreenshot : Action()
    object PowerDialog : Action()

    // ── 탭/클릭 ──────────────────────────────────
    object TapCenter : Action()
    data class TapCustom(val x: Float, val y: Float) : Action()
    data class DoubleTap(val x: Float = 0.5f, val y: Float = 0.5f) : Action()
    data class LongPress(val x: Float = 0.5f, val y: Float = 0.5f) : Action()

    // ── 스와이프 ──────────────────────────────────
    data class SwipeUp(val duration: Long = 300L) : Action()
    data class SwipeDown(val duration: Long = 300L) : Action()
    data class SwipeLeft(val duration: Long = 300L) : Action()
    data class SwipeRight(val duration: Long = 300L) : Action()

    // ── 스크롤 ──────────────────────────────────
    object ScrollUp : Action()
    object ScrollDown : Action()

    // ── 드래그 / 핀치 ────────────────────────────
    data class Drag(
        val startX: Float, val startY: Float,
        val endX: Float, val endY: Float,
        val duration: Long = 500L
    ) : Action()
    object PinchIn : Action()
    object PinchOut : Action()

    // ── 앱 ──────────────────────────────────────
    data class OpenApp(val packageName: String) : Action()

    // ── 미디어 / 볼륨 ────────────────────────────
    object MediaPlayPause : Action()
    object MediaNext : Action()
    object MediaPrev : Action()
    object VolumeUp : Action()
    object VolumeDown : Action()

    // ── 커서 위치 액션 (CURSOR_CLICK / HEAD_MOUSE 모드) ──
    object TapAtCursor : Action()
    object DoubleTapAtCursor : Action()
    object LongPressAtCursor : Action()
    object DragStartAtCursor : Action()
    object DragEndAtCursor : Action()

    // ── 스위치 제어 (모든 모드에서 사용 가능) ──────────
    data class SwitchKey(val keyCode: Int, val label: String = "") : Action()

    // ── MimicEase 내부 ───────────────────────────
    object MimicPause : Action()
}
