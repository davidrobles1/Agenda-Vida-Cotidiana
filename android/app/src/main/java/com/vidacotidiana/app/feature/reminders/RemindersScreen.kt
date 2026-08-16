package com.vidacotidiana.app.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.ui.VidaColor
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.feature.sharing.ShareDialog

@Composable
fun RemindersScreen(
    onNavigateToInvitations: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var sharingReminderId by remember { mutableStateOf<String?>(null) }

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
                    viewModel.createReminder(title)
                    title = ""
                },
            ) { Text("Add") }
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
            color = VidaColor.TextSecondaryLight,
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
        colors = CardDefaults.cardColors(containerColor = VidaColor.SurfaceVariantLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = reminder.status)
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
        "COMPLETED" -> Triple(VidaColor.SuccessContainerLight, VidaColor.SuccessTextLight, "Completed")
        "PENDING" -> Triple(VidaColor.WarningContainerLight, VidaColor.WarningTextLight, "Pending")
        else -> Triple(VidaColor.SurfaceVariantLight, VidaColor.TextSecondaryLight, status)
    }
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(percent = 50))
            .padding(horizontal = VidaSpacing.sm, vertical = VidaSpacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = text)
    }
}
