package com.vidacotidiana.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.vocabulary.ProfessionalProfile
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaChipRow
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.navigation.Routes

/**
 * Ajustes: modos, perfil profesional, invitaciones familiares y apariencia.
 *
 * Cambiar el perfil renombra dos secciones de Laboral Y el cuarto acceso de la
 * barra inferior — con el vocabulario real de `design-system.md` §12, no con
 * etiquetas inventadas.
 */
@Composable
fun SettingsScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors

    VidaScreen(
        title = "Ajustes",
        subtitle = "Activa los modos con los que quieres usar Cotidiana.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) { Eyebrow("Modos") }
        StaggeredAppear(1) {
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                ResourceRow(
                    "Personal", "Tu vida cotidiana, tu hogar, tu familia",
                    icon = Icons.Outlined.Home, pill = "Activado" to PillTone.OK,
                )
                ResourceRow(
                    "Laboral", "Tu consultorio, tu oficina, tu negocio",
                    icon = Icons.Outlined.Work,
                    trailing = {
                        VidaSmallButton(
                            if (state.laboralEnabled) "Activado" else "Activar",
                            { viewModel.setLaboralEnabled(!state.laboralEnabled) },
                            ghost = state.laboralEnabled,
                        )
                    },
                )
            }
        }

        if (state.laboralEnabled) {
            StaggeredAppear(2) { Eyebrow("Perfil") }
            StaggeredAppear(3) {
                VidaCard {
                    Text(
                        "Solo cambia las palabras: tus datos, pantallas y funciones son los mismos.",
                        style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                    )
                    VidaChipRow(
                        options = ProfessionalProfile.entries.map { it.label },
                        selected = state.profile.label,
                        onSelect = { label ->
                            ProfessionalProfile.entries.firstOrNull { it.label == label }?.let(viewModel::setProfile)
                        },
                    )
                    Text(
                        "Con este perfil, Laboral llama ${state.profile.projectPlural} y ${state.profile.personPlural} " +
                            "a esas secciones — y la barra inferior cambia con ellas.",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                    )
                }
            }
        }

        StaggeredAppear(4) { Eyebrow("Invitaciones a una familia") }
        StaggeredAppear(5) {
            ResourceRow(
                "Tienes una invitación pendiente", "Responde para empezar a compartir",
                tone = c.warning, pill = "Pendiente" to PillTone.WARN,
                trailing = { VidaSmallButton("Aceptar", {}) },
            )
        }

        StaggeredAppear(6) { Eyebrow("Apariencia") }
        StaggeredAppear(7) {
            ResourceRow(
                "Tema", "${state.theme.label} · ${state.theme.tagline}",
                icon = Icons.Outlined.Palette,
                onClick = { navController.navigate(Routes.APPEARANCE) },
            )
        }

        StaggeredAppear(8) { Eyebrow("Cuenta") }
        StaggeredAppear(9) {
            ResourceRow(
                "Cerrar sesión", "Se cierra la sesión en este dispositivo",
                icon = Icons.AutoMirrored.Filled.Logout,
                markBackground = c.errorContainer, markTint = c.error, tone = c.error,
                onClick = { navController.popBackStack() },
            )
        }
    }
}
