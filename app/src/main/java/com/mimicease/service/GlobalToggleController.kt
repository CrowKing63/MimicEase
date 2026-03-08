package com.mimicease.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import com.mimicease.data.local.AppSettings
import com.mimicease.domain.model.ServiceState
import timber.log.Timber
import java.util.Locale

/**
 * 다중 채널 글로벌 토글 컨트롤러.
 * 물리 키 조합, 표정, BroadcastReceiver를 같은 상태 기준으로 묶어 처리한다.
 */
class GlobalToggleController(
    private val context: Context,
    private val onToggle: () -> ServiceState,
    private val onEnable: () -> ServiceState,
    private val onDisable: () -> ServiceState
) {
    private var settings: AppSettings = AppSettings()
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var comboStartTime = 0L
    private var expressionHoldStart = 0L

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.KOREAN
                    ttsReady = true
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "TTS initialization failed - TTS feedback disabled")
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
    }

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

        if (volumeUpPressed && volumeDownPressed) {
            val elapsed = System.currentTimeMillis() - comboStartTime
            if (elapsed >= settings.toggleKeyHoldMs) {
                announceState(onToggle())
                resetCombo()
                return true
            }
            return true
        }
        return false
    }

    private fun checkComboStart() {
        if (volumeUpPressed && volumeDownPressed && comboStartTime == 0L) {
            comboStartTime = System.currentTimeMillis()
        }
    }

    private fun resetCombo() {
        if (!volumeUpPressed || !volumeDownPressed) {
            comboStartTime = 0L
        }
    }

    fun checkExpressionToggle(smoothedValues: Map<String, Float>): Boolean {
        if (!settings.toggleByExpression) return false

        val leftEye = smoothedValues["eyeBlinkLeft"] ?: 0f
        val rightEye = smoothedValues["eyeBlinkRight"] ?: 0f
        val threshold = 0.7f

        if (leftEye > threshold && rightEye > threshold) {
            if (expressionHoldStart == 0L) {
                expressionHoldStart = System.currentTimeMillis()
            }
            val elapsed = System.currentTimeMillis() - expressionHoldStart
            if (elapsed >= settings.toggleExpressionHoldMs) {
                announceState(onToggle())
                expressionHoldStart = 0L
                return true
            }
        } else {
            expressionHoldStart = 0L
        }
        return false
    }

    fun handleBroadcastToggle() {
        announceState(onToggle())
    }

    fun handleBroadcastEnable() {
        announceState(onEnable())
    }

    fun handleBroadcastDisable() {
        announceState(onDisable())
    }

    private fun announceState(state: ServiceState) {
        val message = when (state) {
            ServiceState.Running -> "미믹이즈 활성화"
            ServiceState.Paused -> "미믹이즈 일시정지"
            ServiceState.Stopped -> "미믹이즈 종료"
        }

        if (ttsReady) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "toggle_announce")
        }

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = when (state) {
                    ServiceState.Running -> {
                        VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    ServiceState.Paused -> {
                        VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1)
                    }
                    ServiceState.Stopped -> {
                        VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80, 80, 80), -1)
                    }
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
