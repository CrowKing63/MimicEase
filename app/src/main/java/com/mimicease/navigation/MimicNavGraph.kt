package com.mimicease.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.presentation.ui.home.HomeScreen
import com.mimicease.presentation.ui.onboarding.OnboardingScreen
import com.mimicease.presentation.ui.profile.ProfileEditScreen
import com.mimicease.presentation.ui.profile.ProfileListScreen
import com.mimicease.presentation.ui.profile.TriggerEditScreen
import com.mimicease.presentation.ui.profile.TriggerListScreen
import com.mimicease.presentation.ui.settings.SettingsScreen
import com.mimicease.presentation.ui.test.ExpressionTestScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _onboardingCompleted = MutableStateFlow<Boolean?>(null)
    val onboardingCompleted: StateFlow<Boolean?> = _onboardingCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _onboardingCompleted.value = settings.onboardingCompleted
            }
        }
    }
}

@Composable
fun MimicNavGraph(
    navViewModel: NavViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val onboardingCompleted by navViewModel.onboardingCompleted.collectAsState()

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted == false) {
            navController.navigate("onboarding") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {

        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("test") {
            ExpressionTestScreen(navController = navController)
        }

        composable("profiles") {
            ProfileListScreen(navController = navController)
        }

        // 트리거 목록 (특정 프로필)
        composable(
            route = "triggerList/{profileId}",
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            TriggerListScreen(navController = navController, profileId = profileId)
        }

        // 트리거 신규 생성
        composable(
            route = "triggerEdit/{profileId}",
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            TriggerEditScreen(
                navController = navController,
                profileId = profileId
            )
        }

        // 트리거 편집 (기존 triggerId)
        composable(
            route = "triggerEdit/{profileId}/{triggerId}",
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("triggerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            TriggerEditScreen(
                navController = navController,
                profileId = profileId
            )
        }

        // 프로필 편집
        composable(
            route = "profileEdit/{profileId}",
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            ProfileEditScreen(navController = navController, profileId = profileId)
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
