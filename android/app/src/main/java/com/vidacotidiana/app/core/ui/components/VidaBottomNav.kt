package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.navigation.Destination

/**
 * Barra inferior por contexto, con el «+» al centro.
 *
 * Toma de la versión Light el CONCEPTO —cinco posiciones y una acción de
 * creación diferenciada— pero se construye con los tokens de Complete: la
 * píldora de acento tras el icono activo, el radio y la sombra del tema, y el
 * borde de 2 dp de Neo. No es un componente importado de otra app.
 *
 * Sustituye al `VidaBottomNav` de UX-006, que tenía cuatro destinos fijos
 * (Inicio/Tareas/Compartidos/Más): los destinos ahora vienen del contexto
 * activo y el «Más» se convierte en el menú lateral.
 */
@Composable
fun VidaBottomNav(
    destinations: List<Destination>,
    currentRoute: String?,
    onSelect: (Destination) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceVariant)
            .drawBehind {
                drawRect(color = c.line, size = Size(size.width, spec.borderWidth.toPx()))
            }
            .navigationBarsPadding()
            .padding(top = 5.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (destinations.size <= 1) Arrangement.Center else Arrangement.SpaceEvenly,
    ) {
        when {
            // Portal tiene una sola sección real: la barra se centra en lo que
            // existe en vez de estirarse a cinco huecos vacíos.
            destinations.size <= 1 -> {
                destinations.forEach { NavTab(it, it.route == currentRoute, { onSelect(it) }, Modifier.width(140.dp)) }
                CreateFab(onCreate)
            }
            else -> {
                val left = destinations.take(2)
                val right = destinations.drop(2).take(2)
                left.forEach { NavTab(it, it.route == currentRoute, { onSelect(it) }, Modifier.weight(1f)) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CreateFab(onCreate) }
                right.forEach { NavTab(it, it.route == currentRoute, { onSelect(it) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NavTab(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(if (selected) c.primary else c.textTertiary, label = "tabTint")
    val pillBg by animateColorAsState(
        if (selected) c.primaryContainer else Color.Transparent,
        label = "tabPill",
    )
    val pillShape = RoundedCornerShape(spec.radii.pill)

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = c.primary, bounded = false),
                role = Role.Tab,
                onClick = onClick,
            )
            // 56 dp de alto: por encima del mínimo táctil de 48 dp incluso con
            // el texto en dos líneas si el usuario amplía la letra del sistema.
            .height(58.dp)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.width(46.dp).height(28.dp).background(pillBg, pillShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(destination.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(
            destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** El «+»: acción primaria de creación, claramente distinta de un destino. */
@Composable
private fun CreateFab(onCreate: () -> Unit) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(if (spec.radii.control >= 999.dp) 999.dp else 18.dp)

    Box(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .pressScale(interaction, 0.9f)
            .then(
                if (spec.elevation.isHard) Modifier.hardShadow(spec.elevation.hardOffset, 18.dp, c.text)
                else Modifier.shadow(8.dp, shape, clip = false),
            )
            .size(54.dp)
            .background(c.primary, shape)
            .then(if (spec.borderWidth > 1.dp) Modifier.border(spec.borderWidth, c.text, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = c.onPrimary, bounded = false),
                role = Role.Button,
                onClick = onCreate,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Crear", tint = c.onPrimary, modifier = Modifier.size(24.dp))
    }
}
