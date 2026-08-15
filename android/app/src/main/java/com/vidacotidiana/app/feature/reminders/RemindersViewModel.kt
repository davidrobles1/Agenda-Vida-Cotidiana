package com.vidacotidiana.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.network.CompleteReminderRequest
import com.vidacotidiana.app.core.network.CreateReminderRequest
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.network.ReminderApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(private val api: ReminderApi) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { api.listReminders() }
                .onSuccess { page -> _uiState.value = RemindersUiState(reminders = page.items, loading = false) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
        }
    }

    fun createReminder(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching { api.createReminder(CreateReminderRequest(title)) }
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            runCatching { api.completeReminder(reminder.id, CompleteReminderRequest(reminder.version)) }
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }
}
