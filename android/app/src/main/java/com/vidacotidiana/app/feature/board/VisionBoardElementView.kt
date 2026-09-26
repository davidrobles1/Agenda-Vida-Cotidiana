package com.vidacotidiana.app.feature.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.data.BoardElement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Cómo se dibuja cada tipo de elemento del tablero.
 *
 * Aquí NO hay gestos: el contenido se pinta y nada más. Quién recibe el
 * toque, quién arrastra y quién hace zoom lo decide un único árbitro en la
 * raíz del lienzo (`VisionBoardScreen`), que es justo lo que evita que el
 * desplazamiento de la pantalla y el arrastre de un elemento se peleen por
 * el mismo movimiento del dedo.
 */
@Composable
fun BoardElementContent(
    element: BoardElement,
    theme: BoardTheme,
    image: androidx.compose.ui.graphics.ImageBitmap?,
    /** Cuánto mide en pantalla un punto del tablero. El texto se compone a
        ESE tamaño, así que se lee nítido con cualquier zoom en vez de
        ampliarse pixelado. */
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    when (element.type) {
        "TEXT" -> TextContent(element, theme, scale, modifier)
        "NOTE" -> NoteContent(element, theme, scale, modifier)
        "IMAGE" -> ImageContent(element, theme, image, scale, modifier)
        "SHAPE" -> ShapeContent(element, theme, scale, modifier)
        "STICKER" -> StickerContent(element, theme, modifier)
        "TABLE" -> TableContent(element, theme, scale, modifier)
        "CHART" -> ChartContent(element, modifier)
        "GRID" -> GridContent(element, modifier)
        else -> Box(modifier)
    }
}

/** Un tamaño del tablero, en el tipográfico que le toca en pantalla. */
@Composable
private fun scaledSp(value: Float, scale: Float) =
    with(androidx.compose.ui.platform.LocalDensity.current) { (value * scale).dp.toSp() }

/* =========================================================
   TEXTO Y NOTA
   ========================================================= */

@Composable
private fun TextContent(element: BoardElement, theme: BoardTheme, scale: Float, modifier: Modifier) {
    val size = fontSizeOf(element.int("fontSize")).toFloat()
    Box(modifier.padding((4 * scale).dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = element.text.orEmpty(),
            style = TextStyle(
                fontFamily = fontFamilyOf(element.str("font")),
                fontSize = scaledSp(size, scale),
                lineHeight = scaledSp(size * 1.3f, scale),
                color = theme.colors.text,
            ),
        )
    }
}

@Composable
private fun NoteContent(element: BoardElement, theme: BoardTheme, scale: Float, modifier: Modifier) {
    Box(
        modifier
            .background(theme.colors.noteBackground, RoundedCornerShape((4 * scale).dp))
            .padding((10 * scale).dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = element.text.orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = theme.colors.text,
                fontSize = scaledSp(14f, scale),
                lineHeight = scaledSp(18f, scale),
            ),
        )
    }
}

/* =========================================================
   IMAGEN Y SUS MARCOS
   ========================================================= */

private val HexagonShape = GenericShape { size, _ ->
    // El mismo hexágono del marco de la Web: 50% 2%, 93% 26%, 93% 74%…
    val pts = listOf(
        0.50f to 0.02f, 0.93f to 0.26f, 0.93f to 0.74f,
        0.50f to 0.98f, 0.07f to 0.74f, 0.07f to 0.26f,
    )
    moveTo(size.width * pts[0].first, size.height * pts[0].second)
    pts.drop(1).forEach { lineTo(size.width * it.first, size.height * it.second) }
    close()
}

@Composable
private fun ImageContent(
    element: BoardElement,
    theme: BoardTheme,
    image: androidx.compose.ui.graphics.ImageBitmap?,
    scale: Float,
    modifier: Modifier,
) {
    val frame = element.str("frameStyle")
    val inner: @Composable (Modifier) -> Unit = { m ->
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Imagen del tablero",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = m,
            )
        } else {
            Box(m.background(theme.colors.surface), contentAlignment = Alignment.Center) {
                Text("…", color = theme.colors.textSecondary)
            }
        }
    }

    when (frame) {
        "circle" -> inner(modifier.clip(CircleShape))
        "hexagon" -> inner(modifier.clip(HexagonShape))
        "rounded" -> inner(modifier.clip(RoundedCornerShape((18 * scale).dp)))
        "polaroid" -> Box(
            modifier
                .background(Color.White)
                .padding(start = (7 * scale).dp, top = (7 * scale).dp, end = (7 * scale).dp, bottom = (22 * scale).dp),
        ) { inner(Modifier.fillMaxSize()) }
        "film" -> Box(modifier.background(Color(0xFF17181C)).padding(horizontal = (9 * scale).dp, vertical = (5 * scale).dp)) {
            inner(Modifier.fillMaxSize())
            // Las perforaciones, arriba y abajo, como en la Web.
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                repeat(2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        repeat(6) {
                            Box(Modifier.size(width = (5 * scale).dp, height = (3 * scale).dp).background(Color(0xFF3A3D45)))
                        }
                    }
                }
            }
        }
        else -> inner(modifier)
    }
}

/* =========================================================
   FORMAS  ·  las 98 del catálogo
   ========================================================= */

@Composable
private fun ShapeContent(element: BoardElement, theme: BoardTheme, scale: Float, modifier: Modifier) {
    val def = shapeOf(element.shape)
    val fill = parseHexColor(element.str("fill")) ?: theme.colors.shapeBackground
    val label = element.text
    val textColor = parseHexColor(element.str("textColor"))
        ?: parseHexColor(suggestTextColor(element.str("fill") ?: DEFAULT_SHAPE_FILL))
        ?: theme.colors.text

    Box(modifier, contentAlignment = Alignment.Center) {
        when (def.id) {
            "rectangle" -> Box(Modifier.fillMaxSize().background(fill, RoundedCornerShape((6 * scale).dp)))
            "circle" -> Box(Modifier.fillMaxSize().background(fill, CircleShape))
            "capsule" -> Box(Modifier.fillMaxSize().background(fill, RoundedCornerShape(999.dp)))
            "line" -> Box(
                Modifier.fillMaxWidth().height((4 * scale).dp).align(Alignment.Center)
                    .background(fill, RoundedCornerShape(999.dp)),
            )
            else -> {
                val path = remember(def.id) { PathParser().parsePathString(def.path.orEmpty()).toPath() }
                Canvas(Modifier.fillMaxSize()) {
                    path.fillType = if (def.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
                    withTransform({ scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) }) {
                        drawPath(path, fill)
                    }
                }
            }
        }
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = textColor,
                    fontSize = scaledSp(14f, scale),
                    lineHeight = scaledSp(18f, scale),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding((8 * scale).dp),
            )
        }
    }
}

/* =========================================================
   STICKERS Y EMOJIS
   ========================================================= */

/**
 * Un sticker es una INSIGNIA, no un icono suelto: el dibujo dentro de un
 * círculo de su color. Es lo que hace que se lea como pegatina pegada al
 * tablero y no como un icono de la interfaz — la Web hace exactamente lo
 * mismo (`StickerPicker`/`VisionBoardElementView`).
 *
 * Un emoji, en cambio, va suelto y con el color de su categoría.
 */
@Composable
private fun StickerContent(element: BoardElement, theme: BoardTheme, modifier: Modifier) {
    val emoji = emojiOf(element.str("emojiId"))
    val sticker = stickerOf(element.str("stickerId"))
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            emoji != null -> Icon(emoji.icon, emoji.label, Modifier.fillMaxSize().padding(4.dp), tint = emoji.color)
            sticker != null -> StickerBadge(sticker.icon, sticker.label, sticker.color, Modifier.fillMaxSize())
            else -> Icon(
                Icons.Outlined.Star, null,
                Modifier.fillMaxSize().padding(6.dp), tint = theme.colors.accent,
            )
        }
    }
}

@Composable
fun StickerBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(color.copy(alpha = 0.16f), CircleShape)
            .border(2.dp, color, CircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.fillMaxSize(), tint = color)
    }
}

/* =========================================================
   TABLA  ·  contenido de ejemplo, como en la Web
   ========================================================= */

@Composable
private fun TableContent(element: BoardElement, theme: BoardTheme, scale: Float, modifier: Modifier) {
    val border = theme.colors.border
    Column(
        modifier
            .background(theme.colors.surface, RoundedCornerShape((6 * scale).dp))
            .border(1.dp, border, RoundedCornerShape((6 * scale).dp)),
    ) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            listOf("Columna 1", "Columna 2").forEachIndexed { i, title ->
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .background(theme.colors.selectionContainer)
                        .padding((6 * scale).dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(title, style = MaterialTheme.typography.labelSmall.copy(color = theme.colors.text, fontSize = scaledSp(12f, scale)))
                }
                if (i == 0) Box(Modifier.width(1.dp).fillMaxHeight().background(border))
            }
        }
        repeat(2) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(border))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.weight(1f).fillMaxHeight())
                Box(Modifier.width(1.dp).fillMaxHeight().background(border))
                Box(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/* =========================================================
   GRÁFICAS  ·  los mismos datos de ejemplo y colores que la Web
   ========================================================= */

@Composable
private fun ChartContent(element: BoardElement, modifier: Modifier) {
    val type = element.str("chartType") ?: "bar"
    Canvas(modifier.background(Color.White, RoundedCornerShape(6.dp)).padding(10.dp)) {
        when (type) {
            "bar" -> drawBars()
            "line" -> drawSeries(area = false)
            "area" -> drawSeries(area = true)
            "pie" -> drawPie(donut = false)
            "donut" -> drawPie(donut = true)
            "gauge" -> drawGauge()
            else -> drawBars()
        }
    }
}

private fun DrawScope.drawBars() {
    val max = CHART_SAMPLE_SERIES.max() * 1.15f
    val gap = size.width * 0.03f
    val barWidth = (size.width - gap * (CHART_SAMPLE_SERIES.size - 1)) / CHART_SAMPLE_SERIES.size
    CHART_SAMPLE_SERIES.forEachIndexed { i, v ->
        val h = size.height * (v / max)
        drawRect(
            color = CHART_SLICE_COLORS[i % CHART_SLICE_COLORS.size],
            topLeft = Offset(i * (barWidth + gap), size.height - h),
            size = Size(barWidth, h),
        )
    }
}

private fun DrawScope.drawSeries(area: Boolean) {
    val max = CHART_SAMPLE_SERIES.max() * 1.15f
    val step = size.width / (CHART_SAMPLE_SERIES.size - 1)
    val points = CHART_SAMPLE_SERIES.mapIndexed { i, v ->
        Offset(i * step, size.height - (v / max) * size.height)
    }
    if (area) {
        val path = Path().apply {
            moveTo(0f, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, CHART_SLICE_COLORS[0].copy(alpha = 0.18f))
    }
    for (i in 0 until points.size - 1) {
        drawLine(CHART_SLICE_COLORS[0], points[i], points[i + 1], strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)
    }
    if (!area) points.forEach { drawCircle(CHART_SLICE_COLORS[0], radius = 3.dp.toPx(), center = it) }
}

private fun DrawScope.drawPie(donut: Boolean) {
    val total = CHART_SAMPLE_SLICES.sum()
    val d = min(size.width, size.height)
    val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
    var start = -90f
    CHART_SAMPLE_SLICES.forEachIndexed { i, v ->
        val sweep = 360f * (v / total)
        drawArc(
            color = CHART_SLICE_COLORS[i % CHART_SLICE_COLORS.size],
            startAngle = start, sweepAngle = sweep, useCenter = !donut,
            topLeft = topLeft, size = Size(d, d),
            style = if (donut) Stroke(width = d * 0.26f) else androidx.compose.ui.graphics.drawscope.Fill,
        )
        start += sweep
    }
}

private fun DrawScope.drawGauge() {
    val d = min(size.width, size.height * 2f)
    val topLeft = Offset((size.width - d) / 2f, size.height - d / 2f)
    val stroke = Stroke(width = d * 0.14f, cap = StrokeCap.Round)
    drawArc(Color(0xFFE2E4E9), 180f, 180f, false, topLeft, Size(d, d), style = stroke)
    drawArc(CHART_SLICE_COLORS[0], 180f, 126f, false, topLeft, Size(d, d), style = stroke)
    // La aguja, al 70 % del recorrido.
    val angle = (180f + 126f) * PI.toFloat() / 180f
    val cx = topLeft.x + d / 2f
    val cy = topLeft.y + d / 2f
    drawLine(
        CHART_SLICE_COLORS[3],
        Offset(cx, cy),
        Offset(cx + cos(angle) * d * 0.34f, cy + sin(angle) * d * 0.34f),
        strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round,
    )
}

/* =========================================================
   CUADRÍCULA  ·  celdas de color, mismo alcance que la Web
   ========================================================= */

@Composable
private fun GridContent(element: BoardElement, modifier: Modifier) {
    val layout = gridOf(element.str("layout"))
    Box(modifier.clip(RoundedCornerShape(6.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val gap = 3.dp.toPx()
            val cellW = (size.width - gap * (layout.cols - 1)) / layout.cols
            val cellH = (size.height - gap * (layout.rows - 1)) / layout.rows
            var index = 0
            var col = 0
            var row = 0
            while (index < layout.cells && row < layout.rows) {
                val spans = index == layout.spanCell
                val h = if (spans) size.height else cellH
                drawRect(
                    color = GRID_CELL_COLORS[index % GRID_CELL_COLORS.size],
                    topLeft = Offset(col * (cellW + gap), row * (cellH + gap)),
                    size = Size(cellW, h),
                )
                index++
                if (spans) {
                    col++
                } else {
                    col++
                    if (col >= layout.cols) { col = 0; row++ }
                }
                if (col >= layout.cols) { col = 0; row++ }
            }
        }
    }
}

/** El rectángulo que ocupa un elemento, en coordenadas del tablero. */
fun BoardElement.rect(): Rect = Rect(x, y, x + width, y + height)
