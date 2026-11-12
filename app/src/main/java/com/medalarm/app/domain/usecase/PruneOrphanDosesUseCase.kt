package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.ScheduleType
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * One-time heal for "orphan" dose rows whose time-of-day matches none of their
 * medication's current schedule times.
 *
 * Context: some medications carry a second daily dose at a shifted time — e.g. the
 * schedule fires at 08:30 but rows also exist at 09:30 — that corresponds to no
 * active schedule. [GenerateUpcomingDosesUseCase] never creates these (it generates
 * strictly from the schedule), so they are stale leftovers from a schedule time that
 * was later edited or removed. They surface as a duplicate reminder on every day,
 * past and future, which [DedupeDoseLogsUseCase] cannot fix (it only collapses rows
 * at the *same* exact slot, not a shifted-time twin).
 *
 * This removes every dose whose local time-of-day is not among its medication's
 * schedule times. To stay safe it only judges medications whose schedules are all
 * time-based (DAILY_TIMES / WEEKLY_DAYS): a medication with any INTERVAL_HOURS
 * schedule is left untouched, since interval schedules have no fixed set of clock
 * times to validate against. A medication with no schedules at all is also skipped.
 *
 * Destructive and intentionally NOT idempotent across schedule edits — removing a
 * time from a schedule would make genuine past rows at that time look orphan — so
 * the caller gates it to run exactly once (see [com.medalarm.app.data.alarm.ReliabilityWorker]).
 * Stale AlarmManager alarms for deleted rows no-op when they fire (the receiver
 * looks the dose up and returns if it's gone), and the worker re-arms the correct
 * next alarm right after.
 */
class PruneOrphanDosesUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository
) {

    /** @return number of orphan rows removed. */
    suspend operator fun invoke(): Int {
        val doses = doseLogRepository.getAll()
        if (doses.isEmpty()) return 0
        val zone = ZoneId.systemDefault()

        val toDelete = mutableListOf<Long>()
        doses.groupBy { it.medicationId }.forEach { (medicationId, medDoses) ->
            val schedules = medicationRepository.getSchedules(medicationId)
            // Can't judge a medication we have no schedules for, and never prune one
            // with an interval schedule (no fixed clock times to compare against).
            if (schedules.isEmpty()) return@forEach
            if (schedules.any { it.type == ScheduleType.INTERVAL_HOURS }) return@forEach

            val allowedTimes = schedules
                .flatMap { it.times }
                .map { it.truncatedTo(ChronoUnit.MINUTES) }
                .toSet()
            if (allowedTimes.isEmpty()) return@forEach

            medDoses.forEach { dose ->
                val timeOfDay = dose.scheduledAt.atZone(zone)
                    .toLocalTime()
                    .truncatedTo(ChronoUnit.MINUTES)
                if (timeOfDay !in allowedTimes) toDelete += dose.id
            }
        }

        doseLogRepository.deleteByIds(toDelete)
        return toDelete.size
    }
}
