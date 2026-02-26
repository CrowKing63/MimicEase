package com.mimicease.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mimicease.presentation.ui.home.HomeScreen
import com.mimicease.presentation.ui.onboarding.OnboardingScreen
import com.mimicease.presentation.ui.test.ExpressionTestScreen
import com.mimicease.presentation.ui.profile.ProfileListScreen
import com.mimicease.presentation.ui.settings.SettingsScreen

@Composable
fun MimicNavGraph() {
    val navController = rememberNavController()
    
    // For simplicity, we start at a "Home" route which will include bottom bar
    // In actual implementation, we'd check if onboarding is needed
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
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
