package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Drives the no-response re-reminder loop. Invoked by AutoSnoozeReceiver one
 * snooze-interval after a medication reminder is posted.
 *
 * Behavior (per user spec):
 *  - If the dose was acted on (TAKEN/SKIPPED/MISSED) → [Result.Stop].
 *  - If the snooze cap (maxSnoozeCount, 0 = unlimited) is reached → [Result.Stop];
 *    the already-visible notification stays (it's never dismissed).
 *  - Otherwise it counts one snooze, schedules the next check one interval later,
 *    and returns [Result.ReAlert] so the receiver re-posts the (persistent)
 *    notification — re-alerting the user. The loop continues across SNOOZED state,
 *    which is the fix for "only the first notification ever arrives".
 */
class AutoReAlertUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val medicationRepository: MedicationRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmRegistrar: AlarmRegistrar
) {
    sealed interface Result {
        data object Stop : Result
        data class ReAlert(
            val dose: DoseLog,
            val medication: Medication,
            val snoozeStillAllowed: Boolean
        ) : Result
    }

    suspend operator fun invoke(doseLogId: Long, now: Instant = Instant.now()): Result {
        val log = doseLogRepository.get(doseLogId) ?: return Result.Stop
        // Stop only once the user has actually dealt with it (or it's stale).
        if (log.status == DoseStatus.TAKEN ||
            log.status == DoseStatus.SKIPPED ||
            log.status == DoseStatus.MISSED
        ) {
            return Result.Stop
        }

        val medication = medicationRepository.get(log.medicationId) ?: return Result.Stop
        if (!medication.isActive) return Result.Stop

        val settings = settingsRepository.get()
        val unlimited = settings.maxSnoozeCount == 0
        if (!unlimited && log.snoozeCount >= settings.maxSnoozeCount) {
            // Cap reached: stop re-alerting but leave the existing notification up.
            return Result.Stop
        }

        val interval = settings.defaultSnoozeMinutes.toLong()
        val nextAt = now.plus(interval, ChronoUnit.MINUTES)
        doseLogRepository.snooze(doseLogId, until = nextAt, at = now) // status=SNOOZED, snoozeCount++
        alarmRegistrar.scheduleAutoSnoozeCheck(doseLogId, nextAt)

        val updated = doseLogRepository.get(doseLogId) ?: return Result.Stop
        val snoozeStillAllowed = unlimited || updated.snoozeCount < settings.maxSnoozeCount
        return Result.ReAlert(updated, medication, snoozeStillAllowed)
    }
}
