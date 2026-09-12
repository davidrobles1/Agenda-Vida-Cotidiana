package com.vidacotidiana.app.feature.laboral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.Commitment
import com.vidacotidiana.app.core.data.Project
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaTile
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError
import com.vidacotidiana.app.core.app.tasksError

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
fun HoyScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
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
        /* ══════════════════════════════════════════════════════════════════
           LA RETÍCULA DE CUATRO PIEZAS, la misma de Inicio
           ══════════════════════════════════════════════════════════════════

           Hoy es la portada de Laboral igual que Inicio lo es de Personal, y
           por tanto abre igual: `1.32fr / 1fr` en dos filas de 120 y 100 dp,
           10 dp de hueco.

           EL HÉROE QUE HABÍA DECÍA UNA SOLA COSA —el título de la primera
           tarea— y ocupaba este mismo espacio. Saber cómo se llama lo primero
           de la lista no ayuda a decidir nada: la lista está justo debajo y lo
           dice mejor. Lo que faltaba era la FORMA de la jornada.

           LAS CUATRO CIFRAS SON DE LOS MISMOS DATOS QUE YA SE PINTABAN:
             · Por hacer  → tareas de hoy sin cerrar
             · Vencidos   → seguimientos abiertos con fecha ya pasada
             · Abiertos   → todos los seguimientos vivos, repartidos por dueño
             · Hechas     → tareas de hoy ya cerradas

           «Vencidos» va en rojo y no en ámbar porque aquí NO es una alerta
           anticipada de ADR-018 —esas avisan ANTES— sino un hecho consumado:
           la fecha ya pasó.
        */
        StaggeredAppear(0) {
            val overdue = pressing.filter { it.dueOn?.isBefore(today) == true }
            val oldestDays = overdue.mapNotNull { it.dueOn }.minOrNull()
                ?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today) } ?: 0L
            val mine = pressing.count { it.direction == "MINE" }
            val doneToday = todayContent.tasks.count { it.done }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Fila 1 — 120 dp. Lo que te toca hoy y lo que se te pasó.
                Row(
                    Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VidaTile(
                        kicker = "Por hacer",
                        figure = openTasks.size.toString(),
                        caption = when {
                            state.loading && openTasks.isEmpty() -> "cargando tu jornada"
                            openTasks.isEmpty() -> "jornada despejada"
                            else -> plural(openTasks.size, "tarea abierta", "tareas abiertas")
                        },
                        background = Brush.linearGradient(listOf(c.primary, c.primaryDeep)),
                        figureColor = c.onPrimary,
                        kickerColor = c.onPrimary.copy(alpha = 0.85f),
                        captionColor = c.onPrimary.copy(alpha = 0.78f),
                        figureSize = 54.dp,
                        chevron = true,
                        modifier = Modifier.weight(1.32f).fillMaxHeight(),
                        onClick = { navController.navigate(Routes.TASKS_LAB) },
                    )
                    VidaTile(
                        kicker = "Vencidos",
                        figure = overdue.size.toString(),
                        caption = when {
                            overdue.isEmpty() -> "ninguno se te pasó"
                            oldestDays <= 1L -> "el más viejo, de ayer"
                            else -> "el más viejo, $oldestDays días"
                        },
                        background = SolidColor(c.errorContainer),
                        figureColor = c.error,
                        kickerColor = c.error,
                        captionColor = c.error.copy(alpha = 0.75f),
                        figureSize = 46.dp,
                        chevron = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { navController.navigate(Routes.COMMITMENTS) },
                    )
                }
                // Fila 2 — 100 dp. De quién depende cada cosa, y lo ya cerrado.
                Row(
                    Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VidaTile(
                        kicker = "Seguimientos",
                        figure = pressing.size.toString(),
                        caption = if (pressing.isEmpty()) {
                            "nada pendiente de nadie"
                        } else {
                            "$mine míos · ${pressing.size - mine} los espero"
                        },
                        background = SolidColor(c.sunken),
                        figureColor = c.text,
                        kickerColor = c.textSecondary,
                        captionColor = c.textTertiary,
                        figureSize = 38.dp,
                        chevron = true,
                        modifier = Modifier.weight(1.32f).fillMaxHeight(),
                        onClick = { navController.navigate(Routes.COMMITMENTS) },
                    )
                    VidaTile(
                        kicker = "Hechas",
                        figure = doneToday.toString(),
                        caption = "hoy",
                        background = SolidColor(c.secondContainer),
                        figureColor = c.second,
                        kickerColor = c.second,
                        captionColor = c.second.copy(alpha = 0.75f),
                        figureSize = 38.dp,
                        chevron = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { navController.navigate(Routes.TASKS_LAB) },
                    )
                }
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
    val c = VidaTheme.colors
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
        val days = week.map { it to viewModel.contentFor(it) }
        val busy = days.filter { (_, content) -> !content.isEmpty }

        // LA FORMA DE LA SEMANA ANTES QUE SU CONTENIDO.
        //
        // «Esta semana» a secas no decía nada que no dijera el título. Lo que
        // se necesita para decidir qué mover es cuánto hay, en cuántos días
        // está repartido y cuántos quedan libres.
        if (busy.isNotEmpty()) {
            StaggeredAppear(0) {
                VidaTileRow(
                    listOf(
                        VidaTileSpec(
                            "Esta semana",
                            busy.sumOf { (_, content) -> content.total }.toString(),
                            "en los próximos 7 días",
                            c.primaryContainer, c.primary, c.primaryDeep,
                            weight = 1.32f,
                        ),
                        VidaTileSpec(
                            "Con algo", busy.size.toString(), "días ocupados",
                            c.sunken, c.text, c.textSecondary,
                        ),
                        VidaTileSpec(
                            "Libres", (week.size - busy.size).toString(), "días despejados",
                            c.successContainer, c.successText, c.successText,
                        ),
                    ),
                )
            }
        }

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
            // LOS DÍAS, EN RETÍCULA DE DOS.
            //
            // Apilados en filas de una tarjeta, siete días son siete renglones
            // casi idénticos que hay que leer uno a uno. En cuadricula la
            // semana se ve de golpe, y la CIFRA de cada día —cuántas cosas
            // tiene— permite comparar dos días sin leer sus resúmenes.
            //
            // Tocar un día sigue haciendo exactamente lo que hacía: seleccionarlo.
            else -> StaggeredAppear(1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    busy.chunked(2).forEach { pair ->
                        Row(
                            Modifier.fillMaxWidth().height(104.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { (date, content) ->
                                val isToday = date == today
                                VidaTile(
                                    kicker = date
                                        .format(DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("es")))
                                        .replaceFirstChar { it.uppercase() },
                                    figure = content.total.toString(),
                                    caption = content.summary(),
                                    background = SolidColor(
                                        when {
                                            isToday -> c.primaryContainer
                                            content.hasUrgent -> c.warningContainer
                                            else -> c.sunken
                                        },
                                    ),
                                    figureColor = when {
                                        isToday -> c.primary
                                        content.hasUrgent -> c.warningText
                                        else -> c.text
                                    },
                                    kickerColor = when {
                                        isToday -> c.primaryDeep
                                        content.hasUrgent -> c.warningText
                                        else -> c.textSecondary
                                    },
                                    captionColor = if (isToday) c.primaryDeep.copy(alpha = 0.75f) else c.textTertiary,
                                    figureSize = 34.dp,
                                    chevron = true,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = { viewModel.selectDate(date) },
                                )
                            }
                            // La última fila impar conserva la anchura de pieza
                            // en vez de estirar la que queda a lo ancho.
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
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
        error = state.tasksError,
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
            subtitle = listOfNotNull(it.role, it.organization)
                .joinToString(" · ").ifBlank { "Sin detalle" },
            // LA CIFRA QUE CONVIERTE LA LISTA EN RETÍCULA.
            //
            // Estaba dicha dentro del subtítulo, donde era una coletilla más
            // detrás del rol y la organización. Como `highlight` pasa a ser el
            // foco de la pieza, y `shapeOf` reparte a estas personas en
            // cuadricula en vez de en renglones.
            //
            // Nula cuando no participa en ninguno: escribir «0 proyectos» en
            // grande en la tarjeta de alguien es afear un dato que no importa.
            // Esa pieza muestra el nombre como foco, que es lo que queda.
            highlight = when {
                inProjects == 0 -> null
                inProjects == 1 -> "1 ${state.profile.project.lowercase()}"
                else -> "$inProjects ${state.profile.projectPlural.lowercase()}"
            },
            icon = Icons.Outlined.Groups,
            group = it.role?.ifBlank { null } ?: "Sin rol",
            // Una persona no se completa: no existe ese estado en su backend.
            onEdit = { viewModel.requestEdit(CreatableResource.PERSON, it.id) },
            onDelete = { viewModel.deleteResource(CreatableResource.PERSON, it.id) },
        )
    }
    // Los filtros salen de los roles que el usuario realmente registró.
    val roles = listOf("Todas") + people.mapNotNull { it.role }.distinct().sorted()

    val c = VidaTheme.colors
    val openWith = state.data.commitments
        .filter { it.status != "DONE" && it.personId != null }
        .map { it.personId }
        .distinct()
        .size

    ResourceListScreen(
        // El artefacto le da pantalla propia a la persona.
        onOpenRoute = { navController.navigate(Routes.personRoute(it.id)) },
        title = state.profile.personPlural,
        subtitle = "Con quién trabajas.",
        eyebrow = plural(entries.size, "registrada", "registradas"),
        // La retícula del artefacto: cuánta gente hay, con cuánta compartes
        // trabajo y con cuánta tienes algo abierto. La tercera es la que hace
        // actuar, y por eso lleva ámbar (ADR-018: el rojo es para errores).
        header = {
            VidaTileRow(
                listOf(
                    VidaTileSpec(
                        state.profile.personPlural, people.size.toString(), "registradas",
                        c.primaryContainer, c.primary, c.primaryDeep,
                        weight = 1.32f,
                    ),
                    VidaTileSpec(
                        "Contigo", projectCount.values.count { it > 0 }.toString(),
                        "en ${state.profile.projectPlural.lowercase()}",
                        c.sunken, c.text, c.textSecondary,
                    ),
                    VidaTileSpec(
                        "Abiertos", openWith.toString(), "con seguimiento",
                        if (openWith == 0) c.successContainer else c.warningContainer,
                        if (openWith == 0) c.successText else c.warningText,
                        if (openWith == 0) c.successText else c.warningText,
                    ),
                ),
            )
        },
        entries = entries,
        filters = roles,
        addLabel = "Nueva ${state.profile.person.lowercase()}",
        emptyBody = "Registra con quién trabajas.",
        loading = state.loading,
        error = state.sliceError(DataSlice.PEOPLE),
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
                    involved == 0 -> null
                    involved == 1 -> "1 persona"
                    else -> "$involved personas"
                },
                it.deadline?.let { d -> "entrega ${d.format(DAY_MONTH)}" },
            ).joinToString(" · ").ifBlank { "Sin gente ni entrega" },
            // Lo abierto pasa de coletilla del subtítulo a FOCO de la pieza, y
            // con ello la lista se reparte en cuadricula. Sin nada abierto no
            // se escribe «0»: el foco vuelve a ser el nombre del proyecto.
            highlight = if (open == 0) null else plural(open, "abierto", "abiertos"),
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
        // El artefacto le da pantalla propia al proyecto.
        onOpenRoute = { navController.navigate(Routes.projectRoute(it.id)) },
        title = state.profile.projectPlural,
        subtitle = "En qué estás trabajando.",
        eyebrow = plural(projects.size, "abierto", "abiertos"),
        // Cuántos hay, cuánto trabajo vivo cuelga de ellos y cuántos tienen
        // fecha comprometida: las tres preguntas que se hacen antes de abrir
        // uno concreto.
        header = {
            val c = VidaTheme.colors
            val withDeadline = projects.count { it.deadline != null }
            VidaTileRow(
                listOf(
                    VidaTileSpec(
                        state.profile.projectPlural, projects.size.toString(), "en marcha",
                        c.primaryContainer, c.primary, c.primaryDeep,
                        weight = 1.32f,
                    ),
                    VidaTileSpec(
                        "Abiertos", openByProject.values.sum().toString(), "seguimientos",
                        c.sunken, c.text, c.textSecondary,
                    ),
                    VidaTileSpec(
                        "Con fecha", withDeadline.toString(), "de entrega",
                        c.secondContainer, c.second, c.second,
                    ),
                ),
            )
        },
        entries = entries,
        filters = statuses,
        addLabel = "Nuevo ${state.profile.project.lowercase()}",
        emptyBody = "Abre un ${state.profile.project.lowercase()} para agrupar su trabajo.",
        loading = state.loading,
        error = state.sliceError(DataSlice.PROJECTS),
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
        // LA DIRECCIÓN, EN CIFRAS.
        //
        // Es la razón de ser de la sección —lo que debo yo frente a lo que
        // espero de otro— y hasta ahora solo se veía leyendo las píldoras una
        // por una. La pieza grande es «me toca» porque es la única de las tres
        // sobre la que el usuario puede actuar hoy.
        //
        // «Vencidos» aparte, y en rojo, por la misma razón que en Hoy: no es
        // un aviso anticipado de ADR-018, es una fecha que ya pasó.
        header = {
            val c = VidaTheme.colors
            val open = commitments.filter { it.status != "DONE" }
            val mine = open.count { it.direction == "MINE" }
            val overdue = open.count { it.dueOn?.isBefore(LocalDate.now()) == true }
            VidaTileRow(
                listOf(
                    VidaTileSpec(
                        "Me toca", mine.toString(), "los debo yo",
                        c.primaryContainer, c.primary, c.primaryDeep,
                        weight = 1.32f,
                    ),
                    VidaTileSpec(
                        "Espero", (open.size - mine).toString(), "de alguien",
                        c.sunken, c.text, c.textSecondary,
                    ),
                    VidaTileSpec(
                        "Vencidos", overdue.toString(), "ya pasaron",
                        if (overdue == 0) c.successContainer else c.errorContainer,
                        if (overdue == 0) c.successText else c.error,
                        if (overdue == 0) c.successText else c.error,
                    ),
                ),
            )
        },
        entries = entries,
        filters = listOf("Todos", "Mío", "Esperando", "Cerrado"),
        addLabel = "Nuevo seguimiento",
        emptyBody = "Anota un compromiso para no perderle la pista.",
        loading = state.loading,
        error = state.sliceError(DataSlice.COMMITMENTS),
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
        // DOS PIEZAS, NO TRES: el Inbox solo tiene dos estados posibles, y una
        // tercera cifra («total») sería la suma de las dos que ya están a la
        // vista. La primera pesa más porque es la que pide trabajo.
        if (state.data.inbox.isNotEmpty()) {
            StaggeredAppear(0) {
                val c = VidaTheme.colors
                val classified = state.data.inbox.size - unclassified.size
                VidaTileRow(
                    listOf(
                        VidaTileSpec(
                            "Sin clasificar", unclassified.size.toString(),
                            if (unclassified.isEmpty()) "inbox al día" else "esperan sitio",
                            if (unclassified.isEmpty()) c.successContainer else c.primaryContainer,
                            if (unclassified.isEmpty()) c.successText else c.primary,
                            if (unclassified.isEmpty()) c.successText else c.primaryDeep,
                            weight = 1.32f,
                        ),
                        VidaTileSpec(
                            "Colocadas", classified.toString(), "ya tienen sitio",
                            c.sunken, c.text, c.textSecondary,
                        ),
                    ),
                )
            }
        }
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
