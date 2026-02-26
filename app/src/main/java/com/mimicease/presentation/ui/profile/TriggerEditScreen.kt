package com.mimicease.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mimicease.domain.model.Action
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.TriggerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TriggerEditViewModel @Inject constructor(
    private val triggerRepository: TriggerRepository
) : ViewModel() {

    fun saveTrigger(
        profileId: String,
        name: String,
        blendShape: String,
        threshold: Float,
        action: Action
    ) {
        viewModelScope.launch {
            val trigger = Trigger(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = name,
                blendShape = blendShape,
                threshold = threshold,
                action = action
            )
            triggerRepository.saveTrigger(trigger)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerEditScreen(
    navController: NavController,
    profileId: String,
    viewModel: TriggerEditViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var blendShape by remember { mutableStateOf("eyeBlinkRight") }
    var threshold by remember { mutableFloatStateOf(0.5f) }
    var selectedAction by remember { mutableStateOf<Action>(Action.GlobalBack) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("트리거 편집") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveTrigger(profileId, name, blendShape, threshold, selectedAction)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("트리거 이름 (예: 오른쪽 윙크)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("표정 선택", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = blendShape,
                onValueChange = { blendShape = it },
                label = { Text("BlendShape ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("인식 민감도 (현재: %.2f)".format(threshold))
            Slider(
                value = threshold,
                onValueChange = { threshold = it },
                valueRange = 0.1f..1.0f
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("동작 선택", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { /* TODO: Open Action Picker BottomSheet */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("기본 동작: 뒤로가기") // Example hardcoded action selection UI for simplicity
            }
        }
    }
}
