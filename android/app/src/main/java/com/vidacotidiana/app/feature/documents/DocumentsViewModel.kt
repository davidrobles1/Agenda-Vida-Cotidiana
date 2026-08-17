package com.vidacotidiana.app.feature.documents

import androidx.lifecycle.ViewModel
import com.vidacotidiana.app.core.mock.MockData
import com.vidacotidiana.app.core.mock.MockDocument
import com.vidacotidiana.app.core.mock.MockDocumentCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** UX-006: mock module — see MockData.kt. No backend, no persistence, presentation only. */
data class DocumentsUiState(
    val categories: List<MockDocumentCategory> = MockData.documentCategories,
    val documents: List<MockDocument> = MockData.documents,
)

@HiltViewModel
class DocumentsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()
}
