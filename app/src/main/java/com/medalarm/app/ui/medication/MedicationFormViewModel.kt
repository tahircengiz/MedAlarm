package com.medalarm.app.ui.medication

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.data.image.MedicationPhotoStore
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * One editable schedule block. A medication can have several (e.g. weekday 08:00
 * + weekend 10:00). The UI offers DAILY_TIMES and WEEKLY_DAYS; INTERVAL_HOURS is
 * supported by the domain but not yet exposed here.
 */
data class ScheduleDraft(
    val id: Long = 0,
    val type: ScheduleType = ScheduleType.DAILY_TIMES,
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val mealRelation: MealRelation = MealRelation.NONE
) {
    val isValid: Boolean
        get() = when (type) {
            ScheduleType.DAILY_TIMES -> times.isNotEmpty()
            ScheduleType.WEEKLY_DAYS -> times.isNotEmpty() && daysOfWeek.isNotEmpty()
            ScheduleType.INTERVAL_HOURS -> false // not offered in the form
        }
}

data class MedicationFormState(
    val editingId: Long? = null,
    val name: String = "",
    val unit: MedicationUnit = MedicationUnit.TABLET,
    val dosageRaw: String = "1",
    val colorHex: String? = null,
    val photoPath: String? = null,
    /** Last photo import failed (unreadable file, revoked Uri, …) — shown inline. */
    val photoImportFailed: Boolean = false,
    /** One-time (single-dose) medication: the form shows a simplified set of fields. */
    val isOneTime: Boolean = false,
    val schedules: List<ScheduleDraft> = listOf(ScheduleDraft()),
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
    val schedulesError: Boolean get() = schedules.isEmpty() || schedules.any { !it.isValid }

    val canSave: Boolean
        get() = !nameError && !dosageError && !schedulesError && !isSaving
}

@HiltViewModel
class MedicationFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val medicationRepository: MedicationRepository,
    private val settingsRepository: SettingsRepository,
    private val generateUpcomingDosesUseCase: GenerateUpcomingDosesUseCase,
    private val photoStore: MedicationPhotoStore
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationFormState(isLoading = true))
    val state: StateFlow<MedicationFormState> = _state.asStateFlow()

    private val editingId: Long? =
        savedStateHandle.get<String>(Routes.MEDICATION_ID_KEY)?.toLongOrNull()

    /** True when the add flow was opened in one-time (single-dose) mode. */
    private val oneTimeArg: Boolean = savedStateHandle.get<Boolean>(Routes.ONE_TIME_KEY) ?: false

    val isEditing: Boolean get() = editingId != null

    /** Photo of the loaded medication (editing only); deleted on save if replaced/removed. */
    private var originalPhotoPath: String? = null

    /** Files imported this session; all but the kept one are orphans to clean up. */
    private val importedPhotos = mutableSetOf<String>()
    private var savedSuccessfully = false

    init {
        viewModelScope.launch {
            val defaults = settingsRepository.get()
            if (editingId == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isOneTime = oneTimeArg,
                        // One-time meds are due on a single day; pin end == start.
                        endDate = if (oneTimeArg) it.startDate else it.endDate,
                        stockThresholdRaw = defaults.defaultLowStockThreshold.toCleanString()
                    )
                }
            } else {
                val med = medicationRepository.get(editingId)
                if (med == null) {
                    _state.update { it.copy(isLoading = false, saveError = "Medication not found") }
                    return@launch
                }
                val drafts = medicationRepository.getSchedules(editingId)
                    .map { it.toDraft() }
                    .ifEmpty { listOf(ScheduleDraft()) }
                originalPhotoPath = med.photoPath
                _state.update {
                    MedicationFormState(
                        editingId = editingId,
                        name = med.name,
                        unit = med.unit,
                        dosageRaw = med.dosageAmount.toCleanString(),
                        colorHex = med.colorHex,
                        photoPath = med.photoPath,
                        schedules = drafts,
                        startDate = med.startDate,
                        endDate = med.endDate,
                        trackStock = med.stockAmount != null,
                        stockAmountRaw = med.stockAmount?.toCleanString().orEmpty(),
                        stockThresholdRaw = med.stockThreshold?.toCleanString()
                            ?: defaults.defaultLowStockThreshold.toCleanString(),
                        notes = med.notes.orEmpty(),
                        // A saved medication whose window is a single day is a one-time med.
                        isOneTime = med.endDate != null && med.endDate == med.startDate,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun update(transform: (MedicationFormState) -> MedicationFormState) = _state.update(transform)

    // --- Schedule block operations ---

    fun addScheduleDraft() = update { s -> s.copy(schedules = s.schedules + ScheduleDraft()) }

    fun removeScheduleDraft(index: Int) = update { s ->
        if (s.schedules.size <= 1) s // keep at least one
        else s.copy(schedules = s.schedules.filterIndexed { i, _ -> i != index })
    }

    fun setScheduleType(index: Int, type: ScheduleType) = updateDraft(index) { it.copy(type = type) }

    fun setScheduleMeal(index: Int, rel: MealRelation) = updateDraft(index) { it.copy(mealRelation = rel) }

    fun addTime(index: Int, time: LocalTime) = updateDraft(index) { d ->
        if (d.times.any { it == time }) d else d.copy(times = (d.times + time).sorted())
    }

    fun removeTime(index: Int, time: LocalTime) = updateDraft(index) { d ->
        d.copy(times = d.times.filterNot { it == time })
    }

    fun toggleDay(index: Int, day: DayOfWeek) = updateDraft(index) { d ->
        d.copy(daysOfWeek = if (day in d.daysOfWeek) d.daysOfWeek - day else d.daysOfWeek + day)
    }

    private fun updateDraft(index: Int, transform: (ScheduleDraft) -> ScheduleDraft) = update { s ->
        s.copy(schedules = s.schedules.mapIndexed { i, d -> if (i == index) transform(d) else d })
    }

    // --- Treatment window mode (open-ended / date range / single day) ---

    fun setOpenEnded() = update { it.copy(endDate = null) }

    /** One-time: the medication is only due on [MedicationFormState.startDate]. Coerce
     *  schedules to fixed clock times — weekly rules make no sense for a single day. */
    fun setSingleDay() = update { s ->
        s.copy(
            endDate = s.startDate,
            schedules = s.schedules.map { it.copy(type = ScheduleType.DAILY_TIMES) }
        )
    }

    fun setDateRange() = update { s ->
        val end = if (s.endDate == null || !s.endDate.isAfter(s.startDate)) s.startDate.plusDays(6) else s.endDate
        s.copy(endDate = end)
    }

    /** Picks the single day; keeps start == end so it stays one-time. */
    fun setSingleDayDate(date: LocalDate) = update { it.copy(startDate = date, endDate = date) }

    /** One-time mode: a single dose at one clock time. Replaces the schedule list with
     *  one DAILY_TIMES block, preserving its id when editing so the row is updated. */
    fun setOneTimeTime(time: LocalTime) = update { s ->
        s.copy(
            schedules = listOf(
                ScheduleDraft(
                    id = s.schedules.firstOrNull()?.id ?: 0,
                    type = ScheduleType.DAILY_TIMES,
                    times = listOf(time)
                )
            )
        )
    }

    // --- Box photo ---

    /**
     * The camera capture target, parked in SavedStateHandle so it survives the
     * activity being recreated (or the process killed) while the system camera
     * app is foregrounded — otherwise the captured photo is silently dropped on
     * return. Uri is Parcelable, so SavedStateHandle stores it directly.
     */
    private var pendingCameraUri: Uri?
        get() = savedStateHandle[KEY_PENDING_CAMERA_URI]
        set(value) { savedStateHandle[KEY_PENDING_CAMERA_URI] = value }

    /** Uri target for the camera; the capture lands in our FileProvider cache. */
    fun newCameraCaptureUri(): Uri =
        photoStore.newCameraCaptureUri().also { pendingCameraUri = it }

    /**
     * Camera returned: hand the parked capture Uri back to the caller (to route
     * through the crop step) on success, then clear it. Returns null on failure.
     */
    fun consumeCameraCapture(success: Boolean): Uri? {
        val uri = pendingCameraUri
        pendingCameraUri = null
        return if (success) uri else null
    }

    /** The crop screen returned an error — surface it like an import failure. */
    fun onPhotoCropFailed() = _state.update { it.copy(photoImportFailed = true) }

    /** Imports a gallery pick or camera capture into internal storage. */
    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = photoStore.importFromUri(uri)
            if (path == null) {
                // Surface the failure — a silent no-op here cost us a beta cycle.
                _state.update { it.copy(photoImportFailed = true) }
                return@launch
            }
            importedPhotos += path
            _state.update { it.copy(photoPath = path, photoImportFailed = false) }
        }
    }

    fun removePhoto() = update { it.copy(photoPath = null, photoImportFailed = false) }

    /** Deletes files that are no longer referenced after a successful save. */
    private fun cleanUpPhotosAfterSave(keptPhoto: String?) {
        importedPhotos.filter { it != keptPhoto }.forEach { photoStore.deleteQuietly(it) }
        importedPhotos.clear()
        if (originalPhotoPath != null && originalPhotoPath != keptPhoto) {
            photoStore.deleteQuietly(originalPhotoPath)
        }
        savedSuccessfully = true
    }

    override fun onCleared() {
        // Form abandoned: drop every photo imported this session.
        if (!savedSuccessfully) importedPhotos.forEach { photoStore.deleteQuietly(it) }
        super.onCleared()
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
                    val schedules = current.schedules.map { it.toSchedule(medicationId = 0) }
                    val medId = medicationRepository.add(medication, schedules)
                    generateUpcomingDosesUseCase(medId)
                } else {
                    val existing = medicationRepository.get(editingId) ?: error("Medication missing")
                    medicationRepository.update(
                        existing.copy(
                            name = current.name.trim(),
                            unit = current.unit,
                            dosageAmount = current.dosageRaw.toFloat(),
                            colorHex = current.colorHex,
                            photoPath = current.photoPath,
                            notes = current.notes.takeIf { it.isNotBlank() },
                            startDate = current.startDate,
                            endDate = current.endDate,
                            stockAmount = if (current.trackStock) current.stockAmountRaw.toFloatOrNull() else null,
                            stockThreshold = if (current.trackStock) current.stockThresholdRaw.toFloatOrNull() else null,
                            updatedAt = now
                        )
                    )
                    syncSchedules(editingId, current.schedules)

                    // Rebuild the 14-day window from the new rule set; resetFuture clears
                    // stale future PENDING (removed/edited blocks) and cancels their alarms.
                    generateUpcomingDosesUseCase(editingId, resetFuture = true)
                }
                cleanUpPhotosAfterSave(current.photoPath)
                onSaved()
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, saveError = t.message) }
            }
        }
    }

    /** Diffs the draft list against the stored schedules: delete removed, update kept, insert new. */
    private suspend fun syncSchedules(medicationId: Long, drafts: List<ScheduleDraft>) {
        val existing = medicationRepository.getSchedules(medicationId)
        val keptIds = drafts.mapNotNull { it.id.takeIf { id -> id > 0 } }.toSet()
        existing.filter { it.id !in keptIds }.forEach { medicationRepository.deleteSchedule(it.id) }
        drafts.forEach { draft ->
            val schedule = draft.toSchedule(medicationId)
            if (draft.id > 0) medicationRepository.updateSchedule(schedule)
            else medicationRepository.addSchedule(schedule)
        }
    }

    private fun MedicationFormState.toNewMedication(now: Instant) = Medication(
        name = name.trim(),
        unit = unit,
        dosageAmount = dosageRaw.toFloat(),
        colorHex = colorHex,
        photoPath = photoPath,
        notes = notes.takeIf { it.isNotBlank() },
        startDate = startDate,
        endDate = endDate,
        stockAmount = if (trackStock) stockAmountRaw.toFloatOrNull() else null,
        stockThreshold = if (trackStock) stockThresholdRaw.toFloatOrNull() else null,
        createdAt = now,
        updatedAt = now
    )

    private fun ScheduleDraft.toSchedule(medicationId: Long) = Schedule(
        id = id,
        medicationId = medicationId,
        type = type,
        times = times,
        daysOfWeek = if (type == ScheduleType.WEEKLY_DAYS) daysOfWeek else emptySet(),
        mealRelation = mealRelation
    )

    private fun Schedule.toDraft() = ScheduleDraft(
        id = id,
        type = type,
        times = times.ifEmpty { listOf(LocalTime.of(8, 0)) },
        daysOfWeek = daysOfWeek.ifEmpty { DayOfWeek.values().toSet() },
        mealRelation = mealRelation
    )

    private companion object {
        const val KEY_PENDING_CAMERA_URI = "pending_camera_uri"
    }
}

private fun Float.toCleanString(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()
