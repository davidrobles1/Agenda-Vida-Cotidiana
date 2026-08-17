package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * UX-006: reference dashboard's metric-card pattern (icon badge + number +
 * label + subtitle) — reused by Home for Tareas/Compartidos (real data) and
 * by the mock-data screens' own summary cards. `subtitleTone` lets the
 * subtitle read as a warning (e.g. "3 vencidas") without a second component.
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    subtitle: String?,
    icon: ImageVector,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    subtitleTone: BadgeTone? = null,
    onClick: (() -> Unit)? = null,
) {
    val toneColors = tone.resolve()
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(VidaShape.card),
        colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = VidaTheme.colors.textSecondary)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(toneColors.container, RoundedCornerShape(VidaShape.control)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = toneColors.on)
                }
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)
            subtitle?.let {
                val color = subtitleTone?.resolve()?.on ?: VidaTheme.colors.textSecondary
                Text(it, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}
