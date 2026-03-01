package com.mimicease.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * 외부 앱, AI 어시스턴트(Gemini, Bixby), Tasker 등에서 Intent로
 * MimicEase 서비스를 ON/OFF할 수 있는 BroadcastReceiver.
 *
 * 사용 예시:
 *   adb shell am broadcast -a com.mimicease.ACTION_TOGGLE
 *   Google Assistant: "미믹이즈 켜줘" → deep link → ACTION_ENABLE
 *   Bixby Routines: 커스텀 Intent → ACTION_TOGGLE
 *   Tasker: Intent 전송 → ACTION_DISABLE
 */
class ToggleBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE = "com.mimicease.ACTION_TOGGLE"
        const val ACTION_ENABLE = "com.mimicease.ACTION_ENABLE"
        const val ACTION_DISABLE = "com.mimicease.ACTION_DISABLE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("ToggleBroadcastReceiver: received ${intent.action}")

        val service = MimicAccessibilityService.instance ?: run {
            Timber.w("ToggleBroadcastReceiver: AccessibilityService not running")
            return
        }

        val toggleController = service.globalToggleController ?: run {
            Timber.w("ToggleBroadcastReceiver: GlobalToggleController not initialized")
            return
        }

        when (intent.action) {
            ACTION_TOGGLE -> toggleController.handleBroadcastToggle()
            ACTION_ENABLE -> toggleController.handleBroadcastEnable()
            ACTION_DISABLE -> toggleController.handleBroadcastDisable()
        }
    }
}
