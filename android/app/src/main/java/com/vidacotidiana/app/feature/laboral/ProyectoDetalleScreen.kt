package com.vidacotidiana.app.feature.laboral

import com.vidacotidiana.app.navigation.go
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.components.InfoEdit
import com.vidacotidiana.app.core.ui.components.InfoItem
import com.vidacotidiana.app.core.ui.components.VidaDetailHeroCard
import com.vidacotidiana.app.core.ui.components.VidaDetailPage
import com.vidacotidiana.app.core.ui.components.VidaInfoSection
import com.vidacotidiana.app.core.ui.components.infoItemsFor
import com.vidacotidiana.app.core.ui.components.infoRefsOf
import com.vidacotidiana.app.core.ui.components.rememberDetailSnack
import com.vidacotidiana.app.core.ui.components.rememberInlineSaver
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.Icons
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

    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, CreatableResource.PROJECT, projectId, snack)

    VidaDetailPage(
        title = "Proyecto",
        onBack = { navController.popBackStack() },
        snack = snack,
    ) {
        if (project == null) {
            VidaError(
                title = "No encontramos ese proyecto",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaDetailPage
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

        val values = viewModel.editSnapshot(CreatableResource.PROJECT, projectId)?.first.orEmpty()
        val refs = infoRefsOf(state, viewModel)
        VidaDetailHeroCard(
            section = VidaSections.Calendario,
            eyebrow = "Laboral · Proyecto",
            title = project.name,
            onRename = { saver.save("name", "Nombre", it, project.name, flashKey = "title") },
            onEmptyTitle = { snack.show("El nombre no puede quedar vacío") },
            figure = "${people.size}",
            figureCaption = if (people.size == 1) "participante" else "participantes",
            status = listOfNotNull(
                VidaVocabulary.human(project.status),
                days?.let {
                    when {
                        it < 0 -> "venció hace ${-it} días"
                        it == 0L -> "vence hoy"
                        else -> "vence en $it días"
                    }
                },
            ).joinToString(" · ").ifBlank { null },
            statusColor = tone,
        )
        VidaInfoSection(
            items = infoItemsFor(CreatableResource.PROJECT, values, refs, saver) + listOfNotNull(
                project.clientPersonId?.let { pid ->
                    state.data.people.firstOrNull { it.id == pid }?.let { person ->
                        InfoItem(
                            "client", "Cliente", person.name,
                            InfoEdit.Action(Icons.AutoMirrored.Outlined.OpenInNew) { navController.go(Routes.personRoute(person.id)) },
                        )
                    }
                },
            ),
            refs = refs,
            flash = saver.flash,
        )

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
                    onOpenDetail = { navController.go(Routes.personRoute(it.id)) },
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
                    onOpenDetail = { navController.go(Routes.COMMITMENTS) },
                )
            }
        }
    }
}
