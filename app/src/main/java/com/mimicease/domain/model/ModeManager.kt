package com.mimicease.domain.model

/**
 * 모드별 Action 허용/차단 필터링을 담당합니다.
 *
 * 안전 원칙:
 * - HEAD_MOUSE 모드에서 고정좌표 탭 액션을 차단하여 오발동 방지
 * - 시스템 액션, 미디어, MimicPause 등은 모든 모드에서 허용
 * - Switch Access (SwitchKey)는 모드와 무관하게 항상 허용
 */
object ModeManager {

    /**
     * HEAD_MOUSE 모드에서 차단되는 Action 타입.
     * 커서 기반 클릭이 활성화되므로 고정좌표 탭은 혼란을 야기할 수 있습니다.
     */
    private val HEAD_MOUSE_BLOCKED_TYPES: Set<String> = setOf(
        "TapCenter",
        "TapCustom",
        "DoubleTap",
        "LongPress"
    )

    /**
     * 현재 모드에서 해당 Action의 실행이 허용되는지 판단합니다.
     *
     * @return true이면 실행 가능, false이면 차단
     */
    fun isActionAllowed(mode: InteractionMode, action: Action): Boolean {
        return when (mode) {
            InteractionMode.EXPRESSION_ONLY -> {
                // 기존 모드: 커서 기반 액션 차단
                action !is Action.TapAtCursor &&
                action !is Action.DoubleTapAtCursor &&
                action !is Action.LongPressAtCursor &&
                action !is Action.DragStartAtCursor &&
                action !is Action.DragEndAtCursor
            }
            InteractionMode.CURSOR_CLICK -> {
                // 커서 클릭 모드: 모든 액션 허용 (기존 + 커서 기반 병행)
                true
            }
            InteractionMode.HEAD_MOUSE -> {
                // 헤드 마우스 모드: 고정좌표 탭 차단, 커서 기반만 허용
                val actionType = action::class.simpleName ?: ""
                actionType !in HEAD_MOUSE_BLOCKED_TYPES
            }
        }
    }

    /**
     * 모드에서 사용 가능한 Action 카테고리 목록 (UI 표시용)
     */
    fun getAvailableActionCategories(mode: InteractionMode): List<ActionCategory> {
        val base = listOf(
            ActionCategory.SYSTEM,
            ActionCategory.MEDIA,
            ActionCategory.APP,
            ActionCategory.SWITCH
        )
        return when (mode) {
            InteractionMode.EXPRESSION_ONLY -> base + ActionCategory.GESTURE
            InteractionMode.CURSOR_CLICK -> base + ActionCategory.GESTURE + ActionCategory.CURSOR
            InteractionMode.HEAD_MOUSE -> base + ActionCategory.CURSOR
        }
    }
}

/**
 * Action 카테고리 (UI 탭 분류용)
 */
enum class ActionCategory {
    SYSTEM,      // 시스템 액션 (홈, 뒤로가기 등)
    GESTURE,     // 고정좌표 제스처 (탭, 스와이프 등)
    CURSOR,      // 커서 위치 기반 액션
    MEDIA,       // 미디어 제어
    APP,         // 앱 실행
    SWITCH       // 스위치 제어
}
