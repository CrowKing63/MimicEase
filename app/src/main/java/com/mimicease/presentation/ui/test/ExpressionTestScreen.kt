package com.mimicease.presentation.ui.test

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.presentation.ui.home.MimicBottomNavigation
import com.mimicease.service.FaceDetectionForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── BlendShape 카테고리 분류 ────────────────────────────────────────────

enum class BlendShapeCategory(val label: String) {
    ALL("전체"),
    EYES("눈"),
    BROWS("눈썹"),
    MOUTH("입/턱"),
    CHEEK("볼/코")
}

// BlendShape 이름을 카테고리별로 분류
private fun blendShapeCategory(name: String): BlendShapeCategory = when {
    name.startsWith("eyeBlink") || name.startsWith("eyeWide") ||
    name.startsWith("eyeSquint") || name.startsWith("eyeLook") -> BlendShapeCategory.EYES

    name.startsWith("brow") -> BlendShapeCategory.BROWS

    name.startsWith("mouth") || name.startsWith("jaw") ||
    name == "tongueOut" -> BlendShapeCategory.MOUTH

    name.startsWith("cheek") || name.startsWith("noseSneer") -> BlendShapeCategory.CHEEK

    else -> BlendShapeCategory.ALL
}

// 사람이 읽기 쉬운 표정 이름 매핑
private val BLENDSHAPE_DISPLAY_NAMES = mapOf(
    "eyeBlinkLeft" to "눈 깜빡임 (왼쪽)",
    "eyeBlinkRight" to "눈 깜빡임 (오른쪽)",
    "eyeWideLeft" to "눈 크게 (왼쪽)",
    "eyeWideRight" to "눈 크게 (오른쪽)",
    "eyeSquintLeft" to "눈 찡그림 (왼쪽)",
    "eyeSquintRight" to "눈 찡그림 (오른쪽)",
    "eyeLookUpLeft" to "눈 위로 (왼쪽)",
    "eyeLookUpRight" to "눈 위로 (오른쪽)",
    "eyeLookDownLeft" to "눈 아래로 (왼쪽)",
    "eyeLookDownRight" to "눈 아래로 (오른쪽)",
    "eyeLookInLeft" to "눈 안쪽 (왼쪽)",
    "eyeLookInRight" to "눈 안쪽 (오른쪽)",
    "eyeLookOutLeft" to "눈 바깥쪽 (왼쪽)",
    "eyeLookOutRight" to "눈 바깥쪽 (오른쪽)",
    "browInnerUp" to "눈썹 안쪽 올리기",
    "browOuterUpLeft" to "눈썹 바깥쪽 올리기 (왼)",
    "browOuterUpRight" to "눈썹 바깥쪽 올리기 (오)",
    "browDownLeft" to "눈썹 내리기 (왼쪽)",
    "browDownRight" to "눈썹 내리기 (오른쪽)",
    "jawOpen" to "입 벌리기",
    "jawLeft" to "턱 왼쪽",
    "jawRight" to "턱 오른쪽",
    "jawForward" to "턱 앞으로",
    "mouthSmileLeft" to "미소 (왼쪽)",
    "mouthSmileRight" to "미소 (오른쪽)",
    "mouthFrownLeft" to "입꼬리 내리기 (왼)",
    "mouthFrownRight" to "입꼬리 내리기 (오)",
    "mouthPucker" to "입술 오므리기",
    "mouthFunnel" to "입 모으기",
    "mouthLeft" to "입 왼쪽",
    "mouthRight" to "입 오른쪽",
    "mouthRollLower" to "아랫입술 말기",
    "mouthRollUpper" to "윗입술 말기",
    "mouthShrugLower" to "아랫입술 올리기",
    "mouthShrugUpper" to "윗입술 올리기",
    "mouthClose" to "입 다물기",
    "mouthUpperUpLeft" to "윗입술 위로 (왼)",
    "mouthUpperUpRight" to "윗입술 위로 (오)",
    "mouthLowerDownLeft" to "아랫입술 아래로 (왼)",
    "mouthLowerDownRight" to "아랫입술 아래로 (오)",
    "mouthDimpleLeft" to "보조개 (왼쪽)",
    "mouthDimpleRight" to "보조개 (오른쪽)",
    "mouthPressLeft" to "입술 누르기 (왼)",
    "mouthPressRight" to "입술 누르기 (오)",
    "mouthStretchLeft" to "입 늘리기 (왼쪽)",
    "mouthStretchRight" to "입 늘리기 (오른쪽)",
    "cheekPuff" to "볼 부풀리기",
    "cheekSquintLeft" to "볼 찡그림 (왼쪽)",
    "cheekSquintRight" to "볼 찡그림 (오른쪽)",
    "noseSneerLeft" to "코 찡그림 (왼쪽)",
    "noseSneerRight" to "코 찡그림 (오른쪽)"
)

// ─── ViewModel ───────────────────────────────────────────────────────────

data class ExpressionTestUiState(
    val blendShapeValues: Map<String, Float> = emptyMap(),
    val selectedCategory: BlendShapeCategory = BlendShapeCategory.ALL,
    val topExpressions: List<Pair<String, Float>> = emptyList(),
    val isFaceVisible: Boolean = false,
    val inferenceTimeMs: Long = 0L
)

@HiltViewModel
class ExpressionTestViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExpressionTestUiState())
    val uiState: StateFlow<ExpressionTestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            FaceDetectionForegroundService.blendShapeFlow
                .flowOn(Dispatchers.Default)
                .collect { values ->
                    val top3 = values.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key to it.value }
                    _uiState.update { it.copy(blendShapeValues = values, topExpressions = top3) }
                }
        }
        viewModelScope.launch {
            FaceDetectionForegroundService.isFaceVisible.collect { visible ->
                _uiState.update { it.copy(isFaceVisible = visible) }
            }
        }
        viewModelScope.launch {
            FaceDetectionForegroundService.inferenceTimeMs.collect { ms ->
                _uiState.update { it.copy(inferenceTimeMs = ms) }
            }
        }
    }

    fun selectCategory(category: BlendShapeCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}

// ─── UI ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressionTestScreen(
    navController: NavController,
    viewModel: ExpressionTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 현재 카테고리에 따라 필터링된 blendshape 목록
    val filteredBlendShapes = remember(uiState.blendShapeValues, uiState.selectedCategory) {
        uiState.blendShapeValues.entries
            .filter { (name, _) ->
                uiState.selectedCategory == BlendShapeCategory.ALL ||
                    blendShapeCategory(name) == uiState.selectedCategory
            }
            .sortedByDescending { it.value }
    }

    val topBlendShape = uiState.topExpressions.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("표정 테스트") })
        },
        bottomBar = { MimicBottomNavigation(navController) },
        floatingActionButton = {
            if (topBlendShape != null) {
                ExtendedFloatingActionButton(
                    text = { Text("이 표정으로 트리거 만들기") },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = {
                        val bs = topBlendShape.first
                        val threshold = (topBlendShape.second * 0.85f).coerceIn(0.1f, 0.95f)
                        navController.navigate("profiles")
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 얼굴 감지 상태 표시 ─────────────────────
            FaceStatusBanner(
                isFaceVisible = uiState.isFaceVisible,
                inferenceTimeMs = uiState.inferenceTimeMs
            )

            // ── 상위 3개 표정 요약 ─────────────────────
            if (uiState.topExpressions.isNotEmpty()) {
                TopExpressionsRow(expressions = uiState.topExpressions)
            }

            // ── 카테고리 탭 ──────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedCategory.ordinal,
                edgePadding = 0.dp
            ) {
                BlendShapeCategory.entries.forEachIndexed { index, category ->
                    Tab(
                        selected = uiState.selectedCategory.ordinal == index,
                        onClick = { viewModel.selectCategory(category) },
                        text = { Text(category.label) }
                    )
                }
            }

            // ── BlendShape 목록 ──────────────────────────
            if (uiState.blendShapeValues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "카메라 서비스를 실행해주세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredBlendShapes, key = { it.key }) { (name, value) ->
                        BlendShapeRow(name = name, value = value)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceStatusBanner(isFaceVisible: Boolean, inferenceTimeMs: Long) {
    val (color, text) = if (isFaceVisible) {
        MaterialTheme.colorScheme.primaryContainer to "얼굴 감지 중 • 추론: ${inferenceTimeMs}ms"
    } else {
        MaterialTheme.colorScheme.errorContainer to "얼굴이 감지되지 않습니다"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFaceVisible)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun TopExpressionsRow(expressions: List<Pair<String, Float>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        expressions.forEach { (name, value) ->
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = "${BLENDSHAPE_DISPLAY_NAMES[name] ?: name}: ${"%.2f".format(value)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun BlendShapeRow(name: String, value: Float) {
    var expanded by remember { mutableStateOf(false) }
    var previewThreshold by remember { mutableFloatStateOf(0.5f) }

    val animatedValue by animateFloatAsState(targetValue = value, label = "blendshapeAnim")
    val isAboveThreshold = animatedValue >= previewThreshold
    val barColor = if (isAboveThreshold) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이름
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BLENDSHAPE_DISPLAY_NAMES[name] ?: name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isAboveThreshold) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // 수치
            Text(
                text = "%.2f".format(animatedValue),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(48.dp),
                color = if (isAboveThreshold) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )

            // 게이지 바
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedValue.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor)
                )
                // 임계값 마커 (expanded 상태에서만)
                if (expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .offset(x = (previewThreshold * 120).dp - 1.dp)
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }
        }

        // 펼침: 임계값 슬라이더
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "임계값 미리보기: ${"%.2f".format(previewThreshold)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = previewThreshold,
                onValueChange = { previewThreshold = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
            if (isAboveThreshold) {
                Text(
                    text = "✓ 현재 값이 임계값을 초과합니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
