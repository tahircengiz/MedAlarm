package com.medalarm.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medalarm.app.ui.backup.BackupScreen
import com.medalarm.app.ui.history.HistoryScreen
import com.medalarm.app.ui.home.HomeScreen
import com.medalarm.app.ui.medication.MedicationDetailScreen
import com.medalarm.app.ui.medication.MedicationFormScreen
import com.medalarm.app.ui.medication.MedicationListScreen
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

        composable(Routes.MEDICATIONS) {
            MedicationListScreen(
                onBack = { navController.popBackStack() },
                onOpenMedication = { id -> navController.navigate(Routes.medicationDetail(id)) },
                onAddMedication = { navController.navigate(Routes.ADD_MEDICATION) }
            )
        }

        composable(Routes.ADD_MEDICATION) {
            MedicationFormScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            Routes.EDIT_MEDICATION,
            arguments = listOf(navArgument(Routes.MEDICATION_ID_KEY) { type = NavType.StringType })
        ) {
            MedicationFormScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            Routes.MEDICATION_DETAIL,
            arguments = listOf(navArgument(Routes.MEDICATION_ID_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Routes.MEDICATION_ID_KEY)?.toLongOrNull()
            MedicationDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = {
                    if (id != null) navController.navigate(Routes.editMedication(id))
                },
                onDeleted = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSystemStatus = { navController.navigate(Routes.SETTINGS_SYSTEM_STATUS) },
                onOpenBackup = { navController.navigate(Routes.SETTINGS_BACKUP) },
                onOpenMedications = { navController.navigate(Routes.MEDICATIONS) }
            )
        }

        composable(Routes.SETTINGS_SYSTEM_STATUS) {
            SystemStatusScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
