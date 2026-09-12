package com.vidacotidiana.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaTile
import androidx.compose.ui.graphics.SolidColor
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * PERFIL — la cuenta, con las tres cifras del artefacto.
 *
 * Composición: avatar grande centrado, nombre y correo, la retícula de TRES
 * piezas (no cuatro como Inicio: aquí no hay una cifra que domine) y luego los
 * datos de la cuenta como filas de propiedad.
 *
 * DATOS REALES: `/api/v1/me` para la identidad y `VidaData` para las cifras.
 * Nada de esto es un número escrito a mano.
 */
@Composable
fun PerfilScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type

    LaunchedEffect(Unit) { viewModel.loadUser() }

    val user = state.user
    val name = user?.username?.takeIf { it.isNotBlank() } ?: user?.email?.substringBefore('@').orEmpty()
    val initial = name.firstOrNull()?.uppercase() ?: "·"

    val d = state.data
    val total = d.warranties.size + d.maintenance.size + d.inventory.size + d.documents.size +
        d.payments.size + d.places.size + d.routines.size + viewModel.allTasks().size

    val weekAgo = Instant.now().minus(7, ChronoUnit.DAYS)
    val doneThisWeek = state.reminders.count { r ->
        r.status == "COMPLETED" &&
            (r.updatedAt?.let { runCatching { Instant.parse(it).isAfter(weekAgo) }.getOrDefault(false) } ?: false)
    }
    val pending = AttentionEngine.now(
        AttentionEngine.scan(state.data, viewModel.allTasks(), LocalDate.now()),
    ).size

    VidaScreen(
        title = "Perfil",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        if (user == null) {
            LoadingRows(2)
            return@VidaScreen
        }

        /* ── IDENTIDAD, CENTRADA ─────────────────────────────────────────── */
        StaggeredAppear(0) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(
                            Brush.linearGradient(listOf(c.primary, c.primaryDeep)),
                            RoundedCornerShape(30.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, style = t.heroFigure.copy(fontSize = 32.sp), color = c.onPrimary)
                }
                Text(
                    name.replaceFirstChar { it.uppercase() },
                    style = t.sectionTitle.copy(fontSize = 24.sp, lineHeight = 30.sp),
                    color = c.text,
                )
                Text(user.email, style = t.caption, color = c.textTertiary, textAlign = TextAlign.Center)
            }
        }

        /* ── LAS TRES CIFRAS ─────────────────────────────────────────────── */
        StaggeredAppear(1) {
            Row(
                Modifier.fillMaxWidth().height(92.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VidaTile(
                    kicker = "Registros", figure = total.toString(), caption = "en total",
                    background = SolidColor(c.primaryContainer),
                    figureColor = c.primary, kickerColor = c.primaryDeep, captionColor = c.primaryDeep.copy(alpha = .75f),
                    figureSize = 26.dp, modifier = Modifier.weight(1f),
                )
                VidaTile(
                    kicker = "Hechas", figure = doneThisWeek.toString(), caption = "esta semana",
                    background = SolidColor(c.successContainer),
                    figureColor = c.successText, kickerColor = c.successText, captionColor = c.successText.copy(alpha = .75f),
                    figureSize = 26.dp, modifier = Modifier.weight(1f),
                )
                VidaTile(
                    kicker = "Te esperan", figure = pending.toString(), caption = "ahora mismo",
                    background = SolidColor(c.secondContainer),
                    figureColor = c.second, kickerColor = c.second, captionColor = c.second.copy(alpha = .75f),
                    figureSize = 26.dp, modifier = Modifier.weight(1f),
                )
            }
        }

        /* ── DATOS DE LA CUENTA ──────────────────────────────────────────── */
        StaggeredAppear(2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Nombre" to name.replaceFirstChar { it.uppercase() },
                    "Correo" to user.email,
                    "Estado de la cuenta" to when (user.deletionStatus) {
                        "ACTIVE" -> "Activa"
                        "DELETION_REQUESTED" -> "Eliminación solicitada"
                        else -> user.deletionStatus
                    },
                ).forEach { (k, v) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surfaceElevated, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = t.body, color = c.textSecondary, modifier = Modifier.weight(1f))
                        Text(v, style = t.cardTitle.copy(fontSize = 14.sp), color = c.text)
                    }
                }
            }
        }

        StaggeredAppear(3) {
            Text(
                "El borrado de la cuenta es suave, con 30 días de gracia (DEC-015). " +
                    "Se solicita desde Configuración.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }
    }
}
