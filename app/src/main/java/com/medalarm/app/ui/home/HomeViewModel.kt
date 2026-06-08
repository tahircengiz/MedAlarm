package com.medalarm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.permission.OemAutostartHelper
import com.medalarm.app.permission.SystemHealthChecker
import com.medalarm.app.permission.SystemHealthReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val medications: List<Medication> = emptyList(),
    val todaysDoses: List<DoseLog> = emptyList(),
    val healthReport: SystemHealthReport? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    medicationRepository: MedicationRepository,
    doseLogRepository: DoseLogRepository,
    private val settingsRepository: SettingsRepository,
    private val systemHealthChecker: SystemHealthChecker,
    private val oemAutostartHelper: OemAutostartHelper
) : ViewModel() {

    private val _healthReport = MutableStateFlow<SystemHealthReport?>(null)
    val healthReport: StateFlow<SystemHealthReport?> = _healthReport.asStateFlow()

    private val todayRange: Pair<Instant, Instant> = computeTodayRange()

    val uiState: StateFlow<HomeUiState> = combine(
        medicationRepository.observeActive(),
        // For MVP this window is captured at VM init; the cross-midnight refresh
        // will be handled by re-collecting on resume in a later commit.
        doseLogRepository.observeRange(todayRange.first, todayRange.second),
        healthReport
    ) { meds, doses, health ->
        HomeUiState(medications = meds, todaysDoses = doses, healthReport = health)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshHealth() {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            val report = systemHealthChecker.check(
                settings,
                oemAutostartSupported = oemAutostartHelper.isAggressive()
            )
            _healthReport.update { report }
        }
    }

    private fun computeTodayRange(): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }
}
