@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medalarm.app.R
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.ui.common.resolveMedicationColor
import java.time.format.DateTimeFormatter

@Composable
fun MedicationDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: MedicationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    var showAddStock by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.med_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, enabled = state.medication != null) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { confirmDelete = true }, enabled = state.medication != null) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            )
        }
    ) { padding ->
        val med = state.medication ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HeaderCard(med)
            Spacer(Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.med_detail_schedule))
            Spacer(Modifier.height(8.dp))
            state.schedules.forEach { ScheduleCard(it) }

            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.med_detail_treatment))
            Spacer(Modifier.height(8.dp))
            Text(
                if (med.endDate == null) {
                    "${med.startDate.format(DATE_FMT)} — ${stringResource(R.string.med_detail_open_ended)}"
                } else {
                    "${med.startDate.format(DATE_FMT)} — ${med.endDate.format(DATE_FMT)}"
                },
                style = MaterialTheme.typography.bodyLarge
            )

            if (med.stockAmount != null) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.med_detail_stock))
                Spacer(Modifier.height(8.dp))
                val amount = if (med.stockAmount % 1f == 0f) med.stockAmount.toInt().toString()
                else med.stockAmount.toString()
                val threshold = med.stockThreshold?.let { t ->
                    if (t % 1f == 0f) t.toInt().toString() else t.toString()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$amount ${med.unit.name.lowercase()}" +
                            if (threshold != null) " (≤ $threshold ⚠️)" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.TextButton(onClick = { showAddStock = true }) {
                        Text(stringResource(R.string.med_add_stock))
                    }
                }
            }

            if (!med.notes.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.med_detail_notes))
                Spacer(Modifier.height(8.dp))
                Text(med.notes, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { viewModel.togglePause() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (med.isActive) stringResource(R.string.med_pause)
                    else stringResource(R.string.med_resume)
                )
            }
        }
    }

    if (confirmDelete) {
        val med = state.medication
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.med_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.med_delete_confirm_body, med?.name.orEmpty()))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted)
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddStock) {
        var amountRaw by remember { mutableStateOf("") }
        val amount = amountRaw.toFloatOrNull()
        AlertDialog(
            onDismissRequest = { showAddStock = false },
            title = { Text(stringResource(R.string.med_add_stock_title)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = amountRaw,
                    onValueChange = { v -> amountRaw = v.filter { it.isDigit() || it == '.' } },
                    label = { Text(stringResource(R.string.med_add_stock_amount)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = amount != null && amount > 0f,
                    onClick = {
                        amount?.let { viewModel.addStock(it) }
                        showAddStock = false
                    }
                ) {
                    Text(stringResource(R.string.med_add_stock_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStock = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun HeaderCard(med: Medication) {
    val accent = resolveMedicationColor(med.colorHex)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                med.name,
                style = MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(Modifier.height(4.dp))
            val amount = if (med.dosageAmount % 1f == 0f) med.dosageAmount.toInt().toString()
            else med.dosageAmount.toString()
            Text(
                "$amount ${med.unit.name.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ScheduleCard(schedule: Schedule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                schedule.times.joinToString(" · ") { it.format(TIME_FMT) },
                style = MaterialTheme.typography.titleMedium
            )
            if (schedule.type == com.medalarm.app.domain.model.ScheduleType.WEEKLY_DAYS &&
                schedule.daysOfWeek.isNotEmpty()
            ) {
                Spacer(Modifier.height(4.dp))
                Text(
                    schedule.daysOfWeek
                        .sortedBy { it.value }
                        .joinToString(", ") {
                            it.getDisplayName(
                                java.time.format.TextStyle.SHORT,
                                java.util.Locale.getDefault()
                            )
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (schedule.mealRelation != MealRelation.NONE) {
                Spacer(Modifier.height(4.dp))
                Text(
                    schedule.mealRelation.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
