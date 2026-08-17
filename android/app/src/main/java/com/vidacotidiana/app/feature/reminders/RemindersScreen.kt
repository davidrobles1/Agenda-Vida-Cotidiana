package com.vidacotidiana.app.feature.reminders

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.resolve
import com.vidacotidiana.app.feature.sharing.ShareDialog
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onNavigateToInvitations: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var sharingReminderId by remember { mutableStateOf<String?>(null) }
    var dueAtMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillisUtc by remember { mutableStateOf<Long?>(null) }

    // Real bug found verifying AND-007 on-device (dumpsys notification showed
    // "com.vidacotidiana.app importance=NONE" — the whole app blocked at the OS
    // level, not just this channel): POST_NOTIFICATIONS has been declared in
    // AndroidManifest.xml since AND-005, but nothing in the app ever actually
    // requested it at runtime — on API 33+ that means it silently stays denied
    // forever, so push (AND-005) has almost certainly had this exact same gap
    // all along, not just local reminders. Fixed here since this is the screen
    // that needs it next; AND-005's own request path is a separate, real
    // pending fix noted in the backlog rather than silently bundled in here.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Denied just means no local notification shows — same fail-closed behavior as the exact-alarm permission below. */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            Text("Vida Cotidiana", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(modifier = Modifier.testTag("invitations_button"), onClick = onNavigateToInvitations) {
                Text("Invitations")
            }
            TextButton(modifier = Modifier.testTag("notifications_button"), onClick = onNavigateToNotifications) {
                Text("Notifications")
            }
        }
        // UX-001 real verification only, not user-facing — see AND-006. Found for real
        // (on-device): a Row with 4 buttons at their natural width overflowed the screen
        // and silently dropped the debug buttons from the UI tree; kept on their own row.
        Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            Button(
                modifier = Modifier.testTag("debug_record_exception_button"),
                onClick = {
                    FirebaseCrashlytics.getInstance()
                        .recordException(RuntimeException("AND-006 debug non-fatal: manually triggered from RemindersScreen"))
                },
            ) { Text("Debug: record error") }
            Button(
                modifier = Modifier.testTag("debug_crash_button"),
                onClick = { throw RuntimeException("AND-006 debug crash: manually triggered from RemindersScreen") },
            ) { Text("Debug: crash") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier.testTag("reminder_title_input").weight(1f),
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("New reminder") },
                shape = RoundedCornerShape(VidaShape.control),
                singleLine = true,
            )
            Button(
                modifier = Modifier.testTag("add_reminder_button"),
                shape = RoundedCornerShape(VidaShape.control),
                onClick = {
                    viewModel.createReminder(title, dueAtMillis)
                    title = ""
                    dueAtMillis = null
                },
            ) { Text("Add") }
        }

        // AND-007: optional due date/time — only reminders with one get a local
        // alarm (see ReminderAlarmScheduler). Requesting exact-alarm permission
        // happens here, the moment the user opts into a due date, rather than
        // silently at creation time — the task explicitly says not to assume
        // it's already granted.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            if (dueAtMillis == null) {
                TextButton(
                    modifier = Modifier.testTag("add_due_date_button"),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        requestExactAlarmPermissionIfNeeded(context)
                        showDatePicker = true
                    },
                ) { Text("Add due date") }
            } else {
                Text(
                    "Due: ${formatDueAt(dueAtMillis!!)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("due_date_summary").weight(1f),
                )
                TextButton(
                    modifier = Modifier.testTag("clear_due_date_button"),
                    onClick = { dueAtMillis = null },
                ) { Text("Clear") }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickedDateMillisUtc = datePickerState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    }) { Text("Next") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
            ) { DatePicker(state = datePickerState) }
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState()
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val dateMillisUtc = pickedDateMillisUtc
                        if (dateMillisUtc != null) {
                            val pickedDate = Instant.ofEpochMilli(dateMillisUtc).atZone(ZoneOffset.UTC).toLocalDate()
                            val pickedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            dueAtMillis = pickedDate.atTime(pickedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
                text = { TimePicker(state = timePickerState) },
            )
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        when {
            state.loading -> LoadingState()
            state.reminders.isEmpty() -> EmptyState()
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                items(state.reminders, key = { it.id }) { reminder ->
                    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                        ReminderCard(
                            reminder = reminder,
                            isOwner = reminder.ownerUserId == state.currentUserId,
                            onComplete = { viewModel.completeReminder(reminder) },
                            onShareToggle = {
                                sharingReminderId = if (sharingReminderId == reminder.id) null else reminder.id
                            },
                        )
                        if (sharingReminderId == reminder.id) {
                            ShareDialog(reminderId = reminder.id)
                        }
                    }
                }
            }
        }
    }
}

/**
 * AND-007. Pre-S, exact alarms need no special permission — nothing to do.
 * On S+, ReminderAlarmScheduler.schedule() already fails closed if this isn't
 * granted, but proactively prompting here (instead of only failing silently
 * later) is what the task asked for: "no asumas que ya está concedido".
 */
private fun requestExactAlarmPermissionIfNeeded(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    if (!alarmManager.canScheduleExactAlarms()) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
        )
    }
}

private fun formatDueAt(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    return formatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis))
}

/** Reminder.dueAt (from the backend) is an ISO-8601 UTC instant string. */
private fun formatDueAtIso(iso: String): String = formatDueAt(Instant.parse(iso).toEpochMilli())

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().padding(VidaSpacing.xxl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = VidaSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.xs),
    ) {
        Text("No reminders yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add one above to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = VidaTheme.colors.textSecondary,
        )
    }
}

/**
 * UX-001: reminder card — the highest-priority visual pass per the design-system.md
 * rollout order. Title/status on their own row (full width, never squeezed), actions
 * on a second row — the same failure mode as the earlier zero-width button bug
 * (AND-004/AND-006) is structurally avoided here by never sharing a row between
 * a variable-length title and a fixed-size action.
 */
@Composable
private fun ReminderCard(reminder: Reminder, isOwner: Boolean, onComplete: () -> Unit, onShareToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("reminder_row_${reminder.title}"),
        shape = RoundedCornerShape(VidaShape.card),
        colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                // UX-006: reference's list-item icon pattern (colored badge next to the
                // title) — purely additive, the title Text/StatusBadge that existing
                // tests scope by testTag/hasText are untouched.
                val tone = if (reminder.status == "COMPLETED") BadgeTone.Success else BadgeTone.Primary
                val toneColors = tone.resolve()
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(toneColors.container, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (reminder.status == "COMPLETED") Icons.Filled.CheckCircle else Icons.Filled.Description,
                        contentDescription = null,
                        tint = toneColors.on,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = reminder.status)
            }

            reminder.dueAt?.let { dueAt ->
                Text(
                    "Due: ${formatDueAtIso(dueAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VidaTheme.colors.textSecondary,
                )
            }

            if (reminder.status == "PENDING" || isOwner) {
                Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                    if (reminder.status == "PENDING") {
                        Button(
                            onClick = onComplete,
                            shape = RoundedCornerShape(VidaShape.control),
                        ) { Text("Complete") }
                    }
                    if (isOwner) {
                        OutlinedButton(
                            modifier = Modifier.testTag("share_button_${reminder.title}"),
                            onClick = onShareToggle,
                            shape = RoundedCornerShape(VidaShape.control),
                        ) { Text("Share") }
                    }
                }
            }
        }
    }
}

/** design-system.md §5 — state communicated with text AND color, never color alone. */
@Composable
private fun StatusBadge(status: String) {
    val (container, text, label) = when (status) {
        "COMPLETED" -> Triple(VidaTheme.colors.successContainer, VidaTheme.colors.successText, "Completed")
        "PENDING" -> Triple(VidaTheme.colors.warningContainer, VidaTheme.colors.warningText, "Pending")
        else -> Triple(VidaTheme.colors.surfaceVariant, VidaTheme.colors.textSecondary, status)
    }
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(percent = 50))
            .padding(horizontal = VidaSpacing.sm, vertical = VidaSpacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = text)
    }
}
