package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaTheme

/** UX-006: Home's greeting header (reference: "¡Hola, David! 👋" + avatar). No real user-profile-photo feature exists — the avatar is a plain initial, not a mock photo. */
@Composable
fun AppTopBar(greetingName: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("¡Hola, $greetingName! 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VidaTheme.colors.textSecondary)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(VidaTheme.colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                greetingName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VidaTheme.colors.primary,
            )
        }
    }
}
