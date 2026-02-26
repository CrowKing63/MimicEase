package com.mimicease.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.domain.model.Profile
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.presentation.ui.home.MimicBottomNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    navController: NavController,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to create profile */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Profile")
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
                .padding(16.dp)
        ) {
            items(profiles) { profile ->
                ProfileItemCard(
                    profile = profile,
                    onActivate = { viewModel.activateProfile(profile.id) },
                    onClick = { /* TODO: Navigate to Trigger list for profile */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileItemCard(
    profile: Profile,
    onActivate: () -> Unit,
    onClick: () -> Unit
) {
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
            Column {
                Text(
                    text = "${profile.icon} ${profile.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "트리거 ${profile.triggers.size}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.isActive) {
                    Text("활성됨", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                } else {
                    OutlinedButton(onClick = onActivate) {
                        Text("선택")
                    }
                }
            }
        }
    }
}
