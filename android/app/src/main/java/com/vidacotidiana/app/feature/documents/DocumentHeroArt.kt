package com.vidacotidiana.app.feature.documents

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * LA LÁMINA DE FONDO DEL DETALLE — «vista previa personalizada según su
 * categoría» de la propuesta aprobada (2026-09-26).
 *
 * Es una imagen REPRESENTATIVA, no el documento: la cabecera se ve al abrir
 * cualquier archivo, y pintar ahí el recibo o la identificación real los
 * enseñaría a quien mire la pantalla. El documento de verdad sólo aparece en
 * «Vista previa», más abajo, y al abrirlo.
 *
 * Se dibuja con `Canvas` en el tono de la categoría, sin recursos gráficos:
 * cambia con el tema claro/oscuro y no suma peso al APK. Ocupa la mitad
 * derecha y se funde hacia la izquierda y hacia abajo con el fondo, para que
 * el nombre y los datos que van ENCIMA se lean siempre.
 */
@Composable
internal fun DocumentHeroArt(categoryKey: String, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    val tone = categoryVisual(categoryKey).tone
    val paper = c.surface
    val ink = c.border.copy(alpha = 0.30f)
    val edge = c.border.copy(alpha = 0.16f)
    val green = c.success

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Halo de color de la categoría, desde la esquina superior derecha.
        drawRect(
            Brush.radialGradient(
                listOf(tone.bg, tone.bg.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(w * 0.86f, h * 0.22f),
                radius = w * 0.72f,
            ),
        )

        val sheetW = minOf(w * 0.34f, h * 0.46f)
        val sheetH = sheetW * 1.3f
        val sheets = Sheets(paper, ink, edge, tone.fg, tone.bg, green)

        when (categoryKey) {
            "IDENTIFICACION" -> {
                sheets.page(this, Offset(w * 0.83f, h * 0.30f), sheetW * 0.9f, sheetH * 0.9f, 10f)
                sheets.idCard(this, Offset(w * 0.66f, h * 0.40f), sheetW * 1.45f, sheetW * 0.92f, -6f)
            }
            "SEGUROS" -> {
                sheets.page(this, Offset(w * 0.84f, h * 0.30f), sheetW, sheetH, 9f)
                sheets.page(this, Offset(w * 0.66f, h * 0.38f), sheetW, sheetH, -6f) { center, sw, sh ->
                    shield(center + Offset(sw * 0.18f, sh * 0.2f), sw * 0.34f, tone.fg, tone.bg)
                }
            }
            "CONTRATOS" -> {
                sheets.page(this, Offset(w * 0.84f, h * 0.28f), sheetW, sheetH, 10f)
                sheets.page(this, Offset(w * 0.75f, h * 0.33f), sheetW, sheetH, 3f)
                sheets.page(this, Offset(w * 0.64f, h * 0.40f), sheetW, sheetH, -7f) { center, sw, sh ->
                    signature(center + Offset(0f, sh * 0.3f), sw * 0.6f, tone.fg)
                }
            }
            "COMPROBANTES" -> {
                sheets.page(this, Offset(w * 0.84f, h * 0.30f), sheetW, sheetH, 9f)
                sheets.page(this, Offset(w * 0.66f, h * 0.38f), sheetW, sheetH, -6f) { center, sw, sh ->
                    bars(center + Offset(sw * 0.16f, sh * 0.3f), sw * 0.36f, sh * 0.18f, green)
                }
            }
            else -> {
                folder(Offset(w * 0.74f, h * 0.44f), sheetW * 1.7f, sheetW * 1.05f, tone.fg, tone.bg)
                sheets.page(this, Offset(w * 0.70f, h * 0.30f), sheetW * 0.9f, sheetH * 0.8f, -5f)
                sheets.page(this, Offset(w * 0.82f, h * 0.32f), sheetW * 0.9f, sheetH * 0.8f, 7f)
                folderFront(Offset(w * 0.74f, h * 0.44f), sheetW * 1.7f, sheetW * 1.05f, tone.fg, tone.bg)
            }
        }

        // Fundido hacia la izquierda (donde va el texto) y hacia abajo.
        drawRect(
            Brush.horizontalGradient(
                0f to paper,
                0.28f to paper.copy(alpha = 0.85f),
                0.55f to paper.copy(alpha = 0.15f),
                0.7f to Color.Transparent,
            ),
        )
        drawRect(
            Brush.verticalGradient(
                0.55f to Color.Transparent,
                0.82f to paper.copy(alpha = 0.8f),
                1f to paper,
            ),
        )
    }
}

/** Las piezas de papel: hoja y tarjeta, con sombra, filete y renglones. */
private class Sheets(
    val paper: Color,
    val ink: Color,
    val edge: Color,
    val accent: Color,
    val accentBg: Color,
    val green: Color,
) {
    fun page(
        scope: DrawScope,
        center: Offset,
        sw: Float,
        sh: Float,
        degrees: Float,
        motif: DrawScope.(Offset, Float, Float) -> Unit = { _, _, _ -> },
    ) = with(scope) {
        rotate(degrees, pivot = center) {
            val topLeft = center - Offset(sw / 2, sh / 2)
            val r = CornerRadius(sw * 0.07f)
            drawRoundRect(Color.Black.copy(alpha = 0.07f), topLeft + Offset(0f, sw * 0.04f), Size(sw, sh), r)
            drawRoundRect(paper, topLeft, Size(sw, sh), r)
            drawRoundRect(edge, topLeft, Size(sw, sh), r, style = Stroke(1.2f))
            val pad = sw * 0.12f
            // Membrete en el color de la categoría.
            drawRoundRect(
                accent.copy(alpha = 0.75f), topLeft + Offset(pad, pad),
                Size(sw * 0.34f, sh * 0.045f), CornerRadius(sh * 0.02f),
            )
            val widths = listOf(0.76f, 0.6f, 0.7f, 0.5f, 0.66f)
            widths.forEachIndexed { i, f ->
                drawRoundRect(
                    ink, topLeft + Offset(pad, pad + sh * (0.12f + i * 0.075f)),
                    Size((sw - pad * 2) * f, sh * 0.025f), CornerRadius(sh * 0.0125f),
                )
            }
            motif(center, sw, sh)
        }
    }

    fun idCard(scope: DrawScope, center: Offset, cw: Float, ch: Float, degrees: Float) = with(scope) {
        rotate(degrees, pivot = center) {
            val topLeft = center - Offset(cw / 2, ch / 2)
            val r = CornerRadius(cw * 0.06f)
            drawRoundRect(Color.Black.copy(alpha = 0.08f), topLeft + Offset(0f, cw * 0.03f), Size(cw, ch), r)
            drawRoundRect(paper, topLeft, Size(cw, ch), r)
            drawRoundRect(edge, topLeft, Size(cw, ch), r, style = Stroke(1.2f))
            // Franja superior y retrato genérico, sin ningún dato.
            drawRoundRect(accentBg, topLeft, Size(cw, ch * 0.2f), r)
            val photoTL = topLeft + Offset(cw * 0.08f, ch * 0.3f)
            val photo = Size(cw * 0.28f, ch * 0.56f)
            drawRoundRect(accentBg, photoTL, photo, CornerRadius(cw * 0.03f))
            val head = photoTL + Offset(photo.width / 2, photo.height * 0.38f)
            drawCircle(accent.copy(alpha = 0.7f), photo.width * 0.2f, head)
            drawArc(
                accent.copy(alpha = 0.7f), 180f, 180f, true,
                topLeft = Offset(head.x - photo.width * 0.34f, photoTL.y + photo.height * 0.62f),
                size = Size(photo.width * 0.68f, photo.height * 0.7f),
            )
            listOf(0.5f, 0.38f, 0.44f, 0.3f).forEachIndexed { i, f ->
                drawRoundRect(
                    ink, topLeft + Offset(cw * 0.44f, ch * (0.34f + i * 0.13f)),
                    Size(cw * f, ch * 0.05f), CornerRadius(ch * 0.025f),
                )
            }
        }
    }
}

private fun DrawScope.bars(origin: Offset, bw: Float, bh: Float, color: Color) {
    val heights = listOf(0.35f, 0.6f, 0.45f, 0.8f, 1f)
    val gap = bw / heights.size
    heights.forEachIndexed { i, f ->
        drawRoundRect(
            color.copy(alpha = 0.55f + 0.09f * i),
            Offset(origin.x - bw / 2 + i * gap, origin.y - bh * f),
            Size(gap * 0.62f, bh * f),
            CornerRadius(gap * 0.15f),
        )
    }
}

private fun DrawScope.shield(center: Offset, s: Float, fg: Color, bg: Color) {
    val p = Path().apply {
        moveTo(center.x, center.y - s * 0.55f)
        lineTo(center.x + s * 0.45f, center.y - s * 0.35f)
        quadraticTo(center.x + s * 0.45f, center.y + s * 0.3f, center.x, center.y + s * 0.6f)
        quadraticTo(center.x - s * 0.45f, center.y + s * 0.3f, center.x - s * 0.45f, center.y - s * 0.35f)
        close()
    }
    drawPath(p, bg)
    drawPath(p, fg.copy(alpha = 0.8f), style = Stroke(s * 0.07f))
    val tick = Path().apply {
        moveTo(center.x - s * 0.18f, center.y)
        lineTo(center.x - s * 0.03f, center.y + s * 0.15f)
        lineTo(center.x + s * 0.2f, center.y - s * 0.12f)
    }
    drawPath(tick, fg, style = Stroke(s * 0.08f, cap = StrokeCap.Round))
}

private fun DrawScope.signature(center: Offset, sw: Float, color: Color) {
    val p = Path().apply {
        moveTo(center.x - sw / 2, center.y)
        cubicTo(center.x - sw * 0.3f, center.y - sw * 0.25f, center.x - sw * 0.15f, center.y + sw * 0.2f, center.x, center.y - sw * 0.05f)
        cubicTo(center.x + sw * 0.12f, center.y - sw * 0.22f, center.x + sw * 0.25f, center.y + sw * 0.15f, center.x + sw / 2, center.y - sw * 0.08f)
    }
    drawPath(p, color.copy(alpha = 0.8f), style = Stroke(sw * 0.035f, cap = StrokeCap.Round))
}

private fun DrawScope.folder(center: Offset, fw: Float, fh: Float, fg: Color, bg: Color) {
    val tl = center - Offset(fw / 2, fh / 2)
    drawRoundRect(bg, tl + Offset(0f, -fh * 0.12f), Size(fw * 0.4f, fh * 0.3f), CornerRadius(fw * 0.04f))
    drawRoundRect(bg, tl, Size(fw, fh), CornerRadius(fw * 0.05f))
    drawRoundRect(fg.copy(alpha = 0.25f), tl, Size(fw, fh), CornerRadius(fw * 0.05f), style = Stroke(1.5f))
}

private fun DrawScope.folderFront(center: Offset, fw: Float, fh: Float, fg: Color, bg: Color) {
    val tl = center - Offset(fw / 2, fh / 2) + Offset(0f, fh * 0.35f)
    val size = Size(fw, fh * 0.65f)
    drawRoundRect(Color.Black.copy(alpha = 0.05f), tl + Offset(0f, -fh * 0.03f), size, CornerRadius(fw * 0.05f))
    drawRoundRect(bg, tl, size, CornerRadius(fw * 0.05f))
    drawRoundRect(fg.copy(alpha = 0.35f), tl, size, CornerRadius(fw * 0.05f), style = Stroke(1.5f))
}
