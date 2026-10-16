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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.VidaLayout
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
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.core.ui.VidaDates

/**
 * DETALLE DE UNA PERSONA (Laboral).
 *
 * La pantalla existe para responder una pregunta: **¿qué tengo pendiente con
 * esta persona?** Por eso, debajo de la identidad, van sus relaciones reales —
 * seguimientos, proyectos y tareas— y no una ficha de contacto.
 *
 * TODO SALE DEL BACKEND QUE YA EXISTE: `people`, `commitments`, `projects` y
 * `reminders` con `personId`. No se creó ninguna capacidad nueva para esto.
 *
 * La inicial del héroe hace de avatar: no hay foto en el modelo y poner un
 * marcador genérico sería fingir un dato que nadie ha dado.
 */
@Composable
fun PersonaDetalleScreen(
    personId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type

    val person = state.data.people.firstOrNull { it.id == personId }

    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, CreatableResource.PERSON, personId, snack)

    VidaDetailPage(
        title = "Persona",
        onBack = { navController.popBackStack() },
        snack = snack,
    ) {
        if (person == null) {
            VidaError(
                title = "No encontramos a esa persona",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaDetailPage
        }

        val commitments = state.data.commitments.filter { it.personId == personId }
        val open = commitments.count { it.status != "DONE" }
        val projects = state.data.projects.filter { it.clientPersonId == personId }

        val values = viewModel.editSnapshot(CreatableResource.PERSON, personId)?.first.orEmpty()
        val refs = infoRefsOf(state, viewModel)
        VidaDetailHeroCard(
            section = VidaSections.Familia,
            eyebrow = listOfNotNull("Laboral", "Persona", person.organization?.takeIf { it.isNotBlank() }).joinToString(" · "),
            title = person.name,
            onRename = { saver.save("name", "Nombre", it, person.name, flashKey = "title") },
            onEmptyTitle = { snack.show("El nombre no puede quedar vacío") },
            figure = person.name.firstOrNull()?.uppercase(),
            status = if (open > 0) "$open seguimiento${if (open == 1) "" else "s"} abierto${if (open == 1) "" else "s"}" else "Sin nada pendiente",
            statusColor = if (open > 0) c.warning else c.success,
        )
        VidaInfoSection(
            items = infoItemsFor(CreatableResource.PERSON, values, refs, saver),
            refs = refs,
            flash = saver.flash,
        )

        /* ── SEGUIMIENTOS CON ESTA PERSONA ───────────────────────────────── */
        StaggeredAppear(2) {
            VidaSectionLabel("Seguimientos", trailing = "${commitments.size}")
        }
        StaggeredAppear(3) {
            if (commitments.isEmpty()) {
                Text("Nada pendiente con ${person.name.substringBefore(' ')}.", style = t.body, color = c.textTertiary)
            } else {
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

        /* ── PROYECTOS DONDE ES CLIENTE ──────────────────────────────────── */
        if (projects.isNotEmpty()) {
            StaggeredAppear(4) { VidaSectionLabel("Proyectos", trailing = "${projects.size}") }
            StaggeredAppear(5) {
                ResourceBoard(
                    entries = projects.map { p ->
                        ResourceEntry(
                            id = p.id,
                            title = p.name,
                            subtitle = listOfNotNull(
                        p.status?.takeIf { it.isNotBlank() }?.let(VidaVocabulary::human),
                        p.deadline?.let(VidaDates::absolute),
                    ).joinToString(" · "),
                            tone = c.primary,
                        )
                    },
                    onOpenDetail = { navController.go(Routes.projectRoute(it.id)) },
                )
            }
        }

        /* ── TAREAS ASIGNADAS ────────────────────────────────────────────── */
        StaggeredAppear(6) {
            Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.blockGap)) {
                VidaSectionLabel("Tareas")
                Text(
                    "Las tareas enlazadas a una persona se ven en Tareas, filtrando por ella.",
                    style = t.body,
                    color = c.textTertiary,
                )
                VidaSmallButton("Abrir Tareas", { navController.go(Routes.TASKS_LAB) }, ghost = true)
            }
        }
    }
}
