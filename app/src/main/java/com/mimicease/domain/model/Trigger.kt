package com.mimicease.domain.model

data class Trigger(
    val id: String,
    val profileId: String,
    val name: String,
    val blendShape: String,          // 블렌드쉐이프 ID
    val threshold: Float,
    val holdDurationMs: Int = 0,
    val cooldownMs: Int = 300,
    val action: Action,
    val isEnabled: Boolean = true,
    val priority: Int = 0
)
