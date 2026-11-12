package com.medalarm.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.model.ScheduleType
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.StockAdjustResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalTime
import java.util.TimeZone

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PruneOrphanDosesUseCaseTest {

    private lateinit var savedTz: TimeZone

    @BeforeAll fun pinUtc() {
        savedTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterAll fun restoreTz() {
        TimeZone.setDefault(savedTz)
    }

    private fun dose(id: Long, medId: Long, atUtc: String, status: DoseStatus = DoseStatus.PENDING) = DoseLog(
        id = id,
        medicationId = medId,
        scheduleId = 10,
        scheduledAt = Instant.parse(atUtc),
        status = status,
        dosageAmountSnapshot = 1f,
        unitSnapshot = MedicationUnit.TABLET
    )

    private fun daily(medId: Long, vararg times: LocalTime) = Schedule(
        id = 10, medicationId = medId, type = ScheduleType.DAILY_TIMES, times = times.toList()
    )

    @Test
    fun `removes doses whose time of day matches no schedule time`() = runTest {
        // Schedule fires only at 08:30; a 09:30 twin lingers on two days, any status.
        val doseRepo = OrphanFakeDoseRepo(
            dose(1, 1, "2026-01-15T08:30:00Z", DoseStatus.TAKEN),
            dose(2, 1, "2026-01-15T09:30:00Z", DoseStatus.PENDING),   // orphan
            dose(3, 1, "2026-01-16T08:30:00Z", DoseStatus.SKIPPED),
            dose(4, 1, "2026-01-16T09:30:00Z", DoseStatus.SKIPPED)    // orphan (even skipped)
        )
        val medRepo = OrphanFakeMedRepo(mapOf(1L to listOf(daily(1, LocalTime.of(8, 30)))))

        val removed = PruneOrphanDosesUseCase(medRepo, doseRepo)()

        assertThat(removed).isEqualTo(2)
        assertThat(doseRepo.store.map { it.id }).containsExactly(1L, 3L)
    }

    @Test
    fun `keeps all of a medication's scheduled times`() = runTest {
        val doseRepo = OrphanFakeDoseRepo(
            dose(1, 1, "2026-01-15T08:00:00Z"),
            dose(2, 1, "2026-01-15T20:00:00Z"),
            dose(3, 1, "2026-01-15T14:00:00Z")   // orphan: not 08:00 or 20:00
        )
        val medRepo = OrphanFakeMedRepo(mapOf(1L to listOf(daily(1, LocalTime.of(8, 0), LocalTime.of(20, 0)))))

        val removed = PruneOrphanDosesUseCase(medRepo, doseRepo)()

        assertThat(removed).isEqualTo(1)
        assertThat(doseRepo.store.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `never prunes a medication with an interval schedule`() = runTest {
        val doseRepo = OrphanFakeDoseRepo(
            dose(1, 2, "2026-01-15T06:00:00Z"),
            dose(2, 2, "2026-01-15T13:17:00Z"),  // odd time, but interval meds are untouched
            dose(3, 2, "2026-01-15T21:45:00Z")
        )
        val interval = Schedule(
            id = 10, medicationId = 2, type = ScheduleType.INTERVAL_HOURS,
            intervalHours = 6, intervalStartTime = LocalTime.of(6, 0)
        )
        val medRepo = OrphanFakeMedRepo(mapOf(2L to listOf(interval)))

        val removed = PruneOrphanDosesUseCase(medRepo, doseRepo)()

        assertThat(removed).isEqualTo(0)
        assertThat(doseRepo.store).hasSize(3)
    }

    @Test
    fun `no-op when every dose matches its schedule`() = runTest {
        val doseRepo = OrphanFakeDoseRepo(
            dose(1, 1, "2026-01-15T08:30:00Z"),
            dose(2, 1, "2026-01-16T08:30:00Z")
        )
        val medRepo = OrphanFakeMedRepo(mapOf(1L to listOf(daily(1, LocalTime.of(8, 30)))))

        val removed = PruneOrphanDosesUseCase(medRepo, doseRepo)()

        assertThat(removed).isEqualTo(0)
        assertThat(doseRepo.store).hasSize(2)
    }

    @Test
    fun `leaves doses untouched when the medication has no schedules`() = runTest {
        val doseRepo = OrphanFakeDoseRepo(dose(1, 3, "2026-01-15T09:30:00Z"))
        val medRepo = OrphanFakeMedRepo(emptyMap())   // med 3 has no schedules

        val removed = PruneOrphanDosesUseCase(medRepo, doseRepo)()

        assertThat(removed).isEqualTo(0)
        assertThat(doseRepo.store).hasSize(1)
    }
}

// ---- Fakes (only the methods the use case touches) ----

private class OrphanFakeDoseRepo(vararg initial: DoseLog) : DoseLogRepository {
    val store = initial.toMutableList()

    override suspend fun getAll(): List<DoseLog> = store.toList()
    override suspend fun deleteByIds(ids: List<Long>) {
        store.removeAll { it.id in ids }
    }

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

private class OrphanFakeMedRepo(private val schedulesByMed: Map<Long, List<Schedule>>) : MedicationRepository {
    override suspend fun getSchedules(medicationId: Long): List<Schedule> =
        schedulesByMed[medicationId].orEmpty()

    override suspend fun get(id: Long): Medication? = nyi()
    override fun observeActive() = nyi()
    override fun observeAll() = nyi()
    override fun observe(id: Long) = nyi()
    override suspend fun add(medication: Medication, schedules: List<Schedule>) = nyi()
    override suspend fun update(medication: Medication) = nyi()
    override suspend fun softDelete(id: Long) = nyi()
    override suspend fun adjustStock(id: Long, amount: Float): StockAdjustResult = nyi()
    override suspend fun addStock(id: Long, amount: Float) = nyi()
    override suspend fun setStock(id: Long, amount: Float) = nyi()
    override suspend fun markLowStockNotified(id: Long) = nyi()
    override fun observeSchedules(medicationId: Long) = nyi()
    override suspend fun addSchedule(schedule: Schedule) = nyi()
    override suspend fun updateSchedule(schedule: Schedule) = nyi()
    override suspend fun deleteSchedule(scheduleId: Long) = nyi()
    override suspend fun getAllSchedulesForActiveMedications() = nyi()
    private fun nyi(): Nothing = throw NotImplementedError("not used in test")
}
