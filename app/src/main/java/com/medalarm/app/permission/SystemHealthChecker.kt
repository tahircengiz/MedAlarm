package com.medalarm.app.permission

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.medalarm.app.domain.model.UserSettings
import com.medalarm.app.notification.NotificationChannels
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateless query layer over Android's permission + system-setting surface.
 * Each check returns a [CheckStatus]; the report is the snapshot at call time.
 *
 * The OEM autostart check is special — there's no API to query it. We trust
 * the user's in-app confirmation (stored in [UserSettings.userConfirmedOemAutostart]).
 */
@Singleton
class SystemHealthChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun check(settings: UserSettings, oemAutostartSupported: Boolean): SystemHealthReport =
        SystemHealthReport(
            notificationPermission = checkNotificationPermission(),
            exactAlarmPermission = checkExactAlarm(),
            batteryOptimization = checkBatteryOptimization(),
            notificationsEnabled = checkNotificationsEnabled(),
            alarmChannelImportance = checkAlarmChannelImportance(),
            oemAutostart = checkOemAutostart(settings, oemAutostartSupported)
        )

    fun checkNotificationPermission(): CheckStatus =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) CheckStatus.OK
        else when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            PackageManager.PERMISSION_GRANTED -> CheckStatus.OK
            else -> CheckStatus.BLOCKED
        }

    fun checkExactAlarm(): CheckStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return CheckStatus.OK
        val am = context.getSystemService<AlarmManager>() ?: return CheckStatus.UNKNOWN
        return if (am.canScheduleExactAlarms()) CheckStatus.OK else CheckStatus.BLOCKED
    }

    fun checkBatteryOptimization(): CheckStatus {
        val pm = context.getSystemService<PowerManager>() ?: return CheckStatus.UNKNOWN
        val whitelisted = pm.isIgnoringBatteryOptimizations(context.packageName)
        // Recommended but not strictly critical — we use NEEDS_ATTENTION not BLOCKED
        // so the user can decline and still use the app.
        return if (whitelisted) CheckStatus.OK else CheckStatus.NEEDS_ATTENTION
    }

    fun checkNotificationsEnabled(): CheckStatus =
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) CheckStatus.OK
        else CheckStatus.BLOCKED

    fun checkAlarmChannelImportance(): CheckStatus {
        val nm = context.getSystemService<NotificationManager>() ?: return CheckStatus.UNKNOWN
        val channel = nm.getNotificationChannel(NotificationChannels.ID_MEDICATION_ALARM)
            ?: return CheckStatus.UNKNOWN  // channel not yet created
        return if (channel.importance >= NotificationManager.IMPORTANCE_HIGH) CheckStatus.OK
        else CheckStatus.BLOCKED
    }

    private fun checkOemAutostart(settings: UserSettings, oemAutostartSupported: Boolean): CheckStatus =
        when {
            !oemAutostartSupported -> CheckStatus.OK
            settings.userConfirmedOemAutostart -> CheckStatus.OK
            else -> CheckStatus.UNKNOWN
        }
}
