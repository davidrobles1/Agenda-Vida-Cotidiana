package com.vidacotidiana.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.vidacotidiana.app.core.ui.VidaColors
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * UX-006: the reference dashboard uses a small, consistent set of category
 * colors (purple/blue/green/orange, plus red for errors/overdue) across
 * metric-card icon badges, list-item icons and status pills. One enum keeps
 * that mapping in a single place instead of every screen picking colors by
 * hand — resolves through `VidaTheme.colors`, so it's dark-mode-correct by
 * construction (see `Color.kt`'s comment on why that matters here).
 */
enum class BadgeTone {
    Primary,
    Success,
    Warning,
    Info,
    Error,
}

data class ToneColors(val container: Color, val on: Color)

@Composable
fun BadgeTone.resolve(): ToneColors {
    val colors: VidaColors = VidaTheme.colors
    return when (this) {
        BadgeTone.Primary -> ToneColors(colors.primaryContainer, colors.primary)
        BadgeTone.Success -> ToneColors(colors.successContainer, colors.successText)
        BadgeTone.Warning -> ToneColors(colors.warningContainer, colors.warningText)
        BadgeTone.Info -> ToneColors(colors.infoContainer, colors.infoText)
        BadgeTone.Error -> ToneColors(colors.errorContainer, colors.error)
    }
}
