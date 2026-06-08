package com.medalarm.app.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MedicationListUiState(
    val active: List<Medication> = emptyList(),
    val inactive: List<Medication> = emptyList()
)

@HiltViewModel
class MedicationListViewModel @Inject constructor(
    medicationRepository: MedicationRepository
) : ViewModel() {

    val uiState: StateFlow<MedicationListUiState> = medicationRepository.observeAll()
        .map { all ->
            MedicationListUiState(
                active = all.filter { it.isActive }.sortedBy { it.name.lowercase() },
                inactive = all.filter { !it.isActive }.sortedBy { it.name.lowercase() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationListUiState())
}
