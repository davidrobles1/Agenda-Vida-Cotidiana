package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.brand.ButterflyMark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ÁNIMO v3 · «QUE DEN GANAS DE CONTARLO» (artefacto «Inicio en cinco zonas»,
 * pestaña Ánimo, aprobado 2026-09-27).
 *
 * La v2 hacía del ánimo un campo de formulario —misma fila y mismo selector que
 * un calendario, caras de trazo—: coherente pero frío. La v3 conserva el
 * sistema (tarjeta de radio 24, título de zona, azul tinta, mariposa) y le
 * devuelve la emoción:
 *
 *  · La pregunta lleva tu nombre y va en Fraunces, la voz de la marca.
 *  · Cinco caritas rellenas, de durazno (genial) a lavanda (bajo). Mientras no
 *    eliges, respiran en ola.
 *  · Al elegir: la carita salta, la mariposa del icono vuela y se posa encima,
 *    la tarjeta toma el tono del ánimo y la app responde con una frase.
 *  · La celebración es proporcional: Bajo y Regular reciben un halo suave;
 *    Normal, cinco facetas; Bien y Genial, doce facetas de la marca.
 *  · «Reducir movimiento» del sistema apaga todas las animaciones.
 *
 * El dato es el mismo de siempre: `setMood(valor)` con la escala de
 * `MoodScale` (0 genial … 4 bajo). «Cambiar» no borra nada en el servidor:
 * vuelve a abrir la elección y el valor guardado sigue hasta que eliges otro.
 */

/** Paleta y respuesta de cada ánimo, en el orden de `MoodScale` (0 genial … 4 bajo). */
private class MoodLook(
    val face: Color,
    val deep: Color,
    val bg: Color,
    val bgNight: Color,
    val reply: String,
    val fx: Fx,
)

private enum class Fx { GLOW, FEW, BURST }

/*
 * Las respuestas son las del artefacto y solo viven en Inicio. `MoodScale.ECHOES`
 * sigue siendo el texto de Bienestar: allí describen el día, aquí le contestan
 * a la persona.
 */
private val LOOKS = listOf(
    MoodLook(Color(0xFFFFC3AB), Color(0xFF83341B), Color(0xFFFFECE4), Color(0x24FFC3AB),
        "¡Se nota desde aquí! Que el día te siga sonriendo.", Fx.BURST),
    MoodLook(Color(0xFFFFD4AC), Color(0xFF7E4210), Color(0xFFFFF1E3), Color(0x21FFD4AC),
        "¡Qué bueno! Aprovechemos esa energía.", Fx.BURST),
    MoodLook(Color(0xFFF5E4B4), Color(0xFF6E520F), Color(0xFFFBF5E4), Color(0x1FF5E4B4),
        "Un día tranquilo también cuenta. Lo llevamos ligero.", Fx.FEW),
    MoodLook(Color(0xFFBFDDF6), Color(0xFF24568A), Color(0xFFEAF4FC), Color(0x247DC2ED),
        "Está bien no estar al cien. Hoy te dejo solo lo justo.", Fx.GLOW),
    MoodLook(Color(0xFFCBC7F4), Color(0xFF3B3790), Color(0xFFEFEEFD), Color(0x297C74F0),
        "Gracias por contármelo. Hoy vamos despacio, un paso a la vez.", Fx.GLOW),
)

private val BURST_COLORS = listOf(Color(0xFF7DC2ED), Color(0xFFCFEAFB), Color(0xFF4A4DA0))

private data class MoodBit(val angle: Float, val dist: Float, val rot: Float, val shape: Int, val color: Color)
private data class MoodBurst(val value: Int, val key: Int, val particles: List<MoodBit>)

/**
 * El azul tinta de la marca (estructura, selección, contadores). En Noche se
 * aclara para no hundirse en el fondo. Lo usan Inicio y Bienestar.
 */
@Composable
fun vidaBrandInk(): Color = if (VidaTheme.spec.isDark) Color(0xFFC9C6FA) else Color(0xFF2B2870)

/** Acento de cada ánimo (disco de la carita), para piezas que lo necesiten fuera de la tarjeta. */
fun moodFaceColor(value: Int): Color = LOOKS[value.coerceIn(0, 4)].face

/** Respuesta de la app a cada ánimo. */
fun moodReply(value: Int): String = LOOKS[value.coerceIn(0, 4)].reply

/**
 * La tarjeta de ánimo. En Inicio va compacta; en Bienestar (`hero = true`)
 * añade la cara grande flotando sobre su halo, que es la pieza central de
 * esa pantalla. `onOpenWellbeing` nulo = ya estamos en Bienestar y el pie no
 * ofrece ir a él.
 */
@Composable
fun VidaMoodCard(
    name: String?,
    marked: Int?,
    loading: Boolean,
    failed: Boolean,
    onMark: (Int) -> Unit,
    onOpenWellbeing: (() -> Unit)?,
    hero: Boolean = false,
) {
    val c = VidaTheme.colors
    val dark = VidaTheme.spec.isDark
    val ink = vidaBrandInk()
    val motion = motionEnabled()
    val haptics = LocalHapticFeedback.current

    // «Cambiar» reabre la elección sin tocar el dato guardado.
    var choosing by remember { mutableStateOf(false) }
    val shown = if (choosing) null else marked
    var celebration by remember { mutableStateOf<MoodBurst?>(null) }
    var popKey by remember { mutableIntStateOf(0) }

    fun pick(v: Int) {
        choosing = false
        if (v == marked) return
        onMark(v)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        popKey++
        if (motion) {
            val look = LOOKS[v]
            val n = when (look.fx) { Fx.GLOW -> 0; Fx.FEW -> 5; Fx.BURST -> 12 }
            val palette = listOf(look.face) + BURST_COLORS + look.deep
            celebration = MoodBurst(v, popKey, List(n) { i ->
                MoodBit(
                    angle = (-160f + Random.nextFloat() * 140f) * (PI.toFloat() / 180f),
                    dist = 40f + Random.nextFloat() * 50f,
                    rot = Random.nextFloat() * 240f - 120f,
                    shape = i % 2,
                    color = palette[i % palette.size],
                )
            })
        }
    }

    val tint by animateColorAsState(
        when {
            shown == null -> if (dark) Color.Transparent else c.surfaceVariant
            dark -> LOOKS[shown].bgNight
            else -> LOOKS[shown].bg
        },
        animationSpec = tween(if (motion) 700 else 0),
        label = "moodTint",
    )
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceVariant)
            .background(tint)
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.88f to c.surfaceVariant))
            .border(1.dp, c.line, shape)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
    ) {
        // La pregunta, en la voz de la marca.
        Column(Modifier.padding(end = 56.dp).heightIn(min = 52.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                name?.let { "¿Cómo llegas hoy, $it?" } ?: "¿Cómo llegas hoy?",
                style = TextStyle(fontFamily = VidaFonts.FrauncesWordmark, fontWeight = FontWeight.Light, fontSize = 21.sp, lineHeight = 25.sp),
                color = ink,
            )
            Text(
                when {
                    shown != null -> "Lo guardé para ti. Puedes cambiarlo cuando quieras."
                    failed && marked == null -> "No pudimos consultarlo. Toca una carita para marcarlo."
                    loading && marked == null -> "Un momento…"
                    else -> "Un toque basta. Solo tú lo ves."
                },
                style = VidaTheme.type.micro.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                color = c.textSecondary,
            )
        }

        if (hero) {
            // La cara grande: la del ánimo marcado, flotando sobre su halo. Sin
            // marcar, la mariposa espera en su lugar — nunca una cara que
            // aparente un ánimo que no se eligió.
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                if (shown != null) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .then(if (motion) Modifier.vidaHalo() else Modifier)
                            .blur(22.dp)
                            .background(LOOKS[shown].face.copy(alpha = 0.55f), CircleShape),
                    )
                    AnimatedContent(
                        targetState = shown,
                        transitionSpec = { fadeIn(tween(if (motion) 400 else 0)) togetherWith fadeOut(tween(if (motion) 200 else 0)) },
                        label = "bigFace",
                    ) { v ->
                        MoodFilledFace(
                            v,
                            Modifier.size(120.dp).then(if (motion) Modifier.vidaFloat(7.dp, 5200, rotation = 1.2f) else Modifier),
                        )
                    }
                } else {
                    ButterflyMark(
                        size = 96.dp,
                        modifier = if (motion) Modifier.vidaFloat(7.dp, 5200, rotation = 1.2f) else Modifier,
                    )
                }
            }
        }

        // Las cinco caritas, con la mariposa y la celebración encima.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            val slot = maxWidth / 5
            val breathe = rememberInfiniteTransition(label = "breathe")
            val phase by breathe.animateFloat(
                0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "phase",
            )

            Row(Modifier.fillMaxWidth()) {
                (0..4).forEach { v ->
                    FaceButton(
                        value = v,
                        selected = shown == v,
                        dimmed = shown != null && shown != v,
                        breatheScale = if (motion && shown == null) {
                            1f + 0.025f * (1f - cos(2f * PI.toFloat() * (phase - v * 0.109f)))
                        } else 1f,
                        popKey = if (shown == v) popKey else -1,
                        motion = motion,
                        onClick = { pick(v) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Celebración: halo o facetas que salen de la carita elegida.
            celebration?.let { cel ->
                val progress = remember(cel.key) { Animatable(0f) }
                LaunchedEffect(cel.key) {
                    progress.animateTo(1f, tween(if (cel.particles.isEmpty()) 1400 else 1100, easing = FastOutSlowInEasing))
                    if (celebration?.key == cel.key) celebration = null
                }
                val look = LOOKS[cel.value]
                Canvas(Modifier.matchParentSize()) {
                    val origin = Offset((slot * (cel.value + 0.5f)).toPx(), 31.dp.toPx())
                    val p = progress.value
                    if (cel.particles.isEmpty()) {
                        val r = 35.dp.toPx() * (0.6f + 1.3f * p)
                        drawCircle(
                            Brush.radialGradient(listOf(look.face, Color.Transparent), center = origin, radius = r),
                            radius = r, center = origin, alpha = 0.7f * (1f - p),
                        )
                    } else {
                        cel.particles.forEach { part -> drawFacet(part, origin, p) }
                    }
                }
            }

            // La mariposa del icono: espera arriba a la derecha y se posa sobre la elegida.
            val flyX by animateDpAsState(
                if (shown == null) maxWidth - 26.dp else slot * (shown + 0.5f),
                tween(if (motion) 900 else 0, easing = FastOutSlowInEasing), label = "flyX",
            )
            val flyY by animateDpAsState(
                if (shown == null) (-66).dp else (-22).dp,
                tween(if (motion) 900 else 0, easing = FastOutSlowInEasing), label = "flyY",
            )
            val wings = rememberInfiniteTransition(label = "wings")
            val flap by wings.animateFloat(
                1f, 0.5f,
                infiniteRepeatable(tween(if (shown == null) 500 else 1600), RepeatMode.Reverse), label = "flap",
            )
            val bob by wings.animateFloat(
                0f, -3f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "bob",
            )
            if (!hero || shown != null) ButterflyMark(
                size = 34.dp,
                modifier = Modifier
                    .offset(x = flyX - 17.dp, y = flyY)
                    .graphicsLayer {
                        if (motion) {
                            scaleX = flap
                            translationY = bob * density
                        }
                    },
            )
        }

        // La respuesta, o la privacidad dicha en humano.
        Box(Modifier.padding(top = 14.dp).fillMaxWidth().background(c.line).padding(top = 1.dp))
        AnimatedContent(
            targetState = shown,
            transitionSpec = { fadeIn(tween(if (motion) 500 else 0)) togetherWith fadeOut(tween(if (motion) 200 else 0)) },
            label = "reply",
        ) { v ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (v == null) {
                    Row(
                        if (onOpenWellbeing != null) Modifier.vidaClickable(onClick = onOpenWellbeing) else Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(12.dp))
                        Text(
                            if (onOpenWellbeing != null) "Solo tú lo ves · Tu semana en Bienestar ›" else "Solo tú lo ves",
                            style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                            color = c.textSecondary,
                        )
                    }
                } else {
                    Text(
                        LOOKS[v].reply,
                        style = TextStyle(fontFamily = VidaFonts.FrauncesWordmark, fontWeight = FontWeight.Light, fontSize = 15.5.sp, lineHeight = 21.sp),
                        color = c.text,
                        modifier = Modifier.weight(1f).then(
                            if (onOpenWellbeing != null) Modifier.vidaClickable(onClick = onOpenWellbeing) else Modifier,
                        ),
                    )
                    Text(
                        "Cambiar",
                        style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.primary,
                        modifier = Modifier.vidaClickable(onClick = { choosing = true }).padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** Una carita: 56 dp de área táctil, disco de 46 dp y su nombre debajo. */
@Composable
private fun FaceButton(
    value: Int,
    selected: Boolean,
    dimmed: Boolean,
    breatheScale: Float,
    popKey: Int,
    motion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = VidaTheme.colors
    val ring = vidaBrandInk()
    val card = c.surfaceVariant
    val label = MoodScale.LABELS[value]
    val pop = remember { Animatable(1f) }
    LaunchedEffect(popKey) {
        if (popKey < 0) { pop.snapTo(1f); return@LaunchedEffect }
        // 0 = llegó marcado del servidor: ya está elegido, no se celebra.
        if (!motion || popKey == 0) { pop.snapTo(1.08f); return@LaunchedEffect }
        pop.snapTo(1f)
        pop.animateTo(1.22f, tween(240))
        pop.animateTo(1.08f, spring(dampingRatio = 0.45f, stiffness = 400f))
    }
    val scale = if (selected) pop.value else breatheScale
    val alpha = if (dimmed) (if (VidaTheme.spec.isDark) 0.72f else 0.55f) else 1f

    Column(
        modifier
            .alpha(alpha)
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Me siento ${label.lowercase()}"
                this.selected = selected
            }
            .vidaClickable(onClick = onClick)
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .drawBehind {
                    if (selected) {
                        val r = size.minDimension / 2f
                        drawCircle(card, radius = r + 2.5.dp.toPx(), style = Stroke(2.5.dp.toPx()))
                        drawCircle(ring, radius = r + 3.5.dp.toPx(), style = Stroke(2.dp.toPx()))
                    }
                },
            contentAlignment = Alignment.Center,
        ) { MoodFilledFace(value, Modifier.size(44.dp)) }
        Text(
            label,
            style = VidaTheme.type.micro.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = if (selected) c.text else c.textSecondary,
            maxLines = 1,
        )
    }
}

/**
 * La carita rellena del artefacto, en su lienzo de 48: disco de color, brillo,
 * mejillas, cejas, ojos y boca. `value` en la escala de `MoodScale`.
 */
@Composable
fun MoodFilledFace(value: Int, modifier: Modifier) {
    val look = LOOKS[value.coerceIn(0, 4)]
    val k = 4 - value.coerceIn(0, 4) // 0 bajo … 4 genial, como el artefacto
    Canvas(modifier) {
        val u = size.minDimension / 48f
        fun o(x: Float, y: Float) = Offset(x * u, y * u)
        val d = look.deep
        drawCircle(look.face, radius = 22f * u, center = o(24f, 24f))
        // Brillo.
        drawArc(
            Color.White.copy(alpha = 0.45f), 203f, 84f, false,
            topLeft = o(6f, 6f), size = Size(36f * u, 36f * u),
            style = Stroke(2.5f * u, cap = StrokeCap.Round),
        )
        if (k >= 2) {
            val a = if (k == 2) 0.18f else 0.32f
            listOf(13f, 35f).forEach { cx ->
                drawOval(Color(0xFFF28B82).copy(alpha = a), topLeft = o(cx - 3.2f, 25f), size = Size(6.4f * u, 4f * u))
            }
        }
        when (k) {
            0 -> {
                drawLine(d, o(14.5f, 15.5f), o(20.5f, 13.5f), 2f * u, StrokeCap.Round)
                drawLine(d, o(33.5f, 15.5f), o(27.5f, 13.5f), 2f * u, StrokeCap.Round)
            }
            1 -> {
                drawLine(d.copy(alpha = 0.7f), o(15f, 15f), o(21f, 15f), 1.8f * u, StrokeCap.Round)
                drawLine(d.copy(alpha = 0.7f), o(27f, 15f), o(33f, 15f), 1.8f * u, StrokeCap.Round)
            }
        }
        if (k == 4) {
            listOf(15f, 26f).forEach { x ->
                drawPath(Path().apply { moveTo(x * u, 21f * u); quadraticTo((x + 3.5f) * u, 17f * u, (x + 7f) * u, 21f * u) },
                    d, style = Stroke(2.6f * u, cap = StrokeCap.Round))
            }
        } else {
            val ry = if (k == 3) 2.4f else 2.8f
            listOf(18.5f, 29.5f).forEach { cx ->
                drawOval(d, topLeft = o(cx - 2.4f, 20.5f - ry), size = Size(4.8f * u, 2 * ry * u))
                drawCircle(Color.White, radius = 0.8f * u, center = o(cx + 0.8f, 19.5f))
            }
        }
        val stroke = Stroke(2.6f * u, cap = StrokeCap.Round)
        when (k) {
            0 -> drawPath(Path().apply { moveTo(17.5f * u, 33.5f * u); quadraticTo(24f * u, 28f * u, 30.5f * u, 33.5f * u) }, d, style = stroke)
            1 -> drawPath(Path().apply {
                moveTo(17.5f * u, 31.5f * u)
                quadraticTo(20.7f * u, 29.5f * u, 24f * u, 31.5f * u)
                quadraticTo(27.3f * u, 33.5f * u, 30.5f * u, 31.5f * u)
            }, d, style = Stroke(2.4f * u, cap = StrokeCap.Round))
            2 -> drawLine(d, o(18.5f, 31f), o(29.5f, 31f), 2.6f * u, StrokeCap.Round)
            3 -> drawPath(Path().apply { moveTo(16.5f * u, 28.5f * u); quadraticTo(24f * u, 35.5f * u, 31.5f * u, 28.5f * u) }, d, style = stroke)
            else -> {
                drawPath(Path().apply {
                    moveTo(15.5f * u, 27f * u); lineTo(32.5f * u, 27f * u)
                    quadraticTo(31.5f * u, 36.5f * u, 24f * u, 36.5f * u)
                    quadraticTo(16.5f * u, 36.5f * u, 15.5f * u, 27f * u); close()
                }, d)
                drawPath(Path().apply {
                    moveTo(19.5f * u, 33.2f * u)
                    quadraticTo(24f * u, 30.2f * u, 28.5f * u, 33.2f * u)
                    quadraticTo(27f * u, 36.5f * u, 24f * u, 36.5f * u)
                    quadraticTo(21f * u, 36.5f * u, 19.5f * u, 33.2f * u); close()
                }, Color(0xFFF08A7E))
            }
        }
    }
}

/** Una faceta de la celebración: sale disparada, gira, se encoge y se apaga. */
private fun DrawScope.drawFacet(part: MoodBit, origin: Offset, p: Float) {
    val eased = 1f - (1f - p) * (1f - p)
    val center = origin + Offset(cos(part.angle) * part.dist * eased, sin(part.angle) * part.dist * eased) * density
    val s = 10.dp.toPx() * (1f - 0.6f * p)
    withTransform({
        translate(center.x - s / 2f, center.y - s / 2f)
        rotate(part.rot * p, pivot = Offset(s / 2f, s / 2f))
    }) {
        val path = if (part.shape == 1) {
            Path().apply { moveTo(0f, s); lineTo(s / 2f, 0f); lineTo(s, s); close() }
        } else {
            Path().apply { moveTo(0f, 0f); lineTo(s, s * 0.3f); lineTo(s * 0.4f, s); close() }
        }
        drawPath(path, part.color, alpha = 1f - p)
    }
}
