package com.vidacotidiana.app.feature.subscriptions

import androidx.lifecycle.ViewModel
import com.vidacotidiana.app.core.mock.MockData
import com.vidacotidiana.app.core.mock.MockSubscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** UX-006: mock module — see MockData.kt. No backend, no persistence, presentation only. Price is a descriptive field of each mock item, not a Finanzas dashboard. */
data class SubscriptionsUiState(val subscriptions: List<MockSubscription> = MockData.subscriptions)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()
}
