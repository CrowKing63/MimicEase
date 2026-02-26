package com.mimicease

import com.mimicease.service.ExpressionAnalyzer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExpressionAnalyzerTest {

    private lateinit var analyzer: ExpressionAnalyzer

    @Before
    fun setUp() {
        analyzer = ExpressionAnalyzer(alpha = 0.5f)
    }

    @Test
    fun `EMA 필터 - 첫 번째 값은 그대로 반환`() {
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.8f))
        // 첫 프레임: prev = newValue (초기화), EMA = alpha*new + (1-alpha)*new = new
        assertEquals(0.8f, result["eyeBlinkRight"] ?: 0f, 0.001f)
    }

    @Test
    fun `EMA 필터 - 두 번째 값은 평균으로 수렴`() {
        analyzer.processSmoothed(mapOf("eyeBlinkRight" to 1.0f))
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.0f))
        // 2nd: EMA = 0.5 * 0.0 + 0.5 * 1.0 = 0.5
        assertEquals(0.5f, result["eyeBlinkRight"] ?: 0f, 0.001f)
    }

    @Test
    fun `EMA 필터 - alpha 0으로 수렴하면 이전 값 유지`() {
        val slowAnalyzer = ExpressionAnalyzer(alpha = 0.1f)
        slowAnalyzer.processSmoothed(mapOf("jawOpen" to 1.0f))
        val result = slowAnalyzer.processSmoothed(mapOf("jawOpen" to 0.0f))
        // EMA = 0.1 * 0.0 + 0.9 * 1.0 = 0.9
        assertEquals(0.9f, result["jawOpen"] ?: 0f, 0.001f)
    }

    @Test
    fun `EMA 필터 - 여러 블렌드쉐이프 독립적으로 처리`() {
        analyzer.processSmoothed(mapOf(
            "eyeBlinkLeft" to 1.0f,
            "jawOpen" to 0.6f
        ))
        val result = analyzer.processSmoothed(mapOf(
            "eyeBlinkLeft" to 0.0f,
            "jawOpen" to 0.0f
        ))
        assertEquals(0.5f, result["eyeBlinkLeft"] ?: 0f, 0.001f)
        assertEquals(0.3f, result["jawOpen"] ?: 0f, 0.001f)
    }

    @Test
    fun `reset 후 상태 초기화`() {
        analyzer.processSmoothed(mapOf("eyeBlinkRight" to 1.0f))
        analyzer.reset()
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.5f))
        // 리셋 후 첫 프레임이므로 그대로 0.5
        assertEquals(0.5f, result["eyeBlinkRight"] ?: 0f, 0.001f)
    }

    @Test
    fun `updateSettings - alpha 범위 강제 (0_1 ~ 0_9)`() {
        analyzer.updateSettings(emaAlpha = 2.0f, consecutiveFrames = 3)
        // alpha는 0.9로 clamped 됨
        analyzer.processSmoothed(mapOf("x" to 1.0f))
        val result = analyzer.processSmoothed(mapOf("x" to 0.0f))
        // EMA with alpha=0.9: 0.9*0 + 0.1*1 = 0.1
        assertEquals(0.1f, result["x"] ?: 0f, 0.001f)
    }
}
