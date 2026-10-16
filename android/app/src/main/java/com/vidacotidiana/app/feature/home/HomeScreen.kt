package com.vidacotidiana.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.loadFailed
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionItem
import com.vidacotidiana.app.core.attention.AttentionSource
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.data.paidThisPeriod
import com.vidacotidiana.app.core.ui.SectionVisual
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.brand.ButterflyMark
import com.vidacotidiana.app.core.ui.components.MoodScale
import com.vidacotidiana.app.core.ui.components.SectionTile
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaMoodCard
import com.vidacotidiana.app.core.ui.components.vidaBrandInk
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaTintedBar
import com.vidacotidiana.app.core.ui.components.VisionBoardBackdrop
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.core.ui.components.plural
import com.vidacotidiana.app.core.ui.components.safeRatio
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.core.ui.components.vidaFloat
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/**
 * INICIO RESPONDE A UNA PREGUNTA: ¿qué necesita mi atención?
 *
 * PROPUESTA 2 · «INICIO EN CINCO ZONAS» (orden A, aprobada por el PO el
 * 2026-09-27). La pantalla anterior apilaba ocho bloques del mismo peso, cada
 * uno con su propio fondo —foto, cuatro tintes de sección, baldosas, paisaje—,
 * repetía Pagos tres veces y abría Calendario desde cuatro sitios. Ahora se lee
 * de arriba abajo en cinco zonas, cada una con UNA pregunta:
 *
 *  1. TU DÍA        — ¿cómo va mi día? El icono hecho pantalla: su degradado,
 *                     sus facetas y su mariposa. Saludo, anillo de hechas y
 *                     cuántas cosas reclaman.
 *  2. ÁNIMO         — ¿cómo me siento? Sigue segundo: es petición expresa.
 *  3. HOY           — ¿qué me reclama? Una lista; la baldosa dice el módulo y
 *                     a la derecha el CUÁNDO. El rojo, solo lo atrasado.
 *  4. ESTA SEMANA   — ¿qué viene? Días, pagos del mes y próximo mantenimiento
 *                     en una sola tarjeta (antes: tres widgets y una tira).
 *  5. TUS ESPACIOS  — ¿a dónde voy? Retícula 4 × 2 de baldosas iguales y
 *                     Vision Board al cierre.
 *
 * DOS CONTENEDORES Y NADA MÁS: el héroe del icono y `VidaCard`. El color de
 * sección vive solo dentro de su baldosa.
 *
 * NO AÑADE DATOS: todo lo que pinta ya lo leía la pantalla anterior.
 *
 * NO FILTRA POR CONTEXTO y es correcto: `loadAll` ya pide los datos con el
 * contexto activo (ADR-019).
 */

/**
 * Cuántas filas dibuja «Hoy». Lo que no cabe sigue contado en el título de la
 * zona y sigue en su módulo: se acota lo que se PINTA, no lo que se sabe.
 */
private const val MAX_ROWS = 6

/** Aire entre zonas y entre el título de una zona y su contenido. */
private val ZONE_GAP = 28.dp
private val ZONE_INNER = 12.dp

/** Los colores del icono (ADR-027), para el héroe. */
private val IconDeep = Color(0xFF1E1B4B)
private val IconMid = Color(0xFF2F2E7A)
private val IconLight = Color(0xFF4A4DA0)
private val IconHalo = Color(0xFFCFEAFB)
private val IconSky = Color(0xFF7DC2ED)

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    // El ánimo de hoy y QUIÉN eres: el saludo del héroe lee `state.user`.
    LaunchedEffect(Unit) {
        viewModel.loadMood()
        viewModel.loadUser()
    }

    val attention = AttentionEngine.scan(state.data, viewModel.allTasks(), today)
    val now = AttentionEngine.now(attention)
    val soon = AttentionEngine.soon(attention)

    // Cargando o sin respuesta no es «nada te reclama»: un cero es una
    // afirmación, y sin datos no hay nada que afirmar.
    val firstLoad = state.loading && state.data.collections.all { it.isEmpty() }
    val cannotAnswer = state.loadFailed && attention.isEmpty()

    val todayTasks = viewModel.contentFor(today).tasks
    val doneCount = todayTasks.count { it.done }
    val hoy = (now + soon).distinctBy { it.id }

    VidaScreen(
        title = "Inicio",
        brandMark = true,
        onNavigationClick = openDrawerAction(drawerState, scope),
        onRefresh = viewModel::refresh,
        refreshing = state.loading,
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
        actions = {
            // El punto dice la verdad o no está: sale del mismo conjunto que
            // pinta la pantalla de avisos, menos lo ya leído.
            val sinLeer = attention.count { it.noticeKey !in state.readNotices }
            VidaIconButton(
                Icons.Outlined.Notifications,
                if (sinLeer == 0) "Avisos" else "Avisos · $sinLeer sin leer",
                badge = sinLeer > 0,
            ) { onNavigate(Routes.NOTIFICATIONS) }
            VidaIconButton(Icons.Outlined.Settings, "Ajustes") { onNavigate(Routes.SETTINGS) }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ZONE_GAP)) {
            // 1 — TU DÍA
            StaggeredAppear(0) {
                DayHero(
                    name = state.user?.username?.takeIf { it.isNotBlank() },
                    today = today,
                    now = now,
                    soonCount = soon.size,
                    done = doneCount,
                    total = todayTasks.size,
                    firstLoad = firstLoad,
                    cannotAnswer = cannotAnswer,
                    onOpen = { onNavigate(Routes.DAY) },
                )
            }

            // 2 — ÁNIMO (petición expresa: segundo lugar)
            StaggeredAppear(1) {
                MoodZone(
                    name = state.user?.username?.takeIf { it.isNotBlank() },
                    marked = state.moodToday?.value,
                    loading = state.moodLoading,
                    failed = state.moodError != null,
                    onMark = viewModel::setMood,
                    onOpen = { onNavigate(Routes.WELLBEING) },
                )
            }

            // 3 — HOY
            StaggeredAppear(2) {
                Zone(
                    title = "Hoy · ${if (firstLoad || cannotAnswer) "—" else hoy.size}",
                    link = "Ver todo".takeIf { hoy.isNotEmpty() },
                    onLink = { onNavigate(Routes.DAY) },
                ) {
                    ZoneCard {
                        when {
                            firstLoad -> ZoneRow(
                                tile = { SectionTile(VidaSections.Tareas, size = 32.dp) },
                                title = "Comprobando tu día…",
                                detail = { RowDetail("Un momento") },
                                trailing = { WhenFigure("—") },
                            )
                            cannotAnswer -> ZoneRow(
                                tile = { SectionTile(VidaSections.Tareas, size = 32.dp) },
                                title = "No pudimos consultarlo",
                                detail = { RowDetail("Desliza hacia abajo para reintentar") },
                                trailing = { WhenFigure("—") },
                            )
                            // El día despejado es UNA FILA MÁS, no un bloque de otro tamaño.
                            hoy.isEmpty() -> ZoneRow(
                                tile = { SectionTile(VidaSections.Tareas, size = 32.dp) },
                                title = "Semana despejada",
                                detail = { RowDetail("Nada te reclama. Disfruta tu día.") },
                                trailing = { WhenFigure("0", "pendientes") },
                            )
                            else -> hoy.take(MAX_ROWS).forEachIndexed { i, item ->
                                if (i > 0) Divider()
                                AttentionRow(item, today) { onNavigate(Routes.detailRoute(item)) }
                            }
                        }
                    }
                }
            }

            // 4 — ESTA SEMANA
            StaggeredAppear(3) {
                val esteMes = state.data.payments.filter {
                    it.renewsOn.month == today.month && it.renewsOn.year == today.year
                }
                val totalMes = esteMes.sumOf { it.amount ?: 0.0 }
                // La regla de «ya pagado» es la del repositorio, no una copia.
                val yaPagados = state.data.paymentRecords.paidThisPeriod(today)
                val pagado = esteMes.filter { it.id in yaPagados }.sumOf { it.amount ?: 0.0 }
                val proximoMant = state.data.maintenance.minByOrNull { it.nextDueOn }

                Zone(title = "Esta semana", link = "Calendario", onLink = { onNavigate(Routes.CALENDAR) }) {
                    ZoneCard {
                        WeekStrip(today, attention) { date ->
                            viewModel.selectDate(date)
                            onNavigate(Routes.CALENDAR)
                        }
                        Divider()
                        ZoneRow(
                            tile = { SectionTile(VidaSections.Pagos, size = 32.dp) },
                            title = "Pagos del mes",
                            onClick = { onNavigate(Routes.PAYMENTS) },
                            detail = {
                                if (totalMes > 0.0) {
                                    VidaTintedBar(
                                        progress = safeRatio(pagado, totalMes),
                                        tint = brandInk(),
                                        height = 6.dp,
                                        modifier = Modifier.padding(top = 7.dp),
                                    )
                                } else {
                                    RowDetail("Nada que pagar este mes")
                                }
                            },
                            trailing = {
                                if (totalMes > 0.0) WhenFigure(money(pagado), "de ${money(totalMes)}") else WhenFigure("—")
                            },
                        )
                        Divider()
                        ZoneRow(
                            tile = { SectionTile(VidaSections.Mantenimiento, size = 32.dp) },
                            title = "Próximo mantenimiento",
                            onClick = { onNavigate(Routes.MAINTENANCE) },
                            detail = { RowDetail(proximoMant?.item ?: "Nada programado") },
                            trailing = {
                                val d = proximoMant?.nextDueOn
                                WhenFigure(
                                    d?.format(DAY_SHORT)?.trimEnd('.') ?: "—",
                                    d?.dayOfWeek?.getDisplayName(DateTextStyle.FULL, ES_MX),
                                )
                            },
                        )
                    }
                }
            }

            // 5 — TUS ESPACIOS
            StaggeredAppear(4) {
                Zone(title = "Tus espacios") {
                    VidaCard(padding = 0.dp) {
                        SpacesGrid(
                            spaces = listOf(
                                Space(VidaSections.Tareas, Routes.TASKS, attention.count { it.source == AttentionSource.TASK }),
                                Space(VidaSections.Pagos, Routes.PAYMENTS, attention.count { it.source == AttentionSource.PAYMENT }),
                                Space(VidaSections.Documentos, Routes.DOCUMENTS),
                                Space(VidaSections.Inventario, Routes.INVENTORY),
                                Space(VidaSections.Compartidos, Routes.SHARED, attention.count { it.source == AttentionSource.SHARED }),
                                Space(VidaSections.Calendario, Routes.CALENDAR),
                                Space(VidaSections.Familia, Routes.FAMILY),
                                Space(VidaSections.Mantenimiento, Routes.MAINTENANCE, attention.count { it.source == AttentionSource.MAINTENANCE }),
                            ),
                            onNavigate = onNavigate,
                        )
                    }
                    VisionBoardCard(
                        onOpen = { onNavigate(Routes.BOARD) },
                        modifier = Modifier.fillMaxWidth().height(112.dp),
                    )
                }
            }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   PIEZAS COMUNES
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * El azul tinta de la marca: estructura, día de hoy, contadores y barra del
 * mes. En Noche se aclara para no hundirse en el fondo.
 */
@Composable
private fun brandInk(): Color = vidaBrandInk()

/** Una zona: título en versalitas, enlace opcional y su contenido. */
@Composable
private fun Zone(
    title: String,
    link: String? = null,
    onLink: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = VidaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ZONE_INNER)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title.uppercase(),
                style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em),
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            if (link != null) {
                Text(
                    link,
                    style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.primary,
                    modifier = Modifier.vidaClickable(onClick = onLink).padding(vertical = 4.dp),
                )
            }
            trailing?.invoke()
        }
        content()
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(VidaTheme.colors.line))
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

/** Importe corto: «$3,480», sin decimales que no aportan. */
private fun money(value: Double): String =
    "$" + NumberFormat.getIntegerInstance(Locale("es", "MX")).format(value.toLong())

private val ES_MX = Locale("es", "MX")
private val DAY_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", ES_MX)

private fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, ES_MX).replaceFirstChar { it.uppercase() }.take(3)

/* ══════════════════════════════════════════════════════════════════════════
   1 · TU DÍA — el icono hecho pantalla
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Une en un bloque lo que antes eran tres —saludo en la barra, banner con foto
 * y widget de Agenda—. El fondo es el del icono (degradado radial y facetas) y
 * la mariposa es la misma de ADR-027, así que abrir la app es entrar en él.
 *
 * Los colores son literales a propósito: es un momento de MARCA y se ve igual
 * en Claro y en Noche, como el propio icono.
 */
@Composable
private fun DayHero(
    name: String?,
    today: LocalDate,
    now: List<AttentionItem>,
    soonCount: Int,
    done: Int,
    total: Int,
    firstLoad: Boolean,
    cannotAnswer: Boolean,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val sora = VidaFonts.Sora
    val fecha = today.dayOfWeek.getDisplayName(DateTextStyle.FULL, ES_MX).replaceFirstChar { it.uppercase() } +
        " · ${today.dayOfMonth} " + today.month.getDisplayName(DateTextStyle.SHORT, ES_MX).trimEnd('.')
    val clear = !firstLoad && !cannotAnswer && now.isEmpty()

    Box(Modifier.fillMaxWidth().clip(shape).vidaClickable(onClick = onOpen)) {
        HeroBackdrop(Modifier.matchParentSize())
        ButterflyMark(
            size = 78.dp,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp).vidaFloat(),
        )
        Column(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.padding(end = 84.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    fecha.uppercase(),
                    style = TextStyle(fontFamily = sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em),
                    color = Color(0xFFBFD6F2),
                )
                Text(
                    name?.let { "¡Hola, $it!" } ?: "¡Hola!",
                    style = TextStyle(fontFamily = VidaFonts.FrauncesWordmark, fontWeight = FontWeight.Light, fontSize = 27.sp, lineHeight = 30.sp),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (clear) {
                // La frase de marca que antes estaba horneada en la foto del banner.
                Text(
                    "No tienes que acordarte de todo, estoy aquí para ayudarte.",
                    style = TextStyle(fontFamily = VidaFonts.FrauncesWordmark, fontWeight = FontWeight.Light, fontSize = 17.sp, lineHeight = 22.sp),
                    color = Color(0xFFE6EEFA),
                    modifier = Modifier.padding(end = 40.dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(IconHalo.copy(alpha = 0.18f)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (total > 0) HeroRing(done, total)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val (head, sub) = when {
                        firstLoad -> "Comprobando tu día…" to "Un momento"
                        cannotAnswer -> "No pudimos consultarlo" to "Desliza hacia abajo para reintentar"
                        now.isNotEmpty() -> (plural(now.size, "cosa te reclama", "cosas te reclaman") + " hoy") to breakdown(now)
                        soonCount > 0 -> "Nada urgente hoy" to plural(soonCount, "pendiente", "pendientes") + " esta semana"
                        else -> "Nada te reclama hoy" to "Tu vida, en un solo lugar."
                    }
                    Text(head, style = TextStyle(fontFamily = sora, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp), color = Color.White)
                    Text(sub, style = VidaTheme.type.micro.copy(fontSize = 12.sp), color = Color(0xFFC9D8EE), maxLines = 2)
                }
            }
            if (!clear && !firstLoad && !cannotAnswer) {
                Row(
                    Modifier
                        .background(IconHalo, RoundedCornerShape(999.dp))
                        .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Ver mi día", style = TextStyle(fontFamily = sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = IconDeep)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = IconDeep)
                }
            }
        }
    }
}

/** «1 atrasada · 1 pago · 1 tarea»: de dónde sale lo que reclama. */
private fun breakdown(items: List<AttentionItem>): String {
    val late = items.count { it.urgency == AttentionUrgency.OVERDUE }
    val bySource = items.filter { it.urgency != AttentionUrgency.OVERDUE }
        .groupingBy { it.source }.eachCount()
        .entries.sortedByDescending { it.value }
        .map { (s, n) -> plural(n, s.label.lowercase(), s.plural) }
    return (listOfNotNull(if (late > 0) plural(late, "atrasada", "atrasadas") else null) + bySource)
        .take(3).joinToString(" · ")
}

/** Degradado radial del icono y una textura de facetas con su paleta. */
@Composable
private fun HeroBackdrop(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                0f to IconLight, 0.48f to IconMid, 1f to IconDeep,
                center = Offset(size.width * 0.85f, 0f),
                radius = size.maxDimension * 1.1f,
            ),
        )
        // Retícula 7 × 5 con los vértices interiores desplazados. La semilla es
        // fija: las facetas no bailan entre recomposiciones.
        val cols = 7
        val rows = 5
        var seed = 7
        fun rnd(): Float { seed = (seed * 9301 + 49297) % 233280; return seed / 233280f }
        val jitter = size.width / 12f
        val pts = Array((rows + 1) * (cols + 1)) { Offset.Zero }
        for (r in 0..rows) for (col in 0..cols) {
            val inner = r in 1 until rows && col in 1 until cols
            val j = if (inner) 1f else 0f
            pts[r * (cols + 1) + col] = Offset(
                col * size.width / cols + (rnd() - 0.5f) * jitter * j,
                r * size.height / rows + (rnd() - 0.5f) * jitter * j,
            )
        }
        fun tri(a: Offset, b: Offset, c: Offset, color: Color) {
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, color)
        }
        for (r in 0 until rows) for (col in 0 until cols) {
            val a = pts[r * (cols + 1) + col]
            val b = pts[r * (cols + 1) + col + 1]
            val d = pts[(r + 1) * (cols + 1) + col]
            val e = pts[(r + 1) * (cols + 1) + col + 1]
            tri(a, b, e, IconHalo.copy(alpha = 0.02f + rnd() * 0.07f))
            tri(a, e, d, IconSky.copy(alpha = 0.02f + rnd() * 0.07f))
        }
    }
}

/** Anillo de tareas hechas hoy, con «hechas/total» dentro. */
@Composable
private fun HeroRing(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 5.dp.toPx()
            val inset = w / 2f
            val arc = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(IconHalo.copy(alpha = 0.22f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            drawArc(IconHalo, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(
            "$done/$total",
            style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = Color.White,
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   PIEZAS COMPARTIDAS DE ÁNIMO, HOY Y ESTA SEMANA

   Las tres zonas se arman con las MISMAS piezas: una tarjeta (`ZoneCard`), una
   fila tipo de 58 dp (`ZoneRow`) con su cifra a la derecha (`WhenFigure`) y una
   tira de selección (`SegmentStrip`). Antes Ánimo y Hoy tenían cada una su
   propia densidad, sus propios colores y sus propias proporciones, y por eso se
   leían como piezas de otra pantalla.
   ══════════════════════════════════════════════════════════════════════════ */

/** La tarjeta de zona: `VidaCard` con el margen interior de las filas. */
@Composable
private fun ZoneCard(content: @Composable () -> Unit) {
    VidaCard(padding = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) { content() }
    }
}

/** La fila tipo: baldosa de 32 dp, título, una línea de detalle y la cifra a la derecha. */
@Composable
private fun ZoneRow(
    tile: @Composable () -> Unit,
    title: String,
    detail: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (onClick != null) Modifier.vidaClickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tile()
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.5.sp),
                color = VidaTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            detail()
        }
        trailing()
    }
}

@Composable
private fun RowDetail(text: String) {
    Text(
        text,
        style = VidaTheme.type.micro.copy(fontSize = 12.sp),
        color = VidaTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * La cifra de la derecha: el dato en negrita y su aclaración debajo. En Hoy es
 * el CUÁNDO («2 días / atrasado», «Hoy / vence»); el rojo, solo lo atrasado.
 */
@Composable
private fun WhenFigure(figure: String, caption: String? = null, late: Boolean = false) {
    val c = VidaTheme.colors
    val tone = if (late) c.error else c.text
    Column(horizontalAlignment = Alignment.End) {
        Text(
            figure,
            style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, letterSpacing = (-0.01).em),
            color = tone,
            maxLines = 1,
        )
        if (caption != null) {
            Text(caption, style = VidaTheme.type.micro.copy(fontSize = 11.5.sp), color = if (late) c.error else c.textSecondary, maxLines = 1)
        }
    }
}

/**
 * La tira de selección: celdas de 62 dp con radio 16; la activa se rellena de
 * azul tinta. Siete celdas en Semana, cinco en Ánimo.
 */
@Composable
private fun SegmentStrip(
    count: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
    cell: @Composable (index: Int, on: Boolean) -> Unit,
) {
    val fill = if (VidaTheme.spec.isDark) Color(0xFF2B2870) else brandInk()
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            val on = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .height(62.dp)
                    .background(if (on) fill else Color.Transparent, RoundedCornerShape(16.dp))
                    .vidaClickable(onClick = { onSelect(i) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) { cell(i, on) }
        }
    }
}

/** Rótulo pequeño de una celda de la tira. */
@Composable
private fun CellLabel(text: String, on: Boolean) {
    Text(
        text,
        style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
        color = if (on) Color.White else VidaTheme.colors.textSecondary,
        maxLines = 1,
    )
}

/* ══════════════════════════════════════════════════════════════════════════
   2 · ÁNIMO — v3, diseño emocional (ver `VidaMoodCard.kt`)
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Sigue segundo (petición expresa) y conserva el título de zona con «Solo tú».
 * La tarjeta ya no es la fila tipo: el ánimo es el único momento de Inicio
 * que pregunta por la persona, y la v2 lo leía como un campo de formulario.
 */
@Composable
private fun MoodZone(
    name: String?,
    marked: Int?,
    loading: Boolean,
    failed: Boolean,
    onMark: (Int) -> Unit,
    onOpen: () -> Unit,
) {
    val c = VidaTheme.colors
    Zone(
        title = "Ánimo",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(12.dp))
                Text(
                    "Solo tú",
                    style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = c.textSecondary,
                )
            }
        },
    ) {
        VidaMoodCard(name, marked, loading, failed, onMark, onOpen)
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   3 · HOY
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Una fila de Hoy: la baldosa dice el módulo y la derecha dice CUÁNDO. Sin
 * píldoras de color: el único color fuera de la baldosa es el rojo de lo
 * atrasado.
 */
@Composable
private fun AttentionRow(item: AttentionItem, today: LocalDate, onClick: () -> Unit) {
    val section = sectionFor(item.source)
    // Lo compartido no trae importe ni fecha: su motivo («Ana espera tu parte») es el dato.
    val extra = item.amount ?: item.reason.takeIf { item.source == AttentionSource.SHARED }
    ZoneRow(
        tile = { SectionTile(section, size = 32.dp) },
        title = item.title,
        detail = { RowDetail(listOfNotNull(section.label, extra).joinToString(" · ")) },
        trailing = {
            when (item.urgency) {
                AttentionUrgency.OVERDUE -> {
                    val days = ChronoUnit.DAYS.between(item.date, today).toInt().coerceAtLeast(1)
                    WhenFigure(plural(days, "día", "días"), "atrasado", late = true)
                }
                AttentionUrgency.TODAY -> WhenFigure("Hoy", "vence")
                AttentionUrgency.SOON -> {
                    val days = ChronoUnit.DAYS.between(today, item.date).toInt()
                    if (days == 1) {
                        WhenFigure("Mañana", item.date.dayOfWeek.getDisplayName(DateTextStyle.FULL, ES_MX))
                    } else {
                        WhenFigure("${weekdayShort(item.date)} ${item.date.dayOfMonth}", "en $days días")
                    }
                }
            }
        },
        onClick = onClick,
    )
}

/* ══════════════════════════════════════════════════════════════════════════
   4 · ESTA SEMANA
   ══════════════════════════════════════════════════════════════════════════ */

/** Los siete días en la tira compartida; hoy es la celda marcada. */
@Composable
private fun WeekStrip(today: LocalDate, attention: List<AttentionItem>, onDay: (LocalDate) -> Unit) {
    val days = (0..6).map { today.plusDays(it.toLong()) }
    SegmentStrip(count = 7, selected = 0, onSelect = { onDay(days[it]) }) { i, on ->
        val date = days[i]
        val dot = if (on) IconSky else attention.firstOrNull { it.date == date }?.let { sectionFor(it.source).tint }
        CellLabel(weekdayShort(date), on)
        Text(
            date.dayOfMonth.toString(),
            style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = if (on) Color.White else VidaTheme.colors.text,
        )
        Box(Modifier.size(5.dp).background(dot ?: Color.Transparent, CircleShape))
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   5 · TUS ESPACIOS
   ══════════════════════════════════════════════════════════════════════════ */

private data class Space(val section: SectionVisual, val route: String, val badge: Int = 0)

/**
 * Todos los accesos a un solo tamaño: los cinco de la lámina más Calendario,
 * Familia y Mantenimiento, que antes se abrían desde widgets y tarjetas
 * sueltas. El contador va en azul tinta, no en el color de cada módulo.
 */
@Composable
private fun SpacesGrid(spaces: List<Space>, onNavigate: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        spaces.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { SpaceTile(it, Modifier.weight(1f)) { onNavigate(it.route) } }
            }
        }
    }
}

@Composable
private fun SpaceTile(space: Space, modifier: Modifier, onClick: () -> Unit) {
    val c = VidaTheme.colors
    val ink = brandInk()
    Column(
        modifier.vidaClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box {
            SectionTile(space.section, size = 52.dp)
            if (space.badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding()
                        .size(20.dp)
                        .background(ink, CircleShape)
                        .border(2.dp, c.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (space.badge > 9) "9+" else space.badge.toString(),
                        style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = if (VidaTheme.spec.isDark) Color(0xFF0F1216) else Color.White,
                    )
                }
            }
        }
        Text(
            space.section.label,
            style = VidaTheme.type.micro.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = c.text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * «Vision Board — Tus metas, en camino». Cierra la pantalla como contrapunto
 * del héroe: el paisaje llena la tarjeta y el texto va arriba, sobre el cielo.
 *
 * Los tonos son literales porque el texto se apoya en una ilustración clara;
 * el gris del tema oscuro desaparecería sobre ese cielo.
 */
@Composable
private fun VisionBoardCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, VidaTheme.colors.line, shape)
            .vidaClickable(onClick = onOpen),
    ) {
        VisionBoardBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(26.dp).background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = VidaSections.VisionBoard.tint, modifier = Modifier.size(15.dp))
                }
                Text(
                    "Vision Board",
                    style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = Color(0xFF2B2870),
                )
            }
            Text(
                "Tus metas, en camino",
                style = TextStyle(fontFamily = VidaFonts.FrauncesWordmark, fontWeight = FontWeight.Light, fontSize = 19.sp),
                color = Color(0xFF2B2870),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(20.dp).width(20.dp),
        )
    }
}
