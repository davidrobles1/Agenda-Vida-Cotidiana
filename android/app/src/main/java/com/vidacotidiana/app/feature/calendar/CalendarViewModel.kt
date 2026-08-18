package com.vidacotidiana.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.network.CompleteReminderRequest
import com.vidacotidiana.app.core.network.Invitation
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.network.ReminderApi
import com.vidacotidiana.app.core.network.SharingApi
import com.vidacotidiana.app.core.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UX-007: Calendario's data — real reminders (`GET /reminders`, filtered to
 * pending ones that actually have a `dueAt`, since a date-less reminder has
 * nowhere to sit on a month grid; it still shows on Tareas) and real pending
 * invitations (`GET /me/invitations`). Zero new backend: both calls reuse
 * `ReminderApi`/`SharingApi` exactly as `HomeViewModel`/`RemindersViewModel`
 * already do. Garantías/Mantenimiento are mock (`CalendarScreen.kt` reads
 * `MockData` directly, no ViewModel field needed for them) — `mockCompletedIds`
 * is the local-only, lost-on-recreation "completed" state the task requires
 * to be explicit about, not persisted anywhere real.
 */
data class CalendarUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val reminders: List<Reminder> = emptyList(),
    val invitations: List<Invitation> = emptyList(),
    val mockCompletedIds: Set<String> = emptySet(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reminderApi: ReminderApi,
    private val sharingApi: SharingApi,
    private val alarmScheduler: ReminderAlarmScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val remindersResult = runCatching { reminderApi.listReminders() }
            val invitationsResult = runCatching { sharingApi.listMyInvitations() }

            remindersResult
                .onSuccess { page ->
                    val pendingWithDate = page.items.filter { it.status == "PENDING" && it.dueAt != null }
                    _uiState.value = _uiState.value.copy(reminders = pendingWithDate, loading = false, error = null)
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }

            invitationsResult
                .onSuccess { page -> _uiState.value = _uiState.value.copy(invitations = page.items.filter { it.status == "PENDING" }) }
        }
    }

    /** Reuses the exact same real endpoint/side-effects RemindersScreen's "Complete" button does. */
    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            runCatching { reminderApi.completeReminder(reminder.id, CompleteReminderRequest(reminder.version)) }
                .onSuccess {
                    alarmScheduler.cancel(reminder.id)
                    refresh()
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    /** Mock-only toggle — never calls a backend, state lives only in this ViewModel and is lost when it's recreated. */
    fun toggleMockComplete(id: String) {
        val current = _uiState.value.mockCompletedIds
        _uiState.value = _uiState.value.copy(
            mockCompletedIds = if (id in current) current - id else current + id,
        )
    }
}
