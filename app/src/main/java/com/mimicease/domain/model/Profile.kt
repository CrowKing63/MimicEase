package com.mimicease.domain.model

data class Profile(
    val id: String,
    val name: String,
    val icon: String,
    val isActive: Boolean,
    val sensitivity: Float = 1.0f,
    val globalCooldownMs: Int = 300,
    val triggers: List<Trigger> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
