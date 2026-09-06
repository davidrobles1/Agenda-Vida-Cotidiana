package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.material3.ripple
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * Controles del artefacto: segmentos, chips, campo de búsqueda y botones.
 *
 * Todos comparten dos rasgos que el prototipo tiene y que aquí no se pierden:
 * el radio lo pone el tema (de 0 dp en Neo a cápsula en Organic) y **todo lo
 * pulsable se encoge un poco al tocarlo** — el `:active { transform: scale }`
 * de la Web, aquí con muelle real.
 */

/** Encogido al pulsar, con física de muelle. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, to: Float = 0.96f): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) to else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )
    return this.scale(scale)
}

/** Control segmentado (`.seg`) — Mes/Semana/Día, Me compartieron/Yo compartí. */
@Composable
fun VidaSegmented(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = VidaTheme.spec
    val c = spec.colors
    val outerShape = RoundedCornerShape(spec.radii.pill)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (spec.borderWidth > 1.dp) Modifier.border(spec.borderWidth, c.border, outerShape)
                else Modifier.background(c.sunken, outerShape),
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            val interaction = remember { MutableInteractionSource() }
            val bg by animateColorAsState(if (on) c.surfaceVariant else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (on) c.primary else c.textSecondary, label = "segFg")
            val innerShape = RoundedCornerShape(spec.radii.pill)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .background(bg, innerShape)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(color = c.primary),
                        role = Role.Tab,
                    ) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(option, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

/** Fila de chips de filtro deslizable (`.chips`). */
@Composable
fun VidaChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
    ) {
        options.forEach { option ->
            val on = option == selected
            val interaction = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(spec.radii.pill)
            val bg by animateColorAsState(if (on) c.primary else Color.Transparent, label = "chipBg")
            val fg by animateColorAsState(if (on) c.onPrimary else c.textSecondary, label = "chipFg")
            Box(
                modifier = Modifier
                    .pressScale(interaction, 0.94f)
                    .background(bg, shape)
                    .border(spec.borderWidth, if (on) c.primary else c.border, shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(color = c.primary),
                        role = Role.RadioButton,
                    ) { onSelect(option) }
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(option, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

/** Campo de búsqueda (`.search`). Presentacional: el filtrado lo hace la pantalla. */
@Composable
fun VidaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.control)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceElevated, shape)
            .border(spec.borderWidth, c.border, shape)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.textTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge).copy(color = c.text),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Campo de texto de formulario.
 *
 * Es un rol distinto del buscador, aunque compartan caja: `VidaSearchField`
 * lleva su lupa porque anuncia "esto filtra lo que ves", y esa misma lupa
 * delante de "Nombre" en un alta dice algo falso. Comparten forma, borde y
 * altura mínima; se separan en lo que significan.
 */
@Composable
fun VidaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    multiline: Boolean = false,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.control)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceElevated, shape)
            .border(spec.borderWidth, c.border, shape)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.textTertiary)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = !multiline,
            maxLines = if (multiline) 4 else 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                imeAction = if (multiline) ImeAction.Default else ImeAction.Done,
            ),
            textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge).copy(color = c.text),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Botón primario del artefacto. En Neo, con su borde y su sombra dura. */
@Composable
fun VidaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    ghost: Boolean = false,
    enabled: Boolean = true,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.control)
    val bg = if (ghost) Color.Transparent else c.primary
    val fg = if (ghost) c.textSecondary else c.onPrimary
    val borderColor = if (ghost) c.border else c.primary

    Row(
        modifier = modifier
            .pressScale(interaction)
            .then(
                if (!ghost && spec.elevation.isHard) Modifier.hardShadow(spec.elevation.hardOffset, spec.radii.control, c.text)
                else Modifier,
            )
            .background(bg, shape)
            .border(spec.borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = fg),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

/** Botón compacto para acciones dentro de una fila o una hoja. */
@Composable
fun VidaSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
    enabled: Boolean = true,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.control)
    Box(
        modifier = modifier
            .pressScale(interaction)
            .background(if (ghost) Color.Transparent else c.primary, shape)
            .border(spec.borderWidth, if (ghost) c.border else c.primary, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(color = c.primary),
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
            // Deshabilitado se ve, no solo se comporta: un boton que ignora el
            // toque sin decirlo se lee como una aplicacion rota.
            color = (if (ghost) c.textSecondary else c.onPrimary).copy(alpha = if (enabled) 1f else 0.45f),
        )
    }
}

/** Barra de progreso del dia (`.progress`). */
@Composable
fun VidaProgress(fraction: Float, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "progress",
    )
    Box(
        modifier = modifier.fillMaxWidth().height(8.dp).background(c.sunken, RoundedCornerShape(999.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .background(c.primary, RoundedCornerShape(999.dp)),
        )
    }
}

/** Casilla de un formulario, con blanco táctil completo en toda la fila. */
@Composable
fun VidaCheckRow(
    label: String,
    checked: Boolean,
    indent: Boolean = false,
    onToggle: () -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = c.primary),
                role = Role.Checkbox,
                onClick = onToggle,
            )
            .defaultMinSize(minHeight = 48.dp)
            .padding(start = if (indent) 30.dp else 0.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .background(if (checked) c.primary else Color.Transparent, RoundedCornerShape(spec.radii.control / 2))
                .border(spec.borderWidth, if (checked) c.primary else c.border, RoundedCornerShape(spec.radii.control / 2)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null,
                    tint = c.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            label,
            style = if (indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            color = if (indent) c.textSecondary else c.text,
        )
    }
}
