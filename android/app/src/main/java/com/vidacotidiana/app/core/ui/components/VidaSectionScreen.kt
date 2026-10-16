package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * El armazón de una SECCIÓN con la cabecera de Documentos (ADR-028): título
 * grande en negrita con su frase debajo, acciones a la derecha y el contenido
 * desplazable con el mismo margen.
 *
 * Mismos parámetros que `VidaScreen` en lo que importa, para que una sección
 * que no pasa por `ResourceListScreen` (Tareas, Familia, Compartidos) cambie
 * de armazón sin reescribir su cuerpo. `VidaScreen` sigue para lo que no es
 * una sección: ajustes, formularios, visores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VidaSectionScreen(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onNavigationClick: () -> Unit,
    actions: @Composable () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = VidaTheme.colors
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.offset(x = (-12).dp)) {
                    val (back, click) = navButtonFor(showBack, onNavigationClick)
                    VidaIconButton(
                        if (back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                        if (back) "Atrás" else "Abrir navegación",
                        tint = c.text,
                        onClick = click,
                    )
                }
                Column(Modifier.weight(1f).offset(x = (-8).dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 2)
                    }
                }
                actions()
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
    if (onRefresh != null) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().background(c.surface),
        ) { body() }
    } else {
        Box(Modifier.fillMaxSize().background(c.surface)) { body() }
    }
}
