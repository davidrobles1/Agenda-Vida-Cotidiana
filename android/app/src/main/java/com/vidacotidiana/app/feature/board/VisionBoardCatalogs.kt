package com.vidacotidiana.app.feature.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.vidacotidiana.app.core.ui.VidaFonts
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Los catálogos del Vision Board que no son formas: caligrafías, marcos,
 * gráficas, cuadrículas, temas del tablero, colores de forma y animaciones.
 *
 * Todos los identificadores son los de la Web
 * (los módulos de `web/src/features/visionboard`) y por el mismo motivo que explica
 * `VisionBoardShapes.kt`: viajan dentro del `data` del elemento, que es el
 * mismo JSON en las dos plataformas. Lo que cambia aquí es solo cómo se
 * dibuja, nunca qué se guarda.
 */

/* =========================================================
   CALIGRAFÍAS  ·  data.font
   ========================================================= */

data class VisionBoardFont(val id: String, val label: String, val family: FontFamily)

val VISION_BOARD_FONTS = listOf(
    VisionBoardFont("sans", "Sans", VidaFonts.Inter),
    VisionBoardFont("serif", "Serif", VidaFonts.Fraunces),
    VisionBoardFont("modern", "Moderna", VidaFonts.Manrope),
    VisionBoardFont("elegant", "Elegante", VidaFonts.CormorantGaramond),
    VisionBoardFont("editorial", "Editorial", VidaFonts.PlayfairDisplay),
    VisionBoardFont("classic", "Clásica", VidaFonts.LibreBaskerville),
    VisionBoardFont("fashion", "Fashion", VidaFonts.BodoniModa),
    VisionBoardFont("script", "Caligráfica", VidaFonts.AlexBrush),
    VisionBoardFont("handwritten", "Manuscrita", VidaFonts.Caveat),
    VisionBoardFont("parisienne", "Parisienne", VidaFonts.Parisienne),
    VisionBoardFont("extravagant", "Extravagante", VidaFonts.Lobster),
    VisionBoardFont("playful", "Divertida", VidaFonts.Pacifico),
    VisionBoardFont("mono", "Mono", VidaFonts.CourierPrime),
    VisionBoardFont("typewriter", "Máquina", VidaFonts.IbmPlexMono),
)

const val DEFAULT_VISION_BOARD_FONT = "sans"

/** 15 px es lo que valía `.elementText` antes de que existiera el tamaño. */
const val DEFAULT_TEXT_FONT_SIZE = 15
const val MIN_TEXT_FONT_SIZE = 10
const val MAX_TEXT_FONT_SIZE = 160
const val TEXT_FONT_SIZE_STEP = 2

fun fontFamilyOf(id: String?): FontFamily =
    VISION_BOARD_FONTS.firstOrNull { it.id == id }?.family ?: VISION_BOARD_FONTS[0].family

fun fontSizeOf(value: Int?): Int =
    (value ?: DEFAULT_TEXT_FONT_SIZE).coerceIn(MIN_TEXT_FONT_SIZE, MAX_TEXT_FONT_SIZE)

/* =========================================================
   MARCOS DE IMAGEN  ·  data.frameStyle
   ========================================================= */

data class VisionBoardFrame(val id: String?, val label: String)

val FRAME_OPTIONS = listOf(
    VisionBoardFrame(null, "Ninguno"),
    VisionBoardFrame("circle", "Círculo"),
    VisionBoardFrame("hexagon", "Hexágono"),
    VisionBoardFrame("rounded", "Redondeado"),
    VisionBoardFrame("polaroid", "Polaroid"),
    VisionBoardFrame("film", "Película"),
)

/* =========================================================
   GRÁFICAS  ·  data.chartType
   ========================================================= */

data class VisionBoardChart(val id: String, val label: String)

val CHART_OPTIONS = listOf(
    VisionBoardChart("bar", "Barras"),
    VisionBoardChart("line", "Línea"),
    VisionBoardChart("area", "Área"),
    VisionBoardChart("pie", "Pastel"),
    VisionBoardChart("donut", "Anillos"),
    VisionBoardChart("gauge", "Medidor"),
)

/** Los mismos datos de ejemplo y los mismos colores que dibuja la Web. */
val CHART_SAMPLE_SERIES = listOf(12f, 19f, 14f, 24f, 18f, 30f, 22f)
val CHART_SAMPLE_SLICES = listOf(30f, 25f, 20f, 15f, 10f)
val CHART_SLICE_COLORS = listOf(
    Color(0xFF2C5F8C), Color(0xFFB47B48), Color(0xFF4D6B46), Color(0xFF8A5A12), Color(0xFF2F5F66),
)

/* =========================================================
   CUADRÍCULAS  ·  data.layout
   ========================================================= */

data class VisionBoardGrid(
    val id: String,
    val label: String,
    val rows: Int,
    val cols: Int,
    val cells: Int,
    /** Índice de celda que ocupa dos filas — solo en «Mosaico». */
    val spanCell: Int = -1,
)

val GRID_OPTIONS = listOf(
    VisionBoardGrid("twoH", "2 horizontal", rows = 2, cols = 1, cells = 2),
    VisionBoardGrid("twoV", "2 vertical", rows = 1, cols = 2, cells = 2),
    VisionBoardGrid("three", "3 en fila", rows = 1, cols = 3, cells = 3),
    VisionBoardGrid("four", "Cuadrícula 2×2", rows = 2, cols = 2, cells = 4),
    VisionBoardGrid("mosaic", "Mosaico", rows = 2, cols = 3, cells = 5, spanCell = 0),
)

fun gridOf(id: String?): VisionBoardGrid =
    GRID_OPTIONS.firstOrNull { it.id == id } ?: GRID_OPTIONS[3]

/** Los cuatro tonos que la Web recicla en cuadrículas y gráficas. */
val GRID_CELL_COLORS = listOf(
    Color(0xFF2C5F8C), Color(0xFFB47B48), Color(0xFF4D6B46), Color(0xFF8A5A12), Color(0xFF2F5F66),
)

/* =========================================================
   TEMAS DEL TABLERO  ·  board.theme
   ========================================================= */

data class BoardThemeColors(
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color,
    val selectionContainer: Color,
    val noteBackground: Color,
    val shapeBackground: Color,
)

data class BoardTheme(
    val id: String,
    val name: String,
    val description: String,
    val colors: BoardThemeColors,
)

val BOARD_THEMES = listOf(
    BoardTheme(
        "LIGHT", "Claro",
        "El tono neutro por defecto — mismo look que el resto de Vida Cotidiana.",
        BoardThemeColors(
            background = Color(0xFFFFFCF6), surface = Color(0xFFF5EEDF),
            text = Color(0xFF2C425B), textSecondary = Color(0xFF596D83),
            border = Color(0xFF6E8192), accent = Color(0xFF2C5F8C),
            selectionContainer = Color(0xFFE4EEF6), noteBackground = Color(0xFFF3E4C4),
            shapeBackground = Color(0xFFA9CBE8),
        ),
    ),
    BoardTheme(
        "DARK", "Oscuro",
        "Fondo profundo con acentos claros — ideal para trabajar de noche.",
        BoardThemeColors(
            background = Color(0xFF1C2430), surface = Color(0xFF2B3547),
            text = Color(0xFFE8EDF3), textSecondary = Color(0xFFA8B6C6),
            border = Color(0xFF3C4A5C), accent = Color(0xFF6FA8DC),
            selectionContainer = Color(0xFF2A3A4A), noteBackground = Color(0xFF3A3320),
            shapeBackground = Color(0xFF1F4F73),
        ),
    ),
    BoardTheme(
        "PAPER", "Papel",
        "Crema cálido y tinta oscura — el corcho físico de siempre.",
        BoardThemeColors(
            background = Color(0xFFF4ECD8), surface = Color(0xFFF4EAD2),
            text = Color(0xFF4A3F2E), textSecondary = Color(0xFF746750),
            border = Color(0xFFCDBFA0), accent = Color(0xFFA9762C),
            selectionContainer = Color(0xFFECDCB8), noteBackground = Color(0xFFF0D9A6),
            shapeBackground = Color(0xFFE0B06A),
        ),
    ),
    BoardTheme(
        "NATURAL", "Natural",
        "Verdes y tonos de tierra — para tableros de bienestar y hábitos.",
        BoardThemeColors(
            background = Color(0xFFEEF2E6), surface = Color(0xFFEAF0E0),
            text = Color(0xFF33422C), textSecondary = Color(0xFF616F54),
            border = Color(0xFFB8C8A8), accent = Color(0xFF5C8A4A),
            selectionContainer = Color(0xFFDCEBD0), noteBackground = Color(0xFFE2ECD0),
            shapeBackground = Color(0xFF9CC687),
        ),
    ),
    BoardTheme(
        "CALM", "Calma",
        "Azules y lavandas suaves, baja saturación — para enfocarse sin ruido visual.",
        BoardThemeColors(
            background = Color(0xFFEEF1F7), surface = Color(0xFFECEEF7),
            text = Color(0xFF3B4256), textSecondary = Color(0xFF626B7C),
            border = Color(0xFFC3CBDC), accent = Color(0xFF798CBE),
            selectionContainer = Color(0xFFE1E5F3), noteBackground = Color(0xFFE6E9F5),
            shapeBackground = Color(0xFFAAB6DD),
        ),
    ),
    BoardTheme(
        "ENERGY", "Energía",
        "Naranjas vibrantes sobre un fondo cálido — para metas y motivación.",
        BoardThemeColors(
            background = Color(0xFFFFF5EC), surface = Color(0xFFFFF1E4),
            text = Color(0xFF402313), textSecondary = Color(0xFF7A4A2A),
            border = Color(0xFFF0B285), accent = Color(0xFFE8622C),
            selectionContainer = Color(0xFFFFD9B8), noteBackground = Color(0xFFFFE0B8),
            shapeBackground = Color(0xFFFFB37A),
        ),
    ),
)

fun boardThemeOf(id: String?): BoardTheme =
    BOARD_THEMES.firstOrNull { it.id == id } ?: BOARD_THEMES[0]

/* =========================================================
   COLOR DE UNA FORMA  ·  data.fill / data.textColor
   ========================================================= */

data class ColorSwatch(val value: String, val label: String)

/**
 * Los diez fondos de la Web, con su nota al pie incluida: cada tono se
 * eligió para que el MEJOR de las dos tintas llegue a 4.5:1, y así la
 * sugerencia automática nunca tenga que recurrir a blanco o negro puros.
 */
val SHAPE_FILL_SWATCHES = listOf(
    ColorSwatch("#2f6188", "Azul tinta"),
    ColorSwatch("#3f6b7c", "Azul niebla"),
    ColorSwatch("#4f7a5b", "Verde salvia"),
    ColorSwatch("#9aab68", "Verde oliva"),
    ColorSwatch("#a85636", "Terracota"),
    ColorSwatch("#c9a227", "Ocre"),
    ColorSwatch("#8a5a7c", "Ciruela"),
    ColorSwatch("#e8dcc8", "Arena"),
    ColorSwatch("#f4f1ea", "Marfil"),
    ColorSwatch("#2c3038", "Carbón"),
)

val SHAPE_TEXT_SWATCHES = listOf(
    ColorSwatch("#faf7f0", "Marfil"),
    ColorSwatch("#ffffff", "Blanco"),
    ColorSwatch("#e8dcc8", "Arena clara"),
    ColorSwatch("#2c3038", "Carbón"),
    ColorSwatch("#24303c", "Azul marino"),
    ColorSwatch("#3d2f24", "Café oscuro"),
    ColorSwatch("#1a1a1a", "Negro suave"),
)

const val DEFAULT_SHAPE_FILL = "#2f6188"
private const val LIGHT_INK = "#faf7f0"
private const val DARK_INK = "#2c3038"

fun parseHexColor(hex: String?): Color? {
    val value = hex?.trim()?.removePrefix("#") ?: return null
    if (value.length != 6) return null
    return runCatching { Color(value.toLong(16) or 0xFF000000L) }.getOrNull()
}

private fun relativeLuminance(color: Color): Double {
    fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

/** Contraste WCAG 2.1 real entre dos colores. `null` si alguno no es válido. */
fun contrastRatio(a: String?, b: String?): Double? {
    val ca = parseHexColor(a) ?: return null
    val cb = parseHexColor(b) ?: return null
    val la = relativeLuminance(ca)
    val lb = relativeLuminance(cb)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

fun meetsAA(background: String?, text: String?): Boolean = (contrastRatio(background, text) ?: 0.0) >= 4.5

/**
 * LA REGLA DEL CONTRASTE, la misma que la Web: al cambiar el fondo, el
 * color del texto se recalcula por LUMINANCIA, no por el nombre del color.
 * Devuelve marfil o carbón, los dos extremos cálidos de la aplicación.
 */
fun suggestTextColor(fill: String?): String {
    val light = contrastRatio(fill, LIGHT_INK) ?: 0.0
    val dark = contrastRatio(fill, DARK_INK) ?: 0.0
    return if (light >= dark) LIGHT_INK else DARK_INK
}

/* =========================================================
   ANIMACIONES DE ENTRADA  ·  data.entrance
   ========================================================= */

data class EntranceAnimation(val id: String, val label: String)

val ENTRANCE_ANIMATIONS = listOf(
    EntranceAnimation("fadeIn", "Aparecer"),
    EntranceAnimation("slideUp", "Deslizar desde abajo"),
    EntranceAnimation("slideLeft", "Deslizar desde la izquierda"),
    EntranceAnimation("popIn", "Aparecer y crecer"),
    EntranceAnimation("bounceIn", "Rebote"),
    EntranceAnimation("pulse", "Pulso"),
)

/* =========================================================
   TAMAÑOS DE ALTA  ·  los mismos de visionBoardElementDefaults.ts
   ========================================================= */

fun defaultSizeFor(type: String): Pair<Float, Float> = when (type) {
    "TEXT" -> 220f to 60f
    "NOTE" -> 200f to 150f
    "STICKER" -> 80f to 80f
    "IMAGE" -> 240f to 160f
    "SHAPE" -> 120f to 120f
    "TABLE" -> 260f to 130f
    "CHART" -> 260f to 170f
    "GRID" -> 260f to 180f
    else -> 160f to 120f
}
