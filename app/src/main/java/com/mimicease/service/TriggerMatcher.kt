package com.mimicease.service

import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger

class TriggerMatcher(
    triggers: List<Trigger>,
    private val globalCooldownMs: Int,
    requiredFrames: Int = 1
) {
    private val requiredFrames = requiredFrames.coerceIn(1, 10)
    // 생성 시점에 1회 필터링+정렬 — match() 호출마다 재정렬하지 않음
    private val sortedTriggers = triggers
        .filter { it.isEnabled && it.action !is Action.NoOp }
        .sortedBy { it.priority }
    private val activeFrameCount = mutableMapOf<String, Int>()
    private val lastFiredTime = mutableMapOf<String, Long>()
    private var lastAnyFiredTime = 0L
    private val holdStartTime = mutableMapOf<String, Long>()
    // 매 호출마다 새 List 할당 방지 — 액션이 발동될 때만 toList() 복사
    private val resultBuffer = mutableListOf<Action>()

    fun match(smoothedValues: Map<String, Float>): List<Action> {
        val now = System.currentTimeMillis()
        resultBuffer.clear()

        if (now - lastAnyFiredTime < globalCooldownMs) return emptyList()

        sortedTriggers.forEach { trigger ->
                val value = smoothedValues[trigger.blendShape] ?: 0f

                if (value >= trigger.threshold) {
                    val frameCount = (activeFrameCount[trigger.id] ?: 0) + 1
                    activeFrameCount[trigger.id] = frameCount

                    if (frameCount < requiredFrames) {
                        return@forEach
                    }

                    val holdStart = holdStartTime.getOrPut(trigger.id) { now }
                    val holdElapsed = now - holdStart
                    val lastFired = lastFiredTime[trigger.id] ?: 0L

                    if (holdElapsed >= trigger.holdDurationMs &&
                        now - lastFired >= trigger.cooldownMs
                    ) {
                        resultBuffer.add(trigger.action)
                        lastFiredTime[trigger.id] = now
                        lastAnyFiredTime = now
                        holdStartTime.remove(trigger.id)
                    }
                } else {
                    activeFrameCount.remove(trigger.id)
                    holdStartTime.remove(trigger.id)
                }
            }

        return if (resultBuffer.isEmpty()) emptyList() else resultBuffer.toList()
    }
}
