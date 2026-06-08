package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.repository.MedicationRepository
import java.time.Instant
import javax.inject.Inject

/**
 * For each schedule belonging to an active medication, schedules the next dose.
 * Called by BootCompletedReceiver, TimeChangedReceiver, and the periodic health worker.
 *
 * Idempotent: [ScheduleNextDoseUseCase] dedups by reusing existing future PENDING
 * rows, and AlarmManager replaces previous PendingIntents with the same request code.
 */
class RescheduleAllActiveUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleNextDoseUseCase: ScheduleNextDoseUseCase
) {
    suspend operator fun invoke(now: Instant = Instant.now()) {
        val schedules = medicationRepository.getAllSchedulesForActiveMedications()
        schedules.forEach { schedule ->
            scheduleNextDoseUseCase(
                medicationId = schedule.medicationId,
                scheduleId = schedule.id,
                after = now
            )
        }
    }
}
