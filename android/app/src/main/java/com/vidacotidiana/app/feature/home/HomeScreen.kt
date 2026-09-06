package com.vidacotidiana.app.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.calendar.AlertSeverity
import com.vidacotidiana.app.core.calendar.weekOf
import com.vidacotidiana.app.core.data.WarrantyStatus
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DayRibbonItem
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.HeroCard
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.StripCard
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaProgress
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Inicio como CENTRO DE CONTROL, no como lista de registros.
 *
 * La jerarquía del artefacto, en este orden y por este motivo: primero LO QUE
 * URGE —una sola cosa, con su salida—, después los próximos días en el mismo
 * lenguaje visual del calendario (continuidad, no repetición), después el
 * progreso real del día, después las áreas y por último la actividad.
 */
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val today = LocalDate.now()
    val todayContent = viewModel.contentFor(today)
    val urgent = todayContent.alerts.firstOrNull { it.severity == AlertSeverity.HIGH }
        ?: todayContent.alerts.firstOrNull()

    val todayTasks = todayContent.tasks
    val doneCount = todayTasks.count { it.done }

    VidaScreen(
        title = "¡Hola!",
        subtitle = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            .replaceFirstChar { it.uppercase() } + " ${today.dayOfMonth} de " +
            today.month.getDisplayName(TextStyle.FULL, Locale("es", "MX")),
        onNavigationClick = openDrawerAction(drawerState, scope),
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
        actions = {
            VidaIconButton(Icons.Outlined.Notifications, "Notificaciones", badge = true) { onNavigate(Routes.NOTIFICATIONS) }
            VidaIconButton(Icons.Outlined.Settings, "Ajustes") { onNavigate(Routes.SETTINGS) }
        },
    ) {
        StaggeredAppear(0) {
            HeroCard(
                eyebrow = "Lo que urge",
                title = urgent?.label ?: "Nada urgente hoy",
                body = urgent?.let { "${it.source.label} · ${it.message}" } ?: "Tu día está en calma.",
                primaryAction = (urgent?.let { "Ver en ${it.source.label}" } ?: "Ver el día") to {
                    onNavigate(urgent?.source?.route ?: Routes.CALENDAR)
                },
                secondaryAction = "Abrir calendario" to { onNavigate(Routes.CALENDAR) },
            )
        }

        StaggeredAppear(1) { Eyebrow("Los próximos 7 días") }
        StaggeredAppear(2) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                (0..6).map { today.plusDays(it.toLong()) }.forEach { date ->
                    DayRibbonItem(
                        date = date,
                        content = viewModel.contentFor(date),
                        isToday = date == today,
                        isSelected = false,
                        onClick = { viewModel.selectDate(date); onNavigate(Routes.CALENDAR) },
                    )
                }
            }
        }

        StaggeredAppear(3) { Eyebrow("Tu día") }
        StaggeredAppear(4) {
            VidaCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (todayTasks.size - doneCount > 0) {
                            "${todayTasks.size - doneCount} " +
                                if (todayTasks.size - doneCount == 1) "tarea pendiente" else "tareas pendientes"
                        } else {
                            "Todo hecho hoy"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = c.text,
                    )
                    Text("$doneCount de ${todayTasks.size}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                VidaProgress(if (todayTasks.isEmpty()) 0f else doneCount.toFloat() / todayTasks.size)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                    VidaSmallButton("Ver tareas", { onNavigate(Routes.TASKS) }, ghost = true)
                    VidaSmallButton("Compartidos", { onNavigate(Routes.SHARED) }, ghost = true)
                }
            }
        }

        StaggeredAppear(5) { Eyebrow("Lo que se viene") }
        StaggeredAppear(6) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                StripCard("Pagos", state.data.payments.size.toString(), "compromisos", { onNavigate(Routes.PAYMENTS) })
                StripCard("Mantenimiento", state.data.maintenance.size.toString(), "programados", { onNavigate(Routes.MAINTENANCE) })
                StripCard(
                    "Garantías",
                    state.data.warranties.count { it.status != WarrantyStatus.VENCIDA }.toString(),
                    "vigentes",
                    { onNavigate(Routes.WARRANTIES) },
                )
                StripCard("Inventario", state.data.inventory.size.toString(), "artículos", { onNavigate(Routes.INVENTORY) })
            }
        }

        StaggeredAppear(7) { Eyebrow("Actividad reciente") }
        StaggeredAppear(8) {
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                ResourceRow(
                    title = "Tienes ${todayContent.alerts.size} avisos hoy",
                    subtitle = "Derivados de tus garantías, pagos y mantenimientos",
                    icon = Icons.Outlined.Autorenew,
                    onClick = { onNavigate(Routes.CALENDAR) },
                )
                ResourceRow(
                    title = "Compartidos contigo",
                    subtitle = "Lo que tu familia comparte",
                    icon = Icons.Outlined.Share,
                    onClick = { onNavigate(Routes.SHARED) },
                )
            }
        }
    }
}
