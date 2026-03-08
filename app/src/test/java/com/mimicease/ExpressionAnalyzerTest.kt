package com.mimicease

import com.mimicease.service.ExpressionAnalyzer
import org.junit.Assert.assertEquals
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
        assertEquals(0.8f, result["eyeBlinkRight"] ?: 0f, 0.001f)
    }

    @Test
    fun `EMA 필터 - 두 번째 값은 평균으로 반영`() {
        analyzer.processSmoothed(mapOf("eyeBlinkRight" to 1.0f))
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.0f))
        assertEquals(0.5f, result["eyeBlinkRight"] ?: 0f, 0.001f)
    }

    @Test
    fun `EMA 필터 - alpha 가 작을수록 이전 값이 강하게 유지`() {
        val slowAnalyzer = ExpressionAnalyzer(alpha = 0.1f)
        slowAnalyzer.processSmoothed(mapOf("jawOpen" to 1.0f))
        val result = slowAnalyzer.processSmoothed(mapOf("jawOpen" to 0.0f))
        assertEquals(0.9f, result["jawOpen"] ?: 0f, 0.001f)
    }

    @Test
    fun `EMA 필터 - 여러 블렌드쉐이프를 독립적으로 처리`() {
        analyzer.processSmoothed(
            mapOf(
                "eyeBlinkLeft" to 1.0f,
                "jawOpen" to 0.6f
            )
        )
        val result = analyzer.processSmoothed(
            mapOf(
                "eyeBlinkLeft" to 0.0f,
                "jawOpen" to 0.0f
            )
        )
        assertEquals(0.5f, result["eyeBlinkLeft"] ?: 0f, 0.001f)
        assertEquals(0.3f, result["jawOpen"] ?: 0f, 0.001f)
    }

    @Test
    fun `reset 후 상태가 초기화된다`() {
        analyzer.processSmoothed(mapOf("eyeBlinkRight" to 1.0f))
        analyzer.reset()
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.5f))
        assertEquals(0.5f, result["eyeBlinkRight"] ?: 0f, 0.001f)
    }

    @Test
    fun `updateSettings - alpha 범위를 0_1 에서 0_9 로 강제한다`() {
        analyzer.updateSettings(emaAlpha = 2.0f)
        analyzer.processSmoothed(mapOf("x" to 1.0f))
        val result = analyzer.processSmoothed(mapOf("x" to 0.0f))
        assertEquals(0.1f, result["x"] ?: 0f, 0.001f)
    }
}
