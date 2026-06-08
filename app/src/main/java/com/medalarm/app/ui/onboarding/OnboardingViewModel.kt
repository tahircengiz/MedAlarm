package com.medalarm.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.domain.model.UserSettings
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.permission.OemAutostartHelper
import com.medalarm.app.permission.SystemHealthChecker
import com.medalarm.app.permission.SystemHealthReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val systemHealthChecker: SystemHealthChecker,
    private val oemAutostartHelper: OemAutostartHelper
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _healthReport = MutableStateFlow<SystemHealthReport?>(null)
    val healthReport: StateFlow<SystemHealthReport?> = _healthReport.asStateFlow()

    val isOemAggressive: Boolean = oemAutostartHelper.isAggressive()

    fun refreshHealth() {
        val s = settings.value ?: return
        _healthReport.update { systemHealthChecker.check(s, oemAutostartSupported = isOemAggressive) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseDynamicColor(enabled) }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { settingsRepository.setDisclaimerAccepted() }
    }

    fun confirmOemAutostart() {
        viewModelScope.launch { settingsRepository.setUserConfirmedOemAutostart(true) }
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            onDone()
        }
    }
}
