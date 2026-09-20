package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.compose.ui.graphics.compositeOver
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * LA CARA DEL ARTEFACTO MAESTRO.
 *
 * No son cinco dibujos: es UN SOLO NÚMERO de 0 a 4 que un muelle mueve, y de él
 * salen a la vez la curva de la boca, el giro de las cejas, el tamaño de los
 * ojos, el rubor, la lágrima y el color. Por eso al tocar otra carita la cara
 * VIAJA hasta ella en vez de saltar, que es lo que la hace sentirse viva.
 *
 * Cinco dibujos intercambiados darían un parpadeo; esto da una transición.
 *
 * Todas las tablas de abajo están transcritas del artefacto, no interpretadas.
 * El sistema de coordenadas es el suyo —un lienzo de 96×96— y se escala al
 * tamaño pedido, así que la cara de 50 dp de Inicio y la de 140 dp de Bienestar
 * son exactamente la misma geometría.
 */

/** 0 genial · 1 bien · 2 normal · 3 regular · 4 bajo. La escala del artefacto. */
object MoodScale {
    const val GENIAL = 0
    const val BIEN = 1
    const val NORMAL = 2
    const val REGULAR = 3
    const val BAJO = 4

    /** Curvatura de la boca: positiva sonríe, negativa cae. */
    private val CURVE = floatArrayOf(22f, 13f, 0.5f, -7f, -14f)
    /** Altura de la boca en el lienzo de 96. */
    private val MY = floatArrayOf(53f, 54f, 56f, 56.5f, 57f)
    /** Media anchura de la boca. */
    private val MW = floatArrayOf(15f, 13.5f, 12.5f, 12f, 13f)
    /** Giro de las cejas en grados. Positivo = extremo interior arriba. */
    private val BROW = floatArrayOf(-10f, -4f, 2f, 9f, 16f)
    private val BLUSH = floatArrayOf(0.9f, 0.55f, 0.18f, 0.12f, 0.3f)
    /** Radio vertical del ojo: al estar bajo, se abre más y se humedece. */
    private val EYE = floatArrayOf(3.3f, 4.4f, 4.4f, 4.7f, 5.4f)
    private val TEAR = floatArrayOf(0f, 0f, 0f, 0f, 0.85f)

    /**
     * Los cinco acentos. De verde a VIOLETA, nunca a rojo: en este sistema el
     * rojo significa «esto está mal», y sentirse bajo no es un error.
     */
    private val ACC = arrayOf(
        Color(0xFF10A37F), Color(0xFF6DA544), Color(0xFFD9A441), Color(0xFFE2734A), Color(0xFF7C6BD6),
    )
    private val DEEP = arrayOf(
        Color(0xFF0B7A5F), Color(0xFF527C33), Color(0xFFA67A28), Color(0xFFB25432), Color(0xFF5849A8),
    )
    private val SKY = arrayOf(
        Color(0xFFE7F7F1), Color(0xFFF0F6E6), Color(0xFFFCF3E2), Color(0xFFFDEDE6), Color(0xFFF0EDFC),
    )

    val LABELS = listOf("Genial", "Bien", "Normal", "Regular", "Bajo")
    val ECHOES = listOf(
        "Genial. Buen día.",
        "Bien. Un buen día.",
        "Normal. Ni fu ni fa.",
        "Regular. Cuesta arriba.",
        "Bajo. Hoy, despacio.",
    )

    private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
    private fun lerp(a: Color, b: Color, f: Float) = Color(
        red = lerp(a.red, b.red, f), green = lerp(a.green, b.green, f),
        blue = lerp(a.blue, b.blue, f), alpha = 1f,
    )

    private fun at(t: FloatArray, v: Float): Float {
        val i = v.toInt().coerceIn(0, 3)
        return lerp(t[i], t[i + 1], v - i)
    }

    private fun at(t: Array<Color>, v: Float): Color {
        val i = v.toInt().coerceIn(0, 3)
        return lerp(t[i], t[i + 1], v - i)
    }

    fun curve(v: Float) = at(CURVE, v)
    fun mouthY(v: Float) = at(MY, v)
    fun mouthW(v: Float) = at(MW, v)
    fun brow(v: Float) = at(BROW, v)
    fun blush(v: Float) = at(BLUSH, v)
    fun eye(v: Float) = at(EYE, v)
    fun tear(v: Float) = at(TEAR, v)

    /** El acento vivo: bordes, ojos, boca. */
    fun accent(v: Float) = at(ACC, v)
    /** El acento oscuro: texto, que sobre blanco necesita más contraste. */
    fun deep(v: Float) = at(DEEP, v)
    /** El fondo de la tarjeta, que cambia con el ánimo. */
    fun sky(v: Float) = at(SKY, v)

    fun accentOf(value: Int) = ACC[value.coerceIn(0, 4)]
    fun skyOf(value: Int) = SKY[value.coerceIn(0, 4)]
    fun deepOf(value: Int) = DEEP[value.coerceIn(0, 4)]
}

/**
 * La cara, con muelle y parpadeo.
 *
 * `value` es el ánimo marcado (0-4) o `null` si el día aún no se ha marcado —en
 * ese caso la cara descansa en «normal», que es lo que hace el artefacto: no
 * desaparece, espera.
 */
@Composable
fun MoodFace(
    value: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    strokeWidth: Float = 3.6f,
    blinks: Boolean = true,
    surface: Color = Color.White,
) {
    val target = (value ?: MoodScale.NORMAL).coerceIn(0, 4).toFloat()

    /*
     * El muelle del artefacto. Allí es un `requestAnimationFrame` que mueve el
     * número un 15 % de la distancia por fotograma; aquí es el muelle nativo de
     * Compose, que da la misma sensación de llegada suave sin rebote.
     */
    val v by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "moodSpring",
    )

    // Parpadeo: cada 2,4-5,4 s, 125 ms cerrado. Es lo único que se mueve sin
    // que nadie toque nada, y por eso la cara parece que está ahí.
    var blink by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(blinks) {
        if (!blinks) return@LaunchedEffect
        while (true) {
            delay(2400L + Random.nextLong(3000L))
            blink = 0.16f
            delay(125L)
            blink = 1f
        }
    }
    val blinkFactor by animateFloatAsState(blink, label = "moodBlink")

    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            drawMoodFace(v, blinkFactor, strokeWidth, surface)
        }
    }
}

/**
 * El dibujo. Vive aparte del composable para poder pintarse también dentro de
 * otro `Canvas` —la miniatura de la semana de Bienestar— sin duplicar la
 * geometría.
 */
fun DrawScope.drawMoodFace(v: Float, blinkFactor: Float, strokeWidth: Float, surface: Color) {
    // El lienzo del artefacto mide 96; todo se escala desde ahí.
    val k = size.minDimension / 96f
    val acc = MoodScale.accent(v)
    val sw = strokeWidth * k

    // Cabeza
    drawCircle(color = surface, radius = 36f * k, center = Offset(48f * k, 48f * k))
    drawCircle(
        color = acc, radius = 36f * k, center = Offset(48f * k, 48f * k),
        style = Stroke(width = sw),
    )

    // Rubor
    val blush = MoodScale.blush(v)
    if (blush > 0.01f) {
        listOf(26f, 70f).forEach { cx ->
            drawOval(
                color = acc.copy(alpha = blush),
                topLeft = Offset((cx - 7.5f) * k, (58f - 4.6f) * k),
                size = Size(15f * k, 9.2f * k),
            )
        }
    }

    // Cejas. El giro es el rasgo más expresivo: al bajar el ánimo, el extremo
    // INTERIOR sube, que es lo que lee como preocupación.
    val brow = MoodScale.brow(v)
    listOf(36f to -brow, 60f to brow).forEach { (cx, angle) ->
        rotate(degrees = angle, pivot = Offset(cx * k, 31.5f * k)) {
            drawLine(
                color = acc,
                start = Offset((cx - 8f) * k, 31.5f * k),
                end = Offset((cx + 8f) * k, 31.5f * k),
                strokeWidth = (strokeWidth - 0.3f) * k,
                cap = StrokeCap.Round,
            )
        }
    }

    // Ojos
    val eyeR = MoodScale.eye(v) * blinkFactor
    listOf(36f, 60f).forEach { cx ->
        drawOval(
            color = acc,
            topLeft = Offset((cx - 3.5f) * k, (43f - eyeR) * k),
            size = Size(7f * k, eyeR * 2f * k),
        )
    }

    // Boca: una curva cuadrática cuyo punto de control es el que sonríe o cae.
    val my = MoodScale.mouthY(v)
    val mw = MoodScale.mouthW(v)
    val curve = MoodScale.curve(v)
    val mouth = Path().apply {
        moveTo((48f - mw) * k, my * k)
        quadraticTo(48f * k, (my + curve) * k, (48f + mw) * k, my * k)
    }
    drawPath(mouth, color = acc, style = Stroke(width = (strokeWidth + 0.2f) * k, cap = StrokeCap.Round))

    // Lágrima: solo en el peldaño más bajo, y con el violeta de la escala.
    val tear = MoodScale.tear(v)
    if (tear > 0.01f) {
        drawOval(
            color = Color(0xFF7C6BD6).copy(alpha = tear),
            topLeft = Offset((62f - 2.6f) * k, (52f - 3.4f) * k),
            size = Size(5.2f * k, 6.8f * k),
        )
    }
}

/** El peldaño más cercano, para elegir rótulo y frase sin saltos raros. */
fun Float.toMoodStep(): Int = this.roundToInt().coerceIn(0, 4)

/**
 * EL FONDO DE LA TARJETA DE ÁNIMO, EN EL TEMA ACTIVO.
 *
 * EL PROBLEMA QUE RESUELVE. `MoodScale.SKY` son cinco cremas fijos, pensados
 * para un fondo claro, y se usaban como fondo de una tarjeta cuyo texto sí
 * salía de los tokens del tema. Al pasar a Noche, el texto se volvía claro y la
 * tarjeta seguía siendo crema: el título «¿Cómo te sientes hoy?» desaparecía,
 * la cara se convertía en un disco negro y los cinco selectores invertían su
 * lectura —los NO elegidos quedaban oscuros sobre una tarjeta clara, de modo
 * que parecían los activos—. Eso es exactamente lo que ADR-023 prohíbe: el tema
 * reescribe variables, y un componente no puede llevar su propio color al
 * margen.
 *
 * POR QUÉ NO UNA SEGUNDA TABLA DE COLORES. Porque los temas no son dos: son
 * once, y varios oscuros. Una tabla «para Noche» habría que repetirla, y
 * volvería a quedarse corta con el siguiente tema. El tinte se compone aquí
 * sobre la superficie REAL del tema activo, así que cada tema produce el suyo
 * sin que nadie tenga que escribirlo — y el matiz sigue siendo el del ánimo,
 * que es lo único que debe permanecer constante porque identifica el estado.
 */
@Composable
fun moodSky(value: Int?): Color {
    val c = VidaTheme.colors
    if (value == null) return c.sunken
    return if (VidaTheme.spec.isDark) {
        MoodScale.accentOf(value).copy(alpha = 0.20f).compositeOver(c.surface)
    } else {
        MoodScale.skyOf(value)
    }
}

/** El tono del texto de acento sobre [moodSky], legible en los dos fondos. */
@Composable
fun moodInk(value: Int?): Color {
    val c = VidaTheme.colors
    if (value == null) return c.textSecondary
    // En claro manda el tono profundo, que es el que llega a 4,5:1 sobre el
    // crema; en oscuro, sobre un fondo ya oscuro, el profundo se apagaría y el
    // que contrasta es el vivo.
    return if (VidaTheme.spec.isDark) MoodScale.accentOf(value) else MoodScale.deepOf(value)
}

/**
 * El tinte del ánimo, más marcado.
 *
 * Para lo que tiene que destacar SOBRE una tarjeta que ya lleva [moodSky] de
 * fondo — el selector elegido, por ejemplo. Con el mismo tinte suave los dos,
 * el elegido se fundía con la tarjeta y dejaba de distinguirse en Noche.
 */
@Composable
fun moodSkyStrong(value: Int): Color {
    val c = VidaTheme.colors
    return if (VidaTheme.spec.isDark) {
        MoodScale.accentOf(value).copy(alpha = 0.46f).compositeOver(c.surface)
    } else {
        MoodScale.skyOf(value)
    }
}
