package com.mimicease.service

import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger

class TriggerMatcher(
    private val triggers: List<Trigger>,
    private val globalCooldownMs: Int,
    requiredFrames: Int = 1
) {
    private val requiredFrames = requiredFrames.coerceIn(1, 10)
    private val activeFrameCount = mutableMapOf<String, Int>()
    private val lastFiredTime = mutableMapOf<String, Long>()
    private var lastAnyFiredTime = 0L
    private val holdStartTime = mutableMapOf<String, Long>()

    fun match(smoothedValues: Map<String, Float>): List<Action> {
        val now = System.currentTimeMillis()
        val actions = mutableListOf<Action>()

        if (now - lastAnyFiredTime < globalCooldownMs) return emptyList()

        triggers
            .filter { it.isEnabled && it.action !is Action.NoOp }
            .sortedBy { it.priority }
            .forEach { trigger ->
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
                        actions.add(trigger.action)
                        lastFiredTime[trigger.id] = now
                        lastAnyFiredTime = now
                        holdStartTime.remove(trigger.id)
                    }
                } else {
                    activeFrameCount.remove(trigger.id)
                    holdStartTime.remove(trigger.id)
                }
            }

        return actions
    }
}
