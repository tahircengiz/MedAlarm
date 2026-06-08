@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.medalarm.app.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.medalarm.app.R
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.MedicationUnit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun MedicationFormScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MedicationFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val titleRes = if (viewModel.isEditing) R.string.med_edit_title else R.string.add_med_title

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save { onSaved() } },
                        enabled = state.canSave
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { BasicsSection(state, viewModel) }
            item { ScheduleSection(state, viewModel) }
            item { TreatmentWindowSection(state, viewModel) }
            item { StockSection(state, viewModel) }
            item { NotesSection(state, viewModel) }
            state.saveError?.let { err ->
                item {
                    Text(
                        stringResource(R.string.add_med_save_error) + ": " + err,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun BasicsSection(state: MedicationFormState, vm: MedicationFormViewModel) {
    Column {
        SectionLabel(stringResource(R.string.add_med_section_basics))
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { value -> vm.update { it.copy(name = value) } },
            label = { Text(stringResource(R.string.add_med_name)) },
            placeholder = { Text(stringResource(R.string.add_med_name_hint)) },
            singleLine = true,
            isError = state.name.isNotEmpty() && state.nameError,
            supportingText = {
                if (state.name.isNotEmpty() && state.nameError) {
                    Text(stringResource(R.string.add_med_name_required))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.dosageRaw,
            onValueChange = { v ->
                vm.update { it.copy(dosageRaw = v.filter { c -> c.isDigit() || c == '.' }) }
            },
            label = { Text(stringResource(R.string.add_med_dosage)) },
            placeholder = { Text(stringResource(R.string.add_med_dosage_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = state.dosageRaw.isNotEmpty() && state.dosageError,
            supportingText = {
                if (state.dosageRaw.isNotEmpty() && state.dosageError) {
                    Text(stringResource(R.string.add_med_dosage_invalid))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.add_med_unit), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MedicationUnit.values().forEach { unit ->
                FilterChip(
                    selected = state.unit == unit,
                    onClick = { vm.update { it.copy(unit = unit) } },
                    label = { Text(unit.localizedLabel()) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.add_med_color), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            com.medalarm.app.ui.common.MedicationPalette.swatches.forEach { swatch ->
                val isSelected = state.colorHex == swatch.hex
                Surface(
                    onClick = { vm.update { it.copy(colorHex = swatch.hex) } },
                    color = swatch.color,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(36.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.onSurface
                    ) else null
                ) { Box(Modifier.size(36.dp)) {} }
            }
        }
    }
}

@Composable
private fun MedicationUnit.localizedLabel(): String = when (this) {
    MedicationUnit.TABLET -> stringResource(R.string.unit_tablet)
    MedicationUnit.CAPSULE -> stringResource(R.string.unit_capsule)
    MedicationUnit.ML -> stringResource(R.string.unit_ml)
    MedicationUnit.MG -> stringResource(R.string.unit_mg)
    MedicationUnit.DROP -> stringResource(R.string.unit_drop)
    MedicationUnit.PUFF -> stringResource(R.string.unit_puff)
    MedicationUnit.SACHET -> stringResource(R.string.unit_sachet)
    MedicationUnit.OTHER -> stringResource(R.string.unit_other)
}

@Composable
private fun MealRelation.localizedLabel(): String = when (this) {
    MealRelation.NONE -> stringResource(R.string.meal_none)
    MealRelation.BEFORE -> stringResource(R.string.meal_before)
    MealRelation.AFTER -> stringResource(R.string.meal_after)
    MealRelation.WITH -> stringResource(R.string.meal_with)
}

@Composable
private fun ScheduleSection(state: MedicationFormState, vm: MedicationFormViewModel) {
    Column {
        SectionLabel(stringResource(R.string.add_med_section_schedule))
        Spacer(Modifier.height(8.dp))

        Text(stringResource(R.string.add_med_times), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        var showTimePicker by remember { mutableStateOf(false) }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            state.times.forEach { t ->
                AssistChip(
                    onClick = { vm.removeTime(t) },
                    label = { Text(t.format(TIME_FMT)) },
                    trailingIcon = {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
            AssistChip(
                onClick = { showTimePicker = true },
                label = { Text(stringResource(R.string.add_med_add_time)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
        }
        if (state.timesError) {
            Text(
                stringResource(R.string.add_med_at_least_one_time),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (showTimePicker) {
            TimePickerDialog(
                onDismiss = { showTimePicker = false },
                onTimeSelected = { t ->
                    vm.addTime(t)
                    showTimePicker = false
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.add_med_meal_relation), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MealRelation.values().forEach { rel ->
                FilterChip(
                    selected = state.mealRelation == rel,
                    onClick = { vm.update { it.copy(mealRelation = rel) } },
                    label = { Text(rel.localizedLabel()) }
                )
            }
        }
    }
}

@Composable
private fun TimePickerDialog(onDismiss: () -> Unit, onTimeSelected: (LocalTime) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = pickerState)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
                    }) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}

@Composable
private fun TreatmentWindowSection(state: MedicationFormState, vm: MedicationFormViewModel) {
    Column {
        SectionLabel(stringResource(R.string.add_med_section_treatment))
        Spacer(Modifier.height(8.dp))

        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showStartPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${stringResource(R.string.add_med_start_date)}: ${state.startDate.format(DATE_FMT)}")
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.endDate == null,
                onCheckedChange = { open ->
                    vm.update { s ->
                        if (open) s.copy(endDate = null)
                        else s.copy(endDate = s.startDate.plusDays(6))
                    }
                }
            )
            Text(stringResource(R.string.add_med_open_ended))
        }

        if (state.endDate != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${stringResource(R.string.add_med_end_date)}: ${state.endDate!!.format(DATE_FMT)}")
            }
        }

        if (showStartPicker) {
            DatePickerSimple(
                initial = state.startDate,
                onDismiss = { showStartPicker = false },
                onPick = { d ->
                    vm.update { it.copy(startDate = d) }
                    showStartPicker = false
                }
            )
        }
        if (showEndPicker) {
            DatePickerSimple(
                initial = state.endDate ?: state.startDate.plusDays(6),
                onDismiss = { showEndPicker = false },
                onPick = { d ->
                    vm.update { it.copy(endDate = d) }
                    showEndPicker = false
                }
            )
        }
    }
}

@Composable
private fun DatePickerSimple(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis ?: return@TextButton
                val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onPick(picked)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun StockSection(state: MedicationFormState, vm: MedicationFormViewModel) {
    Column {
        SectionLabel(stringResource(R.string.add_med_section_stock))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.trackStock,
                onCheckedChange = { v -> vm.update { it.copy(trackStock = v) } }
            )
            Text(stringResource(R.string.add_med_track_stock))
        }
        if (state.trackStock) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.stockAmountRaw,
                    onValueChange = { v ->
                        vm.update {
                            it.copy(stockAmountRaw = v.filter { c -> c.isDigit() || c == '.' })
                        }
                    },
                    label = { Text(stringResource(R.string.add_med_stock_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.stockThresholdRaw,
                    onValueChange = { v ->
                        vm.update {
                            it.copy(stockThresholdRaw = v.filter { c -> c.isDigit() || c == '.' })
                        }
                    },
                    label = { Text(stringResource(R.string.add_med_stock_threshold)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotesSection(state: MedicationFormState, vm: MedicationFormViewModel) {
    Column {
        SectionLabel(stringResource(R.string.add_med_section_notes))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.notes,
            onValueChange = { v -> vm.update { it.copy(notes = v) } },
            placeholder = { Text(stringResource(R.string.add_med_notes_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5
        )
    }
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
