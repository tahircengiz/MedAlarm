package com.medalarm.app.domain.repository

import com.medalarm.app.domain.model.DoseLog
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DoseLogRepository {

    suspend fun get(id: Long): DoseLog?
    fun observe(id: Long): Flow<DoseLog?>

    fun observeRange(startInclusive: Instant, endExclusive: Instant): Flow<List<DoseLog>>
    fun observeRangeForMedication(
        medicationId: Long,
        startInclusive: Instant,
        endExclusive: Instant
    ): Flow<List<DoseLog>>

    suspend fun insert(log: DoseLog): Long
    suspend fun insertAll(logs: List<DoseLog>): List<Long>

    suspend fun findOverduePending(now: Instant): List<DoseLog>
    suspend fun findNextPending(medicationId: Long, scheduleId: Long, after: Instant): DoseLog?

    suspend fun markTaken(id: Long, at: Instant)
    suspend fun markSkipped(id: Long, at: Instant)
    suspend fun snooze(id: Long, until: Instant, at: Instant)

    /** Bulk-flips PENDING doses older than [threshold] to MISSED. Returns count flipped. */
    suspend fun markOverdueAsMissed(threshold: Instant): Int

    /** Future, not-yet-acted doses for a medication (scheduledAt > [after]). */
    suspend fun getFuturePending(medicationId: Long, after: Instant): List<DoseLog>

    /** Deletes future PENDING doses for a medication. Caller cancels their alarms first. */
    suspend fun deleteFuturePending(medicationId: Long, after: Instant)
}
