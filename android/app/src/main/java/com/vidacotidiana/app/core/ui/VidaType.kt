package com.vidacotidiana.app.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

/**
 * LA ESCALA TIPOGRÁFICA DEL PRODUCTO, POR SIGNIFICADO Y NO POR TAMAÑO.
 *
 * Antes existían dos escalas y ninguna nombraba los peldaños de abajo. Los roles
 * de Material 3 dicen `bodySmall` o `labelMedium`, que describen una MEDIDA; una
 * pantalla que necesita "el dato pequeño que acompaña a un título" no encuentra
 * ese rol y acaba escribiendo `fontSize = 9.5.sp` a mano. Eso pasó cinco veces
 * con 9,5 sp, dos con 12,5 sp y dos con 11 sp: veinticuatro tamaños fijados a
 * mano fuera del tema. No era descuido; era que el rol no existía.
 *
 * Aquí cada estilo se llama por lo que ES en la pantalla, y son exactamente los
 * seis niveles que el producto necesita distinguir:
 *
 *  1. PRIMARIA    — `cardTitle`, `body`        (qué es esto)
 *  2. SECUNDARIA  — `metadata`                 (qué más sé de esto)
 *  3. METADATO    — `caption`                  (cuándo, cuánto, de quién)
 *  4. ACCIÓN      — `action`                   (qué puedo hacer)
 *  5. ESTADO      — `state`                    (en qué situación está)
 *  6. TERCIARIA   — `micro`                    (lo que se consulta, no se lee)
 *
 * DOS REGLAS QUE ANTES SE INCUMPLÍAN:
 *
 * a) El título de una tarjeta y el cuerpo NO pueden medir lo mismo. Medían los
 *    dos 15 sp y solo los separaba el peso, así que en una lista todo pesaba
 *    igual. `cardTitle` sube a 16 sp: la diferencia vuelve a ser de tamaño Y de
 *    peso, que es lo que hace que una lista se lea de un vistazo.
 *
 * b) Las cifras destacadas usan la TIPOGRAFÍA DE DISPLAY DEL TEMA, con su peso.
 *    `ResourceBoard` las escribía con `fontWeight = SemiBold` fijo, anulando el
 *    `displayWeight` del spec — justo el rasgo que separa a Lumen (ExtraLight,
 *    "aire y tipografía") de Neo (Black, "cartel de alto contraste"). Un
 *    componente no puede contradecir al sistema: `heroFigure` y `metricFigure`
 *    heredan familia, peso, cursiva y caja del tema activo.
 *
 * Todo en `sp`, nunca en `dp`: respeta el ajuste de tamaño de letra del sistema.
 */
@Immutable
data class VidaTypeScale(
    /** El nombre de la sección en la barra superior. */
    val screenTitle: TextStyle,
    /** Un título dentro del contenido: héroe de Inicio, cabecera de bloque. */
    val sectionTitle: TextStyle,
    /** NIVEL 1 — el nombre de la cosa. Lo primero que se lee de una pieza. */
    val cardTitle: TextStyle,
    /** NIVEL 1 — texto corrido. */
    val body: TextStyle,
    /** NIVEL 2 — la línea que acompaña al título y lo cualifica. */
    val metadata: TextStyle,
    /** NIVEL 3 — fecha, tamaño, categoría: se consulta de reojo. */
    val caption: TextStyle,
    /** NIVEL 6 — rótulo mínimo: leyendas del calendario, etiquetas de tipo. */
    val micro: TextStyle,
    /** NIVEL 4 — la palabra dentro de un botón. */
    val action: TextStyle,
    /** NIVEL 5 — la palabra dentro de una píldora de estado. */
    val state: TextStyle,
    /** Antetítulo de sección. Su CAJA y su color los pone `EyebrowStyle`. */
    val eyebrow: TextStyle,
    /** La cifra mayor de una pieza: un importe. Con el display del tema. */
    val heroFigure: TextStyle,
    /** El dato derivado de una pieza: días restantes, hora. Con el display. */
    val metricFigure: TextStyle,
)

/**
 * Construye la escala para el tema activo.
 *
 * Las medidas no cambian entre agendas —una jerarquía que se mueve deja de ser
 * jerarquía—; lo que cambia es la FAMILIA y el PESO, que es donde vive la
 * identidad de cada tema según ADR-023.
 */
fun typeScaleFor(spec: VidaThemeSpec): VidaTypeScale {
    val f = spec.fonts
    // El titular del tema, con todos sus rasgos. De aquí salen también las
    // cifras: son titulares numéricos, no texto en negrita.
    val display = TextStyle(
        fontFamily = f.display,
        fontWeight = f.displayWeight,
        fontStyle = if (f.displayItalic) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = (-0.02).em,
    )
    return VidaTypeScale(
        screenTitle = display.copy(fontSize = 22.sp, lineHeight = 27.sp),
        sectionTitle = display.copy(fontSize = 19.sp, lineHeight = 24.sp),

        // 16 sp, no 15: por encima del cuerpo, para que un listado tenga cima.
        cardTitle = TextStyle(
            fontFamily = f.body, fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.01).em,
        ),
        body = TextStyle(
            fontFamily = f.body, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = spec.bodyLineHeight,
        ),
        metadata = TextStyle(
            fontFamily = f.body, fontWeight = FontWeight.Normal,
            fontSize = 13.sp, lineHeight = 18.sp,
        ),
        caption = TextStyle(
            fontFamily = f.body, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp,
        ),
        micro = TextStyle(
            fontFamily = f.label, fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp, lineHeight = 12.sp, letterSpacing = 0.08.em,
        ),
        action = TextStyle(
            fontFamily = f.body, fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp, lineHeight = 17.sp,
        ),
        state = TextStyle(
            fontFamily = f.body, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, lineHeight = 14.sp,
        ),
        eyebrow = TextStyle(
            fontFamily = f.label, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.14.em,
        ),

        // Las dos cifras: mismo carácter, dos pesos de presencia. `hero` es el
        // importe (Pagos), `metric` el dato derivado (días, hora).
        heroFigure = display.copy(fontSize = 26.sp, lineHeight = 30.sp),
        metricFigure = display.copy(fontSize = 21.sp, lineHeight = 25.sp),
    )
}

/**
 * UNA CIFRA NUNCA SE TRUNCA: BAJA DE ESCALÓN.
 *
 * Dentro de una pieza de retícula el ancho no lo decide el contenido, lo decide
 * la columna. Con la cifra a cuerpo fijo, «$3,000 MXN» se leía «$3,000 M…»
 * mientras «$80 MXN» nadaba en su tarjeta. Y truncar un número es peor que
 * truncar un texto: «$3,000 M…» no es una cifra incompleta, es una cifra que ya
 * no se puede leer.
 *
 * La regla, entonces, es que la cifra cede tamaño antes que carácteres. Dos
 * escalones bastan para todo lo que el producto muestra —importes, días, horas—
 * y la proporción se mantiene, así que dos piezas contiguas siguen pareciendo
 * la misma pieza.
 *
 * Solo se aplica donde el ancho está impuesto. En la tira de métricas, que
 * crece con su contenido, la cifra se escribe entera y a cuerpo completo.
 */
fun VidaTypeScale.tileFigure(text: String, hero: Boolean): TextStyle {
    val base = if (hero) heroFigure else metricFigure
    val factor = when {
        text.length <= 8 -> 1f
        text.length <= 12 -> 0.82f
        else -> 0.68f
    }
    return if (factor == 1f) base else base.copy(
        fontSize = base.fontSize * factor,
        lineHeight = base.lineHeight * factor,
    )
}

val LocalVidaType = staticCompositionLocalOf { typeScaleFor(VidaThemes.of(VisualTheme.DEFAULT)) }

private val Double.em: TextUnit get() = TextUnit(this.toFloat(), TextUnitType.Em)
