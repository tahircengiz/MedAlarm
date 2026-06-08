@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medalarm.app.R
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            RangeChips(selected = state.range, onSelect = viewModel::setRange)
            Spacer(Modifier.height(12.dp))
            AdherenceSummary(taken = state.takenCount, total = state.totalCount)
            Spacer(Modifier.height(16.dp))
            if (state.buckets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.buckets.forEach { bucket ->
                        item(key = "header-${bucket.date}") {
                            Text(
                                bucket.date.format(DATE_FMT),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(bucket.doses, key = { it.id }) { dose ->
                            val med = state.medications[dose.medicationId] ?: return@items
                            DoseRow(dose, med)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeChips(selected: HistoryRange, onSelect: (HistoryRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = selected == HistoryRange.SEVEN,
            onClick = { onSelect(HistoryRange.SEVEN) },
            label = { Text(stringResource(R.string.history_range_7_days)) }
        )
        FilterChip(
            selected = selected == HistoryRange.THIRTY,
            onClick = { onSelect(HistoryRange.THIRTY) },
            label = { Text(stringResource(R.string.history_range_30_days)) }
        )
        FilterChip(
            selected = selected == HistoryRange.NINETY,
            onClick = { onSelect(HistoryRange.NINETY) },
            label = { Text(stringResource(R.string.history_range_90_days)) }
        )
    }
}

@Composable
private fun AdherenceSummary(taken: Int, total: Int) {
    val pct = if (total > 0) (taken * 100 / total) else 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.history_adherence, taken, total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                "$pct%",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DoseRow(dose: DoseLog, medication: Medication) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    dose.scheduledAt.atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FMT) +
                        " · " + formatDosage(dose.dosageAmountSnapshot),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(dose.status)
        }
    }
}

@Composable
private fun StatusPill(status: DoseStatus) {
    val (label, color) = when (status) {
        DoseStatus.TAKEN -> stringResource(R.string.history_status_taken) to MaterialTheme.colorScheme.primary
        DoseStatus.SKIPPED -> stringResource(R.string.history_status_skipped) to MaterialTheme.colorScheme.onSurfaceVariant
        DoseStatus.MISSED -> stringResource(R.string.history_status_missed) to MaterialTheme.colorScheme.error
        DoseStatus.SNOOZED -> stringResource(R.string.history_status_snoozed) to MaterialTheme.colorScheme.tertiary
        DoseStatus.PENDING -> stringResource(R.string.history_status_pending) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatDosage(amount: Float): String =
    if (amount % 1f == 0f) amount.toInt().toString() else amount.toString()
