package com.vidacotidiana.app.feature.warranties

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.WarrantyStatus
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError

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
            busy = it.id in state.busy,
            icon = Icons.Outlined.VerifiedUser,
            // Acciones reales: `PATCH /warranties/{id}` y el `complete` que el
            // backend ya expone. Marcar una vencida no tiene sentido, así que
            // ahí no se ofrece.
            onEdit = { viewModel.requestEdit(CreatableResource.WARRANTY, it.id) },
            onComplete = if (it.status != WarrantyStatus.VENCIDA) {
                { viewModel.completeResource(CreatableResource.WARRANTY, it.id) }
            } else null,
            completeLabel = "Ya la usé",
            // VENCIDA cubre dos cosas distintas: caducada por fecha y marcada
            // como usada. Solo la segunda se puede deshacer —el mismo endpoint
            // alterna—, y se reconoce porque su fecha todavía no ha pasado.
            onRevert = if (it.status == WarrantyStatus.VENCIDA && !it.expiresOn.isBefore(today)) {
                { viewModel.revertResource(CreatableResource.WARRANTY, it.id) }
            } else null,
            revertLabel = "No la usé",
            onDelete = { viewModel.deleteResource(CreatableResource.WARRANTY, it.id) },
            pill = when (it.status) {
                WarrantyStatus.VIGENTE -> "Vigente" to PillTone.OK
                WarrantyStatus.POR_VENCER -> "Por vencer" to PillTone.WARN
                WarrantyStatus.VENCIDA -> "Vencida" to PillTone.QUIET
            },
        )
    }
    ResourceListScreen(
        // La composición del artefacto: las severidades de ADR-018 como piezas,
        // no como una tira que se desplaza y deja la tercera fuera de pantalla.
        // UNA SOLA CLASIFICACIÓN, la que ya calcula `WarrantyDto.toDomain`.
        //
        // Aquí había dos bloques de cifras con dos definiciones distintas de la
        // misma palabra: arriba «Vigentes» era `status == VIGENTE` y abajo
        // `status != VENCIDA`, que incluye las que están por vencer. La
        // pantalla llegaba a decir «VIGENTES 0» y «Vigentes 1» a la vez, y los
        // tres mosaicos sumaban dos sobre una única garantía registrada,
        // porque el iPad caía en dos categorías.
        //
        // `WarrantyStatus` ya es una partición de tres, disjunta y completa, y
        // ya aplica los 30 días de ADR-018 en un único sitio. Lo único que hacía
        // falta era dejar de reimplementarla: ahora los contadores, la píldora
        // de cada fila y los filtros leen exactamente lo mismo.
        header = {
            val vigentes = warranties.count { it.status == WarrantyStatus.VIGENTE }
            val pronto = warranties.count { it.status == WarrantyStatus.POR_VENCER }
            val vencidas = warranties.count { it.status == WarrantyStatus.VENCIDA }
            VidaTileRow(
                listOf(
                    VidaTileSpec("Vigentes", vigentes.toString(), "sin prisa",
                        VidaTheme.colors.successContainer, VidaTheme.colors.successText, VidaTheme.colors.successText),
                    VidaTileSpec("Pronto", pronto.toString(), "en 30 días",
                        VidaTheme.colors.warningContainer, VidaTheme.colors.warningText, VidaTheme.colors.warningText),
                    VidaTileSpec("Vencidas", vencidas.toString(), "sin cobertura",
                        VidaTheme.colors.errorContainer, VidaTheme.colors.error, VidaTheme.colors.error),
                ),
            )
        },
        title = "Garantías",
        subtitle = "Lo que aún está cubierto, y hasta cuándo.",
        eyebrow = plural(entries.size, "registrada", "registradas"),
        entries = entries,
        // Los tres filtros son los tres estados, ni uno más: antes había cinco
        // conceptos visuales para tres categorías reales, y dos de ellos
        // («Vigentes» arriba y abajo) querían decir cosas distintas.
        filters = listOf("Todas", "Vigentes", "Por vencer", "Vencidas"),
        addLabel = "Nueva garantía",
        emptyBody = "Registra una garantía y te avisaremos 30, 15 y 0 días antes.",
        loading = state.loading,
        error = state.sliceError(DataSlice.WARRANTIES),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.WARRANTY) },
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
