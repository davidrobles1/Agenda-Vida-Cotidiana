package com.vidacotidiana.app.feature.sharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun InvitationsScreen(onBack: () -> Unit, viewModel: InvitationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text("Invitations")
            Button(onClick = onBack) { Text("Back") }
        }

        state.error?.let { Text(it) }

        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.invitations.isEmpty()) {
            Text("No pending invitations")
        } else {
            LazyColumn {
                items(state.invitations, key = { it.id }) { invitation ->
                    Row(modifier = Modifier.testTag("invitation_row_${invitation.id}")) {
                        Text(invitation.invitedEmail ?: "")
                        Button(onClick = { viewModel.accept(invitation.id) }) { Text("Accept") }
                        Button(onClick = { viewModel.reject(invitation.id) }) { Text("Reject") }
                    }
                }
            }
        }
    }
}
