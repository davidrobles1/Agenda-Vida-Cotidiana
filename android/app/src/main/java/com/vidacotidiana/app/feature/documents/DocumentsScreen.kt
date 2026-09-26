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
import com.vidacotidiana.app.core.ui.VidaSections
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
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.app.Dialogo

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

    // QUIÉN TIENE ACCESO: el diálogo lo monta el controlador, y la

    // sección solo dice sobre qué recurso se abre.

    val acceso = rememberResourceAccess(viewModel)

    val compartidos = state.data.sharedByMe

        .filter { it.kind == ShareableKind.DOCUMENT }

        .groupingBy { it.resourceId }.eachCount()

    val entries = documents.map {
        val familyVisible = it.visibility != "PRIVATE"
        ResourceEntry(
            id = it.id,
            onAccess = { acceso.abrir(ShareableKind.DOCUMENT, it.id, it.name) },
            sharedWith = compartidos[it.id] ?: 0,
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
            //
            // VER ES AHORA DENTRO. Antes esto lanzaba un intent al lector del
            // sistema, lo que significaba sacar de la aplicación un archivo
            // que puede ser una identificación o un contrato. Salir a otra
            // aplicación sigue disponible, pero como acción aparte y pedida.
            onOpen = { navController.navigate(Routes.documentRoute(it.id)) },
            openLabel = "Ver documento",
            // QUÉ ES, en el antetítulo. El backend solo acepta cinco tipos
            // —PDF, PNG, JPEG, WEBP y GIF (`ALLOWED_CONTENT_TYPES`)—, así que
            // todo lo que hay aquí se puede abrir con lo que el teléfono ya
            // trae. Decirlo en la tarjeta ahorra abrir para averiguarlo.
            typeTag = it.name.substringAfterLast('.', "").uppercase().ifBlank { null },
            onDelete = { viewModel.deleteResource(CreatableResource.DOCUMENT, it.id) },
            extraActions = listOf(
                // El nombre dice a dónde va. «Compartir archivo» se confundía
                // con compartirlo dentro de Cotidiana, que es lo que hace el
                // botón de acceso de arriba y lo que casi siempre se quiere.
                "Abrir en otra app" to { viewModel.shareDocumentFile(context, it.id, it.name) },
                (if (familyVisible) "Ocultar a la familia" else "Ver toda la familia") to {
                    viewModel.setDocumentFamilyVisible(it.id, !familyVisible)
                },
            ),
        )
    }
    val categories = listOf("Todas") + documents.map { VidaVocabulary.human(it.category) }.distinct().sorted()

    ResourceListScreen(
        // La ficha del recurso, en su ventana. Antes esto caía en la
        // hoja deslizante que ya no existe.
        onOpenRoute = { navController.navigate(Routes.resourceRoute("DOCUMENT", it.id)) },
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
        section = VidaSections.Documentos,
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

    // Se pinta al final y no compone nada mientras no haya un
    // recurso abierto.
    acceso.Dialogo(viewModel)
}
