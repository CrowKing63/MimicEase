package com.mimicease.presentation.ui.test

import androidx.camera.view.PreviewView
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.mimicease.R
import com.mimicease.presentation.ui.common.BlendShapeCategory
import com.mimicease.presentation.ui.common.BLENDSHAPE_DISPLAY_NAMES
import com.mimicease.presentation.ui.common.blendShapeCategory
import com.mimicease.presentation.ui.home.MimicBottomNavigation
import com.mimicease.service.FaceDetectionForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────

data class ExpressionTestUiState(
    val blendShapeValues: Map<String, Float> = emptyMap(),
    val selectedCategory: BlendShapeCategory = BlendShapeCategory.ALL,
    val topExpressions: List<Pair<String, Float>> = emptyList(),
    val isFaceVisible: Boolean = false,
    val inferenceTimeMs: Long = 0L,
    val faceLandmarks: List<NormalizedLandmark> = emptyList(),
    val imageSize: Pair<Int, Int> = Pair(0, 0)
)

@HiltViewModel
class ExpressionTestViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExpressionTestUiState())
    val uiState: StateFlow<ExpressionTestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            FaceDetectionForegroundService.blendShapeFlow
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
        viewModelScope.launch {
            FaceDetectionForegroundService.faceLandmarksFlow.collect { landmarks ->
                _uiState.update { it.copy(faceLandmarks = landmarks) }
            }
        }
        viewModelScope.launch {
            FaceDetectionForegroundService.imageSizeFlow.collect { size ->
                _uiState.update { it.copy(imageSize = size) }
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

    val filteredBlendShapes = remember(uiState.blendShapeValues, uiState.selectedCategory) {
        uiState.blendShapeValues.entries
            .filter { (name, _) ->
                uiState.selectedCategory == BlendShapeCategory.ALL ||
                    blendShapeCategory(name) == uiState.selectedCategory
            }
            .sortedBy { (name, _) -> BLENDSHAPE_DISPLAY_NAMES[name] ?: name }
    }

    val topBlendShape = uiState.topExpressions.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.test_title)) })
        },
        bottomBar = { MimicBottomNavigation(navController) },
        floatingActionButton = {
            if (topBlendShape != null) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.test_create_trigger_fab)) },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = { navController.navigate("profiles") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Camera feed + mesh overlay (top 1/3 of screen)
            CameraPreviewWithMesh(
                landmarks = uiState.faceLandmarks,
                imageSize = uiState.imageSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.33f)
            )

            // Face detection status banner
            FaceStatusBanner(
                isFaceVisible = uiState.isFaceVisible,
                inferenceTimeMs = uiState.inferenceTimeMs
            )

            // Top 3 expressions summary
            if (uiState.topExpressions.isNotEmpty()) {
                TopExpressionsRow(expressions = uiState.topExpressions)
            }

            // Category tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedCategory.ordinal,
                edgePadding = 0.dp
            ) {
                BlendShapeCategory.entries.forEachIndexed { index, category ->
                    Tab(
                        selected = uiState.selectedCategory.ordinal == index,
                        onClick = { viewModel.selectCategory(category) },
                        text = { Text(stringResource(category.labelRes)) }
                    )
                }
            }

            // BlendShape list
            if (uiState.blendShapeValues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.test_start_service_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
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

// ─── Camera preview + mesh overlay ───────────────────────────────────────

@Composable
private fun CameraPreviewWithMesh(
    landmarks: List<NormalizedLandmark>,
    imageSize: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    // clipToBounds(): FaceMeshOverlay Canvas coordinates from FILL_CENTER transform can exceed
    // view height. Compose Canvas doesn't clip by default so must be explicit.
    Box(modifier = modifier.background(Color.Black).clipToBounds()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { previewView ->
                FaceDetectionForegroundService.attachPreviewSurfaceProvider(previewView.surfaceProvider)
            },
            onRelease = {
                FaceDetectionForegroundService.detachPreviewSurfaceProvider()
            },
            modifier = Modifier.fillMaxSize()
        )

        if (landmarks.isNotEmpty()) {
            FaceMeshOverlay(
                landmarks = landmarks,
                imageSize = imageSize,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────

@Composable
private fun FaceStatusBanner(isFaceVisible: Boolean, inferenceTimeMs: Long) {
    val (color, text) = if (isFaceVisible) {
        MaterialTheme.colorScheme.primaryContainer to
            stringResource(R.string.test_face_detecting, inferenceTimeMs)
    } else {
        MaterialTheme.colorScheme.errorContainer to
            stringResource(R.string.test_no_face)
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
    val slots = (expressions + List(3) { null }).take(3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        slots.forEachIndexed { index, item ->
            val rank = index + 1
            Box(modifier = Modifier.weight(1f)) {
                if (item != null) {
                    val (name, value) = item
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (rank) {
                                1 -> MaterialTheme.colorScheme.primaryContainer
                                2 -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BLENDSHAPE_DISPLAY_NAMES[name] ?: name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "%.2f".format(value),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = when (rank) {
                                    1 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "#$rank", style = MaterialTheme.typography.labelSmall)
                            Text(text = "—", style = MaterialTheme.typography.labelSmall)
                            Text(text = "—", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
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

            Text(
                text = "%.2f".format(animatedValue),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(48.dp),
                color = if (isAboveThreshold) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )

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

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.test_threshold_preview, "%.2f".format(previewThreshold)),
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
                    text = stringResource(R.string.test_threshold_exceeded),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
