package com.medalarm.app.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Rules that determine when reminders fire for a given medication.
 *
 * Only the fields relevant to [type] are populated; the others are null/empty.
 * Validation lives in [Schedule.validate] / use cases — the data class is permissive
 * to keep round-tripping through Room straightforward.
 */
data class Schedule(
    val id: Long = 0,
    val medicationId: Long,
    val type: ScheduleType,

    /** Used by [ScheduleType.DAILY_TIMES] and [ScheduleType.WEEKLY_DAYS]. */
    val times: List<LocalTime> = emptyList(),

    /** Used by [ScheduleType.INTERVAL_HOURS]. */
    val intervalHours: Int? = null,
    val intervalStartTime: LocalTime? = null,

    /** Used by [ScheduleType.WEEKLY_DAYS]. */
    val daysOfWeek: Set<DayOfWeek> = emptySet(),

    val mealRelation: MealRelation = MealRelation.NONE
) {
    /**
     * Returns null if the schedule is internally consistent, or a human-readable
     * error message otherwise. Callers decide whether to surface as a Toast,
     * inline form error, etc.
     */
    fun validate(): String? = when (type) {
        ScheduleType.DAILY_TIMES ->
            if (times.isEmpty()) "DAILY_TIMES requires at least one time" else null
        ScheduleType.INTERVAL_HOURS ->
            when {
                intervalHours == null || intervalHours <= 0 ->
                    "INTERVAL_HOURS requires intervalHours > 0"
                intervalStartTime == null ->
                    "INTERVAL_HOURS requires intervalStartTime"
                24 % intervalHours != 0 ->
                    // Not strictly required, but avoids drift across day boundaries;
                    // enforced by the UI's picker (1, 2, 3, 4, 6, 8, 12 h).
                    null
                else -> null
            }
        ScheduleType.WEEKLY_DAYS ->
            when {
                times.isEmpty() -> "WEEKLY_DAYS requires at least one time"
                daysOfWeek.isEmpty() -> "WEEKLY_DAYS requires at least one day"
                else -> null
            }
    }

    companion object {
        /** Bitmask representation used for Room storage. Mon=1, Tue=2, ..., Sun=64. */
        fun daysOfWeekToBitmask(days: Set<DayOfWeek>): Int =
            days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

        fun bitmaskToDaysOfWeek(mask: Int): Set<DayOfWeek> =
            DayOfWeek.values().filter { (mask and (1 shl (it.value - 1))) != 0 }.toSet()
    }
}
