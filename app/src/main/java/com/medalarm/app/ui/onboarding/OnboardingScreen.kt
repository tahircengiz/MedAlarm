package com.medalarm.app.ui.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.permission.CheckStatus
import com.medalarm.app.permission.OemAutostartHelper

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val healthReport by viewModel.healthReport.collectAsState()
    // rememberSaveable so the step survives the Activity recreation that
    // setApplicationLocales triggers when the user picks a language (step 0).
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 5

    // Refresh health every time the user returns to the foreground (e.g. after
    // visiting a system settings screen).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshHealth()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            StepIndicator(current = step + 1, total = totalSteps)
            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                // Order: language FIRST so the disclaimer + the rest render in the
                // chosen language. Picking a language recreates the Activity (locale
                // change); rememberSaveable keeps us on this step instead of resetting.
                when (step) {
                    0 -> LanguageStep(
                        current = settings?.language ?: AppLanguage.SYSTEM,
                        onSelect = viewModel::setLanguage
                    )
                    1 -> WelcomeStep()
                    2 -> DisclaimerStep(
                        accepted = settings?.disclaimerAccepted == true,
                        onAcceptChange = { if (it) viewModel.acceptDisclaimer() }
                    )
                    3 -> ThemeStep(
                        currentTheme = settings?.themeMode ?: ThemeMode.SYSTEM,
                        useDynamicColor = settings?.useDynamicColor ?: true,
                        onThemeSelect = viewModel::setTheme,
                        onDynamicColorChange = viewModel::setDynamicColor
                    )
                    4 -> PermissionsStep(
                        healthReport = healthReport,
                        isOemAggressive = viewModel.isOemAggressive,
                        onOemConfirmed = {
                            viewModel.confirmOemAutostart()
                            viewModel.refreshHealth()
                        },
                        onAnyChange = viewModel::refreshHealth
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            NavRow(
                step = step,
                totalSteps = totalSteps,
                canAdvance = canAdvance(step, settings?.disclaimerAccepted == true),
                onBack = { step = (step - 1).coerceAtLeast(0) },
                onNext = { step += 1 },
                onDone = { viewModel.completeOnboarding(onCompleted) }
            )
        }
    }
}

private fun canAdvance(step: Int, disclaimerAccepted: Boolean): Boolean = when (step) {
    2 -> disclaimerAccepted   // disclaimer (now step 2) must be accepted to advance
    else -> true
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Column {
        Text(
            text = stringResource(R.string.onboarding_step_indicator, current, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NavRow(
    step: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step > 0) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.onboarding_back))
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        if (step == totalSteps - 1) {
            Button(onClick = onDone, enabled = canAdvance) {
                Text(stringResource(R.string.onboarding_done))
            }
        } else {
            Button(onClick = onNext, enabled = canAdvance) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisclaimerStep(accepted: Boolean, onAcceptChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.disclaimer_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Pinned TR + EN versions, regardless of the user's current locale.
            // The user picks their language in the next step; here we want them
            // to be able to read the disclaimer in whichever they understand.
            Text(
                "Türkçe",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.disclaimer_full_body_tr),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "English",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.disclaimer_full_body_en),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = accepted, onCheckedChange = onAcceptChange)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.disclaimer_accept),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LanguageStep(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))
        RadioRow(
            label = stringResource(R.string.settings_language_system),
            selected = current == AppLanguage.SYSTEM,
            onClick = { onSelect(AppLanguage.SYSTEM) }
        )
        RadioRow(
            label = stringResource(R.string.settings_language_tr),
            selected = current == AppLanguage.TR,
            onClick = { onSelect(AppLanguage.TR) }
        )
        RadioRow(
            label = stringResource(R.string.settings_language_en),
            selected = current == AppLanguage.EN,
            onClick = { onSelect(AppLanguage.EN) }
        )
    }
}

@Composable
private fun ThemeStep(
    currentTheme: ThemeMode,
    useDynamicColor: Boolean,
    onThemeSelect: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.onboarding_theme_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))
        RadioRow(
            label = stringResource(R.string.settings_theme_system),
            selected = currentTheme == ThemeMode.SYSTEM,
            onClick = { onThemeSelect(ThemeMode.SYSTEM) }
        )
        RadioRow(
            label = stringResource(R.string.settings_theme_light),
            selected = currentTheme == ThemeMode.LIGHT,
            onClick = { onThemeSelect(ThemeMode.LIGHT) }
        )
        RadioRow(
            label = stringResource(R.string.settings_theme_dark),
            selected = currentTheme == ThemeMode.DARK,
            onClick = { onThemeSelect(ThemeMode.DARK) }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = useDynamicColor, onCheckedChange = onDynamicColorChange)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_dynamic_color))
            }
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionsStep(
    healthReport: com.medalarm.app.permission.SystemHealthReport?,
    isOemAggressive: Boolean,
    onOemConfirmed: () -> Unit,
    onAnyChange: () -> Unit
) {
    val context = LocalContext.current
    val oemHelper = remember { OemAutostartHelper() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onAnyChange() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        PermissionCard(
            title = stringResource(R.string.permission_notification_title),
            rationale = stringResource(R.string.permission_notification_rationale),
            status = healthReport?.notificationPermission ?: CheckStatus.UNKNOWN,
            actionLabel = stringResource(R.string.permission_grant),
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Pre-Android-13: route to app notification settings.
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }
            }
        )

        PermissionCard(
            title = stringResource(R.string.permission_exact_alarm_title),
            rationale = stringResource(R.string.permission_exact_alarm_rationale),
            status = healthReport?.exactAlarmPermission ?: CheckStatus.UNKNOWN,
            actionLabel = stringResource(R.string.permission_open_settings),
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

        PermissionCard(
            title = stringResource(R.string.permission_battery_title),
            rationale = stringResource(R.string.permission_battery_rationale),
            status = healthReport?.batteryOptimization ?: CheckStatus.UNKNOWN,
            actionLabel = stringResource(R.string.permission_open_settings),
            onAction = {
                runCatching {
                    @Suppress("BatteryLife")
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )

        if (isOemAggressive) {
            PermissionCard(
                title = stringResource(R.string.permission_oem_title),
                rationale = stringResource(R.string.permission_oem_rationale),
                status = healthReport?.oemAutostart ?: CheckStatus.UNKNOWN,
                actionLabel = stringResource(R.string.permission_open_settings),
                onAction = {
                    val intent = oemHelper.autostartIntent(context)
                    if (intent != null) {
                        runCatching { context.startActivity(intent) }
                    }
                },
                secondaryAction = stringResource(R.string.permission_oem_confirm) to onOemConfirmed
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    rationale: String,
    status: CheckStatus,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryAction: Pair<String, () -> Unit>? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (status != CheckStatus.OK) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAction) { Text(actionLabel) }
                    secondaryAction?.let { (label, callback) ->
                        TextButton(onClick = callback) { Text(label) }
                    }
                }
            }
        }
    }
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
