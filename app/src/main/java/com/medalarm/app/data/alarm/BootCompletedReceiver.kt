package com.medalarm.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber

/**
 * On boot / package replace / locked boot, AlarmManager has lost every registration.
 * Enqueue a WorkManager job that re-registers everything for active medications.
 *
 * We use WorkManager (not a direct call into the use case) because:
 * - BroadcastReceiver execution is capped at ~10 seconds; rescheduling N alarms
 *   could exceed that on a busy device.
 * - WorkManager handles retries if the DB isn't ready yet (locked boot, etc.).
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.d("BootCompletedReceiver: ${intent.action} — enqueuing reschedule")
                val request = OneTimeWorkRequestBuilder<RescheduleAllWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    RescheduleAllWorker.UNIQUE_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
