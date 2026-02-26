package com.mimicease.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.data.local.AppSettings
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.presentation.ui.home.MimicBottomNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateEmaAlpha(alpha: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(emaAlpha = alpha) } }
    }

    fun updateConsecutiveFrames(frames: Int) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(consecutiveFrames = frames) } }
    }

    fun toggleCameraFacing() {
        viewModelScope.launch {
            settingsRepository.updateSettings { 
                val newFacing = if (it.cameraFacing == 0) 1 else 0 
                it.copy(cameraFacing = newFacing) 
            }
        }
    }

    fun toggleNotification(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showForegroundNotification = show) } }
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(isDeveloperMode = enabled) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("설정") }) },
        bottomBar = { MimicBottomNavigation(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("감지 설정", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("표정 평활화 (EMA)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("느림")
                        Slider(
                            value = settings.emaAlpha,
                            onValueChange = { viewModel.updateEmaAlpha(it) },
                            valueRange = 0.1f..0.9f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("빠름")
                    }
                    Text("현재 값: %.1f".format(settings.emaAlpha), style = MaterialTheme.typography.bodySmall)
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("연속 확정 프레임 (${settings.consecutiveFrames} 프레임)")
                    Slider(
                        value = settings.consecutiveFrames.toFloat(),
                        onValueChange = { viewModel.updateConsecutiveFrames(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("기타", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("포그라운드 알림 표시")
                        Switch(checked = settings.showForegroundNotification, onCheckedChange = { viewModel.toggleNotification(it) })
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("개발자 모드")
                        Switch(checked = settings.isDeveloperMode, onCheckedChange = { viewModel.toggleDeveloperMode(it) })
                    }
                }
            }
        }
    }
}
