package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vidacotidiana.app.core.ui.SectionVisual
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
    /**
     * QUIÉN TIENE ACCESO a este registro.
     *
     * Nula en los tipos que el dominio no sabe compartir —notas, objetivos,
     * lugares, rutinas, personas, proyectos—; ahí el botón no se dibuja en vez
     * de dibujarse y fallar. Los seis que sí lo admiten son los de
     * `ShareableKind`, que es copia del enum del backend.
     */
    val onAccess: (() -> Unit)? = null,
    /** Con cuánta gente está compartido ya. Cero no se dibuja. */
    val sharedWith: Int = 0,
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
     * ABRIR EL REGISTRO EN SU PANTALLA. Ya no es opcional en la práctica.
     *
     * Antes, cuando no se pasaba, la lista abría una hoja deslizante con el
     * título, el subtítulo y una pila de botones. Esa hoja ya no existe: era
     * una tercera forma de ver un recurso —ni la lista ni la ficha— que
     * obligaba a un toque extra para llegar a cualquier sitio y no dejaba
     * espacio ni para editar ni para enseñar el documento adjunto.
     *
     * Si una sección no pasa ruta, tocar una fila no hace nada en vez de
     * abrir algo a medias.
     */
    onOpenRoute: ((ResourceEntry) -> Unit)? = null,
    addLabel: String,
    emptyBody: String,
    /**
     * LA IDENTIDAD VISUAL DE ESTA SECCIÓN.
     *
     * Con ella, el estado vacío de la sección se dibuja ilustrado —icono
     * grande sobre su halo, con su color y su botón— en vez de como texto
     * suelto. Nula, se comporta como siempre: ninguna sección que no la pase
     * cambia de aspecto.
     *
     * Se deduce sola de la ruta, así que una sección no tiene que declararla
     * dos veces; sólo hay que pasarla donde la ruta no lo diga.
     */
    section: SectionVisual? = null,
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
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    val visible = entries.filter {
        (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)) &&
            (filters.isEmpty() || matchesFilter(it, filter))
    }

    val untrusted = error != null && entries.isEmpty()
    // CABECERA DE ADR-028: la de Documentos. Título grande, «+» y campana a la
    // derecha, cifras en tarjetas, buscador y chips. El botón de la izquierda
    // sigue siendo «atrás» o el menú, según cómo se llegó a la sección.
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = loading && entries.isNotEmpty(),
        onRefresh = onRetry,
        modifier = Modifier.fillMaxSize().background(c.surface),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.offset(x = (-12).dp)) {
                val (back, click) = navButtonFor(showBack, if (showBack) onBack else openDrawerAction(drawerState, scope))
                VidaIconButton(
                    if (back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                    if (back) "Atrás" else "Abrir navegación",
                    tint = c.text,
                    onClick = click,
                )
            }
            Column(Modifier.weight(1f).offset(x = (-8).dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 2)
            }
            VidaIconButton(Icons.Filled.Add, addLabel, tint = c.text, onClick = onAdd)
            onNotifications?.let { abrir ->
                VidaIconButton(
                    Icons.Outlined.Notifications,
                    if (notificationsBadge == 0) "Avisos" else "Avisos · $notificationsBadge sin leer",
                    tint = c.text,
                    badge = notificationsBadge > 0,
                    onClick = abrir,
                )
            }
        }

        // UNA CIFRA ES UNA AFIRMACIÓN SOBRE LOS DATOS, y cuando la carga falló
        // no tenemos datos sobre los que afirmar nada: sin datos en memoria,
        // las cifras y la cabecera propia se callan.
        if (metrics.isNotEmpty() && !untrusted) {
            StaggeredAppear(0) { MetricTiles(metrics, section) }
        }
        if (!untrusted) StaggeredAppear(1) { header() }
        StaggeredAppear(2) {
            ListSearchBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar en ${title.lowercase()}…",
                trailing = if (query.isNotEmpty()) ({ SearchClearButton { query = "" } }) else null,
            )
        }
        if (filters.isNotEmpty()) {
            StaggeredAppear(2) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filters.forEach { f -> ListChip(f, filter == f, onClick = { filter = f }) }
                }
            }
        }
        if (!untrusted) {
            StaggeredAppear(3) {
                Text(
                    if (selecting) "${selected.size} seleccionados" else eyebrow.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = c.text,
                )
            }
        }

        bulkAction?.let { bulk ->
            StaggeredAppear(3) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                ) {
                    if (!selecting) {
                        VidaSmallButton("Seleccionar", { selecting = true }, ghost = true)
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
                    // Ilustrado solo en el vacío de PRIMERA VEZ. Una búsqueda
                    // sin resultados no es una sección vacía: dibujarle la
                    // ilustración de «no tienes nada» diría algo falso.
                    section = section.takeIf { query.isBlank() },
                )
            }
        } else {
            StaggeredAppear(4) {
                ResourceBoard(
                    shape = shape,
                    entries = visible,
                    onOpenDetail = { entry -> onOpenRoute?.invoke(entry) },
                    selecting = selecting,
                    selectedIds = selected,
                    onToggleSelect = { entry ->
                        selected = if (entry.id in selected) selected - entry.id else selected + entry.id
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    }
}

/**
 * Las cifras de la sección en tarjetas, como el resumen de Documentos. Hasta
 * tres reparten el ancho; con más, la fila se desliza.
 */
@Composable
private fun MetricTiles(metrics: List<Triple<String, String, String>>, section: SectionVisual?) {
    val c = VidaTheme.colors
    val tint = section?.tint ?: c.primary
    @Composable
    fun Tile(kicker: String, value: String, caption: String, modifier: Modifier) {
        Column(
            modifier.softCard().padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            section?.let { Icon(it.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = c.text,
                maxLines = 1,
            )
            Text(
                listOf(kicker, caption).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
    if (metrics.size <= 3) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.forEach { (k, v, s) -> Tile(k, v, s, Modifier.weight(1f)) }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            metrics.forEach { (k, v, s) -> Tile(k, v, s, Modifier.width(118.dp)) }
        }
    }
}
