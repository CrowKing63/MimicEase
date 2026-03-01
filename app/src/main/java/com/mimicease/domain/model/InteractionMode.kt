package com.mimicease.domain.model

/**
 * 앱의 상호작용 모드를 정의합니다.
 * 각 모드는 커서 제어 방식과 허용 액션 범위가 다릅니다.
 *
 * - EXPRESSION_ONLY: 기존 모드. 표정 → 고정좌표 제스처/시스템 액션.
 * - CURSOR_CLICK: BT 마우스로 커서를 이동하고 표정으로 클릭. 기존 고정좌표 액션도 병행.
 * - HEAD_MOUSE: 머리 움직임으로 오버레이 커서 제어 + 표정/드웰 클릭.
 *              고정좌표 탭 액션은 차단 (커서 기반만 허용).
 */
enum class InteractionMode {
    EXPRESSION_ONLY,
    CURSOR_CLICK,
    HEAD_MOUSE;

    companion object {
        fun fromString(value: String): InteractionMode {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                EXPRESSION_ONLY
            }
        }
    }
}
