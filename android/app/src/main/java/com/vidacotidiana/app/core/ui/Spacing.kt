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
