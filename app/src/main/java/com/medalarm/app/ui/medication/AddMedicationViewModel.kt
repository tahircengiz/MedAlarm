package com.medalarm.app.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.model.ScheduleType
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.domain.usecase.ScheduleNextDoseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Form state for the Add Medication screen. Kept as a flat data class so the
 * Composable can read and mutate fields without ceremony; validation is computed
 * derived state, not stored separately.
 */
data class AddMedicationFormState(
    val name: String = "",
    val unit: MedicationUnit = MedicationUnit.TABLET,
    val dosageRaw: String = "1",
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val mealRelation: MealRelation = MealRelation.NONE,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val trackStock: Boolean = false,
    val stockAmountRaw: String = "",
    val stockThresholdRaw: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null
) {
    val nameError: Boolean get() = name.isBlank()
    val dosageError: Boolean get() = dosageRaw.toFloatOrNull()?.let { it <= 0f } ?: true
    val timesError: Boolean get() = times.isEmpty()

    val canSave: Boolean
        get() = !nameError && !dosageError && !timesError && !isSaving
}

@HiltViewModel
class AddMedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduleNextDoseUseCase: ScheduleNextDoseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddMedicationFormState())
    val state: StateFlow<AddMedicationFormState> = _state.asStateFlow()

    init {
        // Apply user's default low-stock threshold as a hint
        viewModelScope.launch {
            val s = settingsRepository.get()
            _state.update {
                it.copy(stockThresholdRaw = s.defaultLowStockThreshold.toCleanString())
            }
        }
    }

    fun update(transform: (AddMedicationFormState) -> AddMedicationFormState) {
        _state.update(transform)
    }

    fun addTime(time: LocalTime) = update { s ->
        if (s.times.any { it == time }) s
        else s.copy(times = (s.times + time).sorted())
    }

    fun removeTime(time: LocalTime) = update { s ->
        s.copy(times = s.times.filterNot { it == time })
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                val now = Instant.now()
                val medication = Medication(
                    name = current.name.trim(),
                    unit = current.unit,
                    dosageAmount = current.dosageRaw.toFloat(),
                    notes = current.notes.takeIf { it.isNotBlank() },
                    startDate = current.startDate,
                    endDate = current.endDate,
                    stockAmount = if (current.trackStock) current.stockAmountRaw.toFloatOrNull() else null,
                    stockThreshold = if (current.trackStock) current.stockThresholdRaw.toFloatOrNull() else null,
                    createdAt = now,
                    updatedAt = now
                )
                val schedule = Schedule(
                    medicationId = 0,  // overridden in repo.add() to the inserted id
                    type = ScheduleType.DAILY_TIMES,
                    times = current.times,
                    mealRelation = current.mealRelation
                )

                val medId = medicationRepository.add(medication, listOf(schedule))

                // Pull the newly-inserted schedule's id back to register the alarm.
                val saved = medicationRepository.getSchedules(medId).firstOrNull()
                if (saved != null) {
                    scheduleNextDoseUseCase(medicationId = medId, scheduleId = saved.id)
                }
                onSaved(medId)
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, saveError = t.message) }
            }
        }
    }
}

private fun Float.toCleanString(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()
