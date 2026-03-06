package com.mimicease.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.R
import com.mimicease.domain.model.Profile
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.presentation.ui.home.MimicBottomNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun activateProfile(id: String) {
        viewModelScope.launch { profileRepository.activateProfile(id) }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch { profileRepository.deleteProfile(profile) }
    }

    fun createProfile(name: String, icon: String) {
        viewModelScope.launch {
            val profile = Profile(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                isActive = false
            )
            profileRepository.saveProfile(profile)
        }
    }
}

// ─── UI ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    navController: NavController,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.profiles_add))
                    }
                }
            )
        },
        bottomBar = { MimicBottomNavigation(navController) }
    ) { innerPadding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.profiles_empty), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text(stringResource(R.string.profiles_create_button))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileItemCard(
                        profile = profile,
                        onActivate = { viewModel.activateProfile(profile.id) },
                        onClick = { navController.navigate("triggerList/${profile.id}") },
                        onDelete = { viewModel.deleteProfile(profile) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onConfirm = { name, icon ->
                viewModel.createProfile(name, icon)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

// ─── Profile card ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileItemCard(
    profile: Profile,
    onActivate: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val cardColors = if (profile.isActive) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${profile.icon} ${profile.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                val triggerCountText = stringResource(R.string.profiles_trigger_count, profile.triggers.size)
                val activeSuffix = if (profile.isActive) stringResource(R.string.profiles_active_suffix) else ""
                Text(
                    text = triggerCountText + activeSuffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!profile.isActive) {
                    OutlinedButton(
                        onClick = onActivate,
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(stringResource(R.string.profiles_select)) }
                } else {
                    Text(
                        stringResource(R.string.profiles_active_check),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.profiles_delete)) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.profiles_delete_title)) },
            text = { Text(stringResource(R.string.profiles_delete_message, profile.name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text(stringResource(R.string.profiles_delete))
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

// ─── Create profile dialog ────────────────────────────────────────────────

private val PROFILE_ICONS = listOf("😊","😴","🎮","📺","📖","💼","🏃","✍️","🎵","📱")

@Composable
fun CreateProfileDialog(
    onConfirm: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("😊") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profiles_create_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text(stringResource(R.string.profiles_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(stringResource(R.string.profiles_icon_label), style = MaterialTheme.typography.labelMedium)
                val rowChunks = PROFILE_ICONS.chunked(5)
                rowChunks.forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chunk.forEach { emoji ->
                            FilterChip(
                                selected = selectedIcon == emoji,
                                onClick = { selectedIcon = emoji },
                                label = { Text(emoji) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedIcon) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.profiles_create_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profiles_cancel)) }
        }
    )
}
