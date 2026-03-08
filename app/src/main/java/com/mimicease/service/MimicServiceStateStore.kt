package com.mimicease.service

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mimicease.data.local.AppSettingsKeys
import com.mimicease.data.local.appSettingsDataStore
import com.mimicease.domain.model.ServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

data class ServiceStateSnapshot(
    val runtimeState: ServiceState,
    val targetState: ServiceState,
    val isAccessibilityServiceEnabled: Boolean
)

object MimicServiceStateStore {
    suspend fun readSnapshot(context: Context): ServiceStateSnapshot {
        val preferences = context.appSettingsDataStore.data.first()
        val targetState = readTargetState(preferences)
        val runtimeState = ServiceStatePolicy.resolveRuntimeState(
            storedRuntimeState = readStoredRuntimeState(preferences),
            isForegroundServiceRunning = isForegroundServiceRunning(context)
        )
        return ServiceStateSnapshot(
            runtimeState = runtimeState,
            targetState = targetState,
            isAccessibilityServiceEnabled = context.isMimicAccessibilityServiceEnabled()
        )
    }

    fun readSnapshotBlocking(context: Context): ServiceStateSnapshot {
        return runBlocking(Dispatchers.IO) {
            readSnapshot(context)
        }
    }

    suspend fun persistTargetState(context: Context, state: ServiceState) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[AppSettingsKeys.TARGET_SERVICE_STATE] = state.name
            preferences[AppSettingsKeys.LEGACY_SERVICE_ENABLED] = state.isStarted
        }
    }

    fun persistTargetStateBlocking(context: Context, state: ServiceState) {
        runBlocking(Dispatchers.IO) {
            persistTargetState(context, state)
        }
    }

    suspend fun persistRuntimeState(context: Context, state: ServiceState) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[AppSettingsKeys.SERVICE_STATE] = state.name
        }
    }

    fun persistRuntimeStateBlocking(context: Context, state: ServiceState) {
        runBlocking(Dispatchers.IO) {
            persistRuntimeState(context, state)
        }
    }

    fun readTargetState(preferences: Preferences): ServiceState {
        val storedState = preferences[AppSettingsKeys.TARGET_SERVICE_STATE]
        if (storedState != null) {
            return ServiceState.fromStorage(storedState)
        }

        val legacyEnabled = preferences[AppSettingsKeys.LEGACY_SERVICE_ENABLED] ?: false
        return if (legacyEnabled) ServiceState.Running else ServiceState.Stopped
    }

    fun readStoredRuntimeState(preferences: Preferences): ServiceState {
        return ServiceState.fromStorage(preferences[AppSettingsKeys.SERVICE_STATE])
    }

    @Suppress("DEPRECATION")
    fun isForegroundServiceRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == FaceDetectionForegroundService::class.java.name }
    }
}

fun Context.isMimicAccessibilityServiceEnabled(): Boolean {
    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val myComponent = ComponentName(this, MimicAccessibilityService::class.java)
    return enabledServices.split(':').any {
        ComponentName.unflattenFromString(it.trim()) == myComponent
    }
}
