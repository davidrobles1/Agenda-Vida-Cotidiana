package com.vidacotidiana.app.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.SwipeToCompleteRow
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import com.vidacotidiana.app.core.ui.VidaDates
import com.vidacotidiana.app.core.ui.TemporalState
import com.vidacotidiana.app.core.ui.components.ResourceShape

/**
 * Tareas, agrupadas por CUÁNDO y no en una lista plana: Hoy, Próximas y
 * Hechas. Cada fila se desliza a la izquierda para completarse — gesto real
 * sobre la acción real, que llama al mismo endpoint que el resto de la app.
 */
@Composable
fun TasksScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val context = LocalContext.current
    val today = LocalDate.now()
    val all = viewModel.allTasks()

    val groups = linkedMapOf(
        // Se agrupa por la FECHA, no por el texto que se enseña.
        //
        // Antes se comparaba `meta == today.toString()`, es decir el mismo
        // campo servia de etiqueta visible y de clave de agrupacion — y por eso
        // nadie podia darle formato: escribir «Hace 2 dias» habria roto en
        // silencio el reparto entre «Hoy» y «Proximas». `DayTask.date` ya
        // existe y es el dato; `meta` vuelve a ser solo texto.
        // PASADO · HOY · FUTURO, que son los tres estados temporales reales.
        //
        // Antes solo habia dos grupos —«hoy» y «no hoy»—, asi que una tarea
        // vencida hace dos dias aparecia bajo «Proximas». El formato en crudo
        // lo disimulaba; en cuanto la fecha se escribe como se lee, la
        // contradiccion queda a la vista.
        "Atrasadas" to all.filter {
            !it.done && it.date != null && VidaDates.stateOf(it.date, today) == TemporalState.PASADO
        },
        "Hoy" to all.filter { !it.done && it.date == today },
        "Próximas" to all.filter {
            !it.done && (it.date == null || VidaDates.stateOf(it.date, today) == TemporalState.FUTURO)
        },
        "Hechas" to all.filter { it.done },
    ).filterValues { it.isNotEmpty() }

    VidaScreen(
        title = "Tareas",
        subtitle = "Lo que te toca, agrupado por cuándo.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        onRefresh = viewModel::refresh,
        refreshing = state.loading,
        // UNA SOLA SEMÁNTICA DE CREACIÓN. El «+» de la barra llevaba una lambda
        // vacía: prometía crear una tarea y no hacía nada. Ahora dispara la
        // misma intención que el botón del estado vacío, así que las dos
        // entradas al alta son literalmente la misma llamada.
        actions = {
            VidaIconButton(Icons.Filled.Add, "Nueva tarea") {
                viewModel.requestCreate(CreatableResource.TASK)
            }
        },
    ) {
        // LA RETÍCULA DE CIFRAS, la misma que Inventario, Garantías y
        // Documentos. Tareas era la única sección grande que empezaba
        // directamente en la lista, y por eso se leía más pobre que las demás
        // teniendo más información detrás.
        //
        // Las tres piezas responden preguntas distintas —cuánto aprieta,
        // cuánto queda, cuánto llevas— en vez de tres recuentos del mismo
        // conjunto. Y salen de `all`, que es la misma lista que se agrupa
        // debajo: la cifra y el grupo no pueden discrepar.
        //
        // No se pintan sobre un error de carga: afirmar «0 atrasadas» cuando
        // no sabemos qué hay es exactamente la mentira que prohíbe ADR-021(k).
        if (state.error == null && all.isNotEmpty()) {
            val atrasadas = all.count {
                !it.done && it.date != null && VidaDates.stateOf(it.date, today) == TemporalState.PASADO
            }
            val pendientes = all.count { !it.done }
            val hechas = all.count { it.done }
            StaggeredAppear(0) {
                VidaTileRow(
                    listOf(
                        // La pieza grande es la que aprieta. En rojo solo
                        // cuando hay algo atrasado: un cero en rojo permanente
                        // convertiría la alarma en decoración.
                        VidaTileSpec(
                            "Atrasadas", atrasadas.toString(),
                            if (atrasadas == 1) "te espera" else "te esperan",
                            if (atrasadas > 0) c.errorContainer else c.sunken,
                            if (atrasadas > 0) c.error else c.text,
                            if (atrasadas > 0) c.error else c.textSecondary,
                            weight = 1.32f,
                        ),
                        VidaTileSpec(
                            "Pendientes", pendientes.toString(), "por hacer",
                            c.primaryContainer, c.primary, c.primaryDeep,
                        ),
                        VidaTileSpec(
                            "Hechas", hechas.toString(), "cerradas",
                            c.successContainer, c.successText, c.successText,
                        ),
                    ),
                )
            }
        }

        if (state.error != null) {
            // ERROR, no vacío: no se afirma nada sobre las tareas del usuario
            // —no las conocemos— y se conserva la salida. Antes era una línea
            // de texto roja sin forma de reintentar.
            StaggeredAppear(0) {
                EmptyState(
                    title = "No pudimos cargar tus tareas",
                    body = "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to viewModel::refresh,
                )
            }
        } else if (groups.isEmpty() && !state.loading) {
            // FIRST_USE: la sección existe pero nunca se ha usado. El cuerpo
            // dice qué gana el usuario —el agrupado por cuándo toca, que es lo
            // que esta pantalla hace y no se ve estando vacía— y la acción es
            // la que ya existe arriba.
            StaggeredAppear(0) {
                EmptyState(
                    title = "Aún no hay tareas",
                    body = "Anota lo que tienes que hacer y aparecerá agrupado por cuándo toca.",
                    action = "Nueva tarea" to { viewModel.requestCreate(CreatableResource.TASK) },
                )
            }
        }
        // Empieza en 1 porque la retícula de cifras ya ocupó el 0: si no, el
        // primer encabezado entraría a la vez que ella.
        var index = 1
        groups.forEach { (label, tasks) ->
            StaggeredAppear(index++) { Eyebrow("$label · ${tasks.size}") }
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                // Cuadrícula, no lista: cada tarea es una pieza. El
                // deslizar-para-completar deja de tener sentido en una tarjeta
                // cuadrada, así que la acción pasa al check de la propia pieza.
                ResourceBoard(
                    entries = tasks.map { task ->
                        ResourceEntry(
                            id = task.id,
                            title = task.title,
                            // La ubicación entra en el subtítulo que ya existe,
                            // no en una línea propia: es otro dato de "cuándo y
                            // dónde", no una sección aparte.
                            subtitle = listOfNotNull(task.meta, task.location).joinToString(" · "),
                            highlight = task.time?.toString()?.take(5),
                            icon = if (task.done) Icons.Filled.Check else Icons.AutoMirrored.Outlined.Assignment,
                            // EL ANTETÍTULO Y EL TONO SALEN DE LA PRIORIDAD.
                            //
                            // El artefacto rotula cada tarea con su prioridad
                            // —ALTA, MEDIA, BAJA— y le da su color, que es lo
                            // que permite ojear la lista sin leerla. El campo
                            // existe en el backend desde V33; el cliente ni
                            // siquiera lo declaraba, así que todas las tareas
                            // salían del mismo color.
                            typeTag = priorityLabel(task.priority),
                            // EL ANILLO DEL ARTEFACTO, con datos reales: el
                            // porcentaje sale de los pasos de la tarea, que el
                            // backend ahora resuelve en una sola agregación por
                            // página. Sin pasos no hay anillo — no se inventa
                            // un cero que parecería «empezada y sin avanzar».
                            // Hecha: palomilla verde, como en el artefacto.
                            // El anillo dice «cuánto llevas»; una tarea cerrada
                            // ya no lleva nada, ha terminado.
                            ring = task.percent?.takeIf { !task.done },
                            busy = task.id in state.busy,
                            tone = when {
                                task.done -> c.successText
                                task.priority == "URGENT" -> c.error
                                task.priority == "LOW" -> c.success
                                else -> c.primary
                            },
                            pill = if (task.shared) "Te toca" to PillTone.WARN else null,
                            onEdit = { viewModel.requestEdit(CreatableResource.TASK, task.id) },
                            onComplete = if (!task.done) {
                                { viewModel.toggleTask(task.id) }
                            } else null,
                            completeLabel = "Hecha",
                            // La vuelta atrás. El endpoint SIEMPRE fue un
                            // toggle —`POST /reminders/{id}/complete` alterna
                            // PENDING↔COMPLETED—; lo que no había era forma de
                            // pedirla, así que marcar una tarea por error no
                            // tenía más salida que borrarla.
                            onRevert = if (task.done) {
                                { viewModel.revertResource(CreatableResource.TASK, task.id) }
                            } else null,
                            revertLabel = "Volver a pendiente",
                            // Solo cuando hay ubicación de verdad. La acción va
                            // en la hoja de detalle: la tarjeta conserva «Hecha»
                            // como único gesto rápido.
                            extraActions = task.location?.let { place ->
                                listOf("Cómo llegar" to { viewModel.openDirections(context, place) })
                            } ?: emptyList(),
                        )
                    },
                    // Abrir una tarea lleva a SU pantalla, no al formulario
                    // de edición: el artefacto separa mirar de editar, y los
                    // pasos solo caben en el detalle.
                    onOpenDetail = { navController.navigate(Routes.taskRoute(it.id)) },
                    // FILA, no mosaico: en el artefacto la tarea es una fila con
                    // su antetítulo, su hora al lado y el NOMBRE como dato
                    // principal. Deducir la forma del contenido ascendía la hora
                    // a titular y hundía el nombre debajo.
                    shape = ResourceShape.LIST,
                )
            }
        }
        // La pista explica cómo se maneja UNA LISTA. Sin tareas no hay nada que
        // tocar, y aparecía justo debajo del estado vacío contradiciéndolo.
        if (groups.isNotEmpty()) StaggeredAppear(index) {
            Text(
                "Toca una tarea para editarla, o su marca para darla por hecha.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

/** «ALTA» · «MEDIA» · «BAJA» — la prioridad tal como la rotula el artefacto. */
private fun priorityLabel(priority: String?): String? = when (priority?.uppercase()) {
    "URGENT" -> "ALTA"
    "LOW" -> "BAJA"
    "NORMAL" -> "MEDIA"
    else -> null
}
