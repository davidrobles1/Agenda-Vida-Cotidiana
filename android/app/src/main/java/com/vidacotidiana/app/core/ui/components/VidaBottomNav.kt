package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

enum class BottomNavDestination(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Filled.Home),
    Tasks("Tareas", Icons.Filled.Description),
    Shared("Compartidos", Icons.Filled.Groups),
    More("Más", Icons.Filled.MoreHoriz),
}

/**
 * UX-006: reference's 5-item bottom nav (Inicio/Tareas/+/Compartidos/Más).
 * The "+" isn't a fifth destination with its own screen — it's a shortcut
 * into Tareas' existing create form (that's the one real "create" action
 * this app has; no new creation flow was invented for the mock modules).
 */
@Composable
fun VidaBottomNav(
    current: BottomNavDestination,
    onSelect: (BottomNavDestination) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = VidaTheme.colors.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = VidaSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(BottomNavDestination.Home, current, onSelect)
            NavItem(BottomNavDestination.Tasks, current, onSelect)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(VidaTheme.colors.primary, CircleShape)
                    .clickable(onClick = onCreateClick)
                    .testTag("bottom_nav_create"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo", tint = VidaTheme.colors.onPrimary, modifier = Modifier.size(24.dp))
            }
            NavItem(BottomNavDestination.Shared, current, onSelect)
            NavItem(BottomNavDestination.More, current, onSelect)
        }
    }
}

@Composable
private fun NavItem(destination: BottomNavDestination, current: BottomNavDestination, onSelect: (BottomNavDestination) -> Unit) {
    val selected = destination == current
    val color = if (selected) VidaTheme.colors.primary else VidaTheme.colors.textSecondary
    Column(
        modifier = Modifier
            .testTag("bottom_nav_${destination.name}")
            .clickable(onClick = { onSelect(destination) })
            .padding(VidaSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(destination.icon, contentDescription = destination.label, tint = color, modifier = Modifier.size(22.dp))
        Text(destination.label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
