package com.medalarm.app.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.medalarm.app.domain.usecase.AlarmRegistrar
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [AlarmRegistrar]. Wraps [AlarmManager] with the
 * `setExactAndAllowWhileIdle` flavor — fires through Doze mode, which is the
 * whole point for a medication reminder.
 */
@Singleton
class AndroidAlarmRegistrar @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmRegistrar {

    private val alarmManager: AlarmManager? = context.getSystemService()

    override fun scheduleExact(doseLogId: Long, fireAt: Instant) {
        val pi = pendingIntent(AlarmIntents.alarmFireIntent(context, doseLogId), doseLogId, replace = true)
        setExact(pi, fireAt)
        Timber.d("Scheduled doseLogId=$doseLogId at $fireAt")
    }

    override fun cancel(doseLogId: Long) {
        val am = alarmManager ?: return
        val pi = pendingIntent(AlarmIntents.alarmFireIntent(context, doseLogId), doseLogId, replace = false)
        am.cancel(pi)
        pi.cancel()
    }

    override fun scheduleAutoSnoozeCheck(doseLogId: Long, fireAt: Instant) {
        val pi = pendingIntent(AlarmIntents.autoSnoozeIntent(context, doseLogId), doseLogId, replace = true)
        setExact(pi, fireAt)
        Timber.d("Scheduled auto-snooze check doseLogId=$doseLogId at $fireAt")
    }

    override fun cancelAutoSnoozeCheck(doseLogId: Long) {
        val am = alarmManager ?: return
        val pi = pendingIntent(AlarmIntents.autoSnoozeIntent(context, doseLogId), doseLogId, replace = false)
        am.cancel(pi)
        pi.cancel()
    }

    /** Shared exact-alarm scheduling with the Doze-friendly flavor + permission fallback. */
    private fun setExact(pi: PendingIntent, fireAt: Instant) {
        val am = alarmManager ?: return
        // On Android 12+ canScheduleExactAlarms must be true; we declare USE_EXACT_ALARM
        // (alarm-clock-class app), so it should be granted by default, but check defensively.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms — falling back to inexact.")
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.toEpochMilli(), pi)
            return
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.toEpochMilli(), pi)
    }

    /**
     * Builds the PendingIntent for [intent]. The auto-snooze and main-fire intents
     * target different receivers, so sharing [requestCodeFor] is safe — PendingIntent
     * identity also keys on the target component.
     */
    private fun pendingIntent(intent: android.content.Intent, doseLogId: Long, replace: Boolean): PendingIntent {
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (replace) PendingIntent.FLAG_UPDATE_CURRENT else 0
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(doseLogId),
            intent,
            flags
        )!!
    }

    /**
     * Truncating to Int is safe in practice — Long IDs grow strictly upward and
     * we'd need 2^31 rows before colliding. Mask out the sign bit defensively.
     */
    private fun requestCodeFor(doseLogId: Long): Int = (doseLogId.toInt() and Int.MAX_VALUE)
}
