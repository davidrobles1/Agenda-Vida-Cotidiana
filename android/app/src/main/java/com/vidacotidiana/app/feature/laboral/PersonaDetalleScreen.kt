package com.vidacotidiana.app.feature.laboral

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

    VidaScreen(
        title = "Persona",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            if (person != null) {
                VidaSmallButton(
                    "Editar",
                    { viewModel.requestEdit(CreatableResource.PERSON, personId) },
                    ghost = true,
                )
            }
        },
    ) {
        if (person == null) {
            VidaError(
                title = "No encontramos a esa persona",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaScreen
        }

        val commitments = state.data.commitments.filter { it.personId == personId }
        val open = commitments.count { it.status != "DONE" }
        val projects = state.data.projects.filter { it.clientPersonId == personId }

        StaggeredAppear(0) {
            VidaDetailHero(
                tone = c.primary,
                eyebrow = person.role ?: "Persona",
                title = person.name,
                subtitle = listOfNotNull(
                    person.organization,
                    if (open > 0) "$open seguimiento${if (open == 1) "" else "s"} abierto${if (open == 1) "" else "s"}" else null,
                ).joinToString(" · ").ifBlank { "Sin nada pendiente" },
                figure = { VidaHeroFigure(person.name.firstOrNull()?.uppercase() ?: "·") },
            )
        }

        StaggeredAppear(1) {
            VidaPropertyBlock(
                listOfNotNull(
                    person.role?.let { VidaProperty("Rol", it, c.primaryContainer, c.primaryDeep) },
                    // Una propiedad opcional se omite cuando NO tiene valor,
                    // y vacío es tanto `null` como una cadena en blanco: con
                    // sólo mirar el nulo, un campo guardado vacío pintaba la
                    // fila con su píldora sin nada dentro. Misma regla que
                    // usan las demás propiedades de este detalle.
                    person.organization?.takeIf { it.isNotBlank() }?.let {
                        VidaProperty("Organización", it, c.sunken, c.textSecondary)
                    },
                    VidaProperty(
                        "Seguimientos",
                        if (open == 0) "Ninguno abierto" else "$open abiertos",
                        if (open == 0) c.successContainer else c.warningContainer,
                        if (open == 0) c.successText else c.warningText,
                        onClick = { navController.navigate(Routes.COMMITMENTS) },
                    ),
                ),
            )
        }

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
                    onOpenDetail = { navController.navigate(Routes.COMMITMENTS) },
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
                    onOpenDetail = { navController.navigate(Routes.projectRoute(it.id)) },
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
                VidaSmallButton("Abrir Tareas", { navController.navigate(Routes.TASKS_LAB) }, ghost = true)
            }
        }
    }
}
