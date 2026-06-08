@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.medalarm.app.R
import com.medalarm.app.permission.CheckStatus
import com.medalarm.app.permission.Oem
import com.medalarm.app.permission.OemAutostartHelper
import com.medalarm.app.permission.SystemHealthReport

@Composable
fun SystemStatusScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val report by viewModel.healthReport.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshHealth()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_system_status)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshHealth() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.status_refresh))
                    }
                }
            )
        }
    ) { padding ->
        val context = LocalContext.current
        val oemHelper = remember { OemAutostartHelper() }
        val notificationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ -> viewModel.refreshHealth() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StatusCard(
                    title = stringResource(R.string.status_notifications_permission),
                    status = report?.notificationPermission ?: CheckStatus.UNKNOWN,
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        }
                    }
                )
            }
            item {
                StatusCard(
                    title = stringResource(R.string.status_exact_alarm_permission),
                    status = report?.exactAlarmPermission ?: CheckStatus.UNKNOWN,
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                )
            }
            item {
                StatusCard(
                    title = stringResource(R.string.status_notifications_enabled),
                    status = report?.notificationsEnabled ?: CheckStatus.UNKNOWN,
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    }
                )
            }
            item {
                StatusCard(
                    title = stringResource(R.string.status_battery_optimization),
                    status = report?.batteryOptimization ?: CheckStatus.UNKNOWN,
                    onAction = {
                        @Suppress("BatteryLife")
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
            if (oemHelper.detect() == Oem.SAMSUNG) {
                item { InfoNote(
                    title = stringResource(R.string.status_samsung_note_title),
                    body = stringResource(R.string.status_samsung_note_body)
                ) }
            }
            if (viewModel.isOemAggressive) {
                item {
                    StatusCard(
                        title = stringResource(R.string.status_oem_autostart),
                        status = report?.oemAutostart ?: CheckStatus.UNKNOWN,
                        onAction = {
                            oemHelper.autostartIntent(context)?.let {
                                runCatching { context.startActivity(it) }
                            }
                        },
                        secondaryAction = stringResource(R.string.permission_oem_confirm) to {
                            viewModel.confirmOemAutostart()
                            viewModel.refreshHealth()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoNote(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    status: CheckStatus,
    onAction: () -> Unit,
    secondaryAction: Pair<String, () -> Unit>? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(status)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    statusLabel(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (status != CheckStatus.OK) {
                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(onClick = onAction) {
                        Text(stringResource(R.string.status_fix))
                    }
                    secondaryAction?.let { (label, callback) ->
                        TextButton(onClick = callback) { Text(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: CheckStatus): String = when (status) {
    CheckStatus.OK -> stringResource(R.string.status_ok)
    CheckStatus.NEEDS_ATTENTION -> stringResource(R.string.status_attention)
    CheckStatus.BLOCKED -> stringResource(R.string.status_blocked)
    CheckStatus.UNKNOWN -> stringResource(R.string.status_unknown)
}

@Composable
private fun StatusBadge(status: CheckStatus) {
    val (color, icon) = when (status) {
        CheckStatus.OK -> MaterialTheme.colorScheme.primary to Icons.Outlined.CheckCircle
        CheckStatus.NEEDS_ATTENTION -> MaterialTheme.colorScheme.tertiary to Icons.Outlined.Warning
        CheckStatus.BLOCKED -> MaterialTheme.colorScheme.error to Icons.Outlined.Warning
        CheckStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Outlined.Info
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}
