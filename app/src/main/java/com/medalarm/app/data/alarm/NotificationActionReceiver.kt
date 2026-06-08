package com.medalarm.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.medalarm.app.domain.usecase.AlarmRegistrar
import com.medalarm.app.domain.usecase.MarkDoseTakenUseCase
import com.medalarm.app.domain.usecase.SkipDoseUseCase
import com.medalarm.app.domain.usecase.SnoozeDoseUseCase
import com.medalarm.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var markDoseTakenUseCase: MarkDoseTakenUseCase
    @Inject lateinit var snoozeDoseUseCase: SnoozeDoseUseCase
    @Inject lateinit var skipDoseUseCase: SkipDoseUseCase
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmRegistrar: AlarmRegistrar

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val doseLogId = intent.getLongExtra(AlarmIntents.EXTRA_DOSE_LOG_ID, -1L)
        val notificationId = intent.getIntExtra(AlarmIntents.EXTRA_NOTIFICATION_ID, -1)
        if (doseLogId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Any explicit action cancels the pending no-response auto-snooze.
                alarmRegistrar.cancelAutoSnoozeCheck(doseLogId)
                when (action) {
                    AlarmIntents.ACTION_DOSE_TAKEN -> {
                        val result = markDoseTakenUseCase(doseLogId)
                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                        if (result.crossedThreshold && result.medication != null) {
                            notificationHelper.postLowStock(result.medication)
                        }
                    }
                    AlarmIntents.ACTION_DOSE_SNOOZE -> {
                        val result = snoozeDoseUseCase(doseLogId)
                        if (result.snoozedUntil != null && notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                        // If cap reached, leave notification visible — caller (UI) re-posts as ongoing.
                    }
                    AlarmIntents.ACTION_DOSE_SKIP -> {
                        skipDoseUseCase(doseLogId)
                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    }
                    else -> Timber.w("Unknown notification action: $action")
                }
            } catch (t: Throwable) {
                Timber.e(t, "Failed to process notification action $action doseLogId=$doseLogId")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
