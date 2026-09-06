package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.EyebrowStyle
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * Antetítulo de sección ("TU DÍA", "RESUMEN", "notas").
 *
 * ADR-023(h): un rótulo es un WIDGET, y su tratamiento cambia por completo
 * entre agendas. Papel lo escribe en versalitas de la serif y en terracota;
 * Studio en cursiva de revista y caja baja; Lumen muy espaciado y apagado; Neo
 * pesado y en caja alta. Aquí vive esa decisión una sola vez, para las tres
 * apariciones del mismo rol que tiene el artefacto.
 */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, rule: Boolean = true) {
    val spec = VidaTheme.spec
    val c = spec.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = VidaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
    ) {
        when (spec.eyebrow) {
            EyebrowStyle.SMALL_CAPS_SERIF -> Text(
                // Compose no expone `font-variant-caps`, así que las versalitas
                // se consiguen con la propia serif en caja baja y tamaño algo
                // mayor — el mismo efecto que en la Web, no un sustituto.
                text = text.lowercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.05.sp,
                ),
                color = c.second,
            )
            EyebrowStyle.ITALIC_SERIF -> Text(
                text = text.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 15.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, letterSpacing = 0.sp,
                ),
                color = c.textSecondary,
            )
            EyebrowStyle.HEAVY_UPPER -> Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = c.text,
            )
            EyebrowStyle.UPPER_TRACKED -> Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (spec.theme.id == "lumen") c.textTertiary else c.primary,
            )
        }
        if (rule) {
            Box(
                Modifier.weight(1f).height(1.dp).background(c.line),
            )
        }
    }
}

/** Tono semántico de una píldora. Separado del acento: es información, no marca. */
enum class PillTone { NEUTRAL, QUIET, OK, WARN, DANGER }

/** Píldora de estado (`.pill`). Neo la dibuja cuadrada y con borde. */
@Composable
fun VidaPill(text: String, tone: PillTone = PillTone.NEUTRAL, modifier: Modifier = Modifier) {
    val spec = VidaTheme.spec
    val c = spec.colors
    val (bg, fg) = when (tone) {
        PillTone.NEUTRAL -> c.primaryContainer to c.primary
        PillTone.QUIET -> c.sunken to c.textSecondary
        PillTone.OK -> c.successContainer to c.successText
        PillTone.WARN -> c.warningContainer to c.warningText
        PillTone.DANGER -> c.errorContainer to c.error
    }
    val shape = RoundedCornerShape(spec.radii.pill)
    Box(
        modifier = modifier
            .background(bg, shape)
            .then(if (spec.borderWidth > 1.dp) Modifier.border(1.5.dp, fg, shape) else Modifier)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = fg)
    }
}

/** Etiqueta de tipo de recurso (`.typeTag`): contorno, tipografía de etiqueta. */
@Composable
fun VidaTypeTag(text: String, modifier: Modifier = Modifier) {
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.pill)
    Box(
        modifier = modifier
            .border(1.dp, spec.colors.border, shape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = spec.colors.textSecondary,
        )
    }
}

/** Título de tarjeta o de pantalla, con la tipografía de display del tema. */
@Composable
fun VidaTitle(text: String, modifier: Modifier = Modifier, color: Color = VidaTheme.colors.text) {
    val spec = VidaTheme.spec
    Text(
        text = if (spec.fonts.displayUppercase) text.uppercase() else text,
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        modifier = modifier,
    )
}

/**
 * "1 compromiso" / "2 compromisos".
 *
 * Con listas siempre vacías el plural fijo colaba; en cuanto se puede crear,
 * el primer registro delata el fallo en seis pantallas a la vez. Vive aquí
 * porque el recuento del antetítulo es el mismo rol en todas ellas.
 */
fun plural(count: Int, singular: String, plural: String): String =
    "$count ${if (count == 1) singular else plural}"
