package com.medalarm.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.domain.usecase.ScheduleNextDoseUseCase
import com.medalarm.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Fires when AlarmManager triggers a scheduled dose. Loads the dose + medication,
 * posts the notification, and schedules the next dose for the same (med, schedule).
 *
 * Uses goAsync() to keep the receiver alive while the coroutine does its work —
 * the standard pattern for receivers that need to do I/O.
 */
@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var doseLogRepository: DoseLogRepository
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var scheduleNextDoseUseCase: ScheduleNextDoseUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getLongExtra(AlarmIntents.EXTRA_DOSE_LOG_ID, -1L)
        if (doseLogId == -1L) {
            Timber.w("MedicationAlarmReceiver received without doseLogId")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val log = doseLogRepository.get(doseLogId) ?: return@launch
                val medication = medicationRepository.get(log.medicationId) ?: return@launch
                if (!medication.isActive) return@launch

                val settings = settingsRepository.get()
                val snoozeEnabled = settings.maxSnoozeCount == 0 ||
                    log.snoozeCount < settings.maxSnoozeCount

                notificationHelper.postMedicationAlarm(
                    doseLog = log,
                    medication = medication,
                    snoozeButtonEnabled = snoozeEnabled
                )

                // Schedule the NEXT dose for this medication/schedule. We do this
                // immediately rather than waiting for the user's action, so the
                // chain survives even if they swipe the notification away.
                scheduleNextDoseUseCase(
                    medicationId = log.medicationId,
                    scheduleId = log.scheduleId
                )
            } catch (t: Throwable) {
                Timber.e(t, "Failed to handle medication alarm doseLogId=$doseLogId")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
