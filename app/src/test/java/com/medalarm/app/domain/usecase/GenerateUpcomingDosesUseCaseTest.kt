package com.medalarm.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.MealRelation
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
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerateUpcomingDosesUseCaseTest {

    private lateinit var savedTz: TimeZone

    @BeforeAll fun pinUtc() {
        savedTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterAll fun restoreTz() {
        TimeZone.setDefault(savedTz)
    }

    private val now = Instant.parse("2025-01-15T06:00:00Z")

    private fun medication() = Medication(
        id = 1,
        name = "Aspirin",
        unit = MedicationUnit.TABLET,
        dosageAmount = 1f,
        startDate = LocalDate.parse("2025-01-01"),
        endDate = null,
        isActive = true,
        createdAt = now,
        updatedAt = now
    )

    private fun dailySchedule() = Schedule(
        id = 10,
        medicationId = 1,
        type = ScheduleType.DAILY_TIMES,
        times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        mealRelation = MealRelation.NONE
    )

    @Test
    fun `generates one pending per time per day in the window`() = runTest {
        val medRepo = FakeMedicationRepository(medication(), listOf(dailySchedule()))
        val doseRepo = FakeDoseLogRepository()
        val alarms = FakeAlarmRegistrar()
        val useCase = GenerateUpcomingDosesUseCase(medRepo, doseRepo, ScheduleCalculator(), alarms)

        useCase(medicationId = 1, windowDays = 2, now = now)

        // 2 times/day × 2 days (15th 08/20, 16th 08/20); 17th 08:00 is at/after windowEnd.
        assertThat(doseRepo.store.filter { it.status == DoseStatus.PENDING }).hasSize(4)
        // Soonest pending (15th 08:00) gets an alarm registered.
        assertThat(alarms.scheduled).hasSize(1)
        assertThat(alarms.scheduled.first().second)
            .isEqualTo(Instant.parse("2025-01-15T08:00:00Z"))
    }

    @Test
    fun `is idempotent — second run adds nothing`() = runTest {
        val medRepo = FakeMedicationRepository(medication(), listOf(dailySchedule()))
        val doseRepo = FakeDoseLogRepository()
        val useCase = GenerateUpcomingDosesUseCase(medRepo, doseRepo, ScheduleCalculator(), FakeAlarmRegistrar())

        useCase(medicationId = 1, windowDays = 2, now = now)
        val afterFirst = doseRepo.store.size
        useCase(medicationId = 1, windowDays = 2, now = now)

        assertThat(doseRepo.store).hasSize(afterFirst)
    }

    @Test
    fun `resetFuture cancels and rebuilds future pending`() = runTest {
        val medRepo = FakeMedicationRepository(medication(), listOf(dailySchedule()))
        val doseRepo = FakeDoseLogRepository()
        val alarms = FakeAlarmRegistrar()
        val useCase = GenerateUpcomingDosesUseCase(medRepo, doseRepo, ScheduleCalculator(), alarms)

        useCase(medicationId = 1, windowDays = 2, now = now)
        val firstIds = doseRepo.store.map { it.id }.toSet()

        useCase(medicationId = 1, windowDays = 2, resetFuture = true, now = now)

        // Old future pending were cancelled...
        assertThat(alarms.cancelled).containsAtLeastElementsIn(firstIds)
        // ...and rebuilt (fresh ids, same count).
        assertThat(doseRepo.store.filter { it.status == DoseStatus.PENDING }).hasSize(4)
        assertThat(doseRepo.store.map { it.id }.toSet()).containsNoneIn(firstIds)
    }
}

// ---- Minimal in-memory fakes (only the methods the use case touches) ----

private class FakeAlarmRegistrar : AlarmRegistrar {
    val scheduled = mutableListOf<Pair<Long, Instant>>()
    val cancelled = mutableListOf<Long>()
    val autoSnoozeScheduled = mutableListOf<Pair<Long, Instant>>()
    val autoSnoozeCancelled = mutableListOf<Long>()
    override fun scheduleExact(doseLogId: Long, fireAt: Instant) { scheduled += doseLogId to fireAt }
    override fun cancel(doseLogId: Long) { cancelled += doseLogId }
    override fun scheduleAutoSnoozeCheck(doseLogId: Long, fireAt: Instant) { autoSnoozeScheduled += doseLogId to fireAt }
    override fun cancelAutoSnoozeCheck(doseLogId: Long) { autoSnoozeCancelled += doseLogId }
}

private class FakeMedicationRepository(
    private val med: Medication,
    private val schedules: List<Schedule>
) : MedicationRepository {
    override suspend fun get(id: Long): Medication? = med.takeIf { it.id == id }
    override suspend fun getSchedules(medicationId: Long): List<Schedule> =
        schedules.filter { it.medicationId == medicationId }

    // Unused by the use case under test:
    override fun observeActive() = nyi()
    override fun observeAll() = nyi()
    override fun observe(id: Long) = nyi()
    override suspend fun add(medication: Medication, schedules: List<Schedule>) = nyi()
    override suspend fun update(medication: Medication) = nyi()
    override suspend fun softDelete(id: Long) = nyi()
    override suspend fun adjustStock(id: Long, amount: Float): StockAdjustResult = nyi()
    override suspend fun addStock(id: Long, amount: Float) = nyi()
    override suspend fun markLowStockNotified(id: Long) = nyi()
    override fun observeSchedules(medicationId: Long) = nyi()
    override suspend fun addSchedule(schedule: Schedule) = nyi()
    override suspend fun updateSchedule(schedule: Schedule) = nyi()
    override suspend fun deleteSchedule(scheduleId: Long) = nyi()
    override suspend fun getAllSchedulesForActiveMedications() = nyi()
    private fun nyi(): Nothing = throw NotImplementedError("not used in test")
}

private class FakeDoseLogRepository : DoseLogRepository {
    val store = mutableListOf<DoseLog>()
    private var nextId = 1L

    override suspend fun insert(log: DoseLog): Long {
        val id = nextId++
        store += log.copy(id = id)
        return id
    }

    override suspend fun findNextPending(medicationId: Long, scheduleId: Long, after: Instant): DoseLog? =
        store.filter {
            it.medicationId == medicationId && it.scheduleId == scheduleId &&
                it.status == DoseStatus.PENDING && it.scheduledAt.isAfter(after)
        }.minByOrNull { it.scheduledAt }

    override suspend fun getFuturePending(medicationId: Long, after: Instant): List<DoseLog> =
        store.filter {
            it.medicationId == medicationId && it.status == DoseStatus.PENDING &&
                it.scheduledAt.isAfter(after)
        }

    override suspend fun deleteFuturePending(medicationId: Long, after: Instant) {
        store.removeAll {
            it.medicationId == medicationId && it.status == DoseStatus.PENDING &&
                it.scheduledAt.isAfter(after)
        }
    }

    // Unused:
    override suspend fun get(id: Long) = nyi()
    override fun observe(id: Long): Flow<DoseLog?> = nyi()
    override fun observeRange(startInclusive: Instant, endExclusive: Instant) = nyi()
    override fun observeRangeForMedication(medicationId: Long, startInclusive: Instant, endExclusive: Instant) = nyi()
    override suspend fun insertAll(logs: List<DoseLog>) = nyi()
    override suspend fun findOverduePending(now: Instant) = nyi()
    override suspend fun markTaken(id: Long, at: Instant) = nyi()
    override suspend fun markSkipped(id: Long, at: Instant) = nyi()
    override suspend fun snooze(id: Long, until: Instant, at: Instant) = nyi()
    override suspend fun revertToPending(id: Long) = nyi()
    override suspend fun markOverdueAsMissed(threshold: Instant) = nyi()
    private fun nyi(): Nothing = throw NotImplementedError("not used in test")
}
