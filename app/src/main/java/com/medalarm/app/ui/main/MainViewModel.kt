package com.medalarm.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.domain.model.UserSettings
import com.medalarm.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Hot state stream of [UserSettings]. The activity collects this to:
     * - drive Compose theme (dark/light, dynamic color)
     * - decide between ONBOARDING vs HOME on first composition
     *
     * `null` means "still loading from DataStore" — the activity should show
     * a blank scaffold (no theme flicker) until the first emission.
     */
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Whether to apply the dark theme. Returns null until settings are loaded,
     * so the activity can defer reading isSystemInDarkTheme() until then.
     */
    fun isDarkTheme(): Boolean? = settings.value?.let { s ->
        when (s.themeMode) {
            ThemeMode.SYSTEM -> null
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    fun startDestination(): String? = settings.value?.let { s ->
        if (s.onboardingCompleted) "home" else "onboarding"
    }
}
