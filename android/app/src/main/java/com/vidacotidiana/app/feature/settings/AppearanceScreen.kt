package com.vidacotidiana.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaThemes
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.ThemeCard
import com.vidacotidiana.app.core.ui.components.VidaScreen

/**
 * Las nueve agendas. Cada tarjeta muestra la muestra de color y una vista
 * previa de tres tonos reales del tema, para que la elección no dependa de
 * recordar cómo era cada uno.
 */
@Composable
fun AppearanceScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors

    VidaScreen(
        title = "Tema",
        subtitle = "Nueve agendas. Cambia la aplicación entera.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            VidaThemes.catalogue.forEachIndexed { index, spec ->
                StaggeredAppear(index) {
                    ThemeCard(
                        label = spec.theme.label,
                        tagline = spec.theme.tagline,
                        swatch = spec.colors.primary,
                        preview = listOf(spec.colors.surface, spec.colors.surfaceVariant, spec.colors.second),
                        selected = spec.theme == state.theme,
                        onClick = { viewModel.setTheme(spec.theme) },
                    )
                }
            }
        }
        StaggeredAppear(10) {
            Text(
                "El tema alcanza navegación, listas, calendario, formularios y estados.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}
