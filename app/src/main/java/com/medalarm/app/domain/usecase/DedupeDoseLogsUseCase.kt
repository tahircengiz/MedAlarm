package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.repository.DoseLogRepository
import java.time.Instant
import javax.inject.Inject

/**
 * One-time heal for data corrupted by the pre-fix resurrection bug.
 *
 * Before [GenerateUpcomingDosesUseCase] was taught to dedup against *any* status,
 * a dose the user had already acted on (SKIPPED/TAKEN/SNOOZED/MISSED) could be
 * re-inserted as a fresh PENDING at the same slot on every generation pass. That
 * left duplicate rows for the same `(medicationId, scheduleId, scheduledAt)` —
 * which is why skipped doses "came back" and why past days show phantom
 * PENDING/MISSED entries alongside the real action.
 *
 * This collapses each such slot back to a single row, keeping the one that best
 * reflects the user's intent (an explicit action beats an automatic status):
 *   TAKEN > SKIPPED > SNOOZED > MISSED > PENDING.
 *
 * Idempotent and cheap: after the prevention fix no new duplicates appear, so on
 * every subsequent run this finds nothing to delete. Safe to call from the
 * reliability worker on each app open. Stale AlarmManager alarms for deleted rows
 * no-op when they fire (the receiver looks the dose up and returns if it's gone),
 * and the worker re-arms the correct next alarm right after.
 */
class DedupeDoseLogsUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository
) {

    /** @return number of duplicate rows removed. */
    suspend operator fun invoke(): Int {
        val toDelete = doseLogRepository.getAll()
            .groupBy { Slot(it.medicationId, it.scheduleId, it.scheduledAt) }
            .values
            .filter { it.size > 1 }
            .flatMap { group ->
                val keeper = group.minWith(KEEP_ORDER)
                group.asSequence().map { it.id }.filter { it != keeper.id }.toList()
            }
        doseLogRepository.deleteByIds(toDelete)
        return toDelete.size
    }

    private data class Slot(val medicationId: Long, val scheduleId: Long, val scheduledAt: Instant)

    private companion object {
        /** Lower rank wins. An explicit user action outranks an automatic status. */
        fun rank(status: DoseStatus): Int = when (status) {
            DoseStatus.TAKEN -> 0
            DoseStatus.SKIPPED -> 1
            DoseStatus.SNOOZED -> 2
            DoseStatus.MISSED -> 3
            DoseStatus.PENDING -> 4
        }

        // Within a slot: best status, then the earliest action, then the lowest id.
        val KEEP_ORDER: Comparator<DoseLog> = compareBy(
            { rank(it.status) },
            { it.actionAt ?: Instant.MAX },
            { it.id }
        )
    }
}
