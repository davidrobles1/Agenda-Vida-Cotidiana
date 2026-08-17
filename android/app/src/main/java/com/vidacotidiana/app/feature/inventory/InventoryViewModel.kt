package com.vidacotidiana.app.feature.inventory

import androidx.lifecycle.ViewModel
import com.vidacotidiana.app.core.mock.MockData
import com.vidacotidiana.app.core.mock.MockInventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** UX-006: mock module — see MockData.kt. No backend, no persistence, presentation only. */
data class InventoryUiState(
    val categories: List<String> = MockData.inventoryCategories,
    val selectedCategory: String = "Todos",
    val allItems: List<MockInventoryItem> = MockData.inventoryItems,
) {
    val visibleItems: List<MockInventoryItem>
        get() = if (selectedCategory == "Todos") allItems else allItems.filter { it.category == selectedCategory }
}

@HiltViewModel
class InventoryViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
