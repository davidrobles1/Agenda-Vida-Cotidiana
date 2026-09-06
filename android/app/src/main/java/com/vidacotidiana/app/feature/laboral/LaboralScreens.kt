package com.vidacotidiana.app.feature.laboral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.Commitment
import com.vidacotidiana.app.core.data.Project
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.HeroCard
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Las SIETE secciones del contexto Laboral (ADR-016), con el vocabulario del
 * perfil profesional activo.
 *
 * Todo lo que se ve viene de los endpoints reales: `/people`, `/projects`,
 * `/commitments`, `/notes` y los recordatorios del propio contexto. No queda
 * ninguna lista de maqueta.
 */

private val DAY_MONTH: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))

/** Cómo se lee la fecha de un compromiso: relativa cuando importa. */
private fun dueLabel(date: LocalDate?): String {
    if (date == null) return "Sin fecha"
    val today = LocalDate.now()
    return when {
        date.isBefore(today) -> "Venció el ${date.format(DAY_MONTH)}"
        date == today -> "Hoy"
        date == today.plusDays(1) -> "Mañana"
        else -> "El ${date.format(DAY_MONTH)}"
    }
}

/**
 * Un seguimiento tiene DIRECCIÓN: o lo debo yo, o lo espero de alguien. Esa
 * distinción es la razón de ser de la sección, así que va en la píldora.
 */
private fun Commitment.tone(): Pair<String, PillTone> = when {
    // `CommitmentStatus` es OPEN/DONE y `CommitmentDirection` MINE/THEIRS.
    status == "DONE" -> "Cerrado" to PillTone.OK
    direction == "MINE" -> "Mío" to PillTone.WARN
    else -> "Esperando" to PillTone.QUIET
}

/**
 * El estado de un proyecto es TEXTO LIBRE en el modelo: lo escribe el usuario
 * ("En curso", "En pausa", lo que quiera). Así que se muestra tal cual en vez
 * de traducirlo contra una lista de valores que no existe.
 */
private fun Project.tone(): Pair<String, PillTone>? =
    status.takeIf { it.isNotBlank() }?.let { it to PillTone.NEUTRAL }

@Composable
fun HoyScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val todayContent = viewModel.contentFor(today)
    // Lo que urge de verdad: lo que vence hoy o ya venció.
    val pressing = state.data.commitments
        .filter { it.status != "DONE" }
        .sortedBy { it.dueOn ?: LocalDate.MAX }
    val openTasks = todayContent.tasks.filterNot { it.done }

    VidaScreen(
        title = "Hoy",
        subtitle = "Tu jornada de trabajo.",
        onNavigationClick = openDrawerAction(drawerState, scope),
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
    ) {
        StaggeredAppear(0) {
            val lead = openTasks.firstOrNull()
            val leadCommitment = pressing.firstOrNull()
            when {
                lead != null -> HeroCard(
                    eyebrow = "Lo que urge",
                    title = lead.title,
                    body = lead.meta,
                    primaryAction = null,
                    secondaryAction = null,
                )
                leadCommitment != null -> HeroCard(
                    eyebrow = "Lo que urge",
                    title = leadCommitment.description,
                    body = dueLabel(leadCommitment.dueOn),
                    primaryAction = null,
                    secondaryAction = null,
                )
                else -> HeroCard(
                    eyebrow = "Tu jornada",
                    title = if (state.loading) "Cargando tu día…" else "Nada urgente hoy",
                    body = if (state.loading) "" else "Ninguna tarea ni seguimiento reclama tu atención ahora.",
                    primaryAction = null,
                    secondaryAction = null,
                )
            }
        }

        StaggeredAppear(1) { Eyebrow("Tareas") }
        if (openTasks.isEmpty()) {
            StaggeredAppear(2) {
                Text(
                    "Sin tareas abiertas para hoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VidaTheme.colors.textSecondary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                openTasks.take(4).forEachIndexed { i, task ->
                    StaggeredAppear(2 + i) {
                        ResourceRow(
                            task.title,
                            task.meta,
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            onClick = { viewModel.toggleTask(task.id) },
                        )
                    }
                }
            }
        }

        StaggeredAppear(6) { Eyebrow("Seguimientos") }
        when {
            state.loading && pressing.isEmpty() -> StaggeredAppear(7) { LoadingRows(2) }
            pressing.isEmpty() -> StaggeredAppear(7) {
                Text(
                    "Nada pendiente de nadie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VidaTheme.colors.textSecondary,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                pressing.take(4).forEachIndexed { i, commitment ->
                    StaggeredAppear(7 + i) {
                        ResourceRow(
                            commitment.description,
                            dueLabel(commitment.dueOn),
                            icon = Icons.Outlined.Autorenew,
                            pill = commitment.tone(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    // La semana en curso, día a día, con lo que cada uno contiene de verdad.
    val week = (0..6).map { today.plusDays(it.toLong()) }

    VidaScreen(
        title = "Agenda",
        subtitle = "Tu semana profesional.",
        onNavigationClick = openDrawerAction(drawerState, scope),
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
    ) {
        StaggeredAppear(0) { Eyebrow("Esta semana") }
        val days = week.map { it to viewModel.contentFor(it) }
        val busy = days.filter { (_, content) -> !content.isEmpty }

        when {
            state.loading && busy.isEmpty() -> StaggeredAppear(1) { LoadingRows(3) }
            busy.isEmpty() -> StaggeredAppear(1) {
                EmptyState("Semana despejada", "No hay nada agendado en los próximos siete días.")
            }
            else -> StaggeredAppear(1) {
                VidaCard {
                    busy.forEach { (date, content) ->
                        ResourceRow(
                            title = date.format(DateTimeFormatter.ofPattern("EEEE d", Locale.forLanguageTag("es")))
                                .replaceFirstChar { it.uppercase() },
                            subtitle = content.summary(),
                            icon = Icons.Outlined.Description,
                            onClick = { viewModel.selectDate(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LaboralTasksScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val tasks = viewModel.allTasks()
    val entries = tasks.map {
        ResourceEntry(
            id = it.id,
            title = it.title,
            subtitle = it.meta,
            icon = Icons.AutoMirrored.Outlined.Assignment,
            onEdit = { viewModel.requestEdit(CreatableResource.TASK, it.id) },
            onComplete = if (!it.done) {
                { viewModel.completeResource(CreatableResource.TASK, it.id) }
            } else null,
            completeLabel = "Hecha",
            pill = if (it.done) "Hecha" to PillTone.OK else null,
        )
    }
    ResourceListScreen(
        title = "Tareas",
        subtitle = "Lo que te toca en el trabajo.",
        eyebrow = plural(entries.count { it.pill == null }, "abierta", "abiertas"),
        entries = entries,
        filters = listOf("Todas", "Hoy", "Próximas", "Hechas"),
        addLabel = "Nueva tarea",
        emptyBody = "Crea una tarea para tu jornada.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.TASK) },
        matchesFilter = { entry, f ->
            val task = tasks.firstOrNull { it.id == entry.id }
            when {
                f == "Todas" || task == null -> true
                f == "Hechas" -> task.done
                f == "Hoy" -> !task.done && task.meta == today.toString()
                f == "Próximas" -> !task.done
                else -> true
            }
        },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}

@Composable
fun PeopleScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val people = state.data.people
    val entries = people.map {
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = listOfNotNull(it.role, it.organization).joinToString(" · ").ifBlank { "Sin detalle" },
            icon = Icons.Outlined.Groups,
            // Una persona no se completa: no existe ese estado en su backend.
            onEdit = { viewModel.requestEdit(CreatableResource.PERSON, it.id) },
            onDelete = { viewModel.deleteResource(CreatableResource.PERSON, it.id) },
        )
    }
    // Los filtros salen de los roles que el usuario realmente registró.
    val roles = listOf("Todas") + people.mapNotNull { it.role }.distinct().sorted()

    ResourceListScreen(
        title = state.profile.personPlural,
        subtitle = "Con quién trabajas.",
        eyebrow = plural(entries.size, "registrada", "registradas"),
        entries = entries,
        filters = roles,
        addLabel = "Nueva ${state.profile.person.lowercase()}",
        emptyBody = "Registra con quién trabajas.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.PERSON) },
        matchesFilter = { entry, f -> f == "Todas" || entry.subtitle.startsWith(f) },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}

@Composable
fun ProjectsScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projects = state.data.projects
    // Cuántos seguimientos abiertos cuelgan de cada proyecto: el dato que hace
    // útil la lista, y que ya existe en la relación `Commitment.projectId`.
    val openByProject = state.data.commitments
        .filter { it.status != "DONE" && it.projectId != null }
        .groupingBy { it.projectId!! }
        .eachCount()

    val entries = projects.map {
        val open = openByProject[it.id] ?: 0
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = when {
                open == 0 -> "Sin seguimientos abiertos"
                open == 1 -> "1 seguimiento abierto"
                else -> "$open seguimientos abiertos"
            } + (it.deadline?.let { d -> " · entrega ${d.format(DAY_MONTH)}" } ?: ""),
            icon = Icons.Outlined.Description,
            // El estado de un proyecto es texto libre; no hay «resolver».
            onEdit = { viewModel.requestEdit(CreatableResource.PROJECT, it.id) },
            onDelete = { viewModel.deleteResource(CreatableResource.PROJECT, it.id) },
            pill = it.tone(),
        )
    }
    // Los chips salen de los estados que el usuario realmente escribió, no de
    // una lista fija: `Project.status` es texto libre.
    val statuses = listOf("Todos") + projects.map { it.status }.filter { it.isNotBlank() }.distinct().sorted()

    // Destino prioritario de la barra en Laboral: su cabecera muestra el menú.
    ResourceListScreen(
        title = state.profile.projectPlural,
        subtitle = "En qué estás trabajando.",
        eyebrow = plural(projects.size, "abierto", "abiertos"),
        entries = entries,
        filters = statuses,
        addLabel = "Nuevo ${state.profile.project.lowercase()}",
        emptyBody = "Abre un ${state.profile.project.lowercase()} para agrupar su trabajo.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.PROJECT) },
        drawerState = drawerState,
        scope = scope,
        showBack = false,
        onBack = {},
        onNotifications = {},
    )
}

@Composable
fun CommitmentsScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val commitments = state.data.commitments.sortedBy { it.dueOn ?: LocalDate.MAX }
    val peopleById = state.data.people.associateBy { it.id }

    val entries = commitments.map {
        ResourceEntry(
            id = it.id,
            title = it.description,
            subtitle = listOfNotNull(
                it.personId?.let { id -> peopleById[id]?.name },
                dueLabel(it.dueOn),
            ).joinToString(" · "),
            icon = Icons.Outlined.Autorenew,
            onEdit = { viewModel.requestEdit(CreatableResource.COMMITMENT, it.id) },
            // El backend lo llama `resolve`, no `complete`: un seguimiento ya
            // cerrado no vuelve a ofrecerlo.
            onComplete = if (it.status != "DONE") {
                { viewModel.completeResource(CreatableResource.COMMITMENT, it.id) }
            } else null,
            completeLabel = "Cerrar",
            onDelete = { viewModel.deleteResource(CreatableResource.COMMITMENT, it.id) },
            pill = it.tone(),
        )
    }
    ResourceListScreen(
        title = "Seguimientos",
        subtitle = "Lo que debes y lo que esperas.",
        eyebrow = plural(commitments.count { it.status != "DONE" }, "abierto", "abiertos"),
        entries = entries,
        filters = listOf("Todos", "Mío", "Esperando", "Cerrado"),
        addLabel = "Nuevo seguimiento",
        emptyBody = "Anota un compromiso para no perderle la pista.",
        loading = state.loading,
        error = state.error,
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.COMMITMENT) },
        // La píldora ya dice exactamente esto, así que el chip la mira.
        matchesFilter = { entry, f -> f == "Todos" || entry.pill?.first == f },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        onNotifications = {},
    )
}

@Composable
fun InboxScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // El Inbox es, por definición, lo que aún no se clasificó.
    val unclassified = state.data.inbox.filterNot { it.classified }

    VidaScreen(
        title = "Inbox",
        subtitle = "Lo que aún no clasificas.",
        onNavigationClick = openDrawerAction(drawerState, scope),
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
    ) {
        StaggeredAppear(0) { Eyebrow(plural(unclassified.size, "sin clasificar", "sin clasificar")) }
        when {
            state.loading && unclassified.isEmpty() -> StaggeredAppear(1) { LoadingRows(2) }
            state.error != null && unclassified.isEmpty() -> StaggeredAppear(1) {
                EmptyState(
                    "No pudimos cargar tu Inbox",
                    "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to viewModel::refresh,
                )
            }
            unclassified.isEmpty() -> StaggeredAppear(1) {
                EmptyState("Inbox vacío", "Todo lo que anotaste ya está clasificado.")
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                unclassified.forEachIndexed { i, note ->
                    StaggeredAppear(1 + i) {
                        ResourceRow(
                            note.title,
                            note.description ?: "Sin clasificar",
                            icon = Icons.Outlined.Inbox,
                            onClick = { viewModel.requestEdit(CreatableResource.NOTE, note.id) },
                        )
                    }
                }
            }
        }
    }
}
