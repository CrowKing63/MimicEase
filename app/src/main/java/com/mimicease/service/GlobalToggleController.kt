package com.mimicease.service

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import com.mimicease.data.local.AppSettings
import timber.log.Timber
import java.util.Locale

/**
 * 다중 채널 글로벌 토글 컨트롤러.
 * 물리키 조합, 표정, BroadcastReceiver(AI 어시스턴트) 등 다양한 입력으로
 * 앱 전체 기능을 ON/OFF할 수 있습니다.
 *
 * IoT 디바이스 제어처럼, 여러 채널이 동일한 toggleService()를 호출합니다.
 */
class GlobalToggleController(
    private val context: Context,
    private val onToggle: () -> Unit,
    private val onEnable: () -> Unit,
    private val onDisable: () -> Unit,
    private val isPaused: () -> Boolean
) {
    private var settings: AppSettings = AppSettings()
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // 물리키 조합 추적용
    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var comboStartTime = 0L

    // 표정 토글 추적용
    private var expressionHoldStart = 0L

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                ttsReady = true
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
    }

    // ═══════════════════════════════════════════════════
    // 채널 1: 물리키 (볼륨 Up+Down 동시 홀드)
    // AccessibilityService.onKeyEvent()에서 호출
    // ═══════════════════════════════════════════════════

    /**
     * 키 이벤트를 처리합니다. 토글 조건이 충족되면 true 반환 (이벤트 소비).
     * 처리하지 않으면 false 반환 (다른 앱에 전달).
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!settings.toggleByKeyCombo) return false

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    volumeUpPressed = true
                    checkComboStart()
                } else if (event.action == KeyEvent.ACTION_UP) {
                    volumeUpPressed = false
                    resetCombo()
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    volumeDownPressed = true
                    checkComboStart()
                } else if (event.action == KeyEvent.ACTION_UP) {
                    volumeDownPressed = false
                    resetCombo()
                }
            }
            else -> return false
        }

        // 양쪽 동시 누르기 진행 중이면 이벤트 소비
        if (volumeUpPressed && volumeDownPressed) {
            val elapsed = SystemClock.elapsedRealtime() - comboStartTime
            if (elapsed >= settings.toggleKeyHoldMs) {
                onToggle()
                announceState()
                resetCombo()
                return true
            }
            return true  // 아직 홀드 중 — 볼륨 변경 방지
        }
        return false
    }

    private fun checkComboStart() {
        if (volumeUpPressed && volumeDownPressed && comboStartTime == 0L) {
            comboStartTime = SystemClock.elapsedRealtime()
        }
    }

    private fun resetCombo() {
        if (!volumeUpPressed || !volumeDownPressed) {
            comboStartTime = 0L
        }
    }

    // ═══════════════════════════════════════════════════
    // 채널 2: 표정 (양쪽 눈 감기 등)
    // FaceDetectionForegroundService.processResults()에서 호출
    // ═══════════════════════════════════════════════════

    /**
     * 블렌드쉐이프 값으로 표정 토글을 확인합니다.
     * @return true이면 토글이 발동되어 이번 프레임의 다른 트리거는 건너뜀
     */
    fun checkExpressionToggle(smoothedValues: Map<String, Float>): Boolean {
        if (!settings.toggleByExpression) return false

        // 기본: 양쪽 눈 감기 (eyeBlinkLeft + eyeBlinkRight)
        val leftEye = smoothedValues["eyeBlinkLeft"] ?: 0f
        val rightEye = smoothedValues["eyeBlinkRight"] ?: 0f
        val threshold = 0.7f

        if (leftEye > threshold && rightEye > threshold) {
            if (expressionHoldStart == 0L) {
                expressionHoldStart = SystemClock.elapsedRealtime()
            }
            val elapsed = SystemClock.elapsedRealtime() - expressionHoldStart
            if (elapsed >= settings.toggleExpressionHoldMs) {
                onToggle()
                announceState()
                expressionHoldStart = 0L
                return true
            }
        } else {
            expressionHoldStart = 0L
        }
        return false
    }

    // ═══════════════════════════════════════════════════
    // 채널 3: 브로드캐스트 (AI 어시스턴트 / 외부 앱)
    // ToggleBroadcastReceiver에서 호출
    // ═══════════════════════════════════════════════════

    fun handleBroadcastToggle() {
        if (!settings.toggleByBroadcast) return
        onToggle()
        announceState()
    }

    fun handleBroadcastEnable() {
        if (!settings.toggleByBroadcast) return
        onEnable()
        announceState()
    }

    fun handleBroadcastDisable() {
        if (!settings.toggleByBroadcast) return
        onDisable()
        announceState()
    }

    // ═══════════════════════════════════════════════════
    // 상태 피드백: TTS + 진동
    // ═══════════════════════════════════════════════════

    private fun announceState() {
        val paused = isPaused()
        val message = if (paused) "미믹이즈 일시정지" else "미믹이즈 활성화"

        // TTS 안내
        if (ttsReady) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "toggle_announce")
        }

        // 진동 피드백
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = if (paused) {
                    // 일시정지: 짧은 2회 진동
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1)
                } else {
                    // 활성화: 긴 1회 진동
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(pattern)
            }
        } catch (e: Exception) {
            Timber.w(e, "Vibration feedback failed")
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
