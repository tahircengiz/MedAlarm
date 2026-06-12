package com.medalarm.app.ui.medication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.data.image.MedicationPhotoStore
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
    private val alarmRegistrar: AlarmRegistrar,
    private val photoStore: MedicationPhotoStore
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
                // Resume: clean regenerate from current schedules.
                generateUpcomingDosesUseCase(medicationId, resetFuture = true)
            } else {
                // Pause: cancel + delete ALL future PENDING so nothing lingers
                // in Home/History for a paused medication.
                val now = Instant.now()
                doseLogRepository.getFuturePending(medicationId, after = now).forEach {
                    alarmRegistrar.cancel(it.id)
                }
                doseLogRepository.deleteFuturePending(medicationId, after = now)
            }
            onDone()
        }
    }

    /** Clears the box photo: unlink from the medication first, then delete the file. */
    fun deletePhoto() {
        viewModelScope.launch {
            val current = medicationRepository.get(medicationId) ?: return@launch
            val path = current.photoPath ?: return@launch
            medicationRepository.update(current.copy(photoPath = null, updatedAt = Instant.now()))
            photoStore.deleteQuietly(path)
        }
    }

    fun addStock(amount: Float) {
        if (amount <= 0f) return
        viewModelScope.launch {
            medicationRepository.addStock(medicationId, amount)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val now = Instant.now()
            // Cancel every future alarm, then drop the rows so a deleted med leaves
            // nothing scheduled. Past/acted doses remain for history.
            doseLogRepository.getFuturePending(medicationId, after = now).forEach {
                alarmRegistrar.cancel(it.id)
            }
            doseLogRepository.deleteFuturePending(medicationId, after = now)
            medicationRepository.softDelete(medicationId)
            onDone()
        }
    }
}
