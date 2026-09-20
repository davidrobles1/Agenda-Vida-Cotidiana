package com.vidacotidiana.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSectionLabel
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.navigation.Routes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import com.vidacotidiana.app.core.ui.components.vidaBeat
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import com.vidacotidiana.app.core.ui.components.VidaEase
import com.vidacotidiana.app.core.ui.components.VidaSpring
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

/**
 * TU DÍA — la línea de tiempo del artefacto maestro.
 *
 * LO QUE APORTA Y NO EXISTÍA. Inicio dice QUÉ reclama; esta pantalla dice
 * CUÁNDO. Son preguntas distintas: «4 cosas te esperan» no cuenta si son de las
 * nueve o de las siete, y la forma del día es justamente lo que permite decidir
 * qué cabe.
 *
 * LA LÍNEA ES PROPORCIONAL: 38 dp por hora, de 08:00 a 21:00. Una lista de
 * horas equiespaciadas mentiría sobre el hueco real entre dos citas.
 *
 * LO QUE NO TIENE HORA NO ENTRA EN LA LÍNEA. Un pago no ocurre «a las cuatro»;
 * inventarle una hora para colocarlo sería falsear el dato. Va en su propia
 * bandeja, debajo, dicho tal cual es.
 *
 * Datos reales: `DayTask.time` y `DayTask.location`, que ya existían en el
 * modelo, más `AttentionEngine` para lo que reclama sin hora.
 */
/**
 * LA FRANJA POR DEFECTO, no la franja posible.
 *
 * Antes eran los límites duros de la línea, y lo que caía fuera se arrastraba
 * dentro con un `coerceIn`: una tarea de las 06:00 se dibujaba en la raya de
 * las 08:00, es decir la pantalla mentía sobre la hora en vez de admitir que
 * no cabía. La franja real se calcula abajo a partir de lo que hay ese día.
 */
private const val DEFAULT_FIRST_HOUR = 8
private const val DEFAULT_LAST_HOUR = 21
private val HOUR_HEIGHT = 38.dp

/** Media altura de la etiqueta de hora: lo que hay que subirla para centrarla en su raya. */
private val HOUR_LABEL_LIFT = 7.dp

/**
 * Cuánto hay que sostener una tarea antes de poder arrastrarla.
 *
 * Un segundo: lo bastante para que no se dispare al desplazar la pantalla, y lo
 * bastante poco para que mover una tarea no se sienta como una espera. Dos
 * segundos, probados en el teléfono, resultaban largos.
 */
private const val HOLD_TO_DRAG_MILLIS = 1000L

/** Lo que dura en pantalla una tarea sin duración propia: una hora. */
private const val DEFAULT_SLOT_HOURS = 1f

@Composable
fun DiaScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    val tasks = viewModel.allTasks().filter { it.date == today }
    val timed = tasks.filter { it.time != null }.sortedBy { it.time }

    // EL CONJUNTO DE HOY, el mismo que cuenta el mosaico «Para hoy» de Inicio.
    //
    // Antes esto pedía `now()` —vencido + hoy—, así que esta pantalla y aquel
    // mosaico decían números distintos sobre la misma jornada. Lo atrasado
    // tiene ahora su propia pantalla, que es donde lleva su propio mosaico.
    val claims = AttentionEngine.today(
        AttentionEngine.scan(state.data, viewModel.allTasks(), today),
    )
    // Lo que ya está colocado en la línea no se repite debajo: una tarea con
    // hora está dibujada en su sitio, y volver a listarla como «suelta» la
    // contaría dos veces en la misma pantalla.
    val timedIds = timed.map { "task:${it.id}" }.toSet()
    val untimed = claims.filterNot { it.id in timedIds }

    /*
     * LA FRANJA HORARIA SE DERIVA DE LOS DATOS, no al revés.
     *
     * Ensanchar la ventana para que quepa lo que hay es lo contrario de
     * recortar la posición para que quepa en la ventana: aquí nada se desplaza
     * de su hora real. Si no hay nada fuera del horario habitual, la franja es
     * exactamente la de siempre y la pantalla se ve igual que en el artefacto.
     */
    /*
     * REPROGRAMAR ARRASTRANDO.
     *
     * Qué tarea se está moviendo y cuánto lleva desplazada. Vive aquí y no
     * dentro de la fila porque al soltar hay que convertir un desplazamiento en
     * una hora, y esa conversión necesita la misma escala —`HOUR_HEIGHT` y la
     * franja— con la que se colocaron todas.
     */
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    /** La hora desde la que arrancó el arrastre, en horas decimales. */
    var dragBaseHours by remember { mutableFloatStateOf(0f) }
    /**
     * Dónde cae verticalmente cada tarjeta dentro de la línea, medido.
     *
     * Hace falta porque el gesto ya NO vive en la tarjeta: vive en el lienzo, y
     * el lienzo tiene que poder responder «¿qué tarea hay bajo este punto?».
     * Se mide en vez de deducirse porque la altura de una tarjeta depende de su
     * contenido, y suponerla fallaría con los títulos de dos líneas.
     */
    val rowBounds = remember { mutableStateMapOf<String, ClosedFloatingPointRange<Float>>() }
    val density = LocalDensity.current

    val hours = timed.mapNotNull { it.time?.hour }
    val firstHour = minOf(DEFAULT_FIRST_HOUR, hours.minOrNull() ?: DEFAULT_FIRST_HOUR)
    val lastHour = maxOf(DEFAULT_LAST_HOUR, hours.maxOrNull() ?: DEFAULT_LAST_HOUR)

    VidaScreen(
        title = "Tu día",
        subtitle = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            .replaceFirstChar { it.uppercase() } + " ${today.dayOfMonth}",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        /* ── LA SEMANA, PARA SITUARSE ────────────────────────────────────── */
        StaggeredAppear(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0..6).map { today.minusDays((today.dayOfWeek.value - 1 - it).toLong()) }.forEach { day ->
                    val isToday = day == today
                    val count = viewModel.allTasks().count { it.date == day }
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .background(
                                if (isToday) c.primary else c.surfaceElevated,
                                RoundedCornerShape(16.dp),
                            )
                            .vidaClickable(onClick = { viewModel.selectDate(day) })
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es", "MX")).uppercase(),
                            style = t.micro,
                            color = if (isToday) c.onPrimary.copy(alpha = .75f) else c.textTertiary,
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = t.metricFigure.copy(fontSize = 15.sp, lineHeight = 19.sp),
                            color = if (isToday) c.onPrimary else c.textSecondary,
                        )
                        // Un punto por «hay algo», no un contador: el número
                        // exacto de cada día no cabe y tampoco hace falta.
                        Box(
                            Modifier
                                .size(5.dp)
                                .background(
                                    when {
                                        isToday -> c.onPrimary
                                        count > 0 -> c.primary
                                        else -> c.line
                                    },
                                    RoundedCornerShape(50),
                                ),
                        )
                    }
                }
            }
        }

        /* ── LA LÍNEA DEL DÍA ────────────────────────────────────────────── */
        StaggeredAppear(1) { VidaSectionLabel("La forma del día", trailing = "${timed.size} con hora") }
        StaggeredAppear(2) {
            if (timed.isEmpty()) {
                EmptyState(
                    title = "Nada con hora hoy",
                    body = "Lo que tiene hora aparece aquí colocado en su sitio. " +
                        "Lo demás está abajo, sin hora inventada.",
                    action = "Crear algo" to { navController.navigate(Routes.CREATE) },
                )
            } else {
                val now = LocalTime.now()
                // DOS SEGUNDOS, no el medio segundo de la plataforma.
                //
                // `detectDragGesturesAfterLongPress` lee cuánto hay que sostener
                // de la configuración de vista, así que subir el umbral aquí lo
                // cambia para toda la línea del día sin reescribir el detector:
                // el gesto sigue siendo el del sistema —con su cancelación y su
                // deslizamiento mínimo— y solo espera más.
                //
                // Acotado a esta pantalla a propósito: en el resto de la
                // aplicación una pulsación larga sigue tardando lo de siempre.
                val viewConfig = LocalViewConfiguration.current
                val holdToDrag = remember(viewConfig) {
                    object : ViewConfiguration by viewConfig {
                        override val longPressTimeoutMillis: Long = HOLD_TO_DRAG_MILLIS
                    }
                }
                CompositionLocalProvider(LocalViewConfiguration provides holdToDrag) {
                val hourPxOuter = with(density) { HOUR_HEIGHT.toPx() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(HOUR_HEIGHT * (lastHour - firstHour) + 30.dp)
                        // EL GESTO VIVE AQUÍ, NO EN LA TARJETA.
                        //
                        // ESTA ES LA CAUSA DE TODO LO QUE IBA MAL. Un nodo que
                        // captura punteros recibe las coordenadas YA
                        // transformadas por las capas que tiene encima. Si el
                        // detector está en la propia tarjeta y la tarjeta se
                        // desplaza con el arrastre, cada desplazamiento se resta
                        // de la siguiente lectura: el gesto se mide a sí mismo.
                        // El resultado es lo que se sentía al arrastrar —la
                        // tarjeta iba a media velocidad, a tirones, y a veces la
                        // hora de destino se salía y volvía sola.
                        //
                        // El lienzo NO se mueve jamás, así que lo que mide es el
                        // movimiento real del dedo. La tarjeta solo se dibuja
                        // desplazada; no participa en la medición.
                        .pointerInput(timed, firstHour, lastHour, hourPxOuter) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { press ->
                                    // Qué tarjeta hay bajo el dedo. Sin esto el
                                    // lienzo no sabría qué está moviendo.
                                    val hit = rowBounds.entries
                                        .firstOrNull { press.y in it.value }?.key
                                    draggingId = hit
                                    dragDeltaY = 0f
                                    dragBaseHours = timed
                                        .firstOrNull { it.id == hit }
                                        ?.time
                                        ?.let { it.hour + it.minute / 60f }
                                        ?: 0f
                                },
                                onDrag = { change, amount ->
                                    if (draggingId != null) {
                                        change.consume()
                                        dragDeltaY += amount.y
                                    }
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragDeltaY = 0f
                                },
                                onDragEnd = {
                                    val moved = draggingId
                                    val landing = timeAtDrop(
                                        dragBaseHours + dragDeltaY / hourPxOuter,
                                        firstHour,
                                        lastHour,
                                    )
                                    draggingId = null
                                    dragDeltaY = 0f
                                    if (moved != null) {
                                        viewModel.rescheduleTask(moved, landing)
                                    }
                                },
                            )
                        },
                ) {
                    // POSICIONAR ES `offset`, NO `padding`.
                    //
                    // Un padding es margen INTERIOR: por definición no puede
                    // ser negativo, y Compose lanza `IllegalArgumentException`
                    // si lo es. Usarlo como sistema de coordenadas funcionaba
                    // sólo mientras ninguna posición cayera por encima del
                    // origen — y centrar la etiqueta sobre su raya es
                    // exactamente eso: la primera hora está en y = 0, así que
                    // su etiqueta iba a −7 dp y la pantalla cerraba la
                    // aplicación al abrirse. Siempre, no en un caso raro.
                    //
                    // `offset` es una traslación, admite negativos porque un
                    // desplazamiento negativo es una posición perfectamente
                    // válida, y es el primitivo que corresponde a lo que esta
                    // línea de tiempo hace: colocar cosas en coordenadas.
                    (firstHour..lastHour step 2).forEach { hour ->
                        val y = HOUR_HEIGHT * (hour - firstHour)
                        Text(
                            "%02d:00".format(hour),
                            style = t.micro,
                            color = c.textTertiary,
                            modifier = Modifier.offset(y = y - HOUR_LABEL_LIFT).width(40.dp),
                        )
                        Box(
                            Modifier
                                .offset(y = y)
                                .padding(start = 48.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(c.line),
                        )
                    }

                    // LA FRANJA A LA QUE VA A CAER, mientras se arrastra.
                    //
                    // Sin ella el usuario mueve la tarjeta a ciegas y no sabe en
                    // qué hora va a quedar hasta que suelta — que es justo lo
                    // que hacía que el gesto pareciera no responder.
                    //
                    // Ocupa UNA HORA porque una tarea no guarda duración en este
                    // modelo: mostrar otra cosa sería inventarle un dato. Se
                    // desliza entre cuartos con el easing de la casa, así que la
                    // banda «engancha» visiblemente en cada paso.
                    // UNA SOLA HORA DE DESTINO, para la banda Y para la tarjeta.
                    //
                    // Antes eran dos cálculos distintos —la tarjeta seguía al
                    // dedo en píxeles crudos y la banda mostraba la hora
                    // redondeada—, así que podían señalar sitios diferentes: se
                    // veía la tarjeta en un horario y el sombreado en otro, y al
                    // soltar mandaba el sombreado. Con un único valor no hay dos
                    // respuestas posibles: donde se ve la tarjeta es donde cae.
                    val hourPx = with(density) { HOUR_HEIGHT.toPx() }
                    val dropTarget: LocalTime? = draggingId?.let {
                        timeAtDrop(dragBaseHours + dragDeltaY / hourPx, firstHour, lastHour)
                    }
                    val targetY by animateDpAsState(
                        targetValue = dropTarget?.let {
                            HOUR_HEIGHT * (it.hour - firstHour) + HOUR_HEIGHT * (it.minute / 60f)
                        } ?: 0.dp,
                        animationSpec = tween(180, easing = VidaEase),
                        label = "dropTarget",
                    )

                    if (dropTarget != null) {
                        val target = dropTarget
                        Box(
                            Modifier
                                .offset(y = targetY)
                                .padding(start = 48.dp)
                                .fillMaxWidth()
                                .height(HOUR_HEIGHT * DEFAULT_SLOT_HOURS)
                                .background(
                                    c.primary.copy(alpha = 0.18f),
                                    RoundedCornerShape(18.dp),
                                )
                                .border(2.dp, c.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            // A la DERECHA: el nombre de la tarea ocupa la
                            // izquierda de la tarjeta que se está arrastrando, y
                            // los dos textos encimados no se leerían.
                            Text(
                                "%02d:%02d".format(target.hour, target.minute),
                                style = t.micro.copy(fontWeight = FontWeight.Bold),
                                color = c.primaryDeep,
                                modifier = Modifier.padding(end = 14.dp),
                            )
                        }
                    }

                    timed.forEach { task ->
                        val time = task.time ?: return@forEach
                        // Sin `coerceIn`: la franja ya se ensanchó arriba para
                        // contener esta hora, así que la posición es la real.
                        val offsetY = HOUR_HEIGHT * (time.hour - firstHour) +
                            HOUR_HEIGHT * (time.minute / 60f)
                        val dragging = draggingId == task.id
                        val tone = if (task.done) c.success else c.primary
                        // SE LEVANTA AL COGERLA, con el muelle del artefacto.
                        // Es la señal de que el gesto ya está activo: antes, al
                        // cumplirse el tiempo de espera no cambiaba nada y
                        // parecía que no había pasado.
                        val lift by animateFloatAsState(
                            targetValue = if (dragging) 1.03f else 1f,
                            animationSpec = tween(220, easing = VidaSpring),
                            label = "dragLift",
                        )
                        Row(
                            modifier = Modifier
                                .offset(y = offsetY)
                                .zIndex(if (dragging) 1f else 0f)
                                // EL ARRASTRE TRASLADA, NO RECOLOCA.
                                //
                                // `offset` cambia la posición de LAYOUT, y este
                                // es el mismo nodo que captura el gesto: al
                                // moverlo, el detector se reanclaba bajo un dedo
                                // que no se había movido y leía ese salto como
                                // movimiento nuevo. El desplazamiento crecía
                                // solo, la tarjeta se iba lejos del dedo y la
                                // hora de destino se salía de la franja — que es
                                // exactamente el «a veces me lo regresa».
                                //
                                // `translationY` dibuja en otro sitio sin mover
                                // el nodo, así que el gesto sigue midiendo desde
                                // donde empezó.
                                .graphicsLayer {
                                    // SIGUE AL DEDO, uno a uno y sin redondear.
                                    // Quien indica la franja es el sombreado; la
                                    // tarjeta va donde va la mano. Al revés
                                    // —la tarjeta saltando entre cuartos— el
                                    // arrastre se sentía agarrotado.
                                    translationY = if (dragging) dragDeltaY else 0f
                                    scaleX = lift
                                    scaleY = lift
                                    // FANTASMA MIENTRAS SE ARRASTRA. Una tarjeta
                                    // opaca tapa por completo la franja de
                                    // destino —en esta escala una hora mide
                                    // menos que la propia tarjeta— y entonces
                                    // sombrearla no serviría de nada. Traslúcida,
                                    // se ve a la vez lo que mueves y dónde cae.
                                    alpha = if (dragging) 0.55f else 1f
                                    shadowElevation = if (dragging) 18f else 0f
                                    shape = RoundedCornerShape(18.dp)
                                    clip = false
                                }
                                .padding(start = 48.dp)
                                .fillMaxWidth()
                                .background(
                                    if (task.done) c.successContainer else c.primaryContainer,
                                    RoundedCornerShape(18.dp),
                                )
                                .vidaClickable(onClick = { navController.navigate(Routes.taskRoute(task.id)) })
                                // MEDIR DÓNDE ESTÁ, para que el lienzo sepa qué
                                // tarjeta hay bajo el dedo. La posición de
                                // layout no cambia al arrastrar —el
                                // desplazamiento es solo de dibujo— así que este
                                // dato sigue siendo válido durante el gesto.
                                .onGloballyPositioned { coords ->
                                    val top = coords.positionInParent().y
                                    rowBounds[task.id] = top..(top + coords.size.height)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            Box(Modifier.width(5.dp).height(34.dp).background(tone, RoundedCornerShape(3.dp)))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = t.cardTitle.copy(fontSize = 15.sp),
                                    color = if (task.done) c.textTertiary else c.text,
                                    maxLines = 1,
                                )
                                Text(
                                    listOfNotNull(
                                        time.toString().take(5),
                                        task.location,
                                    ).joinToString(" · "),
                                    style = t.micro,
                                    color = if (task.done) c.successText else c.primaryDeep,
                                )
                            }
                        }
                    }

                    // AHORA: lo único que se mueve solo en toda la pantalla.
                    if (now.hour in firstHour..lastHour) {
                        val y = HOUR_HEIGHT * (now.hour - firstHour) + HOUR_HEIGHT * (now.minute / 60f)
                        Row(
                            modifier = Modifier.offset(y = y).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                now.toString().take(5),
                                style = t.micro.copy(fontWeight = FontWeight.ExtraBold),
                                color = c.second,
                                modifier = Modifier.width(40.dp),
                            )
                            // `.beat` del artefacto: 2,4 s, opacidad .4 → 1 y
                            // escala 1 → 1,7. Es lo ÚNICO que se mueve solo en
                            // la línea del día — por eso el ojo encuentra la
                            // hora actual sin tener que buscarla.
                            Box(Modifier.size(8.dp).vidaBeat().background(c.second, RoundedCornerShape(50)))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(c.second.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                }
                }
            }
        }

        /* ── LO QUE NO TIENE HORA ────────────────────────────────────────── */
        StaggeredAppear(3) { VidaSectionLabel("Sin hora", trailing = "${untimed.size}") }
        StaggeredAppear(4) {
            if (untimed.isEmpty()) {
                Text("Nada suelto hoy.", style = t.body, color = c.textTertiary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
                    untimed.take(8).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(c.surfaceElevated, RoundedCornerShape(50))
                                .vidaClickable(onClick = { navController.navigate(item.source.route) })
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier.size(7.dp).background(
                                    if (item.urgency == AttentionUrgency.OVERDUE) c.error else c.warning,
                                    RoundedCornerShape(50),
                                ),
                            )
                            Text(item.title, style = t.body, color = c.text, modifier = Modifier.weight(1f))
                            Text(item.source.label, style = t.micro, color = c.textTertiary)
                        }
                    }
                }
            }
        }

        /* ── LAS NOTAS DEL DÍA ───────────────────────────────────────────── */
        //
        // AQUÍ ES DONDE ESA PANTALLA SE ALCANZA. `NotasDiaScreen` existía,
        // estaba registrada en el grafo y compilada dentro del APK, pero
        // ninguna pantalla navegaba a ella: era inalcanzable. Su sitio natural
        // es este —lo que escribiste de tu día, junto a la forma de tu día— y
        // no un acceso suelto en el menú, que la habría convertido en una
        // sección más sin relación con nada.
        StaggeredAppear(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceElevated, RoundedCornerShape(50))
                    .vidaClickable(onClick = { navController.navigate(Routes.DAY_NOTES) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Notas de hoy",
                    style = t.body,
                    color = c.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (state.notes.isEmpty()) "Escribir" else "${state.notes.size}",
                    style = t.micro,
                    color = c.primary,
                )
            }
        }

        StaggeredAppear(6) {
            Text(
                "Un pago no ocurre «a las cuatro». No se le inventa una hora para meterlo en la línea.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }
    }
}

/**
 * LA HORA QUE CORRESPONDE AL PUNTO DONDE ESTÁ LA TAREA.
 *
 * Se redondea a cuartos de hora. A 38 dp por hora, un minuto mide seis décimas
 * de punto: leer el píxel literal daría horas como las 10:37 que nadie puede
 * apuntar a propósito, y el cuarto es la unidad con la que de verdad se piensa
 * una agenda.
 *
 * PASARSE DE LA FRANJA NO CANCELA EL GESTO. Antes se devolvía nulo al salirse
 * por arriba o por abajo, y la tarea volvía sola a su sitio sin explicar por
 * qué — probándolo en el teléfono pasaba a menudo, porque el dedo se va más
 * lejos de lo que uno cree. Ahora el destino se ACOTA a la primera y la última
 * hora dibujadas: soltar fuera equivale a soltar en el extremo, que es lo que
 * el usuario está señalando.
 */
private fun timeAtDrop(hoursFromMidnight: Float, firstHour: Int, lastHour: Int): LocalTime {
    val inside = hoursFromMidnight.coerceIn(firstHour.toFloat(), lastHour.toFloat())
    val quarters = Math.round(inside * 4f).coerceIn(0, 23 * 4 + 3)
    return LocalTime.of(quarters / 4, (quarters % 4) * 15)
}
