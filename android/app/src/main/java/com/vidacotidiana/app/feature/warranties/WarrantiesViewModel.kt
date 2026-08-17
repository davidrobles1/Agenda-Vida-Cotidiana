package com.vidacotidiana.app.feature.warranties

import androidx.lifecycle.ViewModel
import com.vidacotidiana.app.core.mock.MockData
import com.vidacotidiana.app.core.mock.MockWarranty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** UX-006: mock module — see MockData.kt. No backend, no persistence, presentation only. */
data class WarrantiesUiState(val warranties: List<MockWarranty> = MockData.warranties)

@HiltViewModel
class WarrantiesViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(WarrantiesUiState())
    val uiState: StateFlow<WarrantiesUiState> = _uiState.asStateFlow()
}
