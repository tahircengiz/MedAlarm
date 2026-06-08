package com.medalarm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medalarm.app.data.local.entity.DoseLogEntity
import com.medalarm.app.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: DoseLogEntity): Long

    /**
     * Bulk insert used by the nightly PENDING-generation worker. Returns the
     * row IDs of newly-inserted logs; collisions (UNIQUE-style ignores via
     * application-level dedup) are skipped silently.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(logs: List<DoseLogEntity>): List<Long>

    @Update
    suspend fun update(log: DoseLogEntity)

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getById(id: Long): DoseLogEntity?

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    fun observeById(id: Long): Flow<DoseLogEntity?>

    @Query("""
        SELECT * FROM dose_logs
        WHERE scheduledAt BETWEEN :startInclusive AND :endExclusive
        ORDER BY scheduledAt
    """)
    fun observeRange(startInclusive: Long, endExclusive: Long): Flow<List<DoseLogEntity>>

    @Query("""
        SELECT * FROM dose_logs
        WHERE medicationId = :medId
          AND scheduledAt BETWEEN :startInclusive AND :endExclusive
        ORDER BY scheduledAt
    """)
    fun observeRangeForMedication(
        medId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<DoseLogEntity>>

    @Query("""
        SELECT * FROM dose_logs
        WHERE status = 'PENDING' AND scheduledAt <= :now
        ORDER BY scheduledAt
    """)
    suspend fun findOverduePending(now: Long): List<DoseLogEntity>

    /**
     * Returns the latest PENDING dose for a medication+schedule combination
     * that is still in the future. Used by the scheduler to avoid duplicates.
     */
    @Query("""
        SELECT * FROM dose_logs
        WHERE medicationId = :medId
          AND scheduleId = :scheduleId
          AND status = 'PENDING'
          AND scheduledAt > :after
        ORDER BY scheduledAt
        LIMIT 1
    """)
    suspend fun findNextPending(medId: Long, scheduleId: Long, after: Long): DoseLogEntity?

    @Query("UPDATE dose_logs SET status = :status, actionAt = :actionAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DoseStatus, actionAt: Long?)

    @Query("""
        UPDATE dose_logs
        SET status = 'SNOOZED',
            snoozeCount = snoozeCount + 1,
            snoozeUntil = :snoozeUntil,
            actionAt = :actionAt
        WHERE id = :id
    """)
    suspend fun snooze(id: Long, snoozeUntil: Long, actionAt: Long)

    @Query("""
        UPDATE dose_logs SET status = 'MISSED'
        WHERE status = 'PENDING' AND scheduledAt < :threshold
    """)
    suspend fun markOverdueAsMissed(threshold: Long): Int
}
