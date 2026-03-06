package com.mimicease.presentation.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mimicease.R

// ──────────────────────────────────────────────────────────────────────────────
// Data model
// ──────────────────────────────────────────────────────────────────────────────

private data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val ctaLabel: String
)

// ──────────────────────────────────────────────────────────────────────────────
// TutorialScreen (entry point)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TutorialScreen(navController: NavController) {
    // TUTORIAL_STEPS is defined inside the Composable so stringResource() can be called
    val tutorialSteps = listOf(
        TutorialStep(
            stepNumber = 1,
            title = stringResource(R.string.tutorial_step1_title),
            description = stringResource(R.string.tutorial_step1_desc),
            icon = Icons.Filled.AccountCircle,
            accentColor = Color(0xFF6C63FF),
            ctaLabel = stringResource(R.string.tutorial_step1_cta)
        ),
        TutorialStep(
            stepNumber = 2,
            title = stringResource(R.string.tutorial_step2_title),
            description = stringResource(R.string.tutorial_step2_desc),
            icon = Icons.Filled.Add,
            accentColor = Color(0xFF00BCD4),
            ctaLabel = stringResource(R.string.tutorial_step2_cta)
        ),
        TutorialStep(
            stepNumber = 3,
            title = stringResource(R.string.tutorial_step3_title),
            description = stringResource(R.string.tutorial_step3_desc),
            icon = Icons.Filled.Face,
            accentColor = Color(0xFFFF9800),
            ctaLabel = stringResource(R.string.tutorial_step3_cta)
        ),
        TutorialStep(
            stepNumber = 4,
            title = stringResource(R.string.tutorial_step4_title),
            description = stringResource(R.string.tutorial_step4_desc),
            icon = Icons.Filled.Settings,
            accentColor = Color(0xFF4CAF50),
            ctaLabel = stringResource(R.string.tutorial_step4_cta)
        ),
        TutorialStep(
            stepNumber = 5,
            title = stringResource(R.string.tutorial_step5_title),
            description = stringResource(R.string.tutorial_step5_desc),
            icon = Icons.Filled.Check,
            accentColor = Color(0xFF2196F3),
            ctaLabel = stringResource(R.string.tutorial_step5_cta)
        ),
        TutorialStep(
            stepNumber = 6,
            title = stringResource(R.string.tutorial_step6_title),
            description = stringResource(R.string.tutorial_step6_desc),
            icon = Icons.Filled.PlayArrow,
            accentColor = Color(0xFFE91E63),
            ctaLabel = stringResource(R.string.tutorial_step6_cta)
        )
    )

    var currentStep by remember { mutableIntStateOf(0) }
    var profileName by remember { mutableStateOf("") }

    val totalSteps = tutorialSteps.size
    val step = tutorialSteps[currentStep]

    val progress by animateFloatAsState(
        targetValue = (currentStep + 1) / totalSteps.toFloat(),
        animationSpec = tween(400),
        label = "progress"
    )

    Scaffold(
        topBar = {
            TutorialTopBar(
                currentStep = currentStep + 1,
                totalSteps = totalSteps,
                progress = progress,
                onSkip = {
                    navController.navigate("home") {
                        popUpTo("tutorial") { inclusive = true }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { it * dir } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { -it * dir } + fadeOut(tween(300)))
                },
                modifier = Modifier.weight(1f),
                label = "stepContent"
            ) { idx ->
                val s = tutorialSteps[idx]
                TutorialStepContent(
                    step = s,
                    profileName = if (idx == 0) profileName else "",
                    onProfileNameChange = { profileName = it }
                )
            }

            TutorialBottomBar(
                step = step,
                currentStep = currentStep,
                totalSteps = totalSteps,
                profileName = profileName,
                onNext = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        navController.navigate("home") {
                            popUpTo("tutorial") { inclusive = true }
                        }
                    }
                },
                onBack = {
                    if (currentStep > 0) currentStep--
                    else navController.popBackStack()
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TopBar
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorialTopBar(
    currentStep: Int,
    totalSteps: Int,
    progress: Float,
    onSkip: () -> Unit
) {
    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.tutorial_title, currentStep, totalSteps)) },
            actions = {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.tutorial_skip)) }
            }
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step content
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TutorialStepContent(
    step: TutorialStep,
    profileName: String,
    onProfileNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon bubble
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(step.accentColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(step.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = step.accentColor
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Step badge
        Surface(
            shape = RoundedCornerShape(50),
            color = step.accentColor.copy(alpha = 0.12f),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "STEP ${step.stepNumber}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = step.accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        // Step 1 only: profile name input
        if (step.stepNumber == 1) {
            ProfileNameInput(
                name = profileName,
                onNameChange = onProfileNameChange,
                accentColor = step.accentColor
            )
        }

        // Step 6 only: completion card
        if (step.stepNumber == 6) {
            CompletionCard()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step 1 only — profile name input
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileNameInput(
    name: String,
    onNameChange: (String) -> Unit,
    accentColor: Color
) {
    val focusManager = LocalFocusManager.current

    Spacer(Modifier.height(32.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.tutorial_step1_name_hint),
                style = MaterialTheme.typography.labelLarge,
                color = accentColor
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(stringResource(R.string.tutorial_step1_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.tutorial_step1_name_editable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step 6 only — completion card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompletionCard() {
    Spacer(Modifier.height(28.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(
                    stringResource(R.string.tutorial_completion_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.tutorial_completion_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// BottomBar — step indicator + prev/next buttons
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TutorialBottomBar(
    step: TutorialStep,
    currentStep: Int,
    totalSteps: Int,
    profileName: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isLastStep = currentStep == totalSteps - 1

    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dot indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { idx ->
                    val isActive = idx == currentStep
                    val isDone = idx < currentStep
                    Box(
                        modifier = Modifier
                            .then(
                                if (isActive) Modifier.size(width = 24.dp, height = 8.dp)
                                else Modifier.size(8.dp)
                            )
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    isActive -> step.accentColor
                                    isDone -> step.accentColor.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                    )
                }
            }

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button (hidden on first step)
                AnimatedVisibility(visible = currentStep > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tutorial_previous))
                    }
                }

                // Next / Done button
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(if (currentStep > 0) 1f else Float.MAX_VALUE),
                    colors = ButtonDefaults.buttonColors(containerColor = step.accentColor)
                ) {
                    Text(if (isLastStep) stringResource(R.string.tutorial_done) else step.ctaLabel)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isLastStep) Icons.Filled.Check
                                      else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
