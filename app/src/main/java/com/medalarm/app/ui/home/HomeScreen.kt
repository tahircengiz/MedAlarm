@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.medalarm.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.medalarm.app.R
import com.medalarm.app.domain.model.DoseLog
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.SwipeAction
import com.medalarm.app.ui.common.rememberMedicationPhoto
import com.medalarm.app.ui.common.MedicationPhotoBox
import com.medalarm.app.ui.common.dosageText
import com.medalarm.app.ui.common.resolveMedicationColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMedication: (oneTime: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStock: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var actionDose by remember { mutableStateOf<DoseLog?>(null) }
    var showAddTypeChooser by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.dose_action_undo)

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshHealth()
        }
    }

    // Swipe gesture → dose action + undo snackbar. Mistake forgiveness matters for
    // elderly users, so every swipe can be undone with one tap.
    val onSwipeAction: (DoseLog, Medication, SwipeAction) -> Unit = { dose, med, action ->
        viewModel.performSwipeAction(dose.id, action)
        val message = when (action) {
            SwipeAction.TAKEN -> context.getString(R.string.home_swipe_taken_msg, med.name)
            SwipeAction.SNOOZE -> context.getString(R.string.home_swipe_snoozed_msg, med.name)
            SwipeAction.SKIP -> context.getString(R.string.home_swipe_skipped_msg, med.name)
            SwipeAction.NONE -> null
        }
        if (message != null) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.revert(dose.id)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    IconButton(onClick = onOpenStock) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = stringResource(R.string.home_action_stock))
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.home_action_history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.home_action_settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.medications.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddTypeChooser = true },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.home_add_first)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                EmptyState(onAdd = { showAddTypeChooser = true })
            } else {
                DayHeader(
                    selectedDate = state.selectedDate,
                    doses = state.doses,
                    onPrev = { viewModel.shiftDay(-1) },
                    onNext = { viewModel.shiftDay(1) },
                    onJumpToday = { viewModel.jumpToToday() }
                )
                DoseList(
                    medications = state.medications,
                    doses = state.doses,
                    swipeRightAction = state.swipeRightAction,
                    swipeLeftAction = state.swipeLeftAction,
                    onSwipeAction = onSwipeAction,
                    onDoseClick = { actionDose = it },
                    onOpenMedication = onOpenMedication
                )
            }
        }
    }

    // Ask one-time vs regular before opening the add form.
    if (showAddTypeChooser) {
        com.medalarm.app.ui.medication.AddMedicationTypeDialog(
            onDismiss = { showAddTypeChooser = false },
            onPick = { oneTime ->
                showAddTypeChooser = false
                onAddMedication(oneTime)
            }
        )
    }

    // Manual action sheet — tap a dose to mark it without waiting for the notification.
    actionDose?.let { dose ->
        val med = state.medications.firstOrNull { it.id == dose.medicationId }
        if (med != null) {
            DoseActionDialog(
                dose = dose,
                medication = med,
                onDismiss = { actionDose = null },
                onTaken = { viewModel.markTaken(dose.id); actionDose = null },
                onSkip = { viewModel.skip(dose.id); actionDose = null },
                onSnooze = { viewModel.snooze(dose.id); actionDose = null },
                onUndo = { viewModel.revert(dose.id); actionDose = null },
                onOpenMedication = { actionDose = null; onOpenMedication(med.id) }
            )
        }
    }
}

@Composable
private fun CriticalBanner(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Outlined.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.height(56.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_add_first), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Colorful hero header: day navigation + daily progress in one card.
 * Big arrows (48dp targets) and a clear progress line for at-a-glance status.
 */
@Composable
private fun DayHeader(
    selectedDate: LocalDate,
    doses: List<DoseLog>,
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
    val takenCount = doses.count { it.status == DoseStatus.TAKEN }
    val totalCount = doses.size

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.home_prev_day),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isToday) {
                        Text(
                            selectedDate.format(DATE_FMT_LONG),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onJumpToday) {
                            Text(stringResource(R.string.home_jump_today))
                        }
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.home_next_day),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (totalCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (takenCount == totalCount) {
                        stringResource(R.string.home_progress_done)
                    } else {
                        stringResource(R.string.home_progress, takenCount, totalCount)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (totalCount == 0) 0f else takenCount.toFloat() / totalCount },
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DoseList(
    medications: List<Medication>,
    doses: List<DoseLog>,
    swipeRightAction: SwipeAction,
    swipeLeftAction: SwipeAction,
    onSwipeAction: (DoseLog, Medication, SwipeAction) -> Unit,
    onDoseClick: (DoseLog) -> Unit,
    onOpenMedication: (Long) -> Unit
) {
    val byId = remember(medications) { medications.associateBy { it.id } }
    // The next dose still awaiting action — the single "you are here" point on the day's rail.
    val nextDueId = remember(doses) { doses.firstOrNull { it.status == DoseStatus.PENDING }?.id }

    if (doses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.home_no_doses_day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(doses, key = { it.id }) { dose ->
                val med = byId[dose.medicationId]
                if (med != null) {
                    SwipeableDoseCard(
                        dose = dose,
                        medication = med,
                        isNextDue = dose.id == nextDueId,
                        swipeRightAction = swipeRightAction,
                        swipeLeftAction = swipeLeftAction,
                        onSwipeAction = onSwipeAction,
                        onClick = { onDoseClick(dose) },
                        onLongClick = { onOpenMedication(med.id) }
                    )
                }
            }
        }
    }
}

/**
 * Wraps [DoseCard] in a swipe container. Swiping right/left triggers the user's
 * configured action; the card snaps back (the row stays, only its status changes).
 * Only PENDING / SNOOZED doses are swipeable.
 */
@Composable
private fun SwipeableDoseCard(
    dose: DoseLog,
    medication: Medication,
    isNextDue: Boolean,
    swipeRightAction: SwipeAction,
    swipeLeftAction: SwipeAction,
    onSwipeAction: (DoseLog, Medication, SwipeAction) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val actionable = dose.status == DoseStatus.PENDING || dose.status == DoseStatus.SNOOZED
    val rightEnabled = actionable && swipeRightAction != SwipeAction.NONE
    val leftEnabled = actionable && swipeLeftAction != SwipeAction.NONE

    if (!rightEnabled && !leftEnabled) {
        DoseCard(dose, medication, isNextDue, onClick, onLongClick)
        return
    }

    // key(status) resets the swipe state once an action lands, so a card that
    // re-enters an actionable state starts from a clean Settled position.
    key(dose.status, swipeRightAction, swipeLeftAction) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> onSwipeAction(dose, medication, swipeRightAction)
                    SwipeToDismissBoxValue.EndToStart -> onSwipeAction(dose, medication, swipeLeftAction)
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                false // never dismiss — the card stays and re-renders with its new status
            },
            // Deliberately high threshold (40% of width) to avoid accidental triggers
            // from shaky scrolling — important for elderly users.
            positionalThreshold = { totalDistance -> totalDistance * 0.4f }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = rightEnabled,
            enableDismissFromEndToStart = leftEnabled,
            backgroundContent = {
                val (action, alignment) = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> swipeRightAction to Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> swipeLeftAction to Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> SwipeAction.NONE to Alignment.Center
                }
                if (action != SwipeAction.NONE) {
                    SwipeActionBackground(action = action, alignment = alignment)
                }
            }
        ) {
            DoseCard(dose, medication, isNextDue, onClick, onLongClick)
        }
    }
}

@Composable
private fun SwipeActionBackground(action: SwipeAction, alignment: Alignment) {
    val (color, icon, label) = when (action) {
        SwipeAction.TAKEN -> Triple(SwipeTakenGreen, Icons.Outlined.CheckCircle, stringResource(R.string.action_taken))
        SwipeAction.SNOOZE -> Triple(SwipeSnoozeAmber, Icons.Outlined.Snooze, stringResource(R.string.action_snooze))
        SwipeAction.SKIP -> Triple(SwipeSkipGray, Icons.Outlined.RemoveCircle, stringResource(R.string.action_skip))
        SwipeAction.NONE -> return
    }
    Box(
        contentAlignment = alignment,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DoseCard(
    dose: DoseLog,
    medication: Medication,
    isNextDue: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val accent = resolveMedicationColor(medication.colorHex)

    Card(
        // tap = action dialog, long-press = open medication detail.
        // Quiet neutral surface + hairline; the next-due row gets a teal hairline,
        // so status reads from the rail dot and chip, not from a tinted background.
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (isNextDue) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signature: each dose sits on the day's quiet time rail. The next dose
            // still due is the one filled teal point — "you are here".
            DoseRail(status = dose.status, isNextDue = isNextDue)
            Spacer(Modifier.width(10.dp))

            // Box photo when available (far easier to recognize than color alone),
            // otherwise the accent-tinted medication avatar.
            val photo = rememberMedicationPhoto(medication.photoPath, displaySize = 48.dp)
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f))
                ) {
                    Icon(
                        Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = medication.dosageText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTime(dose),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isNextDue -> MaterialTheme.colorScheme.primary
                        dose.status == DoseStatus.PENDING -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (dose.status != DoseStatus.PENDING) {
                    Spacer(Modifier.height(4.dp))
                    StatusChip(dose.status)
                }
            }
        }
    }
}

/**
 * One point on the day's dose rail: a hairline vertical line with a status dot.
 * The next-due dose is the emphasized filled teal point; past doses are filled in
 * their status color, doses still ahead are quiet outline dots.
 */
@Composable
private fun DoseRail(status: DoseStatus, isNextDue: Boolean) {
    val dotColor = when {
        isNextDue -> MaterialTheme.colorScheme.primary
        status == DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
        status == DoseStatus.MISSED -> MaterialTheme.colorScheme.error
        status == DoseStatus.SNOOZED -> MaterialTheme.colorScheme.tertiary
        status == DoseStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val filled = isNextDue || status != DoseStatus.PENDING
    Box(
        modifier = Modifier
            .width(16.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.30f))
        )
        when {
            isNextDue -> Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            filled -> Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            else -> Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, dotColor, CircleShape)
            )
        }
    }
}

/** Status as a quiet tonal chip: icon + word. Text labels are far clearer than icon-only for elderly users. */
@Composable
private fun StatusChip(status: DoseStatus) {
    data class ChipStyle(val color: Color, val icon: ImageVector, val label: String)

    val style = when (status) {
        DoseStatus.TAKEN -> ChipStyle(
            MaterialTheme.colorScheme.primary,
            Icons.Outlined.CheckCircle,
            stringResource(R.string.history_status_taken)
        )
        DoseStatus.MISSED -> ChipStyle(
            MaterialTheme.colorScheme.error,
            Icons.Outlined.Warning,
            stringResource(R.string.history_status_missed)
        )
        DoseStatus.SKIPPED -> ChipStyle(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Outlined.RemoveCircle,
            stringResource(R.string.history_status_skipped)
        )
        DoseStatus.SNOOZED -> ChipStyle(
            MaterialTheme.colorScheme.tertiary,
            Icons.Outlined.HourglassEmpty,
            stringResource(R.string.history_status_snoozed)
        )
        DoseStatus.PENDING -> ChipStyle(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Outlined.Schedule,
            stringResource(R.string.history_status_pending)
        )
    }
    Surface(shape = RoundedCornerShape(8.dp), color = style.color.copy(alpha = 0.14f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(style.label, color = style.color, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DoseActionDialog(
    dose: DoseLog,
    medication: Medication,
    onDismiss: () -> Unit,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    onUndo: () -> Unit,
    onOpenMedication: () -> Unit
) {
    val actionable = dose.status == DoseStatus.PENDING || dose.status == DoseStatus.SNOOZED
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(medication.name, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                // Box photo so the user instantly recognizes WHICH medication —
                // proportional, capped height, never a full-screen takeover.
                MedicationPhotoBox(path = medication.photoPath, maxHeight = 160.dp)
                if (medication.photoPath != null) {
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    "${formatTime(dose)} · ${medication.dosageText()}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            // Big, full-width stacked buttons — large touch targets, obvious hierarchy.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (actionable) {
                    Button(
                        onClick = onTaken,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_taken), style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Outlined.Snooze, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_snooze), style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Outlined.RemoveCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_skip), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onUndo,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(stringResource(R.string.dose_action_undo), style = MaterialTheme.typography.titleMedium)
                    }
                }
                TextButton(onClick = onOpenMedication, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dose_action_open_med))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// Saturated semantic colors for swipe backgrounds — same in light & dark, white content
// keeps contrast high in both.
private val SwipeTakenGreen = Color(0xFF2E9E5B)
private val SwipeSnoozeAmber = Color(0xFFE8930C)
private val SwipeSkipGray = Color(0xFF67737E)

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT_LONG: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatTime(dose: DoseLog): String =
    dose.scheduledAt.atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FMT)
