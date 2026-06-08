package com.medalarm.app.domain.usecase

import java.time.Instant

/**
 * Domain-layer boundary so use cases don't import [android.app.AlarmManager].
 * Implemented by `data/alarm/AndroidAlarmRegistrar`.
 */
interface AlarmRegistrar {
    fun scheduleExact(doseLogId: Long, fireAt: Instant)
    fun cancel(doseLogId: Long)
}
