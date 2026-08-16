package com.vidacotidiana.app.feature.sharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.network.CreateInvitationRequest
import com.vidacotidiana.app.core.network.Invitation
import com.vidacotidiana.app.core.network.ReminderShare
import com.vidacotidiana.app.core.network.SharesAndInvitationsResponse
import com.vidacotidiana.app.core.network.sharingApi
import kotlinx.coroutines.launch

/** AND-004: inline share panel shown from a reminder's own row (owner only). */
@Composable
fun ShareDialog(reminderId: String) {
    val context = LocalContext.current
    val api = remember(context) { sharingApi(context) }
    val scope = rememberCoroutineScope()

    var shares by remember { mutableStateOf<List<ReminderShare>>(emptyList()) }
    var invitations by remember { mutableStateOf<List<Invitation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recipient by remember { mutableStateOf("") }

    suspend fun refresh() {
        runCatching { api.listSharesAndInvitations(reminderId) }
            .onSuccess { result: SharesAndInvitationsResponse ->
                shares = result.shares
                invitations = result.invitations
                error = null
            }
            .onFailure { e -> error = e.message }
        loading = false
    }

    LaunchedEffect(reminderId) { refresh() }

    Column(modifier = Modifier.padding(8.dp).testTag("share_dialog_$reminderId")) {
        Row {
            OutlinedTextField(
                modifier = Modifier.testTag("invite_recipient_input"),
                value = recipient,
                onValueChange = { recipient = it },
                placeholder = { Text("Email or username") },
            )
            Button(
                modifier = Modifier.testTag("invite_button"),
                onClick = {
                    val value = recipient.trim()
                    if (value.isBlank()) return@Button
                    val request = if (value.contains("@")) {
                        CreateInvitationRequest(email = value)
                    } else {
                        CreateInvitationRequest(username = value)
                    }
                    scope.launch {
                        runCatching { api.createInvitation(reminderId, request) }
                            .onSuccess { recipient = ""; refresh() }
                            .onFailure { e -> error = e.message }
                    }
                },
            ) { Text("Invite") }
        }

        error?.let { Text(it) }

        if (loading) {
            CircularProgressIndicator()
        } else {
            shares.forEach { share ->
                Row {
                    Text("${share.collaboratorUserId} — ${share.status}")
                    if (share.status == "ACTIVE") {
                        Button(onClick = {
                            scope.launch {
                                runCatching { api.revokeShare(reminderId, share.id) }
                                    .onSuccess { refresh() }
                                    .onFailure { e -> error = e.message }
                            }
                        }) { Text("Revoke") }
                    }
                }
            }
            invitations.filter { it.status == "PENDING" }.forEach { invitation ->
                Row {
                    Text("${invitation.invitedEmail} — ${invitation.status}")
                    Button(onClick = {
                        scope.launch {
                            runCatching { api.cancelInvitation(invitation.id) }
                                .onSuccess { refresh() }
                                .onFailure { e -> error = e.message }
                        }
                    }) { Text("Cancel") }
                }
            }
        }
    }
}
