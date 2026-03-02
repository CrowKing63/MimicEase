package com.mimicease.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.BuildConfig
import com.mimicease.data.local.AppSettings
import com.mimicease.domain.model.InteractionMode
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.presentation.ui.home.MimicBottomNavigation
import com.mimicease.service.MimicAccessibilityService
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

    fun toggleNotification(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showForegroundNotification = show) } }
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(isDeveloperMode = enabled) } }
    }

    fun updateActiveMode(mode: InteractionMode) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(activeMode = mode) } }
    }

    fun updateHeadMouseSensitivity(v: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(headMouseSensitivity = v) } }
    }

    fun updateHeadMouseDeadZone(v: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(headMouseDeadZone = v) } }
    }

    fun toggleDwellClick(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(dwellClickEnabled = enabled) } }
    }

    fun updateDwellClickTime(ms: Long) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(dwellClickTimeMs = ms) } }
    }

    fun updateDwellClickRadius(px: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(dwellClickRadiusPx = px) } }
    }

    fun toggleByKeyCombo(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(toggleByKeyCombo = enabled) } }
    }

    fun toggleByExpression(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(toggleByExpression = enabled) } }
    }

    fun toggleByBroadcast(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(toggleByBroadcast = enabled) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    val isAccessibilityEnabled = remember {
        MimicAccessibilityService.instance != null
    }

    val isBatteryOptExcluded = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(PowerManager::class.java)
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("설정") }) },
        bottomBar = { MimicBottomNavigation(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 상호작용 모드 ──────────────────────────────────────────────
            SettingsSectionHeader("상호작용 모드")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val modeLabels = mapOf(
                        InteractionMode.EXPRESSION_ONLY to "표정 전용",
                        InteractionMode.CURSOR_CLICK    to "커서 + 표정 클릭",
                        InteractionMode.HEAD_MOUSE      to "헤드 마우스"
                    )
                    val modeDescriptions = mapOf(
                        InteractionMode.EXPRESSION_ONLY to "표정으로 고정 좌표 제스처 실행",
                        InteractionMode.CURSOR_CLICK    to "블루투스 마우스로 커서 이동, 표정으로 클릭",
                        InteractionMode.HEAD_MOUSE      to "머리 움직임으로 커서 제어, 드웰 클릭"
                    )
                    InteractionMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.activeMode == mode,
                                onClick = { viewModel.updateActiveMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(modeLabels[mode] ?: mode.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    modeDescriptions[mode] ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── 헤드 마우스 ────────────────────────────────────────────────
            SettingsSectionHeader("헤드 마우스")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "감도: ${"%.1f".format(settings.headMouseSensitivity)}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.headMouseSensitivity,
                        onValueChange = { viewModel.updateHeadMouseSensitivity(it) },
                        valueRange = 0.5f..3.0f,
                        steps = 24
                    )

                    HorizontalDivider()

                    Text(
                        "데드존: ${"%.2f".format(settings.headMouseDeadZone)} rad",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "작을수록 미세한 움직임에도 반응",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.headMouseDeadZone,
                        onValueChange = { viewModel.updateHeadMouseDeadZone(it) },
                        valueRange = 0.0f..0.1f,
                        steps = 9
                    )
                }
            }

            // ── 드웰 클릭 ─────────────────────────────────────────────────
            SettingsSectionHeader("드웰 클릭 (헤드 마우스)")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("드웰 클릭 활성화")
                        Switch(
                            checked = settings.dwellClickEnabled,
                            onCheckedChange = { viewModel.toggleDwellClick(it) }
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "클릭 대기 시간: ${settings.dwellClickTimeMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.dwellClickEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.dwellClickTimeMs.toFloat(),
                        onValueChange = { viewModel.updateDwellClickTime(it.toLong()) },
                        valueRange = 500f..3000f,
                        steps = 24,
                        enabled = settings.dwellClickEnabled
                    )

                    HorizontalDivider()

                    Text(
                        "클릭 허용 반경: ${settings.dwellClickRadiusPx.toInt()}px",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.dwellClickEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "이 범위 안에서 정지하면 클릭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.dwellClickRadiusPx,
                        onValueChange = { viewModel.updateDwellClickRadius(it) },
                        valueRange = 10f..100f,
                        steps = 17,
                        enabled = settings.dwellClickEnabled
                    )
                }
            }

            // ── 글로벌 토글 ───────────────────────────────────────────────
            SettingsSectionHeader("글로벌 토글")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("볼륨 키 조합 (Up+Down 홀드)")
                            Text(
                                "${settings.toggleKeyHoldMs}ms 동안 누르면 토글",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.toggleByKeyCombo,
                            onCheckedChange = { viewModel.toggleByKeyCombo(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("표정 토글 (양쪽 눈 감기)")
                            Text(
                                "오발동 위험 — 신중히 활성화",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = settings.toggleByExpression,
                            onCheckedChange = { viewModel.toggleByExpression(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("브로드캐스트 토글")
                            Text(
                                "AI 어시스턴트 / 외부 앱으로 제어",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.toggleByBroadcast,
                            onCheckedChange = { viewModel.toggleByBroadcast(it) }
                        )
                    }
                }
            }

            // ── 감지 설정 ─────────────────────────────────────────────────
            SettingsSectionHeader("감지 설정")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "표정 평활화 (EMA α): ${"%.1f".format(settings.emaAlpha)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "높을수록 빠른 반응, 낮을수록 부드럽게",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("느림", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = settings.emaAlpha,
                            onValueChange = { viewModel.updateEmaAlpha(it) },
                            valueRange = 0.1f..0.9f,
                            steps = 7,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("빠름", style = MaterialTheme.typography.labelSmall)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        "연속 확정 프레임: ${settings.consecutiveFrames}프레임",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.consecutiveFrames.toFloat(),
                        onValueChange = { viewModel.updateConsecutiveFrames(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            }

            // ── 알림 설정 ─────────────────────────────────────────────────
            SettingsSectionHeader("알림 설정")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("포그라운드 알림 표시")
                        Switch(
                            checked = settings.showForegroundNotification,
                            onCheckedChange = { viewModel.toggleNotification(it) }
                        )
                    }
                }
            }

            // ── 시스템 ────────────────────────────────────────────────────
            SettingsSectionHeader("시스템")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 접근성 서비스 상태
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("접근성 서비스")
                            Text(
                                text = if (isAccessibilityEnabled) "활성화됨 ✓" else "비활성화됨",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!isAccessibilityEnabled) {
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                )
                            }) { Text("설정으로 이동") }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 배터리 최적화 제외
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("배터리 최적화 제외")
                            Text(
                                text = if (isBatteryOptExcluded) "제외됨 ✓" else "최적화 대상",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryOptExcluded) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isBatteryOptExcluded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }) { Text("제외 요청") }
                        }
                    }
                }
            }

            // ── 기타 ──────────────────────────────────────────────────────
            SettingsSectionHeader("기타")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("개발자 모드")
                        Switch(
                            checked = settings.isDeveloperMode,
                            onCheckedChange = { viewModel.toggleDeveloperMode(it) }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("버전")
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
