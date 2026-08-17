package com.vidacotidiana.app.feature.sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.network.CreateInvitationRequest
import com.vidacotidiana.app.core.network.Invitation
import com.vidacotidiana.app.core.network.ReminderShare
import com.vidacotidiana.app.core.network.SharesAndInvitationsResponse
import com.vidacotidiana.app.core.network.sharingApi
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.StatusPill
import com.vidacotidiana.app.core.ui.components.resolve
import kotlinx.coroutines.launch

/** AND-004/UX-001: inline share panel shown from a reminder's own card (owner only). */
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

    val pendingInvitations = invitations.filter { it.status == "PENDING" }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("share_dialog_$reminderId"),
        shape = RoundedCornerShape(VidaShape.card),
        colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
            Text("Share this reminder", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.testTag("invite_recipient_input").weight(1f),
                    value = recipient,
                    onValueChange = { recipient = it },
                    placeholder = { Text("Email or username") },
                    shape = RoundedCornerShape(VidaShape.control),
                    singleLine = true,
                )
                Button(
                    modifier = Modifier.testTag("invite_button"),
                    shape = RoundedCornerShape(VidaShape.control),
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

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.xs)) {
                    Text("Collaborators", style = MaterialTheme.typography.labelMedium, color = VidaTheme.colors.textSecondary)
                    if (shares.isEmpty()) {
                        Text("No collaborators yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                    shares.forEach { share ->
                        PersonRow(
                            label = share.collaboratorUserId,
                            status = share.status,
                        ) {
                            if (share.status == "ACTIVE") {
                                OutlinedButton(
                                    shape = RoundedCornerShape(VidaShape.control),
                                    onClick = {
                                        scope.launch {
                                            runCatching { api.revokeShare(reminderId, share.id) }
                                                .onSuccess { refresh() }
                                                .onFailure { e -> error = e.message }
                                        }
                                    },
                                ) { Text("Revoke") }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.xs)) {
                    Text("Pending invitations", style = MaterialTheme.typography.labelMedium, color = VidaTheme.colors.textSecondary)
                    if (pendingInvitations.isEmpty()) {
                        Text("No pending invitations.", style = MaterialTheme.typography.bodyMedium)
                    }
                    pendingInvitations.forEach { invitation ->
                        PersonRow(
                            label = invitation.invitedEmail ?: "",
                            status = invitation.status,
                        ) {
                            OutlinedButton(
                                shape = RoundedCornerShape(VidaShape.control),
                                onClick = {
                                    scope.launch {
                                        runCatching { api.cancelInvitation(invitation.id) }
                                            .onSuccess { refresh() }
                                            .onFailure { e -> error = e.message }
                                    }
                                },
                            ) { Text("Cancel") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * UX-006: shares/invitations rows in the same "icon + label + status pill (+
 * trailing action)" shape as the rest of the app's list rows. The label Text
 * stays its own node (not concatenated with status) so tests scoping by
 * exact/substring text — e.g. SharingFlowTest's "userb@example.com" — keep
 * matching without depending on the pill's wording.
 */
@Composable
private fun PersonRow(label: String, status: String, trailing: @Composable () -> Unit) {
    val tone = statusTone(status)
    val toneColors = tone.resolve()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(toneColors.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = toneColors.on, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        StatusPill(label = statusLabel(status), container = toneColors.container, text = toneColors.on)
        trailing()
    }
}

/** design-system.md §5 status-pill mapping, reused for share/invitation states. */
private fun statusTone(status: String): BadgeTone = when (status) {
    "ACTIVE", "ACCEPTED" -> BadgeTone.Success
    "PENDING" -> BadgeTone.Warning
    "REVOKED", "REJECTED", "EXPIRED", "CANCELLED" -> BadgeTone.Error
    else -> BadgeTone.Primary
}

private fun statusLabel(status: String): String = when (status) {
    "ACTIVE" -> "Active"
    "PENDING" -> "Pending"
    "ACCEPTED" -> "Accepted"
    "REVOKED" -> "Revoked"
    "REJECTED" -> "Rejected"
    "EXPIRED" -> "Expired"
    "CANCELLED" -> "Cancelled"
    else -> status
}
