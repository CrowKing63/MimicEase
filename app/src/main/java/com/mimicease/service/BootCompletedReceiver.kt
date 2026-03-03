package com.mimicease.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mimicease.data.local.AppSettingsKeys
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 부팅 완료 시 설정에 따라 FaceDetectionForegroundService를 자동 시작합니다.
 * 기본 동작: opt-out (autoStartOnBoot == false이면 시작하지 않음).
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("BootCompletedReceiver: BOOT_COMPLETED received")

        // DataStore에서 autoStartOnBoot 설정 읽기 (비동기)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStoreForBoot.data.first()
                val autoStart = prefs[AppSettingsKeys.AUTO_START_ON_BOOT] ?: false

                if (autoStart) {
                    Timber.d("BootCompletedReceiver: autoStartOnBoot=true → starting service")
                    FaceDetectionForegroundService.createNotificationChannel(context)
                    val serviceIntent = Intent(context, FaceDetectionForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    Timber.d("BootCompletedReceiver: autoStartOnBoot=false → skipping")
                }
            } catch (e: Exception) {
                Timber.e(e, "BootCompletedReceiver: failed to check settings")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

// DataStore 접근 — SettingsRepositoryImpl의 name = "settings" 와 반드시 동일해야 함
private val Context.dataStoreForBoot: androidx.datastore.core.DataStore<Preferences>
    by androidx.datastore.preferences.preferencesDataStore(name = "settings")
