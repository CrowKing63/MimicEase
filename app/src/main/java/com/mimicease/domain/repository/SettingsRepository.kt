package com.mimicease.domain.repository

import com.mimicease.data.local.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(updateParams: (AppSettings) -> AppSettings)
}
