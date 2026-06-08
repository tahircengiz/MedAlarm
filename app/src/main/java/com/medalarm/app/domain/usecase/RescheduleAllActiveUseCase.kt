package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.repository.MedicationRepository
import java.time.Instant
import javax.inject.Inject

/**
 * For each schedule belonging to an active medication, schedules the next dose.
 * Called by BootCompletedReceiver, TimeChangedReceiver, and the periodic health worker.
 *
 * Idempotent: [GenerateUpcomingDosesUseCase] dedups by skipping existing PENDING
 * rows at the same scheduledAt, and AlarmManager replaces previous PendingIntents
 * with the same request code.
 */
class RescheduleAllActiveUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val generateUpcomingDosesUseCase: GenerateUpcomingDosesUseCase
) {
    suspend operator fun invoke(now: Instant = Instant.now()) {
        // Repopulate the 14-day PENDING window for every active medication.
        // Idempotent — existing PENDING rows at unchanged times are preserved.
        val active = medicationRepository.getAllSchedulesForActiveMedications()
            .map { it.medicationId }
            .distinct()
        active.forEach { medicationId ->
            generateUpcomingDosesUseCase(medicationId, now = now)
        }
    }
}
