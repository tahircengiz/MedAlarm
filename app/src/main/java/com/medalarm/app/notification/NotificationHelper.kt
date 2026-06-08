package com.medalarm.app.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medalarm.app.MainActivity
import com.medalarm.app.R
import com.medalarm.app.data.alarm.AlarmIntents
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.Medication
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun postMedicationAlarm(doseLog: DoseLog, medication: Medication, snoozeButtonEnabled: Boolean) {
        val notifId = notificationIdFor(doseLog.id)

        val tapIntent = PendingIntent.getActivity(
            context,
            notifId,
            android.content.Intent(context, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenPi = actionPendingIntent(
            doseLog.id, notifId, AlarmIntents.ACTION_DOSE_TAKEN, requestSuffix = 1
        )
        val snoozePi = actionPendingIntent(
            doseLog.id, notifId, AlarmIntents.ACTION_DOSE_SNOOZE, requestSuffix = 2
        )
        val skipPi = actionPendingIntent(
            doseLog.id, notifId, AlarmIntents.ACTION_DOSE_SKIP, requestSuffix = 3
        )

        val title = medication.name
        val text = buildString {
            append(formatDosage(medication))
            medication.notes?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.ID_MEDICATION_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Don't auto-dismiss — the user must explicitly act.
            .setAutoCancel(false)
            .setOngoing(!snoozeButtonEnabled) // once snooze limit hit, becomes ongoing
            .addAction(0, context.getString(R.string.action_taken), takenPi)
            .addAction(0, context.getString(R.string.action_skip), skipPi)

        if (snoozeButtonEnabled) {
            builder.addAction(0, context.getString(R.string.action_snooze), snoozePi)
        }

        safelyNotify(notifId, builder.build())
    }

    fun postLowStock(medication: Medication) {
        if (medication.stockAmount == null) return
        val notifId = lowStockNotificationIdFor(medication.id)
        val text = context.getString(
            R.string.stock_low_body,
            medication.name,
            formatRemainingStock(medication)
        )

        val tapIntent = PendingIntent.getActivity(
            context,
            notifId,
            android.content.Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.ID_LOW_STOCK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.stock_low_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        safelyNotify(notifId, builder.build())
    }

    fun cancel(doseLogId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(doseLogId))
    }

    private fun actionPendingIntent(
        doseLogId: Long,
        notificationId: Int,
        action: String,
        requestSuffix: Int
    ): PendingIntent {
        val intent = AlarmIntents.actionIntent(context, action, doseLogId, notificationId)
        // Request code derived from notificationId + per-action suffix so the three
        // actions for the same notification don't clobber each other.
        val requestCode = notificationId * 10 + requestSuffix
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun safelyNotify(id: Int, notification: android.app.Notification) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission revoked between check and post.
        }
    }

    private fun formatDosage(med: Medication): String {
        val amount = if (med.dosageAmount % 1f == 0f) med.dosageAmount.toInt().toString()
        else med.dosageAmount.toString()
        return "$amount ${med.unit.name.lowercase()}"
    }

    private fun formatRemainingStock(med: Medication): String {
        val amount = med.stockAmount ?: return ""
        val amountStr = if (amount % 1f == 0f) amount.toInt().toString() else amount.toString()
        return "$amountStr ${med.unit.name.lowercase()}"
    }

    companion object {
        // Stable IDs derived from the DB row. Long → Int truncation is safe in practice.
        fun notificationIdFor(doseLogId: Long): Int = doseLogId.toInt() and Int.MAX_VALUE
        fun lowStockNotificationIdFor(medicationId: Long): Int =
            (1_000_000_000 + medicationId).toInt() and Int.MAX_VALUE
    }
}
