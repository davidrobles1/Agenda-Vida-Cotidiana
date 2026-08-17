package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class DonutSlice(val value: Float, val color: Color)

/**
 * UX-006: the reference's donut-chart pattern ("Gastos por categoría"), only
 * ever fed with real data in this app (completed vs. pending reminders — see
 * design-system.md's note on why the Finanzas use of this exact widget
 * wasn't ported over). Deliberately simple: an arc-stroke ring, no
 * animation/legend baked in (the caller composes its own legend from the
 * same `slices` list so labels/values never drift from what's drawn).
 */
@Composable
fun DonutChart(slices: List<DonutSlice>, modifier: Modifier = Modifier, strokeWidthDp: Float = 14f) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Canvas(modifier = modifier.size(120.dp)) {
        if (total <= 0f) return@Canvas
        var startAngle = -90f
        val stroke = Stroke(width = strokeWidthDp.dp.toPx())
        val diameter = size.minDimension - stroke.width
        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
        slices.forEach { slice ->
            val sweep = (slice.value / total) * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += sweep
        }
    }
}
