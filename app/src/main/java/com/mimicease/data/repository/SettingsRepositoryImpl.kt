package com.mimicease.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mimicease.data.local.AppSettings
import com.mimicease.data.local.AppSettingsKeys
import com.mimicease.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { preferences ->
            AppSettings(
                cameraFacing = preferences[AppSettingsKeys.CAMERA_FACING] ?: androidx.camera.core.CameraSelector.LENS_FACING_FRONT,
                emaAlpha = preferences[AppSettingsKeys.EMA_ALPHA] ?: 0.5f,
                consecutiveFrames = preferences[AppSettingsKeys.CONSECUTIVE_FRAMES] ?: 3,
                showForegroundNotification = preferences[AppSettingsKeys.SHOW_NOTIFICATION] ?: true,
                isDeveloperMode = preferences[AppSettingsKeys.DEVELOPER_MODE] ?: false,
                isServiceEnabled = preferences[AppSettingsKeys.SERVICE_ENABLED] ?: false,
                activeProfileId = preferences[AppSettingsKeys.ACTIVE_PROFILE_ID],
                onboardingCompleted = preferences[AppSettingsKeys.ONBOARDING_COMPLETED] ?: false
            )
        }
    }

    override suspend fun updateSettings(updateParams: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            val current = AppSettings(
                cameraFacing = preferences[AppSettingsKeys.CAMERA_FACING] ?: androidx.camera.core.CameraSelector.LENS_FACING_FRONT,
                emaAlpha = preferences[AppSettingsKeys.EMA_ALPHA] ?: 0.5f,
                consecutiveFrames = preferences[AppSettingsKeys.CONSECUTIVE_FRAMES] ?: 3,
                showForegroundNotification = preferences[AppSettingsKeys.SHOW_NOTIFICATION] ?: true,
                isDeveloperMode = preferences[AppSettingsKeys.DEVELOPER_MODE] ?: false,
                isServiceEnabled = preferences[AppSettingsKeys.SERVICE_ENABLED] ?: false,
                activeProfileId = preferences[AppSettingsKeys.ACTIVE_PROFILE_ID],
                onboardingCompleted = preferences[AppSettingsKeys.ONBOARDING_COMPLETED] ?: false
            )
            
            val updated = updateParams(current)
            
            preferences[AppSettingsKeys.CAMERA_FACING] = updated.cameraFacing
            preferences[AppSettingsKeys.EMA_ALPHA] = updated.emaAlpha
            preferences[AppSettingsKeys.CONSECUTIVE_FRAMES] = updated.consecutiveFrames
            preferences[AppSettingsKeys.SHOW_NOTIFICATION] = updated.showForegroundNotification
            preferences[AppSettingsKeys.DEVELOPER_MODE] = updated.isDeveloperMode
            preferences[AppSettingsKeys.SERVICE_ENABLED] = updated.isServiceEnabled
            if (updated.activeProfileId != null) {
                preferences[AppSettingsKeys.ACTIVE_PROFILE_ID] = updated.activeProfileId
            } else {
                preferences.remove(AppSettingsKeys.ACTIVE_PROFILE_ID)
            }
            preferences[AppSettingsKeys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
        }
    }
}
