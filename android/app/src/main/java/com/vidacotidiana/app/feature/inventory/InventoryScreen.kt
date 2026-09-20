package com.vidacotidiana.app.feature.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
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
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError

/**
 * Inventario. ADR-022 y V31: un artículo lleva su garantía Y su mantenimiento,
 * y los dos vínculos se muestran aquí — no son datos nuevos, son las relaciones
 * que ya existen en el modelo (`Warranty.inventoryItemId`,
 * `MaintenanceRecord.inventoryItemId`).
 *
 * Con las dos, el artículo responde las dos preguntas que se le hacen: si
 * sigue cubierto y qué le toca.
 */
@Composable
fun InventoryScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = state.data.inventory
    // Los vínculos reales: qué artículos tienen algo apuntándoles.
    val warrantied = state.data.warranties.mapNotNull { it.inventoryItemId }.toSet()
    // De varios mantenimientos gana el que toca ANTES, al revés que la
    // garantía —de la que importa la que sigue cubriendo—: de un mantenimiento
    // importa el que hay que hacer primero.
    val dueByItem = state.data.maintenance
        .filter { it.inventoryItemId != null }
        .groupBy { it.inventoryItemId!! }
        .mapValues { (_, records) -> records.minBy { it.nextDueOn } }

    val entries = items.map {
        val due = dueByItem[it.id]
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = listOfNotNull(
                // El valor interno (`ELECTRONICOS`) no se le enseña a nadie.
                VidaVocabulary.human(it.category),
                it.location,
                due?.let { record -> "${record.item}: ${record.nextDueLabel}" },
            ).joinToString(" · "),
            icon = Icons.Outlined.Inventory2,
            // La cifra que convierte la lista en RETÍCULA (`shapeOf`): los días
            // que faltan para su próximo mantenimiento. El artículo que no
            // tiene ninguno se queda sin cifra en vez de recibir un cero que
            // parecería «vence hoy».
            highlight = due?.let { record ->
                val d = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), record.nextDueOn)
                if (d < 0) "${-d}d" else "${d}d"
            },
            group = VidaVocabulary.human(it.category),
            // Un artículo no se «completa»: no existe ese estado en su backend.
            onEdit = { viewModel.requestEdit(CreatableResource.INVENTORY, it.id) },
            onDelete = { viewModel.deleteResource(CreatableResource.INVENTORY, it.id) },
            // La píldora dice LO QUE APREMIA. Un mantenimiento vencido gana a
            // cualquier estado de garantía: la garantía dice si estás cubierto,
            // el mantenimiento dice que hay algo que hacer.
            pill = when {
                due?.status == MaintenanceStatus.VENCIDO -> "Mantenimiento vencido" to PillTone.WARN
                due?.status == MaintenanceStatus.PROXIMO -> "Mantenimiento próximo" to PillTone.WARN
                it.id in warrantied -> "Con garantía" to PillTone.OK
                due != null -> "Con mantenimiento" to PillTone.OK
                else -> "Sin garantía" to PillTone.QUIET
            },
        )
    }
    // Las categorías salen de lo que el usuario realmente tiene, no de una
    // lista fija que podría no coincidir con sus artículos.
    val categories = listOf("Todos") + items.map { VidaVocabulary.human(it.category) }.distinct().sorted()

    ResourceListScreen(
        // Retícula del artefacto: el inventario no reclama nada, así que sus
        // piezas dicen QUÉ HAY y cuánto de eso está cubierto.
        header = {
            VidaTileRow(
                listOf(
                    VidaTileSpec("Artículos", items.size.toString(), "en casa",
                        VidaTheme.colors.sunken, VidaTheme.colors.text, VidaTheme.colors.textSecondary, weight = 1.32f),
                    VidaTileSpec("Con garantía", warrantied.size.toString(), "cubiertos",
                        VidaTheme.colors.successContainer, VidaTheme.colors.successText, VidaTheme.colors.successText),
                    VidaTileSpec("Con mantenim.", dueByItem.size.toString(), "programado",
                        VidaTheme.colors.primaryContainer, VidaTheme.colors.primary, VidaTheme.colors.primaryDeep),
                ),
            )
        },
        title = "Inventario",
        subtitle = "Qué tienes, dónde está y si sigue con garantía.",
        eyebrow = plural(entries.size, "artículo", "artículos"),
        entries = entries,
        filters = categories,
        addLabel = "Nuevo artículo",
        emptyBody = "Registra lo que tienes para no perderle la pista.",
        loading = state.loading,
        error = state.sliceError(DataSlice.INVENTORY),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.INVENTORY) },
        // Aquí los chips son categorías, no estados: filtran por el texto que
        // la fila lleva en su subtítulo.
        matchesFilter = { entry, f -> f == "Todos" || entry.subtitle.startsWith(f) },
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
