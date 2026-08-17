package com.vidacotidiana.app.feature.sharing

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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.StatusPill
import com.vidacotidiana.app.core.ui.components.resolve

@Composable
fun InvitationsScreen(onBack: () -> Unit, viewModel: InvitationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Invitations", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else if (state.invitations.isEmpty()) {
            Text(
                "No pending invitations.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidaTheme.colors.textSecondary,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                items(state.invitations, key = { it.id }) { invitation ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("invitation_row_${invitation.id}"),
                        shape = RoundedCornerShape(VidaShape.card),
                        colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                                val toneColors = BadgeTone.Info.resolve()
                                Box(
                                    modifier = Modifier.size(32.dp).background(toneColors.container, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = toneColors.on, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    invitation.invitedEmail ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                val pillColors = BadgeTone.Warning.resolve()
                                StatusPill(label = "Pendiente", container = pillColors.container, text = pillColors.on)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                                Button(
                                    shape = RoundedCornerShape(VidaShape.control),
                                    onClick = { viewModel.accept(invitation.id) },
                                ) { Text("Accept") }
                                OutlinedButton(
                                    shape = RoundedCornerShape(VidaShape.control),
                                    onClick = { viewModel.reject(invitation.id) },
                                ) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}
