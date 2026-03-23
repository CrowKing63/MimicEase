package com.mimicease.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import com.mimicease.data.local.AppSettings
import com.mimicease.domain.model.ServiceState
import timber.log.Timber

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

    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var comboStartTime = 0L
    private var expressionHoldStart = 0L

    private val toneHandler = Handler(Looper.getMainLooper())

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
        playStateTone(state)

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

    /**
     * 상태 변경 시 ToneGenerator로 짧은 비프음 재생.
     * - Running : 높은 단음 1회  (활성화)
     * - Paused  : 중간 음 1회    (일시정지)
     * - Stopped : 낮은 음 2회    (종료)
     *
     * TTS 대신 사용하는 이유: 삼성 빅스비와 동일한 TTS 엔진을 공유하면
     * 오디오 세션 경합으로 빅스비가 호출 직후 종료되는 문제가 발생함.
     * ToneGenerator는 TTS 엔진과 독립적으로 동작한다.
     */
    private fun playStateTone(state: ServiceState) {
        try {
            when (state) {
                ServiceState.Running -> {
                    // 높은 단음 1회
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
                    toneHandler.postDelayed({ toneGen.release() }, 300L)
                }
                ServiceState.Paused -> {
                    // 중간 단음 1회
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    toneHandler.postDelayed({ toneGen.release() }, 300L)
                }
                ServiceState.Stopped -> {
                    // 낮은 음 2회 (종료 느낌)
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME)
                    toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 150)
                    toneHandler.postDelayed({
                        toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 150)
                        toneHandler.postDelayed({ toneGen.release() }, 200L)
                    }, 250L)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Tone playback failed")
        }
    }

    fun destroy() {
        toneHandler.removeCallbacksAndMessages(null)
    }
}
