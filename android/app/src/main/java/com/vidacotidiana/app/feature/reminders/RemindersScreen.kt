package com.vidacotidiana.app.feature.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.vidacotidiana.app.core.network.Reminder
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

    Column(modifier = Modifier.padding(16.dp)) {
        // Found for real (on-device, AND-006 verification): a Row with 4 buttons at their
        // natural width overflowed this screen's width — the same squeeze-to-invisible
        // failure mode as ReminderRow before its fillMaxWidth(0.6f) fix, just without a
        // long title to make it obvious. Splitting into two rows avoids reintroducing it
        // rather than guessing at weights for a header that keeps growing.
        Row {
            Text("Vida Cotidiana")
            Button(modifier = Modifier.testTag("invitations_button"), onClick = onNavigateToInvitations) {
                Text("Invitations")
            }
            Button(modifier = Modifier.testTag("notifications_button"), onClick = onNavigateToNotifications) {
                Text("Notifications")
            }
        }
        Row {
            // AND-006 real verification only — not a user-facing feature. Gated at
            // runtime by VidaCotidianaApplication's collection-enabled flag, not by
            // hiding these buttons, since the point is to prove the real pipeline works.
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

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier.testTag("reminder_title_input"),
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("New reminder") },
            )
            Button(
                modifier = Modifier.testTag("add_reminder_button"),
                onClick = {
                    viewModel.createReminder(title)
                    title = ""
                },
            ) { Text("Add") }
        }

        state.error?.let { Text(it) }

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(state.reminders, key = { it.id }) { reminder ->
                    Column {
                        ReminderRow(
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
private fun ReminderRow(reminder: Reminder, isOwner: Boolean, onComplete: () -> Unit, onShareToggle: () -> Unit) {
    // Tagged by title, not just "reminder_row": the reminders list keeps every
    // reminder ever created against this backend (real DB, no reset between
    // runs), so an untagged/generic row is ambiguous for testing — exactly
    // what broke LoginAndRemindersFlowTest's sibling-text matcher (4
    // "Complete" buttons all had *a* sibling matching the substring search).
    // Found for real (SharingFlowTest, physical device): without a weight on the title,
    // a long reminder title consumed the full Row width and squeezed the trailing
    // Complete/Share buttons down to zero-width nodes — present in the semantics tree
    // but unclickable (a tap at their coordinates landed on the title or Complete
    // instead). weight(1f) + ellipsis caps the title so both buttons keep their
    // natural tap target size regardless of title length.
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("reminder_row_${reminder.title}"),
    ) {
        Text(
            "${reminder.title} — ${reminder.status}",
            modifier = Modifier.fillMaxWidth(0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (reminder.status == "PENDING") {
            Button(onClick = onComplete) { Text("Complete") }
        }
        if (isOwner) {
            Button(modifier = Modifier.testTag("share_button_${reminder.title}"), onClick = onShareToggle) {
                Text("Share")
            }
        }
    }
}
