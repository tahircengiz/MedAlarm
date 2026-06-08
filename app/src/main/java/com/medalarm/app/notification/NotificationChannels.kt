package com.medalarm.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.core.content.getSystemService
import com.medalarm.app.R

object NotificationChannels {

    /** High-importance channel for actual dose reminders. */
    const val ID_MEDICATION_ALARM = "medication_alarm"

    /** Default-importance channel for stock-running-out reminders. */
    const val ID_LOW_STOCK = "low_stock"

    /**
     * Default-importance channel for warnings that the app's own ability to deliver
     * reminders is at risk (revoked permission, battery optimization, etc.).
     */
    const val ID_SYSTEM_HEALTH = "system_health"

    /**
     * Creates all channels. Safe to call repeatedly — Android merges by ID.
     * Call from [com.medalarm.app.MedAlarmApplication.onCreate].
     */
    fun createAll(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                ID_MEDICATION_ALARM,
                context.getString(R.string.channel_medication_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_medication_alarm_description)
                enableVibration(true)
                setBypassDnd(false)
                lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                ID_LOW_STOCK,
                context.getString(R.string.channel_low_stock_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_low_stock_description)
                enableVibration(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                ID_SYSTEM_HEALTH,
                context.getString(R.string.channel_system_health_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_system_health_description)
                enableVibration(false)
            }
        )
    }
}
