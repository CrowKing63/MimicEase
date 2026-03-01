package com.mimicease.presentation.ui.profile

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.TriggerRepository
import com.mimicease.service.FaceDetectionForegroundService
import com.mimicease.service.SwitchAccessBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

// ─── 액션 표시 이름 ─────────────────────────────────────────────────────

fun actionDisplayName(action: Action): String = when (action) {
    is Action.GlobalHome          -> "홈 버튼"
    is Action.GlobalBack          -> "뒤로가기"
    is Action.GlobalRecents       -> "최근 앱"
    is Action.GlobalNotifications -> "알림 패널"
    is Action.GlobalQuickSettings -> "빠른 설정"
    is Action.ScreenLock          -> "화면 잠금"
    is Action.TakeScreenshot      -> "스크린샷"
    is Action.PowerDialog         -> "전원 메뉴"
    is Action.TapCenter           -> "화면 중앙 탭"
    is Action.TapCustom           -> "커스텀 탭"
    is Action.DoubleTap           -> "더블 탭"
    is Action.LongPress           -> "길게 누르기"
    is Action.SwipeUp             -> "위로 스와이프"
    is Action.SwipeDown           -> "아래로 스와이프"
    is Action.SwipeLeft           -> "왼쪽으로 스와이프"
    is Action.SwipeRight          -> "오른쪽으로 스와이프"
    is Action.ScrollUp            -> "위로 스크롤"
    is Action.ScrollDown          -> "아래로 스크롤"
    is Action.Drag                -> "드래그"
    is Action.PinchIn             -> "핀치 인 (축소)"
    is Action.PinchOut            -> "핀치 아웃 (확대)"
    is Action.OpenApp             -> "앱 열기: ${action.packageName}"
    is Action.MediaPlayPause      -> "재생/일시정지"
    is Action.MediaNext           -> "다음 곡"
    is Action.MediaPrev           -> "이전 곡"
    is Action.VolumeUp            -> "볼륨 올리기"
    is Action.VolumeDown          -> "볼륨 내리기"
    is Action.MimicPause          -> "MimicEase 일시정지"
    is Action.TapAtCursor         -> "커서 위치 탭"
    is Action.DoubleTapAtCursor   -> "커서 위치 더블탭"
    is Action.LongPressAtCursor   -> "커서 위치 길게 누르기"
    is Action.DragStartAtCursor   -> "커서 위치 드래그 시작"
    is Action.DragEndAtCursor     -> "커서 위치 드래그 종료"
    is Action.SwitchKey           -> "스위치 입력: ${action.label}"
    else                          -> "알 수 없는 액션"
}

// 액션 카테고리
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
    Action.DragStartAtCursor, Action.DragEndAtCursor
)
private val ACTION_SWITCH = SwitchAccessBridge.SUPPORTED_SWITCH_KEYS.map { info ->
    Action.SwitchKey(keyCode = info.keyCode, label = info.label)
}
private val ACTION_OTHER = listOf(Action.MimicPause)

// BlendShape 정보 (MediaPipe Face Landmarker 52개 전체)
private val ALL_BLENDSHAPES = listOf(
    // 눈 깜빡임 / 크기
    "eyeBlinkLeft"       to "눈 깜빡임 (왼)",
    "eyeBlinkRight"      to "눈 깜빡임 (오)",
    "eyeWideLeft"        to "눈 크게 (왼)",
    "eyeWideRight"       to "눈 크게 (오)",
    "eyeSquintLeft"      to "눈 찡그림 (왼)",
    "eyeSquintRight"     to "눈 찡그림 (오)",
    // 눈 시선
    "eyeLookUpLeft"      to "눈 위로 (왼)",
    "eyeLookUpRight"     to "눈 위로 (오)",
    "eyeLookDownLeft"    to "눈 아래로 (왼)",
    "eyeLookDownRight"   to "눈 아래로 (오)",
    "eyeLookInLeft"      to "눈 안쪽 (왼)",
    "eyeLookInRight"     to "눈 안쪽 (오)",
    "eyeLookOutLeft"     to "눈 바깥쪽 (왼)",
    "eyeLookOutRight"    to "눈 바깥쪽 (오)",
    // 눈썹
    "browInnerUp"        to "눈썹 안쪽 올리기",
    "browOuterUpLeft"    to "눈썹 바깥 올리기 (왼)",
    "browOuterUpRight"   to "눈썹 바깥 올리기 (오)",
    "browDownLeft"       to "눈썹 내리기 (왼)",
    "browDownRight"      to "눈썹 내리기 (오)",
    // 턱
    "jawOpen"            to "입 벌리기",
    "jawLeft"            to "턱 왼쪽",
    "jawRight"           to "턱 오른쪽",
    "jawForward"         to "턱 앞으로",
    // 미소 / 입꼬리
    "mouthSmileLeft"     to "미소 (왼)",
    "mouthSmileRight"    to "미소 (오)",
    "mouthFrownLeft"     to "입꼬리 내리기 (왼)",
    "mouthFrownRight"    to "입꼬리 내리기 (오)",
    // 입 모양
    "mouthPucker"        to "입술 오므리기",
    "mouthFunnel"        to "입 모으기",
    "mouthLeft"          to "입 왼쪽",
    "mouthRight"         to "입 오른쪽",
    "mouthClose"         to "입 다물기",
    // 입술 말기 / 올리기
    "mouthRollLower"     to "아랫입술 말기",
    "mouthRollUpper"     to "윗입술 말기",
    "mouthShrugLower"    to "아랫입술 올리기",
    "mouthShrugUpper"    to "윗입술 올리기",
    // 입술 세부
    "mouthUpperUpLeft"   to "윗입술 위로 (왼)",
    "mouthUpperUpRight"  to "윗입술 위로 (오)",
    "mouthLowerDownLeft" to "아랫입술 아래로 (왼)",
    "mouthLowerDownRight" to "아랫입술 아래로 (오)",
    "mouthDimpleLeft"    to "보조개 (왼)",
    "mouthDimpleRight"   to "보조개 (오)",
    "mouthPressLeft"     to "입술 누르기 (왼)",
    "mouthPressRight"    to "입술 누르기 (오)",
    "mouthStretchLeft"   to "입 늘리기 (왼)",
    "mouthStretchRight"  to "입 늘리기 (오)",
    // 볼 / 코
    "cheekPuff"          to "볼 부풀리기",
    "cheekSquintLeft"    to "볼 찡그림 (왼)",
    "cheekSquintRight"   to "볼 찡그림 (오)",
    "noseSneerLeft"      to "코 찡그림 (왼)",
    "noseSneerRight"     to "코 찡그림 (오)",
    // 혀
    "tongueOut"          to "혀 내밀기"
)

// ─── ViewModel ───────────────────────────────────────────────────────────

data class TriggerEditUiState(
    val triggerId: String? = null,   // null = 신규 생성
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
        // 기존 트리거 로드 (triggerId 인수가 있을 경우)
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

        // 실시간 블렌드쉐이프 값 관찰
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

    fun saveTrigger(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank() || state.selectedBlendShape.isBlank()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val trigger = Trigger(
                id = state.triggerId ?: UUID.randomUUID().toString(),
                profileId = profileId,
                name = state.name,
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
    var showActionPicker by remember { mutableStateOf(false) }
    var showBlendShapePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.triggerId == null) "트리거 추가" else "트리거 편집") },
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
                            onClick = { viewModel.saveTrigger { navController.popBackStack() } },
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
            // ① 트리거 이름
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("트리거 이름") },
                placeholder = { Text("예: 오른쪽 윙크 → 뒤로가기") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ② 표정(BlendShape) 선택
            SectionLabel("표정 선택")
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
                            text = ALL_BLENDSHAPES.find { it.first == uiState.selectedBlendShape }?.second
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
                    Text("변경 >", color = MaterialTheme.colorScheme.primary)
                }
            }

            // ③ 실시간 미리보기
            SectionLabel("실시간 현재값")
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("현재: ${"%.3f".format(uiState.currentBlendShapeValue)}")
                    Text("임계값: ${"%.2f".format(uiState.threshold)}")
                }
                LinearProgressIndicator(
                    progress = { uiState.currentBlendShapeValue.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (uiState.currentBlendShapeValue >= uiState.threshold)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                )
            }

            // ④ 임계값 슬라이더
            SectionLabel("임계값: ${"%.2f".format(uiState.threshold)}")
            Slider(
                value = uiState.threshold,
                onValueChange = viewModel::updateThreshold,
                valueRange = 0.1f..1.0f,
                steps = 17
            )

            // ⑤ 유지 시간 슬라이더
            SectionLabel("유지 시간 (홀드): ${uiState.holdDurationMs}ms")
            Slider(
                value = uiState.holdDurationMs.toFloat(),
                onValueChange = { viewModel.updateHoldDuration(it.roundToInt()) },
                valueRange = 0f..2000f,
                steps = 39
            )

            // ⑥ 쿨다운 슬라이더
            SectionLabel("쿨다운: ${uiState.cooldownMs}ms")
            Slider(
                value = uiState.cooldownMs.toFloat(),
                onValueChange = { viewModel.updateCooldown(it.roundToInt()) },
                valueRange = 100f..5000f,
                steps = 48
            )

            // ⑦ 액션 선택
            SectionLabel("수행할 액션")
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
                        text = actionDisplayName(uiState.selectedAction),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text("변경 >", color = MaterialTheme.colorScheme.primary)
                }
            }

            // ⑧ 파라미터 편집 (TapCustom)
            AnimatedVisibility(visible = uiState.selectedAction is Action.TapCustom) {
                val action = uiState.selectedAction as? Action.TapCustom
                if (action != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionLabel("탭 위치 X: ${"%.2f".format(action.x)}  (왼쪽 0.0 → 오른쪽 1.0)")
                        Slider(
                            value = action.x,
                            onValueChange = { viewModel.updateAction(action.copy(x = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel("탭 위치 Y: ${"%.2f".format(action.y)}  (위 0.0 → 아래 1.0)")
                        Slider(
                            value = action.y,
                            onValueChange = { viewModel.updateAction(action.copy(y = it)) },
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            // ⑧ 파라미터 편집 (Drag)
            AnimatedVisibility(visible = uiState.selectedAction is Action.Drag) {
                val action = uiState.selectedAction as? Action.Drag
                if (action != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionLabel("시작 X: ${"%.2f".format(action.startX)}")
                        Slider(
                            value = action.startX,
                            onValueChange = { viewModel.updateAction(action.copy(startX = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel("시작 Y: ${"%.2f".format(action.startY)}")
                        Slider(
                            value = action.startY,
                            onValueChange = { viewModel.updateAction(action.copy(startY = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel("끝 X: ${"%.2f".format(action.endX)}")
                        Slider(
                            value = action.endX,
                            onValueChange = { viewModel.updateAction(action.copy(endX = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel("끝 Y: ${"%.2f".format(action.endY)}")
                        Slider(
                            value = action.endY,
                            onValueChange = { viewModel.updateAction(action.copy(endY = it)) },
                            valueRange = 0f..1f
                        )
                        SectionLabel("드래그 시간: ${action.duration}ms")
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

// ─── BlendShape 선택 바텀시트 ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendShapePickerSheet(
    currentBlendShapeValues: Map<String, Float>,
    currentLiveValues: Map<String, Float>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "표정 선택",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(ALL_BLENDSHAPES) { (id, displayName) ->
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

// ─── 액션 선택 바텀시트 ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerSheet(
    onSelect: (Action) -> Unit,
    onDismiss: () -> Unit
) {
    val tabs = listOf("시스템", "제스처", "미디어", "커서", "스위치", "앱", "기타")
    var selectedTab by remember { mutableIntStateOf(0) }

    val staticActionsByTab = listOf(
        ACTION_SYSTEM, ACTION_GESTURE, ACTION_MEDIA, ACTION_CURSOR, ACTION_SWITCH
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Text(
            text = "액션 선택",
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
        when (selectedTab) {
            5 -> AppPickerTab(onSelect = onSelect)  // 앱
            6 -> LazyColumn(                         // 기타
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(ACTION_OTHER) { action ->
                    ListItem(
                        headlineContent = { Text(actionDisplayName(action)) },
                        modifier = Modifier.clickable { onSelect(action) }
                    )
                    HorizontalDivider()
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(staticActionsByTab[selectedTab]) { action ->
                    ListItem(
                        headlineContent = { Text(actionDisplayName(action)) },
                        modifier = Modifier.clickable { onSelect(action) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppPickerTab(onSelect: (Action) -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to pm.getApplicationLabel(it.activityInfo.applicationInfo).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }
    LazyColumn(
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
