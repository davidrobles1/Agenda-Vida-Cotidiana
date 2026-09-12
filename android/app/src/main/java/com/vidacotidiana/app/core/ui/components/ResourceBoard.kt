package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaIconSize
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.tileFigure

/**
 * LA FORMA DE UNA PIEZA LA DECIDE SU CONTENIDO, NO SU ENTIDAD.
 *
 * Antes todo era un cuadrado. `aspectRatio(1f)` con el contenido repartido en
 * tres zonas funciona cuando hay una cifra en el centro —un importe en Pagos,
 * los días que faltan en Garantías— pero la zona central SOLO existe si la
 * pantalla manda `amount` o `highlight`. Personas, Proyectos, Documentos,
 * Inventario y las tareas sin hora no mandan ninguno, así que el cuadrado se
 * quedaba con un icono arriba, el nombre hundido abajo y **más de la mitad del
 * alto vacío**. El ojo lee ese hueco como "aquí falta algo", y por eso la
 * aplicación parecía vacía teniendo datos.
 *
 * El arreglo no es "cuadrado o fila" por sección: eso volvería a atar la forma
 * a la entidad y obligaría a cada pantalla a saber cómo quiere dibujarse. La
 * regla mira lo que la pieza LLEVA:
 *
 *  - `HERO`    — trae un importe. Es la cifra la que manda; merece una pieza
 *                cuadrada con la cifra en grande.
 *  - `METRIC`  — trae un dato derivado (días, hora, «Hoy»). Misma retícula,
 *                cifra algo menor: informa, no encabeza.
 *  - `LIST`    — no trae cifra pero sí un dato que cualifica. Fila de altura
 *                natural: ocupa lo que necesita y ni un dp más.
 *  - `COMPACT` — solo un nombre. Fila densa, sin cuadro de icono.
 *
 * SE DECIDE POR GRUPO, no por pantalla ni por pieza. Que en Tareas «HOY» sean
 * cuadrados con su hora y «PRÓXIMAS» filas sin fecha no es una incoherencia: el
 * antetítulo ya los separa, y la forma acaba diciendo lo mismo que el
 * encabezado. Dentro de un grupo todas las piezas comparten forma, que es lo
 * que mantiene la alineación entre vecinas.
 *
 * Ninguna pantalla cambia. Siguen mandando `amount`, `highlight`, `subtitle` y
 * `pill` como siempre; lo que cambia es que ahora eso DECIDE algo.
 */
enum class ResourceShape { HERO, METRIC, LIST, COMPACT }

/**
 * La regla, en una función. Se lee de arriba abajo: gana la señal más fuerte
 * que el grupo traiga.
 *
 * Basta con que ALGUNA pieza traiga cifra para que el grupo sea retícula: si
 * cuatro pagos de cinco tienen importe, mezclar una fila entre cuatro cuadrados
 * rompería la retícula por una excepción. La pieza sin cifra se resuelve dentro
 * del cuadrado (ver `focal` en `ResourceTile`), no fuera de él.
 */
internal fun shapeOf(entries: List<ResourceEntry>): ResourceShape = when {
    entries.any { it.amount != null } -> ResourceShape.HERO
    entries.any { it.highlight != null } -> ResourceShape.METRIC
    entries.any { it.subtitle.isNotBlank() || it.pill != null } -> ResourceShape.LIST
    else -> ResourceShape.COMPACT
}

/**
 * La vista de una sección de recursos.
 *
 * Se compone A MANO en filas y no con `LazyVerticalGrid` porque `VidaScreen` ya
 * desplaza su contenido, y anidar dos contenedores desplazables en el mismo eje
 * revienta en tiempo de ejecución.
 */
@Composable
fun ResourceBoard(
    entries: List<ResourceEntry>,
    onOpenDetail: (ResourceEntry) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * MODO SELECCIÓN. Cuando está activo, tocar una pieza la marca en vez de
     * abrir su detalle: son dos gestos incompatibles sobre el mismo objetivo.
     *
     * Solo lo usa Documentos, que es la única sección con una acción sobre
     * VARIOS a la vez (descargar en zip).
     */
    selecting: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: (ResourceEntry) -> Unit = {},
) {
    // Se conserva el orden en que la pantalla los entregó: ya vienen ordenados
    // por lo que su dominio considera importante.
    val groups = LinkedHashMap<String, MutableList<ResourceEntry>>()
    entries.forEach { groups.getOrPut(it.group ?: "") { mutableListOf() }.add(it) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Mínimo de 170 dp por pieza cuadrada: por debajo, el título y la cifra
        // dejan de caber y la retícula pasa a ser decorativa.
        val columns = ((maxWidth / 170.dp).toInt()).coerceIn(2, 4)

        Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
            groups.forEach { (group, items) ->
                if (group.isNotBlank()) {
                    // El antetítulo abre sección: respira por encima más que las
                    // piezas entre sí, para que se lea como cabecera y no como
                    // una pieza más de la lista.
                    Box(Modifier.padding(top = VidaLayout.blockGap)) { Eyebrow("$group · ${items.size}") }
                }
                when (shapeOf(items)) {
                    ResourceShape.HERO, ResourceShape.METRIC -> TileGrid(
                        items = items,
                        columns = columns,
                        hero = shapeOf(items) == ResourceShape.HERO,
                        onOpenDetail = onOpenDetail,
                        selecting = selecting,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                    )
                    ResourceShape.LIST, ResourceShape.COMPACT -> Column(
                        verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap),
                    ) {
                        val dense = shapeOf(items) == ResourceShape.COMPACT
                        items.forEach { entry ->
                            ResourceRowItem(
                                entry = entry,
                                dense = dense,
                                onOpenDetail = {
                                    if (selecting) onToggleSelect(entry) else onOpenDetail(entry)
                                },
                                selecting = selecting,
                                selected = entry.id in selectedIds,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * La retícula cuadrada, para los grupos que traen cifra.
 *
 * UNA PIEZA SOLA NO ES UNA RETÍCULA. Con un único elemento —«HOY · 1»,
 * «HECHAS · 1»— el cuadrado ocupaba media fila y dejaba la otra media vacía: el
 * hueco que quitamos del centro de la tarjeta reaparecía a su derecha, más
 * pequeño pero igual de accidental. Cuando no hay con quién alinearse, la pieza
 * se extiende y la cifra pasa a leerse de izquierda a derecha junto a su nombre,
 * que es como se lee una sola cosa.
 */
@Composable
private fun TileGrid(
    items: List<ResourceEntry>,
    columns: Int,
    hero: Boolean,
    onOpenDetail: (ResourceEntry) -> Unit,
    selecting: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (ResourceEntry) -> Unit,
) {
    if (items.size == 1) {
        val entry = items.first()
        WideTile(
            entry = entry,
            hero = hero,
            onOpenDetail = {
                if (selecting) onToggleSelect(entry) else onOpenDetail(entry)
            },
            selecting = selecting,
            selected = entry.id in selectedIds,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
        items.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap),
            ) {
                row.forEach { entry ->
                    Box(Modifier.weight(1f)) {
                        ResourceTile(
                            entry = entry,
                            hero = hero,
                            onOpenDetail = {
                                if (selecting) onToggleSelect(entry) else onOpenDetail(entry)
                            },
                            selecting = selecting,
                            selected = entry.id in selectedIds,
                        )
                    }
                }
                // Rellena la última fila incompleta: sin esto, una pieza sola se
                // estiraría al ancho de la fila y dejaría de ser cuadrada.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * La pieza con cifra cuando está sola en su grupo.
 *
 * Mismo material que el cuadrado —misma tarjeta, misma cifra, misma acción— en
 * horizontal: la cifra manda a la izquierda, la identidad la sigue y la acción
 * cierra por la derecha. Ocupa el ancho completo porque no hay ninguna vecina
 * con la que formar retícula, y su alto lo pone su contenido.
 */
@Composable
private fun WideTile(
    entry: ResourceEntry,
    hero: Boolean,
    onOpenDetail: () -> Unit,
    selecting: Boolean,
    selected: Boolean,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec
    val figure = entry.amount ?: entry.highlight

    VidaCard(
        modifier = if (selected) {
            Modifier.border(2.dp, c.primary, RoundedCornerShape(spec.radii.card))
        } else {
            Modifier
        },
        onClick = onOpenDetail,
        padding = VidaLayout.cardPadding,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            entry.icon?.let { icon ->
                VidaMark(
                    background = entry.tone?.copy(alpha = 0.14f) ?: c.primaryContainer,
                    contentColor = entry.tone ?: c.primary,
                    boxSize = 36.dp,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(VidaIconSize.small))
                }
                Spacer(Modifier.size(VidaLayout.blockGap))
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VidaLayout.textGap),
            ) {
                figure?.let {
                    Text(it, style = t.tileFigure(it, hero), color = entry.tone ?: c.text, maxLines = 1)
                }
                Text(
                    entry.title,
                    style = t.cardTitle,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.subtitle.isNotBlank()) {
                    Text(
                        entry.subtitle,
                        style = t.caption,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            entry.pill?.let { (label, tone) ->
                Box(Modifier.padding(start = VidaLayout.itemGap)) { VidaPill(label, tone) }
            }
            TileCorner(entry, selecting, selected)
        }
    }
}

/**
 * Pieza cuadrada. Tres zonas: marca y acción arriba, cifra al centro, identidad
 * abajo.
 *
 * SIEMPRE HAY FOCO. Si la pieza no trae cifra —una excepción dentro de un grupo
 * que sí las tiene— el foco pasa a ser su NOMBRE, escrito con el cuerpo de la
 * cifra y sin repetirse debajo. Es lo que impide que reaparezca el hueco: el
 * centro de la pieza nunca queda vacío.
 */
@Composable
private fun ResourceTile(
    entry: ResourceEntry,
    hero: Boolean,
    onOpenDetail: () -> Unit,
    selecting: Boolean = false,
    selected: Boolean = false,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec

    val figure = entry.amount ?: entry.highlight
    val focal = figure ?: entry.title
    // Con el nombre de foco no se repite abajo; en su lugar sube el subtítulo,
    // que es lo que aún no se ha dicho.
    val identity = if (figure != null) entry.title else entry.subtitle
    val support = if (figure != null) entry.subtitle else null

    VidaCard(
        modifier = Modifier
            .aspectRatio(1f)
            // Lo seleccionado se marca en el borde, no con otro fondo: el fondo
            // es del tema (ADR-023) y pisarlo rompería su personalidad.
            .then(
                if (selected) {
                    Modifier.border(2.dp, c.primary, RoundedCornerShape(spec.radii.card))
                } else {
                    Modifier
                },
            ),
        onClick = onOpenDetail,
        padding = VidaLayout.tilePadding,
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Zona 1 — de qué es, y la acción que se repite a diario.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                entry.icon?.let { icon ->
                    VidaMark(
                        background = entry.tone?.copy(alpha = 0.14f) ?: c.primaryContainer,
                        contentColor = entry.tone ?: c.primary,
                        boxSize = 32.dp,
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(VidaIconSize.small))
                    }
                }
                Spacer(Modifier.weight(1f))
                TileCorner(entry, selecting, selected)
            }

            // Zona 2 — el foco.
            Text(
                focal,
                style = t.tileFigure(focal, hero),
                color = entry.tone ?: c.text,
                maxLines = if (figure != null) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Zona 3 — quién es y en qué estado está.
            Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.textGap)) {
                Text(
                    identity,
                    style = if (figure != null) t.cardTitle else t.metadata,
                    color = if (figure != null) c.text else c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                support?.let {
                    Text(
                        it,
                        style = t.caption,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                entry.pill?.let { (label, tone) ->
                    Box(Modifier.padding(top = VidaSpacing.xs)) { VidaPill(label, tone) }
                }
            }
        }
    }
}

/**
 * La esquina de acción de una pieza cuadrada.
 *
 * DOS CAMBIOS DE FONDO respecto a lo que había:
 *
 * 1. El objetivo táctil pasa de 32 dp a los 48 dp que exige Android. Lo que se
 *    DIBUJA sigue siendo un cuadro de 34 dp; lo que se PULSA es la caja
 *    completa. Marcar algo como hecho es el gesto más repetido del producto y
 *    fallaba el mínimo de accesibilidad.
 *
 * 2. Completar deja de pintarse de verde. Antes una tarea pendiente mostraba un
 *    check verde —el botón— y una hecha mostraba otro check verde —su estado—:
 *    el mismo signo para «púlsame» y para «ya está». Ahora la ACCIÓN es un
 *    contorno neutro (`role.complete*`) y el verde queda reservado al ESTADO,
 *    que es la regla de `VidaRoles`.
 */
@Composable
private fun TileCorner(entry: ResourceEntry, selecting: Boolean, selected: Boolean) {
    val c = VidaTheme.colors
    val role = VidaTheme.role
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.control)

    if (selecting) {
        // En modo selección la esquina dice si ESTA pieza está marcada.
        TouchTarget {
            Box(
                Modifier
                    .size(34.dp)
                    .background(if (selected) role.selectedBg else c.surfaceVariant, shape)
                    .border(spec.borderWidth, if (selected) role.selectedBg else c.border, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Seleccionado",
                        tint = role.selectedFg,
                        modifier = Modifier.size(VidaIconSize.small),
                    )
                }
            }
        }
        return
    }

    entry.onComplete?.let { done ->
        TouchTarget(onClick = done) {
            Box(
                Modifier
                    .size(34.dp)
                    .border(spec.borderWidth, role.completeBorder, shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = entry.completeLabel,
                    tint = role.completeFg,
                    modifier = Modifier.size(VidaIconSize.small),
                )
            }
        }
    }
}

/**
 * Envoltorio que garantiza el mínimo táctil sin agrandar lo que se ve.
 *
 * Separar las dos medidas es la única forma de cumplir los 48 dp sin que un
 * botón de esquina se coma la pieza: el área transparente se solapa con el
 * padding, que no tiene nada que proteger.
 */
@Composable
private fun TouchTarget(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(VidaLayout.touchTarget)
            .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Fila de altura natural, para los grupos sin cifra.
 *
 * Esto es lo que sustituye al cuadrado medio vacío. Una persona, un proyecto o
 * un documento se identifican por su NOMBRE y una línea que lo cualifica: eso
 * mide lo que mide, y forzarlo a un cuadrado de 195 dp solo añadía aire donde
 * no hay contenido. Aquí caben seis donde antes cabían dos, y ninguna miente
 * sobre cuánta información tiene.
 *
 * `dense` quita el cuadro de icono cuando la pieza es solo un nombre: sin
 * subtítulo, la marca sería la mitad del peso visual de la fila.
 */
@Composable
private fun ResourceRowItem(
    entry: ResourceEntry,
    dense: Boolean,
    onOpenDetail: () -> Unit,
    selecting: Boolean,
    selected: Boolean,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val role = VidaTheme.role
    val spec = VidaTheme.spec

    VidaRow(
        modifier = if (selected) {
            Modifier.border(2.dp, c.primary, RoundedCornerShape(spec.radii.card))
        } else {
            Modifier
        },
        // SIN FILETE DE TONO. `VidaRow` puede dibujar uno en su borde izquierdo,
        // pero con el radio de Calm (26 dp) u Organic (24 dp) lo que se recorta
        // no es un filete: es una astilla de tres píxeles en mitad de una curva.
        // Y además sobra — el cuadro del icono ya se tiñe con el mismo tono, así
        // que el estado se dice dos veces y una de ellas queda mal.
        onClick = onOpenDetail,
        verticalPadding = if (dense) VidaLayout.itemGap else VidaLayout.rowPadding,
    ) {
        if (!dense) {
            entry.icon?.let { icon ->
                VidaMark(
                    background = entry.tone?.copy(alpha = 0.14f) ?: c.primaryContainer,
                    contentColor = entry.tone ?: c.primary,
                    boxSize = 36.dp,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(VidaIconSize.small))
                }
            }
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VidaLayout.textGap),
        ) {
            Text(
                entry.title,
                style = t.cardTitle,
                color = c.text,
                // Dos líneas, no una: la fila ya crece con su contenido, y el
                // nombre es lo que identifica la pieza. Con una píldora a la
                // derecha, un título medianamente largo se cortaba —«Impermea-
                // bilizar la a…»— y perdía justamente el dato que lo distingue.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.subtitle.isNotBlank()) {
                Text(
                    entry.subtitle,
                    style = t.metadata,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // El estado va al final de la fila, alineado con el de sus vecinas: en
        // una lista, lo que se compara entre piezas debe caer en la misma
        // columna. Dentro del cuadrado no había columna que compartir.
        entry.pill?.let { (label, tone) -> VidaPill(label, tone) }

        if (selecting) {
            TouchTarget {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(
                            if (selected) role.selectedBg else c.surfaceVariant,
                            RoundedCornerShape(spec.radii.control),
                        )
                        .border(
                            spec.borderWidth,
                            if (selected) role.selectedBg else c.border,
                            RoundedCornerShape(spec.radii.control),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Seleccionado",
                            tint = role.selectedFg,
                            modifier = Modifier.size(VidaIconSize.micro),
                        )
                    }
                }
            }
        } else {
            entry.onComplete?.let { done ->
                TouchTarget(onClick = done) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .border(spec.borderWidth, role.completeBorder, RoundedCornerShape(spec.radii.control)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = entry.completeLabel,
                            tint = role.completeFg,
                            modifier = Modifier.size(VidaIconSize.small),
                        )
                    }
                }
            }
        }
    }
}
