package com.vidacotidiana.app.feature.maintenance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.MaintenanceStatus
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError

/** Mantenimiento. Origen de los avisos de 7/3/0 días del calendario. */
@Composable
fun MaintenanceScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val records = state.data.maintenance
    val today = java.time.LocalDate.now()
    fun cuando(d: java.time.LocalDate): String {
        val n = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        return when {
            n < 0L -> "Atrasado"
            n == 0L -> "Hoy"
            n == 1L -> "Mañana"
            n < 30L -> "$n días"
            else -> "${n / 30} ${if (n / 30 == 1L) "mes" else "meses"}"
        }
    }
    val entries = records.map {
        val hecho = it.status == MaintenanceStatus.HECHO
        ResourceEntry(
            id = it.id,
            title = it.task,
            // Un puntual terminado no tiene «próxima vez» que anunciar: su
            // fecha ya pasó y repetirla sonaría a que sigue pendiente.
            subtitle = if (hecho) "Terminado · no se repite" else it.nextDueLabel,
            highlight = if (hecho) null else cuando(it.nextDueOn),
            icon = Icons.Outlined.Build,
            group = when (it.status) {
                MaintenanceStatus.VENCIDO -> "Toca ya"
                MaintenanceStatus.PROXIMO -> "Próximos"
                MaintenanceStatus.AL_DIA -> "Al día"
                MaintenanceStatus.HECHO -> "Hechos"
            },
            onEdit = { viewModel.requestEdit(CreatableResource.MAINTENANCE, it.id) },
            // ADR-021: completar AVANZA la ocurrencia; no cierra el registro.
            // Terminado el puntual, ya no hay ocurrencia que completar.
            onComplete = if (hecho) null else {
                { viewModel.completeResource(CreatableResource.MAINTENANCE, it.id) }
            },
            completeLabel = "Hecho",
            // La vuelta atrás borra la última ejecución y devuelve la fecha
            // anterior. Se ofrece SIEMPRE, no solo sobre los terminados:
            // adelantar un mantenimiento repetible por error es igual de fácil
            // y hasta ahora no había forma de recuperar la fecha.
            onRevert = { viewModel.revertResource(CreatableResource.MAINTENANCE, it.id) },
            revertLabel = if (hecho) "Volver a programarlo" else "Deshacer el último",
            busy = it.id in state.busy,
            onDelete = { viewModel.deleteResource(CreatableResource.MAINTENANCE, it.id) },
            pill = when (it.status) {
                MaintenanceStatus.VENCIDO -> "Toca ya" to PillTone.WARN
                MaintenanceStatus.PROXIMO -> "Próximo" to PillTone.NEUTRAL
                MaintenanceStatus.AL_DIA -> "Al día" to PillTone.OK
                MaintenanceStatus.HECHO -> "Hecho" to PillTone.QUIET
            },
        )
    }
    ResourceListScreen(
        // El artefacto le da pantalla propia a este registro.
        onOpenRoute = { navController.navigate(Routes.maintenanceRoute(it.id)) },
        title = "Mantenimiento",
        subtitle = "Lo que toca revisar, y cuándo vuelve.",
        eyebrow = plural(entries.size, "programado", "programados"),
        entries = entries,
        filters = listOf("Todos", "Toca ya", "Próximo", "Al día", "Hecho"),
        metrics = listOf(
            Triple("Toca ya", records.count { it.status == MaintenanceStatus.VENCIDO }.toString(), "atrasados"),
            Triple("Próximos", records.count { it.status == MaintenanceStatus.PROXIMO }.toString(), "en camino"),
        ),
        addLabel = "Nuevo mantenimiento",
        emptyBody = "Programa un mantenimiento y el calendario avisará solo.",
        loading = state.loading,
        error = state.sliceError(DataSlice.MAINTENANCE),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.MAINTENANCE) },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        // La campana lleva de verdad a los avisos, y el punto sale de
        // cuántos quedan sin leer. Antes era `{}` con `badge = true`.
        onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        notificationsBadge = viewModel.avisosSinLeer(),
    )
}
