package com.vidacotidiana.app.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UX-007: subtle ruled-notebook texture — reinforces Home/Calendario's
 * "agenda personal" identity without a photo or AI-generated image (same
 * cost/license limit already applied to the login theme). Thin horizontal
 * lines at low alpha, drawn behind content so they never compete with it —
 * same visual weight as Web's `.notebook-bg` (`index.css`).
 */
fun Modifier.notebookBackground(lineColor: Color, spacing: Dp = 28.dp): Modifier = drawBehind {
    val spacingPx = spacing.toPx()
    var y = spacingPx
    while (y < size.height) {
        drawLine(color = lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += spacingPx
    }
}
