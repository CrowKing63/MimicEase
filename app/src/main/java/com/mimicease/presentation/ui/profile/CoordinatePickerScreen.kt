package com.mimicease.presentation.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private fun modeTitle(mode: String) = when (mode) {
    "tapCustom" -> "탭 위치 선택"
    "dragStart" -> "드래그 시작 위치"
    "dragEnd"   -> "드래그 끝 위치"
    else         -> "좌표 선택"
}

/**
 * 전체 화면에서 탭/드래그로 좌표(0.0~1.0 정규화)를 선택하는 화면.
 *
 * 결과는 [navController.previousBackStackEntry.savedStateHandle]에 저장됨:
 *   - "pickerX" : Float
 *   - "pickerY" : Float
 *   - "pickerMode" : String  ("tapCustom" | "dragStart" | "dragEnd")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinatePickerScreen(
    navController: NavController,
    mode: String,
    initialXStr: String,
    initialYStr: String
) {
    val initialX = initialXStr.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
    val initialY = initialYStr.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f

    var pickedX by remember { mutableStateOf(initialX) }
    var pickedY by remember { mutableStateOf(initialY) }

    fun confirm() {
        navController.previousBackStackEntry?.savedStateHandle?.apply {
            set("pickerX", pickedX)
            set("pickerY", pickedY)
            set("pickerMode", mode)
        }
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(modeTitle(mode)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "취소")
                    }
                },
                actions = {
                    IconButton(onClick = { confirm() }) {
                        Icon(Icons.Filled.Check, contentDescription = "확인")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val accent = MaterialTheme.colorScheme.primary
            val accentAlpha = accent.copy(alpha = 0.15f)
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            // 첫 터치 다운
                            val first = awaitFirstDown(requireUnconsumed = false)
                            pickedX = (first.position.x / size.width).coerceIn(0f, 1f)
                            pickedY = (first.position.y / size.height).coerceIn(0f, 1f)
                            first.consume()
                            // 드래그 추적
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == first.id } ?: break
                                if (!change.pressed) break
                                pickedX = (change.position.x / size.width).coerceIn(0f, 1f)
                                pickedY = (change.position.y / size.height).coerceIn(0f, 1f)
                                change.consume()
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))

                // 격자선 (1/4 분할)
                for (i in 1..3) {
                    drawLine(gridColor, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), pathEffect = dashEffect)
                    drawLine(gridColor, Offset(w * i / 4f, 0f), Offset(w * i / 4f, h), pathEffect = dashEffect)
                }

                // 크로스헤어
                val cx = pickedX * w
                val cy = pickedY * h
                val arm = 60f
                val strokeW = 3f

                drawLine(accent, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(accent, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawCircle(color = accent, radius = 20f, center = Offset(cx, cy), style = Stroke(width = strokeW))
                drawCircle(color = accentAlpha, radius = 20f, center = Offset(cx, cy))
            }

            // 좌표 표시
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "X: ${"%.3f".format(pickedX)}  |  Y: ${"%.3f".format(pickedY)}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
