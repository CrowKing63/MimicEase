package com.mimicease

import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger
import com.mimicease.service.TriggerMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `임계값 미달이면 액션이 없다`() {
        val trigger = makeTrigger("eyeBlinkRight", threshold = 0.6f, holdMs = 0)
        val matcher = TriggerMatcher(listOf(trigger), globalCooldownMs = 0)

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.5f))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `임계값 초과와 연속 프레임 1이면 즉시 발동한다`() {
        val trigger = makeTrigger("eyeBlinkRight", threshold = 0.5f, holdMs = 0)
        val matcher = TriggerMatcher(
            triggers = listOf(trigger),
            globalCooldownMs = 0,
            requiredFrames = 1
        )

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.8f))
        assertEquals(1, result.size)
        assertTrue(result[0] is Action.GlobalBack)
    }

    @Test
    fun `연속 프레임 3이면 세 번째 프레임에서 발동한다`() {
        val trigger = makeTrigger("eyeBlinkRight", threshold = 0.5f, holdMs = 0)
        val matcher = TriggerMatcher(
            triggers = listOf(trigger),
            globalCooldownMs = 0,
            requiredFrames = 3
        )

        assertTrue(matcher.match(mapOf("eyeBlinkRight" to 0.8f)).isEmpty())
        assertTrue(matcher.match(mapOf("eyeBlinkRight" to 0.8f)).isEmpty())

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.8f))
        assertEquals(1, result.size)
    }

    @Test
    fun `연속 프레임은 임계값 아래로 떨어지면 리셋된다`() {
        val trigger = makeTrigger("eyeBlinkRight", threshold = 0.5f, holdMs = 0)
        val matcher = TriggerMatcher(
            triggers = listOf(trigger),
            globalCooldownMs = 0,
            requiredFrames = 3
        )

        matcher.match(mapOf("eyeBlinkRight" to 0.8f))
        matcher.match(mapOf("eyeBlinkRight" to 0.8f))
        matcher.match(mapOf("eyeBlinkRight" to 0.2f))

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.8f))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `비활성화된 트리거는 발동하지 않는다`() {
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
    fun `우선순위가 높은 트리거가 먼저 실행된다`() {
        val highPriority = makeTrigger("eyeBlinkRight", 0.3f, priority = 1, action = Action.GlobalHome)
        val lowPriority = makeTrigger("eyeBlinkRight", 0.3f, priority = 100, action = Action.GlobalBack)
        val matcher = TriggerMatcher(listOf(lowPriority, highPriority), globalCooldownMs = 0)

        val result = matcher.match(mapOf("eyeBlinkRight" to 0.9f))
        assertEquals(Action.GlobalHome, result[0])
    }

    @Test
    fun `임계값 아래로 내려가면 hold 시작 시간이 리셋된다`() {
        val trigger = makeTrigger("jawOpen", threshold = 0.5f, holdMs = 200)
        val matcher = TriggerMatcher(listOf(trigger), globalCooldownMs = 0)

        matcher.match(mapOf("jawOpen" to 0.8f))
        matcher.match(mapOf("jawOpen" to 0.0f))

        val result = matcher.match(mapOf("jawOpen" to 0.8f))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `전역 쿨다운 중에는 다른 트리거도 발동하지 않는다`() {
        val trigger1 = makeTrigger("eyeBlinkRight", 0.5f, holdMs = 0, cooldownMs = 100, action = Action.GlobalBack)
        val trigger2 = makeTrigger("jawOpen", 0.5f, holdMs = 0, cooldownMs = 100, action = Action.GlobalHome)
        val matcher = TriggerMatcher(listOf(trigger1, trigger2), globalCooldownMs = 1000)

        val first = matcher.match(mapOf("eyeBlinkRight" to 0.9f, "jawOpen" to 0.9f))
        assertTrue(first.isNotEmpty())

        val second = matcher.match(mapOf("eyeBlinkRight" to 0.9f, "jawOpen" to 0.9f))
        assertTrue(second.isEmpty())
    }

    @Test
    fun `NoOp 액션을 가진 트리거는 발동하지 않는다`() {
        val noopTrigger = makeTrigger(
            blendShape = "eyeBlinkRight",
            threshold = 0.3f,
            action = Action.NoOp
        )
        val normalTrigger = makeTrigger(
            blendShape = "jawOpen",
            threshold = 0.3f,
            action = Action.GlobalHome
        )
        val matcher = TriggerMatcher(listOf(noopTrigger, normalTrigger), globalCooldownMs = 0)

        val result = matcher.match(mapOf("eyeBlinkRight" to 1.0f, "jawOpen" to 1.0f))

        assertEquals(1, result.size)
        assertTrue(result[0] is Action.GlobalHome)
    }
}
