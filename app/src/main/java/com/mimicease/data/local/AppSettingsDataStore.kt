package com.mimicease.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.camera.core.CameraSelector

data class AppSettings(
    val cameraFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val emaAlpha: Float = 0.5f,              // EMA 필터 계수 (0.1~0.9)
    val consecutiveFrames: Int = 3,          // 표정 확정 필요 연속 프레임 수
    val showForegroundNotification: Boolean = true,
    val notificationTapAction: String = "OPEN_APP",  // "OPEN_APP" | "PAUSE"
    val isDeveloperMode: Boolean = false,
    val isServiceEnabled: Boolean = false,
    val activeProfileId: String? = null,
    val onboardingCompleted: Boolean = false
)

// DataStore Keys
object AppSettingsKeys {
    val CAMERA_FACING        = intPreferencesKey("camera_facing")
    val EMA_ALPHA            = floatPreferencesKey("ema_alpha")
    val CONSECUTIVE_FRAMES   = intPreferencesKey("consecutive_frames")
    val SHOW_NOTIFICATION    = booleanPreferencesKey("show_notification")
    val DEVELOPER_MODE       = booleanPreferencesKey("developer_mode")
    val SERVICE_ENABLED      = booleanPreferencesKey("service_enabled")
    val ACTIVE_PROFILE_ID    = stringPreferencesKey("active_profile_id")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}
