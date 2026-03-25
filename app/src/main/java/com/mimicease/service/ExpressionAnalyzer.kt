package com.mimicease.service

import java.util.Collections

class ExpressionAnalyzer(private var alpha: Float = 0.5f) {

    private val smoothedValues = mutableMapOf<String, Float>()
    // 매 프레임 toMap() 복사 비용 제거: 동일 내부 맵의 읽기 전용 뷰를 반환
    private val smoothedReadOnly: Map<String, Float> = Collections.unmodifiableMap(smoothedValues)

    fun updateSettings(emaAlpha: Float) {
        alpha = emaAlpha.coerceIn(0.1f, 0.9f)
    }

    // EMA 필터만 적용한 값 반환 (TriggerMatcher용)
    // 반환된 맵은 다음 processSmoothed() 호출까지만 유효한 읽기 전용 뷰 (복사본 아님)
    fun processSmoothed(rawValues: Map<String, Float>): Map<String, Float> {
        rawValues.forEach { (key, newValue) ->
            val prev = smoothedValues[key] ?: newValue
            smoothedValues[key] = alpha * newValue + (1f - alpha) * prev
        }
        return smoothedReadOnly
    }

    fun reset() {
        smoothedValues.clear()
    }
}
