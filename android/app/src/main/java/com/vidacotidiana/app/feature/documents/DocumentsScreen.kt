package com.vidacotidiana.app.feature.documents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.components.BulkAction
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.ui.VidaVocabulary

/** Documentos. Sección secundaria: se llega desde el menú, con vuelta atrás. */
@Composable
fun DocumentsScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val documents = state.data.documents

    val entries = documents.map {
        val familyVisible = it.visibility != "PRIVATE"
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = "${VidaVocabulary.human(it.category)} · ${it.sizeLabel} · ${it.dateLabel}",
            icon = Icons.Outlined.Description,
            group = VidaVocabulary.human(it.category),
            // ADR-025: los documentos se comparten SOLO para verlos, así que la
            // píldora dice quién los ve, no quién puede tocarlos.
            pill = when (it.visibility) {
                "PRIVATE" -> "Solo yo" to PillTone.QUIET
                "FAMILY_PUBLIC" -> "Toda la familia" to PillTone.NEUTRAL
                else -> "Compartido" to PillTone.NEUTRAL
            },
            // PARIDAD CON WEB: la Web permite editar, descargar, compartir,
            // hacer visible u ocultar a la familia y eliminar. Android solo
            // listaba y subía. Todos usan endpoints que ya existían.
            onEdit = { viewModel.requestEdit(CreatableResource.DOCUMENT, it.id) },
            // Un documento no se «completa»: no existe ese estado en su backend.
            // Abrirlo se delega al visor del sistema en vez de construir uno
            // propio dentro de Cotidiana.
            onOpen = { viewModel.openDocument(context, it.id, it.name) },
            openLabel = "Abrir",
            onDelete = { viewModel.deleteResource(CreatableResource.DOCUMENT, it.id) },
            extraActions = listOf(
                "Compartir archivo" to { viewModel.shareDocumentFile(context, it.id, it.name) },
                (if (familyVisible) "Ocultar a la familia" else "Ver toda la familia") to {
                    viewModel.setDocumentFamilyVisible(it.id, !familyVisible)
                },
            ),
        )
    }
    val categories = listOf("Todas") + documents.map { VidaVocabulary.human(it.category) }.distinct().sorted()

    ResourceListScreen(
        title = "Documentos",
        subtitle = "Lo importante, a mano y en su sitio.",
        eyebrow = plural(entries.size, "archivo", "archivos"),
        entries = entries,
        filters = categories,
        addLabel = "Subir documento",
        emptyBody = "Sube un documento para tenerlo siempre a mano.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.DOCUMENT) },
        matchesFilter = { entry, f -> f == "Todas" || entry.subtitle.startsWith(f) },
        // PARIDAD CON WEB: la Web permite bajarse varios documentos en un zip
        // (`POST /documents/download`) y Android no lo ofrecía, pese a que el
        // endpoint existía desde el principio.
        bulkAction = BulkAction(
            actionLabel = "Descargar zip",
            allLabel = "Descargar todos",
            onRun = { ids -> viewModel.downloadDocumentsZip(context, ids) },
        ),
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}
