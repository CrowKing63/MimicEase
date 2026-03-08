package com.mimicease.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mimicease.data.local.AppSettingsKeys
import com.mimicease.data.local.appSettingsDataStore
import com.mimicease.domain.model.ServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("BootCompletedReceiver: BOOT_COMPLETED received")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = context.appSettingsDataStore.data.first()
                val autoStartOnBoot = preferences[AppSettingsKeys.AUTO_START_ON_BOOT] ?: false
                val targetState = MimicServiceStateStore.readTargetState(preferences)

                MimicServiceStateStore.persistRuntimeState(context, ServiceState.Stopped)

                if (!autoStartOnBoot) {
                    Timber.d("BootCompletedReceiver: autoStartOnBoot=false -> skipping")
                    return@launch
                }

                if (!ServiceStatePolicy.shouldRestoreService(targetState)) {
                    Timber.d("BootCompletedReceiver: target state is stopped -> skipping")
                    return@launch
                }

                if (!context.isMimicAccessibilityServiceEnabled()) {
                    Timber.w("BootCompletedReceiver: accessibility service is disabled -> not restoring runtime")
                    return@launch
                }

                FaceDetectionForegroundService.createNotificationChannel(context)
                val serviceIntent = FaceDetectionForegroundService.createStartIntent(context, targetState)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Timber.e(e, "BootCompletedReceiver: failed to restore service state")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
