package com.vidacotidiana.app.feature.objectives

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.Objective
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError

/**
 * Objetivos (FR-031). Sección secundaria de Laboral: se llega desde el cajón,
 * con vuelta atrás.
 *
 * REGLA QUE ESTA PANTALLA NO PUEDE ROMPER (AC-018): `completed` y
 * `currentValue` son independientes. Llegar a la meta NO cumple el objetivo y
 * cumplirlo NO toca el progreso. Aquí no hay ni una comparación entre los dos
 * que decida nada: el progreso se pinta, el estado se lee, y nadie deriva uno
 * del otro.
 *
 * Un objetivo no lleva persona ni proyecto (FR-031 lo deja fuera de alcance) ni
 * módulo: su backend no conoce `ModuleContext`.
 */
@Composable
fun ObjectivesScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Lo pendiente primero, y dentro de eso lo que vence antes. Un objetivo sin
    // fecha no es urgente, así que va al final de los suyos.
    val objectives = state.data.objectives.sortedWith(
        compareBy({ it.completed }, { it.deadline ?: LocalDate.MAX }),
    )

    val entries = objectives.map { objective ->
        ResourceEntry(
            id = objective.id,
            title = objective.title,
            subtitle = objective.subtitle(),
            highlight = objective.highlight(),
            icon = Icons.Outlined.Flag,
            group = if (objective.completed) "Cumplidos" else "En curso",
            // La píldora es lo que mira el filtro; solo la lleva lo cumplido,
            // que es el único estado que un objetivo tiene.
            pill = if (objective.completed) "Cumplido" to PillTone.OK else null,
            onEdit = { viewModel.requestEdit(CreatableResource.OBJECTIVE, objective.id) },
            // El check DESAPARECE al cumplirse: ofrecer "cumplir" sobre algo ya
            // cumplido sería un botón que no dice la verdad. Reabrir vive en la
            // hoja de detalle, que es donde van las acciones secundarias — la
            // tarjeta nunca tiene dos acciones rápidas.
            onComplete = if (objective.completed) {
                null
            } else {
                { viewModel.completeResource(CreatableResource.OBJECTIVE, objective.id) }
            },
            completeLabel = "Cumplido",
            extraActions = if (objective.completed) {
                listOf(
                    "Volver a ponerlo en curso" to {
                        viewModel.completeResource(CreatableResource.OBJECTIVE, objective.id)
                    },
                )
            } else {
                emptyList()
            },
            onDelete = { viewModel.deleteResource(CreatableResource.OBJECTIVE, objective.id) },
        )
    }

    ResourceListScreen(
        title = "Objetivos",
        subtitle = "Lo que quieres lograr, y por dónde vas.",
        eyebrow = plural(objectives.count { !it.completed }, "en curso", "en curso"),
        entries = entries,
        filters = listOf("Todos", "En curso", "Cumplidos"),
        addLabel = "Nuevo objetivo",
        emptyBody = "Anota lo que quieres lograr y podrás seguir su avance.",
        loading = state.loading,
        error = state.sliceError(DataSlice.OBJECTIVES),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.OBJECTIVE) },
        // El `matchesFilter` por defecto compara con la píldora, y lo que está
        // en curso no lleva ninguna. Se resuelve contra el dato real, no contra
        // su representación.
        matchesFilter = { entry, f ->
            val objective = objectives.firstOrNull { it.id == entry.id }
            when {
                f == "Todos" || objective == null -> true
                f == "En curso" -> !objective.completed
                f == "Cumplidos" -> objective.completed
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

/**
 * El dato grande de la tarjeta.
 *
 * Con meta, el progreso — es lo que define a un objetivo medible, y SE CONSERVA
 * al cumplirlo: saber que lo cerraste en 7/10 sigue siendo información.
 * Sin meta pero con fecha, los días que faltan, igual que hace Garantías.
 * Sin nada de eso no hay dato grande: un objetivo puede ser solo un enunciado,
 * y rellenar ese hueco con algo inventado sería peor que dejarlo vacío.
 */
private fun Objective.highlight(): String? = when {
    targetValue != null -> "$currentValue/$targetValue"
    deadline != null -> daysLabel(deadline)
    else -> null
}

/** La línea de identidad: dice lo que el dato grande no cuenta. */
private fun Objective.subtitle(): String = when {
    deadline != null -> "Vence el ${deadline.format(DAY_MONTH)}"
    targetValue != null -> "Sin fecha límite"
    else -> "Sin meta ni fecha"
}

/**
 * Días hasta la fecha, con el mismo vocabulario que el resto de la aplicación.
 * Un objetivo vencido lo dice: la fecha pasó y el objetivo sigue abierto.
 */
private fun daysLabel(date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days < 0 -> "Vencido"
        days == 0L -> "Hoy"
        days == 1L -> "Mañana"
        else -> "$days días"
    }
}

private val DAY_MONTH: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"))
