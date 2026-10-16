package com.vidacotidiana.app.core.ui.components

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
 * Qué se dibuja detrás de la cabecera de una ficha.
 *
 * Un motivo por CLASE de registro, no por registro: es una imagen
 * representativa, nunca el contenido real, así que no puede exponer nada
 * sensible a quien mire la pantalla (ADR-028).
 */
enum class HeroMotif {
    RECEIPT, SHIELD, ID_CARD, CONTRACT, FOLDER,
    WARRANTY, BOX, WRENCH, PAYMENT, CHECKLIST, TARGET, ROUTINE, PLACE, LINK, PERSON, PROJECT, COMMITMENT, NOTE,
}

/**
 * LA LÁMINA DE FONDO DE LAS FICHAS — nació en Documentos y se extiende a todas
 * las secciones (ADR-028).
 *
 * Se dibuja con `Canvas` en el tono del registro: sin recursos gráficos, sigue
 * al tema claro/oscuro y no suma peso al APK. Ocupa la mitad derecha y se
 * funde hacia la izquierda y hacia abajo con `fade`, para que el texto que va
 * ENCIMA se lea siempre.
 */
@Composable
fun VidaHeroArt(
    motif: HeroMotif,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    fade: Color = VidaTheme.colors.surface,
) {
    val c = VidaTheme.colors
    val kit = ArtKit(
        paper = c.surface,
        ink = c.border.copy(alpha = 0.30f),
        edge = c.border.copy(alpha = 0.16f),
        accent = tone.fg,
        accentBg = tone.bg,
        green = c.success,
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            Brush.radialGradient(
                listOf(tone.bg, tone.bg.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(w * 0.86f, h * 0.22f),
                radius = w * 0.72f,
            ),
        )

        val sw = minOf(w * 0.34f, h * 0.46f)
        val sh = sw * 1.3f
        kit.draw(this, motif, w, h, sw, sh)

        drawRect(
            Brush.horizontalGradient(
                0f to fade,
                0.28f to fade.copy(alpha = 0.85f),
                0.55f to fade.copy(alpha = 0.15f),
                0.7f to Color.Transparent,
            ),
        )
        drawRect(
            Brush.verticalGradient(
                0.55f to Color.Transparent,
                0.82f to fade.copy(alpha = 0.8f),
                1f to fade,
            ),
        )
    }
}

private class ArtKit(
    val paper: Color,
    val ink: Color,
    val edge: Color,
    val accent: Color,
    val accentBg: Color,
    val green: Color,
) {
    fun draw(s: DrawScope, motif: HeroMotif, w: Float, h: Float, sw: Float, sh: Float) = with(s) {
        val back = Offset(w * 0.84f, h * 0.30f)
        val front = Offset(w * 0.66f, h * 0.38f)
        when (motif) {
            HeroMotif.RECEIPT -> {
                page(this, back, sw, sh, 9f)
                page(this, front, sw, sh, -6f) { c, pw, ph -> bars(c + Offset(pw * 0.16f, ph * 0.3f), pw * 0.36f, ph * 0.18f, green) }
            }
            HeroMotif.SHIELD -> {
                page(this, back, sw, sh, 9f)
                page(this, front, sw, sh, -6f) { c, pw, ph -> shield(c + Offset(pw * 0.18f, ph * 0.2f), pw * 0.34f) }
            }
            HeroMotif.ID_CARD, HeroMotif.PERSON -> {
                page(this, Offset(w * 0.83f, h * 0.30f), sw * 0.9f, sh * 0.9f, 10f)
                idCard(this, Offset(w * 0.66f, h * 0.40f), sw * 1.45f, sw * 0.92f, -6f)
            }
            HeroMotif.CONTRACT -> {
                page(this, Offset(w * 0.84f, h * 0.28f), sw, sh, 10f)
                page(this, Offset(w * 0.75f, h * 0.33f), sw, sh, 3f)
                page(this, Offset(w * 0.64f, h * 0.40f), sw, sh, -7f) { c, pw, ph -> signature(c + Offset(0f, ph * 0.3f), pw * 0.6f) }
            }
            HeroMotif.FOLDER -> {
                val fc = Offset(w * 0.74f, h * 0.44f)
                folderBack(fc, sw * 1.7f, sw * 1.05f)
                page(this, Offset(w * 0.70f, h * 0.30f), sw * 0.9f, sh * 0.8f, -5f)
                page(this, Offset(w * 0.82f, h * 0.32f), sw * 0.9f, sh * 0.8f, 7f)
                folderFront(fc, sw * 1.7f, sw * 1.05f)
            }
            HeroMotif.WARRANTY -> {
                page(this, back, sw, sh, 8f) { c, pw, ph -> bars(c + Offset(pw * 0.16f, ph * 0.32f), pw * 0.3f, ph * 0.12f, accent) }
                shield(Offset(w * 0.64f, h * 0.44f), sw * 0.95f)
            }
            HeroMotif.BOX -> {
                box(Offset(w * 0.72f, h * 0.42f), sw * 1.35f)
                tag(Offset(w * 0.88f, h * 0.2f), sw * 0.5f)
            }
            HeroMotif.WRENCH -> {
                calendarPage(this, back, sw, sh, 8f)
                wrench(Offset(w * 0.64f, h * 0.44f), sw * 1.25f)
            }
            HeroMotif.PAYMENT -> {
                calendarPage(this, back, sw, sh, 9f)
                card(this, Offset(w * 0.66f, h * 0.44f), sw * 1.45f, sw * 0.92f, -8f)
            }
            HeroMotif.CHECKLIST -> {
                page(this, back, sw, sh, 9f)
                page(this, front, sw, sh, -6f, lines = false) { c, pw, ph -> checklist(c, pw, ph) }
            }
            HeroMotif.TARGET -> {
                target(Offset(w * 0.72f, h * 0.38f), sw * 0.8f)
            }
            HeroMotif.ROUTINE -> {
                calendarPage(this, back, sw, sh, 9f)
                cycle(Offset(w * 0.64f, h * 0.42f), sw * 0.6f)
            }
            HeroMotif.PLACE -> {
                map(this, Offset(w * 0.72f, h * 0.38f), sw * 1.6f, sw * 1.15f, -5f)
                pin(Offset(w * 0.72f, h * 0.30f), sw * 0.42f)
            }
            HeroMotif.LINK -> {
                val fc = Offset(w * 0.74f, h * 0.44f)
                folderBack(fc, sw * 1.7f, sw * 1.05f)
                page(this, Offset(w * 0.76f, h * 0.30f), sw * 0.9f, sh * 0.8f, 5f)
                folderFront(fc, sw * 1.7f, sw * 1.05f)
                chain(Offset(w * 0.74f, h * 0.56f), sw * 0.55f)
            }
            HeroMotif.PROJECT -> {
                page(this, back, sw, sh, 8f)
                board(this, Offset(w * 0.66f, h * 0.40f), sw * 1.5f, sw * 1.0f, -5f)
            }
            HeroMotif.COMMITMENT -> {
                calendarPage(this, back, sw, sh, 9f)
                bubbles(Offset(w * 0.64f, h * 0.42f), sw * 0.9f)
            }
            HeroMotif.NOTE -> {
                page(this, back, sw, sh, 9f)
                page(this, front, sw, sh, -6f)
                pencil(Offset(w * 0.6f, h * 0.5f), sw * 1.1f)
            }
        }
    }

    private fun shadow(s: DrawScope, tl: Offset, size: Size, r: CornerRadius, dy: Float) =
        s.drawRoundRect(Color.Black.copy(alpha = 0.07f), tl + Offset(0f, dy), size, r)

    fun page(
        s: DrawScope,
        center: Offset,
        sw: Float,
        sh: Float,
        degrees: Float,
        lines: Boolean = true,
        motif: DrawScope.(Offset, Float, Float) -> Unit = { _, _, _ -> },
    ) = with(s) {
        rotate(degrees, pivot = center) {
            val tl = center - Offset(sw / 2, sh / 2)
            val r = CornerRadius(sw * 0.07f)
            shadow(this, tl, Size(sw, sh), r, sw * 0.04f)
            drawRoundRect(paper, tl, Size(sw, sh), r)
            drawRoundRect(edge, tl, Size(sw, sh), r, style = Stroke(1.2f))
            val pad = sw * 0.12f
            drawRoundRect(accent.copy(alpha = 0.75f), tl + Offset(pad, pad), Size(sw * 0.34f, sh * 0.045f), CornerRadius(sh * 0.02f))
            if (lines) {
                listOf(0.76f, 0.6f, 0.7f, 0.5f, 0.66f).forEachIndexed { i, f ->
                    drawRoundRect(
                        ink, tl + Offset(pad, pad + sh * (0.12f + i * 0.075f)),
                        Size((sw - pad * 2) * f, sh * 0.025f), CornerRadius(sh * 0.0125f),
                    )
                }
            }
            motif(center, sw, sh)
        }
    }

    fun calendarPage(s: DrawScope, center: Offset, sw: Float, sh: Float, degrees: Float) = with(s) {
        rotate(degrees, pivot = center) {
            val tl = center - Offset(sw / 2, sh / 2)
            val r = CornerRadius(sw * 0.07f)
            shadow(this, tl, Size(sw, sh), r, sw * 0.04f)
            drawRoundRect(paper, tl, Size(sw, sh), r)
            drawRoundRect(edge, tl, Size(sw, sh), r, style = Stroke(1.2f))
            drawRoundRect(accent.copy(alpha = 0.8f), tl, Size(sw, sh * 0.2f), r)
            drawRect(accent.copy(alpha = 0.8f), tl + Offset(0f, sh * 0.1f), Size(sw, sh * 0.1f))
            val cols = 4
            val rows = 4
            val gx = sw * 0.8f / cols
            val gy = sh * 0.66f / rows
            for (row in 0 until rows) for (col in 0 until cols) {
                val o = tl + Offset(sw * 0.1f + gx * (col + 0.5f), sh * 0.28f + gy * (row + 0.5f))
                val hot = row == 1 && col == 2
                drawCircle(if (hot) accent else ink, radius = if (hot) sw * 0.07f else sw * 0.045f, center = o)
            }
        }
    }

    fun idCard(s: DrawScope, center: Offset, cw: Float, ch: Float, degrees: Float) = with(s) {
        rotate(degrees, pivot = center) {
            val tl = center - Offset(cw / 2, ch / 2)
            val r = CornerRadius(cw * 0.06f)
            shadow(this, tl, Size(cw, ch), r, cw * 0.03f)
            drawRoundRect(paper, tl, Size(cw, ch), r)
            drawRoundRect(edge, tl, Size(cw, ch), r, style = Stroke(1.2f))
            drawRoundRect(accentBg, tl, Size(cw, ch * 0.2f), r)
            val photoTL = tl + Offset(cw * 0.08f, ch * 0.3f)
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
                drawRoundRect(ink, tl + Offset(cw * 0.44f, ch * (0.34f + i * 0.13f)), Size(cw * f, ch * 0.05f), CornerRadius(ch * 0.025f))
            }
        }
    }

    fun card(s: DrawScope, center: Offset, cw: Float, ch: Float, degrees: Float) = with(s) {
        rotate(degrees, pivot = center) {
            val tl = center - Offset(cw / 2, ch / 2)
            val r = CornerRadius(cw * 0.07f)
            shadow(this, tl, Size(cw, ch), r, cw * 0.03f)
            drawRoundRect(
                Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f)), tl, tl + Offset(cw, ch)),
                tl, Size(cw, ch), r,
            )
            drawRect(Color.Black.copy(alpha = 0.18f), tl + Offset(0f, ch * 0.2f), Size(cw, ch * 0.14f))
            drawRoundRect(Color(0xFFFCD34D), tl + Offset(cw * 0.1f, ch * 0.45f), Size(cw * 0.16f, ch * 0.2f), CornerRadius(cw * 0.02f))
            listOf(0.14f, 0.14f, 0.14f).forEachIndexed { i, f ->
                drawRoundRect(
                    Color.White.copy(alpha = 0.75f), tl + Offset(cw * (0.1f + i * 0.2f), ch * 0.76f),
                    Size(cw * f, ch * 0.06f), CornerRadius(ch * 0.03f),
                )
            }
        }
    }

    fun map(s: DrawScope, center: Offset, mw: Float, mh: Float, degrees: Float) = with(s) {
        rotate(degrees, pivot = center) {
            val tl = center - Offset(mw / 2, mh / 2)
            val r = CornerRadius(mw * 0.05f)
            shadow(this, tl, Size(mw, mh), r, mw * 0.03f)
            drawRoundRect(paper, tl, Size(mw, mh), r)
            drawRoundRect(edge, tl, Size(mw, mh), r, style = Stroke(1.2f))
            drawCircle(green.copy(alpha = 0.18f), mw * 0.16f, tl + Offset(mw * 0.22f, mh * 0.7f))
            val road = Path().apply {
                moveTo(tl.x, tl.y + mh * 0.62f)
                cubicTo(tl.x + mw * 0.3f, tl.y + mh * 0.4f, tl.x + mw * 0.6f, tl.y + mh * 0.85f, tl.x + mw, tl.y + mh * 0.5f)
            }
            drawPath(road, ink, style = Stroke(mw * 0.035f, cap = StrokeCap.Round))
            val road2 = Path().apply {
                moveTo(tl.x + mw * 0.55f, tl.y)
                cubicTo(tl.x + mw * 0.45f, tl.y + mh * 0.4f, tl.x + mw * 0.7f, tl.y + mh * 0.7f, tl.x + mw * 0.62f, tl.y + mh)
            }
            drawPath(road2, ink, style = Stroke(mw * 0.025f, cap = StrokeCap.Round))
        }
    }

    fun board(s: DrawScope, center: Offset, bw: Float, bh: Float, degrees: Float) = with(s) {
        rotate(degrees, pivot = center) {
            val tl = center - Offset(bw / 2, bh / 2)
            val r = CornerRadius(bw * 0.05f)
            shadow(this, tl, Size(bw, bh), r, bw * 0.03f)
            drawRoundRect(paper, tl, Size(bw, bh), r)
            drawRoundRect(edge, tl, Size(bw, bh), r, style = Stroke(1.2f))
            val colW = bw * 0.26f
            listOf(3, 2, 1).forEachIndexed { col, n ->
                val x = tl.x + bw * 0.06f + col * (colW + bw * 0.05f)
                drawRoundRect(accentBg, Offset(x, tl.y + bh * 0.08f), Size(colW, bh * 0.84f), CornerRadius(bw * 0.03f))
                repeat(n) { i ->
                    drawRoundRect(
                        if (col == 2) green.copy(alpha = 0.7f) else paper,
                        Offset(x + colW * 0.1f, tl.y + bh * (0.16f + i * 0.24f)),
                        Size(colW * 0.8f, bh * 0.18f), CornerRadius(bw * 0.02f),
                    )
                }
            }
        }
    }

    fun DrawScope.checklist(center: Offset, pw: Float, ph: Float) {
        val tl = center - Offset(pw / 2, ph / 2)
        repeat(4) { i ->
            val y = tl.y + ph * (0.26f + i * 0.17f)
            val cx = tl.x + pw * 0.2f
            val done = i < 2
            drawCircle(if (done) accent else ink, pw * 0.065f, Offset(cx, y), style = if (done) androidx.compose.ui.graphics.drawscope.Fill else Stroke(2f))
            if (done) {
                val tick = Path().apply {
                    moveTo(cx - pw * 0.03f, y); lineTo(cx - pw * 0.005f, y + pw * 0.025f); lineTo(cx + pw * 0.035f, y - pw * 0.025f)
                }
                drawPath(tick, paper, style = Stroke(pw * 0.018f, cap = StrokeCap.Round))
            }
            drawRoundRect(ink, Offset(cx + pw * 0.12f, y - ph * 0.0125f), Size(pw * (if (i % 2 == 0) 0.5f else 0.38f), ph * 0.025f), CornerRadius(ph * 0.0125f))
        }
    }

    fun DrawScope.bars(origin: Offset, bw: Float, bh: Float, color: Color) {
        val heights = listOf(0.35f, 0.6f, 0.45f, 0.8f, 1f)
        val gap = bw / heights.size
        heights.forEachIndexed { i, f ->
            drawRoundRect(
                color.copy(alpha = 0.55f + 0.09f * i),
                Offset(origin.x - bw / 2 + i * gap, origin.y - bh * f),
                Size(gap * 0.62f, bh * f), CornerRadius(gap * 0.15f),
            )
        }
    }

    fun DrawScope.shield(center: Offset, s: Float) {
        val p = Path().apply {
            moveTo(center.x, center.y - s * 0.55f)
            lineTo(center.x + s * 0.45f, center.y - s * 0.35f)
            quadraticTo(center.x + s * 0.45f, center.y + s * 0.3f, center.x, center.y + s * 0.6f)
            quadraticTo(center.x - s * 0.45f, center.y + s * 0.3f, center.x - s * 0.45f, center.y - s * 0.35f)
            close()
        }
        drawPath(p, Color.Black.copy(alpha = 0.06f), style = Stroke(s * 0.16f))
        drawPath(p, accentBg)
        drawPath(p, accent.copy(alpha = 0.8f), style = Stroke(s * 0.07f))
        val tick = Path().apply {
            moveTo(center.x - s * 0.18f, center.y); lineTo(center.x - s * 0.03f, center.y + s * 0.15f); lineTo(center.x + s * 0.2f, center.y - s * 0.12f)
        }
        drawPath(tick, accent, style = Stroke(s * 0.08f, cap = StrokeCap.Round))
    }

    fun DrawScope.signature(center: Offset, sw: Float) {
        val p = Path().apply {
            moveTo(center.x - sw / 2, center.y)
            cubicTo(center.x - sw * 0.3f, center.y - sw * 0.25f, center.x - sw * 0.15f, center.y + sw * 0.2f, center.x, center.y - sw * 0.05f)
            cubicTo(center.x + sw * 0.12f, center.y - sw * 0.22f, center.x + sw * 0.25f, center.y + sw * 0.15f, center.x + sw / 2, center.y - sw * 0.08f)
        }
        drawPath(p, accent.copy(alpha = 0.8f), style = Stroke(sw * 0.035f, cap = StrokeCap.Round))
    }

    fun DrawScope.folderBack(center: Offset, fw: Float, fh: Float) {
        val tl = center - Offset(fw / 2, fh / 2)
        drawRoundRect(accentBg, tl + Offset(0f, -fh * 0.12f), Size(fw * 0.4f, fh * 0.3f), CornerRadius(fw * 0.04f))
        drawRoundRect(accentBg, tl, Size(fw, fh), CornerRadius(fw * 0.05f))
        drawRoundRect(accent.copy(alpha = 0.25f), tl, Size(fw, fh), CornerRadius(fw * 0.05f), style = Stroke(1.5f))
    }

    fun DrawScope.folderFront(center: Offset, fw: Float, fh: Float) {
        val tl = center - Offset(fw / 2, fh / 2) + Offset(0f, fh * 0.35f)
        val size = Size(fw, fh * 0.65f)
        drawRoundRect(Color.Black.copy(alpha = 0.05f), tl + Offset(0f, -fh * 0.03f), size, CornerRadius(fw * 0.05f))
        drawRoundRect(accentBg, tl, size, CornerRadius(fw * 0.05f))
        drawRoundRect(accent.copy(alpha = 0.35f), tl, size, CornerRadius(fw * 0.05f), style = Stroke(1.5f))
    }

    /** Caja en perspectiva: frente, lateral y tapa, con su cinta. */
    fun DrawScope.box(center: Offset, s: Float) {
        val d = s * 0.28f
        val front = center - Offset(s * 0.5f, s * 0.3f)
        val top = Path().apply {
            moveTo(front.x, front.y); lineTo(front.x + d, front.y - d * 0.6f)
            lineTo(front.x + s * 0.8f + d, front.y - d * 0.6f); lineTo(front.x + s * 0.8f, front.y); close()
        }
        val side = Path().apply {
            moveTo(front.x + s * 0.8f, front.y); lineTo(front.x + s * 0.8f + d, front.y - d * 0.6f)
            lineTo(front.x + s * 0.8f + d, front.y + s * 0.6f - d * 0.6f); lineTo(front.x + s * 0.8f, front.y + s * 0.6f); close()
        }
        drawRoundRect(Color.Black.copy(alpha = 0.07f), front + Offset(0f, s * 0.05f), Size(s * 0.8f, s * 0.6f), CornerRadius(s * 0.03f))
        drawRoundRect(accentBg, front, Size(s * 0.8f, s * 0.6f), CornerRadius(s * 0.03f))
        drawPath(side, accent.copy(alpha = 0.35f))
        drawPath(top, accent.copy(alpha = 0.22f))
        drawRect(accent.copy(alpha = 0.55f), front + Offset(s * 0.33f, 0f), Size(s * 0.14f, s * 0.6f))
        drawRoundRect(paper, front + Offset(s * 0.08f, s * 0.36f), Size(s * 0.2f, s * 0.14f), CornerRadius(s * 0.02f))
    }

    fun DrawScope.tag(center: Offset, s: Float) {
        val p = Path().apply {
            moveTo(center.x - s * 0.5f, center.y - s * 0.3f); lineTo(center.x + s * 0.25f, center.y - s * 0.3f)
            lineTo(center.x + s * 0.5f, center.y); lineTo(center.x + s * 0.25f, center.y + s * 0.3f)
            lineTo(center.x - s * 0.5f, center.y + s * 0.3f); close()
        }
        drawPath(p, paper)
        drawPath(p, accent.copy(alpha = 0.6f), style = Stroke(2f))
        drawCircle(accent, s * 0.07f, Offset(center.x + s * 0.22f, center.y))
    }

    fun DrawScope.wrench(center: Offset, s: Float) {
        rotate(-40f, pivot = center) {
            drawRoundRect(Color.Black.copy(alpha = 0.06f), center - Offset(s * 0.07f, s * 0.05f - s * 0.03f), Size(s * 0.14f, s * 0.6f), CornerRadius(s * 0.07f))
            drawRoundRect(accent.copy(alpha = 0.85f), center - Offset(s * 0.07f, s * 0.05f), Size(s * 0.14f, s * 0.6f), CornerRadius(s * 0.07f))
            val head = center - Offset(0f, s * 0.12f)
            drawCircle(accent.copy(alpha = 0.85f), s * 0.19f, head)
            drawRoundRect(paper, head - Offset(s * 0.06f, s * 0.24f), Size(s * 0.12f, s * 0.22f), CornerRadius(s * 0.03f))
        }
    }

    fun DrawScope.target(center: Offset, r: Float) {
        drawCircle(Color.Black.copy(alpha = 0.05f), r * 1.04f, center + Offset(0f, r * 0.06f))
        listOf(1f to paper, 0.78f to accentBg, 0.56f to paper, 0.34f to accent.copy(alpha = 0.85f)).forEach { (f, col) ->
            drawCircle(col, r * f, center)
        }
        drawCircle(edge, r, center, style = Stroke(1.5f))
        val tip = center + Offset(r * 0.05f, -r * 0.05f)
        drawLine(accent, tip, tip + Offset(r * 0.9f, -r * 0.9f), strokeWidth = r * 0.06f, cap = StrokeCap.Round)
        drawLine(green, tip + Offset(r * 0.75f, -r * 0.75f), tip + Offset(r * 0.95f, -r * 0.6f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
        drawLine(green, tip + Offset(r * 0.75f, -r * 0.75f), tip + Offset(r * 0.6f, -r * 0.95f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
    }

    fun DrawScope.cycle(center: Offset, r: Float) {
        drawCircle(paper, r * 1.15f, center)
        drawCircle(edge, r * 1.15f, center, style = Stroke(1.5f))
        drawArc(
            accent, -60f, 290f, false,
            topLeft = center - Offset(r * 0.7f, r * 0.7f), size = Size(r * 1.4f, r * 1.4f),
            style = Stroke(r * 0.16f, cap = StrokeCap.Round),
        )
        val a = Math.toRadians(-60.0)
        val tip = center + Offset((r * 0.7f * Math.cos(a)).toFloat(), (r * 0.7f * Math.sin(a)).toFloat())
        val head = Path().apply {
            moveTo(tip.x - r * 0.25f, tip.y - r * 0.05f); lineTo(tip.x + r * 0.12f, tip.y - r * 0.2f); lineTo(tip.x + r * 0.05f, tip.y + r * 0.2f); close()
        }
        drawPath(head, accent)
    }

    fun DrawScope.pin(center: Offset, s: Float) {
        val p = Path().apply {
            moveTo(center.x, center.y + s * 0.9f)
            cubicTo(center.x - s * 0.9f, center.y + s * 0.1f, center.x - s * 0.55f, center.y - s * 0.6f, center.x, center.y - s * 0.6f)
            cubicTo(center.x + s * 0.55f, center.y - s * 0.6f, center.x + s * 0.9f, center.y + s * 0.1f, center.x, center.y + s * 0.9f)
            close()
        }
        drawOval(Color.Black.copy(alpha = 0.1f), center + Offset(-s * 0.3f, s * 0.85f), Size(s * 0.6f, s * 0.16f))
        drawPath(p, accent)
        drawCircle(paper, s * 0.22f, center - Offset(0f, s * 0.08f))
    }

    fun DrawScope.chain(center: Offset, s: Float) {
        rotate(-30f, pivot = center) {
            val size = Size(s * 0.8f, s * 0.42f)
            drawRoundRect(accent, center - Offset(s * 0.7f, s * 0.21f), size, CornerRadius(s * 0.21f), style = Stroke(s * 0.1f))
            drawRoundRect(green, center - Offset(s * 0.1f, s * 0.21f), size, CornerRadius(s * 0.21f), style = Stroke(s * 0.1f))
        }
    }

    fun DrawScope.bubbles(center: Offset, s: Float) {
        val a = center - Offset(s * 0.55f, s * 0.35f)
        drawRoundRect(Color.Black.copy(alpha = 0.06f), a + Offset(0f, s * 0.04f), Size(s * 0.8f, s * 0.5f), CornerRadius(s * 0.18f))
        drawRoundRect(paper, a, Size(s * 0.8f, s * 0.5f), CornerRadius(s * 0.18f))
        drawRoundRect(edge, a, Size(s * 0.8f, s * 0.5f), CornerRadius(s * 0.18f), style = Stroke(1.5f))
        repeat(3) { i -> drawCircle(ink, s * 0.045f, a + Offset(s * (0.22f + i * 0.18f), s * 0.25f)) }
        val b = center - Offset(s * 0.05f, -s * 0.05f)
        drawRoundRect(accent.copy(alpha = 0.9f), b, Size(s * 0.7f, s * 0.44f), CornerRadius(s * 0.16f))
        val tick = Path().apply {
            moveTo(b.x + s * 0.24f, b.y + s * 0.22f); lineTo(b.x + s * 0.32f, b.y + s * 0.3f); lineTo(b.x + s * 0.47f, b.y + s * 0.14f)
        }
        drawPath(tick, paper, style = Stroke(s * 0.05f, cap = StrokeCap.Round))
    }

    fun DrawScope.pencil(center: Offset, s: Float) {
        rotate(-35f, pivot = center) {
            drawRoundRect(accent.copy(alpha = 0.9f), center - Offset(s * 0.4f, s * 0.06f), Size(s * 0.62f, s * 0.12f), CornerRadius(s * 0.02f))
            drawRect(Color(0xFFF9A8D4), center - Offset(s * 0.48f, s * 0.06f), Size(s * 0.08f, s * 0.12f))
            val tip = Path().apply {
                moveTo(center.x + s * 0.22f, center.y - s * 0.06f); lineTo(center.x + s * 0.36f, center.y); lineTo(center.x + s * 0.22f, center.y + s * 0.06f); close()
            }
            drawPath(tip, Color(0xFFFDE68A))
        }
    }
}
