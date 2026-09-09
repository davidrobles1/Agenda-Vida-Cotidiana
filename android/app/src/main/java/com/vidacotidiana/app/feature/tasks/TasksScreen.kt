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
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate

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
        "Hoy" to all.filter { !it.done && it.meta == today.toString() },
        "Próximas" to all.filter { !it.done && it.meta != today.toString() },
        "Hechas" to all.filter { it.done },
    ).filterValues { it.isNotEmpty() }

    VidaScreen(
        title = "Tareas",
        subtitle = "Lo que te toca, agrupado por cuándo.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
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
        var index = 0
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
                            tone = if (task.done) c.successText else c.primary,
                            pill = if (task.shared) "Te toca" to PillTone.WARN else null,
                            onEdit = { viewModel.requestEdit(CreatableResource.TASK, task.id) },
                            onComplete = if (!task.done) {
                                { viewModel.toggleTask(task.id) }
                            } else null,
                            completeLabel = "Hecha",
                            // Solo cuando hay ubicación de verdad. La acción va
                            // en la hoja de detalle: la tarjeta conserva «Hecha»
                            // como único gesto rápido.
                            extraActions = task.location?.let { place ->
                                listOf("Cómo llegar" to { viewModel.openDirections(context, place) })
                            } ?: emptyList(),
                        )
                    },
                    onOpenDetail = { viewModel.requestEdit(CreatableResource.TASK, it.id) },
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
