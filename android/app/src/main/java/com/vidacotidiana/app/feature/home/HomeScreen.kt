package com.vidacotidiana.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.components.MoodFace
import com.vidacotidiana.app.core.ui.components.MoodScale
import com.vidacotidiana.app.core.ui.components.drawMoodFace
import com.vidacotidiana.app.core.ui.components.vidaClickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.vidacotidiana.app.core.ui.components.VidaTile
import java.time.Instant
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionItem
import com.vidacotidiana.app.core.attention.AttentionSource
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.data.WarrantyStatus
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DayRibbonItem
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.HeroCard
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.StripCard
import com.vidacotidiana.app.core.ui.components.VidaCard
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.VidaProgress
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import com.vidacotidiana.app.core.ui.components.plural
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.vidacotidiana.app.core.app.loadFailed
import com.vidacotidiana.app.core.ui.components.AttentionList
import com.vidacotidiana.app.core.ui.components.moodSky
import com.vidacotidiana.app.core.ui.components.moodInk
import com.vidacotidiana.app.core.ui.components.vidaFloat

/**
 * INICIO RESPONDE A UNA PREGUNTA: ¿qué necesita mi atención?
 *
 * ANTES ENUMERABA MÓDULOS. De sus cinco secciones, dos eran contadores en crudo
 * («Inventario · 4 artículos», que ni viene ni requiere nada) y otra eran dos
 * enlaces fijos rotulados como «actividad reciente». La única que intentaba
 * hablar de atención —el héroe— leía `contentFor(hoy).alerts`, es decir los
 * avisos derivados de ADR-018, que solo existen en las fechas exactas de
 * antelación. Una tarea vencida ayer, un pago cuyo día pasó sin pagarse o un
 * mantenimiento atrasado no llegaban aquí: había que ir módulo por módulo a
 * comprobarlo.
 *
 * AHORA LA PANTALLA LEE `AttentionEngine`, que mira el estado real de las siete
 * fuentes con fecha que ya existían y trae lo que reclama, con el motivo por el
 * que reclama. La diferencia de percepción no está en el diseño: está en que
 * «Garantías — 5» pasa a ser «La garantía del ipad vence en 3 días».
 *
 * EL ORDEN ES LA TESIS:
 *
 *  1. HÉROE     — cuántas cosas te reclaman y de dónde salen. Cuando no hay
 *                 ninguna, lo dice con seguridad: eso también es información.
 *  2. AHORA     — lo vencido y lo de hoy, junto, sin importar de qué módulo.
 *  3. ESTA SEMANA — lo que viene en siete días.
 *  4. TU DÍA    — el cierre del día: cuánto llevas hecho.
 *  5. PRÓXIMOS 7 DÍAS — la semana en el lenguaje del calendario.
 *  6. TUS ÁREAS — acceso. Va al final porque es lo que el usuario consulta
 *                 cuando quiere, no lo que necesita saber al abrir.
 *
 * NO FILTRA POR CONTEXTO y es correcto: `loadAll` ya pide los datos con el
 * contexto activo (ADR-019), así que en Personal solo hay Personal. Portal
 * recibe todo junto porque eso es Portal.
 */
/**
 * Cuántas filas dibuja cada sección. Lo que no cabe sigue contado en el
 * antetítulo y sigue estando en su módulo: se acota lo que se PINTA, no lo que
 * se sabe.
 */
private const val MAX_ROWS = 6

/**
 * La cifra de un mosaico, o una raya cuando todavía no hay cifra que dar.
 *
 * Un cero es una afirmación —«no tienes nada»— y mientras se carga o cuando la
 * consulta falló no hay nada que afirmar. Poner «0» ahí era lo que hacía que
 * Inicio le dijera al usuario que tenía el día libre justo cuando el teléfono
 * no había podido preguntarlo.
 */
private fun figureOrDash(value: Int, loading: Boolean, failed: Boolean): String =
    if (loading || failed) "—" else value.toString()

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    // El ánimo de HOY, no la semana: en Inicio la cara es pequeña y le basta.
    // El ánimo de hoy y QUIÉN eres. Lo segundo faltaba: el saludo leía
    // `state.user` pero nadie lo pedía desde aquí —solo Portal—, así que en
    // Inicio siempre era nulo y el saludo se quedaba en «¡Hola!».
    LaunchedEffect(Unit) {
        viewModel.loadMood()
        viewModel.loadUser()
    }

    val attention = AttentionEngine.scan(state.data, viewModel.allTasks(), today)
    val now = AttentionEngine.now(attention)
    val soon = AttentionEngine.soon(attention)
    val forToday = AttentionEngine.today(attention)
    val overdue = AttentionEngine.overdue(attention)

    // LOS TRES ESTADOS QUE INICIO NO DISTINGUÍA.
    //
    // Esta pantalla no consultaba `loading` ni el resultado de la carga, así
    // que pintaba ceros con total confianza tanto mientras cargaba como
    // cuando no había podido hablar con el servidor: «Nada te reclama» era la
    // misma frase para «no tienes nada» y para «no lo sé». Como lee las siete
    // fuentes a la vez, le basta con que falle una para no poder responder.
    val firstLoad = state.loading && state.data.collections.all { it.isEmpty() }
    val cannotAnswer = state.loadFailed && attention.isEmpty()

    val todayTasks = viewModel.contentFor(today).tasks
    val doneCount = todayTasks.count { it.done }

    VidaScreen(
        // El saludo era «¡Hola!» a secas aunque la sesión ya conocía al
        // usuario: `/api/v1/me` se pedía y se guardaba, y nadie lo leía. No se
        // inventa una segunda fuente ni se guarda el nombre aparte — se usa la
        // que ya hay, y si todavía no ha llegado el saludo es el de siempre.
        title = state.user?.username?.takeIf { it.isNotBlank() }
            ?.let { "¡Hola, $it!" } ?: "¡Hola!",
        subtitle = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            .replaceFirstChar { it.uppercase() } + " ${today.dayOfMonth} de " +
            today.month.getDisplayName(TextStyle.FULL, Locale("es", "MX")),
        onNavigationClick = openDrawerAction(drawerState, scope),
        onRefresh = viewModel::refresh,
        refreshing = state.loading,
        context = state.context,
        laboralEnabled = state.laboralEnabled,
        onContextSelect = viewModel::setContext,
        actions = {
            // EL PUNTO DICE LA VERDAD O NO ESTÁ.
            //
            // Estaba fijado a `true`: un aviso permanente que nunca se apagaba
            // por mucho que el usuario leyera, y que por eso dejaba de
            // significar nada. Ahora sale del mismo conjunto que pinta la
            // pantalla de avisos, menos lo ya leído.
            val sinLeer = attention.count { it.noticeKey !in state.readNotices }
            VidaIconButton(
                Icons.Outlined.Notifications,
                if (sinLeer == 0) "Avisos" else "Avisos · $sinLeer sin leer",
                badge = sinLeer > 0,
            ) { onNavigate(Routes.NOTIFICATIONS) }
            VidaIconButton(Icons.Outlined.Settings, "Ajustes") { onNavigate(Routes.SETTINGS) }
        },
    ) {
        /* ══════════════════════════════════════════════════════════════════
           1 — LA RETÍCULA DE CUATRO PIEZAS
           ══════════════════════════════════════════════════════════════════

           EL ARTEFACTO MAESTRO ABRE CON CUATRO CIFRAS, no con un héroe.

           Su retícula es `1.32fr / 1fr` en dos filas de 120 y 100 dp, con 10 dp
           de hueco. Esa proporción es la tesis de la pantalla: la pieza grande
           —lo que te toca hoy— domina sin aplastar a las otras tres, y las
           cuatro se leen de un vistazo antes de leer una sola fila de lista.

           El héroe anterior decía lo mismo en una frase y ocupaba el mismo
           espacio. La diferencia es que una frase se lee; cuatro cifras se
           reconocen.

           CADA CIFRA ES EXACTAMENTE SU ETIQUETA, y lleva a ese mismo conjunto:
             · Para hoy  → `AttentionEngine.today`, siete fuentes  → Día
             · Atrasado  → `AttentionEngine.overdue`, siete fuentes → lo atrasado
             · En total  → TODAS las colecciones de `VidaData`      → sin destino
             · Hechas    → tareas COMPLETED de la semana            → Tareas

           «Para hoy» contaba antes `now()`, que es vencido + hoy: la etiqueta
           prometía hoy y la cifra incluía lo atrasado, que además volvía a
           contarse en el mosaico contiguo. Los tres registros atrasados se
           contaban dos veces y el usuario leía nueve donde tenía seis.

           «En total» no lleva a ningún sitio, y es deliberado: el artefacto es
           el único de los cuatro que NO le pone galón. Es una cifra para
           mirar, no una puerta — y la puerta que tenía llevaba a Inventario,
           que contiene una fracción de lo que la cifra cuenta.
        */
        StaggeredAppear(0) {
            val oldest = overdue.minByOrNull { it.date }
            val oldestDays = oldest?.let { ChronoUnit.DAYS.between(it.date, today) } ?: 0L

            val d = state.data
            val total = d.collections.sumOf { it.size } + viewModel.allTasks().size
            val sections = d.collections.count { it.isNotEmpty() } +
                if (viewModel.allTasks().isNotEmpty()) 1 else 0

            val weekAgo = Instant.now().minus(7, ChronoUnit.DAYS)
            val doneThisWeek = state.reminders.count { r ->
                r.status == "COMPLETED" &&
                    (r.updatedAt?.let { runCatching { Instant.parse(it).isAfter(weekAgo) }.getOrDefault(false) } ?: false)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Fila 1 — 120 dp. La pieza grande y la de lo atrasado.
                Row(
                    Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VidaTile(
                        kicker = "Para hoy",
                        figure = figureOrDash(forToday.size, firstLoad, cannotAnswer),
                        caption = when {
                            firstLoad -> "Mirando tus secciones…"
                            cannotAnswer -> "No pudimos consultarlo"
                            forToday.isEmpty() -> "Nada te reclama hoy"
                            else -> AttentionEngine.breakdown(forToday)
                        },
                        background = Brush.linearGradient(listOf(c.primary, c.primaryDeep)),
                        figureColor = c.onPrimary,
                        kickerColor = c.onPrimary.copy(alpha = 0.85f),
                        captionColor = c.onPrimary.copy(alpha = 0.78f),
                        // 46 dp y no 54: en 88 dp útiles, 16 de antetítulo
                        // + 54 de cifra + 3 + 15 de leyenda son 88 justos, y
                        // cualquier redondeo se comía la leyenda. La jerarquía
                        // de esta pieza la da su ancho (1.32f) y su degradado,
                        // no ocho puntos más de cifra.
                        figureSize = 46.dp,
                        chevron = true,
                        modifier = Modifier.weight(1.32f).fillMaxHeight(),
                        // «Para hoy» describe la jornada: lleva a la línea del
                        // día, que ahora muestra las siete fuentes de hoy y no
                        // solo las tareas — el mismo conjunto que cuenta.
                        onClick = { onNavigate(Routes.DAY) },
                    )
                    VidaTile(
                        kicker = "Atrasado",
                        figure = figureOrDash(overdue.size, firstLoad, cannotAnswer),
                        caption = when {
                            firstLoad -> "Comprobando…"
                            cannotAnswer -> "Sin respuesta"
                            overdue.isEmpty() -> "Nada pendiente"
                            oldestDays <= 1L -> "lo más viejo, de ayer"
                            else -> "lo más viejo, $oldestDays días"
                        },
                        background = SolidColor(c.errorContainer),
                        figureColor = c.error,
                        kickerColor = c.error,
                        captionColor = c.error.copy(alpha = 0.75f),
                        figureSize = 46.dp,
                        chevron = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        // Lo atrasado cruza módulos —una tarea, un pago y un
                        // mantenimiento—, así que llevaba a Tareas, donde sólo
                        // estaba uno de los tres. Ahora lleva a la misma lista
                        // que Inicio ya pinta debajo, filtrada por atrasado.
                        onClick = { onNavigate(Routes.attentionRoute(AttentionUrgency.OVERDUE)) },
                    )
                }
                // Fila 2 — 100 dp. El inventario del producto y lo ya resuelto.
                Row(
                    Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VidaTile(
                        kicker = "En total",
                        figure = figureOrDash(total, firstLoad, cannotAnswer),
                        caption = when {
                            firstLoad -> "Contando…"
                            cannotAnswer -> "Sin respuesta"
                            else -> plural(sections, "sección", "secciones").let { "en $it" }
                        },
                        background = SolidColor(c.sunken),
                        figureColor = c.text,
                        kickerColor = c.textSecondary,
                        captionColor = c.textTertiary,
                        // 28 dp. La fila de 100 dp deja 68 utiles y el
                        // contenido pide 16 de antetitulo + cifra + 3 + 15 de
                        // leyenda: con 34 daba 68 EXACTOS y se cortaba igual,
                        // porque «justo» y «cabe» no son lo mismo cuando el
                        // texto redondea hacia arriba. Con 28 sobran 6.
                        figureSize = 28.dp,
                        modifier = Modifier.weight(1.32f).fillMaxHeight(),
                    )
                    VidaTile(
                        // «Hechas» a secas parecía abarcar todo el esfuerzo de
                        // la semana, pero sólo cuenta tareas: una rutina
                        // cumplida o un pago liquidado no lo movían. Contarlo
                        // todo exigiría un «cuándo se completó» que rutinas,
                        // pagos y mantenimientos no guardan, así que la cifra
                        // se queda en lo que sí puede sostener y la leyenda lo
                        // dice — antes que un número que promete de más.
                        kicker = "Hechas",
                        figure = figureOrDash(doneThisWeek, firstLoad, state.remindersFailed),
                        caption = when {
                            firstLoad -> "Revisando…"
                            state.remindersFailed -> "Sin respuesta"
                            else -> "tareas, esta semana"
                        },
                        background = SolidColor(c.secondContainer),
                        figureColor = c.second,
                        kickerColor = c.second,
                        captionColor = c.second.copy(alpha = 0.75f),
                        // 28 dp. La fila de 100 dp deja 68 utiles y el
                        // contenido pide 16 de antetitulo + cifra + 3 + 15 de
                        // leyenda: con 34 daba 68 EXACTOS y se cortaba igual,
                        // porque «justo» y «cabe» no son lo mismo cuando el
                        // texto redondea hacia arriba. Con 28 sobran 6.
                        figureSize = 28.dp,
                        chevron = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onNavigate(Routes.TASKS) },
                    )
                }
            }
        }

        // 2 — «¿Cómo te sientes hoy?» (V36).
        //
        // Va AQUÍ, entre el héroe y lo que reclama, que es su sitio en el
        // artefacto maestro: después de saber cómo está tu día y antes de
        // ponerte a resolverlo. La cara es la misma pieza de Bienestar, en
        // pequeño, y tocar cualquier parte de la tarjeta abre la sección.
        StaggeredAppear(1) {
            val marked = state.moodToday?.value
            // El fondo lo decide el TEMA, no una tabla de cremas fijos: con
            // el color literal, en Noche esta tarjeta se quedaba clara bajo
            // un texto que sí había cambiado a claro.
            val sky = moodSky(marked)
            val deep = moodInk(marked)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sky, RoundedCornerShape(26.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Box(Modifier.vidaClickable(onClick = { onNavigate(Routes.WELLBEING) })) {
                        // `.floaty` del artefacto: la cara respira despacio en
                        // vez de quedarse impresa sobre la tarjeta.
                        MoodFace(
                            value = marked, size = 50.dp, strokeWidth = 4f, surface = c.surfaceVariant,
                            modifier = Modifier.vidaFloat(),
                        )
                    }
                    Column(
                        Modifier.weight(1f).vidaClickable(onClick = { onNavigate(Routes.WELLBEING) }),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("¿Cómo te sientes hoy?", style = t.cardTitle, color = c.text)
                        Text(
                            // «Sin marcar» afirma que el usuario no marcó su
                            // día; si la consulta falló, eso no lo sabemos, y
                            // decirlo borraba de la pantalla un ánimo que sí
                            // estaba guardado.
                            when {
                                marked != null -> MoodScale.ECHOES[marked]
                                state.moodError != null -> "No pudimos consultarlo"
                                state.moodLoading -> "Un momento…"
                                else -> "Sin marcar"
                            },
                            style = t.caption,
                            color = deep,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(12.dp))
                        Text("Solo tú", style = t.micro, color = c.textTertiary)
                    }
                }
                // Los cinco, a lo ancho. Marcar desde aquí guarda de verdad:
                // no abre la sección, la resuelve.
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (0..4).forEach { k ->
                        val on = marked == k
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (on) c.surfaceVariant else c.surfaceVariant.copy(alpha = 0.55f),
                                    RoundedCornerShape(16.dp),
                                )
                                .vidaClickable(onClick = { viewModel.setMood(k) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(24.dp)) {
                                drawMoodFace(k.toFloat(), 1f, 2f, androidx.compose.ui.graphics.Color.Transparent)
                            }
                        }
                    }
                }
            }
        }

        // 3 — Lo que no puede esperar, de los siete módulos a la vez.
        if (now.isNotEmpty()) {
            // El recuento va en el antetítulo, no en la lista: así la cifra
            // dice la verdad completa aunque solo se dibujen las primeras. Un
            // muro de veinte filas no reduce carga mental, la traslada.
            StaggeredAppear(1) { Eyebrow("Ahora · ${now.size}") }
            StaggeredAppear(2) { AttentionList(now.take(MAX_ROWS), onNavigate) }
        }

        // 3 — Lo que viene. Se calla cuando no hay nada: una sección vacía en
        // Inicio es ruido, no información.
        if (soon.isNotEmpty()) {
            StaggeredAppear(3) { Eyebrow("Esta semana · ${soon.size}") }
            StaggeredAppear(4) { AttentionList(soon.take(MAX_ROWS), onNavigate) }
        }

        // 4 — El cierre del día. No repite lo de arriba: aquello dice qué falta,
        // esto dice cuánto llevas.
        StaggeredAppear(5) { Eyebrow("Tu día") }
        StaggeredAppear(6) {
            VidaCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        when {
                            // Mismo criterio que los mosaicos: sin datos
                            // fiables no se afirma que el día está libre.
                            state.remindersFailed && todayTasks.isEmpty() -> "No pudimos consultar tus tareas"
                            state.loading && todayTasks.isEmpty() -> "Comprobando tu día…"
                            todayTasks.isEmpty() -> "Sin tareas para hoy"
                            todayTasks.size - doneCount > 0 ->
                                plural(todayTasks.size - doneCount, "tarea pendiente", "tareas pendientes")
                            else -> "Todo hecho hoy"
                        },
                        style = t.cardTitle,
                        color = c.text,
                    )
                    if (todayTasks.isNotEmpty()) {
                        Text("$doneCount de ${todayTasks.size}", style = t.caption, color = c.textSecondary)
                    }
                }
                VidaProgress(if (todayTasks.isEmpty()) 0f else doneCount.toFloat() / todayTasks.size)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
                    VidaSmallButton("Ver tareas", { onNavigate(Routes.TASKS) }, ghost = true)
                    VidaSmallButton("Compartidos", { onNavigate(Routes.SHARED) }, ghost = true)
                }
            }
        }

        // 5 — La semana, en el mismo lenguaje visual del calendario.
        StaggeredAppear(7) { Eyebrow("Los próximos 7 días") }
        StaggeredAppear(8) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap),
            ) {
                (0..6).map { today.plusDays(it.toLong()) }.forEach { date ->
                    DayRibbonItem(
                        date = date,
                        content = viewModel.contentFor(date),
                        isToday = date == today,
                        isSelected = false,
                        onClick = { viewModel.selectDate(date); onNavigate(Routes.CALENDAR) },
                    )
                }
            }
        }

        // 6 — Acceso, al final y rotulado como lo que es. Antes esto se llamaba
        // «Lo que se viene» y presentaba cuatro contadores como si reclamaran
        // algo; el inventario no viene ni requiere nada, solo existe. Ahora que
        // la atención está resuelta arriba, estas cifras son lo que siempre
        // fueron: el estado general, para consultar cuando se quiera.
        StaggeredAppear(9) { Eyebrow("Tus áreas") }
        StaggeredAppear(10) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap),
            ) {
                StripCard("Pagos", state.data.payments.size.toString(), "al mes", { onNavigate(Routes.PAYMENTS) })
                StripCard("Mantenimiento", state.data.maintenance.size.toString(), "programados", { onNavigate(Routes.MAINTENANCE) })
                StripCard(
                    "Garantías",
                    state.data.warranties.count { it.status != WarrantyStatus.VENCIDA }.toString(),
                    "vigentes",
                    { onNavigate(Routes.WARRANTIES) },
                )
                StripCard("Inventario", state.data.inventory.size.toString(), "artículos", { onNavigate(Routes.INVENTORY) })
            }
        }
    }
}

/**
 * La lista de cosas que reclaman.
 *
 * Usa `ResourceBoard` como el resto de la aplicación, y cae en su composición de
 * FILA porque ninguna entrada trae cifra destacada. Es deliberado: el importe de
 * un pago va dentro del motivo («Se paga hoy · $349 MXN») y no como dato héroe,
 * porque aquí lo que importa no es cuánto es sino que te toca. Pasarlo como
 * `amount` convertiría la lista en una retícula de cuadrados y mezclaría siete
 * módulos en piezas que compiten entre sí.
 *
 * La píldora dice de qué módulo viene cada cosa. Sin ella, siete fuentes juntas
 * se leerían como una lista plana y se perdería justo lo que hace valiosa a la
 * sección: que la aplicación miró en todas partes.
 */
