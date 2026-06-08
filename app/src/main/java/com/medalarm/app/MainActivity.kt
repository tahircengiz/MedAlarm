package com.medalarm.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.ui.main.MainViewModel
import com.medalarm.app.ui.navigation.MedAlarmNavHost
import com.medalarm.app.ui.navigation.Routes
import com.medalarm.app.ui.navigation.rememberMedAlarmNavController
import com.medalarm.app.ui.theme.MedAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()

            val darkOverride = settings?.let { s ->
                when (s.themeMode) {
                    ThemeMode.SYSTEM -> null
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            }
            val dynamicColor = settings?.useDynamicColor ?: true

            MedAlarmTheme(darkTheme = darkOverride, dynamicColor = dynamicColor) {
                when {
                    settings == null -> SplashSurface()
                    else -> {
                        val startDestination = if (settings!!.onboardingCompleted) {
                            Routes.HOME
                        } else {
                            Routes.ONBOARDING
                        }
                        val navController = rememberMedAlarmNavController()
                        MedAlarmNavHost(navController = navController, startDestination = startDestination)
                    }
                }
            }
        }
    }
}

/** Solid theme-colored surface shown while [MainViewModel.settings] is still loading.
 *  Prevents the visible flash from default theme → user-preferred theme. */
@Composable
private fun SplashSurface() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}
