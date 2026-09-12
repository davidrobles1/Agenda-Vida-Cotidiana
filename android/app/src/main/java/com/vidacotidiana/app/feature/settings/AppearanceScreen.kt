package com.vidacotidiana.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaThemes
import com.vidacotidiana.app.core.ui.VisualTheme
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaMark
import com.vidacotidiana.app.core.ui.components.VidaPill
import com.vidacotidiana.app.core.ui.components.VidaRing
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaTick
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.core.ui.components.vidaSoftShadow

/**
 * APARIENCIA — la pantalla del artefacto maestro, no la anterior.
 *
 * Antes ofrecía las nueve agendas de ADR-023. El artefacto define DOS —Claro y
 * Noche— y es la fuente de verdad de lo que el usuario ve. Las otras ocho no se
 * borran: siguen en `VidaThemes.catalogue`, siguen resolviéndose por id y
 * siguen pintando correctamente si alguien las tiene guardadas. Simplemente no
 * se ofrecen.
 *
 * El antetítulo hardcodeado `StaggeredAppear(10)` que había al final asumía que
 * el catálogo medía nueve; con dos agendas visibles el retardo se calcula desde
 * la lista, para que añadir o quitar una no vuelva a descolocar la entrada.
 */
@Composable
fun AppearanceScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec
    val themes = VidaThemes.selectable

    VidaScreen(
        title = "Apariencia",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
            themes.forEachIndexed { index, s ->
                StaggeredAppear(index) {
                    val selected = s.theme == state.theme
                    val shape = RoundedCornerShape(spec.radii.card)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .vidaSoftShadow(spec.radii.card)
                            .background(c.surfaceVariant, shape)
                            .border(if (selected) 1.5.dp else spec.borderWidth, if (selected) c.primary else c.line, shape)
                            .vidaClickable(onClick = { viewModel.setTheme(s.theme) })
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        VidaMark(
                            background = if (selected) c.primaryContainer else c.sunken,
                            contentColor = if (selected) c.primary else c.textSecondary,
                            boxSize = 40.dp,
                        ) {
                            Icon(
                                if (s.theme == VisualTheme.NOCHE) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.theme.label, style = t.cardTitle, color = c.text)
                            Text(s.theme.tagline, style = t.caption, color = c.textSecondary)
                        }
                        if (selected) {
                            VidaTick(checked = true, tone = c.primary)
                        } else {
                            Spacer(Modifier.width(23.dp))
                        }
                    }
                }
            }
        }

        // VISTA PREVIA — el artefacto la pone para que el cambio se vea antes de
        // salir de la pantalla, con piezas reales y no con muestras de color.
        StaggeredAppear(themes.size) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .vidaSoftShadow(spec.radii.card)
                    .background(c.surfaceVariant, RoundedCornerShape(spec.radii.card))
                    .border(spec.borderWidth, c.line, RoundedCornerShape(spec.radii.card))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("VISTA PREVIA", style = t.eyebrow, color = c.textTertiary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surfaceElevated, RoundedCornerShape(VidaSpacing.lg + 2.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VidaRing(percent = 66, tone = c.primary, diameter = 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Una tarjeta cualquiera", style = t.cardTitle, color = c.text)
                        Text("Los mismos tokens, otro valor", style = t.caption, color = c.textSecondary)
                    }
                }
                // Las cuatro severidades. El ROJO solo en Error: la severidad
                // alta de ADR-018 va en ámbar, igual que en el artefacto.
                Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                    VidaPill("Alta", PillTone.WARN)
                    VidaPill("Media", PillTone.NEUTRAL)
                    VidaPill("Baja", PillTone.OK)
                    VidaPill("Error", PillTone.DANGER)
                }
            }
        }

        StaggeredAppear(themes.size + 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceElevated, RoundedCornerShape(20.dp))
                    .padding(horizontal = 17.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
                Text(
                    "El tema reescribe variables, nunca duplica componentes (ADR-023). " +
                        "Ninguna pantalla sabe qué tema está activo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }
}
