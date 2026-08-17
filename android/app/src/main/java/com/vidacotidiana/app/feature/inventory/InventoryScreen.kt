package com.vidacotidiana.app.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.ListItemRow
import com.vidacotidiana.app.core.ui.components.VidaFilterChip

/** UX-006: mock module (scaffolding only, MockData.kt) — matches the reference's Inventario widget. */
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Inventario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            items(state.categories) { category ->
                VidaFilterChip(
                    label = category,
                    selected = category == state.selectedCategory,
                    onClick = { viewModel.selectCategory(category) },
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
            items(state.visibleItems, key = { it.id }) { item ->
                ListItemRow(
                    title = item.name,
                    subtitle = item.category,
                    icon = Icons.Filled.Inventory2,
                    tone = BadgeTone.Primary,
                    pillLabel = item.status,
                    pillTone = BadgeTone.Success,
                )
            }
        }
    }
}
