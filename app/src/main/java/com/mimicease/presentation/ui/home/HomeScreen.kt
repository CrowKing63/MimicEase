package com.mimicease.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MimicEase") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        },
        bottomBar = { MimicBottomNavigation(navController) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ServiceStatusCard(
                    uiState = uiState,
                    onTogglePause = { viewModel.toggleServicePause() },
                    onStartService = { viewModel.startService() },
                    onStopService = { viewModel.stopService() }
                )
            }

            item {
                ActiveProfileCard(
                    uiState = uiState,
                    onChangeProfile = { navController.navigate("profiles") }
                )
            }

            if (uiState.quickTriggers.isNotEmpty()) {
                item {
                    Text(
                        text = "빠른 트리거",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(uiState.quickTriggers) { trigger ->
                    QuickTriggerCard(
                        trigger = trigger,
                        onToggle = { isEnabled -> viewModel.toggleTriggerEnabled(trigger, isEnabled) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceStatusCard(
    uiState: HomeUiState,
    onTogglePause: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "서비스 상태",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val (statusColor, statusText) = when {
                !uiState.isServiceRunning -> Color.Gray to "서비스 꺼짐"
                uiState.isPaused -> Color(0xFFFFB300) to "일시정지됨"
                else -> Color(0xFF4CAF50) to "표정 감지 중"
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor,
                    modifier = Modifier.size(12.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = statusText, style = MaterialTheme.typography.bodyLarge)
            }

            if (uiState.isDeveloperMode && uiState.isServiceRunning) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "FPS: ${uiState.currentFps}  추론시간: ${uiState.inferenceTimeMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.isServiceRunning) {
                Button(onClick = onStartService, modifier = Modifier.fillMaxWidth()) {
                    Text("서비스 시작")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onTogglePause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.isPaused) "재개" else "일시정지")
                    }
                    OutlinedButton(
                        onClick = onStopService,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("서비스 종료")
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveProfileCard(
    uiState: HomeUiState,
    onChangeProfile: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "현재 프로필", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                if (uiState.activeProfile != null) {
                    val profile = uiState.activeProfile
                    val activeTriggers = profile.triggers.count { it.isEnabled }
                    Text(
                        text = "${profile.icon} ${profile.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "트리거 ${activeTriggers}개 활성",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(text = "프로필 없음", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "프로필을 만들어 트리거를 설정하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onChangeProfile) { Text("프로필 변경 ›") }
        }
    }
}

@Composable
fun QuickTriggerCard(
    trigger: Trigger,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trigger.name.ifBlank { trigger.blendShape },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (trigger.isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "→ ${getActionLabel(trigger.action)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trigger.isEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = trigger.isEnabled,
                onCheckedChange = { onToggle(it) }
            )
        }
    }
}

fun getActionLabel(action: Action): String = when (action) {
    is Action.GlobalHome          -> "홈 버튼"
    is Action.GlobalBack          -> "뒤로가기"
    is Action.GlobalRecents       -> "최근 앱"
    is Action.GlobalNotifications -> "알림 패널"
    is Action.GlobalQuickSettings -> "빠른 설정"
    is Action.ScreenLock          -> "화면 잠금"
    is Action.TakeScreenshot      -> "스크린샷"
    is Action.ScrollUp            -> "위로 스크롤"
    is Action.ScrollDown          -> "아래로 스크롤"
    is Action.SwipeUp             -> "위로 스와이프"
    is Action.SwipeDown           -> "아래로 스와이프"
    is Action.SwipeLeft           -> "왼쪽 스와이프"
    is Action.SwipeRight          -> "오른쪽 스와이프"
    is Action.MimicPause          -> "일시 정지"
    else                          -> action.javaClass.simpleName ?: ""
}

@Composable
fun MimicBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Face, contentDescription = "테스트") },
            label = { Text("테스트") },
            selected = currentRoute == "test",
            onClick = {
                navController.navigate("test") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "프로필") },
            label = { Text("프로필") },
            selected = currentRoute == "profiles",
            onClick = {
                navController.navigate("profiles") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
            label = { Text("설정") },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            }
        )
    }
}
