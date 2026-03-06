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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mimicease.R
import com.mimicease.domain.model.Trigger
import com.mimicease.presentation.ui.profile.actionDisplayName

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
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
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
                        text = stringResource(R.string.home_quick_triggers),
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
                text = stringResource(R.string.home_service_status),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val (statusColor, statusText) = when {
                !uiState.isServiceRunning -> Color.Gray to stringResource(R.string.home_service_stopped)
                uiState.isPaused -> Color(0xFFFFB300) to stringResource(R.string.home_service_paused)
                else -> Color(0xFF4CAF50) to stringResource(R.string.home_service_running)
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
                    text = stringResource(R.string.home_fps_info, uiState.currentFps, uiState.inferenceTimeMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.isServiceRunning) {
                Button(onClick = onStartService, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_start_service))
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
                        Text(if (uiState.isPaused) stringResource(R.string.home_resume) else stringResource(R.string.home_pause))
                    }
                    OutlinedButton(
                        onClick = onStopService,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.home_stop_service))
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
                Text(text = stringResource(R.string.home_current_profile), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                if (uiState.activeProfile != null) {
                    val profile = uiState.activeProfile
                    val activeTriggers = profile.triggers.count { it.isEnabled }
                    Text(
                        text = "${profile.icon} ${profile.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.home_triggers_active, activeTriggers),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(text = stringResource(R.string.home_no_profile), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.home_create_profile_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onChangeProfile) { Text(stringResource(R.string.home_change_profile)) }
        }
    }
}

@Composable
fun QuickTriggerCard(
    trigger: Trigger,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
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
                    text = "→ ${actionDisplayName(trigger.action, context)}",
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

@Composable
fun MimicBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Face, contentDescription = stringResource(R.string.nav_test)) },
            label = { Text(stringResource(R.string.nav_test)) },
            selected = currentRoute == "test",
            onClick = {
                navController.navigate("test") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profiles)) },
            label = { Text(stringResource(R.string.nav_profiles)) },
            selected = currentRoute == "profiles",
            onClick = {
                navController.navigate("profiles") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
            label = { Text(stringResource(R.string.nav_settings)) },
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
