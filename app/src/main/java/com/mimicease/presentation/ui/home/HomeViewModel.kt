package com.mimicease.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimicease.domain.model.Profile
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.domain.repository.TriggerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val triggerRepository: TriggerRepository
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
    }

    fun toggleServicePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
        // TODO: Intent to Foreground Service to pause analysis
    }

    fun toggleTriggerEnabled(trigger: Trigger, isEnabled: Boolean) {
        viewModelScope.launch {
            triggerRepository.setTriggerEnabled(trigger.id, isEnabled)
        }
    }
}
