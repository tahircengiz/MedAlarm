package com.medalarm.app.ui.medication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.usecase.AlarmRegistrar
import com.medalarm.app.domain.usecase.GenerateUpcomingDosesUseCase
import com.medalarm.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class MedicationDetailUiState(
    val medication: Medication? = null,
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val generateUpcomingDosesUseCase: GenerateUpcomingDosesUseCase,
    private val alarmRegistrar: AlarmRegistrar
) : ViewModel() {

    private val medicationId: Long =
        savedStateHandle.get<String>(Routes.MEDICATION_ID_KEY)?.toLongOrNull() ?: -1L

    val uiState: StateFlow<MedicationDetailUiState> = combine(
        medicationRepository.observe(medicationId),
        medicationRepository.observeSchedules(medicationId)
    ) { med, schedules ->
        MedicationDetailUiState(
            medication = med,
            schedules = schedules,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MedicationDetailUiState()
    )

    fun togglePause(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val current = medicationRepository.get(medicationId) ?: return@launch
            val updated = current.copy(isActive = !current.isActive, updatedAt = Instant.now())
            medicationRepository.update(updated)

            if (updated.isActive) {
                generateUpcomingDosesUseCase(medicationId)
            } else {
                val now = Instant.now()
                medicationRepository.getSchedules(medicationId).forEach { s ->
                    val next = doseLogRepository.findNextPending(medicationId, s.id, after = now)
                    next?.let { alarmRegistrar.cancel(it.id) }
                }
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val now = Instant.now()
            medicationRepository.getSchedules(medicationId).forEach { s ->
                val next = doseLogRepository.findNextPending(medicationId, s.id, after = now)
                next?.let { alarmRegistrar.cancel(it.id) }
            }
            medicationRepository.softDelete(medicationId)
            onDone()
        }
    }
}
