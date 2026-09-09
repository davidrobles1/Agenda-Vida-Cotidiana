package com.vidacotidiana.app.feature.resources

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.WorkResource
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.ui.VidaVocabulary

/**
 * Recursos de trabajo (FR-034).
 *
 * POR QUÉ TIENE PANTALLA PROPIA, y no va embebido como en la Web: Android no
 * tiene pantallas de detalle de Persona ni de Proyecto donde alojarlo, así que
 * replicar el modelo de la Web obligaría a construirlas solo para esto. Y hay
 * una ganancia real: un recurso SIN persona ni proyecto es válido en el backend
 * y hoy no se ve en ninguna parte — aquí sí.
 *
 * NO TIENE ACCIÓN RÁPIDA, y es correcto: su backend expone solo CRUD. No se
 * completa, no se ejecuta, no se resuelve. `onComplete` queda nulo a propósito.
 *
 * Un recurso NUNCA guarda un archivo: son metadatos más una referencia de
 * texto. Los documentos reales viven en Documentos (FR-030).
 */
@Composable
fun WorkResourcesScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = state.data.workResources.sortedBy { it.name }
    val peopleById = state.data.people.associateBy { it.id }
    val projectsById = state.data.projects.associateBy { it.id }

    val entries = resources.map { resource ->
        val person = resource.personId?.let { peopleById[it]?.name }
        val project = resource.projectId?.let { projectsById[it]?.name }
        val url = resource.openableUrl()
        ResourceEntry(
            id = resource.id,
            title = resource.name,
            // Solo lo que EXISTE. Sin persona no se pinta una etiqueta vacía;
            // sin proyecto tampoco. Un recurso suelto muestra su referencia o su
            // detalle, y si no tiene ninguno, que no tiene nada más.
            subtitle = listOfNotNull(
                person?.let { "De $it" },
                project,
                resource.reference?.ifBlank { null },
                resource.description?.ifBlank { null },
            ).joinToString(" · ").ifBlank { "Sin más detalle" },
            icon = Icons.Outlined.FolderOpen,
            group = typeLabel(resource.type),
            pill = typeLabel(resource.type) to PillTone.QUIET,
            onEdit = { viewModel.requestEdit(CreatableResource.WORK_RESOURCE, resource.id) },
            // «Abrir» SOLO si la referencia es de verdad una URL. Una ruta de
            // carpeta compartida o una nota suelta no lo son, y ofrecer un botón
            // que no lleva a ningún sitio sería peor que no ofrecerlo.
            onOpen = url?.let {
                {
                    val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                    runCatching { context.startActivity(intent) }
                }
            },
            openLabel = "Abrir",
            onDelete = { viewModel.deleteResource(CreatableResource.WORK_RESOURCE, resource.id) },
            // Sin `onComplete`: no existe ninguna acción de estado en su backend.
        )
    }

    ResourceListScreen(
        title = "Recursos",
        subtitle = "Enlaces, plantillas y manuales de tu trabajo.",
        eyebrow = plural(resources.size, "recurso", "recursos"),
        entries = entries,
        filters = listOf("Todos") + TYPES.map { it.second },
        addLabel = "Nuevo recurso",
        emptyBody = "Guarda un enlace, una plantilla o un manual para tenerlo a mano.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.WORK_RESOURCE) },
        // El filtro es el TIPO real, no una categoría inventada: la píldora ya
        // muestra exactamente eso, así que basta con compararla.
        matchesFilter = { entry, f -> f == "Todos" || entry.pill?.first == f },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}

/**
 * La referencia SOLO se ofrece como enlace cuando lo es.
 *
 * `reference` es un campo libre a propósito (DECISION del Product Owner): puede
 * ser una URL, la ruta de una carpeta compartida o cualquier apunte. No se
 * valida al escribirla — se comprueba aquí, al mostrarla.
 *
 * Se exige el esquema explícito en vez de adivinarlo: convertir "manual v3" en
 * "http://manual v3" sería inventar un enlace que no existe.
 */
private fun WorkResource.openableUrl(): String? {
    val value = reference?.trim().orEmpty()
    if (value.isEmpty()) return null
    return value.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
}

/** Los seis tipos del backend con su etiqueta. Los valores son los del enum. */
private val TYPES = VidaVocabulary.resourceTypes

/** Un valor desconocido se muestra tal cual: el backend declara seis, pero
    mostrar algo falso sería peor que mostrar el valor crudo. */
private fun typeLabel(type: String): String =
    TYPES.firstOrNull { it.first == type }?.second ?: type
