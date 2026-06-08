package com.medalarm.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.medalarm.app.ui.history.HistoryScreen
import com.medalarm.app.ui.home.HomeScreen
import com.medalarm.app.ui.medication.AddMedicationScreen
import com.medalarm.app.ui.onboarding.OnboardingScreen
import com.medalarm.app.ui.settings.SettingsScreen
import com.medalarm.app.ui.settings.SystemStatusScreen

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

        composable(Routes.ADD_MEDICATION) {
            AddMedicationScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSystemStatus = { navController.navigate(Routes.SETTINGS_SYSTEM_STATUS) }
            )
        }

        composable(Routes.SETTINGS_SYSTEM_STATUS) {
            SystemStatusScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}

