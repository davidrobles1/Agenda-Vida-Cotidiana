package com.vidacotidiana.app.feature.maintenance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.MaintenanceStatus
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope

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
    val entries = records.map {
        ResourceEntry(
            id = it.id,
            title = it.task,
            subtitle = buildString {
                append("próxima: ${it.nextDueLabel}")
                it.intervalMonths?.let { months ->
                    append(" · cada $months ${if (months == 1) "mes" else "meses"}")
                }
            },
            icon = Icons.Outlined.Build,
            onEdit = { viewModel.requestEdit(CreatableResource.MAINTENANCE, it.id) },
            // ADR-021: completar AVANZA la ocurrencia; no cierra el registro.
            onComplete = { viewModel.completeResource(CreatableResource.MAINTENANCE, it.id) },
            completeLabel = "Hecho",
            onDelete = { viewModel.deleteResource(CreatableResource.MAINTENANCE, it.id) },
            pill = when (it.status) {
                MaintenanceStatus.VENCIDO -> "Toca ya" to PillTone.WARN
                MaintenanceStatus.PROXIMO -> "Próximo" to PillTone.NEUTRAL
                MaintenanceStatus.AL_DIA -> "Al día" to PillTone.OK
            },
        )
    }
    ResourceListScreen(
        title = "Mantenimiento",
        subtitle = "Lo que toca revisar, y cuándo vuelve.",
        eyebrow = plural(entries.size, "programado", "programados"),
        entries = entries,
        filters = listOf("Todos", "Toca ya", "Próximo", "Al día"),
        metrics = listOf(
            Triple("Toca ya", records.count { it.status == MaintenanceStatus.VENCIDO }.toString(), "atrasados"),
            Triple("Próximos", records.count { it.status == MaintenanceStatus.PROXIMO }.toString(), "en camino"),
        ),
        addLabel = "Nuevo mantenimiento",
        emptyBody = "Programa un mantenimiento y el calendario avisará solo.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.MAINTENANCE) },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}
