package com.mimicease

import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger
import com.mimicease.service.TriggerMatcher
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TriggerMatcherTest {

    private fun makeTrigger(
        blendShape: String,
        threshold: Float,
        holdMs: Int = 0,
        cooldownMs: Int = 500,
        priority: Int = 100,
        action: Action = Action.GlobalBack
    ) = Trigger(
        id = UUID.randomUUID().toString(),
        profileId = "profile1",
        name = "test",
        blendShape = blendShape,
        threshold = threshold,
        holdDurationMs = holdMs,
        cooldownMs = cooldownMs,
        action = action,
        priority = priority
    )

    @Test
    fun `임계값 미달 시 액션 없음`() {
        val trigger = makeTrigger("eyeBlinkRight", threshold = 0.6f, holdMs = 0)
        val matcher = TriggerMatcher(listOf(trigger), globalCooldownMs = 0)

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.5f))
        assertTrue("임계값 미달이면 빈 리스트", result.isEmpty())
    }

    @Test
    fun `임계값 초과 시 holdDuration 0이면 즉시 발동`() {
        val trigger = makeTrigger("eyeBlinkRight", threshold = 0.5f, holdMs = 0)
        val matcher = TriggerMatcher(listOf(trigger), globalCooldownMs = 0)

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.8f))
        assertEquals(1, result.size)
        assertTrue(result[0] is Action.GlobalBack)
    }

    @Test
    fun `비활성화된 트리거는 발동 안 함`() {
        val trigger = Trigger(
            id = UUID.randomUUID().toString(),
            profileId = "profile1",
            name = "test",
            blendShape = "jawOpen",
            threshold = 0.3f,
            holdDurationMs = 0,
            cooldownMs = 500,
            action = Action.GlobalHome,
            isEnabled = false
        )
        val matcher = TriggerMatcher(listOf(trigger), globalCooldownMs = 0)
        val result = matcher.match(mapOf("jawOpen" to 1.0f))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `우선순위 낮은 번호가 먼저 실행`() {
        val highPriority = makeTrigger("eyeBlinkRight", 0.3f, priority = 1, action = Action.GlobalHome)
        val lowPriority = makeTrigger("eyeBlinkRight", 0.3f, priority = 100, action = Action.GlobalBack)
        val matcher = TriggerMatcher(listOf(lowPriority, highPriority), globalCooldownMs = 0)

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.9f))
        assertEquals(Action.GlobalHome, result[0])
    }

    @Test
    fun `표정 해제 시 holdStartTime 리셋 - 재축적 필요`() {
        val trigger = makeTrigger("jawOpen", threshold = 0.5f, holdMs = 200)
        val matcher = TriggerMatcher(listOf(trigger), globalCooldownMs = 0)

        // 임계값 초과 → 표정 해제 (holdStartTime 리셋)
        matcher.match(mapOf("jawOpen" to 0.8f))
        matcher.match(mapOf("jawOpen" to 0.0f))

        // holdMs=200ms 이내에 다시 임계값 초과해도 발동 안 함 (hold 재시작)
        val result = matcher.match(mapOf("jawOpen" to 0.8f))
        assertTrue("hold 재시작이므로 발동 없음", result.isEmpty())
    }

    @Test
    fun `전역 쿨다운 중 다른 트리거도 발동 없음`() {
        val trigger1 = makeTrigger("eyeBlinkRight", 0.5f, holdMs = 0, cooldownMs = 100, action = Action.GlobalBack)
        val trigger2 = makeTrigger("jawOpen", 0.5f, holdMs = 0, cooldownMs = 100, action = Action.GlobalHome)
        val matcher = TriggerMatcher(listOf(trigger1, trigger2), globalCooldownMs = 1000)

        // 첫 번째 트리거 발동
        val first = matcher.match(mapOf("eyeBlinkRight" to 0.9f, "jawOpen" to 0.9f))
        assertTrue("첫 발동 있음", first.isNotEmpty())

        // 전역 쿨다운 중 → 다른 트리거도 발동 안 함
        val second = matcher.match(mapOf("eyeBlinkRight" to 0.9f, "jawOpen" to 0.9f))
        assertTrue("전역 쿨다운 중 발동 없음", second.isEmpty())
    }
}
