package com.medalarm.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

class DedupeDoseLogsUseCaseTest {

    private val slot = Instant.parse("2025-01-16T08:00:00Z")
    private val other = Instant.parse("2025-01-16T20:00:00Z")

    private fun dose(
        id: Long,
        status: DoseStatus,
        at: Instant = slot,
        actionAt: Instant? = null
    ) = DoseLog(
        id = id,
        medicationId = 1,
        scheduleId = 10,
        scheduledAt = at,
        status = status,
        actionAt = actionAt,
        dosageAmountSnapshot = 1f,
        unitSnapshot = MedicationUnit.TABLET
    )

    @Test
    fun `collapses a resurrected slot and keeps the user's SKIPPED action`() = runTest {
        val repo = FakeRepo(
            dose(1, DoseStatus.SKIPPED, actionAt = Instant.parse("2025-01-15T09:00:00Z")),
            dose(2, DoseStatus.PENDING), // resurrected duplicate
            dose(3, DoseStatus.PENDING, at = other) // unrelated, untouched
        )

        val removed = DedupeDoseLogsUseCase(repo)()

        assertThat(removed).isEqualTo(1)
        assertThat(repo.store.map { it.id }).containsExactly(1L, 3L)
        assertThat(repo.store.first { it.scheduledAt == slot }.status).isEqualTo(DoseStatus.SKIPPED)
    }

    @Test
    fun `keeps TAKEN over every other status in the same slot`() = runTest {
        val repo = FakeRepo(
            dose(1, DoseStatus.MISSED),
            dose(2, DoseStatus.TAKEN, actionAt = Instant.parse("2025-01-16T08:05:00Z")),
            dose(3, DoseStatus.PENDING)
        )

        val removed = DedupeDoseLogsUseCase(repo)()

        assertThat(removed).isEqualTo(2)
        assertThat(repo.store.single().id).isEqualTo(2L)
    }

    @Test
    fun `no duplicates — nothing removed`() = runTest {
        val repo = FakeRepo(
            dose(1, DoseStatus.SKIPPED),
            dose(2, DoseStatus.PENDING, at = other)
        )

        val removed = DedupeDoseLogsUseCase(repo)()

        assertThat(removed).isEqualTo(0)
        assertThat(repo.store).hasSize(2)
    }
}

private class FakeRepo(vararg initial: DoseLog) : DoseLogRepository {
    val store = initial.toMutableList()

    override suspend fun getAll(): List<DoseLog> = store.toList()
    override suspend fun deleteByIds(ids: List<Long>) {
        store.removeAll { it.id in ids }
    }

    // Unused:
    override suspend fun get(id: Long) = nyi()
    override fun observe(id: Long): Flow<DoseLog?> = nyi()
    override fun observeRange(startInclusive: Instant, endExclusive: Instant) = nyi()
    override fun observeRangeForMedication(medicationId: Long, startInclusive: Instant, endExclusive: Instant) = nyi()
    override suspend fun insert(log: DoseLog) = nyi()
    override suspend fun insertAll(logs: List<DoseLog>) = nyi()
    override suspend fun findOverduePending(now: Instant) = nyi()
    override suspend fun findNextPending(medicationId: Long, scheduleId: Long, after: Instant) = nyi()
    override suspend fun findAtSlot(medicationId: Long, scheduleId: Long, scheduledAt: Instant) = nyi()
    override suspend fun markTaken(id: Long, at: Instant) = nyi()
    override suspend fun markSkipped(id: Long, at: Instant) = nyi()
    override suspend fun snooze(id: Long, until: Instant, at: Instant) = nyi()
    override suspend fun revertToPending(id: Long) = nyi()
    override suspend fun markOverdueAsMissed(threshold: Instant) = nyi()
    override suspend fun getFuturePending(medicationId: Long, after: Instant) = nyi()
    override suspend fun deleteFuturePending(medicationId: Long, after: Instant) = nyi()
    private fun nyi(): Nothing = throw NotImplementedError("not used in test")
}
