package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * Las superficies del artefacto: tarjeta y fila.
 *
 * Cada tema las dibuja distinto y esa diferencia es su identidad, no un matiz:
 * Neo lleva 2 dp de borde negro y una SOMBRA DURA DESPLAZADA (no difuminada),
 * Lumen y Studio pierden la caja y se convierten en bloques abiertos con
 * filete, Calm redondea a 26 dp y flota. Por eso la sombra no se delega en
 * `Card(elevation)`: la dura de Neo no existe en Material y hay que dibujarla.
 */

/** Sombra dura desplazada de Neo. `Modifier.shadow` siempre difumina; esta no. */
fun Modifier.hardShadow(offset: Dp, corner: Dp, color: Color): Modifier = this.drawBehind {
    val dx = offset.toPx()
    val r = corner.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(dx, dx),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(r, r),
    )
}

/** Fondo + borde + sombra del tema activo, en un solo modificador. */
@Composable
fun Modifier.vidaSurface(
    shape: Shape,
    corner: Dp,
    background: Color,
    borderColor: Color,
    elevated: Boolean = true,
): Modifier {
    val spec = VidaTheme.spec
    var m = this
    if (elevated) {
        m = when {
            spec.elevation.isHard -> m.hardShadow(spec.elevation.hardOffset, corner, spec.colors.text)
            spec.elevation.card > 0.dp -> m.shadow(spec.elevation.card, shape, clip = false)
            else -> m
        }
    }
    return m.background(background, shape).border(spec.borderWidth, borderColor, shape)
}

/** Los dos temas editoriales sustituyen la caja por un filete. */
@Composable
private fun isOpenSurface(): Boolean =
    VidaTheme.spec.theme.id == "lumen" || VidaTheme.spec.theme.id == "studio"

/**
 * Tarjeta del artefacto (`.card`). En Lumen y Studio deja de ser una caja y
 * pasa a ser un bloque abierto con filete superior — igual que en la Web,
 * donde esos dos temas hacen `background: transparent; border-top: 1px`.
 */
@Composable
fun VidaCard(
    modifier: Modifier = Modifier,
    padding: Dp = VidaSpacing.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spec = VidaTheme.spec
    val c = spec.colors

    if (isOpenSurface()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .drawBehind { drawRect(c.border, size = Size(size.width, spec.borderWidth.toPx())) }
                .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier)
                .padding(top = padding, bottom = padding),
            verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            content = content,
        )
        return
    }

    val shape = RoundedCornerShape(spec.radii.card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .vidaSurface(shape, spec.radii.card, c.surfaceVariant, c.line)
            .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
        content = content,
    )
}

/**
 * Fila del artefacto (`.row`): superficie con un filete de tono a la izquierda
 * que codifica el estado. En Lumen y Studio pierde la caja y se separa con una
 * línea inferior.
 */
@Composable
fun VidaRow(
    modifier: Modifier = Modifier,
    tone: Color? = null,
    onClick: (() -> Unit)? = null,
    verticalPadding: Dp = 12.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val spec = VidaTheme.spec
    val c = spec.colors
    val flat = isOpenSurface()
    val shape = RoundedCornerShape(spec.radii.card)
    val toneColor = tone ?: c.line
    val railWidth = if (spec.borderWidth > 1.dp) 6.dp else 3.dp

    val base = if (flat) {
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = c.line,
                    topLeft = Offset(0f, size.height - spec.borderWidth.toPx()),
                    size = Size(size.width, spec.borderWidth.toPx()),
                )
            }
    } else {
        Modifier
            .fillMaxWidth()
            .vidaSurface(shape, spec.radii.card, c.surfaceVariant, c.line)
            .drawBehind {
                // El filete de tono solo afecta al borde izquierdo, así que se
                // dibuja en vez de componer un borde de cuatro lados.
                //
                // SE RECORTA AL CONTORNO DE LA FILA. Antes se conseguía la
                // esquina redondeada dibujando un rectángulo redondeado de
                // `max(ancho, radio) * 2` y dejando que la mitad derecha se
                // saliera de vista. Con un radio pequeño colaba; con el de Calm
                // (26 dp) o el de Organic (24 dp) eso son 52 dp de losa maciza
                // detrás del contenido. No se veía porque ninguna lista usaba
                // esta fila todavía: el mosaico cuadrado la había dejado sin
                // uso. Recortar da la misma esquina sin inventar anchura.
                val w = railWidth.toPx()
                val r = spec.radii.card.toPx()
                val outline = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
                }
                clipPath(outline) { drawRect(toneColor, size = Size(w, size.height)) }
            }
    }

    Row(
        modifier = modifier
            .then(base)
            .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier)
            .padding(
                start = if (flat) 0.dp else VidaSpacing.md,
                end = if (flat) 0.dp else VidaSpacing.md,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
        content = content,
    )
}

/**
 * Toque con onda del sistema.
 *
 * Sustituye a la onda dibujada a mano del prototipo: la nativa respeta el tema
 * y el ajuste de accesibilidad de animaciones, cosa que la del navegador no
 * podía hacer.
 */
@Composable
fun Modifier.vidaClickable(onClick: () -> Unit, enabled: Boolean = true): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val color = VidaTheme.colors.primary
    return this.clickable(
        interactionSource = interaction,
        indication = ripple(color = color),
        enabled = enabled,
        onClick = onClick,
    )
}

/** Cuadro de icono de una fila (`.mark`). */
@Composable
fun VidaMark(
    modifier: Modifier = Modifier,
    background: Color = VidaTheme.colors.primaryContainer,
    contentColor: Color = VidaTheme.colors.primary,
    boxSize: Dp = 36.dp,
    circle: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = if (circle) CircleShape else RoundedCornerShape(VidaTheme.spec.radii.control)
    Box(
        modifier = modifier.size(boxSize).background(background, shape),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
