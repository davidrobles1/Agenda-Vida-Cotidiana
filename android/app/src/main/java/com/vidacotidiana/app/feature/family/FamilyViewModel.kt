package com.vidacotidiana.app.feature.family

import androidx.lifecycle.ViewModel
import com.vidacotidiana.app.core.mock.MockData
import com.vidacotidiana.app.core.mock.MockFamilyMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** UX-006: mock module — see MockData.kt. No backend, no persistence, presentation only. Not the same thing as sharing/collaborators (that's real, ADR-006) — this is a purely descriptive household member list. */
data class FamilyUiState(val members: List<MockFamilyMember> = MockData.familyMembers)

@HiltViewModel
class FamilyViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()
}
