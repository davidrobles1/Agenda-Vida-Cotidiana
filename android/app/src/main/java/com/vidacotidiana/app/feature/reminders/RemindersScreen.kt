package com.vidacotidiana.app.feature.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.vidacotidiana.app.core.network.Reminder

@Composable
fun RemindersScreen(viewModel: RemindersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Vida Cotidiana")

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("New reminder") })
            Button(onClick = {
                viewModel.createReminder(title)
                title = ""
            }) { Text("Add") }
        }

        state.error?.let { Text(it) }

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(state.reminders, key = { it.id }) { reminder ->
                    ReminderRow(reminder = reminder, onComplete = { viewModel.completeReminder(reminder) })
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onComplete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("${reminder.title} — ${reminder.status}")
        if (reminder.status == "PENDING") {
            Button(onClick = onComplete) { Text("Complete") }
        }
    }
}
