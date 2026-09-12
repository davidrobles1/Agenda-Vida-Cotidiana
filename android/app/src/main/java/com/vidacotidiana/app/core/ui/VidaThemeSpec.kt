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

/**
 * Identidad de cada agenda. El orden es el del selector de la Web.
 *
 * CLARO y NOCHE van primero porque son las dos del ARTEFACTO MAESTRO
 * (https://claude.ai/code/artifact/b31707e4-2719-4a26-b7e8-b870ded5a0ec), que
 * es la fuente de verdad visual aprobada. No son un segundo lenguaje: son dos
 * juegos de valores más dentro del mismo sistema de tokens de ADR-023, que es
 * justamente lo que permite que el artefacto gobierne sin duplicar un solo
 * componente.
 *
 * Las ocho agendas anteriores siguen compilando y funcionando. Si deben
 * retirarse es una decisión de producto abierta — ver `36-artefacto-matriz-
 * implementacion.md` §B-2.
 */
enum class VisualTheme(val id: String, val label: String, val tagline: String) {
    CLARO("claro", "Claro", "El del artefacto"),
    NOCHE("noche", "Noche", "Mismos tokens, otro valor"),
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
        /** El artefacto maestro es la fuente de verdad: Claro es el de serie. */
        val DEFAULT = CLARO
        fun from(id: String?): VisualTheme = entries.firstOrNull { it.id == id } ?: DEFAULT

        /** Las dos del artefacto, que Apariencia muestra arriba y separadas. */
        val ARTEFACTO = listOf(CLARO, NOCHE)
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
    /**
     * Peso de las CIFRAS, separado del de los titulares.
     *
     * El artefacto maestro los distingue: los títulos van a 600 y las cifras
     * grandes —el «5» de Para hoy, el «27» de En total, los importes— a 700.
     * Con un solo peso no se puede reproducir, y §2 prohíbe aproximar. Por
     * defecto vale lo mismo que `displayWeight`, así que las ocho agendas
     * anteriores no cambian ni un píxel.
     */
    val figureWeight: FontWeight? = null,
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
        /** `--indigo-dd`: solo lo declaran las agendas del artefacto. */
        primaryDeep: Long? = null,
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
        primaryDeep = if (primaryDeep == null) hex(primary) else hex(primaryDeep),
    )

    /**
     * CLARO — transcripción literal de los tokens del artefacto maestro.
     *
     * Cada valor sale del `:root` del artefacto, sin redondear:
     *   --bg #FFFFFF · --ink #101322 · --ink2 #6B7280 · --ink3 #9CA3AF
     *   --line #ECEEF2 · --line3 #E2E5EA · --sunk #F6F7F9 · --sunk2 #FAFBFC
     *   --indigo #4F46E5 · --indigo-c #EEF0FF · --violet #7C3AED
     *   --green #10A37F · --amber2 #C2670A · --rose #E11D48
     *   --r-card 24 · --r-field 18
     *
     * El ROJO se reserva a lo que está mal: la severidad alta de ADR-018 usa
     * ámbar, igual que en el artefacto.
     */
    private val claro = VidaThemeSpec(
        theme = VisualTheme.CLARO,
        colors = colors(
            surface = 0xFFFFFFFF, surfaceVariant = 0xFFFFFFFF, sunken = 0xFFF6F7F9, surfaceElevated = 0xFFFAFBFC,
            text = 0xFF101322, textSecondary = 0xFF6B7280, textTertiary = 0xFF9CA3AF,
            primary = 0xFF4F46E5, primaryContainer = 0xFFEEF0FF, onPrimary = 0xFFFFFFFF,
            second = 0xFF7C3AED, secondContainer = 0xFFF3EEFF,
            success = 0xFF10A37F, successContainer = 0xFFE7F7F1,
            warning = 0xFFC2670A, warningContainer = 0xFFFFF4E0,
            error = 0xFFE11D48, errorContainer = 0xFFFFF0F3,
            line = 0xFFECEEF2, border = 0xFFE2E5EA,
            // --indigo-dd del artefacto: el tono con el que se ESCRIBE sobre
            // primaryContainer. Noche no lo declara porque allí escribir en
            // oscuro sobre #1E2140 sería ilegible: usa su propio primary claro.
            primaryDeep = 0xFF3730A3,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Sora, body = VidaFonts.Inter, label = VidaFonts.Inter,
            hand = VidaFonts.Caveat,
            displayWeight = FontWeight.SemiBold,
            // BLOQUEANTE B-1: falta `sora_700.ttf` en res/font. Hasta que el
            // fichero exista, Compose resuelve W700 al peso más cercano (600).
            figureWeight = FontWeight.Bold,
        ),
        radii = VidaRadii(card = 24.dp, control = 18.dp),
        elevation = VidaElevation(card = 3.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
        bodyLineHeight = 19.sp,
    )

    /** NOCHE — el bloque `[data-vida-theme="noche"]` del artefacto, tal cual. */
    private val noche = VidaThemeSpec(
        theme = VisualTheme.NOCHE,
        colors = colors(
            surface = 0xFF0F1216, surfaceVariant = 0xFF161A20, sunken = 0xFF181C22, surfaceElevated = 0xFF151920,
            text = 0xFFECEFF4, textSecondary = 0xFF9AA3B2, textTertiary = 0xFF7A8494,
            primary = 0xFF7C74F0, primaryContainer = 0xFF1E2140, onPrimary = 0xFF0F1216,
            second = 0xFF9B7BF5, secondContainer = 0xFF211A32,
            success = 0xFF2FC79F, successContainer = 0xFF10291F,
            warning = 0xFFE08A2E, warningContainer = 0xFF2A1E0E,
            error = 0xFFFF5C7A, errorContainer = 0xFF2C141C,
            line = 0xFF232830, border = 0xFF2C323C,
        ),
        fonts = VidaFontSet(
            display = VidaFonts.Sora, body = VidaFonts.Inter, label = VidaFonts.Inter,
            hand = VidaFonts.Caveat,
            displayWeight = FontWeight.SemiBold, figureWeight = FontWeight.Bold,
        ),
        radii = VidaRadii(card = 24.dp, control = 18.dp),
        elevation = VidaElevation(card = 3.dp),
        eyebrow = EyebrowStyle.UPPER_TRACKED,
        isDark = true,
        bodyLineHeight = 19.sp,
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
        VisualTheme.CLARO to claro,
        VisualTheme.NOCHE to noche,
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

    /**
     * TODAS las agendas. Sigue existiendo entera: `AppPreferences` puede
     * devolver cualquiera de las once por su id, y las vistas previas
     * parametrizadas las recorren. Que una agenda no se ofrezca no significa
     * que deje de funcionar.
     */
    val catalogue: List<VidaThemeSpec> = VisualTheme.entries.map { all.getValue(it) }

    /**
     * Lo que el usuario PUEDE elegir: únicamente las dos del artefacto maestro.
     *
     * Las ocho de ADR-023 se conservan en `catalogue` y siguen resolviéndose
     * por id —nada que dependa de ellas se rompe, y un usuario que ya tuviera
     * Papel guardado lo sigue viendo—, pero no se ofrecen en Apariencia: la
     * experiencia visible es la del artefacto y solo la del artefacto.
     */
    val selectable: List<VidaThemeSpec> = VisualTheme.ARTEFACTO.map { all.getValue(it) }
}
