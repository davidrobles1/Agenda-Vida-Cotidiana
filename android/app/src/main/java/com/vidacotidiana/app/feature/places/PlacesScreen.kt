package com.vidacotidiana.app.feature.places

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.Place
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError

/**
 * Lugares (FR-033). Sección secundaria de Laboral: se llega desde el cajón.
 *
 * ES UN CATÁLOGO, NO UNA RELACIÓN. Ninguna tarea referencia a un lugar — el
 * backend lo declara y lo deja fuera de alcance a propósito. En la Web, elegir
 * un lugar al crear una tarea COPIA su texto al campo `location`; no lo enlaza.
 * Esta pantalla NO toca tareas de ninguna forma.
 *
 * SIN ACCIÓN RÁPIDA: un lugar no se completa ni se ejecuta. Su backend expone
 * solo CRUD, así que `onComplete` queda nulo.
 *
 * SIN FILTROS, y no por descuido: un lugar no tiene tipo, ni categoría, ni
 * estado. No hay nada por lo que filtrar, y unos chips inventados serían
 * relleno. La búsqueda sí sirve, y la da `ResourceListScreen` gratis sobre el
 * título y el subtítulo.
 */
@Composable
fun PlacesScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val places = state.data.places.sortedBy { it.name }
    val peopleById = state.data.people.associateBy { it.id }

    val entries = places.map { place ->
        val person = place.personId?.let { peopleById[it]?.name }
        ResourceEntry(
            id = place.id,
            title = place.name,
            // Solo lo que existe. Un lugar sin dirección y sin persona no
            // muestra etiquetas vacías: muestra que no tiene más datos.
            subtitle = listOfNotNull(
                place.address?.ifBlank { null },
                person?.let { "De $it" },
            ).joinToString(" · ").ifBlank { "Sin dirección" },
            icon = Icons.Outlined.Place,
            onEdit = { viewModel.requestEdit(CreatableResource.PLACE, place.id) },
            // «Cómo llegar» reutiliza `openDirections`, escrito en la fase de
            // Ubicación: mismo `geo:`, mismos cero permisos, misma delegación
            // en la aplicación de mapas del teléfono. No se duplica.
            extraActions = listOf(
                "Cómo llegar" to { viewModel.openDirections(context, place.locationText()) },
            ),
            onDelete = { viewModel.deleteResource(CreatableResource.PLACE, place.id) },
            // Sin `onComplete`: no existe ninguna acción de estado.
        )
    }

    ResourceListScreen(
        title = "Lugares",
        subtitle = "Las direcciones que repites, guardadas.",
        eyebrow = plural(places.size, "lugar", "lugares"),
        entries = entries,
        // Lista vacía a propósito: no hay ningún campo por el que filtrar.
        filters = emptyList(),
        addLabel = "Nuevo lugar",
        emptyBody = "Guarda una dirección a la que vuelvas para no volver a escribirla.",
        loading = state.loading,
        error = state.sliceError(DataSlice.PLACES),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.PLACE) },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}

/**
 * Qué texto se manda al mapa: la dirección si la hay, y si no el nombre.
 *
 * Es la misma regla que `placeLocationText` en `web/src/features/places/api.ts`
 * —`place.address?.trim() ? place.address : place.name`—, replicada aquí
 * porque aquello es TypeScript y no se puede compartir. Dos reglas distintas
 * mandarían al usuario a dos sitios distintos desde el mismo lugar.
 *
 * Con solo el nombre, el mapa puede no encontrar nada. Se acepta: es lo que ya
 * hace la Web, y divergir sería inventar una segunda política.
 */
private fun Place.locationText(): String =
    address?.takeIf { it.isNotBlank() } ?: name
