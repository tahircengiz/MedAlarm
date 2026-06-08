package com.medalarm.app.data.repository

import com.medalarm.app.data.local.dao.DoseLogDao
import com.medalarm.app.data.local.mapper.toDomain
import com.medalarm.app.data.local.mapper.toEntity
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class DoseLogRepositoryImpl(
    private val dao: DoseLogDao
) : DoseLogRepository {

    override suspend fun get(id: Long): DoseLog? = dao.getById(id)?.toDomain()

    override fun observe(id: Long): Flow<DoseLog?> = dao.observeById(id).map { it?.toDomain() }

    override fun observeRange(startInclusive: Instant, endExclusive: Instant): Flow<List<DoseLog>> =
        dao.observeRange(startInclusive.toEpochMilli(), endExclusive.toEpochMilli())
            .map { list -> list.map { it.toDomain() } }

    override fun observeRangeForMedication(
        medicationId: Long,
        startInclusive: Instant,
        endExclusive: Instant
    ): Flow<List<DoseLog>> =
        dao.observeRangeForMedication(
            medicationId,
            startInclusive.toEpochMilli(),
            endExclusive.toEpochMilli()
        ).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(log: DoseLog): Long = dao.insert(log.toEntity())

    override suspend fun insertAll(logs: List<DoseLog>): List<Long> =
        dao.insertAll(logs.map { it.toEntity() })

    override suspend fun findOverduePending(now: Instant): List<DoseLog> =
        dao.findOverduePending(now.toEpochMilli()).map { it.toDomain() }

    override suspend fun findNextPending(medicationId: Long, scheduleId: Long, after: Instant): DoseLog? =
        dao.findNextPending(medicationId, scheduleId, after.toEpochMilli())?.toDomain()

    override suspend fun markTaken(id: Long, at: Instant) {
        dao.updateStatus(id, DoseStatus.TAKEN, at.toEpochMilli())
    }

    override suspend fun markSkipped(id: Long, at: Instant) {
        dao.updateStatus(id, DoseStatus.SKIPPED, at.toEpochMilli())
    }

    override suspend fun snooze(id: Long, until: Instant, at: Instant) {
        dao.snooze(id, until.toEpochMilli(), at.toEpochMilli())
    }

    override suspend fun markOverdueAsMissed(threshold: Instant): Int =
        dao.markOverdueAsMissed(threshold.toEpochMilli())
}
