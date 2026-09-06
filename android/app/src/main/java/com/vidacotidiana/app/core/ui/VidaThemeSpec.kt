package com.vidacotidiana.app.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * ADR-023 — LOS NUEVE TEMAS, con su identidad completa.
 *
 * No son nueve paletas. Cada agenda cambia además la familia tipográfica, el
 * peso de sus titulares, la escala de radios, la sombra, el tratamiento de los
 * antetítulos y hasta el grosor de los bordes: Neo no tiene radio y usa sombra
 * dura desplazada, Lumen no tiene sombra y su filete es de un píxel, Studio
 * titula en cursiva de serif, Papel escribe los antetítulos en versalitas y las
 * notas a mano.
 *
 * POR QUÉ UNA ESPECIFICACIÓN PROPIA Y NO SOLO `ColorScheme`. Material 3 no
 * tiene dónde guardar la mitad de eso —ni `--second`, ni `--ink3`, ni
 * éxito/aviso/peligro, ni una escala de radios por tema—. Así que la fuente de
 * verdad es este `VidaThemeSpec` y de él se DERIVA el `ColorScheme` de M3
 * (ver `Theme.kt`), exactamente el mismo puente que la Web ya usa entre `--ink`
 * y `--color-text`: una sola fuente, dos vocabularios.
 *
 * Todos los valores salen de `web/src/themes.css`. Ninguno está inventado.
 */

/** Identidad de cada agenda. El orden es el del selector de la Web. */
enum class VisualTheme(val id: String, val label: String, val tagline: String) {
    AURORA("aurora", "Aurora", "Noche premium"),
    LUMEN("lumen", "Lumen", "Aire y tipografía"),
    NEO("neo", "Neo", "Cartel de alto contraste"),
    CALM("calm", "Calm", "Cálido y orgánico"),
    STUDIO("studio", "Studio", "Revista"),
    PAPEL("papel", "Papel", "Cuaderno"),
    MINIMAL("minimal", "Premium Minimal", "Escala dramática"),
    PRODUCTIVITY("productivity", "Modern Productivity", "Centro de control"),
    ORGANIC("organic", "Organic / Human", "Formas vivas");

    companion object {
        val DEFAULT = PAPEL
        fun from(id: String?): VisualTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Cómo trata cada tema los antetítulos ("TU DÍA", "RESUMEN", "notas").
 * Es un rasgo de identidad, no una variación de color: en Papel son versalitas
 * de la serif en ocre, en Studio cursiva de revista, en Lumen caja alta muy
 * espaciada. Se modela como enum porque el componente que los pinta necesita
 * decidir mayúsculas, familia y tracking a la vez.
 */
enum class EyebrowStyle { UPPER_TRACKED, SMALL_CAPS_SERIF, ITALIC_SERIF, HEAVY_UPPER }

/** Sombra de tema. Neo la quiere dura y desplazada; Lumen y Studio, ninguna. */
@Immutable
data class VidaElevation(
    val card: Dp,
    /** Desplazamiento de la sombra dura de Neo. `Dp.Unspecified` = sombra normal. */
    val hardOffset: Dp = Dp.Unspecified,
) {
    val isHard: Boolean get() = hardOffset != Dp.Unspecified
}

/** Escala de radios. `--r-scale` de cada tema, ya resuelto a dp. */
@Immutable
data class VidaRadii(val card: Dp, val control: Dp, val pill: Dp = 999.dp)

/** Las cuatro familias que puede usar un tema. */
@Immutable
data class VidaFontSet(
    val display: FontFamily,
    val body: FontFamily,
    val label: FontFamily,
    /** Solo Papel la tiene. Nula en el resto: nadie más escribe a mano. */
    val hand: FontFamily? = null,
    /** Peso de los titulares — de 200 en Lumen a 900 en Neo. */
    val displayWeight: FontWeight = FontWeight.SemiBold,
    /** Studio titula en cursiva. */
    val displayItalic: Boolean = false,
    /** Neo titula en caja alta. */
    val displayUppercase: Boolean = false,
)

/** Todo lo que define visualmente una agenda. */
@Immutable
data class VidaThemeSpec(
    val theme: VisualTheme,
    val colors: VidaColors,
    val fonts: VidaFontSet,
    val radii: VidaRadii,
    val elevation: VidaElevation,
    val eyebrow: EyebrowStyle,
    /** Neo dibuja 2 dp de borde; el resto, 1 dp. */
    val borderWidth: Dp = 1.dp,
    val isDark: Boolean = false,
    /** `--lh` de la Web, aplicado como altura de línea del cuerpo. */
    val bodyLineHeight: TextUnit = 22.sp,
)

val LocalVidaThemeSpec = staticCompositionLocalOf { VidaThemes.of(VisualTheme.DEFAULT) }

/** Atajo de lectura desde cualquier composable: `VidaTheme.spec.radii.card`. */
private fun hex(v: Long) = Color(v)

/**
 * Catálogo con los nueve temas. Cada bloque es la transcripción de su bloque
 * homónimo en `themes.css`, incluida la regla de que **el rojo nunca se usa
 * para un aviso** (queda reservado a lo que está mal).
 */
object VidaThemes {

    private fun colors(
        surface: Long, surfaceVariant: Long, sunken: Long, surfaceElevated: Long,
        text: Long, textSecondary: Long, textTertiary: Long,
        primary: Long, primaryContainer: Long, onPrimary: Long,
        second: Long, secondContainer: Long,
        success: Long, successContainer: Long,
        warning: Long, warningContainer: Long,
        error: Long, errorContainer: Long,
        line: Long, border: Long,
    ) = VidaColors(
        primary = hex(primary), onPrimary = hex(onPrimary), primaryContainer = hex(primaryContainer),
        success = hex(success), successText = hex(success), successContainer = hex(successContainer),
        warning = hex(warning), warningText = hex(warning), warningContainer = hex(warningContainer),
        info = hex(primary), infoText = hex(primary), infoContainer = hex(primaryContainer),
        error = hex(error), errorContainer = hex(errorContainer),
        surface = hex(surface), surfaceVariant = hex(surfaceVariant),
        border = hex(border), text = hex(text), textSecondary = hex(textSecondary),
        sunken = hex(sunken), surfaceElevated = hex(surfaceElevated),
        textTertiary = hex(textTertiary),
        second = hex(second), secondContainer = hex(secondContainer),
        line = hex(line),
    )

    private val aurora = VidaThemeSpec(
        theme = VisualTheme.AURORA,
        colors = colors(
            surface = 0xFF0A0D14, surfaceVariant = 0xFF141926, sunken = 0xFF0F1420, surfaceElevated = 0xFF1B2130,
            text = 0xFFEAF0FA, textSecondary = 0xFF9FB0C9, textTertiary = 0xFF6B7C96,
            primary = 0xFF84B0FF, primaryContainer = 0xFF1D2942, onPrimary = 0xFF0A0D14,
            second = 0xFFC3B0FF, secondContainer = 0xFF2A2444,
            success = 0xFF63D59A, successContainer = 0xFF14332A,
            warning = 0xFFF0C169, warningContainer = 0xFF3A2F18,
            error = 0xFFFF8A80, errorContainer = 0xFF3A2220,
            line = 0xFF1E2432, border = 0xFF2B3242,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Sora, body = VidaFonts.Inter, label = VidaFonts.Sora,
            displayWeight = FontWeight.Light,
        ),
        radii = VidaRadii(card = 18.dp, control = 12.dp),
        elevation = VidaElevation(card = 2.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
        isDark = true,
    )

    private val lumen = VidaThemeSpec(
        theme = VisualTheme.LUMEN,
        colors = colors(
            surface = 0xFFFDFDFC, surfaceVariant = 0xFFFDFDFC, sunken = 0xFFF4F4F2, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF0F1012, textSecondary = 0xFF5C6067, textTertiary = 0xFF9AA0A8,
            primary = 0xFF0F1012, primaryContainer = 0xFFEEEEEC, onPrimary = 0xFFFFFFFF,
            second = 0xFF8A7F6D, secondContainer = 0xFFF1EFE9,
            success = 0xFF3D7A4F, successContainer = 0xFFECF2ED,
            warning = 0xFF8F6414, warningContainer = 0xFFF6F0E1,
            error = 0xFFA43A33, errorContainer = 0xFFF8EAE8,
            line = 0xFFEDEDEB, border = 0xFFDFDFDC,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Manrope, body = VidaFonts.Manrope, label = VidaFonts.Manrope,
            displayWeight = FontWeight.ExtraLight,
        ),
        radii = VidaRadii(card = 5.dp, control = 4.dp),
        elevation = VidaElevation(card = 0.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
    )

    private val neo = VidaThemeSpec(
        theme = VisualTheme.NEO,
        colors = colors(
            surface = 0xFFECECEA, surfaceVariant = 0xFFFFFFFF, sunken = 0xFFDEDCD8, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF000000, textSecondary = 0xFF3D3D3D, textTertiary = 0xFF6F6F6F,
            primary = 0xFF000000, primaryContainer = 0xFFE0E6FF, onPrimary = 0xFFFFFFFF,
            second = 0xFF1F3FD6, secondContainer = 0xFFE0E6FF,
            success = 0xFF0A7D3F, successContainer = 0xFFDCF2E5,
            warning = 0xFFB45309, warningContainer = 0xFFFDECCF,
            error = 0xFFC1121F, errorContainer = 0xFFFBDCDD,
            line = 0xFFD4D4D2, border = 0xFF000000,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Archivo, body = VidaFonts.Inter, label = VidaFonts.CourierPrime,
            displayWeight = FontWeight.Black, displayUppercase = true,
        ),
        radii = VidaRadii(card = 0.dp, control = 0.dp, pill = 0.dp),
        elevation = VidaElevation(card = 0.dp, hardOffset = 3.dp),
        eyebrow = EyebrowStyle.HEAVY_UPPER,
        borderWidth = 2.dp,
    )

    private val calm = VidaThemeSpec(
        theme = VisualTheme.CALM,
        colors = colors(
            surface = 0xFFF8F4EE, surfaceVariant = 0xFFFFFDFA, sunken = 0xFFF0EAE1, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF33291F, textSecondary = 0xFF6F6154, textTertiary = 0xFFA2968A,
            primary = 0xFF7D8F68, primaryContainer = 0xFFE7ECE0, onPrimary = 0xFFFFFFFF,
            second = 0xFFC08A5E, secondContainer = 0xFFF6E8DC,
            success = 0xFF5B8A5E, successContainer = 0xFFE6EFE6,
            warning = 0xFFB5813A, warningContainer = 0xFFF8ECD9,
            error = 0xFFB05545, errorContainer = 0xFFF8E4E0,
            line = 0xFFEDE6DC, border = 0xFFD8CEC1,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.PlusJakarta, body = VidaFonts.PlusJakarta, label = VidaFonts.PlusJakarta,
            displayWeight = FontWeight.Bold,
        ),
        radii = VidaRadii(card = 26.dp, control = 18.dp),
        elevation = VidaElevation(card = 6.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
    )

    private val studio = VidaThemeSpec(
        theme = VisualTheme.STUDIO,
        colors = colors(
            surface = 0xFFF2EFE9, surfaceVariant = 0xFFFAF8F4, sunken = 0xFFE9E5DD, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF191713, textSecondary = 0xFF5F594F, textTertiary = 0xFF948D81,
            primary = 0xFF191713, primaryContainer = 0xFFE6E2DA, onPrimary = 0xFFFAF8F4,
            second = 0xFF96412F, secondContainer = 0xFFF3E2DD,
            success = 0xFF41684A, successContainer = 0xFFE6EDE6,
            warning = 0xFF8A6116, warningContainer = 0xFFF4ECDA,
            error = 0xFF96412F, errorContainer = 0xFFF3E2DD,
            line = 0xFFE4DFD6, border = 0xFFCFC8BB,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.InstrumentSerif, body = VidaFonts.Inter, label = VidaFonts.Inter,
            displayWeight = FontWeight.Normal,
        ),
        radii = VidaRadii(card = 3.dp, control = 2.dp),
        elevation = VidaElevation(card = 0.dp),
        eyebrow = EyebrowStyle.ITALIC_SERIF,
    )

    private val papel = VidaThemeSpec(
        theme = VisualTheme.PAPEL,
        colors = colors(
            surface = 0xFFF6F1E4, surfaceVariant = 0xFFFFFCF4, sunken = 0xFFEFE8D8, surfaceElevated = 0xFFFFFDF8,
            text = 0xFF24303C, textSecondary = 0xFF5B6A79, textTertiary = 0xFF93A0AD,
            primary = 0xFF2F6188, primaryContainer = 0xFFE2EDF5, onPrimary = 0xFFFFFFFF,
            second = 0xFFBB6440, secondContainer = 0xFFF8E7DD,
            success = 0xFF3F7D52, successContainer = 0xFFE7F0E8,
            warning = 0xFFA2690F, warningContainer = 0xFFFBEED6,
            error = 0xFFAD3A33, errorContainer = 0xFFFAE7E5,
            line = 0xFFE8E1D2, border = 0xFFD3CBB9,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Fraunces, body = VidaFonts.Inter, label = VidaFonts.Inter,
            hand = VidaFonts.Caveat, displayWeight = FontWeight.Medium,
        ),
        radii = VidaRadii(card = 16.dp, control = 10.dp),
        elevation = VidaElevation(card = 1.dp),
        eyebrow = EyebrowStyle.SMALL_CAPS_SERIF,
    )

    private val minimal = VidaThemeSpec(
        theme = VisualTheme.MINIMAL,
        colors = colors(
            surface = 0xFFFAF8F3, surfaceVariant = 0xFFFFFFFF, sunken = 0xFFF1EEE7, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF1A232E, textSecondary = 0xFF7A8290, textTertiary = 0xFFA5ABB5,
            primary = 0xFF23425F, primaryContainer = 0xFFEEF1F4, onPrimary = 0xFFFFFFFF,
            second = 0xFF23425F, secondContainer = 0xFFEEF1F4,
            success = 0xFF3F7D52, successContainer = 0xFFE7F0E8,
            warning = 0xFFA2690F, warningContainer = 0xFFFBEED6,
            error = 0xFFAD3A33, errorContainer = 0xFFFAE7E5,
            line = 0xFFEDEAE2, border = 0xFFE4DFD3,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Fraunces, body = VidaFonts.Inter, label = VidaFonts.CourierPrime,
            displayWeight = FontWeight.Normal,
        ),
        radii = VidaRadii(card = 3.dp, control = 2.dp),
        elevation = VidaElevation(card = 0.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
    )

    private val productivity = VidaThemeSpec(
        theme = VisualTheme.PRODUCTIVITY,
        colors = colors(
            surface = 0xFFF2EDE1, surfaceVariant = 0xFFFFFDF8, sunken = 0xFFE9E3D4, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF1B2A3A, textSecondary = 0xFF5C6C80, textTertiary = 0xFF8D99A8,
            primary = 0xFF2C5F8C, primaryContainer = 0xFFE4EEF6, onPrimary = 0xFFFFFFFF,
            second = 0xFFE08A3E, secondContainer = 0xFFF7E6D3,
            success = 0xFF3F7D52, successContainer = 0xFFE7F0E8,
            warning = 0xFF8B5E36, warningContainer = 0xFFF7E6D3,
            error = 0xFFAD3A33, errorContainer = 0xFFFAE7E5,
            line = 0xFFE2DCCB, border = 0xFF6E8192,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Fraunces, body = VidaFonts.Inter, label = VidaFonts.CourierPrime,
            displayWeight = FontWeight.SemiBold,
        ),
        radii = VidaRadii(card = 11.dp, control = 8.dp),
        elevation = VidaElevation(card = 3.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
    )

    private val organic = VidaThemeSpec(
        theme = VisualTheme.ORGANIC,
        colors = colors(
            surface = 0xFFEEF0DF, surfaceVariant = 0xFFFBFAF1, sunken = 0xFFE4E7D2, surfaceElevated = 0xFFFFFFFF,
            text = 0xFF33402F, textSecondary = 0xFF6D7860, textTertiary = 0xFF93A089,
            primary = 0xFF5B7A5E, primaryContainer = 0xFFDFE6D7, onPrimary = 0xFFFFFFFF,
            second = 0xFFC17A4A, secondContainer = 0xFFF2E2D3,
            success = 0xFF5B7A5E, successContainer = 0xFFDFE6D7,
            warning = 0xFF8B5E36, warningContainer = 0xFFF2E2D3,
            error = 0xFFAD3A33, errorContainer = 0xFFFAE7E5,
            line = 0xFFDDE0CB, border = 0xFF7D8A72,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Fraunces, body = VidaFonts.Inter, label = VidaFonts.CourierPrime,
            displayWeight = FontWeight.SemiBold,
        ),
        // `--radius-control: 999px` en la Web: los controles de Organic son cápsulas.
        radii = VidaRadii(card = 24.dp, control = 999.dp),
        elevation = VidaElevation(card = 5.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
    )

    private val all = mapOf(
        VisualTheme.AURORA to aurora,
        VisualTheme.LUMEN to lumen,
        VisualTheme.NEO to neo,
        VisualTheme.CALM to calm,
        VisualTheme.STUDIO to studio,
        VisualTheme.PAPEL to papel,
        VisualTheme.MINIMAL to minimal,
        VisualTheme.PRODUCTIVITY to productivity,
        VisualTheme.ORGANIC to organic,
    )

    fun of(theme: VisualTheme): VidaThemeSpec = all.getValue(theme)

    /** Para el selector de Ajustes y para las previsualizaciones parametrizadas. */
    val catalogue: List<VidaThemeSpec> = VisualTheme.entries.map { all.getValue(it) }
}
