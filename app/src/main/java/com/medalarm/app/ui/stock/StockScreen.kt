@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.stock

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medalarm.app.R
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.ui.common.rememberMedicationPhoto
import com.medalarm.app.ui.common.resolveMedicationColor

/**
 * Stock overview reached from the Home top bar: every active medication with its
 * remaining amount, low-stock ones flagged and listed first, with a one-tap
 * "add stock" refill dialog per row.
 */
@Composable
fun StockScreen(
    onBack: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var refillFor by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stock_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (!state.isLoading && state.tracked.isEmpty() && state.untracked.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.stock_screen_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.tracked, key = { "t-${it.id}" }) { med ->
                    StockRow(
                        medication = med,
                        onClick = { onOpenMedication(med.id) },
                        onRefill = { refillFor = med }
                    )
                }

                if (state.untracked.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.stock_screen_untracked_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.stock_screen_untracked_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.untracked, key = { "u-${it.id}" }) { med ->
                        StockRow(
                            medication = med,
                            onClick = { onOpenMedication(med.id) },
                            onRefill = null
                        )
                    }
                }
            }
        }
    }

    refillFor?.let { med ->
        RefillDialog(
            medication = med,
            onDismiss = { refillFor = null },
            onConfirm = { amount ->
                viewModel.addStock(med.id, amount)
                refillFor = null
            }
        )
    }
}

@Composable
private fun StockRow(
    medication: Medication,
    onClick: () -> Unit,
    onRefill: (() -> Unit)?
) {
    val accent = resolveMedicationColor(medication.colorHex)
    val low = medication.isLowStock()

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (low) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val photo = rememberMedicationPhoto(medication.photoPath, displaySize = 44.dp)
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(color = accent, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                        Text(
                            text = medication.name.firstOrNull()?.uppercase().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (low) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (low) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = stockLabel(medication),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (low) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onRefill != null) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onRefill) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.med_add_stock))
                }
            }
        }
    }
}

@Composable
private fun stockLabel(med: Medication): String {
    val amount = med.stockAmount
        ?: return stringResource(R.string.stock_screen_not_tracked)
    val unitName = med.unit.name.lowercase()
    val remaining = stringResource(
        R.string.stock_screen_remaining, "${amount.toCleanString()} $unitName"
    )
    val threshold = med.stockThreshold ?: return remaining
    return "$remaining (≤ ${threshold.toCleanString()} ⚠️)"
}

@Composable
private fun RefillDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var amountRaw by remember { mutableStateOf("") }
    val amount = amountRaw.toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stock_screen_refill_title, medication.name)) },
        text = {
            OutlinedTextField(
                value = amountRaw,
                onValueChange = { v -> amountRaw = v.filter { it.isDigit() || it == '.' } },
                label = { Text(stringResource(R.string.med_add_stock_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > 0f,
                onClick = { amount?.let(onConfirm) }
            ) {
                Text(stringResource(R.string.med_add_stock_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun Float.toCleanString(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()
