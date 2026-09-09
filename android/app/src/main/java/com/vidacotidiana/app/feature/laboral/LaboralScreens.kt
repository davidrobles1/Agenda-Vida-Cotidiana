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
import androidx.compose.ui.platform.LocalContext
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
import com.vidacotidiana.app.core.ui.components.ResourceBoard
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
import com.vidacotidiana.app.core.ui.VidaVocabulary

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
    status?.takeIf { it.isNotBlank() }?.let { VidaVocabulary.human(it) to PillTone.NEUTRAL }

@Composable
fun HoyScreen(viewModel: AppViewModel, drawerState: DrawerState, scope: CoroutineScope) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            StaggeredAppear(2) {
                ResourceBoard(
                    entries = openTasks.take(6).map { task ->
                        ResourceEntry(
                            id = task.id,
                            title = task.title,
                            subtitle = listOfNotNull(task.meta, task.location).joinToString(" · "),
                            highlight = task.time?.toString()?.take(5),
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            onComplete = { viewModel.toggleTask(task.id) },
                            completeLabel = "Hecha",
                            extraActions = task.location?.let { place ->
                                listOf("Cómo llegar" to { viewModel.openDirections(context, place) })
                            } ?: emptyList(),
                        )
                    },
                    onOpenDetail = { entry -> viewModel.requestEdit(CreatableResource.TASK, entry.id) },
                )
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
            else -> StaggeredAppear(7) {
                ResourceBoard(
                    entries = pressing.take(6).map { commitment ->
                        ResourceEntry(
                            id = commitment.id,
                            title = commitment.description,
                            subtitle = commitment.personId?.let { id ->
                                state.data.people.firstOrNull { p -> p.id == id }?.name
                            } ?: "Sin persona",
                            highlight = dueLabel(commitment.dueOn),
                            icon = Icons.Outlined.Autorenew,
                            pill = commitment.tone(),
                            onComplete = { viewModel.completeResource(CreatableResource.COMMITMENT, commitment.id) },
                            completeLabel = "Cerrar",
                        )
                    },
                    onOpenDetail = { entry -> viewModel.requestEdit(CreatableResource.COMMITMENT, entry.id) },
                )
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
            // EMPTY, y en tono de invitación, no de reproche: una semana sin
            // nada agendado no es un incumplimiento. El título afirma el hecho
            // en positivo y la acción se ofrece como opción («si quieres»), no
            // como corrección. La agenda vacía es justo el momento en que uno
            // planifica, así que la acción pertenece aquí.
            busy.isEmpty() -> StaggeredAppear(1) {
                EmptyState(
                    title = "Semana despejada",
                    body = "No tienes nada agendado en los próximos siete días. Buen momento para planificar, si quieres.",
                    action = "Nueva tarea" to { viewModel.requestCreate(CreatableResource.TASK) },
                )
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
    val context = LocalContext.current
    val entries = tasks.map {
        ResourceEntry(
            id = it.id,
            title = it.title,
            // FR-024: la ubicación acompaña a la fecha en el subtítulo, igual
            // que en Tareas de Personal. Una sola forma de mostrarla.
            subtitle = listOfNotNull(it.meta, it.location).joinToString(" · "),
            highlight = it.time?.toString()?.take(5),
            icon = Icons.AutoMirrored.Outlined.Assignment,
            group = if (it.done) "Hechas" else "Abiertas",
            onEdit = { viewModel.requestEdit(CreatableResource.TASK, it.id) },
            onComplete = if (!it.done) {
                { viewModel.completeResource(CreatableResource.TASK, it.id) }
            } else null,
            completeLabel = "Hecha",
            pill = if (it.done) "Hecha" to PillTone.OK else null,
            extraActions = it.location?.let { place ->
                listOf("Cómo llegar" to { viewModel.openDirections(context, place) })
            } ?: emptyList(),
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
    // V32: en cuántos proyectos está cada persona. Se suman las dos formas de
    // estar en uno —participante y cliente—, porque viven en sitios distintos
    // y para quien lee la tarjeta son lo mismo.
    val projectCount = people.associate { person ->
        person.id to (
            state.data.participations.count { it.personId == person.id } +
                state.data.projects.count { it.clientPersonId == person.id }
            )
    }
    val entries = people.map {
        val inProjects = projectCount[it.id] ?: 0
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = listOfNotNull(
                it.role,
                it.organization,
                when {
                    inProjects == 0 -> null
                    inProjects == 1 -> "en 1 ${state.profile.project.lowercase()}"
                    else -> "en $inProjects ${state.profile.projectPlural.lowercase()}"
                },
            ).joinToString(" · ").ifBlank { "Sin detalle" },
            icon = Icons.Outlined.Groups,
            group = it.role?.ifBlank { null } ?: "Sin rol",
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

    // V32: cuánta gente hay en cada proyecto — participantes más el cliente,
    // que sigue viviendo en el propio proyecto y no en la lista.
    val peopleByProject = state.data.participations.groupingBy { it.projectId }.eachCount()

    val entries = projects.map {
        val open = openByProject[it.id] ?: 0
        val involved = (peopleByProject[it.id] ?: 0) + (if (it.clientPersonId != null) 1 else 0)
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = listOfNotNull(
                when {
                    open == 0 -> "Sin seguimientos abiertos"
                    open == 1 -> "1 seguimiento abierto"
                    else -> "$open seguimientos abiertos"
                },
                when {
                    involved == 0 -> null
                    involved == 1 -> "1 persona"
                    else -> "$involved personas"
                },
                it.deadline?.let { d -> "entrega ${d.format(DAY_MONTH)}" },
            ).joinToString(" · "),
            icon = Icons.Outlined.Description,
            group = it.status?.ifBlank { null }?.let(VidaVocabulary::human) ?: "Sin estado",
            // El estado de un proyecto es texto libre; no hay «resolver».
            onEdit = { viewModel.requestEdit(CreatableResource.PROJECT, it.id) },
            onDelete = { viewModel.deleteResource(CreatableResource.PROJECT, it.id) },
            pill = it.tone(),
        )
    }
    // Los chips salen de los estados que el usuario realmente escribió, no de
    // una lista fija: `Project.status` es texto libre.
    val statuses = listOf("Todos") + projects.mapNotNull { it.status?.ifBlank { null }?.let(VidaVocabulary::human) }.distinct().sorted()

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
            subtitle = it.personId?.let { id -> peopleById[id]?.name } ?: "Sin persona",
            highlight = dueLabel(it.dueOn),
            icon = Icons.Outlined.Autorenew,
            // Lo que separa un seguimiento de otro no es su fecha sino DE QUIÉN
            // depende: es la razón de ser de la sección.
            group = when {
                it.status == "DONE" -> "Cerrados"
                it.direction == "MINE" -> "Me toca a mí"
                else -> "Lo espero"
            },
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
            // DOS VACÍOS OPUESTOS QUE DECÍAN LO MISMO.
            //
            // Con el Inbox entero vacío es FIRST_USE: nunca se anotó nada, y
            // «todo lo que anotaste ya está clasificado» afirmaba un hecho
            // falso sobre alguien que no había anotado nunca.
            //
            // Con notas ya clasificadas es COMPLETED, y va SIN acción: haber
            // terminado no es un problema que ofrecerse a resolver.
            unclassified.isEmpty() && state.data.inbox.isEmpty() -> StaggeredAppear(1) {
                EmptyState(
                    title = "Aún no has anotado nada",
                    body = "El Inbox es para apuntar rápido lo que todavía no sabes dónde va. Ya lo clasificarás.",
                    action = "Nueva nota" to { viewModel.requestCreate(CreatableResource.NOTE) },
                )
            }
            unclassified.isEmpty() -> StaggeredAppear(1) {
                EmptyState(
                    title = "Inbox al día",
                    body = "No queda nada por clasificar.",
                )
            }
            else -> StaggeredAppear(1) {
                // Muro de notas, no lista: una nota suelta es un objeto, y en
                // cuadricula se ven de golpe todas las que hay sin clasificar.
                ResourceBoard(
                    entries = unclassified.map { note ->
                        ResourceEntry(
                            id = note.id,
                            title = note.title,
                            subtitle = note.description ?: "Sin clasificar",
                            icon = Icons.Outlined.Inbox,
                            onEdit = { viewModel.requestEdit(CreatableResource.NOTE, note.id) },
                        )
                    },
                    onOpenDetail = { entry -> viewModel.requestEdit(CreatableResource.NOTE, entry.id) },
                )
            }
        }
    }
}
