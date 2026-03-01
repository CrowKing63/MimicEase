package com.mimicease.service

import android.graphics.Rect
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * BT 마우스(또는 터치스크린 포인터)의 커서 위치를 추적합니다.
 *
 * 추적 방식 (우선순위순):
 * 1. TYPE_VIEW_HOVER_ENTER 이벤트 — BT 마우스 호버 시 발생
 * 2. 포커스된 AccessibilityNodeInfo의 화면 좌표 (폴백)
 *
 * CURSOR_CLICK 모드에서 사용됩니다.
 * 사용자가 휠체어 조이스틱 BT 마우스로 커서를 이동하면,
 * 표정으로 현재 커서 위치에 클릭을 실행합니다.
 */
class CursorTracker {

    @Volatile
    private var cursorX: Float = -1f

    @Volatile
    private var cursorY: Float = -1f

    private var lastUpdateTimeMs: Long = 0L

    /**
     * 접근성 이벤트에서 커서 위치를 추출합니다.
     * TYPE_VIEW_HOVER_ENTER: BT 마우스가 View 위를 지날 때 발생
     * TYPE_VIEW_FOCUSED: 포커스 이동 시 해당 뷰의 중앙 좌표를 사용
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,
            AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> {
                extractPositionFromNode(event.source)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // 호버 이벤트보다 낮은 우선순위 — 최근 호버가 있으면 무시
                val now = System.currentTimeMillis()
                if (now - lastUpdateTimeMs > 500) {
                    extractPositionFromNode(event.source)
                }
            }
        }
    }

    /**
     * AccessibilityNodeInfo에서 뷰의 화면 중심 좌표를 추출합니다.
     */
    private fun extractPositionFromNode(node: AccessibilityNodeInfo?) {
        node ?: return
        try {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                cursorX = rect.exactCenterX()
                cursorY = rect.exactCenterY()
                lastUpdateTimeMs = System.currentTimeMillis()
                Timber.v("CursorTracker: position updated to ($cursorX, $cursorY)")
            }
        } catch (e: Exception) {
            Timber.w(e, "CursorTracker: failed to extract position")
        } finally {
            node.recycle()
        }
    }

    /**
     * 현재 커서 위치를 반환합니다.
     * @return (x, y) 절대 픽셀 좌표, 또는 위치 불명 시 null
     */
    fun getCurrentPosition(): Pair<Float, Float>? {
        return if (cursorX >= 0f && cursorY >= 0f) {
            cursorX to cursorY
        } else {
            null
        }
    }

    /**
     * 추적 상태를 초기화합니다.
     */
    fun reset() {
        cursorX = -1f
        cursorY = -1f
        lastUpdateTimeMs = 0L
    }
}
