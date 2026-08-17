package com.vidacotidiana.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = VidaColor.PrimaryLight,
    onPrimary = VidaColor.OnPrimaryLight,
    primaryContainer = VidaColor.PrimaryContainerLight,
    error = VidaColor.ErrorLight,
    errorContainer = VidaColor.ErrorContainerLight,
    surface = VidaColor.SurfaceLight,
    surfaceVariant = VidaColor.SurfaceVariantLight,
    outline = VidaColor.BorderLight,
    onSurface = VidaColor.TextLight,
    onSurfaceVariant = VidaColor.TextSecondaryLight,
    background = VidaColor.SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = VidaColor.PrimaryDark,
    onPrimary = VidaColor.OnPrimaryDark,
    primaryContainer = VidaColor.PrimaryContainerDark,
    error = VidaColor.ErrorDark,
    errorContainer = VidaColor.ErrorContainerDark,
    surface = VidaColor.SurfaceDark,
    surfaceVariant = VidaColor.SurfaceVariantDark,
    outline = VidaColor.BorderDark,
    onSurface = VidaColor.TextDark,
    onSurfaceVariant = VidaColor.TextSecondaryDark,
    background = VidaColor.SurfaceDark,
)

/**
 * UX-001/UX-006: the app's real color scheme + type scale (design-system.md),
 * replacing the bare MaterialTheme default. Also provides `LocalVidaColors`
 * (`Color.kt`) so screens can reach semantic tokens with no MaterialTheme
 * equivalent (success/warning/info) through `VidaTheme.colors.*` instead of
 * hardcoding a `*Light` constant that would silently break dark mode.
 */
@Composable
fun VidaCotidianaTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val materialColors = if (isDark) DarkColors else LightColors
    val vidaColors = if (isDark) DarkVidaColors else LightVidaColors
    CompositionLocalProvider(LocalVidaColors provides vidaColors) {
        MaterialTheme(colorScheme = materialColors, typography = VidaTypography, content = content)
    }
}

/** `VidaTheme.colors.textSecondary` etc. — the theme-aware equivalent of the old `VidaColor.TextSecondaryLight`. */
object VidaTheme {
    val colors: VidaColors
        @Composable get() = LocalVidaColors.current
}
