package com.vidacotidiana.app.core.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.vidacotidiana.app.R

/**
 * ADR-023 — las nueve familias tipográficas reales del sistema visual, las
 * mismas que la Web autoaloja con `@fontsource` (UX-001 prohíbe el CDN).
 *
 * Empaquetadas en `res/font` como TTF, no descargadas en tiempo de ejecución:
 * `ui-text-google-fonts` dependería de Google Play Services y fallaría sin
 * red, además de contradecir esa misma regla de autoalojamiento. Se convierten
 * desde los WOFF2 que ya vivían en el repositorio, así que Android y Web usan
 * literalmente los mismos ficheros de origen — ninguna es un sustituto
 * "parecido".
 *
 * Solo se empaquetan los pesos que algún tema usa de verdad (31 ficheros,
 * ~1,4 MB): incluir familias completas habría triplicado el peso sin que
 * ninguna pantalla llegara a pedirlas.
 */
object VidaFonts {

    /** Cuerpo de seis de los nueve temas. */
    val Inter = FontFamily(
        Font(R.font.inter_400, FontWeight.Normal),
        Font(R.font.inter_500, FontWeight.Medium),
        Font(R.font.inter_600, FontWeight.SemiBold),
        Font(R.font.inter_700, FontWeight.Bold),
    )

    /** Display de Papel, Premium Minimal, Modern Productivity y Organic. */
    val Fraunces = FontFamily(
        Font(R.font.fraunces_300, FontWeight.Light),
        Font(R.font.fraunces_400, FontWeight.Normal),
        Font(R.font.fraunces_500, FontWeight.Medium),
        Font(R.font.fraunces_600, FontWeight.SemiBold),
    )

    /**
     * La caligráfica de la pantalla de entrada: la palabra «Meraki» del
     * template de login, y nada más.
     *
     * Convertida del MISMO woff2 que sirve el tema de Keycloak
     * (`vida-cotidiana-web/login/resources/fonts`), no de una fuente parecida:
     * es la única letra caligráfica del sistema y sustituirla por Caveat —la
     * manuscrita de Papel, que es otra cosa— habría cambiado el carácter de la
     * palabra que sostiene toda esa columna.
     */
    val AlexBrush = FontFamily(Font(R.font.alex_brush_400, FontWeight.Normal))

    /**
     * La letra a mano de Papel. Se usa donde el artefacto escribe a mano —las
     * notas del día— y en ningún otro sitio: no es un adorno, es la diferencia
     * entre un dato tecleado y una anotación.
     */
    val Caveat = FontFamily(
        Font(R.font.caveat_500, FontWeight.Medium),
        Font(R.font.caveat_600, FontWeight.SemiBold),
    )

    /**
     * Aurora — y, desde el artefacto maestro, también Claro y Noche.
     *
     * El 700 se añadió para el artefacto: sus cifras grandes van en Sora Bold y
     * los títulos en SemiBold, y esa diferencia de peso es lo que hace que un
     * «27» pese como dato y no como encabezado. Sin este fichero, Compose caía
     * al 600 más cercano y las dos jerarquías se veían iguales.
     */
    val Sora = FontFamily(
        Font(R.font.sora_200, FontWeight.ExtraLight),
        Font(R.font.sora_300, FontWeight.Light),
        Font(R.font.sora_400, FontWeight.Normal),
        Font(R.font.sora_600, FontWeight.SemiBold),
        Font(R.font.sora_700, FontWeight.Bold),
    )

    /** Lumen — cuerpo y display a la vez. */
    val Manrope = FontFamily(
        Font(R.font.manrope_200, FontWeight.ExtraLight),
        Font(R.font.manrope_300, FontWeight.Light),
        Font(R.font.manrope_400, FontWeight.Normal),
        Font(R.font.manrope_500, FontWeight.Medium),
        Font(R.font.manrope_600, FontWeight.SemiBold),
    )

    /** Neo — el cartel de alto contraste llega hasta el 900. */
    val Archivo = FontFamily(
        Font(R.font.archivo_400, FontWeight.Normal),
        Font(R.font.archivo_600, FontWeight.SemiBold),
        Font(R.font.archivo_800, FontWeight.ExtraBold),
        Font(R.font.archivo_900, FontWeight.Black),
    )

    /** Calm — cuerpo y display. */
    val PlusJakarta = FontFamily(
        Font(R.font.plus_jakarta_sans_400, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans_500, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans_600, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans_700, FontWeight.Bold),
    )

    /** Studio — el titular de revista, con su cursiva real. */
    val InstrumentSerif = FontFamily(
        Font(R.font.instrument_serif_400, FontWeight.Normal),
        Font(R.font.instrument_serif_400_italic, FontWeight.Normal, FontStyle.Italic),
    )

    /** Etiquetas de los tres temas conservados de UX-014. */
    val CourierPrime = FontFamily(
        Font(R.font.courier_prime_400, FontWeight.Normal),
        Font(R.font.courier_prime_700, FontWeight.Bold),
    )
}
