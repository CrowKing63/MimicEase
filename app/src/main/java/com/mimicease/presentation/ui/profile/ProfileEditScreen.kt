package com.mimicease.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.domain.model.Profile
import com.mimicease.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ─── ViewModel ───────────────────────────────────────────────────────────

data class ProfileEditUiState(
    val name: String = "",
    val icon: String = "😊",
    val sensitivity: Float = 1.0f,
    val globalCooldownMs: Int = 300,
    val isSaving: Boolean = false
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileId: String = savedStateHandle["profileId"] ?: ""
    private var originalProfile: Profile? = null

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.getAllProfiles().collect { profiles ->
                profiles.find { it.id == profileId }?.let { profile ->
                    if (originalProfile == null) {
                        originalProfile = profile
                        _uiState.update {
                            it.copy(
                                name = profile.name,
                                icon = profile.icon,
                                sensitivity = profile.sensitivity,
                                globalCooldownMs = profile.globalCooldownMs
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateIcon(icon: String) = _uiState.update { it.copy(icon = icon) }
    fun updateSensitivity(v: Float) = _uiState.update { it.copy(sensitivity = v) }
    fun updateGlobalCooldown(ms: Int) = _uiState.update { it.copy(globalCooldownMs = ms) }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val original = originalProfile ?: return
        if (state.name.isBlank()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            profileRepository.saveProfile(
                original.copy(
                    name = state.name.trim(),
                    icon = state.icon,
                    sensitivity = state.sensitivity,
                    globalCooldownMs = state.globalCooldownMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}

// ─── UI ──────────────────────────────────────────────────────────────────

private val PROFILE_ICONS = listOf("😊","😴","🎮","📺","📖","💼","🏃","✍️","🎵","📱")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    profileId: String,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 편집") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = { viewModel.save { navController.popBackStack() } },
                            enabled = uiState.name.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "저장")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 프로필 이름
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("프로필 이름") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 아이콘 선택
            Text("아이콘", style = MaterialTheme.typography.titleSmall)
            val chunks = PROFILE_ICONS.chunked(5)
            chunks.forEach { chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunk.forEach { emoji ->
                        FilterChip(
                            selected = uiState.icon == emoji,
                            onClick = { viewModel.updateIcon(emoji) },
                            label = { Text(emoji) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 감도 슬라이더 (0.5x ~ 2.0x)
            Text(
                text = "감도: ${"%.1f".format(uiState.sensitivity)}x",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "높을수록 약한 표정에도 트리거가 발동됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = uiState.sensitivity,
                onValueChange = viewModel::updateSensitivity,
                valueRange = 0.5f..2.0f,
                steps = 14
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("느림 (0.5x)", style = MaterialTheme.typography.labelSmall)
                Text("빠름 (2.0x)", style = MaterialTheme.typography.labelSmall)
            }

            HorizontalDivider()

            // 전역 쿨다운 슬라이더 (0 ~ 2000ms)
            Text(
                text = "전역 쿨다운: ${uiState.globalCooldownMs}ms",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "어떤 트리거든 발동 후 전체 일시 대기 시간",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = uiState.globalCooldownMs.toFloat(),
                onValueChange = { viewModel.updateGlobalCooldown(it.roundToInt()) },
                valueRange = 0f..2000f,
                steps = 39
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
