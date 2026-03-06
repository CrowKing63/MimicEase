package com.mimicease.presentation.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimicease.R
import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Profile
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.domain.repository.TriggerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class OnboardingEvent {
    object NavigateToHome : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val triggerRepository: TriggerRepository
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<OnboardingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(onboardingCompleted = true) }
            _eventFlow.emit(OnboardingEvent.NavigateToHome)
        }
    }

    fun createDefaultProfileAndComplete() {
        viewModelScope.launch {
            val profileId = UUID.randomUUID().toString()
            val defaultProfile = Profile(
                id = profileId,
                name = context.getString(R.string.default_profile_name),
                icon = "😊",
                isActive = true,
                sensitivity = 1.0f,
                globalCooldownMs = 300,
                triggers = emptyList()
            )

            // 프로필 먼저 저장
            profileRepository.saveProfile(defaultProfile)

            // 기본 트리거 4개 생성
            val defaultTriggers = listOf(
                Trigger(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    name = context.getString(R.string.default_trigger_wink_right),
                    blendShape = "eyeBlinkRight",
                    threshold = 0.6f,
                    holdDurationMs = 300,
                    action = Action.GlobalBack
                ),
                Trigger(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    name = context.getString(R.string.default_trigger_wink_left),
                    blendShape = "eyeBlinkLeft",
                    threshold = 0.6f,
                    holdDurationMs = 300,
                    action = Action.GlobalHome
                ),
                Trigger(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    name = context.getString(R.string.default_trigger_mouth_open),
                    blendShape = "jawOpen",
                    threshold = 0.5f,
                    holdDurationMs = 200,
                    action = Action.ScrollUp
                ),
                Trigger(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    name = context.getString(R.string.default_trigger_eyebrow_raise),
                    blendShape = "browInnerUp",
                    threshold = 0.5f,
                    holdDurationMs = 400,
                    action = Action.GlobalRecents
                )
            )
            
            defaultTriggers.forEach { triggerRepository.saveTrigger(it) }

            // 활성 프로필 ID 업데이트
            settingsRepository.updateSettings { 
                it.copy(
                    onboardingCompleted = true,
                    activeProfileId = profileId
                ) 
            }
            
            _eventFlow.emit(OnboardingEvent.NavigateToHome)
        }
    }
}
