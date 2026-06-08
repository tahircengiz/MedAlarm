package com.medalarm.app.domain.model

/**
 * Unit of measurement for a medication. `OTHER` falls back to a free-text
 * label stored in [Medication.notes] — we don't add a column for a rare case.
 */
enum class MedicationUnit {
    TABLET, CAPSULE, ML, MG, DROP, PUFF, SACHET, OTHER
}

enum class ScheduleType {
    /** Discrete clock times each day, e.g. 08:00, 14:00, 20:00. */
    DAILY_TIMES,

    /** Every N hours, starting from a fixed time of day. */
    INTERVAL_HOURS,

    /** Specific days of the week (bitmask), at fixed times. */
    WEEKLY_DAYS
}

enum class MealRelation { NONE, BEFORE, AFTER, WITH }

enum class DoseStatus {
    /** Future or just-fired dose awaiting user action. */
    PENDING,

    /** User confirmed they took it. */
    TAKEN,

    /** User explicitly skipped (e.g. "skip this dose"). */
    SKIPPED,

    /** User snoozed; new alarm scheduled for [DoseLog.snoozeUntil]. */
    SNOOZED,

    /** PENDING dose whose scheduledAt is too far in the past with no action;
     *  flipped by the periodic worker so the UI doesn't accumulate stale items. */
    MISSED
}
