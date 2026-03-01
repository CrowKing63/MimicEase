package com.mimicease.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * Android Switch Access와 MimicEase 표정 인식을 연결하는 브리지.
 *
 * Switch Access는 물리적 스위치(버튼)로 화면 항목을 순차 스캔하고 선택하는 접근성 기능입니다.
 * SwitchAccessBridge는 표정(Action.SwitchKey)을 접근성 동작으로 변환하여 실행합니다.
 *
 * 지원 키 코드:
 *  - KEYCODE_TAB / KEYCODE_DPAD_RIGHT → 다음 항목으로 포커스 이동
 *  - KEYCODE_DPAD_LEFT               → 이전 항목으로 포커스 이동
 *  - KEYCODE_ENTER / KEYCODE_SPACE / KEYCODE_DPAD_CENTER → 현재 포커스 클릭
 *  - KEYCODE_DPAD_UP                 → 위로 스크롤
 *  - KEYCODE_DPAD_DOWN               → 아래로 스크롤
 *  - KEYCODE_DEL                     → 현재 포커스 삭제 액션
 */
class SwitchAccessBridge(private val service: AccessibilityService) {

    companion object {
        /** Switch Access에서 일반적으로 사용하는 키 코드 목록 (UI 표시 및 Action.SwitchKey 생성용) */
        val SUPPORTED_SWITCH_KEYS = listOf(
            SwitchKeyInfo(KeyEvent.KEYCODE_DPAD_RIGHT,  "다음 항목 (→)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_DPAD_LEFT,   "이전 항목 (←)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_ENTER,       "선택/클릭 (Enter)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_DPAD_CENTER, "선택/클릭 (D-패드 중앙)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_TAB,         "다음 항목 (Tab)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_SPACE,       "선택/클릭 (스페이스)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_DPAD_UP,     "위로 스크롤 (↑)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_DPAD_DOWN,   "아래로 스크롤 (↓)"),
            SwitchKeyInfo(KeyEvent.KEYCODE_DEL,         "삭제 (Backspace)"),
        )
    }

    data class SwitchKeyInfo(val keyCode: Int, val label: String)

    /**
     * SwitchKey 액션을 받아 해당 접근성 동작을 실행합니다.
     */
    fun injectKeyEvent(keyCode: Int) {
        Timber.d("SwitchAccessBridge: injecting keyCode=$keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_TAB         -> moveFocus(forward = true)
            KeyEvent.KEYCODE_DPAD_LEFT   -> moveFocus(forward = false)
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_DPAD_CENTER -> clickFocusedNode()
            KeyEvent.KEYCODE_DPAD_UP     -> scrollFocusedOrScreen(scrollUp = true)
            KeyEvent.KEYCODE_DPAD_DOWN   -> scrollFocusedOrScreen(scrollUp = false)
            KeyEvent.KEYCODE_DEL         -> deleteAtFocusedNode()
            else -> Timber.w("SwitchAccessBridge: unsupported keyCode=$keyCode")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 포커스 이동
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 현재 접근성 포커스에서 앞/뒤 항목으로 순환 이동합니다.
     *
     * 방식: rootInActiveWindow에서 포커스 가능/클릭 가능한 노드를 DFS 순서로 수집 →
     * 현재 포커스 인덱스를 화면 좌표로 비교 → 다음/이전 노드에 ACCESSIBILITY_FOCUS 설정.
     */
    private fun moveFocus(forward: Boolean) {
        val root = service.rootInActiveWindow ?: run {
            Timber.w("SwitchAccessBridge: rootInActiveWindow is null")
            return
        }

        val currentFocus = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        val nodes = collectFocusableNodes(root)
        root.recycle()

        if (nodes.isEmpty()) {
            currentFocus?.recycle()
            return
        }

        val currentBounds = currentFocus?.boundsInScreen()
        val currentIndex = if (currentBounds != null) {
            nodes.indexOfFirst { it.boundsInScreen() == currentBounds }
        } else {
            -1
        }
        currentFocus?.recycle()

        val nextIndex = when {
            currentIndex == -1 -> if (forward) 0 else nodes.size - 1
            forward            -> (currentIndex + 1) % nodes.size
            else               -> (currentIndex - 1 + nodes.size) % nodes.size
        }

        val success = nodes[nextIndex].performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        Timber.d("SwitchAccessBridge: moveFocus forward=$forward index=$nextIndex/${nodes.size} success=$success")
        nodes.forEach { it.recycle() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 클릭
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 현재 접근성 포커스 노드를 클릭합니다.
     * 노드 자체가 클릭 불가능하면 클릭 가능한 조상 노드를 탐색합니다.
     */
    private fun clickFocusedNode() {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused == null) {
            Timber.w("SwitchAccessBridge: no focused node to click")
            return
        }

        val clickTarget = if (focused.isClickable) {
            focused
        } else {
            findClickableAncestor(focused) ?: focused
        }

        val success = clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Timber.d("SwitchAccessBridge: click success=$success")
        if (clickTarget !== focused) clickTarget.recycle()
        focused.recycle()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 스크롤
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 포커스된 노드 주변의 스크롤 가능 컨테이너를 스크롤합니다.
     * 포커스가 없으면 화면 전체에서 첫 번째 스크롤 가능 노드를 찾습니다.
     */
    private fun scrollFocusedOrScreen(scrollUp: Boolean) {
        val action = if (scrollUp)
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        else
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD

        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        val scrollable = if (focused != null) {
            findScrollableAncestor(focused).also { focused.recycle() }
        } else {
            null
        }

        if (scrollable != null) {
            scrollable.performAction(action)
            scrollable.recycle()
            return
        }

        val root = service.rootInActiveWindow ?: return
        val firstScrollable = findFirstScrollableNode(root)
        root.recycle()
        firstScrollable?.let {
            it.performAction(action)
            it.recycle()
        } ?: Timber.w("SwitchAccessBridge: no scrollable node found")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 삭제
    // ─────────────────────────────────────────────────────────────────────────

    private fun deleteAtFocusedNode() {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        focused?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CUT)
            it.recycle()
        } ?: Timber.w("SwitchAccessBridge: no focused node for delete")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 루트부터 DFS로 접근성 포커스 가능하거나 클릭 가능한 노드를 화면 순서대로 수집합니다.
     * Switch Access의 순환 탐색 대상과 동일한 조건입니다.
     */
    private fun collectFocusableNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(node: AccessibilityNodeInfo) {
            if ((node.isFocusable || node.isClickable) && node.isVisibleToUser) {
                result.add(AccessibilityNodeInfo.obtain(node))
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child)
                child.recycle()
            }
        }
        traverse(root)
        return result
    }

    private fun AccessibilityNodeInfo.boundsInScreen(): Rect {
        val rect = Rect()
        getBoundsInScreen(rect)
        return rect
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent
            val next = parent.parent
            parent.recycle()
            parent = next
        }
        return null
    }

    private fun findScrollableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        while (parent != null) {
            if (parent.isScrollable) return parent
            val next = parent.parent
            parent.recycle()
            parent = next
        }
        return null
    }

    private fun findFirstScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return AccessibilityNodeInfo.obtain(root)
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFirstScrollableNode(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }
}
