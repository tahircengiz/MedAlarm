package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.model.ScheduleType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure computation: given a [Schedule] and a reference instant, returns the next
 * dose time. Stateless and side-effect free — easy to unit test.
 *
 * Always computes in the user's default timezone; alarms are stored as [Instant]
 * but interpreted as wall-clock by the user, so DST and timezone changes are
 * handled by recomputing from the schedule, not by trying to adjust the stored
 * instant.
 */
@Singleton
class ScheduleCalculator @Inject constructor() {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /**
     * Returns the next instant the schedule should fire strictly after [after].
     * Returns null only if the schedule is somehow exhausted (e.g. WEEKLY_DAYS
     * with no days set — caller should have validated upstream).
     */
    fun nextFireTime(schedule: Schedule, after: Instant): Instant? {
        return when (schedule.type) {
            ScheduleType.DAILY_TIMES -> nextDailyTimes(schedule.times, after)
            ScheduleType.INTERVAL_HOURS -> {
                val intervalHours = schedule.intervalHours ?: return null
                val startTime = schedule.intervalStartTime ?: return null
                nextInterval(intervalHours, startTime, after)
            }
            ScheduleType.WEEKLY_DAYS -> nextWeekly(
                times = schedule.times,
                daysOfWeek = schedule.daysOfWeek,
                after = after
            )
        }
    }

    private fun nextDailyTimes(times: List<LocalTime>, after: Instant): Instant? {
        if (times.isEmpty()) return null
        val sorted = times.sorted()
        val afterLocal = after.atZone(zone).toLocalDateTime()
        // Today's remaining times
        for (t in sorted) {
            val candidate = LocalDateTime.of(afterLocal.toLocalDate(), t)
            if (candidate.isAfter(afterLocal)) {
                return candidate.atZone(zone).toInstant()
            }
        }
        // Otherwise: tomorrow's first time
        val tomorrow = afterLocal.toLocalDate().plusDays(1)
        return LocalDateTime.of(tomorrow, sorted.first()).atZone(zone).toInstant()
    }

    private fun nextInterval(intervalHours: Int, startTime: LocalTime, after: Instant): Instant {
        require(intervalHours > 0) { "intervalHours must be > 0" }
        val afterLocal = after.atZone(zone).toLocalDateTime()
        // Today's first slot is startTime; each subsequent slot is +intervalHours
        // until we leave today, then continue rolling into tomorrow.
        var candidate = LocalDateTime.of(afterLocal.toLocalDate(), startTime)
        // Step forward until strictly after `after`
        while (!candidate.isAfter(afterLocal)) {
            candidate = candidate.plusHours(intervalHours.toLong())
        }
        return candidate.atZone(zone).toInstant()
    }

    private fun nextWeekly(
        times: List<LocalTime>,
        daysOfWeek: Set<java.time.DayOfWeek>,
        after: Instant
    ): Instant? {
        if (times.isEmpty() || daysOfWeek.isEmpty()) return null
        val sortedTimes = times.sorted()
        val afterLocal = after.atZone(zone).toLocalDateTime()

        // Look up to 7 days ahead (inclusive of today).
        for (dayOffset in 0..7) {
            val date: LocalDate = afterLocal.toLocalDate().plusDays(dayOffset.toLong())
            if (date.dayOfWeek !in daysOfWeek) continue
            for (t in sortedTimes) {
                val candidate = LocalDateTime.of(date, t)
                if (candidate.isAfter(afterLocal)) {
                    return candidate.atZone(zone).toInstant()
                }
            }
        }
        return null
    }
}
