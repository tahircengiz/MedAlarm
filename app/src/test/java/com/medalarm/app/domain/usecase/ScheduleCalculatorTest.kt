package com.medalarm.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.model.ScheduleType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.TimeZone

/**
 * Pure unit tests for next-fire-time computation. The calculator reads
 * ZoneId.systemDefault(), so we pin the default timezone to UTC for determinism.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScheduleCalculatorTest {

    private val calc = ScheduleCalculator()
    private lateinit var savedTz: TimeZone

    @BeforeAll
    fun pinUtc() {
        savedTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterAll
    fun restoreTz() {
        TimeZone.setDefault(savedTz)
    }

    private fun daily(vararg times: LocalTime) = Schedule(
        medicationId = 1,
        type = ScheduleType.DAILY_TIMES,
        times = times.toList(),
        mealRelation = MealRelation.NONE
    )

    // ---- DAILY_TIMES ----

    @Test
    fun `daily returns the next time later today`() {
        val schedule = daily(LocalTime.of(8, 0), LocalTime.of(20, 0))
        val now = Instant.parse("2025-01-15T06:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-15T08:00:00Z"))
    }

    @Test
    fun `daily skips a passed time and picks the next slot today`() {
        val schedule = daily(LocalTime.of(8, 0), LocalTime.of(20, 0))
        val now = Instant.parse("2025-01-15T09:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-15T20:00:00Z"))
    }

    @Test
    fun `daily rolls to tomorrow's first time when all today's passed`() {
        val schedule = daily(LocalTime.of(8, 0), LocalTime.of(20, 0))
        val now = Instant.parse("2025-01-15T21:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-16T08:00:00Z"))
    }

    @Test
    fun `daily with no times returns null`() {
        assertThat(calc.nextFireTime(daily(), Instant.parse("2025-01-15T06:00:00Z"))).isNull()
    }

    // ---- INTERVAL_HOURS ----

    private fun interval(hours: Int, start: LocalTime) = Schedule(
        medicationId = 1,
        type = ScheduleType.INTERVAL_HOURS,
        intervalHours = hours,
        intervalStartTime = start
    )

    @Test
    fun `interval returns the start slot when before it`() {
        val schedule = interval(6, LocalTime.of(8, 0))
        val now = Instant.parse("2025-01-15T07:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-15T08:00:00Z"))
    }

    @Test
    fun `interval steps forward by the interval`() {
        val schedule = interval(6, LocalTime.of(8, 0))
        val now = Instant.parse("2025-01-15T09:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-15T14:00:00Z"))
    }

    @Test
    fun `interval continues across midnight`() {
        // 08:00 +6 = 14, +6 = 20, +6 = 02:00 next day
        val schedule = interval(6, LocalTime.of(8, 0))
        val now = Instant.parse("2025-01-15T21:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-16T02:00:00Z"))
    }

    @Test
    fun `interval with null fields returns null`() {
        val schedule = Schedule(medicationId = 1, type = ScheduleType.INTERVAL_HOURS)
        assertThat(calc.nextFireTime(schedule, Instant.parse("2025-01-15T06:00:00Z"))).isNull()
    }

    // ---- WEEKLY_DAYS ----

    private fun weekly(days: Set<DayOfWeek>, vararg times: LocalTime) = Schedule(
        medicationId = 1,
        type = ScheduleType.WEEKLY_DAYS,
        times = times.toList(),
        daysOfWeek = days
    )

    @Test
    fun `weekly returns same-day time when today is a scheduled day and time is ahead`() {
        // 2025-01-13 is a Monday.
        val schedule = weekly(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0))
        val now = Instant.parse("2025-01-13T08:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-13T09:00:00Z"))
    }

    @Test
    fun `weekly rolls to next week when today's time has passed`() {
        val schedule = weekly(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0))
        val now = Instant.parse("2025-01-13T10:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-20T09:00:00Z"))
    }

    @Test
    fun `weekly finds the nearest of several days`() {
        val schedule = weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), LocalTime.of(9, 0))
        // Monday 2025-01-13 10:00 → next is Thursday 2025-01-16 09:00
        val now = Instant.parse("2025-01-13T10:00:00Z")
        assertThat(calc.nextFireTime(schedule, now))
            .isEqualTo(Instant.parse("2025-01-16T09:00:00Z"))
    }

    @Test
    fun `weekly with no days returns null`() {
        val schedule = weekly(emptySet(), LocalTime.of(9, 0))
        assertThat(calc.nextFireTime(schedule, Instant.parse("2025-01-13T08:00:00Z"))).isNull()
    }
}
