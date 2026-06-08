package com.medalarm.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber

/**
 * On timezone change or manual time/date change, AlarmManager keeps its absolute
 * trigger times — but those times may no longer match the user's expected wall-clock.
 *
 * Re-derive every schedule from the current clock so an "08:00 every day" alarm
 * still fires at 08:00 in the new timezone.
 */
class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                Timber.d("TimeChangedReceiver: ${intent.action} — enqueuing reschedule")
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
