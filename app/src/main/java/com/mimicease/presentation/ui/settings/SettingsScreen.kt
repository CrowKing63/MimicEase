package com.mimicease.presentation.ui.settings

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.mimicease.R
import com.mimicease.service.MimicToggleTileService
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    fun toggleAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(autoStartOnBoot = enabled) } }
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

    // Locale-aware mode labels / descriptions (must be inside Composable)
    val modeLabels = mapOf(
        InteractionMode.EXPRESSION_ONLY to stringResource(R.string.settings_mode_expression_only),
        InteractionMode.CURSOR_CLICK    to stringResource(R.string.settings_mode_cursor_click),
        InteractionMode.HEAD_MOUSE      to stringResource(R.string.settings_mode_head_mouse)
    )
    val modeDescriptions = mapOf(
        InteractionMode.EXPRESSION_ONLY to stringResource(R.string.settings_mode_expression_only_desc),
        InteractionMode.CURSOR_CLICK    to stringResource(R.string.settings_mode_cursor_click_desc),
        InteractionMode.HEAD_MOUSE      to stringResource(R.string.settings_mode_head_mouse_desc)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
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
            // ── Interaction mode ──────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_interaction_mode))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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

            // ── Overlay permission warning (HEAD_MOUSE only) ──────────────
            if (settings.activeMode == InteractionMode.HEAD_MOUSE && !isOverlayPermGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_overlay_permission_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                stringResource(R.string.settings_overlay_permission_desc),
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
                            Text(stringResource(R.string.settings_overlay_allow),
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // ── Head mouse ────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_head_mouse_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_sensitivity, "%.1f".format(settings.headMouseSensitivity)),
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
                        stringResource(R.string.settings_dead_zone, "%.2f".format(settings.headMouseDeadZone)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.settings_dead_zone_desc),
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

            // ── Dwell click ───────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_dwell_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_dwell_enable))
                        Switch(
                            checked = settings.dwellClickEnabled,
                            onCheckedChange = { viewModel.toggleDwellClick(it) }
                        )
                    }

                    HorizontalDivider()

                    Text(
                        stringResource(R.string.settings_dwell_time, settings.dwellClickTimeMs.toInt()),
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
                        stringResource(R.string.settings_dwell_radius, settings.dwellClickRadiusPx.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.dwellClickEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.settings_dwell_radius_desc),
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

            // ── Global toggle ─────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_global_toggle_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_volume_key_combo))
                            Text(
                                stringResource(R.string.settings_volume_key_desc, settings.toggleKeyHoldMs.toInt()),
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
                            Text(stringResource(R.string.settings_expression_toggle))
                            Text(
                                stringResource(R.string.settings_expression_toggle_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = settings.toggleByExpression,
                            onCheckedChange = { viewModel.toggleByExpression(it) }
                        )
                    }
                }
            }

            // ── AI assistant integration ──────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_ai_assistant_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Quick Settings tile
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_qs_tile))
                        Text(
                            stringResource(R.string.settings_qs_tile_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            TextButton(
                                onClick = { addQsTileIfSupported(context) },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.settings_qs_tile_add))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Bixby Routines
                    var bixbyNotFound by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_bixby_routines))
                            Text(
                                stringResource(R.string.settings_bixby_routines_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (bixbyNotFound) {
                                Text(
                                    stringResource(R.string.settings_bixby_not_found),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        TextButton(onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage(
                                "com.samsung.android.app.routines"
                            )
                            if (intent != null) {
                                bixbyNotFound = false
                                context.startActivity(intent)
                            } else {
                                bixbyNotFound = true
                            }
                        }) {
                            Text(stringResource(R.string.settings_bixby_open))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Gemini
                    Column {
                        Text(stringResource(R.string.settings_gemini))
                        Text(
                            stringResource(R.string.settings_gemini_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Detection settings ────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_detection_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_ema_alpha, "%.1f".format(settings.emaAlpha)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.settings_ema_alpha_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_ema_slow), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = settings.emaAlpha,
                            onValueChange = { viewModel.updateEmaAlpha(it) },
                            valueRange = 0.1f..0.9f,
                            steps = 7,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text(stringResource(R.string.settings_ema_fast), style = MaterialTheme.typography.labelSmall)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        stringResource(R.string.settings_consecutive_frames, settings.consecutiveFrames),
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

            // ── Notification settings ─────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_notification_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_show_notification))
                        Switch(
                            checked = settings.showForegroundNotification,
                            onCheckedChange = { viewModel.toggleNotification(it) }
                        )
                    }
                }
            }

            // ── Auto start on boot ────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_auto_start_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_auto_start_on_boot))
                            Text(
                                stringResource(R.string.settings_auto_start_on_boot_desc),
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

            // ── System ────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_system_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_accessibility_service))
                            Text(
                                text = if (isAccessibilityEnabled)
                                    stringResource(R.string.settings_accessibility_enabled)
                                else
                                    stringResource(R.string.settings_accessibility_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!isAccessibilityEnabled) {
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }) { Text(stringResource(R.string.settings_go_to_settings)) }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_battery_optimization))
                            Text(
                                text = if (isBatteryOptExcluded)
                                    stringResource(R.string.settings_battery_excluded)
                                else
                                    stringResource(R.string.settings_battery_included),
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
                            }) { Text(stringResource(R.string.settings_battery_request)) }
                        }
                    }
                }
            }

            // ── Other ─────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_other_section))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_developer_mode))
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
                        Text(stringResource(R.string.settings_version))
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_tutorial))
                            Text(
                                stringResource(R.string.settings_tutorial_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { navController.navigate("tutorial") }) {
                            Text(stringResource(R.string.settings_tutorial_view))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Request adding QS tile.
 * - API 33+: show system dialog to guide user to add the tile
 * - API < 33: no-op (caller checks API level before calling)
 */
@SuppressLint("NewApi")
private fun addQsTileIfSupported(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val sbm = context.getSystemService(StatusBarManager::class.java) ?: return
    sbm.requestAddTileService(
        ComponentName(context, MimicToggleTileService::class.java),
        context.getString(R.string.tile_label),
        Icon.createWithResource(context, R.mipmap.ic_launcher),
        context.mainExecutor
    ) { /* result ignored */ }
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
 * Check whether MimicAccessibilityService is active in system accessibility settings.
 * Using system settings instead of MimicAccessibilityService.instance != null avoids
 * transient null state that occurs immediately after a process restart.
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
