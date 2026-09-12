package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.navigation.Destination
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaIconSize

/**
 * Barra superior del artefacto: menú o atrás, título con la tipografía de
 * display del tema, y las acciones de la derecha.
 */
@Composable
fun VidaAppBar(
    title: String,
    subtitle: String? = null,
    showBack: Boolean,
    onNavigationClick: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VidaIconButton(
                icon = if (showBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                contentDescription = if (showBack) "Atrás" else "Abrir navegación",
                onClick = onNavigationClick,
            )
            Text(
                text = if (spec.fonts.displayUppercase) title.uppercase() else title,
                style = MaterialTheme.typography.headlineMedium,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 2.dp),
            )
            actions()
        }
        if (subtitle != null) {
            // ALINEADO CON EL TÍTULO, no con el margen de la pantalla.
            //
            // El título arranca después del botón de navegación (6 + 48 + 2 dp)
            // y el subtítulo arrancaba en el gutter: las dos líneas de una
            // misma cabecera caían en ejes distintos, y el título parecía
            // sangrado respecto a todo lo demás. Una cabecera es un bloque; sus
            // dos líneas comparten eje aunque el bloque entero quede desplazado
            // por el icono.
            //
            // El tamaño no se toca: 22 sp de titular contra 13 sp de apoyo, en
            // dos tintas distintas, ya es una jerarquía correcta. Bajar el
            // subtítulo a `textTertiary` lo habría dejado en 2,5:1 de contraste
            // — por debajo de AA — para resolver un problema que no era de
            // tamaño sino de eje.
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    start = 6.dp + VidaLayout.touchTarget + 2.dp,
                    end = VidaSpacing.lg,
                    bottom = 6.dp,
                ),
            )
        }
    }
}

/**
 * Botón de icono con blanco táctil COMPLETO y onda sin límites.
 *
 * Medía 44 dp. Es el botón de atrás, el de menú, el de notificaciones y el «+»
 * de cabecera: los cuatro que aparecen en todas las pantallas, y los cuatro
 * quedaban 4 dp por debajo del mínimo de Android. Lo que se dibuja no cambia
 * —el icono sigue midiendo lo mismo—; lo que crece es el área que responde.
 */
@Composable
fun VidaIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color? = null,
    badge: Boolean = false,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(VidaLayout.touchTarget)
            .pressScale(interaction, 0.9f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = c.primary, bounded = false),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint ?: c.textSecondary, modifier = Modifier.size(VidaIconSize.medium))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 11.dp)
                    .size(7.dp)
                    .background(c.second, RoundedCornerShape(50)),
            )
        }
    }
}

/** Conmutador de contexto: Portal · Personal · Laboral. */
@Composable
fun ContextBar(
    current: AppContext,
    laboralEnabled: Boolean,
    onSelect: (AppContext) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val options = buildList {
        add(AppContext.PORTAL)
        add(AppContext.PERSONAL)
        if (laboralEnabled) add(AppContext.LABORAL)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = VidaSpacing.md, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
    ) {
        options.forEach { ctx ->
            val on = ctx == current
            val interaction = remember(ctx) { MutableInteractionSource() }
            val shape = RoundedCornerShape(spec.radii.pill)
            val bg by animateColorAsState(if (on) c.primary else Color.Transparent, label = "ctxBg")
            val fg by animateColorAsState(if (on) c.onPrimary else c.textSecondary, label = "ctxFg")
            Box(
                modifier = Modifier
                    .pressScale(interaction, 0.95f)
                    .background(bg, shape)
                    .border(spec.borderWidth, if (on) c.primary else c.border, shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(color = c.primary),
                        role = Role.Tab,
                    ) { onSelect(ctx) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(ctx.label, style = VidaTheme.type.action, color = fg)
            }
        }
    }
}

/**
 * Contenido del menú lateral: el ACCESO SECUNDARIO.
 *
 * Solo muestra lo que no está en la barra inferior. Repetir aquí los destinos
 * prioritarios los volvería a igualar en peso, que es justo lo que esta
 * arquitectura evita.
 */
@Composable
fun VidaDrawerContent(
    context: AppContext,
    sections: List<Destination>,
    account: List<Destination>,
    currentRoute: String?,
    themeLabel: String,
    onDestination: (Destination) -> Unit,
    onLogout: () -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    Column(
        Modifier
            // Alto completo si, ancho el que le de el cajon: pedir
            // `fillMaxSize` era lo que estiraba el menu hasta cubrir todo.
            .fillMaxHeight()
            .background(c.surfaceVariant)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VidaSpacing.md, vertical = VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // La marca «A · Tiempo»: el arco pesa más que el nombre, igual que en
        // la Web. No cambia de forma entre temas; solo de color.
        Row(
            Modifier.padding(start = 8.dp, bottom = VidaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
        ) {
            Box(
                Modifier.size(width = 48.dp, height = 24.dp).drawBehind {
                    drawArc(
                        color = c.second, startAngle = 200f, sweepAngle = 120f, useCenter = false,
                        topLeft = Offset(0f, 2f),
                        size = Size(size.width * 0.92f, size.height * 1.9f),
                        style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                    drawCircle(color = c.second, radius = 4.5.dp.toPx(), center = Offset(size.width * 0.87f, size.height * 0.62f))
                },
            )
            Text(
                "Cotidiana",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = spec.fonts.display, fontWeight = FontWeight.Light, fontSize = 23.sp,
                ),
                color = c.text,
            )
        }

        if (sections.isNotEmpty()) {
            DrawerGroup("${context.label} · más secciones")
            sections.forEach { d ->
                DrawerItem(d, d.route == currentRoute) { onDestination(d) }
            }
        } else {
            DrawerGroup(context.label)
            Text(
                "Todo este contexto vive en la barra inferior.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
            )
        }

        DrawerGroup("Cuenta")
        account.forEach { d ->
            val label = if (d.route == "appearance") "Tema · $themeLabel" else d.label
            DrawerItem(d.copy(label = label), d.route == currentRoute) { onDestination(d) }
        }
        DrawerItem(
            Destination("logout", "Cerrar sesión", Icons.AutoMirrored.Filled.Logout),
            selected = false,
            danger = true,
            onClick = onLogout,
        )
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

@Composable
private fun DrawerGroup(text: String) {
    Box(Modifier.padding(start = 11.dp, top = VidaSpacing.md, bottom = 5.dp)) {
        Eyebrow(text, rule = false)
    }
}

@Composable
private fun DrawerItem(
    destination: Destination,
    selected: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.control)
    val fg = when {
        danger -> c.error
        selected -> c.primary
        else -> c.textSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) c.primaryContainer else Color.Transparent, shape)
            // Papel marca el activo con un filete lateral, como en la Web.
            .then(
                if (selected && spec.theme.id == "papel") {
                    Modifier.drawBehind { drawRect(c.second, size = Size(3.dp.toPx(), size.height)) }
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interaction, indication = ripple(color = c.primary), onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        Icon(destination.icon, contentDescription = null, tint = fg, modifier = Modifier.size(19.dp))
        Text(
            destination.label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
