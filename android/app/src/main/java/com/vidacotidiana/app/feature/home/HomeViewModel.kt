package com.vidacotidiana.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.network.ReminderApi
import com.vidacotidiana.app.core.network.SharingApi
import com.vidacotidiana.app.core.network.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * UX-006: Home's metric cards/lists use only real data — `GET /reminders`
 * (pending/overdue counts, upcoming list) and `GET /me/invitations` (shared
 * count). No Documentos/Garantías/Gastos numbers here (those modules are
 * mock — see `Documentacion/02-ux-ui/design-system.md`'s note on this
 * decision), and nothing Finanzas-related at all (CLAUDE.md).
 */
data class HomeUiState(
    val greetingName: String = "",
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val overdueCount: Int = 0,
    val sharedCount: Int = 0,
    val upcoming: List<Reminder> = emptyList(),
    val overdue: List<Reminder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reminderApi: ReminderApi,
    private val sharingApi: SharingApi,
    private val userApi: UserApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { userApi.getCurrentUser() }
                .onSuccess { user -> _uiState.value = _uiState.value.copy(greetingName = user.username) }

            val remindersResult = runCatching { reminderApi.listReminders() }
            val invitationsResult = runCatching { sharingApi.listMyInvitations() }

            remindersResult
                .onSuccess { page ->
                    val now = Instant.now()
                    val pending = page.items.filter { it.status == "PENDING" }
                    val overdue = pending.filter { r -> r.dueAt?.let { Instant.parse(it).isBefore(now) } == true }
                    val upcoming = pending
                        .filter { r -> r.dueAt != null && r !in overdue }
                        .sortedBy { it.dueAt }
                        .take(4)
                    _uiState.value = _uiState.value.copy(
                        pendingCount = pending.size,
                        completedCount = page.items.count { it.status == "COMPLETED" },
                        overdueCount = overdue.size,
                        upcoming = upcoming,
                        overdue = overdue.take(4),
                        loading = false,
                        error = null,
                    )
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }

            invitationsResult
                .onSuccess { page -> _uiState.value = _uiState.value.copy(sharedCount = page.items.count { it.status == "PENDING" }) }
        }
    }
}
