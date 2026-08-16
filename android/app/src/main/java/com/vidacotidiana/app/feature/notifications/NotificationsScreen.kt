package com.vidacotidiana.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.ui.VidaColor
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing

@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
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

        Button(
            modifier = Modifier.testTag("enable_notifications_button"),
            shape = RoundedCornerShape(VidaShape.control),
            enabled = !state.registering,
            onClick = { viewModel.enableNotifications() },
        ) {
            Text(if (state.registering) "Enabling…" else "Enable notifications")
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else if (state.devices.isEmpty()) {
            Text(
                "No devices registered yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidaColor.TextSecondaryLight,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                state.devices.forEach { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("device_row_${device.id}"),
                        shape = RoundedCornerShape(VidaShape.card),
                        colors = CardDefaults.cardColors(containerColor = VidaColor.SurfaceVariantLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            "${device.platform} — registered ${device.createdAt}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(VidaSpacing.md),
                        )
                    }
                }
            }
        }
    }
}
