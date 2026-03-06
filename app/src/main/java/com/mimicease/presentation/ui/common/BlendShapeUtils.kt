package com.mimicease.presentation.ui.common

import androidx.annotation.StringRes
import com.mimicease.R

// ─── BlendShape 카테고리 분류 ────────────────────────────────────────────

enum class BlendShapeCategory(@StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    EYES(R.string.category_eyes),
    BROWS(R.string.category_brows),
    MOUTH(R.string.category_mouth),
    CHEEK(R.string.category_cheek),
    TONGUE(R.string.category_tongue)
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

// Human-readable expression name map (52 total — canonical source)
val BLENDSHAPE_DISPLAY_NAMES: Map<String, String> = mapOf(
    "eyeBlinkLeft"        to "Eye Blink (Left)",
    "eyeBlinkRight"       to "Eye Blink (Right)",
    "eyeWideLeft"         to "Eye Wide (Left)",
    "eyeWideRight"        to "Eye Wide (Right)",
    "eyeSquintLeft"       to "Eye Squint (Left)",
    "eyeSquintRight"      to "Eye Squint (Right)",
    "eyeLookUpLeft"       to "Eye Look Up (Left)",
    "eyeLookUpRight"      to "Eye Look Up (Right)",
    "eyeLookDownLeft"     to "Eye Look Down (Left)",
    "eyeLookDownRight"    to "Eye Look Down (Right)",
    "eyeLookInLeft"       to "Eye Look In (Left)",
    "eyeLookInRight"      to "Eye Look In (Right)",
    "eyeLookOutLeft"      to "Eye Look Out (Left)",
    "eyeLookOutRight"     to "Eye Look Out (Right)",
    "browInnerUp"         to "Brow Inner Up",
    "browOuterUpLeft"     to "Brow Outer Up (Left)",
    "browOuterUpRight"    to "Brow Outer Up (Right)",
    "browDownLeft"        to "Brow Down (Left)",
    "browDownRight"       to "Brow Down (Right)",
    "jawOpen"             to "Jaw Open",
    "jawLeft"             to "Jaw Left",
    "jawRight"            to "Jaw Right",
    "jawForward"          to "Jaw Forward",
    "mouthSmileLeft"      to "Mouth Smile (Left)",
    "mouthSmileRight"     to "Mouth Smile (Right)",
    "mouthFrownLeft"      to "Mouth Frown (Left)",
    "mouthFrownRight"     to "Mouth Frown (Right)",
    "mouthPucker"         to "Mouth Pucker",
    "mouthFunnel"         to "Mouth Funnel",
    "mouthLeft"           to "Mouth Left",
    "mouthRight"          to "Mouth Right",
    "mouthRollLower"      to "Mouth Roll Lower",
    "mouthRollUpper"      to "Mouth Roll Upper",
    "mouthShrugLower"     to "Mouth Shrug Lower",
    "mouthShrugUpper"     to "Mouth Shrug Upper",
    "mouthClose"          to "Mouth Close",
    "mouthUpperUpLeft"    to "Mouth Upper Up (Left)",
    "mouthUpperUpRight"   to "Mouth Upper Up (Right)",
    "mouthLowerDownLeft"  to "Mouth Lower Down (Left)",
    "mouthLowerDownRight" to "Mouth Lower Down (Right)",
    "mouthDimpleLeft"     to "Mouth Dimple (Left)",
    "mouthDimpleRight"    to "Mouth Dimple (Right)",
    "mouthPressLeft"      to "Mouth Press (Left)",
    "mouthPressRight"     to "Mouth Press (Right)",
    "mouthStretchLeft"    to "Mouth Stretch (Left)",
    "mouthStretchRight"   to "Mouth Stretch (Right)",
    "cheekPuff"           to "Cheek Puff",
    "cheekSquintLeft"     to "Cheek Squint (Left)",
    "cheekSquintRight"    to "Cheek Squint (Right)",
    "noseSneerLeft"       to "Nose Sneer (Left)",
    "noseSneerRight"      to "Nose Sneer (Right)",
    "tongueOut"           to "Tongue Out"
)
