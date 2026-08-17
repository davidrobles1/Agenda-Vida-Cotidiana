package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * UX-006: "row with colored icon + title + subtitle + status pill" — the
 * reference's list-item pattern, repeated across Próximas tareas,
 * Recordatorios importantes, Documentos, Garantías, Inventario, etc.
 */
@Composable
fun ListItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    pillLabel: String? = null,
    pillTone: BadgeTone? = null,
) {
    val toneColors = tone.resolve()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(toneColors.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = toneColors.on, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = VidaTheme.colors.text)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = VidaTheme.colors.textSecondary)
        }
        if (pillLabel != null && pillTone != null) {
            val pillColors = pillTone.resolve()
            StatusPill(label = pillLabel, container = pillColors.container, text = pillColors.on)
        }
    }
}
