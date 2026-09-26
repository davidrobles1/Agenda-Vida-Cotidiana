package com.vidacotidiana.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.components.MoodFace
import com.vidacotidiana.app.core.ui.components.MoodScale
import com.vidacotidiana.app.core.ui.components.vidaClickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.vidacotidiana.app.core.ui.components.VidaTile
import java.time.Instant
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionItem
import com.vidacotidiana.app.core.attention.AttentionSource
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.data.WarrantyStatus
import com.vidacotidiana.app.core.ui.VidaLayout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.data.paidThisPeriod
import com.vidacotidiana.app.core.ui.SectionVisual
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.components.DayChip
import com.vidacotidiana.app.core.ui.components.LandscapeBanner
import com.vidacotidiana.app.core.ui.components.VisionBoardBackdrop
import com.vidacotidiana.app.core.ui.components.SectionEmptyState
import com.vidacotidiana.app.core.ui.components.SectionQuickAccess
import com.vidacotidiana.app.core.ui.components.SectionTile
import com.vidacotidiana.app.core.ui.components.VidaMetricRing
import com.vidacotidiana.app.core.ui.components.safeRatio
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DayRibbonItem
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.HeroCard
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.StripCard
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaProgress
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.core.ui.components.plural
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.vidacotidiana.app.core.app.loadFailed
import com.vidacotidiana.app.core.ui.components.AttentionList
import com.vidacotidiana.app.core.ui.components.moodSky
import com.vidacotidiana.app.core.ui.components.moodInk
import com.vidacotidiana.app.core.ui.components.vidaFloat
import com.vidacotidiana.app.core.ui.components.MoodChip

/**
 * INICIO RESPONDE A UNA PREGUNTA: ¿qué necesita mi atención?
 *
 * ANTES ENUMERABA MÓDULOS. De sus cinco secciones, dos eran contadores en crudo
 * («Inventario · 4 artículos», que ni viene ni requiere nada) y otra eran dos
 * enlaces fijos rotulados como «actividad reciente». La única que intentaba
 * hablar de atención —el héroe— leía `contentFor(hoy).alerts`, es decir los
 * avisos derivados de ADR-018, que solo existen en las fechas exactas de
 * antelación. Una tarea vencida ayer, un pago cuyo día pasó sin pagarse o un
 * mantenimiento atrasado no llegaban aquí: había que ir módulo por módulo a
 * comprobarlo.
 *
 * AHORA LA PANTALLA LEE `AttentionEngine`, que mira el estado real de las siete
 * fuentes con fecha que ya existían y trae lo que reclama, con el motivo por el
 * que reclama. La diferencia de percepción no está en el diseño: está en que
 * «Garantías — 5» pasa a ser «La garantía del ipad vence en 3 días».
 *
 * EL ORDEN ES LA TESIS:
 *
 *  1. HÉROE     — cuántas cosas te reclaman y de dónde salen. Cuando no hay
 *                 ninguna, lo dice con seguridad: eso también es información.
 *  2. AHORA     — lo vencido y lo de hoy, junto, sin importar de qué módulo.
 *  3. ESTA SEMANA — lo que viene en siete días.
 *  4. TU DÍA    — el cierre del día: cuánto llevas hecho.
 *  5. PRÓXIMOS 7 DÍAS — la semana en el lenguaje del calendario.
 *  6. TUS ÁREAS — acceso. Va al final porque es lo que el usuario consulta
 *                 cuando quiere, no lo que necesita saber al abrir.
 *
 * NO FILTRA POR CONTEXTO y es correcto: `loadAll` ya pide los datos con el
 * contexto activo (ADR-019), así que en Personal solo hay Personal. Portal
 * recibe todo junto porque eso es Portal.
 */
/**
 * Cuántas filas dibuja cada sección. Lo que no cabe sigue contado en el
 * antetítulo y sigue estando en su módulo: se acota lo que se PINTA, no lo que
 * se sabe.
 */
private const val MAX_ROWS = 6

/**
 * La cifra de un mosaico, o una raya cuando todavía no hay cifra que dar.
 *
 * Un cero es una afirmación —«no tienes nada»— y mientras se carga o cuando la
 * consulta falló no hay nada que afirmar. Poner «0» ahí era lo que hacía que
 * Inicio le dijera al usuario que tenía el día libre justo cuando el teléfono
 * no había podido preguntarlo.
 */
private fun figureOrDash(value: Int, loading: Boolean, failed: Boolean): String =
    if (loading || failed) "—" else value.toString()

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    // El ánimo de HOY, no la semana: en Inicio la cara es pequeña y le basta.
    // El ánimo de hoy y QUIÉN eres. Lo segundo faltaba: el saludo leía
    // `state.user` pero nadie lo pedía desde aquí —solo Portal—, así que en
    // Inicio siempre era nulo y el saludo se quedaba en «¡Hola!».
    LaunchedEffect(Unit) {
        viewModel.loadMood()
        viewModel.loadUser()
    }

    val attention = AttentionEngine.scan(state.data, viewModel.allTasks(), today)
    val now = AttentionEngine.now(attention)
    val soon = AttentionEngine.soon(attention)
    val forToday = AttentionEngine.today(attention)
    val overdue = AttentionEngine.overdue(attention)

    // LOS TRES ESTADOS QUE INICIO NO DISTINGUÍA.
    //
    // Esta pantalla no consultaba `loading` ni el resultado de la carga, así
    // que pintaba ceros con total confianza tanto mientras cargaba como
    // cuando no había podido hablar con el servidor: «Nada te reclama» era la
    // misma frase para «no tienes nada» y para «no lo sé». Como lee las siete
    // fuentes a la vez, le basta con que falle una para no poder responder.
    val firstLoad = state.loading && state.data.collections.all { it.isEmpty() }
    val cannotAnswer = state.loadFailed && attention.isEmpty()

    val todayTasks = viewModel.contentFor(today).tasks
    val doneCount = todayTasks.count { it.done }

    VidaScreen(
        // El saludo era «¡Hola!» a secas aunque la sesión ya conocía al
        // usuario: `/api/v1/me` se pedía y se guardaba, y nadie lo leía. No se
        // inventa una segunda fuente ni se guarda el nombre aparte — se usa la
        // que ya hay, y si todavía no ha llegado el saludo es el de siempre.
        title = state.user?.username?.takeIf { it.isNotBlank() }
            ?.let { "¡Hola, $it!" } ?: "¡Hola!",
        subtitle = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            .replaceFirstChar { it.uppercase() } + " ${today.dayOfMonth} de " +
            today.month.getDisplayName(TextStyle.FULL, Locale("es", "MX")),
        onNavigationClick = openDrawerAction(drawerState, scope),
        onRefresh = viewModel::refresh,
        refreshing = state.loading,
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
        actions = {
            // EL PUNTO DICE LA VERDAD O NO ESTÁ.
            //
            // Estaba fijado a `true`: un aviso permanente que nunca se apagaba
            // por mucho que el usuario leyera, y que por eso dejaba de
            // significar nada. Ahora sale del mismo conjunto que pinta la
            // pantalla de avisos, menos lo ya leído.
            val sinLeer = attention.count { it.noticeKey !in state.readNotices }
            VidaIconButton(
                Icons.Outlined.Notifications,
                if (sinLeer == 0) "Avisos" else "Avisos · $sinLeer sin leer",
                badge = sinLeer > 0,
            ) { onNavigate(Routes.NOTIFICATIONS) }
            VidaIconButton(Icons.Outlined.Settings, "Ajustes") { onNavigate(Routes.SETTINGS) }
        },
    ) {
        /* ══════════════════════════════════════════════════════════════════
           1 — LA BANDA DEL ENCABEZADO
           ══════════════════════════════════════════════════════════════════

           «Ilustración principal (encabezado del home)» de la lámina: el
           amanecer de montaña con la frase que da el tono de la aplicación.

           SUSTITUYE A LA RETÍCULA DE CUATRO CIFRAS. No se pierde ninguna:
           «Para hoy» y «Atrasado» son ahora el bloque «Hoy», y «En total» y
           «Hechas» viven en los widgets contextuales, que es donde la
           propuesta los coloca. Lo que cambia es la entrada a la pantalla —
           antes se abría con cuatro números y ahora con una frase, que es
           exactamente lo que la propuesta pide: «menos dashboard y más vida
           real».

           El paisaje se dibuja (ver `LandscapeBanner`): ni una fotografía en
           el instalable.
        */
        StaggeredAppear(0) {
            LandscapeBanner()
        }

        // 2 — «¿Cómo te sientes hoy?» (V36).
        //
        // SIGUE EN SU SITIO, el segundo, y con el mismo aspecto: es petición
        // expresa. En la propuesta no aparece —su lámina no lo contempla—
        // pero la sección existe y su lugar en la lectura de la pantalla no
        // cambia: después de saber cómo viene el día y antes de ponerse a
        // resolverlo.
        StaggeredAppear(1) {
            val marked = state.moodToday?.value
            // El fondo lo decide el TEMA, no una tabla de cremas fijos: con
            // el color literal, en Noche esta tarjeta se quedaba clara bajo
            // un texto que sí había cambiado a claro.
            val sky = moodSky(marked)
            val deep = moodInk(marked)
            // EL MISMO ENVOLTORIO QUE EL RESTO DEL INICIO: radio 22 y un filete
            // de un dp. Antes era un bloque de color a 26 dp sin borde, y sobre
            // una pantalla que ya usa tarjetas con filete era lo único que se
            // leía como de otro sitio. El tinte sigue siendo el del ánimo.
            val edge = marked?.let { MoodScale.accentOf(it).copy(alpha = 0.22f) } ?: c.line
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sky, RoundedCornerShape(22.dp))
                    .border(1.dp, edge, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Box(Modifier.vidaClickable(onClick = { onNavigate(Routes.WELLBEING) })) {
                        MoodFace(
                            value = marked, size = 50.dp, strokeWidth = 4f, surface = c.surfaceVariant,
                            modifier = Modifier.vidaFloat(),
                        )
                    }
                    Column(
                        Modifier.weight(1f).vidaClickable(onClick = { onNavigate(Routes.WELLBEING) }),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("¿Cómo te sientes hoy?", style = t.cardTitle, color = c.text)
                        Text(
                            when {
                                marked != null -> MoodScale.ECHOES[marked]
                                state.moodError != null -> "No pudimos consultarlo"
                                state.moodLoading -> "Un momento…"
                                else -> "Sin marcar"
                            },
                            style = t.caption,
                            color = deep,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(12.dp))
                        Text("Solo tú", style = t.micro, color = c.textTertiary)
                    }
                }
                // LOS CINCO, EN EL FORMATO DE LA LÁMINA: disco teñido y rótulo
                // debajo. Eran cuadrados blancos con una carita de 24 dp y sin
                // nombre — a ese tamaño «normal» y «regular» se confunden, y
                // había que tocar para saber cuál era cuál.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..4).forEach { k ->
                        MoodChip(
                            value = k,
                            selected = marked == k,
                            onClick = { viewModel.setMood(k) },
                            modifier = Modifier.weight(1f),
                            size = 44.dp,
                        )
                    }
                }
            }
        }

        /* 3 — ACCESOS RÁPIDOS
           «Tus módulos al alcance de un toque. Con iconos y contadores de
           actividad cuando sea necesario.»

           El contador sale del MISMO motor que alimenta el resto de la
           pantalla, no de un recuento propio: lo que reclama de esa sección.
           Un cero no se dibuja, que es lo que la propuesta pide con «cuando
           sea necesario». */
        StaggeredAppear(2) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionQuickAccess(
                    VidaSections.Tareas,
                    { onNavigate(Routes.TASKS) },
                    badge = attention.count { it.source == AttentionSource.TASK },
                )
                SectionQuickAccess(
                    VidaSections.Pagos,
                    { onNavigate(Routes.PAYMENTS) },
                    badge = attention.count { it.source == AttentionSource.PAYMENT },
                )
                SectionQuickAccess(VidaSections.Documentos, { onNavigate(Routes.DOCUMENTS) })
                SectionQuickAccess(VidaSections.Inventario, { onNavigate(Routes.INVENTORY) })
                SectionQuickAccess(
                    VidaSections.Compartidos,
                    { onNavigate(Routes.SHARED) },
                    badge = attention.count { it.source == AttentionSource.SHARED },
                )
            }
        }

        /* 4 — RESUMEN DE HOY
           «Muestra tus tareas más importantes del día con prioridad, hora y
           categoría.»

           Lleva lo que reclama HOY de las siete fuentes, no solo tareas: es
           el conjunto que antes vivía en «Ahora» y «Esta semana», con la
           presentación de la propuesta. Sin él se perdería la única lectura
           de la aplicación que cruza módulos. */
        StaggeredAppear(3) {
            val hoy = (now + soon).distinctBy { it.id }
            VidaCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append("Hoy")
                            if (hoy.isNotEmpty()) append(" · ").append(plural(hoy.size, "pendiente", "pendientes"))
                        },
                        style = t.cardTitle, color = c.text, modifier = Modifier.weight(1f),
                    )
                    if (hoy.isNotEmpty()) {
                        Row(
                            Modifier.vidaClickable(onClick = { onNavigate(Routes.DAY) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Ver todas", style = t.caption, color = c.primary)
                            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = c.primary)
                        }
                    }
                }
                when {
                    firstLoad -> Text("Comprobando tu día…", style = t.caption, color = c.textSecondary)
                    cannotAnswer -> Text("No pudimos consultarlo", style = t.caption, color = c.textSecondary)
                    hoy.isEmpty() -> SectionEmptyState(
                        section = VidaSections.Tareas,
                        title = "Semana despejada",
                        body = "No hay nada que te reclame. Disfruta tu día.",
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        hoy.take(MAX_ROWS).forEach { item -> TodayRow(item, onNavigate) }
                    }
                }
            }
        }

        /* 5 — WIDGETS CONTEXTUALES
           «Muestran datos clave y relevantes de cada área. Sin ser un
           dashboard, se integran de forma natural en el flujo.» */
        StaggeredAppear(4) {
            val pendientesHoy = todayTasks.count { !it.done }
            val esteMes = state.data.payments.filter {
                it.renewsOn.month == today.month && it.renewsOn.year == today.year
            }
            val totalMes = esteMes.sumOf { it.amount ?: 0.0 }
            // La regla de «ya pagado» NO se reescribe aquí: es la que la Web
            // fijó y el repositorio ya expone (`paidThisPeriod`). Dos
            // versiones de la misma regla acabarían discrepando.
            val yaPagados = state.data.paymentRecords.paidThisPeriod(today)
            val pagado = esteMes.filter { it.id in yaPagados }.sumOf { it.amount ?: 0.0 }
            val proximoMant = state.data.maintenance.minByOrNull { it.nextDueOn }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContextWidget(
                    section = VidaSections.Calendario,
                    title = "Agenda",
                    kicker = "Hoy",
                    figure = pendientesHoy.toString(),
                    caption = plural(pendientesHoy, "tarea", "tareas"),
                    onClick = { onNavigate(Routes.CALENDAR) },
                    modifier = Modifier.weight(1f),
                )
                ContextWidget(
                    section = VidaSections.Pagos,
                    title = "Pagos",
                    kicker = "Este mes",
                    figure = money(pagado),
                    caption = "de ${money(totalMes)}",
                    onClick = { onNavigate(Routes.PAYMENTS) },
                    modifier = Modifier.weight(1f),
                    ring = safeRatio(pagado, totalMes).takeIf { totalMes > 0.0 },
                )
                ContextWidget(
                    section = VidaSections.Mantenimiento,
                    title = "Mantenimiento",
                    kicker = "Próximo",
                    figure = proximoMant?.nextDueOn?.format(DAY_SHORT) ?: "—",
                    caption = proximoMant?.item ?: "Nada programado",
                    onClick = { onNavigate(Routes.MAINTENANCE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        /* 6 — CALENDARIO INTELIGENTE
           «Muestra tus próximos días con indicadores de tareas, eventos,
           pagos o recordatorios.» El punto de cada día se tiñe con el color
           de la sección que lo ocupa. */
        StaggeredAppear(5) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Próximos 7 días", style = t.cardTitle, color = c.text, modifier = Modifier.weight(1f))
                Row(
                    Modifier.vidaClickable(onClick = { onNavigate(Routes.CALENDAR) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ver calendario", style = t.caption, color = c.primary)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = c.primary)
                }
            }
        }
        StaggeredAppear(6) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0..6).map { today.plusDays(it.toLong()) }.forEach { date ->
                    val delDia = attention.filter { it.date == date }
                    DayChip(
                        weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX"))
                            .replaceFirstChar { it.uppercase() }.take(3),
                        day = date.dayOfMonth,
                        isToday = date == today,
                        dotColor = delDia.firstOrNull()?.let { sectionFor(it.source).tint },
                        onClick = { viewModel.selectDate(date); onNavigate(Routes.CALENDAR) },
                    )
                }
            }
        }

        /* 7 — FAMILIA Y VISION BOARD
           «Acceso rápido a tu familia/usuarios compartidos y a tu Vision
           Board.» */
        StaggeredAppear(7) {
            // ALTURA FIJA Y COMPARTIDA. En la propuesta las dos tarjetas miden
            // lo mismo, y no por casualidad: la de Vision Board necesita una
            // altura conocida para repartirla entre el texto y el paisaje. Con
            // alturas naturales cada una acababa a una altura distinta y el
            // paisaje se estiraba o se aplastaba según cuántos miembros
            // tuviera la familia de al lado.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FamilyCard(
                    members = state.data.familyMembers.size,
                    onOpen = { onNavigate(Routes.FAMILY) },
                    modifier = Modifier.weight(1f).height(120.dp),
                )
                VisionBoardCard(
                    onOpen = { onNavigate(Routes.BOARD) },
                    modifier = Modifier.weight(1f).height(120.dp),
                )
            }
        }
    }
}

/** El color de la sección de la que viene cada cosa que reclama. */
private fun sectionFor(source: AttentionSource): SectionVisual = when (source) {
    AttentionSource.TASK -> VidaSections.Tareas
    AttentionSource.PAYMENT -> VidaSections.Pagos
    AttentionSource.WARRANTY -> VidaSections.Garantias
    AttentionSource.MAINTENANCE -> VidaSections.Mantenimiento
    AttentionSource.SHARED -> VidaSections.Compartidos
    AttentionSource.COMMITMENT, AttentionSource.ROUTINE -> VidaSections.Calendario
}

/** Importe corto, como la propuesta: «$3,480», sin decimales que no aportan. */
private fun money(value: Double): String =
    "$" + NumberFormat.getIntegerInstance(Locale("es", "MX")).format(value.toLong())

private val DAY_SHORT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale("es", "MX"))

/**
 * Una fila del resumen de hoy.
 *
 * La composición es la de la propuesta: círculo de estado, título, «Hoy ·
 * hora» y la píldora de categoría. El color del círculo y de la píldora
 * salen de la sección, que es lo que permite reconocer de dónde viene cada
 * línea sin leer la etiqueta.
 */
@Composable
private fun TodayRow(item: AttentionItem, onNavigate: (String) -> Unit) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val section = sectionFor(item.source)
    Row(
        Modifier
            .fillMaxWidth()
            .vidaClickable(onClick = { onNavigate(item.source.route) })
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // El círculo vacío de «Pendiente» de la lámina de estados.
        Box(
            Modifier.size(22.dp).border(2.dp, section.tint.copy(alpha = 0.55f), CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.title, style = t.cardTitle.copy(fontSize = 15.sp), color = c.text, maxLines = 1)
            Text(item.reason, style = t.micro, color = c.textSecondary, maxLines = 1)
        }
        Box(
            Modifier
                .background(section.container, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(item.source.label, style = t.micro, color = section.tint)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = c.textTertiary)
    }
}

/**
 * Un widget contextual: cabecera con su icono, antetítulo, cifra y leyenda.
 *
 * El anillo es opcional porque solo Pagos tiene una proporción que enseñar —
 * la propuesta lo dibuja solo ahí, y un anillo al 0 % en los otros dos
 * sería un adorno que miente.
 */
@Composable
private fun ContextWidget(
    section: SectionVisual,
    title: String,
    kicker: String,
    figure: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ring: Float? = null,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Column(
        modifier
            .background(section.wash, RoundedCornerShape(22.dp))
            .border(1.dp, section.tint.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .vidaClickable(onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // La ilustración de la lámina, a tamaño de cabecera. La lámina
            // las pone así en «WIDGETS Y TARJETAS»: el dibujo del módulo
            // junto a su nombre, no un icono de trazo teñido.
            SectionTile(section, size = 19.dp)
            // `Ellipsis` explícito y tamaño propio: «Mantenimiento» no cabe en
            // un tercio de pantalla y se cortaba a media palabra sin puntos.
            Text(
                title,
                style = t.micro.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                color = section.tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(kicker, style = t.micro, color = c.textTertiary)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    figure,
                    style = t.cardTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = c.text,
                    maxLines = 1,
                )
                Text(caption, style = t.micro, color = c.textTertiary, maxLines = 1)
            }
            if (ring != null) {
                VidaMetricRing(
                    progress = ring,
                    figure = "${(ring * 100).toInt()}%",
                    tint = section.tint,
                    size = 38.dp,
                    stroke = 5.dp,
                )
            }
        }
    }
}

/** «Familia — N miembros», con sus avatares y el «+» de la lámina. */
@Composable
private fun FamilyCard(members: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val section = VidaSections.Familia
    Column(
        modifier
            .background(section.wash, RoundedCornerShape(22.dp))
            .border(1.dp, section.tint.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .vidaClickable(onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTile(section, size = 24.dp)
            Text("Familia", style = t.cardTitle.copy(fontSize = 15.sp), color = c.text)
        }
        Text(plural(members, "miembro", "miembros"), style = t.micro, color = c.textTertiary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            // Los avatares son iniciales y no fotografías: la aplicación no
            // guarda foto de perfil, y un hueco gris diría menos que esto.
            repeat(minOf(members, 3)) { i ->
                SectionTile(section, size = 30.dp)
            }
            Box(
                Modifier
                    .size(30.dp)
                    .background(c.surface, CircleShape)
                    .border(1.dp, section.tint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, "Añadir a la familia", Modifier.size(15.dp), tint = section.tint)
            }
        }
    }
}

/**
 * «Vision Board — Tus metas, en camino».
 *
 * LA ILUSTRACIÓN ES EL FONDO. No es una estampa colocada debajo del texto con
 * su margen alrededor: el amanecer llena la tarjeta —a sangre por los lados y
 * por abajo— y el título y la frase van ENCIMA, en la franja alta donde la
 * imagen es sólo cielo. Así las letras nunca se plantan sobre las montañas,
 * que es lo que la propuesta hace y lo que distingue esta tarjeta de las
 * demás del inicio.
 *
 * Por eso es un `Box` y no una `Column`: una columna apila, y apilar era
 * justamente el error —dejaba la imagen como el último elemento de una lista
 * en vez de como el lienzo sobre el que se escribe.
 *
 * El recorte de la tarjeta se hace ANTES del fondo para que el paisaje quede
 * cortado por la esquina redondeada; si se recortara después, la imagen se
 * saldría por los picos.
 */
@Composable
private fun VisionBoardCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val t = VidaTheme.type
    val shape = RoundedCornerShape(22.dp)
    // LOS TONOS SON LOS DE LA PROPUESTA, medidos sobre ella, y son literales
    // en vez de venir del tema por dos razones:
    //
    //  · El texto se apoya en una ilustración clara. El gris del tema oscuro
    //    desaparecería sobre el cielo del amanecer.
    //  · La identidad de la sección —el verde azulado de `VidaSections`— es la
    //    de su icono y su pestaña, no la de esta tarjeta. Aquí manda el
    //    paisaje, y un borde verde sobre un amanecer lavanda desentonaba.
    val title = Color(0xFF23206B)
    val subtitle = Color(0xFF7C7392)
    val edge = Color(0xFF8B7BD8)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, edge.copy(alpha = 0.22f), shape)
            .vidaClickable(onClick = onOpen),
    ) {
        VisionBoardBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxWidth().padding(start = 13.dp, top = 12.dp, end = 13.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                // LA BOMBILLA, no la baldosa verde del resto de la aplicación:
                // es lo que la propuesta dibuja en ESTA tarjeta. Va en vector y
                // no recortada de la lámina porque allí mide 34 px —ampliarla
                // se veía emborronada, y es un icono de trazo, no un dibujo con
                // volumen que haya que conservar tal cual.
                Box(
                    Modifier.size(26.dp).background(Color(0xFFE6DEF8), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFF3B22CC),
                        modifier = Modifier.size(15.dp),
                    )
                }
                Text("Vision Board", style = t.cardTitle.copy(fontSize = 14.sp), color = title)
            }
            Text("Tus metas, en camino", style = t.micro, color = subtitle)
        }
        // La flecha va SOBRE el bosque, sin pastilla detrás: ahí la imagen es
        // lo bastante oscura para que el blanco se lea solo, y una pastilla
        // habría tapado justo la esquina que la propuesta deja ver.
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(20.dp),
        )
    }
}

/**
 * La lista de cosas que reclaman.
 *
 * Usa `ResourceBoard` como el resto de la aplicación, y cae en su composición de
 * FILA porque ninguna entrada trae cifra destacada. Es deliberado: el importe de
 * un pago va dentro del motivo («Se paga hoy · $349 MXN») y no como dato héroe,
 * porque aquí lo que importa no es cuánto es sino que te toca. Pasarlo como
 * `amount` convertiría la lista en una retícula de cuadrados y mezclaría siete
 * módulos en piezas que compiten entre sí.
 *
 * La píldora dice de qué módulo viene cada cosa. Sin ella, siete fuentes juntas
 * se leerían como una lista plana y se perdería justo lo que hace valiosa a la
 * sección: que la aplicación miró en todas partes.
 */
