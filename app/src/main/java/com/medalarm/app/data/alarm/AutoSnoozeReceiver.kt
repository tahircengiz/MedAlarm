package com.medalarm.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.usecase.SnoozeDoseUseCase
import com.medalarm.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Fires [AlarmIntents.AUTO_SNOOZE_TIMEOUT_MINUTES] after a reminder is posted.
 * If the user still hasn't acted (dose is PENDING), the dose is auto-snoozed:
 * SnoozeDoseUseCase increments the snooze count, and — if the snooze cap hasn't
 * been reached — reschedules the main alarm for `defaultSnoozeMinutes` later.
 * When the main alarm re-fires it posts the notification again and schedules a
 * fresh auto-snooze check, so the loop continues until the user acts or the cap
 * is hit (after which the reliability worker eventually marks it MISSED).
 */
@AndroidEntryPoint
class AutoSnoozeReceiver : BroadcastReceiver() {

    @Inject lateinit var doseLogRepository: DoseLogRepository
    @Inject lateinit var snoozeDoseUseCase: SnoozeDoseUseCase
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getLongExtra(AlarmIntents.EXTRA_DOSE_LOG_ID, -1L)
        if (doseLogId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val log = doseLogRepository.get(doseLogId) ?: return@launch
                // Only auto-snooze if the user never acted on it.
                if (log.status != DoseStatus.PENDING) return@launch

                val result = snoozeDoseUseCase(doseLogId)
                if (result.snoozedUntil != null) {
                    // Snoozed: dismiss the stale notification; it re-posts when the
                    // rescheduled main alarm fires.
                    notificationHelper.cancel(doseLogId)
                }
                // If cap reached (snoozedUntil == null), leave the notification up.
            } catch (t: Throwable) {
                Timber.e(t, "Auto-snooze failed doseLogId=$doseLogId")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
