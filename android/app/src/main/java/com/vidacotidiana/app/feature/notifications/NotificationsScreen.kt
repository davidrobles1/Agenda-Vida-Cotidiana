package com.vidacotidiana.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaChipRow
import com.vidacotidiana.app.core.ui.components.VidaScreen

/** Notificaciones, agrupadas por cuándo llegaron. */
@Composable
fun NotificationsScreen(navController: NavHostController) {
    val c = VidaTheme.colors
    var filter by remember { mutableStateOf("Todas") }

    VidaScreen(
        title = "Notificaciones",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) { VidaChipRow(listOf("Todas", "Sin leer"), filter, { filter = it }) }
        StaggeredAppear(1) { Eyebrow("Hoy") }
        Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            StaggeredAppear(2) {
                ResourceRow(
                    "Te comprometieron con «Llevar el coche al taller»",
                    "Hace 2 horas · userb",
                    icon = Icons.Outlined.Share,
                    markBackground = c.warningContainer, markTint = c.warningText, tone = c.warning,
                )
            }
            StaggeredAppear(3) {
                ResourceRow(
                    "Un mantenimiento toca hoy",
                    "Derivado de tus registros",
                    icon = Icons.Outlined.Build,
                    markBackground = c.warningContainer, markTint = c.warningText, tone = c.warning,
                )
            }
        }
        StaggeredAppear(4) { Eyebrow("Antes") }
        Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            StaggeredAppear(5) {
                ResourceRow(
                    "Aceptaron tu invitación familiar",
                    "Ayer",
                    icon = Icons.Outlined.Groups,
                    markBackground = c.successContainer, markTint = c.successText, tone = c.successText,
                )
            }
            StaggeredAppear(6) {
                ResourceRow("Un pago se acerca", "Hace 3 días", icon = Icons.Outlined.Autorenew)
            }
        }
    }
}
