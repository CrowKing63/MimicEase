package com.mimicease.presentation.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimicease.domain.model.Profile
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.domain.repository.TriggerRepository
import com.mimicease.service.FaceDetectionForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentFps: Int = 0,
    val inferenceTimeMs: Long = 0,
    val activeProfile: Profile? = null,
    val quickTriggers: List<Trigger> = emptyList(),
    val isDeveloperMode: Boolean = false
)

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
                Pair(settings, profile)
            }.collect { (settings, profile) ->
                _uiState.update { current ->
                    current.copy(
                        isServiceRunning = settings.isServiceEnabled,
                        isDeveloperMode = settings.isDeveloperMode,
                        activeProfile = profile,
                        quickTriggers = profile?.triggers?.take(5) ?: emptyList()
                    )
                }
            }
        }

        // 서비스의 실시간 pause 상태 관찰
        viewModelScope.launch {
            FaceDetectionForegroundService.isPaused.collect { paused ->
                _uiState.update { it.copy(isPaused = paused) }
            }
        }

        // 추론 시간 관찰 (개발자 모드)
        viewModelScope.launch {
            FaceDetectionForegroundService.inferenceTimeMs.collect { ms ->
                _uiState.update { it.copy(inferenceTimeMs = ms) }
            }
        }
    }

    fun toggleServicePause() {
        val action = if (_uiState.value.isPaused) {
            FaceDetectionForegroundService.ACTION_RESUME
        } else {
            FaceDetectionForegroundService.ACTION_PAUSE
        }
        val intent = Intent(appContext, FaceDetectionForegroundService::class.java).apply {
            this.action = action
        }
        appContext.startService(intent)
    }

    fun startService() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(isServiceEnabled = true) }
        }
        val intent = Intent(appContext, FaceDetectionForegroundService::class.java)
        appContext.startForegroundService(intent)
    }

    fun stopService() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(isServiceEnabled = false) }
        }
        val intent = Intent(appContext, FaceDetectionForegroundService::class.java)
        appContext.stopService(intent)
    }

    fun toggleTriggerEnabled(trigger: Trigger, isEnabled: Boolean) {
        viewModelScope.launch {
            triggerRepository.setTriggerEnabled(trigger.id, isEnabled)
        }
    }
}
