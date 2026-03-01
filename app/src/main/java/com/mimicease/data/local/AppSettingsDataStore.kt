package com.mimicease.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.camera.core.CameraSelector
import com.mimicease.domain.model.InteractionMode

data class AppSettings(
    val cameraFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val emaAlpha: Float = 0.5f,              // EMA 필터 계수 (0.1~0.9)
    val consecutiveFrames: Int = 3,          // 표정 확정 필요 연속 프레임 수
    val showForegroundNotification: Boolean = true,
    val notificationTapAction: String = "OPEN_APP",  // "OPEN_APP" | "PAUSE"
    val isDeveloperMode: Boolean = false,
    val isServiceEnabled: Boolean = false,
    val activeProfileId: String? = null,
    val onboardingCompleted: Boolean = false,

    // ── 모드 시스템 ────────────────────────────────
    val activeMode: InteractionMode = InteractionMode.EXPRESSION_ONLY,

    // ── 글로벌 토글: 표정 채널 (기본 OFF — 오발동 위험) ──
    val toggleByExpression: Boolean = false,
    val toggleExpressionHoldMs: Int = 3000,

    // ── 글로벌 토글: 물리키/외부기기 채널 (기본 ON) ──
    val toggleByKeyCombo: Boolean = true,
    val toggleKeyHoldMs: Int = 2000,

    // ── 글로벌 토글: 브로드캐스트 채널 — AI 어시스턴트 (기본 ON) ──
    val toggleByBroadcast: Boolean = true,

    // ── 헤드 마우스 설정 ──────────────────────────
    val headMouseSensitivity: Float = 1.0f,    // 0.5 ~ 3.0
    val headMouseDeadZone: Float = 0.02f,      // 데드존 (0.0 ~ 0.1)
    val dwellClickEnabled: Boolean = true,
    val dwellClickTimeMs: Long = 1000L,        // 드웰 클릭 시간 (500~3000)
    val dwellClickRadiusPx: Float = 30f        // 드웰 영역 반경
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

    // 모드
    val ACTIVE_MODE          = stringPreferencesKey("active_mode")

    // 글로벌 토글
    val TOGGLE_BY_EXPRESSION     = booleanPreferencesKey("toggle_by_expression")
    val TOGGLE_EXPRESSION_HOLD_MS = intPreferencesKey("toggle_expression_hold_ms")
    val TOGGLE_BY_KEY_COMBO      = booleanPreferencesKey("toggle_by_key_combo")
    val TOGGLE_KEY_HOLD_MS       = intPreferencesKey("toggle_key_hold_ms")
    val TOGGLE_BY_BROADCAST      = booleanPreferencesKey("toggle_by_broadcast")

    // 헤드 마우스
    val HEAD_MOUSE_SENSITIVITY = floatPreferencesKey("head_mouse_sensitivity")
    val HEAD_MOUSE_DEAD_ZONE   = floatPreferencesKey("head_mouse_dead_zone")
    val DWELL_CLICK_ENABLED    = booleanPreferencesKey("dwell_click_enabled")
    val DWELL_CLICK_TIME_MS    = longPreferencesKey("dwell_click_time_ms")
    val DWELL_CLICK_RADIUS_PX  = floatPreferencesKey("dwell_click_radius_px")
}
