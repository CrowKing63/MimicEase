package com.mimicease.presentation.ui.settings

import android.content.ComponentName
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    fun toggleAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(autoStartOnBoot = enabled) } }
    }

    fun updateVoiceCommandStop(cmd: String) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(voiceCommandStop = cmd) } }
    }

    fun updateVoiceCommandStart(cmd: String) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(voiceCommandStart = cmd) } }
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

    var isAccessibilityEnabled by remember {
        // instance != null 대신 시스템 접근성 설정을 확인.
        // 앱 프로세스가 재시작된 직후 instance가 일시적으로 null이 되더라도 올바른 상태를 표시.
        mutableStateOf(context.isMimicAccessibilityServiceEnabled())
    }

    var isBatteryOptExcluded by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(PowerManager::class.java)
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else true
        )
    }

    // HEAD_MOUSE 모드에서 커서 오버레이를 표시하려면 SYSTEM_ALERT_WINDOW 권한 필요.
    // Settings.canDrawOverlays()로 실시간 확인하고 ON_RESUME에서 갱신.
    var isOverlayPermGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = context.isMimicAccessibilityServiceEnabled()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val pm = context.getSystemService(PowerManager::class.java)
                    isBatteryOptExcluded = pm.isIgnoringBatteryOptimizations(context.packageName)
                }
                isOverlayPermGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

            // ── 헤드 마우스 오버레이 권한 경고 ──────────────────────────────
            // HEAD_MOUSE를 선택했으나 SYSTEM_ALERT_WINDOW 권한이 없으면
            // CursorOverlayView.show()가 WindowManager에서 SecurityException을 던져
            // 커서가 화면에 전혀 표시되지 않음. 사용자가 직접 권한을 허용해야 함.
            if (settings.activeMode == InteractionMode.HEAD_MOUSE && !isOverlayPermGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "오버레이 권한 필요",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "헤드 마우스 커서를 화면에 표시하려면 '다른 앱 위에 표시' 권한이 필요합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        ) {
                            Text("권한 허용", color = MaterialTheme.colorScheme.onErrorContainer)
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

            // ── 부팅 자동 시작 ─────────────────────────────────────────────
            SettingsSectionHeader("서비스 자동 시작")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("부팅 시 자동 시작")
                            Text(
                                "기기 재시작 후 서비스를 자동으로 실행합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoStartOnBoot,
                            onCheckedChange = { viewModel.toggleAutoStartOnBoot(it) }
                        )
                    }
                }
            }

            // ── 음성 명령 설정 ─────────────────────────────────────────────
            SettingsSectionHeader("음성 명령 설정")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "AI 어시스턴트(Google, Bixby 등)에서 사용할 관용구를 설정합니다. Bixby Routines 또는 Tasker로 브로드캐스트를 연동하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = settings.voiceCommandStop,
                        onValueChange = { viewModel.updateVoiceCommandStop(it) },
                        label = { Text("정지 관용구") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = settings.voiceCommandStart,
                        onValueChange = { viewModel.updateVoiceCommandStart(it) },
                        label = { Text("시작 관용구") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("튜토리얼")
                            Text(
                                "앱 사용 방법을 단계별로 확인합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { navController.navigate("tutorial") }) {
                            Text("다시 보기")
                        }
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

/**
 * 시스템 접근성 설정에서 MimicAccessibilityService가 실제로 활성화되어 있는지 확인.
 * MimicAccessibilityService.instance != null 대신 이 함수를 사용하면
 * 앱 프로세스 재시작 시 발생하는 일시적 null 상태에서도 올바른 결과를 반환.
 */
private fun android.content.Context.isMimicAccessibilityServiceEnabled(): Boolean {
    val enabledServices = Settings.Secure.getString(
        contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val myComponent = ComponentName(this, MimicAccessibilityService::class.java)
    return enabledServices.split(':').any {
        try {
            ComponentName.unflattenFromString(it.trim()) == myComponent
        } catch (e: Exception) {
            false
        }
    }
}
