package com.mimicease.presentation.ui.profile

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.R
import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.TriggerRepository
import com.mimicease.presentation.ui.common.BlendShapeCategory
import com.mimicease.presentation.ui.common.BLENDSHAPE_DISPLAY_NAMES
import com.mimicease.presentation.ui.common.blendShapeCategory
import com.mimicease.service.FaceDetectionForegroundService
import com.mimicease.service.SwitchAccessBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

// ─── Action display name (locale-aware) ──────────────────────────────────

fun actionDisplayName(action: Action, context: Context): String = when (action) {
    is Action.GlobalHome          -> context.getString(R.string.action_home)
    is Action.GlobalBack          -> context.getString(R.string.action_back)
    is Action.GlobalRecents       -> context.getString(R.string.action_recents)
    is Action.GlobalNotifications -> context.getString(R.string.action_notifications)
    is Action.GlobalQuickSettings -> context.getString(R.string.action_quick_settings)
    is Action.ScreenLock          -> context.getString(R.string.action_screen_lock)
    is Action.TakeScreenshot      -> context.getString(R.string.action_screenshot)
    is Action.PowerDialog         -> context.getString(R.string.action_power_dialog)
    is Action.TapCenter           -> context.getString(R.string.action_tap_center)
    is Action.TapCustom           -> context.getString(R.string.action_tap_custom)
    is Action.DoubleTap           -> context.getString(R.string.action_double_tap)
    is Action.LongPress           -> context.getString(R.string.action_long_press)
    is Action.SwipeUp             -> context.getString(R.string.action_swipe_up)
    is Action.SwipeDown           -> context.getString(R.string.action_swipe_down)
    is Action.SwipeLeft           -> context.getString(R.string.action_swipe_left)
    is Action.SwipeRight          -> context.getString(R.string.action_swipe_right)
    is Action.ScrollUp            -> context.getString(R.string.action_scroll_up)
    is Action.ScrollDown          -> context.getString(R.string.action_scroll_down)
    is Action.Drag                -> context.getString(R.string.action_drag)
    is Action.PinchIn             -> context.getString(R.string.action_pinch_in)
    is Action.PinchOut            -> context.getString(R.string.action_pinch_out)
    is Action.OpenApp             -> context.getString(R.string.action_open_app, action.packageName)
    is Action.MediaPlayPause      -> context.getString(R.string.action_media_play_pause)
    is Action.MediaNext           -> context.getString(R.string.action_media_next)
    is Action.MediaPrev           -> context.getString(R.string.action_media_prev)
    is Action.VolumeUp            -> context.getString(R.string.action_volume_up)
    is Action.VolumeDown          -> context.getString(R.string.action_volume_down)
    is Action.MimicPause          -> context.getString(R.string.action_mimic_pause)
    is Action.TapAtCursor         -> context.getString(R.string.action_tap_at_cursor)
    is Action.DoubleTapAtCursor   -> context.getString(R.string.action_double_tap_at_cursor)
    is Action.LongPressAtCursor   -> context.getString(R.string.action_long_press_at_cursor)
    is Action.DragToggleAtCursor  -> context.getString(R.string.action_drag_toggle_at_cursor)
    is Action.RecenterCursor      -> context.getString(R.string.action_recenter_cursor)
    is Action.SwitchKey           -> context.getString(R.string.action_switch_key, action.label)
    else                          -> context.getString(R.string.action_unknown)
}

// ─── Action category lists ────────────────────────────────────────────────

private val ACTION_SYSTEM = listOf(
    Action.GlobalHome, Action.GlobalBack, Action.GlobalRecents,
    Action.GlobalNotifications, Action.GlobalQuickSettings,
    Action.ScreenLock, Action.TakeScreenshot, Action.PowerDialog
)
private val ACTION_GESTURE = listOf(
    Action.TapCenter, Action.TapCustom(0.5f, 0.5f), Action.DoubleTap(), Action.LongPress(),
    Action.SwipeUp(), Action.SwipeDown(), Action.SwipeLeft(), Action.SwipeRight(),
    Action.ScrollUp, Action.ScrollDown,
    Action.Drag(0.1f, 0.5f, 0.9f, 0.5f), Action.PinchIn, Action.PinchOut
)
private val ACTION_MEDIA = listOf(
    Action.MediaPlayPause, Action.MediaNext, Action.MediaPrev,
    Action.VolumeUp, Action.VolumeDown
)
private val ACTION_CURSOR = listOf(
    Action.TapAtCursor, Action.DoubleTapAtCursor, Action.LongPressAtCursor,
    Action.DragToggleAtCursor, Action.RecenterCursor
)
private val ACTION_SWITCH = SwitchAccessBridge.SUPPORTED_SWITCH_KEYS.map { info ->
    Action.SwitchKey(keyCode = info.keyCode, label = info.label)
}
private val ACTION_OTHER = listOf(Action.MimicPause)

// ─── ViewModel ───────────────────────────────────────────────────────────

data class TriggerEditUiState(
    val triggerId: String? = null,
    val name: String = "",
    val selectedBlendShape: String = "eyeBlinkRight",
    val threshold: Float = 0.5f,
    val holdDurationMs: Int = 200,
    val cooldownMs: Int = 1000,
    val selectedAction: Action = Action.GlobalBack,
    val currentBlendShapeValue: Float = 0f,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false
)

@HiltViewModel
class TriggerEditViewModel @Inject constructor(
    private val triggerRepository: TriggerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val profileId: String = savedStateHandle["profileId"] ?: ""
    private val triggerIdArg: String? = savedStateHandle["triggerId"]

    private val _uiState = MutableStateFlow(TriggerEditUiState())
    val uiState: StateFlow<TriggerEditUiState> = _uiState.asStateFlow()

    init {
        if (!triggerIdArg.isNullOrBlank()) {
            viewModelScope.launch {
                triggerRepository.getTriggersByProfile(profileId).collect { triggers ->
                    val existing = triggers.find { it.id == triggerIdArg }
                    if (existing != null && !_uiState.value.isLoaded) {
                        _uiState.update {
                            it.copy(
                                triggerId = existing.id,
                                name = existing.name,
                                selectedBlendShape = existing.blendShape,
                                threshold = existing.threshold,
                                holdDurationMs = existing.holdDurationMs,
                                cooldownMs = existing.cooldownMs,
                                selectedAction = existing.action,
                                isLoaded = true
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoaded = true) }
        }

        viewModelScope.launch {
            FaceDetectionForegroundService.blendShapeFlow.collect { values ->
                val value = values[_uiState.value.selectedBlendShape] ?: 0f
                _uiState.update { it.copy(currentBlendShapeValue = value) }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateBlendShape(bs: String) = _uiState.update { it.copy(selectedBlendShape = bs) }
    fun updateThreshold(v: Float) = _uiState.update { it.copy(threshold = v) }
    fun updateHoldDuration(ms: Int) = _uiState.update { it.copy(holdDurationMs = ms) }
    fun updateCooldown(ms: Int) = _uiState.update { it.copy(cooldownMs = ms) }
    fun updateAction(action: Action) = _uiState.update { it.copy(selectedAction = action) }

    // autoName is computed in Composable layer (needs Context for locale-aware strings)
    fun saveTrigger(autoName: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.selectedBlendShape.isBlank()) return

        val finalName = state.name.ifBlank { autoName }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val trigger = Trigger(
                id = state.triggerId ?: UUID.randomUUID().toString(),
                profileId = profileId,
                name = finalName,
                blendShape = state.selectedBlendShape,
                threshold = state.threshold,
                holdDurationMs = state.holdDurationMs,
                cooldownMs = state.cooldownMs,
                action = state.selectedAction
            )
            triggerRepository.saveTrigger(trigger)
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}

// ─── UI ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerEditScreen(
    navController: NavController,
    profileId: String,
    preselectedBlendShape: String? = null,
    preselectedThreshold: Float? = null,
    viewModel: TriggerEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showActionPicker by remember { mutableStateOf(false) }
    var showBlendShapePicker by remember { mutableStateOf(false) }

    // Auto-name computed in Composable for locale-aware strings
    val autoName = remember(uiState.selectedBlendShape, uiState.selectedAction) {
        val bsName = BLENDSHAPE_DISPLAY_NAMES[uiState.selectedBlendShape] ?: uiState.selectedBlendShape
        "$bsName → ${actionDisplayName(uiState.selectedAction, context)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.triggerId == null)
                            stringResource(R.string.trigger_edit_add_title)
                        else
                            stringResource(R.string.trigger_edit_edit_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.trigger_edit_back))
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = { viewModel.saveTrigger(autoName) { navController.popBackStack() } }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.trigger_edit_save))
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
            // ① Trigger name
            val namePlaceholder = stringResource(
                R.string.trigger_name_example,
                BLENDSHAPE_DISPLAY_NAMES[uiState.selectedBlendShape] ?: uiState.selectedBlendShape,
                actionDisplayName(uiState.selectedAction, context)
            )
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.trigger_name_label)) },
                placeholder = { Text(namePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ② BlendShape selection
            SectionLabel(stringResource(R.string.trigger_expression_section))
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBlendShapePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = BLENDSHAPE_DISPLAY_NAMES[uiState.selectedBlendShape]
                                ?: uiState.selectedBlendShape,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = uiState.selectedBlendShape,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(stringResource(R.string.trigger_change), color = MaterialTheme.colorScheme.primary)
                }
            }

            // ③ Live preview
            SectionLabel(stringResource(R.string.trigger_live_section))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.trigger_current_value, "%.3f".format(uiState.currentBlendShapeValue)))
                    Text(stringResource(R.string.trigger_threshold_section, "%.2f".format(uiState.threshold)))
                }
                LinearProgressIndicator(
                    progress = { uiState.currentBlendShapeValue.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (uiState.currentBlendShapeValue >= uiState.threshold)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                )
            }

            // ④ Threshold slider
            SectionLabel(stringResource(R.string.trigger_threshold_section, "%.2f".format(uiState.threshold)))
            Slider(
                value = uiState.threshold,
                onValueChange = viewModel::updateThreshold,
                valueRange = 0.1f..1.0f,
                steps = 17
            )

            // ⑤ Hold duration slider
            SectionLabel(stringResource(R.string.trigger_hold_section, uiState.holdDurationMs))
            Slider(
                value = uiState.holdDurationMs.toFloat(),
                onValueChange = { viewModel.updateHoldDuration(it.roundToInt()) },
                valueRange = 0f..2000f,
                steps = 39
            )

            // ⑥ Cooldown slider
            SectionLabel(stringResource(R.string.trigger_cooldown_section, uiState.cooldownMs))
            Slider(
                value = uiState.cooldownMs.toFloat(),
                onValueChange = { viewModel.updateCooldown(it.roundToInt()) },
                valueRange = 100f..5000f,
                steps = 48
            )

            // ⑦ Action selection
            SectionLabel(stringResource(R.string.trigger_action_section))
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showActionPicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = actionDisplayName(uiState.selectedAction, context),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(stringResource(R.string.trigger_change), color = MaterialTheme.colorScheme.primary)
                }
            }

            // ⑧ TapCustom parameters
            AnimatedVisibility(visible = uiState.selectedAction is Action.TapCustom) {
                val action = uiState.selectedAction as? Action.TapCustom
                if (action != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionLabel(stringResource(R.string.trigger_tap_x, "%.2f".format(action.x)))
                        Slider(
                            value = action.x,
                            onValueChange = { viewModel.updateAction(action.copy(x = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel(stringResource(R.string.trigger_tap_y, "%.2f".format(action.y)))
                        Slider(
                            value = action.y,
                            onValueChange = { viewModel.updateAction(action.copy(y = it)) },
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            // ⑨ Drag parameters
            AnimatedVisibility(visible = uiState.selectedAction is Action.Drag) {
                val action = uiState.selectedAction as? Action.Drag
                if (action != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionLabel(stringResource(R.string.trigger_drag_start_x, "%.2f".format(action.startX)))
                        Slider(
                            value = action.startX,
                            onValueChange = { viewModel.updateAction(action.copy(startX = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel(stringResource(R.string.trigger_drag_start_y, "%.2f".format(action.startY)))
                        Slider(
                            value = action.startY,
                            onValueChange = { viewModel.updateAction(action.copy(startY = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel(stringResource(R.string.trigger_drag_end_x, "%.2f".format(action.endX)))
                        Slider(
                            value = action.endX,
                            onValueChange = { viewModel.updateAction(action.copy(endX = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel(stringResource(R.string.trigger_drag_end_y, "%.2f".format(action.endY)))
                        Slider(
                            value = action.endY,
                            onValueChange = { viewModel.updateAction(action.copy(endY = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel(stringResource(R.string.trigger_drag_duration, action.duration.toInt()))
                        Slider(
                            value = action.duration.toFloat(),
                            onValueChange = { viewModel.updateAction(action.copy(duration = it.toLong())) },
                            valueRange = 100f..2000f,
                            steps = 37
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showBlendShapePicker) {
        BlendShapePickerSheet(
            currentBlendShapeValues = emptyMap(),
            currentLiveValues = mapOf(uiState.selectedBlendShape to uiState.currentBlendShapeValue),
            onSelect = { bs ->
                viewModel.updateBlendShape(bs)
                showBlendShapePicker = false
            },
            onDismiss = { showBlendShapePicker = false }
        )
    }

    if (showActionPicker) {
        ActionPickerSheet(
            onSelect = { action ->
                viewModel.updateAction(action)
                showActionPicker = false
            },
            onDismiss = { showActionPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ─── BlendShape picker bottom sheet ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendShapePickerSheet(
    currentBlendShapeValues: Map<String, Float>,
    currentLiveValues: Map<String, Float>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(BlendShapeCategory.ALL) }

    val allBlendShapes = remember {
        BLENDSHAPE_DISPLAY_NAMES.entries.map { it.key to it.value }
    }

    val filteredBlendShapes = remember(selectedCategory) {
        allBlendShapes.filter { (id, _) ->
            selectedCategory == BlendShapeCategory.ALL ||
                blendShapeCategory(id) == selectedCategory
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
            Text(
                text = stringResource(R.string.blendshape_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                edgePadding = 0.dp
            ) {
                BlendShapeCategory.entries.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategory.ordinal == index,
                        onClick = { selectedCategory = category },
                        text = { Text(stringResource(category.labelRes)) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredBlendShapes) { (id, displayName) ->
                    val liveValue = currentLiveValues[id] ?: currentBlendShapeValues[id] ?: 0f
                    ListItem(
                        headlineContent = { Text(displayName) },
                        supportingContent = {
                            Column {
                                Text(id, style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                LinearProgressIndicator(
                                    progress = { liveValue.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        modifier = Modifier.clickable { onSelect(id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// ─── Action picker bottom sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerSheet(
    onSelect: (Action) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tabs = listOf(
        stringResource(R.string.action_tab_system),
        stringResource(R.string.action_tab_gesture),
        stringResource(R.string.action_tab_media),
        stringResource(R.string.action_tab_cursor),
        stringResource(R.string.action_tab_switch),
        stringResource(R.string.action_tab_app),
        stringResource(R.string.action_tab_other)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    val staticActionsByTab = listOf(
        ACTION_SYSTEM, ACTION_GESTURE, ACTION_MEDIA, ACTION_CURSOR, ACTION_SWITCH
    )

    // ModalBottomSheet may pass infinite height on first layout pass.
    // Use heightIn(max = absolute) to guarantee finite max height so weight() works.
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
        ) {
            Text(
                text = stringResource(R.string.action_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    5 -> AppPickerTab(onSelect = onSelect)
                    6 -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(ACTION_OTHER) { action ->
                            ListItem(
                                headlineContent = { Text(actionDisplayName(action, context)) },
                                modifier = Modifier.clickable { onSelect(action) }
                            )
                            HorizontalDivider()
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(staticActionsByTab.getOrNull(selectedTab) ?: emptyList()) { action ->
                            ListItem(
                                headlineContent = { Text(actionDisplayName(action, context)) },
                                modifier = Modifier.clickable { onSelect(action) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerTab(onSelect: (Action) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
                    .mapNotNull { resolve ->
                        val ai = resolve.activityInfo ?: return@mapNotNull null
                        val label = runCatching {
                            pm.getApplicationLabel(ai.applicationInfo).toString()
                        }.getOrDefault(ai.packageName)
                        ai.packageName to label
                    }
                    .distinctBy { it.first }
                    .sortedBy { it.second }
            }.getOrElse { emptyList() }
        }
        apps = result
        isLoading = false
    }

    when {
        isLoading -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        apps.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.action_apps_load_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(apps) { (pkg, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = {
                        Text(
                            text = pkg,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.clickable { onSelect(Action.OpenApp(pkg)) }
                )
                HorizontalDivider()
            }
        }
    }
}
