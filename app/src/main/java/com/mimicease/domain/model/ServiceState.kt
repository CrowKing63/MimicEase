package com.mimicease.domain.model

enum class ServiceState {
    Stopped,
    Running,
    Paused;

    val isStarted: Boolean
        get() = this != Stopped

    companion object {
        fun fromStorage(value: String?): ServiceState {
            return entries.firstOrNull { it.name == value } ?: Stopped
        }
    }
}
