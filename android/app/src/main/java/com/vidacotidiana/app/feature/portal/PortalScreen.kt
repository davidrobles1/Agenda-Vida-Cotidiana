package com.vidacotidiana.app.feature.portal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaMark
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.core.ui.components.vidaSoftShadow
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.navigation.Routes
import java.time.LocalDate

/**
 * PORTAL — el selector de contexto (ADR-019).
 *
 * Composición del artefacto: dos piezas de contexto muy desiguales a propósito
 * —Personal en degradado a toda anchura, Laboral en superficie hundida— porque
 * no son dos opciones equivalentes: una es donde vives y la otra donde
 * trabajas. Debajo, la cuenta.
 *
 * NO TIENE BARRA INFERIOR. Portal no es una sección con destinos hermanos: es
 * la puerta. Una barra de un destino sería un adorno.
 *
 * Las cifras de las píldoras salen de `AttentionEngine` y de `VidaData`, no de
 * un número escrito a mano.
 */
@Composable
fun PortalScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    LaunchedEffect(Unit) { viewModel.loadUser() }

    val attention = AttentionEngine.scan(state.data, viewModel.allTasks(), today)
    // «Para hoy» es HOY, no «hoy + lo atrasado».
    //
    // Esta pantalla contaba `now()` —que son las dos cosas— bajo la etiqueta
    // «para hoy», y ademas enseñaba lo atrasado al lado: los mismos registros
    // se contaban dos veces, exactamente el mismo fallo que tenia Inicio. Como
    // la causa era una sola —confundir «lo que no puede esperar» con «lo de
    // hoy»—, la correccion es la misma en los dos sitios.
    val forToday = AttentionEngine.today(attention)
    val overdue = AttentionEngine.overdue(attention).size
    val name = state.user?.username?.takeIf { it.isNotBlank() }
        ?: state.user?.email?.substringBefore('@')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(horizontal = VidaLayout.gutter),
    ) {
        Spacer(Modifier.height(VidaLayout.sectionGap))

        StaggeredAppear(0) {
            Column {
                Text("Agenda", style = t.screenTitle.copy(fontSize = 28.sp, lineHeight = 34.sp), color = c.text)
                Text(
                    if (name != null) "Hola, $name. ¿Dónde quieres estar?" else "¿Dónde quieres estar?",
                    style = t.body,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(VidaLayout.sectionGap))

        /* ── PERSONAL: la pieza grande, en degradado ─────────────────────── */
        StaggeredAppear(1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .vidaSoftShadow(30.dp, color = c.primary, keyBlur = 30.dp, keyOffsetY = 12.dp, keyAlpha = 0.30f)
                    .background(
                        Brush.linearGradient(listOf(c.primary, c.primaryDeep)),
                        RoundedCornerShape(30.dp),
                    )
                    .vidaClickable(onClick = { viewModel.setContext(AppContext.PERSONAL) })
                    .padding(20.dp),
            ) {
                Text(
                    "CONTEXTO",
                    style = t.eyebrow,
                    color = c.onPrimary.copy(alpha = 0.8f),
                )
                Text(
                    "Personal",
                    style = t.screenTitle.copy(fontSize = 30.sp, lineHeight = 36.sp),
                    color = c.onPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "Tu casa, tu día, tus pagos y lo que compartes con tu familia.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                    color = c.onPrimary.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    listOfNotNull(
                        "${forToday.size} para hoy",
                        if (overdue > 0) "$overdue atrasado" else null,
                        "${state.data.payments.size} pagos",
                    ).forEach { label ->
                        Box(
                            Modifier
                                .background(c.onPrimary.copy(alpha = 0.18f), RoundedCornerShape(50))
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                ),
                                color = c.onPrimary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(VidaLayout.blockGap))

        /* ── LABORAL: la pieza hundida ───────────────────────────────────── */
        StaggeredAppear(2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.sunken, RoundedCornerShape(30.dp))
                    .vidaClickable(onClick = { viewModel.setContext(AppContext.LABORAL) })
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                VidaMark(background = c.secondContainer, contentColor = c.second, boxSize = 48.dp) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("CONTEXTO", style = t.eyebrow, color = c.textTertiary)
                    Text(
                        "Laboral",
                        style = t.sectionTitle.copy(fontSize = 24.sp, lineHeight = 30.sp),
                        color = c.text,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Text(
                        "Tu jornada, tus proyectos, tu gente y lo que estás esperando.",
                        style = t.body,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        listOf(
                            "${state.data.projects.size} proyectos" to c.secondContainer,
                            "${state.data.commitments.size} seguimientos" to c.primaryContainer,
                        ).forEach { (label, bg) ->
                            Box(Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 5.dp)) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    ),
                                    color = c.textSecondary,
                                )
                            }
                        }
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp).padding(top = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(VidaLayout.sectionGap))

        /* ── TU CUENTA ───────────────────────────────────────────────────── */
        StaggeredAppear(3) {
            Text("TU CUENTA", style = t.eyebrow, color = c.textTertiary)
        }
        Spacer(Modifier.height(VidaLayout.blockGap))
        StaggeredAppear(4) {
            Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
                listOf(
                    Quad(Routes.PROFILE, Icons.Outlined.Groups, "Perfil", state.user?.email ?: "Tu cuenta"),
                    Quad(Routes.SETTINGS, Icons.Outlined.Settings, "Configuración", "Tema, privacidad, invitaciones"),
                    Quad(Routes.NOTIFICATIONS, Icons.Outlined.Notifications, "Notificaciones", "Lo que reclamó tu atención"),
                    Quad(Routes.CAPABILITIES, Icons.Outlined.Storage, "Capacidades y backend", "Qué existe y qué falta crear"),
                ).forEach { (route, icon, title, sub) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surfaceElevated, RoundedCornerShape(20.dp))
                            .vidaClickable(onClick = { navController.navigate(route) })
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        VidaMark(background = c.surfaceVariant, contentColor = c.textSecondary, boxSize = 34.dp) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, style = t.cardTitle.copy(fontSize = 14.sp), color = c.text)
                            Text(sub, style = t.caption, color = c.textTertiary)
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(VidaLayout.sectionGap))
    }
}

/** Cuatro datos por fila de cuenta. Local: no merece un tipo compartido. */
private data class Quad(
    val route: String,
    val icon: ImageVector,
    val title: String,
    val sub: String,
)
