package com.vidacotidiana.app.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.resolve

data class MoreDestination(val label: String, val icon: ImageVector, val tone: BadgeTone)

/**
 * UX-006: hub for everything that doesn't fit the 5-slot bottom nav — the
 * reference doesn't show this screen directly (it's implied by "Más" in the
 * bottom nav), so this is a reasonable, standard "more menu" pattern rather
 * than an invented flow.
 *
 * UX-007: "Calendario" lives here rather than as a literal 6th bottom-nav
 * icon — a design call, not a limitation of the router. The bottom nav
 * already has real history with overflow at 4 items + a center FAB
 * (AND-004/AND-006: a 5-button row silently dropped buttons off-screen);
 * adding a 6th real icon risks the same failure mode again for marginal
 * benefit, since Calendario is a secondary, occasional-use screen, not a
 * daily-driver tab like Tareas/Compartidos. Web's sidebar has no such
 * width constraint, so Calendario is a first-class sidebar item there
 * (`AppShell.tsx`).
 */
@Composable
fun MoreScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToWarranties: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onLogout: () -> Unit,
) {
    val items = listOf(
        MoreDestination("Notificaciones", Icons.Filled.Notifications, BadgeTone.Primary) to onNavigateToNotifications,
        MoreDestination("Calendario", Icons.Filled.CalendarMonth, BadgeTone.Primary) to onNavigateToCalendar,
        MoreDestination("Documentos", Icons.Filled.Description, BadgeTone.Info) to onNavigateToDocuments,
        MoreDestination("Garantías", Icons.Filled.VerifiedUser, BadgeTone.Success) to onNavigateToWarranties,
        MoreDestination("Inventario", Icons.Filled.Inventory2, BadgeTone.Primary) to onNavigateToInventory,
        MoreDestination("Mantenimiento", Icons.Filled.Build, BadgeTone.Info) to onNavigateToMaintenance,
        MoreDestination("Suscripciones", Icons.Filled.Autorenew, BadgeTone.Primary) to onNavigateToSubscriptions,
        MoreDestination("Familia", Icons.Filled.Groups, BadgeTone.Success) to onNavigateToFamily,
    )

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Más", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            items(items) { (destination, onClick) ->
                MoreRow(destination, onClick)
            }
            item {
                MoreRow(MoreDestination("Cerrar sesión", Icons.AutoMirrored.Filled.Logout, BadgeTone.Error), onLogout)
            }
        }
    }
}

@Composable
private fun MoreRow(destination: MoreDestination, onClick: () -> Unit) {
    val tone = destination.tone.resolve()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(VidaShape.card),
        colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VidaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(36.dp).background(tone.container, RoundedCornerShape(VidaShape.control)), contentAlignment = Alignment.Center) {
                Icon(destination.icon, contentDescription = null, tint = tone.on, modifier = Modifier.size(18.dp))
            }
            Text(destination.label, style = MaterialTheme.typography.bodyLarge, color = VidaTheme.colors.text, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = VidaTheme.colors.textSecondary)
        }
    }
}
