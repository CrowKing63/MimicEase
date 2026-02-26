package com.mimicease.domain.model

data class Profile(
    val id: String,
    val name: String,
    val icon: String,
    val isActive: Boolean,
    val sensitivity: Float,
    val globalCooldownMs: Int,
    val triggers: List<Trigger>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
