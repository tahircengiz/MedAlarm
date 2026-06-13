package com.medalarm.app.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medalarm.app.R

/**
 * First step of "add medication": ask whether this is a regular (recurring) or a
 * one-time (single-dose) medication. The choice routes to the same form in two
 * modes — one-time hides the recurring schedule / treatment-window / stock fields.
 *
 * @param onPick receives true for one-time, false for regular.
 */
@Composable
fun AddMedicationTypeDialog(
    onDismiss: () -> Unit,
    onPick: (oneTime: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_type_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.add_type_subtitle))

                OutlinedButton(
                    onClick = { onPick(false) },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Icon(Icons.Outlined.EventRepeat, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.add_type_regular),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedButton(
                    onClick = { onPick(true) },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.add_type_one_time),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
