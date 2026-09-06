package com.vidacotidiana.app.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSearchField
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import kotlinx.coroutines.CoroutineScope

/**
 * Familia (ADR-025 §1). La búsqueda solo consulta a partir de CINCO
 * caracteres y con freno de 350 ms: la regla existe para no lanzar una
 * consulta por tecla sobre el padrón de usuarios, y aquí se respeta igual que
 * en la Web. El freno vive en el ViewModel, no en la pantalla.
 */
@Composable
fun FamilyScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val c = VidaTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val typed = query.trim()
    val enough = typed.length >= 5

    VidaScreen(
        title = "Familia",
        subtitle = "Las personas con las que compartes tu día a día.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) {
            VidaSearchField(
                query,
                {
                    query = it
                    viewModel.searchUsers(it)
                },
                "Buscar por nombre de usuario…",
            )
        }
        StaggeredAppear(1) {
            Text(
                when {
                    typed.isEmpty() -> "Mínimo 5 caracteres antes de buscar."
                    !enough -> "Escribe ${5 - typed.length} más para buscar."
                    state.searching -> "Buscando «$typed»…"
                    state.userSearch.isEmpty() -> "Nadie coincide con «$typed»."
                    else -> "${state.userSearch.size} coincidencias."
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }

        // Resultados de búsqueda: cada uno dice en qué situación está contigo,
        // porque invitar a quien ya es familia no tiene sentido.
        if (enough && state.userSearch.isNotEmpty()) {
            StaggeredAppear(2) { Eyebrow("Resultados") }
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                state.userSearch.forEachIndexed { index, user ->
                    StaggeredAppear(3 + index) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                        ) {
                            ResourceRow(
                                title = user.username,
                                subtitle = relationLabel(user.relation),
                                pill = when (user.relation) {
                                    "FAMILY" -> "En tu familia" to PillTone.OK
                                    "INVITATION_SENT" -> "Invitación enviada" to PillTone.NEUTRAL
                                    "INVITATION_RECEIVED" -> "Te invitó" to PillTone.NEUTRAL
                                    else -> null
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (user.relation == "NONE") {
                                VidaSmallButton("Invitar", { viewModel.invite(user.userId) })
                            }
                        }
                    }
                }
            }
        }

        // Invitaciones recibidas: se aceptan o se rechazan aquí mismo.
        if (state.data.receivedInvitations.isNotEmpty()) {
            StaggeredAppear(4) { Eyebrow("Te invitaron") }
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                state.data.receivedInvitations.forEachIndexed { index, invitation ->
                    StaggeredAppear(5 + index) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                        ) {
                            ResourceRow(
                                title = invitation.username,
                                subtitle = "Quiere compartir contigo",
                                modifier = Modifier.weight(1f),
                            )
                            VidaSmallButton("Rechazar", { viewModel.rejectInvitation(invitation.id) }, ghost = true)
                            VidaSmallButton("Aceptar", { viewModel.acceptInvitation(invitation.id) })
                        }
                    }
                }
            }
        }

        StaggeredAppear(6) { Eyebrow("Tu familia") }
        when {
            state.loading && state.data.familyMembers.isEmpty() -> StaggeredAppear(7) { LoadingRows(2) }
            state.data.familyMembers.isEmpty() -> StaggeredAppear(7) {
                Text(
                    if (state.error != null) {
                        "No pudimos cargar tu familia. Revisa tu conexión."
                    } else {
                        "Todavía no compartes con nadie. Busca a alguien por su nombre de usuario."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                state.data.familyMembers.forEachIndexed { index, member ->
                    StaggeredAppear(7 + index) {
                        ResourceRow(
                            title = member.username,
                            subtitle = member.sinceLabel?.let { "Desde el $it" } ?: "En tu familia",
                            pill = "En tu familia" to PillTone.OK,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lo que `RelationState` significa para quien lo lee, no su nombre técnico.
 * Los cuatro valores son los que devuelve `FamilyService.RelationState`.
 */
private fun relationLabel(relation: String): String = when (relation) {
    "FAMILY" -> "Ya comparten"
    "INVITATION_SENT" -> "Esperando su respuesta"
    "INVITATION_RECEIVED" -> "Te invitó — acéptalo abajo"
    else -> "Puedes invitarle"
}
