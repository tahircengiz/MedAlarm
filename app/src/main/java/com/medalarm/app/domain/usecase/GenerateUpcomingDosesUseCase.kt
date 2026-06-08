package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Materializes PENDING DoseLog rows for the next [windowDays] for one medication.
 *
 * Why this exists:
 * - Home / History show all planned doses for a given day. If only the next dose
 *   were stored, multi-time schedules (08:00 / 14:00 / 20:00 etc.) would show only
 *   one PENDING row at a time, which makes "tomorrow's plan" impossible to render.
 * - The next AlarmManager trigger is registered for the soonest PENDING; when it
 *   fires, the receiver just finds the next PENDING and registers the next alarm
 *   — no recomputation needed until the window starts to expire.
 *
 * Idempotent: before inserting any row we look up an existing PENDING with the
 * same (medicationId, scheduleId, scheduledAt). Calling this twice in a row is
 * cheap and produces no duplicates.
 */
class GenerateUpcomingDosesUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val calculator: ScheduleCalculator,
    private val alarmRegistrar: AlarmRegistrar
) {

    /**
     * @param resetFuture when true, all future PENDING doses for this medication are
     *        cancelled (alarms) and deleted before regenerating. Use this on schedule
     *        edits so old-time rows don't linger alongside the new ones. Leave false
     *        for the additive top-up path (new medication, alarm-fire, boot).
     */
    suspend operator fun invoke(
        medicationId: Long,
        windowDays: Int = DEFAULT_WINDOW_DAYS,
        resetFuture: Boolean = false,
        now: Instant = Instant.now()
    ) {
        val medication = medicationRepository.get(medicationId) ?: return
        if (!medication.isActive) return
        val schedules = medicationRepository.getSchedules(medicationId)
        if (schedules.isEmpty()) return

        if (resetFuture) {
            // Cancel + delete stale future PENDING so an edited schedule doesn't
            // leave the old times behind.
            doseLogRepository.getFuturePending(medicationId, after = now).forEach { stale ->
                alarmRegistrar.cancel(stale.id)
            }
            doseLogRepository.deleteFuturePending(medicationId, after = now)
        }

        val zone = ZoneId.systemDefault()
        // Window end is derived from `now` (not the wall clock) so the result is
        // deterministic and testable, and consistent with the cursor below.
        val windowEnd = now.atZone(zone).toLocalDate()
            .plusDays(windowDays.toLong())
            .atStartOfDay(zone)
            .toInstant()

        schedules.forEach { schedule ->
            populateSchedule(medication, schedule, windowEnd, now)
            // Re-register the alarm for the soonest PENDING in this schedule.
            // (Calls are idempotent in AlarmManager — same request code overwrites.)
            val next = doseLogRepository.findNextPending(
                medicationId = medication.id,
                scheduleId = schedule.id,
                after = now
            )
            if (next != null) {
                alarmRegistrar.scheduleExact(next.id, next.scheduledAt)
            }
        }
    }

    private suspend fun populateSchedule(
        medication: Medication,
        schedule: Schedule,
        windowEnd: Instant,
        now: Instant
    ) {
        val zone = ZoneId.systemDefault()
        val medStart = medication.startDate.atStartOfDay(zone).toInstant()
        val medEndExclusive = medication.endDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()

        // Start stepping forward from max(now, medication.startDate).
        var cursor = if (now.isBefore(medStart)) medStart else now

        while (true) {
            val next = calculator.nextFireTime(schedule, after = cursor) ?: return
            if (!next.isBefore(windowEnd)) return
            if (medEndExclusive != null && !next.isBefore(medEndExclusive)) return

            // Dedup: is there already a PENDING at this exact scheduledAt?
            val existing = doseLogRepository.findNextPending(
                medicationId = medication.id,
                scheduleId = schedule.id,
                after = next.minusMillis(1L)
            )
            if (existing == null || existing.scheduledAt != next) {
                doseLogRepository.insert(
                    DoseLog(
                        medicationId = medication.id,
                        scheduleId = schedule.id,
                        scheduledAt = next,
                        status = DoseStatus.PENDING,
                        dosageAmountSnapshot = medication.dosageAmount,
                        unitSnapshot = medication.unit
                    )
                )
            }
            cursor = next
        }
    }

    companion object {
        const val DEFAULT_WINDOW_DAYS = 14
    }
}
