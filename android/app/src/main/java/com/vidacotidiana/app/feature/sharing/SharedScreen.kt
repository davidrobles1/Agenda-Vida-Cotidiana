package com.vidacotidiana.app.feature.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSegmented
import kotlinx.coroutines.CoroutineScope

/**
 * Compartidos (ADR-025 §3/§4) — las dos direcciones separadas por pestaña,
 * porque confundir recibido con enviado cambia quién tiene que actuar.
 *
 * "Ya hice mi parte" NO toca el recurso: registra que ESA persona cumplió lo
 * suyo. El estado de un recordatorio sigue siendo único y global (DEC-001).
 */
@Composable
fun SharedScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val c = VidaTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Me compartieron") }
    val receiving = tab == "Me compartieron"
    val list = if (receiving) state.data.sharedWithMe else state.data.sharedByMe

    VidaScreen(
        title = "Compartidos",
        subtitle = "Lo que tu familia comparte contigo y lo que tú compartes.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) {
            VidaSegmented(listOf("Me compartieron", "Yo compartí"), tab, { tab = it })
        }
        StaggeredAppear(1) {
            Eyebrow("${list.size} ${if (receiving) "recibidos" else "compartidos"}")
        }
        when {
            state.loading && list.isEmpty() -> StaggeredAppear(2) { LoadingRows(2) }
            // ADR-021(k): si la carga falló no se afirma que no hay nada.
            state.error != null && list.isEmpty() -> StaggeredAppear(2) {
                EmptyState(
                    "No pudimos cargar lo compartido",
                    "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to viewModel::refresh,
                )
            }
            list.isEmpty() -> StaggeredAppear(2) {
                EmptyState("Nada por aquí todavía", "Al crear o editar un recurso puedes elegir con quién compartirlo.")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            list.forEachIndexed { index, share ->
                StaggeredAppear(2 + index) {
                    ResourceRow(
                        title = share.label,
                        subtitle = listOfNotNull(
                            if (receiving) "De ${share.counterpart}" else "Con ${share.counterpart}",
                            share.dateLabel,
                        ).joinToString(" · "),
                        typeTag = share.type,
                        tone = if (share.partDone) c.successText else if (share.responsibility) c.warning else null,
                        pill = when {
                            !share.responsibility -> "Solo ver" to PillTone.QUIET
                            share.partDone && receiving -> "Hiciste tu parte" to PillTone.OK
                            share.partDone -> "Ya hizo su parte" to PillTone.OK
                            receiving -> "Te toca" to PillTone.WARN
                            else -> "Pendiente" to PillTone.WARN
                        },
                        // Solo quien recibe puede marcar SU parte: el emisor ve
                        // el estado del otro, no lo cambia.
                        trailing = if (receiving && share.responsibility) {
                            {
                                VidaIconButton(
                                    icon = if (share.partDone) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Check,
                                    contentDescription = if (share.partDone) "Deshacer mi parte" else "Ya hice mi parte",
                                    tint = if (share.partDone) c.successText else c.primary,
                                    onClick = { viewModel.togglePartDone(share.id, share.partDone) },
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        StaggeredAppear(2 + list.size) {
            Text(
                "Compartir se elige al crear o editar el recurso: con quién, y si se compromete con su parte.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}
