@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.medalarm.app.R
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.permission.SystemHealthReport
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMedication: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshHealth()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.home_action_history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.home_action_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.medications.isNotEmpty()) {
                FloatingActionButton(onClick = onAddMedication) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.home_action_add))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.healthReport?.takeIf { it.hasCriticalIssue }?.let {
                CriticalBanner(onClick = onOpenSettings)
            }

            if (state.medications.isEmpty()) {
                EmptyState(onAdd = onAddMedication)
            } else {
                DaySelector(
                    selectedDate = state.selectedDate,
                    onPrev = { viewModel.shiftDay(-1) },
                    onNext = { viewModel.shiftDay(1) },
                    onJumpToday = { viewModel.jumpToToday() }
                )
                DoseList(
                    medications = state.medications,
                    doses = state.doses,
                    onOpenMedication = onOpenMedication
                )
            }
        }
    }
}

@Composable
private fun CriticalBanner(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.home_critical_banner),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onClick) {
                Text(stringResource(R.string.home_action_settings))
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_add_first))
            }
        }
    }
}

@Composable
private fun DoseList(
    medications: List<Medication>,
    doses: List<DoseLog>,
    onOpenMedication: (Long) -> Unit
) {
    val byId = remember(medications) { medications.associateBy { it.id } }

    if (doses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.home_no_doses_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(doses, key = { it.id }) { dose ->
                val med = byId[dose.medicationId]
                if (med != null) {
                    DoseCard(dose = dose, medication = med, onClick = { onOpenMedication(med.id) })
                }
            }
        }
    }
}

@Composable
private fun DoseCard(dose: DoseLog, medication: Medication, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (dose.status) {
                DoseStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer
                DoseStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
                DoseStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatTime(dose)} · ${formatDose(medication)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = dose.status.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpToday: () -> Unit
) {
    val today = LocalDate.now()
    val label = when (selectedDate) {
        today -> stringResource(R.string.home_today)
        today.plusDays(1) -> stringResource(R.string.home_tomorrow)
        today.minusDays(1) -> stringResource(R.string.home_yesterday)
        else -> selectedDate.format(DATE_FMT_LONG)
    }
    val isToday = selectedDate == today

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(R.string.home_prev_day)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (!isToday) {
                    Text(
                        selectedDate.format(DATE_FMT_LONG),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isToday) {
                androidx.compose.material3.TextButton(onClick = onJumpToday) {
                    Text(stringResource(R.string.home_jump_today))
                }
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.home_next_day)
                )
            }
        }
    }
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT_LONG: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatTime(dose: DoseLog): String =
    dose.scheduledAt.atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FMT)

private fun formatDose(med: Medication): String {
    val amount = if (med.dosageAmount % 1f == 0f) med.dosageAmount.toInt().toString()
    else med.dosageAmount.toString()
    return "$amount ${med.unit.name.lowercase()}"
}
