package com.vidacotidiana.app.feature.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text("Notifications")
            Button(onClick = onBack) { Text("Back") }
        }

        state.error?.let { Text(it) }

        Button(
            modifier = Modifier.testTag("enable_notifications_button"),
            enabled = !state.registering,
            onClick = { viewModel.enableNotifications() },
        ) {
            Text(if (state.registering) "Enabling…" else "Enable notifications")
        }

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            state.devices.forEach { device ->
                Text("${device.platform} — registered ${device.createdAt}", modifier = Modifier.testTag("device_row_${device.id}"))
            }
        }
    }
}
