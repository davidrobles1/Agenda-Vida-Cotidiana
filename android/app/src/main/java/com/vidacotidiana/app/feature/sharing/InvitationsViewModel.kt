package com.vidacotidiana.app.feature.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.network.Invitation
import com.vidacotidiana.app.core.network.SharingApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvitationsUiState(
    val invitations: List<Invitation> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class InvitationsViewModel @Inject constructor(private val api: SharingApi) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationsUiState())
    val uiState: StateFlow<InvitationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { api.listMyInvitations() }
                .onSuccess { page -> _uiState.value = InvitationsUiState(invitations = page.items, loading = false) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
        }
    }

    fun accept(invitationId: String) {
        viewModelScope.launch {
            runCatching { api.acceptInvitation(invitationId) }
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    fun reject(invitationId: String) {
        viewModelScope.launch {
            runCatching { api.rejectInvitation(invitationId) }
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }
}
