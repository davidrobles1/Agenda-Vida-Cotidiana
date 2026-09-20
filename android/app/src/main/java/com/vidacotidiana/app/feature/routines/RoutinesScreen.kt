package com.vidacotidiana.app.feature.routines

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.Routine
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.VidaRing
import com.vidacotidiana.app.core.ui.components.vidaClickable
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
 * Rutinas (FR-032). Sección secundaria de Laboral: se llega desde el cajón.
 *
 * DOS REGLAS DEL DOMINIO QUE ESTA PANTALLA NO PUEDE ROMPER:
 *
 * 1. **Una rutina no se "cumple", se hace — y siempre vuelve.** No existe
 *    `completed`: su estado permanente es `active`. Por eso el botón dice
 *    «Hecha» y no desaparece nunca al pulsarlo.
 *
 * 2. **Un clic registra UNA ocurrencia.** El avance lo calcula el servidor
 *    desde la fecha PROGRAMADA, no desde hoy. Consecuencia visible y
 *    deliberada: una rutina con varios periodos de retraso **sigue diciendo
 *    "Atrasada" después de pulsar «Hecha»**. No es un fallo — disimularlo
 *    exigiría emitir más llamadas y registrar ejecuciones que nadie hizo.
 *
 * "Atrasada" NO es un campo del backend: se deriva aquí comparando la fecha con
 * hoy, igual que hace la Web. Se usa `parseDate` (en el repositorio) y
 * `LocalDate.now()`, que es la política de fechas ya establecida en el proyecto
 * —día calendario literal, sin conversión de zona—, no una nueva.
 */
@Composable
fun RoutinesScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    // Lo activo primero, y dentro de eso lo que toca antes. Lo pausado al final:
    // su fecha no significa nada mientras esté en pausa.
    val routines = state.data.routines.sortedWith(
        compareBy({ !it.active }, { it.nextExecutionDate }),
    )

    val entries = routines.map { routine ->
        val overdue = routine.active && routine.nextExecutionDate.isBefore(today)
        ResourceEntry(
            id = routine.id,
            title = routine.title,
            subtitle = listOfNotNull(
                frequencyLabel(routine.frequency),
                routine.nextExecutionDate.format(DAY_MONTH),
                routine.description?.ifBlank { null },
            ).joinToString(" · "),
            highlight = routine.highlight(today),
            icon = Icons.Outlined.Repeat,
            group = when {
                !routine.active -> "En pausa"
                overdue -> "Atrasadas"
                else -> "Próximas"
            },
            pill = when {
                !routine.active -> "En pausa" to PillTone.QUIET
                overdue -> "Atrasada" to PillTone.WARN
                else -> null
            },
            onEdit = { viewModel.requestEdit(CreatableResource.ROUTINE, routine.id) },
            // «Hecha» solo mientras esté activa: registrar una ocurrencia de una
            // rutina pausada movería una fecha que el usuario decidió congelar.
            onComplete = if (routine.active) {
                { viewModel.completeResource(CreatableResource.ROUTINE, routine.id) }
            } else {
                null
            },
            completeLabel = "Hecha",
            // Pausar y reanudar viven en la hoja de detalle: la tarjeta conserva
            // UNA sola acción rápida, que es la que se usa a diario.
            extraActions = listOf(
                (if (routine.active) "Pausar" else "Reanudar") to {
                    viewModel.setRoutineActive(routine.id, !routine.active)
                },
            ),
            onDelete = { viewModel.deleteResource(CreatableResource.ROUTINE, routine.id) },
        )
    }

    // Los hábitos CON META van arriba, en anillos: es la composición del
    // artefacto y distingue «Agua, 4 de 8» de «Sacar la basura», que solo se
    // marca. Las rutinas de sí/no siguen en la lista de abajo, intactas.
    val counted = routines.filter { it.targetCount != null && it.active }
    LaunchedEffect(counted.size) { if (counted.isNotEmpty()) viewModel.loadHabitsToday() }

    ResourceListScreen(
        header = {
            if (counted.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.blockGap)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("HÁBITOS DE HOY", style = VidaTheme.type.eyebrow, color = VidaTheme.colors.textTertiary, modifier = Modifier.weight(1f))
                        Text("Toca para sumar", style = VidaTheme.type.caption, color = VidaTheme.colors.textTertiary)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(VidaTheme.colors.surfaceVariant, RoundedCornerShape(VidaTheme.spec.radii.card))
                            .border(VidaTheme.spec.borderWidth, VidaTheme.colors.line, RoundedCornerShape(VidaTheme.spec.radii.card))
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        counted.take(4).forEach { r ->
                            val done = state.habitToday[r.id] ?: 0
                            val cap = r.targetCount ?: 1
                            Column(
                                Modifier
                                    .weight(1f)
                                    // Sumar NO ejecuta la rutina: `execute`
                                    // cierra la ocurrencia y mueve la fecha,
                                    // esto solo suma dentro del día.
                                    .vidaClickable(onClick = { viewModel.addHabitProgress(r.id) }),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                VidaRing(
                                    percent = if (cap == 0) 0 else (done * 100 / cap).coerceAtMost(100),
                                    tone = VidaTheme.colors.primary,
                                    diameter = 66.dp,
                                    stroke = 7.dp,
                                    label = done.toString(),
                                    labelColor = VidaTheme.colors.text,
                                )
                                Text(r.title, style = VidaTheme.type.caption, color = VidaTheme.colors.text, maxLines = 1)
                                Text(
                                    "de $cap${r.unit?.let { " $it" } ?: ""}",
                                    style = VidaTheme.type.micro,
                                    color = VidaTheme.colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }
        },
        title = "Rutinas",
        subtitle = "Lo que repites, y cuándo toca otra vez.",
        eyebrow = plural(routines.count { it.active }, "activa", "activas"),
        entries = entries,
        filters = listOf("Todas", "Próximas", "Atrasadas", "En pausa"),
        addLabel = "Nueva rutina",
        emptyBody = "Anota algo que repitas y sabrás cuándo te toca otra vez.",
        loading = state.loading,
        error = state.sliceError(DataSlice.ROUTINES),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.ROUTINE) },
        // Se resuelve contra el dato real y no contra la píldora: "Próximas" no
        // lleva ninguna, así que el `matchesFilter` por defecto no serviría.
        matchesFilter = { entry, f ->
            val routine = routines.firstOrNull { it.id == entry.id }
            when {
                f == "Todas" || routine == null -> true
                f == "En pausa" -> !routine.active
                f == "Atrasadas" -> routine.active && routine.nextExecutionDate.isBefore(today)
                f == "Próximas" -> routine.active && !routine.nextExecutionDate.isBefore(today)
                else -> true
            }
        },
        drawerState = drawerState,
        scope = scope,
        showBack = true,
        onBack = { navController.popBackStack() },
        // La campana lleva de verdad a los avisos, y el punto sale de
        // cuántos quedan sin leer. Antes era `{}` con `badge = true`.
        onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        notificationsBadge = viewModel.avisosSinLeer(),
    )
}

/**
 * El dato grande: cuándo toca.
 *
 * Una rutina pausada muestra su fecha como INFORMACIÓN, no como una próxima
 * ejecución — por eso no dice "3 días" sino la fecha a secas: mientras esté
 * pausada, esos días no van a correr.
 */
private fun Routine.highlight(today: LocalDate): String = when {
    !active -> nextExecutionDate.format(DAY_MONTH)
    nextExecutionDate.isBefore(today) -> "Atrasada"
    nextExecutionDate == today -> "Hoy"
    else -> {
        val days = ChronoUnit.DAYS.between(today, nextExecutionDate)
        if (days == 1L) "Mañana" else "$days días"
    }
}

/**
 * Las tres frecuencias, con las etiquetas que YA usa la Web (`FREQUENCY_LABELS`
 * en `routines/api.ts`). No se traducen otra vez: dos vocabularios para el mismo
 * enum acabarían discrepando.
 */
private fun frequencyLabel(frequency: String): String = when (frequency) {
    "DAILY" -> "Diaria"
    "WEEKLY" -> "Semanal"
    "MONTHLY" -> "Mensual"
    // Un valor que este cliente no conozca se muestra tal cual en vez de
    // ocultarse: el backend declara solo tres, pero mentir sería peor.
    else -> frequency
}

private val DAY_MONTH: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"))
