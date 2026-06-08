package com.medalarm.app.domain.model

import java.time.Instant

/**
 * One row per planned dose. PENDING rows are generated nightly by
 * `GenerateDailyDoseLogsUseCase` for the next 24h.
 *
 * This is the source of truth for adherence — UI/reports query this table,
 * not the schedule rules directly.
 */
data class DoseLog(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long,

    val scheduledAt: Instant,
    val status: DoseStatus,

    /** When the user tapped a notification action (Taken/Snooze/Skip). */
    val actionAt: Instant? = null,
    val snoozeCount: Int = 0,
    /** If [status] is SNOOZED, the new fire-again time. */
    val snoozeUntil: Instant? = null,

    /** Snapshot of dosage at scheduling time — so historic reports reflect what
     *  was actually planned, not the medication's current (possibly edited) state. */
    val dosageAmountSnapshot: Float,
    val unitSnapshot: MedicationUnit
)
