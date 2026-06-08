package com.medalarm.app.data.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medalarm.app.domain.usecase.RescheduleAllActiveUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * One-shot worker that re-registers AlarmManager triggers for every active
 * medication. Triggered by boot, time/timezone change, and the periodic system
 * health worker.
 */
@HiltWorker
class RescheduleAllWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rescheduleAllActiveUseCase: RescheduleAllActiveUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        rescheduleAllActiveUseCase()
        Timber.d("RescheduleAllWorker completed")
        Result.success()
    } catch (t: Throwable) {
        Timber.e(t, "RescheduleAllWorker failed")
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "reschedule_all"
    }
}
