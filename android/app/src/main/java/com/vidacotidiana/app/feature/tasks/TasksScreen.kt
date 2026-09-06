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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceRow
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
        actions = { VidaIconButton(Icons.Filled.Add, "Nueva tarea") {} },
    ) {
        if (state.error != null) {
            StaggeredAppear(0) {
                Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = c.error)
            }
        } else if (groups.isEmpty() && !state.loading) {
            StaggeredAppear(0) {
                EmptyState("Sin tareas", "Cuando crees una, aparecerá aquí agrupada por cuándo toca.")
            }
        }
        var index = 0
        groups.forEach { (label, tasks) ->
            StaggeredAppear(index++) { Eyebrow("$label · ${tasks.size}") }
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                tasks.forEach { task ->
                    StaggeredAppear(index++) {
                        SwipeToCompleteRow(done = task.done, onToggle = { viewModel.toggleTask(task.id) }) {
                            ResourceRow(
                                title = task.title,
                                subtitle = listOfNotNull(task.time?.toString()?.take(5), task.meta).joinToString(" · "),
                                icon = if (task.done) Icons.Filled.Check else Icons.AutoMirrored.Outlined.Assignment,
                                markBackground = if (task.done) c.successContainer else c.primaryContainer,
                                markTint = if (task.done) c.successText else c.primary,
                                tone = if (task.done) c.successText else c.primary,
                                strikeThrough = task.done,
                                pill = if (task.shared) "Te toca" to PillTone.WARN else null,
                                // Deslizar ya completaba; faltaba poder editar.
                                // Tocar abre el MISMO formulario de la tarea que
                                // usa el calendario y el «+».
                                onClick = { viewModel.requestEdit(CreatableResource.TASK, task.id) },
                            )
                        }
                    }
                }
            }
        }
        StaggeredAppear(index) {
            Text(
                "Desliza una tarea hacia la izquierda para marcarla.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}
