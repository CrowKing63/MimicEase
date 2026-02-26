package com.mimicease.service

class ExpressionAnalyzer(private var alpha: Float = 0.5f) {

    private val smoothedValues = mutableMapOf<String, Float>()
    private val frameCounters = mutableMapOf<String, Int>()
    private var requiredFrames: Int = 3

    fun updateSettings(emaAlpha: Float, consecutiveFrames: Int) {
        alpha = emaAlpha.coerceIn(0.1f, 0.9f)
        requiredFrames = consecutiveFrames.coerceIn(1, 10)
    }

    // EMA 필터만 적용한 값 반환 (TriggerMatcher용)
    fun processSmoothed(rawValues: Map<String, Float>): Map<String, Float> {
        rawValues.forEach { (key, newValue) ->
            val prev = smoothedValues[key] ?: newValue
            smoothedValues[key] = alpha * newValue + (1f - alpha) * prev
        }
        return smoothedValues.toMap()
    }

    fun reset() {
        smoothedValues.clear()
        frameCounters.clear()
    }
}
