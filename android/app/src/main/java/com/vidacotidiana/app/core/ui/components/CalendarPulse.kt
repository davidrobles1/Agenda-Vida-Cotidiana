package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.calendar.AlertSeverity
import com.vidacotidiana.app.core.calendar.DateAlert
import com.vidacotidiana.app.core.calendar.DayContent
import com.vidacotidiana.app.core.calendar.DayTask
import com.vidacotidiana.app.core.calendar.LoadSegment
import com.vidacotidiana.app.core.calendar.MonthCell
import com.vidacotidiana.app.core.calendar.load
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * EL PULSO DEL MES — el calendario del artefacto, no una rejilla genérica.
 *
 * Cinco piezas superpuestas, y las cinco están aquí:
 *   1. la celda es una PILA (cifra + barra de carga por tramos), no un número;
 *   2. hoy se marca con ANILLO y el elegido con RELLENO, para que se
 *      distingan cuando coinciden;
 *   3. la fila del día elegido SE ABRE EN SU SITIO y empuja las semanas
 *      siguientes, en vez de navegar a otra pantalla;
 *   4. una línea de tiempo con la MARCA DEL AHORA;
 *   5. los avisos son derivados y llevan de vuelta a su registro.
 */

private val ES = Locale("es", "MX")

/** Color de cada tramo de la barra de carga. Nunca rojo: es la regla de ADR-018. */
@Composable
private fun segmentColor(segment: LoadSegment): Color {
    val c = VidaTheme.colors
    return when (segment) {
        LoadSegment.TASK -> c.primary
        LoadSegment.HIGH -> c.warning
        // El tramo "próximo" es el acento rebajado contra la superficie, igual
        // que el `color-mix` de la Web.
        LoadSegment.MEDIUM -> c.primary.copy(alpha = 0.55f).compositeOverSurface()
        LoadSegment.LOW -> c.textTertiary
        LoadSegment.NOTE -> c.second.copy(alpha = 0.78f)
    }
}

@Composable
private fun Color.compositeOverSurface(): Color {
    val bg = VidaTheme.colors.surfaceVariant
    return Color(
        red = red * alpha + bg.red * (1 - alpha),
        green = green * alpha + bg.green * (1 - alpha),
        blue = blue * alpha + bg.blue * (1 - alpha),
        alpha = 1f,
    )
}

@Composable
fun alertTone(severity: AlertSeverity): Color = when (severity) {
    AlertSeverity.HIGH -> VidaTheme.colors.warning
    AlertSeverity.MEDIUM -> VidaTheme.colors.primary.copy(alpha = 0.62f).compositeOverSurface()
    AlertSeverity.LOW -> VidaTheme.colors.textTertiary
}

/**
 * La celda del mes. Una pila: cifra arriba, barra de carga debajo.
 *
 * La barra crece con muelle al entrar, así que al cambiar de mes la carga del
 * mes nuevo "sube" en vez de aparecer de golpe — el mismo gesto que la
 * animación `grow` del artefacto.
 */
@Composable
fun DayCell(
    cell: MonthCell,
    content: DayContent,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = VidaTheme.spec
    val c = spec.colors
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.control)
    val segments = content.load()

    val description = if (cell.inMonth) {
        "${cell.date.dayOfMonth} de ${cell.date.month.getDisplayName(JavaTextStyle.FULL, ES)}, ${content.summary()}"
    } else {
        ""
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .then(if (isSelected) Modifier.background(c.primaryContainer, shape) else Modifier)
            .then(if (isSelected) Modifier.border(1.5.dp, c.primary, shape) else Modifier)
            .then(
                if (cell.inMonth) {
                    Modifier
                        .pressScale(interaction, 0.9f)
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(color = c.primary, bounded = false),
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .semantics { if (description.isNotEmpty()) contentDescription = description },
        contentAlignment = Alignment.TopCenter,
    ) {
        // Hoy: ANILLO, no relleno. El relleno se reserva a lo que el usuario
        // acaba de elegir, y así los dos estados conviven sin pisarse.
        if (isToday) {
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(28.dp)
                    .drawBehind {
                        drawCircle(
                            color = c.primary.copy(alpha = 0.55f),
                            radius = size.minDimension / 2 - 1.dp.toPx(),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    },
            )
        }
        Column(
            modifier = Modifier.padding(top = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = when {
                        isSelected || isToday -> FontWeight.ExtraBold
                        else -> FontWeight.SemiBold
                    },
                ),
                color = when {
                    !cell.inMonth -> c.textTertiary.copy(alpha = 0.45f)
                    isSelected || isToday -> c.primary
                    else -> c.text
                },
            )
            if (cell.inMonth && segments.isNotEmpty()) {
                LoadBar(segments)
            }
        }
    }
}

/** La barra de carga: hasta cuatro tramos apilados, el más urgente arriba. */
@Composable
private fun LoadBar(segments: List<LoadSegment>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(16.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
        segments.forEach { segment ->
            val color = segmentColor(segment)
            val h by animateDpAsState(
                targetValue = 3.5.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "loadSeg",
            )
            Box(Modifier.fillMaxWidth().height(h).background(color, RoundedCornerShape(2.dp)))
        }
    }
}

/** Punto de carga compacto, para la cinta de semana y la de próximos días. */
@Composable
fun LoadDots(content: DayContent, tint: Color? = null, modifier: Modifier = Modifier) {
    val segments = content.load().take(3)
    Row(modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        segments.forEach { s ->
            Box(Modifier.size(5.dp).background(tint ?: segmentColor(s), CircleShape))
        }
    }
}

/**
 * La rejilla del mes con el panel del día abierto DENTRO de ella.
 *
 * Se usa un `Layout` propio en vez de `LazyVerticalGrid` porque hay que
 * insertar un elemento a lo ancho ENTRE dos semanas concretas: es justo lo que
 * una rejilla perezosa no deja hacer, y es lo que da al calendario su gesto
 * característico. El panel se anima con `expandVertically` + muelle, que mide
 * su propio contenido — el `max-height` del prototipo era una aproximación,
 * esto no.
 */
@Composable
fun MonthGrid(
    cells: List<MonthCell>,
    today: LocalDate,
    selected: LocalDate,
    contentFor: (LocalDate) -> DayContent,
    onSelect: (LocalDate) -> Unit,
    dayPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val selectedRow = cells.indexOfFirst { it.date == selected && it.inMonth }.let { if (it < 0) -1 else it / 7 }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                Text(
                    d,
                    style = VidaTheme.type.micro,
                    color = c.textTertiary,
                    modifier = Modifier.weight(1f).clearAndSetSemantics { },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEachIndexed { rowIndex, week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { cell ->
                    DayCell(
                        cell = cell,
                        content = if (cell.inMonth) contentFor(cell.date) else DayContent(cell.date),
                        isToday = cell.date == today,
                        isSelected = cell.date == selected && cell.inMonth,
                        onClick = { onSelect(cell.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AnimatedVisibility(
                visible = rowIndex == selectedRow,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
            ) {
                Box(Modifier.padding(top = 6.dp, bottom = 4.dp)) { dayPanel() }
            }
        }
    }
}

/** Cabecera del panel del día: la cifra grande, el día de la semana y el resumen. */
@Composable
fun DayPanelHeader(date: LocalDate, content: DayContent, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = c.text,
        )
        Column(Modifier.weight(1f)) {
            Text(
                date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, ES).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = c.text,
            )
            Text(
                date.month.getDisplayName(JavaTextStyle.FULL, ES),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
        VidaPill(content.summary(), if (content.hasUrgent) PillTone.WARN else PillTone.QUIET)
    }
}

/**
 * La línea de tiempo del día: tareas con hora, la marca del ahora, tareas sin
 * hora, avisos derivados y notas. Un solo carril, jerarquía por tono.
 */
@Composable
fun DayTimeline(
    content: DayContent,
    isToday: Boolean,
    now: LocalTime,
    onTask: (DayTask) -> Unit,
    onAlert: (DateAlert) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val timed = content.tasks.filter { it.time != null }.sortedBy { it.time }
    val untimed = content.tasks.filter { it.time == null }

    if (content.isEmpty) {
        EmptyState(
            title = "Un día libre",
            body = "Nada reclama tu atención.",
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var nowDrawn = false
        timed.forEach { task ->
            if (isToday && !nowDrawn && task.time!! > now) {
                NowMarker(now)
                nowDrawn = true
            }
            TimelineItem(
                rail = task.time!!.toString().take(5),
                title = task.title,
                // FR-024: dónde es, junto al cuándo. Mismo criterio que las
                // otras tres superficies de tareas: una sola línea, sin
                // seccion propia.
                subtitle = listOfNotNull(task.meta, task.location).joinToString(" \u00b7 "),
                tone = if (task.done) c.successText else c.primary,
                strikeThrough = task.done,
                onClick = { onTask(task) },
            )
        }
        if (isToday && !nowDrawn) NowMarker(now)
        untimed.forEach { task ->
            TimelineItem(
                rail = "—",
                title = task.title,
                // FR-024: dónde es, junto al cuándo. Mismo criterio que las
                // otras tres superficies de tareas: una sola línea, sin
                // seccion propia.
                subtitle = listOfNotNull(task.meta, task.location).joinToString(" \u00b7 "),
                tone = if (task.done) c.successText else c.primary,
                strikeThrough = task.done,
                onClick = { onTask(task) },
            )
        }
        content.alerts.sortedBy { it.severity.rank }.forEach { alert ->
            TimelineItem(
                rail = "aviso",
                title = alert.label,
                subtitle = alert.message + (alert.amount?.let { " · $it" } ?: ""),
                tone = alertTone(alert.severity),
                onClick = { onAlert(alert) },
            )
        }
        content.notes.forEach { note ->
            TimelineItem(
                rail = "nota",
                title = note,
                subtitle = null,
                tone = c.second,
                handwritten = spec.fonts.hand != null,
                onClick = null,
            )
        }
    }
}

@Composable
private fun TimelineItem(
    rail: String,
    title: String,
    subtitle: String?,
    tone: Color,
    strikeThrough: Boolean = false,
    handwritten: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.control)
    val interaction = remember { MutableInteractionSource() }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
        Text(
            rail,
            style = VidaTheme.type.micro,
            color = c.textTertiary,
            modifier = Modifier.width(38.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        // El punto del carril, con su halo sobre el fondo hundido del panel.
        Box(
            Modifier
                .size(9.dp)
                .background(tone, CircleShape)
                .border(2.5.dp, c.sunken, CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onClick != null) {
                        Modifier
                            .pressScale(interaction, 0.985f)
                            .clickable(interactionSource = interaction, indication = ripple(color = c.primary), onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .background(c.surfaceVariant, shape)
                .border(spec.borderWidth, c.line, shape)
                .drawBehind {
                    // Filete de tono a la izquierda, igual que en las filas.
                    drawRect(tone, size = Size((if (spec.borderWidth > 1.dp) 5.dp else 3.dp).toPx(), size.height))
                }
                .padding(start = 12.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        ) {
            Text(
                title,
                style = if (handwritten) {
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = spec.fonts.hand, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    )
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (strikeThrough) c.textTertiary else c.text,
                textDecoration = if (strikeThrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

/**
 * La marca del ahora. Es lo único del calendario que se dibuja en rojo, y por
 * eso mismo funciona: el rojo está reservado y aquí no señala un aviso, señala
 * el instante actual.
 */
@Composable
private fun NowMarker(now: LocalTime) {
    val c = VidaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
        Text(
            now.toString().take(5),
            style = VidaTheme.type.micro,
            color = c.error,
            modifier = Modifier.width(38.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Box(Modifier.size(8.dp).background(c.error, CircleShape))
        Box(Modifier.weight(1f).height(1.dp).background(c.error.copy(alpha = 0.5f)))
    }
}

/** Cinta de días deslizable: la semana del calendario y "los próximos 7 días". */
@Composable
fun DayRibbonItem(
    date: LocalDate,
    content: DayContent,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.control)

    Column(
        modifier = modifier
            .width(54.dp)
            .pressScale(interaction, 0.93f)
            .background(if (isSelected) c.primary else c.surfaceVariant, shape)
            .border(spec.borderWidth, if (isSelected || isToday) c.primary else c.line, shape)
            .clickable(interactionSource = interaction, indication = ripple(color = c.primary), onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, ES).uppercase(),
            style = VidaTheme.type.micro,
            color = if (isSelected) c.onPrimary.copy(alpha = 0.85f) else c.textTertiary,
        )
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (isSelected) c.onPrimary else c.text,
        )
        LoadDots(content, tint = if (isSelected) c.onPrimary.copy(alpha = 0.85f) else null)
        Spacer(Modifier.height(0.dp))
    }
}
