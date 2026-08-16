package com.vidacotidiana.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.vidacotidiana.app.core.network.DeviceApi
import com.vidacotidiana.app.core.network.DevicePushToken
import com.vidacotidiana.app.core.network.RegisterDeviceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class NotificationsUiState(
    val devices: List<DevicePushToken> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val registering: Boolean = false,
)

/** AND-005: real google-services.json (Firebase project vida-cotidiana-6da30) — real FCM token, real registration. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(private val api: DeviceApi) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { api.listDevices() }
                .onSuccess { devices -> _uiState.value = _uiState.value.copy(devices = devices, loading = false) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
        }
    }

    fun enableNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(registering = true, error = null)
            runCatching { FirebaseMessaging.getInstance().token.await() }
                .mapCatching { token -> api.registerDevice(RegisterDeviceRequest(platform = "ANDROID", token = token)) }
                .onSuccess { _uiState.value = _uiState.value.copy(registering = false); refresh() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(registering = false, error = e.message) }
        }
    }
}
