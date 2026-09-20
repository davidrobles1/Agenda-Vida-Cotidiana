package com.vidacotidiana.app.feature.documents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.components.BulkAction
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
import com.vidacotidiana.app.core.data.humanSizeParts

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
            // El tamaño como cifra destacada: es lo que hace que la sección sea
            // una RETÍCULA de piezas y no una lista, y de paso lo que más
            // distingue un documento de otro al ojearlos.
            highlight = it.sizeLabel,
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
            openLabel = "Ver documento",
            // QUÉ ES, en el antetítulo. El backend solo acepta cinco tipos
            // —PDF, PNG, JPEG, WEBP y GIF (`ALLOWED_CONTENT_TYPES`)—, así que
            // todo lo que hay aquí se puede abrir con lo que el teléfono ya
            // trae. Decirlo en la tarjeta ahorra abrir para averiguarlo.
            typeTag = it.name.substringAfterLast('.', "").uppercase().ifBlank { null },
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
        // Retícula del artefacto. La tercera pieza es la que hace visible la
        // mecánica genérica de V37: cuántos documentos cuelgan de un recurso.
        header = {
            // La unidad se escoge según el tamaño, con el mismo formateador que
            // usa cada fila de la lista: antes esto dividía siempre entre megas
            // y un archivo de 1 KB salía como «0.0 MB».
            val (espacio, unidad) = humanSizeParts(documents.sumOf { it.sizeBytes })
            val adjuntos = documents.count { it.resourceId != null }
            VidaTileRow(
                listOf(
                    VidaTileSpec("Documentos", documents.size.toString(), "guardados",
                        VidaTheme.colors.primaryContainer, VidaTheme.colors.primary, VidaTheme.colors.primaryDeep,
                        weight = 1.32f),
                    VidaTileSpec("Espacio", espacio, unidad,
                        VidaTheme.colors.sunken, VidaTheme.colors.text, VidaTheme.colors.textSecondary),
                    VidaTileSpec("Adjuntos", adjuntos.toString(), "a un recurso",
                        VidaTheme.colors.secondContainer, VidaTheme.colors.second, VidaTheme.colors.second),
                ),
            )
        },
        title = "Documentos",
        subtitle = "Lo importante, a mano y en su sitio.",
        eyebrow = plural(entries.size, "archivo", "archivos"),
        entries = entries,
        filters = categories,
        addLabel = "Subir documento",
        emptyBody = "Sube un documento para tenerlo siempre a mano.",
        loading = state.loading,
        error = state.sliceError(DataSlice.DOCUMENTS),
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
        // La campana lleva de verdad a los avisos, y el punto sale de
        // cuántos quedan sin leer. Antes era `{}` con `badge = true`.
        onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        notificationsBadge = viewModel.avisosSinLeer(),
    )
}
