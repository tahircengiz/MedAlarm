package com.medalarm.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class HistoryRange(val days: Long) { SEVEN(7), THIRTY(30), NINETY(90) }

data class HistoryDayBucket(
    val date: LocalDate,
    val doses: List<DoseLog>
)

data class HistoryUiState(
    val range: HistoryRange = HistoryRange.SEVEN,
    val medications: Map<Long, Medication> = emptyMap(),
    val buckets: List<HistoryDayBucket> = emptyList(),
    val takenCount: Int = 0,
    val totalCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository
) : ViewModel() {

    private val selectedRange = MutableStateFlow(HistoryRange.SEVEN)

    val uiState: StateFlow<HistoryUiState> = combine(
        medicationRepository.observeAll(),
        selectedRange.flatMapLatest { range ->
            val (start, end) = rangeBounds(range)
            doseLogRepository.observeRange(start, end)
        },
        selectedRange
    ) { meds, doses, range ->
        val byId = meds.associateBy { it.id }
        val buckets = doses
            .groupBy { it.scheduledAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, list) -> HistoryDayBucket(date, list.sortedByDescending { it.scheduledAt }) }
        val taken = doses.count { it.status == DoseStatus.TAKEN }
        // For adherence denominator we count all non-pending statuses (TAKEN+SKIPPED+MISSED+SNOOZED).
        // Pending future doses aren't part of the user's track record yet.
        val total = doses.count { it.status != DoseStatus.PENDING }
        HistoryUiState(
            range = range,
            medications = byId,
            buckets = buckets,
            takenCount = taken,
            totalCount = total
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(range: HistoryRange) {
        selectedRange.value = range
    }

    private fun rangeBounds(range: HistoryRange): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.minusDays(range.days - 1).atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }
}
