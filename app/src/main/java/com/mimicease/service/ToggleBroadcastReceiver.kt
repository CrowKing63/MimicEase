package com.mimicease.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.mimicease.domain.model.ServiceState
import timber.log.Timber

class ToggleBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE = "com.mimicease.ACTION_TOGGLE"
        const val ACTION_ENABLE = "com.mimicease.ACTION_ENABLE"
        const val ACTION_DISABLE = "com.mimicease.ACTION_DISABLE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.d("ToggleBroadcastReceiver: received $action")

        if (action !in listOf(ACTION_TOGGLE, ACTION_ENABLE, ACTION_DISABLE)) {
            Timber.w("ToggleBroadcastReceiver: unknown action=$action")
            return
        }

        MimicAccessibilityService.instance?.globalToggleController?.let { controller ->
            when (action) {
                ACTION_TOGGLE -> controller.handleBroadcastToggle()
                ACTION_ENABLE -> controller.handleBroadcastEnable()
                ACTION_DISABLE -> controller.handleBroadcastDisable()
            }
            return
        }

        val snapshot = MimicServiceStateStore.readSnapshotBlocking(context)
        if (!snapshot.isAccessibilityServiceEnabled) {
            Timber.w("ToggleBroadcastReceiver: accessibility service is disabled")
            showFeedback(context, "MimicEase 접근성 서비스를 먼저 활성화하세요")
            return
        }

        val targetState = when (action) {
            ACTION_TOGGLE -> ServiceStatePolicy.targetStateAfterToggle(snapshot.runtimeState)
            ACTION_ENABLE -> ServiceStatePolicy.targetStateAfterEnable()
            ACTION_DISABLE -> ServiceStatePolicy.targetStateAfterDisable(snapshot.runtimeState)
            else -> snapshot.targetState
        }

        MimicServiceStateStore.persistTargetStateBlocking(context, targetState)

        try {
            when (targetState) {
                ServiceState.Running,
                ServiceState.Paused -> {
                    context.startForegroundService(
                        FaceDetectionForegroundService.createStartIntent(context, targetState)
                    )
                    MimicAccessibilityService.instance?.ensureFaceDetectionServiceBound(targetState)
                }

                ServiceState.Stopped -> {
                    if (snapshot.runtimeState.isStarted) {
                        context.startService(Intent(context, FaceDetectionForegroundService::class.java).apply {
                            this.action = FaceDetectionForegroundService.ACTION_STOP
                        })
                    } else {
                        MimicServiceStateStore.persistRuntimeStateBlocking(context, ServiceState.Stopped)
                    }
                }
            }
            showFeedback(context, feedbackMessage(targetState))
        } catch (e: Exception) {
            Timber.e(e, "ToggleBroadcastReceiver: failed to update service state")
            showFeedback(context, "MimicEase 제어 실패")
        }
    }

    private fun feedbackMessage(state: ServiceState): String {
        return when (state) {
            ServiceState.Running -> "MimicEase 활성화"
            ServiceState.Paused -> "MimicEase 일시정지"
            ServiceState.Stopped -> "MimicEase 종료"
        }
    }

    private fun showFeedback(context: Context, message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.w(e, "Toast 표시 실패")
        }
    }
}
