package com.mimicease.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mimicease.data.local.AppSettings
import com.mimicease.data.local.AppSettingsKeys
import com.mimicease.data.local.appSettingsDataStore
import com.mimicease.domain.model.InteractionMode
import com.mimicease.domain.model.ServiceState
import com.mimicease.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return context.appSettingsDataStore.data.map { preferences ->
            preferences.toAppSettings()
        }
    }

    override suspend fun updateSettings(updateParams: (AppSettings) -> AppSettings) {
        context.appSettingsDataStore.edit { preferences ->
            val current = preferences.toAppSettings()
            val updated = updateParams(current)

            preferences[AppSettingsKeys.CAMERA_FACING] = updated.cameraFacing
            preferences[AppSettingsKeys.EMA_ALPHA] = updated.emaAlpha
            preferences[AppSettingsKeys.CONSECUTIVE_FRAMES] = updated.consecutiveFrames
            preferences[AppSettingsKeys.SHOW_NOTIFICATION] = updated.showForegroundNotification
            preferences[AppSettingsKeys.DEVELOPER_MODE] = updated.isDeveloperMode
            preferences[AppSettingsKeys.TARGET_SERVICE_STATE] = updated.targetServiceState.name
            preferences[AppSettingsKeys.SERVICE_STATE] = updated.serviceState.name
            preferences[AppSettingsKeys.LEGACY_SERVICE_ENABLED] = updated.targetServiceState.isStarted
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
            preferences[AppSettingsKeys.HEAD_MOUSE_SENSITIVITY] = updated.headMouseSensitivity
            preferences[AppSettingsKeys.HEAD_MOUSE_DEAD_ZONE] = updated.headMouseDeadZone
            preferences[AppSettingsKeys.DWELL_CLICK_ENABLED] = updated.dwellClickEnabled
            preferences[AppSettingsKeys.DWELL_CLICK_TIME_MS] = updated.dwellClickTimeMs
            preferences[AppSettingsKeys.DWELL_CLICK_RADIUS_PX] = updated.dwellClickRadiusPx
            preferences[AppSettingsKeys.AUTO_START_ON_BOOT] = updated.autoStartOnBoot
            preferences[AppSettingsKeys.VOICE_CMD_STOP] = updated.voiceCommandStop
            preferences[AppSettingsKeys.VOICE_CMD_START] = updated.voiceCommandStart
        }
    }
}

private fun Preferences.toAppSettings(): AppSettings {
    val legacyEnabled = this[AppSettingsKeys.LEGACY_SERVICE_ENABLED] ?: false

    return AppSettings(
        cameraFacing = this[AppSettingsKeys.CAMERA_FACING] ?: androidx.camera.core.CameraSelector.LENS_FACING_FRONT,
        emaAlpha = this[AppSettingsKeys.EMA_ALPHA] ?: 0.5f,
        consecutiveFrames = this[AppSettingsKeys.CONSECUTIVE_FRAMES] ?: 3,
        showForegroundNotification = this[AppSettingsKeys.SHOW_NOTIFICATION] ?: true,
        isDeveloperMode = this[AppSettingsKeys.DEVELOPER_MODE] ?: false,
        targetServiceState = this[AppSettingsKeys.TARGET_SERVICE_STATE]?.let(ServiceState::fromStorage)
            ?: if (legacyEnabled) ServiceState.Running else ServiceState.Stopped,
        serviceState = this[AppSettingsKeys.SERVICE_STATE]?.let(ServiceState::fromStorage)
            ?: ServiceState.Stopped,
        activeProfileId = this[AppSettingsKeys.ACTIVE_PROFILE_ID],
        onboardingCompleted = this[AppSettingsKeys.ONBOARDING_COMPLETED] ?: false,
        activeMode = InteractionMode.fromString(
            this[AppSettingsKeys.ACTIVE_MODE] ?: InteractionMode.EXPRESSION_ONLY.name
        ),
        toggleByExpression = this[AppSettingsKeys.TOGGLE_BY_EXPRESSION] ?: false,
        toggleExpressionHoldMs = this[AppSettingsKeys.TOGGLE_EXPRESSION_HOLD_MS] ?: 3000,
        toggleByKeyCombo = this[AppSettingsKeys.TOGGLE_BY_KEY_COMBO] ?: true,
        toggleKeyHoldMs = this[AppSettingsKeys.TOGGLE_KEY_HOLD_MS] ?: 2000,
        headMouseSensitivity = this[AppSettingsKeys.HEAD_MOUSE_SENSITIVITY] ?: 1.0f,
        headMouseDeadZone = this[AppSettingsKeys.HEAD_MOUSE_DEAD_ZONE] ?: 0.02f,
        dwellClickEnabled = this[AppSettingsKeys.DWELL_CLICK_ENABLED] ?: true,
        dwellClickTimeMs = this[AppSettingsKeys.DWELL_CLICK_TIME_MS] ?: 1000L,
        dwellClickRadiusPx = this[AppSettingsKeys.DWELL_CLICK_RADIUS_PX] ?: 30f,
        autoStartOnBoot = this[AppSettingsKeys.AUTO_START_ON_BOOT] ?: false,
        voiceCommandStop = this[AppSettingsKeys.VOICE_CMD_STOP] ?: "표정 인식 정지",
        voiceCommandStart = this[AppSettingsKeys.VOICE_CMD_START] ?: "표정 인식 시작"
    )
}
