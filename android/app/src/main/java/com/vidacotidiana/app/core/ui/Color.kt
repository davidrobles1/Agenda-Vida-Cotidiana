package com.vidacotidiana.app.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// UX-006/design-system.md §1. Contrast ratios computed for real against WCAG
// 2.1 (relative luminance), not eyeballed — see accessibility.md §1. Success,
// warning and info at their base tone fail 4.5:1 for text on white — they're
// icon/border/container colors only; the *Text variants are the ones safe
// for text.
object VidaColor {
    val PrimaryLight = Color(0xFF4F46E5)
    val PrimaryContainerLight = Color(0xFFE0E7FF)
    val OnPrimaryLight = Color(0xFFFFFFFF)

    val PrimaryDark = Color(0xFF818CF8)
    val PrimaryContainerDark = Color(0xFF312E81)
    val OnPrimaryDark = Color(0xFF1E1B4B)

    val SuccessLight = Color(0xFF059669)
    val SuccessTextLight = Color(0xFF047857)
    val SuccessContainerLight = Color(0xFFD1FAE5)
    val SuccessDark = Color(0xFF34D399)
    val SuccessTextDark = Color(0xFF34D399)
    val SuccessContainerDark = Color(0xFF064E3B)

    val WarningLight = Color(0xFFD97706)
    val WarningTextLight = Color(0xFFB45309)
    val WarningContainerLight = Color(0xFFFEF3C7)
    val WarningDark = Color(0xFFFBBF24)
    val WarningTextDark = Color(0xFFFBBF24)
    val WarningContainerDark = Color(0xFF78350F)

    // UX-006: new role, added extracting real pixel values from the Android/Web
    // reference dashboards (Documentos badge, "Programada" status pill) — see
    // design-system.md §1 for the sampled hex values and computed contrast.
    val InfoLight = Color(0xFF2563EB)
    val InfoTextLight = Color(0xFF1D4ED8)
    val InfoContainerLight = Color(0xFFDBEAFE)
    val InfoDark = Color(0xFF60A5FA)
    val InfoTextDark = Color(0xFF60A5FA)
    val InfoContainerDark = Color(0xFF1E3A8A)

    val ErrorLight = Color(0xFFDC2626)
    val ErrorContainerLight = Color(0xFFFEE2E2)
    val ErrorDark = Color(0xFFF87171)
    val ErrorContainerDark = Color(0xFF7F1D1D)

    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF9FAFB)
    val BorderLight = Color(0xFFE5E7EB)
    val TextLight = Color(0xFF111827)
    val TextSecondaryLight = Color(0xFF6B7280)

    // UX-006: darkened from the previous #16171D/#1F2028 — the Android
    // reference dashboard sampled at real pixel level (docs/Ejemplo APK vida
    // Cotidiana.png) is closer to near-black (~#0F1516 background, ~#141A1F
    // cards) than the original guess. Text tokens below were re-verified
    // against these new, darker values, not just carried over assuming they'd
    // still pass.
    val SurfaceDark = Color(0xFF101418)
    val SurfaceVariantDark = Color(0xFF171D22)
    val BorderDark = Color(0xFF2E343B)
    val TextDark = Color(0xFFF3F4F6)
    val TextSecondaryDark = Color(0xFF9CA3AF)
}

/**
 * UX-006 real bug fix: every screen before this referenced `VidaColor.*Light`
 * directly (e.g. `VidaColor.TextSecondaryLight`, `CardDefaults.cardColors(
 * containerColor = VidaColor.SurfaceVariantLight)`), even though
 * `VidaCotidianaTheme` already built a real dark `ColorScheme` — so a phone
 * in system dark mode got MaterialTheme's dark background behind
 * hardcoded-light cards and text, an actually-broken combination, not just a
 * missed nicety. `VidaColors` + `LocalVidaColors` is the fix: one
 * CompositionLocal holding every semantic token (including the ones with no
 * MaterialTheme.colorScheme equivalent — success/warning/info aren't
 * standard M3 roles), resolved once in `VidaCotidianaTheme` and read via
 * `VidaTheme.colors.xxx` everywhere a screen used to reach for
 * `VidaColor.xxxLight` by name.
 */
data class VidaColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val success: Color,
    val successText: Color,
    val successContainer: Color,
    val warning: Color,
    val warningText: Color,
    val warningContainer: Color,
    val info: Color,
    val infoText: Color,
    val infoContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
)

val LightVidaColors = VidaColors(
    primary = VidaColor.PrimaryLight,
    onPrimary = VidaColor.OnPrimaryLight,
    primaryContainer = VidaColor.PrimaryContainerLight,
    success = VidaColor.SuccessLight,
    successText = VidaColor.SuccessTextLight,
    successContainer = VidaColor.SuccessContainerLight,
    warning = VidaColor.WarningLight,
    warningText = VidaColor.WarningTextLight,
    warningContainer = VidaColor.WarningContainerLight,
    info = VidaColor.InfoLight,
    infoText = VidaColor.InfoTextLight,
    infoContainer = VidaColor.InfoContainerLight,
    error = VidaColor.ErrorLight,
    errorContainer = VidaColor.ErrorContainerLight,
    surface = VidaColor.SurfaceLight,
    surfaceVariant = VidaColor.SurfaceVariantLight,
    border = VidaColor.BorderLight,
    text = VidaColor.TextLight,
    textSecondary = VidaColor.TextSecondaryLight,
)

val DarkVidaColors = VidaColors(
    primary = VidaColor.PrimaryDark,
    onPrimary = VidaColor.OnPrimaryDark,
    primaryContainer = VidaColor.PrimaryContainerDark,
    success = VidaColor.SuccessDark,
    successText = VidaColor.SuccessTextDark,
    successContainer = VidaColor.SuccessContainerDark,
    warning = VidaColor.WarningDark,
    warningText = VidaColor.WarningTextDark,
    warningContainer = VidaColor.WarningContainerDark,
    info = VidaColor.InfoDark,
    infoText = VidaColor.InfoTextDark,
    infoContainer = VidaColor.InfoContainerDark,
    error = VidaColor.ErrorDark,
    errorContainer = VidaColor.ErrorContainerDark,
    surface = VidaColor.SurfaceDark,
    surfaceVariant = VidaColor.SurfaceVariantDark,
    border = VidaColor.BorderDark,
    text = VidaColor.TextDark,
    textSecondary = VidaColor.TextSecondaryDark,
)

val LocalVidaColors = staticCompositionLocalOf { LightVidaColors }
