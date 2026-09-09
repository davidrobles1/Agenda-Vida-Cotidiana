package com.vidacotidiana.app.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** UX-001/design-system.md §3 — 8pt grid, single source for spacing across every screen. */
object VidaSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/**
 * EL ESPACIADO POR SU FUNCIÓN, no por su medida.
 *
 * `VidaSpacing` es la escala; esto es el SISTEMA. La diferencia importa: con
 * solo la escala, cada pantalla elige entre `sm` y `md` según le parece, y todo
 * lo que no encaja se resuelve a mano. Así estaba: 83 medidas fijadas a mano
 * frente a 98 usos de la escala, con `xxl` sin usar jamás y `xl` cinco veces.
 * El sistema real era "sm o md" y el resto improvisación — 2, 3, 5, 6, 10, 11,
 * 14 y 18 dp repartidos por los componentes.
 *
 * Nombrar el hueco por lo que separa es lo que hace que dos pantallas escritas
 * en semanas distintas respiren igual. Los valores siguen la rejilla de 8 (con
 * el medio paso de 4) y no inventan medidas nuevas.
 */
object VidaLayout {
    /** Margen lateral de toda pantalla. Constante en las diecisiete secciones. */
    val gutter: Dp = 16.dp

    /** Entre dos secciones con antetítulo propio. El respiro mayor de la página. */
    val sectionGap: Dp = 20.dp

    /** Entre bloques dentro de una misma sección. */
    val blockGap: Dp = 12.dp

    /** Entre piezas hermanas de una lista o una rejilla. */
    val itemGap: Dp = 8.dp

    /** Padding interno de una tarjeta de contenido. */
    val cardPadding: Dp = 16.dp

    /** Padding interno de una pieza de rejilla: más ajustado, cabe menos. */
    val tilePadding: Dp = 14.dp

    /** Padding vertical de una fila de lista. */
    val rowPadding: Dp = 12.dp

    /** Entre un título y el dato que lo cualifica: casi se tocan, por eso se leen juntos. */
    val textGap: Dp = 2.dp

    /**
     * Mínimo táctil de Android. No es una preferencia estética: la acción de
     * completar medía 32 dp, y es el gesto que más se repite en el producto.
     * Lo que se dibuja puede ser menor; lo que se PULSA, nunca.
     */
    val touchTarget: Dp = 48.dp
}

/**
 * Los tamaños de icono del sistema.
 *
 * Había ocho en uso —14, 16, 17, 18, 19, 20, 21 y 24 dp— sin ninguna regla que
 * dijera cuál toca. Un icono a 17 dp junto a otro a 19 no se percibe como dos
 * medidas: se percibe como descuido. Cuatro pasos cubren todos los casos reales
 * del artefacto.
 */
object VidaIconSize {
    /** Dentro de una píldora o una leyenda. */
    val micro: Dp = 14.dp

    /** Dentro de un cuadro de marca (`VidaMark`) o de un botón pequeño. */
    val small: Dp = 16.dp

    /** El tamaño por defecto: barra superior, filas, navegación. */
    val medium: Dp = 20.dp

    /** Acción destacada: el «+» central. */
    val large: Dp = 24.dp
}

/**
 * design-system.md §4 — escala de radios heredada.
 *
 * ADR-023: los radios REALES los pone cada tema (`VidaTheme.spec.radii`), que
 * van de 0 dp en Neo a 26 dp en Calm. Estos dos valores se conservan porque
 * los usan pantallas anteriores a esta fase y quitarlos las rompería sin dar
 * nada a cambio; el código nuevo lee el spec.
 */
object VidaShape {
    val card: Dp = 12.dp
    val control: Dp = 8.dp
}
