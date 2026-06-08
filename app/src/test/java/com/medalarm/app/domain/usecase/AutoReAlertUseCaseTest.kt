package com.medalarm.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.domain.model.UserSettings
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.domain.repository.StockAdjustResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AutoReAlertUseCaseTest {

    private val now = Instant.parse("2025-01-15T08:00:00Z")

    private fun med(active: Boolean = true) = Medication(
        id = 1, name = "Aspirin", unit = MedicationUnit.TABLET, dosageAmount = 1f,
        startDate = LocalDate.parse("2025-01-01"), isActive = active, createdAt = now, updatedAt = now
    )

    private fun dose(status: DoseStatus, snoozeCount: Int) = DoseLog(
        id = 7, medicationId = 1, scheduleId = 1,
        scheduledAt = now, status = status, snoozeCount = snoozeCount,
        dosageAmountSnapshot = 1f, unitSnapshot = MedicationUnit.TABLET
    )

    private fun useCase(
        dose: DoseLog,
        settings: UserSettings,
        medication: Medication = med(),
        alarms: FakeAlarms = FakeAlarms()
    ) = AutoReAlertUseCase(
        FakeDoseLogRepo(dose), FakeMedRepo(medication), FakeSettings(settings), alarms
    ) to alarms

    @Test
    fun `stops when already taken`() = runTest {
        val (uc, alarms) = useCase(dose(DoseStatus.TAKEN, 0), UserSettings())
        assertThat(uc(7, now)).isEqualTo(AutoReAlertUseCase.Result.Stop)
        assertThat(alarms.scheduled).isEmpty()
    }

    @Test
    fun `stops when snooze cap reached`() = runTest {
        val (uc, alarms) = useCase(dose(DoseStatus.SNOOZED, 3), UserSettings(maxSnoozeCount = 3))
        assertThat(uc(7, now)).isEqualTo(AutoReAlertUseCase.Result.Stop)
        assertThat(alarms.scheduled).isEmpty()
    }

    @Test
    fun `re-alerts and reschedules while below cap`() = runTest {
        val (uc, alarms) = useCase(
            dose(DoseStatus.PENDING, 0),
            UserSettings(maxSnoozeCount = 3, defaultSnoozeMinutes = 15)
        )
        val result = uc(7, now)
        assertThat(result).isInstanceOf(AutoReAlertUseCase.Result.ReAlert::class.java)
        result as AutoReAlertUseCase.Result.ReAlert
        assertThat(result.snoozeStillAllowed).isTrue() // count became 1, < 3
        assertThat(alarms.scheduled).hasSize(1)
        assertThat(alarms.scheduled.first().second)
            .isEqualTo(now.plusSeconds(15 * 60))
    }

    @Test
    fun `continues across the SNOOZED state — this is the bug fix`() = runTest {
        // A dose already snoozed twice must still re-alert (it used to bail on != PENDING).
        val (uc, _) = useCase(
            dose(DoseStatus.SNOOZED, 2),
            UserSettings(maxSnoozeCount = 3, defaultSnoozeMinutes = 15)
        )
        val result = uc(7, now)
        assertThat(result).isInstanceOf(AutoReAlertUseCase.Result.ReAlert::class.java)
        result as AutoReAlertUseCase.Result.ReAlert
        assertThat(result.snoozeStillAllowed).isFalse() // count became 3, not < 3
    }

    @Test
    fun `unlimited cap always re-alerts`() = runTest {
        val (uc, alarms) = useCase(dose(DoseStatus.SNOOZED, 99), UserSettings(maxSnoozeCount = 0))
        assertThat(uc(7, now)).isInstanceOf(AutoReAlertUseCase.Result.ReAlert::class.java)
        assertThat(alarms.scheduled).hasSize(1)
    }
}

// ---- Fakes ----

private class FakeAlarms : AlarmRegistrar {
    val scheduled = mutableListOf<Pair<Long, Instant>>()
    override fun scheduleExact(doseLogId: Long, fireAt: Instant) = Unit
    override fun cancel(doseLogId: Long) = Unit
    override fun scheduleAutoSnoozeCheck(doseLogId: Long, fireAt: Instant) { scheduled += doseLogId to fireAt }
    override fun cancelAutoSnoozeCheck(doseLogId: Long) = Unit
}

private class FakeDoseLogRepo(private var stored: DoseLog) : DoseLogRepository {
    override suspend fun get(id: Long): DoseLog? = stored.takeIf { it.id == id }
    override suspend fun snooze(id: Long, until: Instant, at: Instant) {
        stored = stored.copy(status = DoseStatus.SNOOZED, snoozeCount = stored.snoozeCount + 1, snoozeUntil = until)
    }
    override fun observe(id: Long): Flow<DoseLog?> = nyi()
    override fun observeRange(startInclusive: Instant, endExclusive: Instant) = nyi()
    override fun observeRangeForMedication(medicationId: Long, startInclusive: Instant, endExclusive: Instant) = nyi()
    override suspend fun insert(log: DoseLog) = nyi()
    override suspend fun insertAll(logs: List<DoseLog>) = nyi()
    override suspend fun findOverduePending(now: Instant) = nyi()
    override suspend fun findNextPending(medicationId: Long, scheduleId: Long, after: Instant) = nyi()
    override suspend fun markTaken(id: Long, at: Instant) = nyi()
    override suspend fun markSkipped(id: Long, at: Instant) = nyi()
    override suspend fun revertToPending(id: Long) = nyi()
    override suspend fun markOverdueAsMissed(threshold: Instant) = nyi()
    override suspend fun getFuturePending(medicationId: Long, after: Instant) = nyi()
    override suspend fun deleteFuturePending(medicationId: Long, after: Instant) = nyi()
    private fun nyi(): Nothing = throw NotImplementedError()
}

private class FakeMedRepo(private val med: Medication) : MedicationRepository {
    override suspend fun get(id: Long): Medication? = med.takeIf { it.id == id }
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
    override suspend fun getSchedules(medicationId: Long) = nyi()
    override suspend fun addSchedule(schedule: Schedule) = nyi()
    override suspend fun updateSchedule(schedule: Schedule) = nyi()
    override suspend fun deleteSchedule(scheduleId: Long) = nyi()
    override suspend fun getAllSchedulesForActiveMedications() = nyi()
    private fun nyi(): Nothing = throw NotImplementedError()
}

private class FakeSettings(private val fixed: UserSettings) : SettingsRepository {
    override val settings: Flow<UserSettings> get() = nyi()
    override suspend fun get(): UserSettings = fixed
    override suspend fun setLanguage(language: AppLanguage) = nyi()
    override suspend fun setThemeMode(mode: ThemeMode) = nyi()
    override suspend fun setUseDynamicColor(value: Boolean) = nyi()
    override suspend fun setTtsEnabled(value: Boolean) = nyi()
    override suspend fun setDefaultSnoozeMinutes(minutes: Int) = nyi()
    override suspend fun setMaxSnoozeCount(count: Int) = nyi()
    override suspend fun setVibrationEnabled(value: Boolean) = nyi()
    override suspend fun setNotificationSoundUri(uri: String?) = nyi()
    override suspend fun setDefaultLowStockThreshold(value: Float) = nyi()
    override suspend fun setDisclaimerAccepted() = nyi()
    override suspend fun setOnboardingCompleted(value: Boolean) = nyi()
    override suspend fun setUserConfirmedOemAutostart(value: Boolean) = nyi()
    private fun nyi(): Nothing = throw NotImplementedError()
}
