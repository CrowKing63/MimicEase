package com.mimicease.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.R
import com.mimicease.domain.model.Profile
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.TriggerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────

data class TriggerListUiState(
    val profile: Profile? = null,
    val triggers: List<Trigger> = emptyList()
)

@HiltViewModel
class TriggerListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val triggerRepository: TriggerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val profileId: String = savedStateHandle["profileId"] ?: ""

    val uiState: StateFlow<TriggerListUiState> = combine(
        profileRepository.getAllProfiles(),
        triggerRepository.getTriggersByProfile(profileId)
    ) { profiles, triggers ->
        TriggerListUiState(
            profile = profiles.find { it.id == profileId },
            triggers = triggers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TriggerListUiState()
    )

    fun toggleTrigger(triggerId: String, enabled: Boolean) {
        viewModelScope.launch { triggerRepository.setTriggerEnabled(triggerId, enabled) }
    }

    fun deleteTrigger(trigger: Trigger) {
        viewModelScope.launch { triggerRepository.deleteTrigger(trigger) }
    }
}

// ─── UI ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerListScreen(
    navController: NavController,
    profileId: String,
    viewModel: TriggerListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.profile?.let { "${it.icon} ${it.name}" }
                        ?: stringResource(R.string.trigger_list_fallback_title))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.trigger_list_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { navController.navigate("profileEdit/$profileId") }
                    ) { Text(stringResource(R.string.trigger_list_edit_profile)) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("triggerEdit/$profileId") }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.trigger_list_add))
            }
        }
    ) { innerPadding ->
        if (uiState.triggers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.trigger_list_empty), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("triggerEdit/$profileId") }
                    ) { Text(stringResource(R.string.trigger_list_add_button)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.triggers, key = { it.id }) { trigger ->
                    TriggerItemCard(
                        trigger = trigger,
                        onToggle = { enabled -> viewModel.toggleTrigger(trigger.id, enabled) },
                        onEdit = { navController.navigate("triggerEdit/$profileId/${trigger.id}") },
                        onDelete = { viewModel.deleteTrigger(trigger) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// ─── Trigger card ─────────────────────────────────────────────────────────

@Composable
private fun TriggerItemCard(
    trigger: Trigger,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val thresholdLabel = stringResource(R.string.trigger_threshold_label, "%.2f".format(trigger.threshold))
    val holdLabel = stringResource(R.string.trigger_hold_label, trigger.holdDurationMs)
    val actionName = actionDisplayName(trigger.action, context)

    ListItem(
        headlineContent = {
            Text(
                text = trigger.name.ifBlank { "${trigger.blendShape} → $actionName" },
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = "${trigger.blendShape}  $thresholdLabel  $holdLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "→ $actionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = trigger.isEnabled,
                    onCheckedChange = onToggle
                )
                TextButton(onClick = onEdit) { Text(stringResource(R.string.trigger_edit)) }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.trigger_delete)) }
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.trigger_delete_title)) },
            text = {
                Text(stringResource(R.string.trigger_delete_message,
                    trigger.name.ifBlank { trigger.blendShape }))
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text(stringResource(R.string.trigger_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.profiles_cancel))
                }
            }
        )
    }
}
