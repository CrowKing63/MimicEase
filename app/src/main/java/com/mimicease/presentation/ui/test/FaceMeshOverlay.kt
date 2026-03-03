package com.mimicease.presentation.ui.test

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

// MediaPipe Face Landmarker 얼굴 윤곽 연결 인덱스
// FaceLandmarker.FACE_LANDMARKS_FACE_OVAL / LEFT_EYE / RIGHT_EYE / LIPS 등 기반
private val FACE_OVAL_CONNECTIONS = listOf(
    10 to 338, 338 to 297, 297 to 332, 332 to 284, 284 to 251, 251 to 389,
    389 to 356, 356 to 454, 454 to 323, 323 to 361, 361 to 288, 288 to 397,
    397 to 365, 365 to 379, 379 to 378, 378 to 400, 400 to 377, 377 to 152,
    152 to 148, 148 to 176, 176 to 149, 149 to 150, 150 to 136, 136 to 172,
    172 to 58, 58 to 132, 132 to 93, 93 to 234, 234 to 127, 127 to 162,
    162 to 21, 21 to 54, 54 to 103, 103 to 67, 67 to 109, 109 to 10
)

private val LEFT_EYE_CONNECTIONS = listOf(
    263 to 249, 249 to 390, 390 to 373, 373 to 374, 374 to 380, 380 to 381,
    381 to 382, 382 to 362, 362 to 398, 398 to 384, 384 to 385, 385 to 386,
    386 to 387, 387 to 388, 388 to 466, 466 to 263
)

private val RIGHT_EYE_CONNECTIONS = listOf(
    33 to 7, 7 to 163, 163 to 144, 144 to 145, 145 to 153, 153 to 154,
    154 to 155, 155 to 133, 133 to 173, 173 to 157, 157 to 158, 158 to 159,
    159 to 160, 160 to 161, 161 to 246, 246 to 33
)

private val LIPS_CONNECTIONS = listOf(
    61 to 185, 185 to 40, 40 to 39, 39 to 37, 37 to 0, 0 to 267, 267 to 269,
    269 to 270, 270 to 409, 409 to 291, 61 to 146, 146 to 91, 91 to 181,
    181 to 84, 84 to 17, 17 to 314, 314 to 405, 405 to 321, 321 to 375,
    375 to 291
)

private val LEFT_EYEBROW_CONNECTIONS = listOf(
    276 to 283, 283 to 282, 282 to 295, 295 to 285, 300 to 293,
    293 to 334, 334 to 296, 296 to 336
)

private val RIGHT_EYEBROW_CONNECTIONS = listOf(
    46 to 53, 53 to 52, 52 to 65, 65 to 55, 70 to 63,
    63 to 105, 105 to 66, 66 to 107
)

@Composable
fun FaceMeshOverlay(
    landmarks: List<NormalizedLandmark>,
    modifier: Modifier = Modifier,
    meshAlpha: Float = 0.6f
) {
    if (landmarks.isEmpty()) return

    val dotColor = Color(0xFF00E676).copy(alpha = meshAlpha)
    val lineColor = Color(0xFF00BCD4).copy(alpha = meshAlpha * 0.8f)
    val ovalColor = Color(0xFF69F0AE).copy(alpha = meshAlpha)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun landmark(index: Int): Offset? {
            if (index >= landmarks.size) return null
            val lm = landmarks[index]
            return Offset(lm.x() * w, lm.y() * h)
        }

        fun drawConnections(connections: List<Pair<Int, Int>>, color: Color, strokeWidth: Float = 1.5f) {
            for ((a, b) in connections) {
                val p1 = landmark(a) ?: continue
                val p2 = landmark(b) ?: continue
                drawLine(color = color, start = p1, end = p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
        }

        // 얼굴 윤곽
        drawConnections(FACE_OVAL_CONNECTIONS, ovalColor, strokeWidth = 2f)
        // 눈
        drawConnections(LEFT_EYE_CONNECTIONS, lineColor)
        drawConnections(RIGHT_EYE_CONNECTIONS, lineColor)
        // 입술
        drawConnections(LIPS_CONNECTIONS, lineColor)
        // 눈썹
        drawConnections(LEFT_EYEBROW_CONNECTIONS, lineColor)
        drawConnections(RIGHT_EYEBROW_CONNECTIONS, lineColor)

        // 핵심 랜드마크 점 (코, 눈 끝, 턱 등 주요 28개)
        val keyIndices = listOf(
            1, 4, 5, 6, 195, 197,       // 코
            33, 133, 263, 362,           // 눈 안쪽/바깥쪽
            70, 105, 300, 334,           // 눈썹
            61, 291, 0, 17,             // 입 꼭짓점
            152, 10,                    // 턱, 이마
            234, 454,                   // 볼
            21, 251                     // 관자놀이
        )
        for (idx in keyIndices) {
            val pt = landmark(idx) ?: continue
            drawCircle(color = dotColor, radius = 3f, center = pt)
        }
    }
}
