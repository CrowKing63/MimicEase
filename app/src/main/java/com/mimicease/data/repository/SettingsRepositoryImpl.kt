package com.mimicease.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mimicease.data.local.AppSettings
import com.mimicease.data.local.AppSettingsKeys
import com.mimicease.domain.model.InteractionMode
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
                onboardingCompleted = preferences[AppSettingsKeys.ONBOARDING_COMPLETED] ?: false,
                activeMode = InteractionMode.fromString(
                    preferences[AppSettingsKeys.ACTIVE_MODE] ?: InteractionMode.EXPRESSION_ONLY.name
                ),
                toggleByExpression = preferences[AppSettingsKeys.TOGGLE_BY_EXPRESSION] ?: false,
                toggleExpressionHoldMs = preferences[AppSettingsKeys.TOGGLE_EXPRESSION_HOLD_MS] ?: 3000,
                toggleByKeyCombo = preferences[AppSettingsKeys.TOGGLE_BY_KEY_COMBO] ?: true,
                toggleKeyHoldMs = preferences[AppSettingsKeys.TOGGLE_KEY_HOLD_MS] ?: 2000,
                toggleByBroadcast = preferences[AppSettingsKeys.TOGGLE_BY_BROADCAST] ?: true,
                headMouseSensitivity = preferences[AppSettingsKeys.HEAD_MOUSE_SENSITIVITY] ?: 1.0f,
                headMouseDeadZone = preferences[AppSettingsKeys.HEAD_MOUSE_DEAD_ZONE] ?: 0.02f,
                dwellClickEnabled = preferences[AppSettingsKeys.DWELL_CLICK_ENABLED] ?: true,
                dwellClickTimeMs = preferences[AppSettingsKeys.DWELL_CLICK_TIME_MS] ?: 1000L,
                dwellClickRadiusPx = preferences[AppSettingsKeys.DWELL_CLICK_RADIUS_PX] ?: 30f
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
                onboardingCompleted = preferences[AppSettingsKeys.ONBOARDING_COMPLETED] ?: false,
                activeMode = InteractionMode.fromString(
                    preferences[AppSettingsKeys.ACTIVE_MODE] ?: InteractionMode.EXPRESSION_ONLY.name
                ),
                toggleByExpression = preferences[AppSettingsKeys.TOGGLE_BY_EXPRESSION] ?: false,
                toggleExpressionHoldMs = preferences[AppSettingsKeys.TOGGLE_EXPRESSION_HOLD_MS] ?: 3000,
                toggleByKeyCombo = preferences[AppSettingsKeys.TOGGLE_BY_KEY_COMBO] ?: true,
                toggleKeyHoldMs = preferences[AppSettingsKeys.TOGGLE_KEY_HOLD_MS] ?: 2000,
                toggleByBroadcast = preferences[AppSettingsKeys.TOGGLE_BY_BROADCAST] ?: true,
                headMouseSensitivity = preferences[AppSettingsKeys.HEAD_MOUSE_SENSITIVITY] ?: 1.0f,
                headMouseDeadZone = preferences[AppSettingsKeys.HEAD_MOUSE_DEAD_ZONE] ?: 0.02f,
                dwellClickEnabled = preferences[AppSettingsKeys.DWELL_CLICK_ENABLED] ?: true,
                dwellClickTimeMs = preferences[AppSettingsKeys.DWELL_CLICK_TIME_MS] ?: 1000L,
                dwellClickRadiusPx = preferences[AppSettingsKeys.DWELL_CLICK_RADIUS_PX] ?: 30f
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
            preferences[AppSettingsKeys.ACTIVE_MODE] = updated.activeMode.name
            preferences[AppSettingsKeys.TOGGLE_BY_EXPRESSION] = updated.toggleByExpression
            preferences[AppSettingsKeys.TOGGLE_EXPRESSION_HOLD_MS] = updated.toggleExpressionHoldMs
            preferences[AppSettingsKeys.TOGGLE_BY_KEY_COMBO] = updated.toggleByKeyCombo
            preferences[AppSettingsKeys.TOGGLE_KEY_HOLD_MS] = updated.toggleKeyHoldMs
            preferences[AppSettingsKeys.TOGGLE_BY_BROADCAST] = updated.toggleByBroadcast
            preferences[AppSettingsKeys.HEAD_MOUSE_SENSITIVITY] = updated.headMouseSensitivity
            preferences[AppSettingsKeys.HEAD_MOUSE_DEAD_ZONE] = updated.headMouseDeadZone
            preferences[AppSettingsKeys.DWELL_CLICK_ENABLED] = updated.dwellClickEnabled
            preferences[AppSettingsKeys.DWELL_CLICK_TIME_MS] = updated.dwellClickTimeMs
            preferences[AppSettingsKeys.DWELL_CLICK_RADIUS_PX] = updated.dwellClickRadiusPx
        }
    }
}

