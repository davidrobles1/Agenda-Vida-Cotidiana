package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vidacotidiana.app.core.ui.VidaSpacing

/**
 * UX-006/design-system.md §5: state communicated with text AND color, never
 * color alone. Generalizes the pill pattern that used to be hardcoded inline
 * per-screen (`StatusBadge` in `RemindersScreen.kt`) into one shared
 * component, since the new list-item rows (Documentos, Garantías, etc.) need
 * the same pattern with different color/text pairs.
 */
@Composable
fun StatusPill(label: String, container: Color, text: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(container, RoundedCornerShape(percent = 50))
            .padding(horizontal = VidaSpacing.sm, vertical = VidaSpacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = text)
    }
}
