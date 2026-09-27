package com.vidacotidiana.app.core.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ADR-027 · La mariposa del artefacto aprobado «Mariposa Aprobada».
 *
 * GEOMETRÍA LITERAL. Cada faceta, degradado, nervio y contorno es el del SVG
 * `full` del artefacto (viewBox 0 0 215 215), transcrito por un generador —no
 * redibujado—. El icono del lanzador (`ic_launcher_foreground.xml`) sale del
 * mismo SVG, así que icono y pantalla de inicio son el mismo dibujo.
 *
 * SE DIBUJA COMO VECTOR, NO COMO IMAGEN. En la transición la mariposa crece
 * hasta once veces el alto de la pantalla; un `VectorPainter` la rasterizaría
 * a ese tamaño (un mapa de bits de decenas de miles de píxeles) y una imagen
 * escalada se pixelaría. Trazada en cada cuadro con la transformación del
 * lienzo, se mantiene nítida a cualquier escala, como en el artefacto.
 */
object Butterfly {
    /** Lado de la caja del dibujo, en unidades del SVG. */
    const val BOX = 215f

    /** Eje del cuerpo, fracción horizontal de la caja (`V.cx` del artefacto). */
    const val AXIS_X = 0.5004651f

    /** Centro del cuerpo, donde convergen los nervios (`BODY` del artefacto). */
    val BODY = Offset(AXIS_X, 110f / 215f)

    /** Mitad izquierda / derecha / franja del cuerpo: los recortes `L`, `R` y `B`. */
    val CLIP_LEFT = Rect(0f, 0f, 107.6f, BOX)
    val CLIP_RIGHT = Rect(107.6f, 0f, BOX, BOX)
    val CLIP_BODY = Rect(106f, 60f, 109.2f, 160f)

    private class Facet(
        val points: FloatArray,
        x1: Float, y1: Float, x2: Float, y2: Float,
        c0: Long, c1: Long,
    ) {
        val path = Path().apply {
            moveTo(points[0], points[1])
            var i = 2
            while (i < points.size) { lineTo(points[i], points[i + 1]); i += 2 }
            close()
        }
        val brush = Brush.linearGradient(listOf(Color(c0), Color(c1)), Offset(x1, y1), Offset(x2, y2))
    }

    private val facets = listOf(
    Facet(floatArrayOf(20.3f, 36.9f, 70f, 56.3f, 81.9f, 81.9f, 48.8f, 102.5f), 27.28f, 45.66f, 70.25f, 86.66f, 0xFF2B0558, 0xFF4D90CF),
    Facet(floatArrayOf(166.4f, 102.5f, 133.3f, 81.9f, 145.2f, 56.3f, 194.9f, 36.9f), 188.04f, 46.02f, 144.52f, 86.48f, 0xFF2B0457, 0xFF4E92D0),
    Facet(floatArrayOf(70f, 56.3f, 90f, 71.9f, 95.6f, 83.8f, 107.6f, 110f, 81.9f, 81.9f), 91.52f, 78.01f, 84.1f, 82.3f, 0xFF4D7FB5, 0xFF4D92CE),
    Facet(floatArrayOf(133.3f, 81.9f, 107.6f, 110f, 119.6f, 83.8f, 125.2f, 71.9f, 145.2f, 56.3f), 123.56f, 77.96f, 130.74f, 82.38f, 0xFF3A6DA3, 0xFF5999D7),
    Facet(floatArrayOf(48.8f, 102.5f, 81.9f, 81.9f, 77.5f, 110.6f), 60.46f, 92.46f, 77.82f, 103.91f, 0xFF438DC6, 0xFF67C4F2),
    Facet(floatArrayOf(137.7f, 110.6f, 133.3f, 81.9f, 166.4f, 102.5f), 155.54f, 92.77f, 137.14f, 103.19f, 0xFF468EC4, 0xFF66C3F1),
    Facet(floatArrayOf(81.9f, 81.9f, 107.6f, 110f, 77.5f, 110.6f), 100.19f, 110.24f, 78.71f, 92.82f, 0xFF3C4593, 0xFF53B1E1),
    Facet(floatArrayOf(137.7f, 110.6f, 107.6f, 110f, 133.3f, 81.9f), 115.07f, 110.44f, 136.25f, 92.56f, 0xFF3B3E90, 0xFF55B5E2),
    Facet(floatArrayOf(107.6f, 77.4f, 95.6f, 83.8f, 107.6f, 110f), 102f, 102.71f, 104.55f, 80.25f, 0xFF383D8C, 0xFF5EA8DA),
    Facet(floatArrayOf(107.6f, 110f, 119.6f, 83.8f, 107.6f, 77.4f), 112f, 103.06f, 111.21f, 80.28f, 0xFF37378A, 0xFF5CA9DB),
    Facet(floatArrayOf(77.5f, 110.6f, 61.9f, 114.4f, 53.8f, 128.8f, 60f, 145.6f, 83.8f, 150.2f), 80.26f, 120.05f, 60.07f, 140.42f, 0xFF3E61A9, 0xFF63CAF4),
    Facet(floatArrayOf(131.4f, 150.2f, 155.2f, 145.6f, 161.4f, 128.8f, 153.3f, 114.4f, 137.7f, 110.6f), 134.86f, 120.07f, 154.98f, 140.52f, 0xFF3D5EA7, 0xFF61CAF4),
    Facet(floatArrayOf(77.5f, 110.6f, 107.6f, 110f, 100.4f, 124.6f, 82.4f, 138.6f), 103.35f, 113f, 79.63f, 126.16f, 0xFF313583, 0xFF58ACE2),
    Facet(floatArrayOf(132.8f, 138.6f, 114.8f, 124.6f, 107.6f, 110f, 137.7f, 110.6f), 111.43f, 113.48f, 135.38f, 125.63f, 0xFF2F2F7F, 0xFF59ACE2),
    Facet(floatArrayOf(82.4f, 138.6f, 100.4f, 124.6f, 97.5f, 136.9f, 92.5f, 146.3f, 86.3f, 153.1f, 83.8f, 150.2f), 87.33f, 136.1f, 93.91f, 144.03f, 0xFF4493C7, 0xFF66A3D8),
    Facet(floatArrayOf(131.4f, 150.2f, 128.9f, 153.1f, 122.7f, 146.3f, 117.7f, 136.9f, 114.8f, 124.6f, 132.8f, 138.6f), 121.54f, 128.1f, 127.79f, 150.05f, 0xFF4E88C6, 0xFF58A8D2),
    Facet(floatArrayOf(107.6f, 110f, 97.5f, 136.9f, 107.6f, 145.3f), 103.86f, 117.54f, 104.28f, 141.52f, 0xFF383386, 0xFF5CB2E1),
    Facet(floatArrayOf(107.6f, 145.3f, 117.7f, 136.9f, 107.6f, 110f), 105.55f, 118.82f, 114.48f, 139.37f, 0xFF323587, 0xFF62ADE3),
    Facet(floatArrayOf(64.4f, 146.9f, 78.1f, 152.5f, 70f, 173.8f, 51.3f, 180.6f, 65.6f, 167.5f, 66.9f, 158.8f), 72.65f, 170.67f, 63.8f, 151.69f, 0xFF261760, 0xFF41539B),
    Facet(floatArrayOf(148.3f, 158.8f, 149.6f, 167.5f, 163.9f, 180.6f, 145.2f, 173.8f, 137.1f, 152.5f, 150.8f, 146.9f), 141.92f, 169.48f, 152.04f, 152.63f, 0xFF25165F, 0xFF4358A2),
    )

    /** Nervios: del cuerpo a cada vértice interior. Dos van duplicados en el SVG y se conservan: suman opacidad. */
    private val veins = floatArrayOf(
        107.6f, 110f, 95.6f, 83.8f,
        107.6f, 110f, 119.6f, 83.8f,
        107.6f, 110f, 81.9f, 81.9f,
        107.6f, 110f, 133.3f, 81.9f,
        107.6f, 110f, 77.5f, 110.6f,
        107.6f, 110f, 137.7f, 110.6f,
        107.6f, 110f, 100.4f, 124.6f,
        107.6f, 110f, 114.8f, 124.6f,
        107.6f, 110f, 97.5f, 136.9f,
        107.6f, 110f, 117.7f, 136.9f,
        107.6f, 110f, 107.6f, 77.4f,
        107.6f, 110f, 107.6f, 77.4f,
        107.6f, 110f, 107.6f, 145.3f,
        107.6f, 110f, 107.6f, 145.3f,
    )

    private val ink = Color(0xFF1C2F6E)
    private val core = Brush.radialGradient(
        0f to Color(0xFF241A66).copy(alpha = 0.95f),
        0.45f to Color(0xFF2C2A7A).copy(alpha = 0.45f),
        1f to Color(0xFF2C2A7A).copy(alpha = 0f),
        center = Offset(107.6f, 110f),
        radius = 16f,
    )
    private val facetEdge = Stroke(width = 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    private val outline = Stroke(width = 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    /** Dibuja la mariposa completa en unidades del SVG (0‥215). Quien llama pone la escala. */
    fun DrawScope.drawButterflyUnits() {
        facets.forEach {
            drawPath(it.path, it.brush)
            drawPath(it.path, it.brush, style = facetEdge)
        }
        drawCircle(core, radius = 16f, center = Offset(107.6f, 110f))
        var i = 0
        while (i < veins.size) {
            drawLine(ink, Offset(veins[i], veins[i + 1]), Offset(veins[i + 2], veins[i + 3]), strokeWidth = 0.7f, cap = StrokeCap.Round, alpha = 0.85f)
            i += 4
        }
        facets.forEach { drawPath(it.path, ink, style = outline) }
    }

    /** La mariposa completa ocupando un cuadrado de `side` px con su esquina en `topLeft`. */
    fun DrawScope.drawButterfly(topLeft: Offset, side: Float) {
        val k = side / BOX
        withTransform({
            translate(topLeft.x, topLeft.y)
            scale(k, k, pivot = Offset.Zero)
        }) { drawButterflyUnits() }
    }

    /**
     * La silueta: todas las facetas rellenas de un solo color, sin nervios ni
     * contornos. Es la versión de la marca para usar como textura sobre un
     * color (la marca de agua de `HeroCard`), igual que el icono monocromo.
     */
    fun DrawScope.drawButterflySilhouette(topLeft: Offset, side: Float, color: Color) {
        val k = side / BOX
        withTransform({
            translate(topLeft.x, topLeft.y)
            scale(k, k, pivot = Offset.Zero)
        }) {
            facets.forEach {
                drawPath(it.path, color)
                drawPath(it.path, color, style = facetEdge)
            }
        }
    }

    /**
     * Una capa del aleteo, con la misma óptica que el CSS del artefacto:
     * `rotateY(angle)` alrededor del eje del cuerpo (`transform-origin: cx 50%`),
     * `translateY(bob)` del conjunto de alas y `perspective: 3.4 × lado` con
     * origen en el centro de la figura.
     *
     * Girar un plano y proyectarlo en perspectiva es una homografía, que el
     * lienzo de Android aplica exactamente (degradados incluidos). No es una
     * aproximación con escalas: es la misma proyección que hace el navegador.
     *
     * @param side lado de la figura en px.
     * @param clip recorte en unidades del SVG (`CLIP_LEFT`, `CLIP_RIGHT`, `CLIP_BODY`).
     * @param angleDeg giro en grados; positivo acerca al espectador el borde izquierdo.
     * @param bob desplazamiento vertical en px (negativo = arriba).
     */
    fun DrawScope.drawWingLayer(side: Float, clip: Rect, angleDeg: Float, bob: Float) {
        val a = angleDeg * (PI / 180.0).toFloat()
        val c = cos(a)
        val s = sin(a)
        val d = 3.4f * side
        val o = side / 2f
        val au = AXIS_X * side - o
        val m = android.graphics.Matrix().apply {
            setValues(
                floatArrayOf(
                    c, 0f, au * (1f - c),
                    0f, 1f, bob,
                    s / d, 0f, 1f - au * s / d,
                ),
            )
            preTranslate(-o, -o)
            postTranslate(o, o)
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.concat(m)
            val k = side / BOX
            scale(k, k, pivot = Offset.Zero) {
                clipRect(clip.left, clip.top, clip.right, clip.bottom) { drawButterflyUnits() }
            }
            canvas.nativeCanvas.restore()
        }
    }
}

/**
 * ADR-027 · La mariposa como logo estático, a todo color: la marca dentro de
 * la aplicación (cabecera del menú lateral). Es el mismo dibujo del icono y de
 * la pantalla de inicio, en reposo y con las alas abiertas.
 *
 * Decorativa para la accesibilidad: el nombre que la acompaña ya se anuncia.
 */
@Composable
fun ButterflyMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        with(Butterfly) { drawButterfly(Offset.Zero, this@Canvas.size.minDimension) }
    }
}
