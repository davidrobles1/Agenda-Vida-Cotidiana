package com.vidacotidiana.app.feature.board

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * El catálogo de formas del Vision Board — el MISMO que la Web.
 *
 * Los `id` son literalmente los de `web/src/features/visionboard/
 * visionBoardShapes.ts`, y los `path` están escritos en el mismo lienzo de
 * 0–100 que allí. Eso no es una coincidencia estética: una forma creada en
 * el móvil guarda su id en `data.shape`, y el navegador la dibuja buscando
 * ESE id en su propio catálogo. Un id inventado aquí saldría en blanco allá.
 *
 * Las familias generadas (polígonos, estrellas, engranajes, orgánicas) se
 * calculan con los mismos generadores que la Web en vez de copiarse ya
 * resueltas: así una corrección de geometría se hace una vez y las dos
 * plataformas la heredan igual.
 *
 * Compose dibuja estos `path` con `PathParser`, que entiende la misma
 * sintaxis de SVG — no hace falta convertirlos a nada.
 */

data class VisionBoardShape(
    val id: String,
    val label: String,
    /** `null` en las cuatro nativas: rectángulo, círculo, línea y cápsula. */
    val path: String? = null,
    /** `true` cuando el dibujo es «forma exterior menos hueco» (anillo, luna, marco). */
    val evenOdd: Boolean = false,
    val family: String,
)

data class ShapeFamily(val id: String, val label: String)

val SHAPE_FAMILIES = listOf(
    ShapeFamily("basic", "Básicas"),
    ShapeFamily("polygons", "Polígonos"),
    ShapeFamily("stars", "Estrellas"),
    ShapeFamily("arrows", "Flechas"),
    ShapeFamily("flowchart", "Diagrama de flujo"),
    ShapeFamily("speech", "Diálogo"),
    ShapeFamily("clouds", "Nubes"),
    ShapeFamily("hearts", "Corazones"),
    ShapeFamily("banners", "Banners"),
    ShapeFamily("teardrops", "Lágrimas"),
    ShapeFamily("gears", "Engranajes"),
    ShapeFamily("asterisks", "Asteriscos"),
    ShapeFamily("organic", "Orgánicas"),
    ShapeFamily("abstract", "Abstractas"),
)

/* ---- generadores: la misma geometría que calcula la Web ---- */

private fun fmt(v: Double): String = String.format("%.2f", v)

private fun polygonPath(sides: Int, rot: Double = -90.0): String {
    val pts = (0 until sides).map { i ->
        val a = (rot + (360.0 / sides) * i) * PI / 180.0
        "${fmt(50 + 46 * cos(a))} ${fmt(50 + 46 * sin(a))}"
    }
    return "M${pts.joinToString(" L")} Z"
}

private fun starPath(points: Int, innerRatio: Double, rot: Double = -90.0): String {
    val pts = (0 until points * 2).map { i ->
        val r = if (i % 2 == 0) 46.0 else 46.0 * innerRatio
        val a = (rot + (360.0 / (points * 2)) * i) * PI / 180.0
        "${fmt(50 + r * cos(a))} ${fmt(50 + r * sin(a))}"
    }
    return "M${pts.joinToString(" L")} Z"
}

private fun gearPath(teeth: Int, outerR: Double, innerR: Double, boreR: Double): String {
    val step = 360.0 / (teeth * 4)
    val pts = (0 until teeth * 4).map { i ->
        val phase = i % 4
        val r = if (phase == 0 || phase == 1) outerR else innerR
        val a = step * i * PI / 180.0
        "${fmt(50 + r * cos(a))} ${fmt(50 + r * sin(a))}"
    }
    val bore = "M${50 + boreR} 50 A$boreR $boreR 0 1 0 ${50 - boreR} 50 A$boreR $boreR 0 1 0 ${50 + boreR} 50 Z"
    return "M${pts.joinToString(" L")} Z $bore"
}

private fun blobPath(jitter: List<Double>): String {
    val n = jitter.size
    val pts = (0 until n).map { i ->
        val r = 40.0 * jitter[i]
        val a = (360.0 / n) * i * PI / 180.0
        (50 + r * cos(a)) to (50 + r * sin(a))
    }
    val sb = StringBuilder("M${String.format("%.1f", pts[0].first)} ${String.format("%.1f", pts[0].second)}")
    for (j in 0 until n) {
        val p0 = pts[j]
        val p1 = pts[(j + 1) % n]
        sb.append(
            " Q${String.format("%.1f", p0.first)} ${String.format("%.1f", p0.second)}" +
                " ${String.format("%.1f", (p0.first + p1.first) / 2)}" +
                " ${String.format("%.1f", (p0.second + p1.second) / 2)}",
        )
    }
    return "$sb Z"
}

val SHAPE_CATALOG: List<VisionBoardShape> = listOf(
    // ===== Básicas =====
    VisionBoardShape("rectangle", "Rectángulo", family = "basic"),
    VisionBoardShape("circle", "Círculo", family = "basic"),
    VisionBoardShape("line", "Línea", family = "basic"),
    VisionBoardShape("capsule", "Cápsula", family = "basic"),
    VisionBoardShape("triangle", "Triángulo", "M50 5 L89 72.5 L11 72.5 Z", family = "basic"),
    VisionBoardShape("rhombus", "Rombo", "M50 5 L95 50 L50 95 L5 50 Z", family = "basic"),
    VisionBoardShape("cross", "Cruz", "M35 10 H65 V35 H90 V65 H65 V90 H35 V65 H10 V35 H35 Z", family = "basic"),
    VisionBoardShape("check", "Check", "M8 55 L33 80 L92 15 L80 6 L33 60 L18 43 Z", family = "basic"),
    VisionBoardShape("x", "X", "M15 8 L35 8 L50 35 L65 8 L85 8 L58 50 L85 92 L65 92 L50 65 L35 92 L15 92 L42 50 Z", family = "basic"),
    VisionBoardShape("roundedRect", "Rect. redondeado", "M15 5 H85 A10 10 0 0 1 95 15 V85 A10 10 0 0 1 85 95 H15 A10 10 0 0 1 5 85 V15 A10 10 0 0 1 15 5 Z", family = "basic"),
    VisionBoardShape("trapezoid", "Trapecio", "M20 20 H80 L95 80 H5 Z", family = "basic"),
    VisionBoardShape("parallelogram", "Paralelogramo", "M25 20 H95 L75 80 H5 Z", family = "basic"),
    VisionBoardShape("chevron", "Chevrón", "M10 15 L50 50 L10 85 L25 85 L65 50 L25 15 Z", family = "basic"),
    VisionBoardShape("halfCircle", "Medio círculo", "M5 50 A45 45 0 0 1 95 50 Z", family = "basic"),
    VisionBoardShape("ring", "Anillo", "M50 5 A45 45 0 1 0 50.01 5 Z M50 25 A25 25 0 1 1 49.99 25 Z", evenOdd = true, family = "basic"),

    // ===== Polígonos =====
    VisionBoardShape("pentagon", "Pentágono", "M50 5 L92.8 36.1 L76.5 86.4 L23.5 86.4 L7.2 36.1 Z", family = "polygons"),
    VisionBoardShape("hexagon", "Hexágono", "M50 5 L89 27.5 L89 72.5 L50 95 L11 72.5 L11 27.5 Z", family = "polygons"),
    VisionBoardShape("heptagon", "Heptágono", polygonPath(7), family = "polygons"),
    VisionBoardShape("octagon", "Octágono", "M67.2 8.4 L91.6 32.8 L91.6 67.2 L67.2 91.6 L32.8 91.6 L8.4 67.2 L8.4 32.8 L32.8 8.4 Z", family = "polygons"),
    VisionBoardShape("nonagon", "Eneágono", polygonPath(9), family = "polygons"),
    VisionBoardShape("decagon", "Decágono", polygonPath(10), family = "polygons"),
    VisionBoardShape("dodecagon", "Dodecágono", polygonPath(12), family = "polygons"),

    // ===== Estrellas =====
    VisionBoardShape("star", "Estrella", "M50 5 L60.6 35.4 L92.8 36.1 L67.1 55.6 L76.5 86.4 L50 68 L23.5 86.4 L32.9 55.6 L7.2 36.1 L39.4 35.4 Z", family = "stars"),
    VisionBoardShape("star4", "Estrella 4 puntas", starPath(4, 0.35), family = "stars"),
    VisionBoardShape("star6", "Estrella 6 puntas", starPath(6, 0.55), family = "stars"),
    VisionBoardShape("star6david", "Estrella 6 (David)", starPath(6, 0.58), family = "stars"),
    VisionBoardShape("star7", "Estrella 7 puntas", starPath(7, 0.5), family = "stars"),
    VisionBoardShape("star8", "Estrella 8 puntas", starPath(8, 0.5), family = "stars"),
    VisionBoardShape("star8fat", "Estrella 8 gruesa", starPath(8, 0.72), family = "stars"),
    VisionBoardShape("star10", "Estrella 10 puntas", starPath(10, 0.5), family = "stars"),
    VisionBoardShape("star12", "Estrella 12 puntas", starPath(12, 0.55), family = "stars"),
    VisionBoardShape("starBurst", "Destello", starPath(16, 0.25), family = "stars"),

    // ===== Flechas =====
    VisionBoardShape("arrowRight", "Flecha derecha", "M10 30 L60 30 L60 10 L95 50 L60 90 L60 70 L10 70 Z", family = "arrows"),
    VisionBoardShape("arrowLeft", "Flecha izquierda", "M90 30 L40 30 L40 10 L5 50 L40 90 L40 70 L90 70 Z", family = "arrows"),
    VisionBoardShape("arrowUp", "Flecha arriba", "M30 90 L30 40 L10 40 L50 5 L90 40 L70 40 L70 90 Z", family = "arrows"),
    VisionBoardShape("arrowDown", "Flecha abajo", "M30 10 L30 60 L10 60 L50 95 L90 60 L70 60 L70 10 Z", family = "arrows"),
    VisionBoardShape("arrowDoubleH", "Doble flecha ↔", "M5 50 L25 30 L25 42 L75 42 L75 30 L95 50 L75 70 L75 58 L25 58 L25 70 Z", family = "arrows"),
    VisionBoardShape("arrowDoubleV", "Doble flecha ↕", "M50 5 L30 25 L42 25 L42 75 L30 75 L50 95 L70 75 L58 75 L58 25 L70 25 Z", family = "arrows"),
    VisionBoardShape("arrowChevronBlock", "Flecha muesca", "M10 20 H55 L80 50 L55 80 H10 L35 50 Z", family = "arrows"),

    // ===== Diagrama de flujo =====
    VisionBoardShape("flowDecision", "Decisión", "M50 5 L95 50 L50 95 L5 50 Z", family = "flowchart"),
    VisionBoardShape("flowDocument", "Documento", "M10 10 H90 V70 Q70 90 50 75 Q30 60 10 80 Z", family = "flowchart"),
    VisionBoardShape("flowPredefined", "Proceso predef.", "M5 10 H95 V90 H5 Z M15 10 H21 V90 H15 Z M79 10 H85 V90 H79 Z", family = "flowchart"),
    VisionBoardShape("flowManualInput", "Entrada manual", "M5 30 L20 10 H95 V90 H5 Z", family = "flowchart"),
    VisionBoardShape("flowDelay", "Retraso", "M10 10 H55 A40 40 0 0 1 55 90 H10 Z", family = "flowchart"),
    VisionBoardShape("flowDisplay", "Pantalla", "M10 10 H55 Q95 10 95 50 Q95 90 55 90 H10 Z", family = "flowchart"),

    // ===== Diálogo =====
    VisionBoardShape("speechBubble", "Globo de diálogo", "M10 12 H90 A5 5 0 0 1 95 17 V63 A5 5 0 0 1 90 68 H38 L22 88 L26 68 H10 A5 5 0 0 1 5 63 V17 A5 5 0 0 1 10 12 Z", family = "speech"),
    VisionBoardShape("speechRight", "Diálogo derecho", "M90 12 H10 A5 5 0 0 0 5 17 V63 A5 5 0 0 0 10 68 H62 L78 88 L74 68 H90 A5 5 0 0 0 95 63 V17 A5 5 0 0 0 90 12 Z", family = "speech"),
    VisionBoardShape("thoughtBubble", "Nube de idea", "M26 60 A16 16 0 0 1 22 30 A20 20 0 0 1 61 22 A17 17 0 0 1 82 42 A14 14 0 0 1 78 60 Z M30 72 A6 6 0 1 1 29.9 72 Z M20 88 A3 3 0 1 1 19.9 88 Z", family = "speech"),
    VisionBoardShape("captionBox", "Recuadro con guía", "M8 10 H92 V65 H58 L50 88 L42 65 H8 Z", family = "speech"),

    // ===== Nubes =====
    VisionBoardShape("cloud", "Nube", "M26 72 A16 16 0 0 1 22 40.5 A20 20 0 0 1 61 30 A17 17 0 0 1 82 50 A14 14 0 0 1 78 72 Z", family = "clouds"),
    VisionBoardShape("cloud2", "Nube esponjosa", "M20 75 A14 14 0 0 1 18 48 A17 17 0 0 1 40 32 A15 15 0 0 1 60 28 A18 18 0 0 1 85 45 A13 13 0 0 1 80 75 Z", family = "clouds"),
    VisionBoardShape("cloud3", "Nube compacta", "M30 70 A12 12 0 0 1 28 46 A15 15 0 0 1 55 35 A13 13 0 0 1 75 52 A11 11 0 0 1 72 70 Z", family = "clouds"),

    // ===== Corazones =====
    VisionBoardShape("heart", "Corazón", "M50 90 C10 62 5 35 22 20 C34 10 47 15 50 28 C53 15 66 10 78 20 C95 35 90 62 50 90 Z", family = "hearts"),
    VisionBoardShape("heart2", "Corazón redondo", "M50 88 C15 65 8 40 25 24 C36 14 48 18 50 30 C52 18 64 14 75 24 C92 40 85 65 50 88 Z", family = "hearts"),
    VisionBoardShape("heartBroken", "Corazón roto", "M48 90 C15 65 8 40 22 24 C32 14 44 18 48 28 Z M52 90 C85 65 92 40 78 24 C68 14 56 18 52 28 Z", family = "hearts"),

    // ===== Banners =====
    VisionBoardShape("label", "Etiqueta", "M12 50 L32 12 H88 V88 H32 Z", family = "banners"),
    VisionBoardShape("ribbon", "Cinta", "M10 22 H90 L78 50 L90 78 H10 L22 50 Z", family = "banners"),
    VisionBoardShape("flag", "Bandera", "M14 5 H20 V95 H14 Z M20 10 H86 L71 27 L86 44 H20 Z", family = "banners"),
    VisionBoardShape("scroll", "Pergamino", "M10 30 Q5 20 15 18 H85 Q95 20 90 30 V70 Q95 80 85 82 H15 Q5 80 10 70 Z", family = "banners"),
    VisionBoardShape("pennant", "Banderín", "M8 15 H88 L60 50 L88 85 H8 Z", family = "banners"),

    // ===== Lágrimas =====
    VisionBoardShape("drop", "Gota", "M50 5 C68 32 85 53 85 68 A35 35 0 1 1 15 68 C15 53 32 32 50 5 Z", family = "teardrops"),
    VisionBoardShape("dropWide", "Gota ancha", "M50 10 C72 35 88 55 88 70 A38 38 0 1 1 12 70 C12 55 28 35 50 10 Z", family = "teardrops"),
    VisionBoardShape("dropInverted", "Gota invertida", "M50 95 C32 68 15 47 15 32 A35 35 0 1 1 85 32 C85 47 68 68 50 95 Z", family = "teardrops"),

    // ===== Engranajes =====
    VisionBoardShape("gear6", "Engranaje 6", gearPath(6, 44.0, 34.0, 12.0), evenOdd = true, family = "gears"),
    VisionBoardShape("gear8", "Engranaje 8", gearPath(8, 44.0, 36.0, 14.0), evenOdd = true, family = "gears"),
    VisionBoardShape("gear10", "Engranaje 10", gearPath(10, 44.0, 37.0, 15.0), evenOdd = true, family = "gears"),
    VisionBoardShape("gear12", "Engranaje 12", gearPath(12, 43.0, 38.0, 16.0), evenOdd = true, family = "gears"),

    // ===== Asteriscos =====
    VisionBoardShape("asterisk4", "Destello 4", starPath(4, 0.1), family = "asterisks"),
    VisionBoardShape("asterisk5", "Asterisco 5", starPath(5, 0.15), family = "asterisks"),
    VisionBoardShape("asterisk6", "Asterisco 6", starPath(6, 0.12), family = "asterisks"),

    // ===== Orgánicas =====
    VisionBoardShape("leaf", "Hoja", "M50 5 C85 20 90 60 50 95 C10 60 15 20 50 5 Z", family = "organic"),
    VisionBoardShape("blob1", "Orgánica 1", blobPath(listOf(1.0, 0.82, 1.1, 0.75, 1.05, 0.88, 0.95, 0.8)), family = "organic"),
    VisionBoardShape("blob2", "Orgánica 2", blobPath(listOf(0.9, 1.1, 0.85, 1.15, 0.9, 1.05, 0.8, 1.0)), family = "organic"),
    VisionBoardShape("blob3", "Orgánica 3", blobPath(listOf(1.1, 0.9, 1.05, 0.78, 1.12, 0.85, 1.0, 0.92)), family = "organic"),
    VisionBoardShape("blob4", "Orgánica 4", blobPath(listOf(0.85, 1.05, 0.95, 1.1, 0.8, 1.0, 1.08, 0.88)), family = "organic"),

    // ===== Abstractas =====
    VisionBoardShape("diamond", "Diamante", "M20 35 L35 10 L65 10 L80 35 L50 95 Z", family = "abstract"),
    VisionBoardShape("lightning", "Rayo", "M55 5 L20 55 H42 L35 95 L82 40 H55 Z", family = "abstract"),
    VisionBoardShape("mountain", "Montaña", "M5 90 L30 40 L45 60 L65 20 L95 90 Z", family = "abstract"),
    VisionBoardShape("sun", "Sol", "M28 50 A22 22 0 1 0 72 50 A22 22 0 1 0 28 50 Z M75.7 45.9 L96 50 L75.7 54.1 Z M71 65.3 L82.5 82.5 L65.3 71 Z M54.1 75.7 L50 96 L45.9 75.7 Z M34.7 71 L17.5 82.5 L29 65.3 Z M24.3 54.1 L4 50 L24.3 45.9 Z M29 34.7 L17.5 17.5 L34.7 29 Z M45.9 24.3 L50 4 L54.1 24.3 Z M65.3 29 L82.5 17.5 L71 34.7 Z", family = "abstract"),
    VisionBoardShape("moon", "Luna", "M50 5 A45 45 0 1 0 50 95 A35 35 0 1 1 50 5 Z", evenOdd = true, family = "abstract"),
    VisionBoardShape("frame", "Marco", "M5 5 H95 V95 H5 Z M20 20 V80 H80 V20 Z", evenOdd = true, family = "abstract"),
    VisionBoardShape("divider", "Divisor decorativo", "M5 46 H38 V54 H5 Z M62 46 H95 V54 H62 Z M50 36 L60 46 L50 56 L40 46 Z", family = "abstract"),
    VisionBoardShape("quarterCircle", "Cuarto de círculo", "M5 95 L5 5 A90 90 0 0 1 95 95 Z", family = "abstract"),
    VisionBoardShape("vennCircles", "Círculos superpuestos", "M35 30 A25 25 0 1 0 35.01 30 Z M65 30 A25 25 0 1 0 65.01 30 Z", family = "abstract"),
)

fun shapeOf(id: String?): VisionBoardShape =
    SHAPE_CATALOG.firstOrNull { it.id == id } ?: SHAPE_CATALOG[0]

fun shapesByFamily(family: String): List<VisionBoardShape> =
    SHAPE_CATALOG.filter { it.family == family }
