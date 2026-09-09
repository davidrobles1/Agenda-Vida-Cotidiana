package com.vidacotidiana.app.feature.warranties

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.WarrantyStatus
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope

/** Garantías. De aquí salen los avisos de 30/15/0 días que pinta el calendario. */
@Composable
fun WarrantiesScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val warranties = state.data.warranties
    val today = java.time.LocalDate.now()
    fun quedan(d: java.time.LocalDate): String {
        val n = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        return when {
            n < 0L -> "Venció"
            n == 0L -> "Hoy"
            n == 1L -> "1 día"
            n < 30L -> "$n días"
            else -> "${n / 30} ${if (n / 30 == 1L) "mes" else "meses"}"
        }
    }
    val entries = warranties.map {
        ResourceEntry(
            id = it.id,
            title = it.product,
            subtitle = it.expiresLabel,
            highlight = quedan(it.expiresOn),
            icon = Icons.Outlined.VerifiedUser,
            // Acciones reales: `PATCH /warranties/{id}` y el `complete` que el
            // backend ya expone. Marcar una vencida no tiene sentido, así que
            // ahí no se ofrece.
            onEdit = { viewModel.requestEdit(CreatableResource.WARRANTY, it.id) },
            onComplete = if (it.status != WarrantyStatus.VENCIDA) {
                { viewModel.completeResource(CreatableResource.WARRANTY, it.id) }
            } else null,
            completeLabel = "Ya la usé",
            onDelete = { viewModel.deleteResource(CreatableResource.WARRANTY, it.id) },
            pill = when (it.status) {
                WarrantyStatus.VIGENTE -> "Vigente" to PillTone.OK
                WarrantyStatus.POR_VENCER -> "Por vencer" to PillTone.WARN
                WarrantyStatus.VENCIDA -> "Vencida" to PillTone.QUIET
            },
        )
    }
    ResourceListScreen(
        title = "Garantías",
        subtitle = "Lo que aún está cubierto, y hasta cuándo.",
        eyebrow = plural(entries.size, "registrada", "registradas"),
        entries = entries,
        filters = listOf("Todas", "Vigentes", "Por vencer", "Vencidas"),
        metrics = listOf(
            Triple("Vigentes", warranties.count { it.status == WarrantyStatus.VIGENTE }.toString(), "cubiertas"),
            Triple("Por vencer", warranties.count { it.status == WarrantyStatus.POR_VENCER }.toString(), "atención"),
        ),
        addLabel = "Nueva garantía",
        emptyBody = "Registra una garantía y te avisaremos 30, 15 y 0 días antes.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.WARRANTY) },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}
