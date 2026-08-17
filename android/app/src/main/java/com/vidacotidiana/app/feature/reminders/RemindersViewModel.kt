package com.vidacotidiana.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.network.CompleteReminderRequest
import com.vidacotidiana.app.core.network.CreateReminderRequest
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.network.ReminderApi
import com.vidacotidiana.app.core.network.UserApi
import com.vidacotidiana.app.core.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val currentUserId: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val api: ReminderApi,
    private val userApi: UserApi,
    private val alarmScheduler: ReminderAlarmScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    // Found for real (SharingFlowTest, physical device): refresh() used to launch an
    // independent coroutine on every call. The initial refresh() from init{} — slow on a
    // cold-started app racing with login — could still be in flight when createReminder()'s
    // own refresh() finished, and its stale response then overwrote the fresher list,
    // silently dropping the just-created reminder from the UI. Cancelling any prior
    // in-flight refresh before starting a new one makes only the latest response win.
    private var refreshJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            // Share button visibility only — a failure here just means it won't
            // show, not fatal to the reminders list itself.
            runCatching { userApi.getCurrentUser() }
                .onSuccess { user -> _uiState.value = _uiState.value.copy(currentUserId = user.id) }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { api.listReminders() }
                .onSuccess { page -> _uiState.value = _uiState.value.copy(reminders = page.items, loading = false, error = null) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
        }
    }

    /**
     * AND-007: dueAtMillis, when present, is what actually gets a local alarm
     * scheduled — a freshly created reminder is always PENDING, so there's no
     * separate status check needed here (unlike a future "edit" path, which
     * doesn't exist yet — see ReminderAlarmScheduler's own comment).
     */
    fun createReminder(title: String, dueAtMillis: Long? = null) {
        if (title.isBlank()) return
        val dueAtIso = dueAtMillis?.let { DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(it)) }
        viewModelScope.launch {
            runCatching { api.createReminder(CreateReminderRequest(title, dueAtIso)) }
                .onSuccess { created ->
                    if (dueAtMillis != null) {
                        alarmScheduler.schedule(created.id, created.title, dueAtMillis)
                    }
                    refresh()
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            runCatching { api.completeReminder(reminder.id, CompleteReminderRequest(reminder.version)) }
                .onSuccess {
                    // AND-007: nothing to notify about once it's done — cancels
                    // silently (a no-op if this reminder never had a dueAt/alarm).
                    alarmScheduler.cancel(reminder.id)
                    refresh()
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }
}
