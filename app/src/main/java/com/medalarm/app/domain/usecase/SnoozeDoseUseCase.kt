package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.SettingsRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Snoozes a dose by the user's configured minutes, capped by the configured
 * maxSnoozeCount. Returns the new fire-at time, or null if the cap is reached
 * (in which case the caller keeps the notification ongoing and removes the
 * snooze button).
 */
class SnoozeDoseUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmRegistrar: AlarmRegistrar
) {

    data class Result(val snoozedUntil: Instant?, val capReached: Boolean)

    suspend operator fun invoke(doseLogId: Long, now: Instant = Instant.now()): Result {
        val settings = settingsRepository.get()
        val log = doseLogRepository.get(doseLogId) ?: return Result(null, false)

        val unlimited = settings.maxSnoozeCount == 0
        if (!unlimited && log.snoozeCount >= settings.maxSnoozeCount) {
            return Result(snoozedUntil = null, capReached = true)
        }

        val until = now.plus(settings.defaultSnoozeMinutes.toLong(), ChronoUnit.MINUTES)
        doseLogRepository.snooze(doseLogId, until = until, at = now)
        // Re-alert via the auto-snooze path (re-posts the notification when it fires),
        // not the main dose alarm — keeps manual and automatic snooze on one mechanism.
        alarmRegistrar.scheduleAutoSnoozeCheck(doseLogId, until)
        return Result(snoozedUntil = until, capReached = false)
    }
}
