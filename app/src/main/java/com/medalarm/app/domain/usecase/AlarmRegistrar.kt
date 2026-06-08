package com.medalarm.app.domain.usecase

import java.time.Instant

/**
 * Domain-layer boundary so use cases don't import [android.app.AlarmManager].
 * Implemented by `data/alarm/AndroidAlarmRegistrar`.
 */
interface AlarmRegistrar {
    fun scheduleExact(doseLogId: Long, fireAt: Instant)
    fun cancel(doseLogId: Long)

    /** Schedules the no-response auto-snooze check for a fired dose. */
    fun scheduleAutoSnoozeCheck(doseLogId: Long, fireAt: Instant)

    /** Cancels a pending auto-snooze check (e.g. when the user acts on the dose). */
    fun cancelAutoSnoozeCheck(doseLogId: Long)
}
