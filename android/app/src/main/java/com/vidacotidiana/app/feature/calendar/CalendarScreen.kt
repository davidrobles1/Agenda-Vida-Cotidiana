package com.vidacotidiana.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.calendar.DateAlert
import com.vidacotidiana.app.core.calendar.DayTask
import com.vidacotidiana.app.core.calendar.monthMatrix
import com.vidacotidiana.app.core.calendar.weekOf
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DayPanelHeader
import com.vidacotidiana.app.core.ui.components.DayRibbonItem
import com.vidacotidiana.app.core.ui.components.DayTimeline
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.MonthGrid
import com.vidacotidiana.app.core.ui.components.NotebookNotes
import com.vidacotidiana.app.core.ui.components.ResourceRow
import com.vidacotidiana.app.core.ui.components.SheetSurface
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaPill
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSegmented
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.core.ui.components.vidaSurface
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es", "MX")

/**
 * EL CALENDARIO — la pieza central.
 *
 * Conserva las once cosas que lo definen en el artefacto: la celda como pila,
 * las barras de carga, el anillo del día actual frente al relleno del elegido,
 * la EXPANSIÓN EN CONTEXTO dentro de la propia rejilla, la línea de tiempo con
 * la marca del ahora, las tres densidades con el mismo lenguaje, los avisos
 * derivados, las hojas de detalle que llevan al registro de origen, la
 * navegación entre meses con gesto e inercia, y el cuaderno de notas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val today = LocalDate.now()
    val now = LocalTime.now()
    val selected = state.selectedDate
    val selectedContent = viewModel.contentFor(selected)

    val context = LocalContext.current
    var alertSheet by remember { mutableStateOf<DateAlert?>(null) }
    var taskSheet by remember { mutableStateOf<DayTask?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    VidaScreen(
        title = "Calendario",
        subtitle = "Tu vida, mes a mes.",
        onNavigationClick = openDrawerAction(drawerState, scope),
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
        actions = {
            VidaIconButton(Icons.Filled.Add, "Nueva tarea") { onNavigate(Routes.TASKS) }
            VidaIconButton(Icons.Outlined.Notifications, "Notificaciones", badge = true) { onNavigate(Routes.NOTIFICATIONS) }
        },
    ) {
        StaggeredAppear(0) {
            VidaSegmented(
                options = listOf("Mes", "Semana", "Día"),
                selected = state.calendarDensity,
                onSelect = viewModel::setDensity,
            )
        }

        StaggeredAppear(1) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .vidaSurface(
                        RoundedCornerShape(spec.radii.card), spec.radii.card,
                        c.surfaceVariant, c.line,
                    )
                    .padding(VidaSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                when (state.calendarDensity) {
                    "Mes" -> MonthDensity(viewModel, state.visibleMonth, selected, today, now, ::onTaskNoop, { alertSheet = it }, { taskSheet = it })
                    "Semana" -> WeekDensity(viewModel, selected, today, now, { alertSheet = it }, { taskSheet = it })
                    else -> DayDensity(viewModel, selected, today, now, { alertSheet = it }, { taskSheet = it })
                }
            }
        }

        StaggeredAppear(2) { Eyebrow("Notas del día") }
        StaggeredAppear(3) {
            // Las notas se persisten en `/api/v1/day-notes`, el mismo servicio
            // que usa la Web: una nota escrita aquí aparece allí, y al revés.
            NotebookNotes(
                notes = state.notes,
                placeholder = "Escribe una nota…",
                onAdd = viewModel::addNote,
                onEdit = viewModel::editNote,
                onDelete = viewModel::deleteNote,
                loading = state.notesLoading,
            )
        }

        StaggeredAppear(4) {
            Text(
                "Desliza sobre la rejilla para cambiar de mes. Toca un día para abrirlo aquí mismo.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }

    // Hoja de un aviso: explica que es derivado y lleva a su registro.
    alertSheet?.let { alert ->
        ModalBottomSheet(
            onDismissRequest = { alertSheet = null },
            sheetState = sheetState,
            containerColor = c.surfaceVariant,
        ) {
            SheetSurface(onClose = { alertSheet = null }, title = alert.label) {
                Text(
                    "${alert.source.label} · ${alert.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                VidaCard {
                    Text(
                        "Este aviso no es un registro propio: se deriva de tu ${alert.source.label.lowercase()}. " +
                            "Si cambias su fecha, el aviso se mueve solo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {

                    VidaSmallButton("Ver en ${alert.source.label}", {
                        val route = alert.source.route
                        alertSheet = null
                        onNavigate(route)
                    })
                }
            }
        }
    }

    // Hoja de una tarea: su estado y la acción real de completarla.
    taskSheet?.let { task ->
        ModalBottomSheet(
            onDismissRequest = { taskSheet = null },
            sheetState = sheetState,
            containerColor = c.surfaceVariant,
        ) {
            SheetSurface(onClose = { taskSheet = null }, title = task.title) {
                Text(
                    listOfNotNull(task.meta, task.location).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                ResourceRow(
                    title = "Compartido con",
                    subtitle = "Nadie todavía",
                    pill = "Solo yo" to PillTone.QUIET,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {

                    // FR-024: «Cómo llegar» solo cuando la tarea tiene sitio.
                    // Delega en la aplicación de mapas del teléfono; no se pide
                    // ningún permiso ni se construye mapa propio.
                    task.location?.let { place ->
                        VidaSmallButton("Cómo llegar", {
                            viewModel.openDirections(context, place)
                            taskSheet = null
                        }, ghost = true)
                    }

                    // Editar abre el MISMO formulario de la tarea, ya relleno,
                    // contra `PATCH /reminders/{id}`. Ni editor genérico ni
                    // pantalla aparte: la hoja del calendario lleva al recurso.
                    VidaSmallButton("Editar", {
                        val id = task.id
                        taskSheet = null
                        viewModel.requestEditTask(id)
                    }, ghost = true)
                    if (!task.done) {
                        VidaSmallButton("Marcar hecha", {
                            viewModel.toggleTask(task.id)
                            taskSheet = null
                        })
                    }
                }
            }
        }
    }
}

private fun onTaskNoop(task: DayTask) = Unit

/** Vista de mes: la rejilla con el panel del día abierto dentro. */
@Composable
private fun MonthDensity(
    viewModel: AppViewModel,
    month: java.time.YearMonth,
    selected: LocalDate,
    today: LocalDate,
    now: LocalTime,
    onTaskLegacy: (DayTask) -> Unit,
    onAlert: (DateAlert) -> Unit,
    onTask: (DayTask) -> Unit,
) {
    val c = VidaTheme.colors
    val cells = monthMatrix(month)
    val monthTasks = cells.filter { it.inMonth }.sumOf { viewModel.contentFor(it.date).tasks.size }
    val monthAlerts = cells.filter { it.inMonth }.sumOf { viewModel.contentFor(it.date).alerts.size }
    val busiest = cells.filter { it.inMonth }.maxByOrNull { viewModel.contentFor(it.date).total }

    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
        // Cabecera: mes, resumen derivado y salto a hoy.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") { viewModel.shiftMonth(-1) }
            Column(Modifier.weight(1f)) {
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() }} ${month.year}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.text,
                )
                Text(
                    "$monthTasks tareas · $monthAlerts avisos",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente") { viewModel.shiftMonth(1) }
            VidaSmallButton("Hoy", { viewModel.goToToday() }, ghost = selected != today)
        }

        // LA RETÍCULA DEL ARTEFACTO, Y ARRIBA.
        //
        // Estas tres cifras ya existían, pero vivían AL PIE de la rejilla, en
        // tres cajitas centradas que había que bajar a buscar. Un resumen que
        // se lee después de lo que resume no resume nada: llega cuando ya
        // recorriste el mes celda a celda.
        //
        // Ahora abren, como en todas las demás secciones, y con las piezas del
        // artefacto en vez de con un componente propio de esta pantalla.
        //
        // «El día más cargado» baja al pie de su pieza: es un matiz de «este
        // mes», no una tercera cifra del mismo rango.
        VidaTileRow(
            listOf(
                VidaTileSpec(
                    "Este mes", monthTasks.toString(),
                    busiest?.takeIf { viewModel.contentFor(it.date).total > 0 }
                        ?.let { "tareas · el ${it.date.dayOfMonth} es el más cargado" }
                        ?: "tareas",
                    c.primaryContainer, c.primary, c.primaryDeep,
                    weight = 1.32f,
                ),
                VidaTileSpec(
                    "Avisos", monthAlerts.toString(), "derivados",
                    if (monthAlerts == 0) c.successContainer else c.warningContainer,
                    if (monthAlerts == 0) c.successText else c.warningText,
                    if (monthAlerts == 0) c.successText else c.warningText,
                ),
                VidaTileSpec(
                    "Hoy", viewModel.contentFor(today).total.toString(), "en tu día",
                    c.sunken, c.text, c.textSecondary,
                    onClick = { viewModel.goToToday() },
                ),
            ),
        )

        CalendarLegend()

        // La rejilla, con gesto horizontal para cambiar de mes: mismo umbral
        // que en el artefacto, con la inercia del propio gesto.
        Box(
            Modifier.pointerInput(month) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (total < -60f) viewModel.shiftMonth(1)
                        if (total > 60f) viewModel.shiftMonth(-1)
                        total = 0f
                    },
                    onDragCancel = { total = 0f },
                ) { _, delta -> total += delta }
            },
        ) {
            MonthGrid(
                cells = cells,
                today = today,
                selected = selected,
                contentFor = viewModel::contentFor,
                onSelect = viewModel::selectDate,
                dayPanel = {
                    DayPanel(viewModel, selected, today, now, onAlert, onTask)
                },
            )
        }
    }
}

/** El panel del día que se abre DENTRO de la rejilla. */
@Composable
private fun DayPanel(
    viewModel: AppViewModel,
    date: LocalDate,
    today: LocalDate,
    now: LocalTime,
    onAlert: (DateAlert) -> Unit,
    onTask: (DayTask) -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val content = viewModel.contentFor(date)
    // UNA SUPERFICIE, UNA RAZÓN.
    //
    // Aquí se apilaban cuatro: pantalla → tarjeta del calendario → este panel
    // → las tarjetas de cada tarea. El panel llevaba relleno Y borde Y radio,
    // estando ya dentro de una tarjeta con borde: tres señales para un límite
    // que la tarjeta contenedora ya dibujaba.
    //
    // El RELLENO se queda porque no es decorativo: es lo que separa del blanco
    // de la tarjeta y hace visibles las piezas blancas de la línea de tiempo.
    // El BORDE se va: era la señal redundante. La agrupación la sostienen el
    // tono hundido y la cabecera del día, que ya dice de qué día hablamos.
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.sunken, RoundedCornerShape(spec.radii.control))
            .padding(VidaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        DayPanelHeader(date, content)
        DayTimeline(
            content = content,
            isToday = date == today,
            now = now,
            onTask = onTask,
            onAlert = onAlert,
        )
    }
}

/** Vista de semana: la cinta de días con el mismo idioma, más el día abierto. */
@Composable
private fun WeekDensity(
    viewModel: AppViewModel,
    selected: LocalDate,
    today: LocalDate,
    now: LocalTime,
    onAlert: (DateAlert) -> Unit,
    onTask: (DayTask) -> Unit,
) {
    val c = VidaTheme.colors
    val week = weekOf(selected)
    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Mismas flechas que la vista mensual: la semana se recorre igual
            // que el mes, sin aprender un gesto distinto para cada densidad.
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior") { viewModel.shiftWeek(-1) }
            Column(Modifier.weight(1f)) {
                Text(
                    "Semana del ${week.first().dayOfMonth} de ${week.first().month.getDisplayName(TextStyle.FULL, ES)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.text,
                )
                Text(
                    "al ${week.last().dayOfMonth} de ${week.last().month.getDisplayName(TextStyle.FULL, ES)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente") { viewModel.shiftWeek(1) }
            VidaSmallButton("Hoy", { viewModel.goToToday() }, ghost = selected != today)
        }
        CalendarLegend()
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
        ) {
            week.forEach { date ->
                DayRibbonItem(
                    date = date,
                    content = viewModel.contentFor(date),
                    isToday = date == today,
                    isSelected = date == selected,
                    onClick = { viewModel.selectDate(date) },
                )
            }
        }
        DayPanel(viewModel, selected, today, now, onAlert, onTask)
    }
}

/** Vista de día: la misma composición, a máxima escala. */
@Composable
private fun DayDensity(
    viewModel: AppViewModel,
    selected: LocalDate,
    today: LocalDate,
    now: LocalTime,
    onAlert: (DateAlert) -> Unit,
    onTask: (DayTask) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior") {
                viewModel.selectDate(selected.minusDays(1))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "${selected.dayOfMonth} de ${selected.month.getDisplayName(TextStyle.FULL, ES)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VidaTheme.colors.text,
                    textAlign = TextAlign.Center,
                )
            }
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente") {
                viewModel.selectDate(selected.plusDays(1))
            }
        }
        DayPanel(viewModel, selected, today, now, onAlert, onTask)
    }
}

/** La leyenda enseña el idioma del calendario una sola vez. */
@Composable
private fun CalendarLegend() {
    val c = VidaTheme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        listOf(
            "Tarea" to c.primary,
            "Urgente" to c.warning,
            "Próximo" to c.primary.copy(alpha = 0.55f),
            "Nota" to c.second,
        ).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp).background(color, RoundedCornerShape(2.dp)))
                Text(label, style = VidaTheme.type.caption, color = c.textSecondary)
            }
        }
    }
}

