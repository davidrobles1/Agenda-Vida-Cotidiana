package com.vidacotidiana.app.core.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaFonts
import kotlin.math.PI
import kotlin.math.sin

/**
 * ADR-024 — identidad interna «A · Tiempo».
 *
 * El símbolo es UNO SOLO, el arco del día, y lo único que distingue las tres
 * identidades es LA POSICIÓN DEL PUNTO sobre él: Jornada en el cenit (0.50),
 * Cotidiana en la tarde (0.82), Oficio en la mañana (0.22). No hay iconos por
 * módulo ni monograma; el nombre propio hace el resto.
 *
 * La construcción es la de `web/src/core/ui/brand/BrandMark.tsx`, no una
 * reinterpretación: todo se deriva del cuerpo del rótulo igual que allí —ancho
 * del arco 2.5×cuerpo, alto 0.42×ancho, radios `(W-8)/2` y `H-7`, apoyo en
 * `H-2`— y el punto se sitúa sobre la misma elipse con la misma
 * parametrización. Por eso escala a cualquier tamaño sin dejar de ser el logo
 * aprobado.
 *
 * ADR-024(d): SIN FONDO. Ni placa, ni círculo, ni sombra — el fondo pertenece
 * a la superficie que lo aloja. ADR-024(e): los temas no lo visten; el rótulo
 * usa Fraunces 300 fija y no la tipografía de display del tema activo.
 */
enum class BrandContext(val brandName: String, val dotPosition: Float, val accent: Color) {
    // Los tres acentos son los tokens `--brand-arc-*` de `web/src/index.css`.

    /** El día entero, antes de partirlo: el punto corona el arco. */
    PORTAL("Jornada", 0.50f, Color(0xFF8A6D3B)), //   ocre de mediodía

    /** El tramo que es tuyo: el punto cae en la tarde. */
    PERSONAL("Cotidiana", 0.82f, Color(0xFFA8523C)), // terracota de tarde

    /** La franja de trabajo: el punto sube por la mañana. */
    LABORAL("Oficio", 0.22f, Color(0xFF243447)), //   azul de mañana
}

/** `--brand-ink` de `index.css`. */
val BrandInk = Color(0xFF16161A)

@Composable
fun BrandMark(
    context: BrandContext,
    modifier: Modifier = Modifier,
    /** Cuerpo del rótulo. Todo lo demás se deriva de él, como en la Web. */
    size: Dp = 15.dp,
    inkColor: Color = BrandInk,
) {
    val arcWidth = size * 2.5f
    val arcHeight = arcWidth * 0.42f
    val nameSize = with(LocalDensity.current) { size.toSp() }

    Column(
        // El logo se anuncia UNA vez y por su nombre: arco y rótulo son partes
        // del mismo signo, no dos elementos que leer por separado.
        modifier = modifier.clearAndSetSemantics { contentDescription = context.brandName },
        verticalArrangement = Arrangement.spacedBy(size * 0.6f),
    ) {
        Canvas(Modifier.width(arcWidth).height(arcHeight)) {
            val w = size.toPx() * 2.5f
            val h = w * 0.42f
            val inset = 4.dp.toPx()
            val rx = (w - inset * 2f) / 2f
            val ry = h - 7.dp.toPx()
            val base = h - 2.dp.toPx()

            // Media elipse: de `inset` a `w - inset`, apoyada en la línea del
            // horizonte del día.
            drawPath(
                path = Path().apply {
                    addArc(
                        Rect(left = inset, top = base - ry, right = w - inset, bottom = base + ry),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                    )
                },
                color = context.accent,
                alpha = 0.38f,
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
            )

            // El punto NO flota: se apoya sobre esa misma elipse, en la
            // posición horaria que define la identidad del contexto.
            drawCircle(
                color = context.accent,
                radius = 2.8.dp.toPx(),
                center = Offset(
                    x = inset + (w - inset * 2f) * context.dotPosition,
                    y = base - sin(context.dotPosition * PI).toFloat() * ry,
                ),
            )
        }

        Text(
            text = context.brandName,
            style = TextStyle(
                fontFamily = VidaFonts.Fraunces,
                fontWeight = FontWeight.Light,
                fontSize = nameSize,
                lineHeight = nameSize,
                letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
            ),
            color = inkColor,
        )
    }
}
