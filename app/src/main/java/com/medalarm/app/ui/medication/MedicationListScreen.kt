@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.medication

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medalarm.app.R
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.ui.common.rememberMedicationPhoto
import com.medalarm.app.ui.common.resolveMedicationColor

@Composable
fun MedicationListScreen(
    onBack: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    onAddMedication: (oneTime: Boolean) -> Unit,
    viewModel: MedicationListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddTypeChooser by remember { mutableStateOf(false) }

    if (showAddTypeChooser) {
        AddMedicationTypeDialog(
            onDismiss = { showAddTypeChooser = false },
            onPick = { oneTime ->
                showAddTypeChooser = false
                onAddMedication(oneTime)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.med_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTypeChooser = true }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        if (state.active.isEmpty() && state.inactive.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.med_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
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
                items(state.active, key = { "a-${it.id}" }) { med ->
                    MedicationRow(med, onClick = { onOpenMedication(med.id) })
                }
                if (state.inactive.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.med_list_inactive_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(state.inactive, key = { "i-${it.id}" }) { med ->
                        MedicationRow(med, onClick = { onOpenMedication(med.id) }, muted = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationRow(medication: Medication, onClick: () -> Unit, muted: Boolean = false) {
    val accent = resolveMedicationColor(medication.colorHex)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (muted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val photo = rememberMedicationPhoto(medication.photoPath, displaySize = 40.dp)
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (muted) 0.5f else 1f,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    color = if (muted) accent.copy(alpha = 0.4f) else accent,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = medication.name.firstOrNull()?.uppercase().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                val amount = if (medication.dosageAmount % 1f == 0f) medication.dosageAmount.toInt().toString()
                else medication.dosageAmount.toString()
                Text(
                    "$amount ${medication.unit.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
