package com.vidacotidiana.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSectionLabel
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.navigation.Routes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * TU DÍA — la línea de tiempo del artefacto maestro.
 *
 * LO QUE APORTA Y NO EXISTÍA. Inicio dice QUÉ reclama; esta pantalla dice
 * CUÁNDO. Son preguntas distintas: «4 cosas te esperan» no cuenta si son de las
 * nueve o de las siete, y la forma del día es justamente lo que permite decidir
 * qué cabe.
 *
 * LA LÍNEA ES PROPORCIONAL: 38 dp por hora, de 08:00 a 21:00. Una lista de
 * horas equiespaciadas mentiría sobre el hueco real entre dos citas.
 *
 * LO QUE NO TIENE HORA NO ENTRA EN LA LÍNEA. Un pago no ocurre «a las cuatro»;
 * inventarle una hora para colocarlo sería falsear el dato. Va en su propia
 * bandeja, debajo, dicho tal cual es.
 *
 * Datos reales: `DayTask.time` y `DayTask.location`, que ya existían en el
 * modelo, más `AttentionEngine` para lo que reclama sin hora.
 */
/**
 * LA FRANJA POR DEFECTO, no la franja posible.
 *
 * Antes eran los límites duros de la línea, y lo que caía fuera se arrastraba
 * dentro con un `coerceIn`: una tarea de las 06:00 se dibujaba en la raya de
 * las 08:00, es decir la pantalla mentía sobre la hora en vez de admitir que
 * no cabía. La franja real se calcula abajo a partir de lo que hay ese día.
 */
private const val DEFAULT_FIRST_HOUR = 8
private const val DEFAULT_LAST_HOUR = 21
private val HOUR_HEIGHT = 38.dp

/** Media altura de la etiqueta de hora: lo que hay que subirla para centrarla en su raya. */
private val HOUR_LABEL_LIFT = 7.dp

@Composable
fun DiaScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    val tasks = viewModel.allTasks().filter { it.date == today }
    val timed = tasks.filter { it.time != null }.sortedBy { it.time }

    // EL CONJUNTO DE HOY, el mismo que cuenta el mosaico «Para hoy» de Inicio.
    //
    // Antes esto pedía `now()` —vencido + hoy—, así que esta pantalla y aquel
    // mosaico decían números distintos sobre la misma jornada. Lo atrasado
    // tiene ahora su propia pantalla, que es donde lleva su propio mosaico.
    val claims = AttentionEngine.today(
        AttentionEngine.scan(state.data, viewModel.allTasks(), today),
    )
    // Lo que ya está colocado en la línea no se repite debajo: una tarea con
    // hora está dibujada en su sitio, y volver a listarla como «suelta» la
    // contaría dos veces en la misma pantalla.
    val timedIds = timed.map { "task:${it.id}" }.toSet()
    val untimed = claims.filterNot { it.id in timedIds }

    /*
     * LA FRANJA HORARIA SE DERIVA DE LOS DATOS, no al revés.
     *
     * Ensanchar la ventana para que quepa lo que hay es lo contrario de
     * recortar la posición para que quepa en la ventana: aquí nada se desplaza
     * de su hora real. Si no hay nada fuera del horario habitual, la franja es
     * exactamente la de siempre y la pantalla se ve igual que en el artefacto.
     */
    val hours = timed.mapNotNull { it.time?.hour }
    val firstHour = minOf(DEFAULT_FIRST_HOUR, hours.minOrNull() ?: DEFAULT_FIRST_HOUR)
    val lastHour = maxOf(DEFAULT_LAST_HOUR, hours.maxOrNull() ?: DEFAULT_LAST_HOUR)

    VidaScreen(
        title = "Tu día",
        subtitle = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            .replaceFirstChar { it.uppercase() } + " ${today.dayOfMonth}",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        /* ── LA SEMANA, PARA SITUARSE ────────────────────────────────────── */
        StaggeredAppear(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0..6).map { today.minusDays((today.dayOfWeek.value - 1 - it).toLong()) }.forEach { day ->
                    val isToday = day == today
                    val count = viewModel.allTasks().count { it.date == day }
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .background(
                                if (isToday) c.primary else c.surfaceElevated,
                                RoundedCornerShape(16.dp),
                            )
                            .vidaClickable(onClick = { viewModel.selectDate(day) })
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es", "MX")).uppercase(),
                            style = t.micro,
                            color = if (isToday) c.onPrimary.copy(alpha = .75f) else c.textTertiary,
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = t.metricFigure.copy(fontSize = 15.sp, lineHeight = 19.sp),
                            color = if (isToday) c.onPrimary else c.textSecondary,
                        )
                        // Un punto por «hay algo», no un contador: el número
                        // exacto de cada día no cabe y tampoco hace falta.
                        Box(
                            Modifier
                                .size(5.dp)
                                .background(
                                    when {
                                        isToday -> c.onPrimary
                                        count > 0 -> c.primary
                                        else -> c.line
                                    },
                                    RoundedCornerShape(50),
                                ),
                        )
                    }
                }
            }
        }

        /* ── LA LÍNEA DEL DÍA ────────────────────────────────────────────── */
        StaggeredAppear(1) { VidaSectionLabel("La forma del día", trailing = "${timed.size} con hora") }
        StaggeredAppear(2) {
            if (timed.isEmpty()) {
                EmptyState(
                    title = "Nada con hora hoy",
                    body = "Lo que tiene hora aparece aquí colocado en su sitio. " +
                        "Lo demás está abajo, sin hora inventada.",
                    action = "Crear algo" to { navController.navigate(Routes.CREATE) },
                )
            } else {
                val now = LocalTime.now()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(HOUR_HEIGHT * (lastHour - firstHour) + 30.dp),
                ) {
                    // POSICIONAR ES `offset`, NO `padding`.
                    //
                    // Un padding es margen INTERIOR: por definición no puede
                    // ser negativo, y Compose lanza `IllegalArgumentException`
                    // si lo es. Usarlo como sistema de coordenadas funcionaba
                    // sólo mientras ninguna posición cayera por encima del
                    // origen — y centrar la etiqueta sobre su raya es
                    // exactamente eso: la primera hora está en y = 0, así que
                    // su etiqueta iba a −7 dp y la pantalla cerraba la
                    // aplicación al abrirse. Siempre, no en un caso raro.
                    //
                    // `offset` es una traslación, admite negativos porque un
                    // desplazamiento negativo es una posición perfectamente
                    // válida, y es el primitivo que corresponde a lo que esta
                    // línea de tiempo hace: colocar cosas en coordenadas.
                    (firstHour..lastHour step 2).forEach { hour ->
                        val y = HOUR_HEIGHT * (hour - firstHour)
                        Text(
                            "%02d:00".format(hour),
                            style = t.micro,
                            color = c.textTertiary,
                            modifier = Modifier.offset(y = y - HOUR_LABEL_LIFT).width(40.dp),
                        )
                        Box(
                            Modifier
                                .offset(y = y)
                                .padding(start = 48.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(c.line),
                        )
                    }

                    timed.forEach { task ->
                        val time = task.time ?: return@forEach
                        // Sin `coerceIn`: la franja ya se ensanchó arriba para
                        // contener esta hora, así que la posición es la real.
                        val offsetY = HOUR_HEIGHT * (time.hour - firstHour) +
                            HOUR_HEIGHT * (time.minute / 60f)
                        val tone = if (task.done) c.success else c.primary
                        Row(
                            modifier = Modifier
                                .offset(y = offsetY)
                                .padding(start = 48.dp)
                                .fillMaxWidth()
                                .background(
                                    if (task.done) c.successContainer else c.primaryContainer,
                                    RoundedCornerShape(18.dp),
                                )
                                .vidaClickable(onClick = { navController.navigate(Routes.taskRoute(task.id)) })
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            Box(Modifier.width(5.dp).height(34.dp).background(tone, RoundedCornerShape(3.dp)))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = t.cardTitle.copy(fontSize = 15.sp),
                                    color = if (task.done) c.textTertiary else c.text,
                                    maxLines = 1,
                                )
                                Text(
                                    listOfNotNull(
                                        time.toString().take(5),
                                        task.location,
                                    ).joinToString(" · "),
                                    style = t.micro,
                                    color = if (task.done) c.successText else c.primaryDeep,
                                )
                            }
                        }
                    }

                    // AHORA: lo único que se mueve solo en toda la pantalla.
                    if (now.hour in firstHour..lastHour) {
                        val y = HOUR_HEIGHT * (now.hour - firstHour) + HOUR_HEIGHT * (now.minute / 60f)
                        Row(
                            modifier = Modifier.offset(y = y).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                now.toString().take(5),
                                style = t.micro.copy(fontWeight = FontWeight.ExtraBold),
                                color = c.second,
                                modifier = Modifier.width(40.dp),
                            )
                            Box(Modifier.size(8.dp).background(c.second, RoundedCornerShape(50)))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(c.second.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                }
            }
        }

        /* ── LO QUE NO TIENE HORA ────────────────────────────────────────── */
        StaggeredAppear(3) { VidaSectionLabel("Sin hora", trailing = "${untimed.size}") }
        StaggeredAppear(4) {
            if (untimed.isEmpty()) {
                Text("Nada suelto hoy.", style = t.body, color = c.textTertiary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
                    untimed.take(8).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(c.surfaceElevated, RoundedCornerShape(50))
                                .vidaClickable(onClick = { navController.navigate(item.source.route) })
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier.size(7.dp).background(
                                    if (item.urgency == AttentionUrgency.OVERDUE) c.error else c.warning,
                                    RoundedCornerShape(50),
                                ),
                            )
                            Text(item.title, style = t.body, color = c.text, modifier = Modifier.weight(1f))
                            Text(item.source.label, style = t.micro, color = c.textTertiary)
                        }
                    }
                }
            }
        }

        /* ── LAS NOTAS DEL DÍA ───────────────────────────────────────────── */
        //
        // AQUÍ ES DONDE ESA PANTALLA SE ALCANZA. `NotasDiaScreen` existía,
        // estaba registrada en el grafo y compilada dentro del APK, pero
        // ninguna pantalla navegaba a ella: era inalcanzable. Su sitio natural
        // es este —lo que escribiste de tu día, junto a la forma de tu día— y
        // no un acceso suelto en el menú, que la habría convertido en una
        // sección más sin relación con nada.
        StaggeredAppear(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceElevated, RoundedCornerShape(50))
                    .vidaClickable(onClick = { navController.navigate(Routes.DAY_NOTES) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Notas de hoy",
                    style = t.body,
                    color = c.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (state.notes.isEmpty()) "Escribir" else "${state.notes.size}",
                    style = t.micro,
                    color = c.primary,
                )
            }
        }

        StaggeredAppear(6) {
            Text(
                "Un pago no ocurre «a las cuatro». No se le inventa una hora para meterlo en la línea.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }
    }
}
