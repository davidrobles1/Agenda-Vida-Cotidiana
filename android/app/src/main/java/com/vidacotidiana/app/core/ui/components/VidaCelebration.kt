package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaTheme
import kotlinx.coroutines.delay
import androidx.compose.animation.core.rememberInfiniteTransition

/**
 * LA RESPUESTA VISUAL A UN HECHO — transcrita del artefacto aprobado.
 *
 * Fuente de verdad:
 * https://claude.ai/artifact/WwqwtbwN41Te3fJb3jcXWG
 *
 * LO QUE ESTA CAPA NO HACE, y es la mitad de su diseño:
 *
 *  · NO mueve nada de la pantalla. El logro no sustituye la retícula de Inicio
 *    ni la tarjeta del objetivo: se superpone. Quien estaba leyendo algo lo
 *    sigue teniendo donde estaba cuando la animación termina.
 *  · NO lleva a ninguna parte. Ninguna celebración navega.
 *  · NO hay que esperarla: cualquier toque la retira.
 *  · NO tiene caja. Nada de tarjeta ni recuadro detrás — un difuminado redondo
 *    aclara el centro y se desvanece antes de los bordes, así que no aparece
 *    ninguna arista que parezca un cuadro.
 *
 * VIVE FUERA DE LA COMPOSICIÓN DE LAS PANTALLAS, en el armazón. Si viviera
 * dentro, cada redibujo —y dar algo por hecho provoca uno— reiniciaría la
 * animación desde cero o la destruiría a media reproducción.
 */

/** Las cuatro intensidades del artefacto. El orden ES la escala. */
enum class CelebrationTier { MICRO, CONFIRM, CELEBRATE, ACHIEVE }

/**
 * Un hecho que merece respuesta visual.
 *
 * `title` dice QUÉ se logró y `line` CIERRA SU CONTEXTO — y esa segunda línea
 * es la razón de ser de todo esto. Una celebración que sólo dice «¡bien!» anima
 * por animar; la que dice «vuelve en 6 meses» ha terminado el asunto.
 *
 * `stamp` distingue dos celebraciones idénticas seguidas: sin él, dar por
 * hechos dos mantenimientos con el mismo texto no volvería a disparar la
 * animación, porque el estado no habría cambiado.
 */
data class Celebration(
    val tier: CelebrationTier,
    val title: String,
    val line: String? = null,
    val stamp: Long = System.nanoTime(),
)

/** Duraciones del artefacto, en milisegundos. No se redondean. */
const val DUR_CONFIRM = 1240
const val DUR_CELEBRATE = 1700
const val DUR_ACHIEVE = 2600

/** El oro del logro. Es el ÚNICO color nuevo, y sólo lo usa el trofeo. */
val VidaGold = Color(0xFFC9911F)
val VidaGoldDeep = Color(0xFF8A6212)

fun CelebrationTier.durationMillis(): Int = when (this) {
    CelebrationTier.MICRO -> 300
    CelebrationTier.CONFIRM -> DUR_CONFIRM
    CelebrationTier.CELEBRATE -> DUR_CELEBRATE
    CelebrationTier.ACHIEVE -> DUR_ACHIEVE
}

/**
 * LA CAPA. Se monta UNA vez, sobre toda la aplicación.
 *
 * Se retira por dos caminos que acaban en el mismo sitio —el tiempo agotado y
 * el toque del usuario—, así que no puede quedarse una animación a medias sin
 * limpiar.
 */
@Composable
fun CelebrationLayer(
    celebration: Celebration?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (celebration == null || celebration.tier == CelebrationTier.MICRO) return

    // Se reinicia con `stamp`: dos hechos seguidos con el mismo texto son dos
    // celebraciones, no una.
    LaunchedEffect(celebration.stamp) {
        delay(celebration.tier.durationMillis().toLong())
        onDismiss()
    }

    // QUIÉN INTERCEPTA EL TOQUE, Y QUIÉN NO.
    //
    // Las dos piezas centradas —medallón y trofeo— sí lo recogen: ocupan el
    // centro de la pantalla y tocarlas para quitarlas es el gesto natural.
    //
    // La banda de confirmación NO. Es pequeña, va al pie y dura poco; si
    // capturase el toque, crear algo dejaría la aplicación sorda durante
    // segundo y cuarto justo cuando el usuario acaba de llegar a la pantalla
    // nueva y quiere tocar lo que ve.
    val interceptaToque = celebration.tier != CelebrationTier.CONFIRM

    Box(
        modifier
            .fillMaxSize()
            .then(
                if (interceptaToque) {
                    Modifier.pointerInput(celebration.stamp) {
                        detectTapGestures(onPress = { onDismiss() })
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (celebration.tier) {
            CelebrationTier.CONFIRM -> ConfirmBand(celebration)
            CelebrationTier.CELEBRATE -> CelebrateBadge(celebration)
            CelebrationTier.ACHIEVE -> AchieveTrophy(celebration)
            CelebrationTier.MICRO -> Unit
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   EL CICLO DE MOVIMIENTO — una sola curva para entrar, flotar y salir.
   El artefacto no deja la pieza quieta esperando a desvanecerse: sigue
   derivando hacia arriba durante toda la reproducción.
   ══════════════════════════════════════════════════════════════════════════ */

/** Lo que vale el ciclo en un instante: transparencia, desplazamiento y escala. */
private data class Lift(val alpha: Float, val shiftDp: Float, val scale: Float)

/**
 * El `ov-lift` del artefacto, resuelto con UN solo valor que va de 0 a 1.
 *
 * Los tramos son los suyos: sube desde +30 dp y aparece hasta el 18 % del
 * ciclo, deriva hasta −7 dp durante la parte central, y sale hacia −38 dp
 * desvaneciéndose. Tres animaciones paralelas darían el mismo resultado con
 * tres relojes que pueden desincronizarse; con uno no pueden.
 */
@Composable
private fun rememberLift(durationMillis: Int, delayMillis: Int = 0, key: Any): Lift {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        p.snapTo(0f)
        if (delayMillis > 0) delay(delayMillis.toLong())
        p.animateTo(1f, tween(durationMillis, easing = VidaEase))
    }
    val t = p.value
    val alpha = when {
        t < 0.18f -> t / 0.18f
        t < 0.62f -> 1f
        else -> 1f - (t - 0.62f) / 0.38f
    }
    val shift = when {
        t < 0.18f -> 30f - 30f * (t / 0.18f)
        t < 0.62f -> -7f * ((t - 0.18f) / 0.44f)
        else -> -7f - 31f * ((t - 0.62f) / 0.38f)
    }
    val scale = when {
        t < 0.18f -> 0.74f + 0.29f * (t / 0.18f)
        t < 0.28f -> 1.03f - 0.03f * ((t - 0.18f) / 0.10f)
        t < 0.62f -> 1f
        else -> 1f + 0.05f * ((t - 0.62f) / 0.38f)
    }
    return Lift(alpha.coerceIn(0f, 1f), shift, scale)
}

private fun Modifier.lift(s: Lift) = this.graphicsLayer {
    alpha = s.alpha
    translationY = s.shiftDp * density
    scaleX = s.scale
    scaleY = s.scale
}

/**
 * EL DIFUMINADO. Aclara el centro para que la felicitación se lea sobre
 * cualquier fila, y se desvanece a nada antes de los bordes: es redondo, así
 * que no hay ninguna arista que parezca una caja.
 */
@Composable
private fun Veil(durationMillis: Int, key: Any) {
    val c = VidaTheme.colors
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        p.snapTo(0f)
        p.animateTo(1f, tween(durationMillis, easing = VidaEase))
    }
    val t = p.value
    val a = when {
        t < 0.20f -> t / 0.20f
        t < 0.72f -> 1f
        else -> 1f - (t - 0.72f) / 0.28f
    }.coerceIn(0f, 1f)

    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = a }) {
        val center = Offset(size.width / 2f, size.height * 0.46f)
        val r = size.minDimension * 0.86f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to c.surface,
                    0.26f to c.surface,
                    0.44f to c.surface.copy(alpha = 0.62f),
                    0.64f to Color.Transparent,
                ),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
}

/** El halo que florece detrás de la pieza: 0,35 → 2,6 de escala, desvaneciéndose. */
@Composable
private fun Bloom(color: Color, durationMillis: Int, key: Any) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        p.snapTo(0f)
        p.animateTo(1f, tween(durationMillis, easing = VidaEase))
    }
    val t = p.value
    Box(
        Modifier
            .size(230.dp)
            .graphicsLayer {
                val s = 0.35f + 2.25f * t
                scaleX = s
                scaleY = s
                alpha = if (t < 0.25f) t * 2f else ((1f - t) * 0.66f).coerceAtLeast(0f)
            }
            .blur(28.dp)
            .background(color.copy(alpha = 0.42f), RoundedCornerShape(50)),
    )
}

/* ══════════════════════════════════════════════════════════════════════════
   T2 · CONFIRMACIÓN — una banda breve al pie.
   ══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun ConfirmBand(cel: Celebration) {
    val c = VidaTheme.colors
    val p = remember(cel.stamp) { Animatable(0f) }
    LaunchedEffect(cel.stamp) {
        p.snapTo(0f)
        p.animateTo(1f, tween(DUR_CONFIRM, easing = VidaEase))
    }
    val t = p.value
    val alpha = when {
        t < 0.16f -> t / 0.16f
        t < 0.72f -> 1f
        else -> 1f - (t - 0.72f) / 0.28f
    }.coerceIn(0f, 1f)
    val shift = when {
        t < 0.16f -> 26f - 26f * (t / 0.16f)
        t < 0.72f -> -3f * ((t - 0.16f) / 0.56f)
        else -> -3f - 13f * ((t - 0.72f) / 0.28f)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .padding(start = 24.dp, end = 24.dp, bottom = 106.dp)
                .fillMaxWidth()
                .graphicsLayer { this.alpha = alpha; translationY = shift * density }
                .background(c.text, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(24.dp).background(c.success, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) { CheckMark(13.dp, Color.White, 1f) }
            Text(
                cel.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = c.surface,
            )
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   T3 · CELEBRACIÓN — el medallón con la palomilla trazándose.
   ══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun CelebrateBadge(cel: Celebration) {
    val c = VidaTheme.colors
    val lift = rememberLift(DUR_CELEBRATE, key = cel.stamp)
    val liftText = rememberLift(DUR_CELEBRATE, delayMillis = 120, key = cel.stamp)

    // El trazo de la palomilla: 520 ms tras 200 de espera, como el artefacto.
    val draw = remember(cel.stamp) { Animatable(0f) }
    LaunchedEffect(cel.stamp) {
        draw.snapTo(0f)
        delay(200)
        draw.animateTo(1f, tween(520, easing = VidaEase))
    }

    Veil(DUR_CELEBRATE, cel.stamp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Bloom(c.success, DUR_CELEBRATE, cel.stamp)
            Box(
                Modifier
                    .size(92.dp)
                    .lift(lift)
                    .background(
                        Brush.linearGradient(listOf(c.success, c.successText)),
                        RoundedCornerShape(50),
                    ),
                contentAlignment = Alignment.Center,
            ) { CheckMark(36.dp, Color.White, draw.value) }
        }
        Text(
            cel.title,
            style = VidaTheme.type.sectionTitle.copy(fontSize = 18.sp, lineHeight = 23.sp),
            color = c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 30.dp).lift(liftText),
        )
        cel.line?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp).lift(liftText),
            )
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   T4 · LOGRO — el trofeo, con sus rayos girando y su balanceo.
   ══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun AchieveTrophy(cel: Celebration) {
    val c = VidaTheme.colors
    val lift = rememberLift(DUR_ACHIEVE, delayMillis = 120, key = cel.stamp)
    val liftTitle = rememberLift(DUR_ACHIEVE, delayMillis = 260, key = cel.stamp)
    val liftLine = rememberLift(DUR_ACHIEVE, delayMillis = 360, key = cel.stamp)

    // Los rayos giran de −14° a 26° durante toda la reproducción.
    val ray = remember(cel.stamp) { Animatable(-14f) }
    LaunchedEffect(cel.stamp) {
        ray.snapTo(-14f)
        ray.animateTo(26f, tween(DUR_ACHIEVE, easing = VidaEase))
    }
    // Y el trofeo se balancea ±1,5°, que es lo que lo hace parecer vivo.
    val sway by rememberInfiniteTransition(label = "trophySway").animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1000, easing = VidaEase), RepeatMode.Reverse),
        label = "swayAngle",
    )

    Veil(DUR_ACHIEVE, cel.stamp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Bloom(VidaGold, DUR_ACHIEVE, cel.stamp)
            Box(
                Modifier
                    .size(124.dp)
                    .lift(lift)
                    .graphicsLayer {
                        rotationZ = sway
                        transformOrigin = TransformOrigin(0.5f, 0.8f)
                    },
            ) { Trophy(ray.value) }
        }
        Text(
            cel.title,
            style = VidaTheme.type.sectionTitle.copy(fontSize = 24.sp, lineHeight = 29.sp),
            color = VidaGoldDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp).lift(liftTitle),
        )
        cel.line?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp).lift(liftLine),
            )
        }
    }
}

/** La palomilla, trazándose. `progress` 0 → 1 la dibuja de principio a fin. */
@Composable
private fun CheckMark(size: Dp, color: Color, progress: Float) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val full = Path().apply {
            moveTo(w * 0.21f, w * 0.52f)
            lineTo(w * 0.40f, w * 0.71f)
            lineTo(w * 0.79f, w * 0.31f)
        }
        val shown = Path()
        val measure = PathMeasure().apply { setPath(full, false) }
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), shown, true)
        drawPath(
            shown,
            color = color,
            style = Stroke(width = w * 0.125f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** El trofeo del artefacto, trazo a trazo. Nunca un emoji: hereda el color. */
@Composable
private fun Trophy(rayAngle: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / 64f
        val centre = Offset(32f * u, 32f * u)

        rotate(rayAngle, pivot = centre) {
            repeat(8) { i ->
                rotate(i * 45f, pivot = centre) {
                    drawLine(
                        VidaGold.copy(alpha = 0.5f),
                        Offset(32f * u, 4f * u),
                        Offset(32f * u, 11f * u),
                        strokeWidth = 2.6f * u,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        val cup = Path().apply {
            moveTo(21f * u, 14f * u)
            lineTo(43f * u, 14f * u)
            lineTo(43f * u, 24f * u)
            cubicTo(43f * u, 30f * u, 38f * u, 35f * u, 32f * u, 35f * u)
            cubicTo(26f * u, 35f * u, 21f * u, 30f * u, 21f * u, 24f * u)
            close()
        }
        drawPath(cup, VidaGold.copy(alpha = 0.18f))
        drawPath(cup, VidaGoldDeep, style = Stroke(2.6f * u, join = StrokeJoin.Round))

        drawPath(
            Path().apply {
                moveTo(21f * u, 17f * u)
                lineTo(16f * u, 17f * u)
                cubicTo(16f * u, 20f * u, 19f * u, 23f * u, 22f * u, 23f * u)
            },
            VidaGoldDeep,
            style = Stroke(2.4f * u, cap = StrokeCap.Round),
        )
        drawPath(
            Path().apply {
                moveTo(43f * u, 17f * u)
                lineTo(48f * u, 17f * u)
                cubicTo(48f * u, 20f * u, 45f * u, 23f * u, 42f * u, 23f * u)
            },
            VidaGoldDeep,
            style = Stroke(2.4f * u, cap = StrokeCap.Round),
        )

        drawLine(
            VidaGoldDeep,
            Offset(32f * u, 35f * u),
            Offset(32f * u, 42f * u),
            strokeWidth = 2.6f * u,
            cap = StrokeCap.Round,
        )
        val base = Path().apply {
            moveTo(24f * u, 46f * u)
            lineTo(40f * u, 46f * u)
            lineTo(40f * u, 50f * u)
            lineTo(24f * u, 50f * u)
            close()
        }
        drawPath(base, VidaGold.copy(alpha = 0.25f))
        drawPath(base, VidaGoldDeep, style = Stroke(2.2f * u, join = StrokeJoin.Round))
    }
}
