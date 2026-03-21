package com.mimicease.service

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.WindowManager
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 표정 트리거로 액션이 실행될 때 시각/청각/진동 피드백을 제공하는 컨트롤러.
 *
 * - 시각: 오버레이 권한이 있을 때 화면을 반투명 흰색으로 150ms 플래시
 * - 청각: ToneGenerator로 100ms 비프음
 * - 진동: 50ms 단발 진동
 */
class ActionFeedbackController(private val context: Context) {

    private var visualEnabled = false
    private var audioEnabled = false
    private var vibrateEnabled = false

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var flashView: FrameLayout? = null

    fun updateSettings(visual: Boolean, audio: Boolean, vibrate: Boolean) {
        visualEnabled = visual
        audioEnabled = audio
        vibrateEnabled = vibrate
    }

    fun onActionTriggered() {
        if (visualEnabled) triggerVisualFlash()
        if (audioEnabled) triggerBeep()
        if (vibrateEnabled) triggerVibrate()
    }

    private fun triggerVisualFlash() {
        if (!Settings.canDrawOverlays(context)) return
        scope.launch {
            try {
                val view = FrameLayout(context).apply {
                    setBackgroundColor(0x55FFFFFF.toInt())
                }
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                windowManager.addView(view, params)
                flashView = view
                delay(150L)
                if (view.parent != null) windowManager.removeView(view)
                if (flashView == view) flashView = null
            } catch (e: Exception) {
                Timber.w(e, "ActionFeedback: 시각 플래시 실패")
            }
        }
    }

    private fun triggerBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (e: Exception) {
            Timber.w(e, "ActionFeedback: 비프음 실패")
        }
    }

    private fun triggerVibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50L)
            }
        } catch (e: Exception) {
            Timber.w(e, "ActionFeedback: 진동 실패")
        }
    }

    fun destroy() {
        scope.cancel()
        flashView?.let { v ->
            if (v.parent != null) {
                try { windowManager.removeView(v) } catch (_: Exception) {}
            }
        }
        flashView = null
    }
}
