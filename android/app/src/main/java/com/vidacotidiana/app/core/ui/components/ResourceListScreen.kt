package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import kotlinx.coroutines.CoroutineScope

/** Una fila de recurso, en el vocabulario que las pantallas comparten. */
/**
 * Una acción que opera sobre VARIOS elementos a la vez.
 *
 * `onRun` recibe los ids marcados, o el conjunto VACÍO cuando el usuario pulsa
 * "todos" — que no es lo mismo que "ninguno": el backend de Documentos
 * interpreta una lista vacía como "todos los de este módulo", precisamente para
 * no depender de que el cliente enumere una lista paginada que solo tiene a
 * medias.
 */
data class BulkAction(
    val actionLabel: String,
    val allLabel: String,
    val onRun: (Set<String>) -> Unit,
)

data class ResourceEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val typeTag: String? = null,
    val amount: String? = null,
    val pill: Pair<String, PillTone>? = null,
    val icon: ImageVector? = null,
    val tone: Color? = null,
    /**
     * Las acciones REALES del recurso. Nulas cuando su backend no las tiene:
     * inventario, documentos, personas y proyectos no tienen «completar», así
     * que no reciben ese botón en vez de recibir uno que no hace nada.
     */
    val onEdit: (() -> Unit)? = null,
    val onComplete: (() -> Unit)? = null,
    val completeLabel: String = "Hecho",
    val onOpen: (() -> Unit)? = null,
    val openLabel: String = "Abrir",
    val onDelete: (() -> Unit)? = null,
    /** Acciones propias de una sección concreta, como las de Documentos. */
    val extraActions: List<Pair<String, () -> Unit>> = emptyList(),
    /**
     * Bajo qué encabezado se agrupa en el mosaico. Lo decide la pantalla:
     * Pagos agrupa por proximidad, Garantías por estado, Inventario por
     * categoría. Nulo o vacío = una sola tanda sin encabezado.
     */
    val group: String? = null,
    /**
     * El dato que se muestra grande en la tarjeta cuando no hay importe: los
     * días que faltan, la hora, lo que defina a ESE recurso. Lo decide cada
     * pantalla, porque qué destaca de un recurso es propio de su dominio.
     */
    val highlight: String? = null,
)

/**
 * La pantalla de un módulo de recursos, con toda la densidad del artefacto:
 * tira de métricas, búsqueda, chips de filtro, antetítulo con el recuento,
 * filas ricas y hoja de detalle.
 *
 * Se comparte porque las ocho secciones de recursos tienen exactamente la
 * misma anatomía; lo que cambia son sus datos y sus filtros. Escribirla ocho
 * veces habría garantizado ocho interpretaciones distintas del espaciado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceListScreen(
    title: String,
    subtitle: String,
    eyebrow: String,
    entries: List<ResourceEntry>,
    filters: List<String>,
    metrics: List<Triple<String, String, String>> = emptyList(),
    addLabel: String,
    emptyBody: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    showBack: Boolean,
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    /** Abre el formulario de alta de ESTA seccion. La hoja la pinta NavGraph. */
    onAdd: () -> Unit = {},
    /**
     * Qué significa cada chip en ESTA sección. Por defecto el primero es
     * «todo» y los demás miran la píldora del elemento, que es donde vive su
     * estado; una sección con otra semántica pasa la suya.
     */
    matchesFilter: (ResourceEntry, String) -> Boolean = { entry, f ->
        f == filters.firstOrNull() || entry.pill?.first.equals(f.trimEnd('s'), ignoreCase = true)
    },
    detailExtra: @Composable (ResourceEntry) -> Unit = {},
    /**
     * Acción sobre VARIOS elementos a la vez. Nula en casi todas las secciones:
     * solo Documentos tiene una (descargar en zip), y darle a las demás una
     * barra de selección que no lleva a ningún sitio sería ruido.
     */
    bulkAction: BulkAction? = null,
) {
    val c = VidaTheme.colors
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(filters.firstOrNull() ?: "") }
    var detail by remember { mutableStateOf<ResourceEntry?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visible = entries.filter {
        (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)) &&
            (filters.isEmpty() || matchesFilter(it, filter))
    }

    VidaScreen(
        title = title,
        subtitle = subtitle,
        showBack = showBack,
        onNavigationClick = if (showBack) onBack else openDrawerAction(drawerState, scope),
        actions = {
            VidaIconButton(Icons.Filled.Add, addLabel, onClick = onAdd)
            VidaIconButton(Icons.Outlined.Notifications, "Notificaciones", badge = true, onClick = onNotifications)
        },
    ) {
        if (metrics.isNotEmpty()) {
            StaggeredAppear(0) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                ) {
                    metrics.forEach { (k, v, s) -> StripCard(k, v, s, {}) }
                }
            }
        }
        StaggeredAppear(1) {
            VidaSearchField(query, { query = it }, "Buscar en ${title.lowercase()}…")
        }
        if (filters.isNotEmpty()) {
            StaggeredAppear(2) { VidaChipRow(filters, filter, { filter = it }) }
        }
        StaggeredAppear(3) { Eyebrow(if (selecting) "${selected.size} seleccionados" else eyebrow) }

        // La barra de selección vive DEBAJO de los filtros, no en la cabecera:
        // lo que se descarga es lo que el filtro y la búsqueda dejaron a la
        // vista, y ponerla arriba sugeriría que actúa sobre todo el listado.
        bulkAction?.let { bulk ->
            StaggeredAppear(3) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                ) {
                    if (!selecting) {
                        VidaSmallButton("Seleccionar", { selecting = true }, ghost = true)
                        // "Todos" no enumera la lista: manda el conjunto vacío,
                        // que el backend interpreta como "todos los de este
                        // módulo". Enumerar desde el cliente dependería de que
                        // la página cargada los tuviera todos.
                        VidaSmallButton(bulk.allLabel, { bulk.onRun(emptySet()) }, ghost = true)
                    } else {
                        VidaSmallButton(
                            "Cancelar",
                            {
                                selecting = false
                                selected = emptySet()
                            },
                            ghost = true,
                        )
                        VidaSmallButton(
                            bulk.actionLabel,
                            {
                                bulk.onRun(selected)
                                selecting = false
                                selected = emptySet()
                            },
                            enabled = selected.isNotEmpty(),
                        )
                    }
                }
            }
        }
        if (loading && entries.isEmpty()) {
            // Mientras carga no se afirma nada: ni que está vacío ni que falló.
            StaggeredAppear(4) { LoadingRows() }
        } else if (error != null && entries.isEmpty()) {
            // ADR-021(k): un estado vacío afirma un hecho sobre los datos del
            // usuario. Si la carga falló no sabemos nada de ellos, así que se
            // dice lo que pasó y se ofrece reintentar, en vez de mentir con
            // «todavía no hay nada aquí».
            StaggeredAppear(4) {
                EmptyState(
                    title = "No pudimos cargar esta sección",
                    body = "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to onRetry,
                )
            }
        } else if (visible.isEmpty()) {
            // Dos vacíos distintos que NO se pueden decir igual:
            //
            // FIRST_USE (`query` en blanco) — la sección no tiene nada todavía.
            //   El título NOMBRA la sección en vez de decir «aquí»: es la única
            //   forma de que responda a «dónde estoy» cuando la pantalla está,
            //   por definición, vacía de pistas. El cuerpo lo pone cada sección
            //   —trece cuerpos escritos, ninguno genérico— y la acción es su
            //   propio alta, que ya existe.
            //
            // NO_RESULTS — hay datos, pero el filtro o la búsqueda no los
            //   alcanza. No lleva acción de alta: crear algo nuevo no es la
            //   respuesta a una búsqueda fallida, y ofrecerlo insinuaría que el
            //   usuario no tiene nada cuando sí tiene.
            StaggeredAppear(4) {
                EmptyState(
                    title = if (query.isBlank()) "Aún no hay nada en ${title.lowercase()}" else "Nada coincide con «$query»",
                    body = if (query.isBlank()) emptyBody else "Prueba con otro término.",
                    action = if (query.isBlank()) addLabel to onAdd else null,
                )
            }
        } else {
            StaggeredAppear(4) {
                ResourceBoard(
                    entries = visible,
                    onOpenDetail = { detail = it },
                    selecting = selecting,
                    selectedIds = selected,
                    onToggleSelect = { entry ->
                        selected = if (entry.id in selected) selected - entry.id else selected + entry.id
                    },
                )
            }
        }
    }

    detail?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { detail = null },
            sheetState = sheetState,
            containerColor = c.surfaceVariant,
        ) {
            SheetSurface(onClose = { detail = null }, title = entry.title) {
                Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                detailExtra(entry)

                entry.extraActions.forEach { (label, action) ->
                    VidaSmallButton(label, { action(); detail = null }, ghost = true)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                    entry.onDelete?.let { del ->
                        VidaSmallButton("Eliminar", { del(); detail = null }, ghost = true)
                    }
                    entry.onComplete?.let { done ->
                        VidaSmallButton(entry.completeLabel, { done(); detail = null }, ghost = true)
                    }
                    entry.onEdit?.let { edit ->
                        VidaSmallButton("Editar", { edit(); detail = null })
                    }
                }
            }
        }
    }

}
