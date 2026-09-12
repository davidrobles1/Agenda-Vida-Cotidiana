package com.vidacotidiana.app.feature.attention

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.loadFailed
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.ui.components.AttentionList
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaScreen
import java.time.LocalDate

/**
 * EL DESTINO DE UN CONTADOR TRANSVERSAL.
 *
 * Existe por una regla concreta: un contador tiene que llevar al mismo
 * conjunto que cuenta. «Atrasado 3» agrupaba una tarea, un pago y un
 * mantenimiento, y abría Tareas —donde sólo estaba uno de los tres—, así que
 * el usuario tocaba un tres y aterrizaba en un uno.
 *
 * No es una sección nueva: lee el MISMO `AttentionEngine` que Inicio y pinta la
 * MISMA `AttentionList`. Lo único que añade es el filtro por urgencia, que es
 * justo lo que distingue un mosaico del otro.
 */
@Composable
fun AtencionScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    urgency: AttentionUrgency,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val items = AttentionEngine.of(
        AttentionEngine.scan(state.data, viewModel.allTasks(), today),
        urgency,
    )

    VidaScreen(
        title = when (urgency) {
            AttentionUrgency.OVERDUE -> "Atrasado"
            AttentionUrgency.TODAY -> "Para hoy"
            AttentionUrgency.SOON -> "Esta semana"
        },
        subtitle = when (urgency) {
            AttentionUrgency.OVERDUE -> "Lo que se pasó de fecha, de todas tus secciones."
            AttentionUrgency.TODAY -> "Lo que te toca hoy, de todas tus secciones."
            AttentionUrgency.SOON -> "Lo que viene en los próximos siete días."
        },
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        when {
            // Mientras carga no se afirma que no hay nada, y si la consulta
            // falló tampoco: un vacío aquí diría que el usuario está al día
            // (ADR-021 k).
            state.loading && items.isEmpty() -> StaggeredAppear(0) { LoadingRows(3) }
            state.loadFailed && items.isEmpty() -> StaggeredAppear(0) {
                EmptyState(
                    "No pudimos comprobarlo",
                    "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to viewModel::refresh,
                )
            }
            items.isEmpty() -> StaggeredAppear(0) {
                EmptyState(
                    title = when (urgency) {
                        AttentionUrgency.OVERDUE -> "No tienes nada atrasado"
                        AttentionUrgency.TODAY -> "Hoy no te reclama nada"
                        AttentionUrgency.SOON -> "La semana viene despejada"
                    },
                    body = "Cuando algo se acerque a su fecha, aparecerá aquí.",
                )
            }
            else -> {
                StaggeredAppear(0) {
                    com.vidacotidiana.app.core.ui.components.Eyebrow(
                        AttentionEngine.breakdown(items),
                    )
                }
                StaggeredAppear(1) {
                    AttentionList(items) { route -> navController.navigate(route) }
                }
            }
        }
    }
}
