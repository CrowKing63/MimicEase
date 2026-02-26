package com.mimicease.domain.model

data class Trigger(
    val id: String,
    val profileId: String,
    val name: String,
    val blendShape: String,          // 블렌드쉐이프 ID
    val threshold: Float,
    val holdDurationMs: Int,
    val cooldownMs: Int,
    val action: Action,
    val isEnabled: Boolean,
    val priority: Int
)
