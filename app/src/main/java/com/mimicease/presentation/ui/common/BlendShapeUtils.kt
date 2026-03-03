package com.mimicease.presentation.ui.common

// ─── BlendShape 카테고리 분류 ────────────────────────────────────────────

enum class BlendShapeCategory(val label: String) {
    ALL("전체"),
    EYES("눈"),
    BROWS("눈썹"),
    MOUTH("입/턱"),
    CHEEK("볼/코"),
    TONGUE("혀")
}

fun blendShapeCategory(name: String): BlendShapeCategory = when {
    name.startsWith("eyeBlink") || name.startsWith("eyeWide") ||
    name.startsWith("eyeSquint") || name.startsWith("eyeLook") -> BlendShapeCategory.EYES

    name.startsWith("brow") -> BlendShapeCategory.BROWS

    name.startsWith("mouth") || name.startsWith("jaw") -> BlendShapeCategory.MOUTH

    name.startsWith("cheek") || name.startsWith("noseSneer") -> BlendShapeCategory.CHEEK

    name == "tongueOut" -> BlendShapeCategory.TONGUE

    else -> BlendShapeCategory.ALL
}

// 사람이 읽기 쉬운 표정 이름 매핑 (52개 전체 — 정규 출처)
val BLENDSHAPE_DISPLAY_NAMES: Map<String, String> = mapOf(
    "eyeBlinkLeft"        to "눈 깜빡임 (왼쪽)",
    "eyeBlinkRight"       to "눈 깜빡임 (오른쪽)",
    "eyeWideLeft"         to "눈 크게 (왼쪽)",
    "eyeWideRight"        to "눈 크게 (오른쪽)",
    "eyeSquintLeft"       to "눈 찡그림 (왼쪽)",
    "eyeSquintRight"      to "눈 찡그림 (오른쪽)",
    "eyeLookUpLeft"       to "눈 위로 (왼쪽)",
    "eyeLookUpRight"      to "눈 위로 (오른쪽)",
    "eyeLookDownLeft"     to "눈 아래로 (왼쪽)",
    "eyeLookDownRight"    to "눈 아래로 (오른쪽)",
    "eyeLookInLeft"       to "눈 안쪽 (왼쪽)",
    "eyeLookInRight"      to "눈 안쪽 (오른쪽)",
    "eyeLookOutLeft"      to "눈 바깥쪽 (왼쪽)",
    "eyeLookOutRight"     to "눈 바깥쪽 (오른쪽)",
    "browInnerUp"         to "눈썹 안쪽 올리기",
    "browOuterUpLeft"     to "눈썹 바깥쪽 올리기 (왼)",
    "browOuterUpRight"    to "눈썹 바깥쪽 올리기 (오)",
    "browDownLeft"        to "눈썹 내리기 (왼쪽)",
    "browDownRight"       to "눈썹 내리기 (오른쪽)",
    "jawOpen"             to "입 벌리기",
    "jawLeft"             to "턱 왼쪽",
    "jawRight"            to "턱 오른쪽",
    "jawForward"          to "턱 앞으로",
    "mouthSmileLeft"      to "미소 (왼쪽)",
    "mouthSmileRight"     to "미소 (오른쪽)",
    "mouthFrownLeft"      to "입꼬리 내리기 (왼)",
    "mouthFrownRight"     to "입꼬리 내리기 (오)",
    "mouthPucker"         to "입술 오므리기",
    "mouthFunnel"         to "입 모으기",
    "mouthLeft"           to "입 왼쪽",
    "mouthRight"          to "입 오른쪽",
    "mouthRollLower"      to "아랫입술 말기",
    "mouthRollUpper"      to "윗입술 말기",
    "mouthShrugLower"     to "아랫입술 올리기",
    "mouthShrugUpper"     to "윗입술 올리기",
    "mouthClose"          to "입 다물기",
    "mouthUpperUpLeft"    to "윗입술 위로 (왼)",
    "mouthUpperUpRight"   to "윗입술 위로 (오)",
    "mouthLowerDownLeft"  to "아랫입술 아래로 (왼)",
    "mouthLowerDownRight" to "아랫입술 아래로 (오)",
    "mouthDimpleLeft"     to "보조개 (왼쪽)",
    "mouthDimpleRight"    to "보조개 (오른쪽)",
    "mouthPressLeft"      to "입술 누르기 (왼)",
    "mouthPressRight"     to "입술 누르기 (오)",
    "mouthStretchLeft"    to "입 늘리기 (왼쪽)",
    "mouthStretchRight"   to "입 늘리기 (오른쪽)",
    "cheekPuff"           to "볼 부풀리기",
    "cheekSquintLeft"     to "볼 찡그림 (왼쪽)",
    "cheekSquintRight"    to "볼 찡그림 (오른쪽)",
    "noseSneerLeft"       to "코 찡그림 (왼쪽)",
    "noseSneerRight"      to "코 찡그림 (오른쪽)",
    "tongueOut"           to "혀 내밀기"
)
