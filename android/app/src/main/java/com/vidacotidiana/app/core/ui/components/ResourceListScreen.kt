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
    /**
     * Abrir el contenido del recurso con lo que el teléfono ya tenga.
     *
     * ESTE CAMPO ERA MUERTO. Documentos y Recursos de trabajo lo rellenaban
     * desde hace tiempo, pero ninguna pieza de la interfaz lo leía: ni la
     * tarjeta, ni el mosaico, ni la hoja de detalle. El resultado es que
     * Documentos tenía un visor completo —descarga, FileProvider e intent al
     * lector del sistema— al que no se podía llegar desde ninguna parte.
     */
    val onOpen: (() -> Unit)? = null,
    val openLabel: String = "Abrir",
    /**
     * VOLVER AL ESTADO ANTERIOR.
     *
     * La contraria de `onComplete`, y nula exactamente donde no existe: un
     * documento no se completa y por tanto tampoco se descompleta, y una rutina
     * ejecutada no tiene forma de borrar su última ocurrencia en el backend.
     *
     * Va aparte de `onComplete` y no como un toggle sobre él porque los dos
     * sentidos no son simétricos en la pantalla: hay registros que pueden
     * deshacerse SIN estar completados —un mantenimiento repetible adelantado
     * por error sigue estando «activo»— y un toggle no sabría expresar eso.
     */
    val onRevert: (() -> Unit)? = null,
    val revertLabel: String = "Deshacer",
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
    /**
     * EL AVANCE, en tanto por ciento — el anillo del artefacto.
     *
     * Cuando viene, SUSTITUYE al cuadro del icono en la fila, exactamente como
     * hace el artefacto aprobado: `o.ring !== undefined ? C.ring(...) : mark`.
     * No se añade al lado; una fila con anillo Y marca tendría dos piezas
     * redondas compitiendo a la izquierda.
     *
     * Nulo cuando no hay avance que mostrar —una tarea sin pasos—, y entonces
     * la fila enseña su icono como siempre.
     */
    val ring: Int? = null,
    /**
     * Hay una acción de este registro EN VUELO.
     *
     * El control de «hecho» lo dice mientras dura —gira en vez de quedarse
     * quieto— y deja de aceptar toques. Sin esto, pulsar la palomilla no
     * producía ninguna señal hasta que volvía la red, así que parecía rota y se
     * pulsaba otra vez.
     */
    val busy: Boolean = false,
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
    /**
     * Composición propia de la sección, encima de la búsqueda.
     *
     * La tira de métricas sirve para «7 · al mes», pero el artefacto pone en
     * varias secciones piezas que no son cifras sueltas: los anillos de hábito
     * de Rutinas, la retícula de severidades de Garantías. Sin este hueco,
     * cada pantalla tendría que abandonar `ResourceListScreen` y reimplementar
     * cabecera, búsqueda, filtros y estados — que es exactamente cómo aparecen
     * cuatro versiones distintas de la misma pantalla.
     *
     * Vacío por defecto: ninguna de las llamadas existentes cambia.
     */
    header: @Composable () -> Unit = {},
    /**
     * Abrir el registro en SU PANTALLA en vez de en la hoja interna.
     *
     * El artefacto le da pantalla propia a pago, mantenimiento, persona y
     * proyecto, con héroe, propiedades y secciones — eso no cabe en una hoja.
     * Cuando se pasa, la lista navega; cuando no, se conserva exactamente el
     * comportamiento anterior, así que ninguna sección existente cambia.
     */
    onOpenRoute: ((ResourceEntry) -> Unit)? = null,
    addLabel: String,
    emptyBody: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    showBack: Boolean,
    onBack: () -> Unit,
    /**
     * A dónde lleva la campana. NULA = no se dibuja.
     *
     * Era obligatoria y las TRECE secciones le pasaban `{}`: un botón que no
     * hacía nada, con el punto rojo fijado a `true` encima, en todas las
     * pantallas de recursos. Ahora o lleva a los avisos o no está.
     */
    onNotifications: (() -> Unit)? = null,
    /** Cuántos avisos sin leer. El punto sale de esta cifra, no de un `true`. */
    notificationsBadge: Int = 0,
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
    /** La forma del artefacto cuando la sección la fija. Nula: se deduce. */
    shape: ResourceShape? = null,
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
        // Las ocho secciones de recursos ya tienen su propio «reintentar»; el
        // gesto de tirar es el mismo acto, disponible sin tener que fallar
        // primero. Una sola línea aquí lo da en todas.
        onRefresh = onRetry,
        refreshing = loading,
        actions = {
            VidaIconButton(Icons.Filled.Add, addLabel, onClick = onAdd)
            onNotifications?.let { abrir ->
                VidaIconButton(
                    Icons.Outlined.Notifications,
                    if (notificationsBadge == 0) "Avisos" else "Avisos · $notificationsBadge sin leer",
                    badge = notificationsBadge > 0,
                    onClick = abrir,
                )
            }
        },
    ) {
        // UNA CIFRA ES UNA AFIRMACIÓN SOBRE LOS DATOS, y cuando la carga falló
        // no tenemos datos sobre los que afirmar nada.
        //
        // Antes las métricas y la cabecera se componían siempre, así que la
        // pantalla llegaba a decir «0 compromisos · todo al día» justo encima
        // de «No pudimos cargar esta sección». Callarlas no es esconder
        // información: es no inventarla. Cuando hay datos en memoria de una
        // carga anterior sí se siguen mostrando — ahí la cifra sí tiene
        // respaldo, y borrarla castigaría al usuario por un fallo de red.
        val untrusted = error != null && entries.isEmpty()
        if (metrics.isNotEmpty() && !untrusted) {
            StaggeredAppear(0) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                ) {
                    metrics.forEach { (k, v, s) -> StripCard(k, v, s, {}) }
                }
            }
        }
        if (!untrusted) StaggeredAppear(1) { header() }
        StaggeredAppear(2) {
            VidaSearchField(query, { query = it }, "Buscar en ${title.lowercase()}…")
        }
        if (filters.isNotEmpty()) {
            StaggeredAppear(2) { VidaChipRow(filters, filter, { filter = it }) }
        }
        // El antetítulo («1 REGISTRADA», «0 ARTÍCULOS») es un recuento, y por
        // tanto la misma clase de afirmación que las métricas de arriba.
        if (!untrusted) {
            StaggeredAppear(3) { Eyebrow(if (selecting) "${selected.size} seleccionados" else eyebrow) }
        }

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
                    shape = shape,
                    entries = visible,
                    onOpenDetail = { entry -> onOpenRoute?.invoke(entry) ?: run { detail = entry } },
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

                // VER EL CONTENIDO, arriba y sin compartir fila con nada.
                // Es la acción principal de un documento —para eso se guardó—
                // y hasta ahora no estaba en ningún sitio: el campo existía y
                // nadie lo pintaba.
                entry.onOpen?.let { open ->
                    VidaSmallButton(entry.openLabel, { open(); detail = null })
                }

                entry.extraActions.forEach { (label, action) ->
                    VidaSmallButton(label, { action(); detail = null }, ghost = true)
                }

                // Deshacer en su propia línea: es la salida a un error, y
                // ponerla al lado de «Hecho» invitaba justo al toque
                // equivocado que viene a reparar.
                entry.onRevert?.let { revert ->
                    VidaSmallButton(entry.revertLabel, { revert(); detail = null }, ghost = true)
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
