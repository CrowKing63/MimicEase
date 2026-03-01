package com.mimicease.service

import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger

class TriggerMatcher(
    private val triggers: List<Trigger>,
    private val globalCooldownMs: Int
) {
    private val lastFiredTime = mutableMapOf<String, Long>()  // triggerId → timestamp
    private var lastAnyFiredTime = 0L
    private val holdStartTime = mutableMapOf<String, Long>()  // triggerId → hold 시작 시각

    fun match(smoothedValues: Map<String, Float>): List<Action> {
        val now = System.currentTimeMillis()
        val actions = mutableListOf<Action>()

        // 1. 전역 쿨다운 체크
        if (now - lastAnyFiredTime < globalCooldownMs) return emptyList()

        // 2. 활성화된 트리거만 우선순위 순으로 처리
        triggers.filter { it.isEnabled }
            .sortedBy { it.priority }
            .forEach { trigger ->
                val value = smoothedValues[trigger.blendShape] ?: 0f

                if (value >= trigger.threshold) {
                    // 홀드 시간 추적
                    val holdStart = holdStartTime.getOrPut(trigger.id) { now }
                    val holdElapsed = now - holdStart

                    // 개별 쿨다운 체크 + 홀드 시간 충족 시 발동
                    val lastFired = lastFiredTime[trigger.id] ?: 0L
                    if (holdElapsed >= trigger.holdDurationMs &&
                        now - lastFired >= trigger.cooldownMs) {

                        actions.add(trigger.action)
                        lastFiredTime[trigger.id] = now
                        lastAnyFiredTime = now
                        holdStartTime.remove(trigger.id)
                    }
                } else {
                    holdStartTime.remove(trigger.id)  // 표정 해제 시 홀드 리셋
                }
            }

        return actions
    }
}
