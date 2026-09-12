package com.vidacotidiana.app.feature.laboral

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaDetailHero
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaHeroFigure
import com.vidacotidiana.app.core.ui.components.VidaProperty
import com.vidacotidiana.app.core.ui.components.VidaPropertyBlock
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSectionLabel
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.navigation.Routes
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.vidacotidiana.app.core.ui.VidaVocabulary

/**
 * DETALLE DE UN PROYECTO (Laboral).
 *
 * El avance NO SE INVENTA. `Project` no guarda un porcentaje, así que se deriva
 * de sus tareas: hechas sobre el total de las que le pertenecen. Sin tareas no
 * hay barra —se dice que no las hay— en vez de pintar un 0 % que parecería
 * «empezado y sin avanzar».
 *
 * Participantes de `/projects/{id}/participants` (V32), que ya existía.
 */
@Composable
fun ProyectoDetalleScreen(
    projectId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type

    val project = state.data.projects.firstOrNull { it.id == projectId }

    VidaScreen(
        title = "Proyecto",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            if (project != null) {
                VidaSmallButton(
                    "Editar",
                    { viewModel.requestEdit(CreatableResource.PROJECT, projectId) },
                    ghost = true,
                )
            }
        },
    ) {
        if (project == null) {
            VidaError(
                title = "No encontramos ese proyecto",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaScreen
        }

        val tasks = viewModel.allTasks()
        val commitments = state.data.commitments.filter { it.projectId == projectId }
        val people = state.data.participations
            .filter { it.projectId == projectId }
            .mapNotNull { part -> state.data.people.firstOrNull { it.id == part.personId } }

        val days = project.deadline?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
        val tone = when {
            project.status == "DONE" -> c.success
            days != null && days < 0 -> c.warning
            else -> c.primary
        }

        StaggeredAppear(0) {
            VidaDetailHero(
                tone = tone,
                eyebrow = "Proyecto",
                title = project.name,
                subtitle = listOfNotNull(
                    // El héroe mostraba `EN_CURSO` tal como viene del
                    // servidor. `VidaVocabulary.human` ya existía y ya lo
                    // traducían las listas; los detalles no lo usaban.
                    VidaVocabulary.human(project.status),
                    project.deadline?.let { d ->
                        when {
                            days == null -> null
                            days < 0 -> "venció hace ${-days} días"
                            days == 0L -> "vence hoy"
                            else -> "vence en $days días"
                        }
                    },
                    "${people.size} participante${if (people.size == 1) "" else "s"}",
                ).joinToString(" · "),
                figure = { VidaHeroFigure(project.name.firstOrNull()?.uppercase() ?: "·") },
            )
        }

        StaggeredAppear(1) {
            VidaPropertyBlock(
                listOfNotNull(
                    project.status?.takeIf { it.isNotBlank() }?.let {
                        VidaProperty("Estado", VidaVocabulary.human(it), c.primaryContainer, c.primaryDeep)
                    },
                    project.deadline?.let {
                        VidaProperty(
                            "Fecha límite",
                            it.toString(),
                            if (days != null && days < 0) c.warningContainer else c.sunken,
                            if (days != null && days < 0) c.warningText else c.textSecondary,
                        )
                    },
                    project.clientPersonId?.let { pid ->
                        state.data.people.firstOrNull { it.id == pid }?.let { person ->
                            VidaProperty(
                                "Cliente",
                                person.name,
                                c.secondContainer,
                                c.second,
                                onClick = { navController.navigate(Routes.personRoute(person.id)) },
                            )
                        }
                    },
                ),
            )
        }

        /* ── PARTICIPANTES ───────────────────────────────────────────────── */
        StaggeredAppear(2) { VidaSectionLabel("Participantes", trailing = "${people.size}") }
        StaggeredAppear(3) {
            if (people.isEmpty()) {
                Text("Todavía no hay nadie asignado.", style = t.body, color = c.textTertiary)
            } else {
                ResourceBoard(
                    entries = people.map { p ->
                        ResourceEntry(
                            id = p.id,
                            title = p.name,
                            subtitle = listOfNotNull(p.role, p.organization).joinToString(" · "),
                            tone = c.primary,
                        )
                    },
                    onOpenDetail = { navController.navigate(Routes.personRoute(it.id)) },
                )
            }
        }

        /* ── SEGUIMIENTOS DEL PROYECTO ───────────────────────────────────── */
        if (commitments.isNotEmpty()) {
            StaggeredAppear(4) { VidaSectionLabel("Seguimientos", trailing = "${commitments.size}") }
            StaggeredAppear(5) {
                ResourceBoard(
                    entries = commitments.map { cmt ->
                        ResourceEntry(
                            id = cmt.id,
                            title = cmt.description,
                            subtitle = listOfNotNull(
                                if (cmt.direction == "MINE") "Lo debes" else "Lo esperas",
                                cmt.dueOn?.toString(),
                            ).joinToString(" · "),
                            tone = if (cmt.status == "DONE") c.successText else c.warning,
                        )
                    },
                    onOpenDetail = { navController.navigate(Routes.COMMITMENTS) },
                )
            }
        }
    }
}
