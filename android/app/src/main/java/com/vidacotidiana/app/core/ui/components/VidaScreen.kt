package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.navigation.CreateAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * El esqueleto que comparten todas las pantallas: barra superior con menú o
 * atrás, subtítulo, conmutador de contexto y un cuerpo con desplazamiento y
 * entrada escalonada.
 *
 * Centralizarlo no es solo ahorro de líneas: es lo que garantiza que las
 * diecisiete secciones respiren igual —mismos márgenes, mismo ritmo vertical,
 * misma animación de llegada— en vez de que cada una acabe con su propia
 * interpretación del espaciado.
 */
@Composable
fun VidaScreen(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onNavigationClick: () -> Unit,
    context: AppContext? = null,
    laboralEnabled: Boolean = true,
    onContextSelect: (AppContext) -> Unit = {},
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        VidaAppBar(
            title = title,
            subtitle = subtitle,
            showBack = showBack,
            onNavigationClick = onNavigationClick,
            actions = actions,
        )
        if (context != null) {
            ContextBar(current = context, laboralEnabled = laboralEnabled, onSelect = onContextSelect)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VidaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VidaSpacing.md),
        ) {
            Spacer(Modifier.height(VidaSpacing.xs))
            content()
            // Aire al final: el artefacto nunca deja el último bloque pegado al
            // borde, y con la barra inferior encima se notaría todavía más.
            Spacer(Modifier.height(VidaSpacing.xl))
        }
    }
}

/** Abre el cajón desde la barra superior sin que cada pantalla repita el `launch`. */
@Composable
fun openDrawerAction(drawerState: DrawerState, scope: CoroutineScope): () -> Unit = {
    scope.launch { drawerState.open() }
}

/**
 * La hoja del «+»: reúne las acciones de creación que YA existen en el
 * contexto activo. No convierte el botón en un menú de capacidades nuevas.
 */
@Composable
fun CreateSheet(
    context: AppContext,
    actions: List<CreateAction>,
    onPick: (CreatableResource) -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = VidaSpacing.lg)
            .padding(bottom = VidaSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
    ) {
        Text("Crear", style = MaterialTheme.typography.headlineSmall, color = c.text)
        Text(
            "En ${context.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = VidaSpacing.sm),
        )
        actions.forEach { action ->
            ResourceRow(
                title = action.label,
                subtitle = "",
                icon = action.icon,
                onClick = { onPick(action.resource) },
            )
        }
    }
}

/** Título de sección con contador, para las cabeceras internas del cuerpo. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Eyebrow(text, modifier)
}

/** Cabecera de una tira horizontal de tarjetas. */
@Composable
fun StripRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { content() }
}
