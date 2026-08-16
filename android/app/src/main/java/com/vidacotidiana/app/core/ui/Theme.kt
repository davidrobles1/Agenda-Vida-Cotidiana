package com.vidacotidiana.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

/** UX-001: the app's real color scheme + type scale (design-system.md), replacing the bare MaterialTheme default. */
@Composable
fun VidaCotidianaTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = VidaTypography, content = content)
}
