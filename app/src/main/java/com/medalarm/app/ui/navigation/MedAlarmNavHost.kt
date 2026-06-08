package com.medalarm.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.medalarm.app.ui.home.HomeScreen
import com.medalarm.app.ui.onboarding.OnboardingScreen

@Composable
fun rememberMedAlarmNavController(): NavHostController = rememberNavController()

@Composable
fun MedAlarmNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onCompleted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAddMedication = { navController.navigate(Routes.ADD_MEDICATION) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenMedication = { id -> navController.navigate(Routes.medicationDetail(id)) }
            )
        }

        // Stubs for screens that land in subsequent commits.
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings") }
        composable(Routes.SETTINGS_SYSTEM_STATUS) { PlaceholderScreen("System Status") }
        composable(Routes.ADD_MEDICATION) { PlaceholderScreen("Add Medication") }
        composable(Routes.HISTORY) { PlaceholderScreen("History") }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("$name — coming soon")
        }
    }
}
