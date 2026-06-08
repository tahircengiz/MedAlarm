package com.medalarm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.domain.usecase.AlarmRegistrar
import com.medalarm.app.domain.usecase.MarkDoseTakenUseCase
import com.medalarm.app.domain.usecase.RevertDoseUseCase
import com.medalarm.app.domain.usecase.SkipDoseUseCase
import com.medalarm.app.domain.usecase.SnoozeDoseUseCase
import com.medalarm.app.notification.NotificationHelper
import com.medalarm.app.permission.OemAutostartHelper
import com.medalarm.app.permission.SystemHealthChecker
import com.medalarm.app.permission.SystemHealthReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val medications: List<Medication> = emptyList(),
    val doses: List<DoseLog> = emptyList(),
    val healthReport: SystemHealthReport? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val settingsRepository: SettingsRepository,
    private val systemHealthChecker: SystemHealthChecker,
    private val oemAutostartHelper: OemAutostartHelper,
    private val markDoseTakenUseCase: MarkDoseTakenUseCase,
    private val skipDoseUseCase: SkipDoseUseCase,
    private val snoozeDoseUseCase: SnoozeDoseUseCase,
    private val revertDoseUseCase: RevertDoseUseCase,
    private val alarmRegistrar: AlarmRegistrar,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _healthReport = MutableStateFlow<SystemHealthReport?>(null)
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        medicationRepository.observeActive(),
        _selectedDate.flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            doseLogRepository.observeRange(start, end)
        },
        _selectedDate,
        _healthReport
    ) { meds, doses, date, health ->
        HomeUiState(
            selectedDate = date,
            medications = meds,
            doses = doses.sortedBy { it.scheduledAt },
            healthReport = health
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun jumpToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun shiftDay(delta: Int) {
        _selectedDate.update { it.plusDays(delta.toLong()) }
    }

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

    // --- Manual dose actions (from the Home dose dialog) ---
    // These mirror the notification action handlers so manual marking and
    // notification marking behave identically.

    fun markTaken(doseLogId: Long) = viewModelScope.launch {
        alarmRegistrar.cancelAutoSnoozeCheck(doseLogId)
        notificationHelper.cancel(doseLogId)
        val result = markDoseTakenUseCase(doseLogId)
        if (result.crossedThreshold && result.medication != null) {
            notificationHelper.postLowStock(result.medication)
        }
    }

    fun skip(doseLogId: Long) = viewModelScope.launch {
        alarmRegistrar.cancelAutoSnoozeCheck(doseLogId)
        notificationHelper.cancel(doseLogId)
        skipDoseUseCase(doseLogId)
    }

    fun snooze(doseLogId: Long) = viewModelScope.launch {
        alarmRegistrar.cancelAutoSnoozeCheck(doseLogId)
        notificationHelper.cancel(doseLogId)
        snoozeDoseUseCase(doseLogId)
    }

    fun revert(doseLogId: Long) = viewModelScope.launch {
        revertDoseUseCase(doseLogId)
    }
}
