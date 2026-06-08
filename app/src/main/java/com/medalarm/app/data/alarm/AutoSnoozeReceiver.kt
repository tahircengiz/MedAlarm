package com.medalarm.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medalarm.app.domain.usecase.AutoReAlertUseCase
import com.medalarm.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Fires one snooze-interval after a reminder is posted. If the user hasn't acted,
 * [AutoReAlertUseCase] counts a snooze, schedules the next check, and tells us to
 * re-post the (persistent) notification so the user is alerted again. The loop
 * continues — across the SNOOZED state — up to the snooze cap. The notification
 * is never dismissed here, so there's no silent gap.
 */
@AndroidEntryPoint
class AutoSnoozeReceiver : BroadcastReceiver() {

    @Inject lateinit var autoReAlertUseCase: AutoReAlertUseCase
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getLongExtra(AlarmIntents.EXTRA_DOSE_LOG_ID, -1L)
        if (doseLogId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (val result = autoReAlertUseCase(doseLogId)) {
                    is AutoReAlertUseCase.Result.ReAlert ->
                        notificationHelper.postMedicationAlarm(
                            doseLog = result.dose,
                            medication = result.medication,
                            snoozeButtonEnabled = result.snoozeStillAllowed
                        )
                    AutoReAlertUseCase.Result.Stop -> Unit
                }
            } catch (t: Throwable) {
                Timber.e(t, "Auto re-alert failed doseLogId=$doseLogId")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
