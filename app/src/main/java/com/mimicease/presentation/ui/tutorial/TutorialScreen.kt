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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ──────────────────────────────────────────────────────────────────────────────
// 데이터 모델
// ──────────────────────────────────────────────────────────────────────────────

private data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val ctaLabel: String
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        stepNumber = 1,
        title = "프로필 만들기",
        description = "표정-액션 매핑 묶음인 '프로필'을 먼저 만들어야 합니다.\n이름을 입력하고 '만들기' 버튼을 눌러보세요.",
        icon = Icons.Filled.AccountCircle,
        accentColor = Color(0xFF6C63FF),
        ctaLabel = "만들기"
    ),
    TutorialStep(
        stepNumber = 2,
        title = "트리거 추가하기",
        description = "프로필에 트리거를 추가합니다.\n트리거란 '어떤 표정을 지으면 어떤 동작을 할지'를 정의한 규칙입니다.",
        icon = Icons.Filled.Add,
        accentColor = Color(0xFF00BCD4),
        ctaLabel = "트리거 추가"
    ),
    TutorialStep(
        stepNumber = 3,
        title = "표정 선택",
        description = "인식할 표정을 선택합니다.\n예: 웃기(mouthSmileLeft) — 가볍게 웃는 표정으로도 트리거가 동작해요!",
        icon = Icons.Filled.Face,
        accentColor = Color(0xFFFF9800),
        ctaLabel = "표정 선택"
    ),
    TutorialStep(
        stepNumber = 4,
        title = "액션 설정",
        description = "표정을 지었을 때 실행될 동작을 선택합니다.\n예: 뒤로가기, 탭, 스크롤 등 다양한 시스템 액션을 매핑할 수 있습니다.",
        icon = Icons.Filled.Settings,
        accentColor = Color(0xFF4CAF50),
        ctaLabel = "액션 선택"
    ),
    TutorialStep(
        stepNumber = 5,
        title = "저장 완료!",
        description = "트리거를 저장했습니다. 🎉\n저장 버튼을 눌러 설정을 확정하면 목록에 추가됩니다.",
        icon = Icons.Filled.Check,
        accentColor = Color(0xFF2196F3),
        ctaLabel = "저장"
    ),
    TutorialStep(
        stepNumber = 6,
        title = "서비스 시작",
        description = "홈 화면에서 '서비스 시작' 버튼을 눌러 표정 인식을 활성화하세요.\n이제 설정한 표정을 지으면 앱이 자동으로 반응합니다!",
        icon = Icons.Filled.PlayArrow,
        accentColor = Color(0xFFE91E63),
        ctaLabel = "완료"
    )
)

// ──────────────────────────────────────────────────────────────────────────────
// TutorialScreen (진입점)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TutorialScreen(navController: NavController) {
    var currentStep by remember { mutableIntStateOf(0) }

    // 스텝 1(프로필 만들기)에서 사용자가 입력한 프로필 이름을 보관
    var profileName by remember { mutableStateOf("") }

    val totalSteps = TUTORIAL_STEPS.size
    val step = TUTORIAL_STEPS[currentStep]

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
            // 스텝 콘텐츠 (슬라이드 애니메이션)
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
                val s = TUTORIAL_STEPS[idx]
                TutorialStepContent(
                    step = s,
                    profileName = if (idx == 0) profileName else "",
                    onProfileNameChange = { profileName = it }
                )
            }

            // 하단 스텝 인디케이터 + 버튼
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
            title = { Text("튜토리얼 ($currentStep / $totalSteps)") },
            actions = {
                TextButton(onClick = onSkip) { Text("건너뛰기") }
            }
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 스텝별 콘텐츠
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
        // 아이콘 버블
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

        // 스텝 배지
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

        // 스텝 1 전용: 프로필 이름 직접 입력 UI
        if (step.stepNumber == 1) {
            ProfileNameInput(
                name = profileName,
                onNameChange = onProfileNameChange,
                accentColor = step.accentColor
            )
        }

        // 스텝 6 전용: 완료 축하 카드
        if (step.stepNumber == 6) {
            CompletionCard()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 스텝 1 전용 — 프로필 이름 입력
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
                "프로필 이름 미리 정해보기",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("예: 내 첫 번째 프로필") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "이름은 나중에 언제든지 수정할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 스텝 6 전용 — 완료 축하 카드
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompletionCard() {
    Spacer(Modifier.height(28.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
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
                    "훌륭해요!",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "이제 표정만으로 스마트폰을 자유롭게 조작할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// BottomBar — 스텝 인디케이터 + 이전/다음 버튼
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
    // 스텝 1에서는 이름 미입력이어도 '다음'을 허용 (입력은 선택)
    val canProceed = true

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
            // 도트 인디케이터
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

            // 버튼 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 이전 버튼 (첫 스텝에서는 숨김)
                AnimatedVisibility(visible = currentStep > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("이전")
                    }
                }

                // 다음 / 완료 버튼
                Button(
                    onClick = onNext,
                    enabled = canProceed,
                    modifier = Modifier.weight(if (currentStep > 0) 1f else Float.MAX_VALUE),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = step.accentColor
                    )
                ) {
                    Text(if (isLastStep) "완료" else step.ctaLabel)
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
