package com.vidacotidiana.app.feature.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope

/**
 * Inventario. ADR-022: un artículo puede llevar garantía, y ese vínculo se
 * muestra en la píldora — no es un dato nuevo, es la relación que ya existe
 * en el modelo (`Warranty.inventoryItemId`).
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
    // El vínculo real: qué artículos tienen una garantía apuntándoles.
    val warrantied = state.data.warranties.mapNotNull { it.inventoryItemId }.toSet()

    val entries = items.map {
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = listOfNotNull(it.category, it.location).joinToString(" · "),
            icon = Icons.Outlined.Inventory2,
            // Un artículo no se «completa»: no existe ese estado en su backend.
            onEdit = { viewModel.requestEdit(CreatableResource.INVENTORY, it.id) },
            onDelete = { viewModel.deleteResource(CreatableResource.INVENTORY, it.id) },
            pill = if (it.id in warrantied) "Con garantía" to PillTone.OK else "Sin garantía" to PillTone.QUIET,
        )
    }
    // Las categorías salen de lo que el usuario realmente tiene, no de una
    // lista fija que podría no coincidir con sus artículos.
    val categories = listOf("Todos") + items.map { it.category }.distinct().sorted()

    ResourceListScreen(
        title = "Inventario",
        subtitle = "Qué tienes, dónde está y si sigue con garantía.",
        eyebrow = plural(entries.size, "artículo", "artículos"),
        entries = entries,
        filters = categories,
        addLabel = "Nuevo artículo",
        emptyBody = "Registra lo que tienes para no perderle la pista.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.INVENTORY) },
        // Aquí los chips son categorías, no estados: filtran por el texto que
        // la fila lleva en su subtítulo.
        matchesFilter = { entry, f -> f == "Todos" || entry.subtitle.startsWith(f) },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}
