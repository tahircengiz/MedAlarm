package com.medalarm.app.ui.medication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.model.ScheduleType
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.domain.usecase.GenerateUpcomingDosesUseCase
import com.medalarm.app.ui.navigation.Routes
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
data class MedicationFormState(
    val editingId: Long? = null,
    val name: String = "",
    val unit: MedicationUnit = MedicationUnit.TABLET,
    val dosageRaw: String = "1",
    val colorHex: String? = null,
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val mealRelation: MealRelation = MealRelation.NONE,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val trackStock: Boolean = false,
    val stockAmountRaw: String = "",
    val stockThresholdRaw: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
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
class MedicationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicationRepository: MedicationRepository,
    private val settingsRepository: SettingsRepository,
    private val generateUpcomingDosesUseCase: GenerateUpcomingDosesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationFormState(isLoading = true))
    val state: StateFlow<MedicationFormState> = _state.asStateFlow()

    /** Edit mode if navigated to /medication/edit/{medicationId}; null otherwise. */
    private val editingId: Long? =
        savedStateHandle.get<String>(Routes.MEDICATION_ID_KEY)?.toLongOrNull()

    val isEditing: Boolean get() = editingId != null

    init {
        viewModelScope.launch {
            val defaults = settingsRepository.get()
            if (editingId == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        stockThresholdRaw = defaults.defaultLowStockThreshold.toCleanString()
                    )
                }
            } else {
                val med = medicationRepository.get(editingId)
                val schedule = medicationRepository.getSchedules(editingId).firstOrNull()
                if (med == null) {
                    _state.update { it.copy(isLoading = false, saveError = "Medication not found") }
                    return@launch
                }
                _state.update {
                    MedicationFormState(
                        editingId = editingId,
                        name = med.name,
                        unit = med.unit,
                        dosageRaw = med.dosageAmount.toCleanString(),
                        colorHex = med.colorHex,
                        times = schedule?.times ?: listOf(LocalTime.of(8, 0)),
                        mealRelation = schedule?.mealRelation ?: MealRelation.NONE,
                        startDate = med.startDate,
                        endDate = med.endDate,
                        trackStock = med.stockAmount != null,
                        stockAmountRaw = med.stockAmount?.toCleanString().orEmpty(),
                        stockThresholdRaw = med.stockThreshold?.toCleanString()
                            ?: defaults.defaultLowStockThreshold.toCleanString(),
                        notes = med.notes.orEmpty(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun update(transform: (MedicationFormState) -> MedicationFormState) {
        _state.update(transform)
    }

    fun addTime(time: LocalTime) = update { s ->
        if (s.times.any { it == time }) s
        else s.copy(times = (s.times + time).sorted())
    }

    fun removeTime(time: LocalTime) = update { s ->
        s.copy(times = s.times.filterNot { it == time })
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                val now = Instant.now()
                if (editingId == null) {
                    val medication = current.toNewMedication(now)
                    val schedule = current.toSchedule(medicationId = 0)
                    val medId = medicationRepository.add(medication, listOf(schedule))
                    generateUpcomingDosesUseCase(medId)
                } else {
                    val existing = medicationRepository.get(editingId) ?: error("Medication missing")
                    val updated = existing.copy(
                        name = current.name.trim(),
                        unit = current.unit,
                        dosageAmount = current.dosageRaw.toFloat(),
                        colorHex = current.colorHex,
                        notes = current.notes.takeIf { it.isNotBlank() },
                        startDate = current.startDate,
                        endDate = current.endDate,
                        stockAmount = if (current.trackStock) current.stockAmountRaw.toFloatOrNull() else null,
                        stockThreshold = if (current.trackStock) current.stockThresholdRaw.toFloatOrNull() else null,
                        updatedAt = now
                    )
                    medicationRepository.update(updated)

                    val existingSchedule = medicationRepository.getSchedules(editingId).firstOrNull()
                    val rewritten = current.toSchedule(medicationId = editingId)
                        .copy(id = existingSchedule?.id ?: 0)
                    if (existingSchedule == null) {
                        medicationRepository.addSchedule(rewritten)
                    } else {
                        medicationRepository.updateSchedule(rewritten)
                    }

                    // Regenerate the 14-day window with the updated schedule rules.
                    // Existing PENDING rows at the same scheduledAt are kept (dedup),
                    // so the user's interaction history isn't lost — only newly-shifted
                    // times produce new rows.
                    generateUpcomingDosesUseCase(editingId)
                }
                onSaved()
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, saveError = t.message) }
            }
        }
    }

    private fun MedicationFormState.toNewMedication(now: Instant) = Medication(
        name = name.trim(),
        unit = unit,
        dosageAmount = dosageRaw.toFloat(),
        colorHex = colorHex,
        notes = notes.takeIf { it.isNotBlank() },
        startDate = startDate,
        endDate = endDate,
        stockAmount = if (trackStock) stockAmountRaw.toFloatOrNull() else null,
        stockThreshold = if (trackStock) stockThresholdRaw.toFloatOrNull() else null,
        createdAt = now,
        updatedAt = now
    )

    private fun MedicationFormState.toSchedule(medicationId: Long) = Schedule(
        medicationId = medicationId,
        type = ScheduleType.DAILY_TIMES,
        times = times,
        mealRelation = mealRelation
    )
}

private fun Float.toCleanString(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()
