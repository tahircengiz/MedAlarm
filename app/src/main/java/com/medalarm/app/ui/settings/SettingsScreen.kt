@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medalarm.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.medalarm.app.BuildConfig
import com.medalarm.app.R
import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.domain.model.UserSettings

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSystemStatus: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val s = settings ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { AppearanceSection(s, viewModel) }
            item { HorizontalDivider() }
            item { RemindersSection(s, viewModel) }
            item { HorizontalDivider() }
            item {
                SystemSection(
                    onOpenSystemStatus = onOpenSystemStatus,
                    onOpenBackup = onOpenBackup
                )
            }
            item { HorizontalDivider() }
            item { AboutSection() }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun AppearanceSection(s: UserSettings, vm: SettingsViewModel) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_appearance))

        Text(stringResource(R.string.settings_theme), modifier = Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        SegmentedRow(
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
            ),
            selected = s.themeMode,
            onSelect = vm::setTheme
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            SwitchRow(
                label = stringResource(R.string.settings_dynamic_color),
                checked = s.useDynamicColor,
                onChange = vm::setDynamicColor
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_language), modifier = Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        SegmentedRow(
            options = listOf(
                AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                AppLanguage.TR to stringResource(R.string.settings_language_tr),
                AppLanguage.EN to stringResource(R.string.settings_language_en)
            ),
            selected = s.language,
            onSelect = vm::setLanguage
        )
    }
}

@Composable
private fun RemindersSection(s: UserSettings, vm: SettingsViewModel) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_reminders))

        SwitchRow(
            label = stringResource(R.string.settings_vibration),
            checked = s.vibrationEnabled,
            onChange = vm::setVibration
        )
        SwitchRow(
            label = stringResource(R.string.settings_tts),
            checked = s.ttsEnabled,
            onChange = vm::setTts
        )

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_snooze_minutes),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        SliderRow(
            value = s.defaultSnoozeMinutes.toFloat(),
            range = 5f..60f,
            steps = 10,
            valueLabel = "${s.defaultSnoozeMinutes} min",
            onChange = { vm.setSnoozeMinutes(it.toInt()) }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_snooze_count),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        SliderRow(
            value = s.maxSnoozeCount.toFloat(),
            range = 0f..10f,
            steps = 9,
            valueLabel = if (s.maxSnoozeCount == 0) stringResource(R.string.settings_snooze_unlimited)
            else s.maxSnoozeCount.toString(),
            onChange = { vm.setMaxSnoozeCount(it.toInt()) }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_low_stock_default),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        SliderRow(
            value = s.defaultLowStockThreshold,
            range = 0f..30f,
            steps = 29,
            valueLabel = s.defaultLowStockThreshold.toInt().toString(),
            onChange = vm::setLowStockDefault
        )
    }
}

@Composable
private fun SystemSection(onOpenSystemStatus: () -> Unit, onOpenBackup: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_system))
        NavigationRow(
            label = stringResource(R.string.settings_open_system_status),
            onClick = onOpenSystemStatus
        )
        NavigationRow(
            label = stringResource(R.string.backup_open_section),
            onClick = onOpenBackup
        )
    }
}

@Composable
private fun AboutSection() {
    Column {
        SectionHeader(stringResource(R.string.settings_section_about))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.settings_about_version), style = MaterialTheme.typography.bodyLarge)
            Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.settings_about_source), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val selectedNow = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = MaterialTheme.shapes.medium,
                color = if (selectedNow) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedNow) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
private fun NavigationRow(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
