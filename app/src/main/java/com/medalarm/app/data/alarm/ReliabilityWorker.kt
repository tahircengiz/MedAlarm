package com.medalarm.app.data.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.usecase.RescheduleAllActiveUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Periodic safety net for alarm reliability. Android can silently drop a scheduled
 * alarm (Doze, OEM battery kills, exact-alarm revocation, force-stop). Because the
 * normal alarm chain only registers the *next* dose when one fires, a single
 * dropped alarm would otherwise stall a medication's reminders indefinitely.
 *
 * This worker:
 *  1. Re-arms the next alarm for every active medication (idempotent — existing
 *     PENDING rows are reused, so nothing is duplicated).
 *  2. Flips long-overdue PENDING doses (more than [OVERDUE_HOURS] past) to MISSED,
 *     so Home/History don't accumulate stale "pending" items.
 *
 * Scheduled both periodically (every 6h) and once on each app open.
 */
@HiltWorker
class ReliabilityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rescheduleAllActiveUseCase: RescheduleAllActiveUseCase,
    private val doseLogRepository: DoseLogRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val now = Instant.now()
        rescheduleAllActiveUseCase(now = now)
        val flipped = doseLogRepository.markOverdueAsMissed(
            threshold = now.minus(OVERDUE_HOURS, ChronoUnit.HOURS)
        )
        Timber.d("ReliabilityWorker: re-armed active meds, marked $flipped overdue as MISSED")
        Result.success()
    } catch (t: Throwable) {
        Timber.e(t, "ReliabilityWorker failed")
        Result.retry()
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "reliability_periodic"
        const val UNIQUE_ONESHOT_NAME = "reliability_oneshot"
        const val PERIOD_HOURS = 6L
        private const val OVERDUE_HOURS = 4L
    }
}
