package com.mimicease.presentation.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimicease.domain.model.Profile
import com.mimicease.domain.model.ServiceState
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.domain.repository.TriggerRepository
import com.mimicease.service.FaceDetectionForegroundService
import com.mimicease.service.MimicServiceStateStore
import com.mimicease.service.MimicAccessibilityService
import com.mimicease.service.ServiceStatePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

data class HomeUiState(
    val serviceState: ServiceState = ServiceState.Stopped,
    val currentFps: Int = 0,
    val inferenceTimeMs: Long = 0,
    val activeProfile: Profile? = null,
    val quickTriggers: List<Trigger> = emptyList(),
    val isDeveloperMode: Boolean = false
) {
    val isServiceRunning: Boolean
        get() = serviceState.isStarted

    val isPaused: Boolean
        get() = serviceState == ServiceState.Paused
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val triggerRepository: TriggerRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.getSettings(),
                profileRepository.getActiveProfile()
            ) { settings, profile ->
                settings to profile
            }.collect { (settings, profile) ->
                val runtimeState = ServiceStatePolicy.resolveRuntimeState(
                    storedRuntimeState = settings.serviceState,
                    isForegroundServiceRunning = MimicServiceStateStore.isForegroundServiceRunning(appContext)
                )

                _uiState.update { current ->
                    current.copy(
                        serviceState = runtimeState,
                        isDeveloperMode = settings.isDeveloperMode,
                        activeProfile = profile,
                        quickTriggers = profile?.triggers?.take(5) ?: emptyList()
                    )
                }
            }
        }

        viewModelScope.launch {
            FaceDetectionForegroundService.inferenceTimeMs.collect { ms ->
                val fps = if (ms > 0) {
                    (1000f / ms.toFloat()).roundToInt()
                } else {
                    0
                }

                _uiState.update {
                    it.copy(
                        inferenceTimeMs = ms,
                        currentFps = fps
                    )
                }
            }
        }
    }

    fun toggleServicePause() {
        val nextTargetState = ServiceStatePolicy.targetStateAfterToggle(_uiState.value.serviceState)
        requestTargetState(nextTargetState)
    }

    fun startService() {
        requestTargetState(ServiceState.Running)
    }

    fun stopService() {
        requestTargetState(ServiceState.Stopped)
    }

    fun toggleTriggerEnabled(trigger: Trigger, isEnabled: Boolean) {
        viewModelScope.launch {
            triggerRepository.setTriggerEnabled(trigger.id, isEnabled)
        }
    }

    private fun requestTargetState(targetState: ServiceState) {
        viewModelScope.launch {
            settingsRepository.updateSettings { settings ->
                settings.copy(targetServiceState = targetState)
            }
        }

        val intent = when (targetState) {
            ServiceState.Running,
            ServiceState.Paused -> FaceDetectionForegroundService.createStartIntent(appContext, targetState)
            ServiceState.Stopped -> Intent(appContext, FaceDetectionForegroundService::class.java).apply {
                action = FaceDetectionForegroundService.ACTION_STOP
            }
        }

        when (targetState) {
            ServiceState.Running,
            ServiceState.Paused -> {
                appContext.startForegroundService(intent)
                MimicAccessibilityService.instance?.ensureFaceDetectionServiceBound(targetState)
            }
            ServiceState.Stopped -> appContext.startService(intent)
        }
    }
}
