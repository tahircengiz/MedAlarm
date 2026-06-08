package com.medalarm.app.data.alarm

import android.content.Context
import android.content.Intent

/**
 * Shared Intent constants for alarm + notification action plumbing.
 * Kept in one place so receivers, scheduler, and notification builder agree.
 */
object AlarmIntents {

    /** Notification action: user tapped "Aldım" / "Taken". */
    const val ACTION_DOSE_TAKEN = "com.medalarm.app.action.DOSE_TAKEN"

    /** Notification action: user tapped "Ertele" / "Snooze". */
    const val ACTION_DOSE_SNOOZE = "com.medalarm.app.action.DOSE_SNOOZE"

    /** Notification action: user tapped "Atla" / "Skip". */
    const val ACTION_DOSE_SKIP = "com.medalarm.app.action.DOSE_SKIP"

    /** Standard extra: which DoseLog row this is about. */
    const val EXTRA_DOSE_LOG_ID = "extra_dose_log_id"

    /** Standard extra: notification ID, so action receiver can dismiss the right one. */
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    /**
     * Build the Intent that fires when a scheduled alarm goes off.
     * Targets [MedicationAlarmReceiver] explicitly so it survives the lockdown
     * on implicit broadcasts in modern Android.
     */
    fun alarmFireIntent(context: Context, doseLogId: Long): Intent =
        Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_DOSE_LOG_ID, doseLogId)
        }

    fun actionIntent(context: Context, action: String, doseLogId: Long, notificationId: Int): Intent =
        Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_DOSE_LOG_ID, doseLogId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

    /**
     * Fires one snooze-interval after a reminder is posted. If the user hasn't
     * acted by then, [AutoSnoozeReceiver] re-alerts (and reschedules the next
     * check). Targets a different receiver than [alarmFireIntent], so it's a
     * distinct PendingIntent even though it shares the request-code derivation.
     */
    fun autoSnoozeIntent(context: Context, doseLogId: Long): Intent =
        Intent(context, AutoSnoozeReceiver::class.java).apply {
            putExtra(EXTRA_DOSE_LOG_ID, doseLogId)
        }
}
