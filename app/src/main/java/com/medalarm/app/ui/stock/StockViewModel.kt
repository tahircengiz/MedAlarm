package com.medalarm.app.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** True when stock tracking is on and the amount is at or below the warn threshold. */
fun Medication.isLowStock(): Boolean =
    stockAmount != null && stockThreshold != null && stockAmount <= stockThreshold

data class StockUiState(
    val isLoading: Boolean = true,
    /** Medications with stock tracking enabled — low-stock ones first. */
    val tracked: List<Medication> = emptyList(),
    /** Active medications without stock tracking, so the user sees they're not counted. */
    val untracked: List<Medication> = emptyList()
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    val uiState: StateFlow<StockUiState> = medicationRepository.observeActive()
        .map { meds ->
            val (tracked, untracked) = meds.partition { it.stockAmount != null }
            StockUiState(
                isLoading = false,
                tracked = tracked.sortedWith(
                    compareByDescending<Medication> { it.isLowStock() }.thenBy { it.name.lowercase() }
                ),
                untracked = untracked.sortedBy { it.name.lowercase() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    /** Same semantics as the detail screen's add-stock: also re-arms the low-stock alert. */
    fun addStock(medicationId: Long, amount: Float) {
        viewModelScope.launch { medicationRepository.addStock(medicationId, amount) }
    }
}
