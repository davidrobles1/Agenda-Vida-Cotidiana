package com.vidacotidiana.app.core.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vidacotidiana.app.core.ui.VidaTheme

/** UX-006: filter-chip row pattern (Documentos "Todos/Personales/Hogar/...", Inventario categories). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VidaFilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VidaTheme.colors.primary,
            selectedLabelColor = VidaTheme.colors.onPrimary,
            containerColor = VidaTheme.colors.surfaceVariant,
            labelColor = VidaTheme.colors.textSecondary,
        ),
    )
}
