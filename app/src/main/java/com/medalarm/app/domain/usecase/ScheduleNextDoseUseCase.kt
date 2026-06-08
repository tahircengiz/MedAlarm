package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Computes when the next dose should fire for a (medication, schedule) pair,
 * inserts a PENDING DoseLog row, and asks the scheduler to register an alarm
 * for it. Idempotent: if a future PENDING log already exists for the same
 * schedule, reuses that row instead of creating a duplicate.
 *
 * The actual AlarmManager interaction lives behind a typed boundary
 * ([AlarmRegistrar]) so the domain layer never imports Android APIs.
 */
class ScheduleNextDoseUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val calculator: ScheduleCalculator,
    private val alarmRegistrar: AlarmRegistrar
) {

    suspend operator fun invoke(medicationId: Long, scheduleId: Long, after: Instant = Instant.now()): Long? {
        val medication = medicationRepository.get(medicationId) ?: return null
        if (!medication.isActive) return null

        val schedule = medicationRepository.getSchedules(medicationId)
            .firstOrNull { it.id == scheduleId } ?: return null

        // Respect treatment window
        val fireAt = calculator.nextFireTime(schedule, after) ?: return null
        val fireDate = fireAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (fireDate.isBefore(medication.startDate)) {
            // Treatment hasn't started yet; schedule from startDate boundary instead.
            val startBoundary = medication.startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            val adjusted = calculator.nextFireTime(schedule, startBoundary) ?: return null
            return registerAlarm(medicationId, scheduleId, medication, adjusted)
        }
        if (medication.endDate != null && fireDate.isAfter(medication.endDate)) {
            // Treatment window over; nothing more to schedule.
            return null
        }

        return registerAlarm(medicationId, scheduleId, medication, fireAt)
    }

    private suspend fun registerAlarm(
        medicationId: Long,
        scheduleId: Long,
        medication: com.medalarm.app.domain.model.Medication,
        fireAt: Instant
    ): Long {
        // Reuse existing future PENDING log if one is already there for this schedule
        // — covers reschedule-after-boot without producing duplicates.
        val existing = doseLogRepository.findNextPending(medicationId, scheduleId, after = Instant.now())
        val doseLogId = if (existing != null && existing.scheduledAt == fireAt) {
            existing.id
        } else {
            doseLogRepository.insert(
                DoseLog(
                    medicationId = medicationId,
                    scheduleId = scheduleId,
                    scheduledAt = fireAt,
                    status = DoseStatus.PENDING,
                    dosageAmountSnapshot = medication.dosageAmount,
                    unitSnapshot = medication.unit
                )
            )
        }
        alarmRegistrar.scheduleExact(doseLogId, fireAt)
        return doseLogId
    }
}

/**
 * Boundary so the domain layer doesn't import [android.app.AlarmManager].
 * Implemented by `data/alarm/AndroidAlarmRegistrar`.
 */
interface AlarmRegistrar {
    fun scheduleExact(doseLogId: Long, fireAt: Instant)
    fun cancel(doseLogId: Long)
}
