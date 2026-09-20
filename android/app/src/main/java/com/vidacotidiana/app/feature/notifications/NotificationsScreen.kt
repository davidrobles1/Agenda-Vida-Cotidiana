package com.vidacotidiana.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.attention.AttentionEngine
import com.vidacotidiana.app.core.attention.AttentionItem
import com.vidacotidiana.app.core.attention.AttentionSource
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceShape
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaChipRow
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.icon
import com.vidacotidiana.app.navigation.Routes
import java.time.LocalDate

/**
 * AVISOS.
 *
 * ESTA PANTALLA ERA UNA MAQUETA. Enseñaba cuatro filas escritas a mano —«Te
 * comprometieron con "Llevar el coche al taller"», «Hace 2 horas · userb»— que
 * no salían de ningún dato del usuario. De ahí venían las dos cosas que
 * faltaban: no se podía ir al recurso porque no había recurso detrás, y no
 * había leído/sin leer porque no había nada que leer.
 *
 * Ahora la fuente es `AttentionEngine`, el MISMO motor que alimenta Inicio,
 * Atención, El Portal y Tu día. Eso es deliberado: dos listas de «lo que
 * reclama» calculadas por separado acaban discrepando, y la que se equivoca es
 * siempre la que nadie mira. Aquí no hay cálculo propio — solo una lectura
 * distinta del mismo conjunto.
 *
 * DIFERENCIA CON «ATENCIÓN»: aquélla ordena por urgencia y sirve para actuar;
 * ésta ordena por si lo has visto y sirve para enterarte. Por eso el filtro de
 * aquí es «Sin leer» y no «Atrasado».
 */
@Composable
fun NotificationsScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val today = LocalDate.now()
    var filter by remember { mutableStateOf("Sin leer") }

    val items = AttentionEngine.scan(state.data, viewModel.allTasks(), today)
    val sinLeer = items.filterNot { it.noticeKey in state.readNotices }

    // Se poda al entrar, con el conjunto completo de claves vigentes. Es el
    // único sitio que lo conoce, y sin esto lo guardado crecería para siempre.
    LaunchedEffect(items.size) { viewModel.pruneNotices(items.map { it.noticeKey }) }

    val visible = if (filter == "Sin leer") sinLeer else items

    VidaScreen(
        title = "Avisos",
        subtitle = if (sinLeer.isEmpty()) "Nada nuevo." else "${sinLeer.size} sin leer.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        onRefresh = viewModel::refresh,
        refreshing = state.loading,
    ) {
        StaggeredAppear(0) {
            VidaChipRow(listOf("Sin leer", "Todos"), filter, { filter = it })
        }

        if (sinLeer.isNotEmpty()) {
            StaggeredAppear(1) {
                VidaSmallButton(
                    "Marcar todo como leído",
                    { viewModel.markNoticesRead(sinLeer.map { it.noticeKey }) },
                    ghost = true,
                )
            }
        }

        if (visible.isEmpty()) {
            StaggeredAppear(2) {
                EmptyState(
                    // DOS VACÍOS DISTINTOS, porque significan cosas distintas:
                    // no tener nada pendiente es un logro, y haber leído lo que
                    // hay es solo estar al corriente. Un texto único para los
                    // dos habría felicitado por filtrar.
                    title = if (items.isEmpty()) "No hay nada que avisar" else "Todo leído",
                    body = if (items.isEmpty()) {
                        "Cuando una garantía, un pago o un mantenimiento se acerque, aparecerá aquí."
                    } else {
                        "Ya has visto los ${items.size} avisos. Míralos otra vez en «Todos»."
                    },
                    action = if (items.isEmpty()) null else "Ver todos" to { filter = "Todos" },
                )
            }
            return@VidaScreen
        }

        // AGRUPADO POR CUÁNTO APRIETA, no por «Hoy / Antes».
        //
        // Un aviso de Cotidiana no es un mensaje que llegó a una hora: es un
        // estado que se recalcula en cada carga. No existe el instante en que
        // «llegó», así que agrupar por eso obligaba a inventarse un «Hace 2
        // horas» — que es exactamente lo que hacía la maqueta.
        val grupos = linkedMapOf(
            "Ya venció" to visible.filter { it.urgency == AttentionUrgency.OVERDUE },
            "Hoy" to visible.filter { it.urgency == AttentionUrgency.TODAY },
            "Esta semana" to visible.filter { it.urgency == AttentionUrgency.SOON },
        ).filterValues { it.isNotEmpty() }

        var index = 2
        grupos.forEach { (label, grupo) ->
            StaggeredAppear(index++) { Eyebrow("$label · ${grupo.size}") }
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                ResourceBoard(
                    entries = grupo.map { item ->
                        val leido = item.noticeKey in state.readNotices
                        ResourceEntry(
                            id = item.noticeKey,
                            title = item.title,
                            subtitle = listOfNotNull(item.reason, item.amount).joinToString(" · "),
                            icon = item.source.icon(),
                            // El tono dice cuánto aprieta, y lo leído lo apaga:
                            // seguir gritando en rojo algo que ya has visto
                            // convierte el color en ruido.
                            tone = when {
                                leido -> null
                                item.urgency == AttentionUrgency.OVERDUE -> c.error
                                item.urgency == AttentionUrgency.TODAY -> c.warning
                                else -> null
                            },
                            // La píldora dice de qué módulo viene, igual que en
                            // Atención. QUIET cuando ya está leído.
                            pill = item.source.label to if (leido) PillTone.QUIET else PillTone.NEUTRAL,
                            // Marcar leído SIN irse: la palomilla de la fila,
                            // que es el mismo control que en el resto de la
                            // aplicación significa «ya está».
                            onComplete = if (leido) null else {
                                { viewModel.markNoticeRead(item.noticeKey) }
                            },
                            completeLabel = "Marcar leído",
                            onRevert = if (leido) {
                                { viewModel.markNoticeUnread(item.noticeKey) }
                            } else null,
                            revertLabel = "Marcar sin leer",
                        )
                    },
                    // ABRIR EL AVISO LLEVA AL RECURSO, que era lo que faltaba.
                    // Y de paso lo marca leído: si lo has abierto, lo has visto.
                    onOpenDetail = { entry ->
                        grupo.firstOrNull { it.noticeKey == entry.id }?.let { item ->
                            viewModel.markNoticeRead(item.noticeKey)
                            navController.navigate(destinoDe(item))
                        }
                    },
                    shape = ResourceShape.LIST,
                )
            }
        }

        StaggeredAppear(index) {
            Text(
                "Los avisos se calculan a partir de tus registros: no hay una bandeja que se llene, " +
                    "así que uno desaparece solo cuando resuelves lo que lo provoca.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

/**
 * A dónde lleva cada aviso.
 *
 * Al REGISTRO cuando tiene pantalla propia, y a su sección cuando no. Nunca a
 * una ruta inventada: `AttentionSource.route` es la lista de ese módulo y
 * siempre existe, así que el peor caso es aterrizar en la sección correcta en
 * vez de en ningún sitio.
 */
private fun destinoDe(item: AttentionItem): String = when (item.source) {
    AttentionSource.TASK -> Routes.taskRoute(item.resourceId)
    AttentionSource.PAYMENT -> Routes.paymentRoute(item.resourceId)
    AttentionSource.MAINTENANCE -> Routes.maintenanceRoute(item.resourceId)
    // Garantías, seguimientos, rutinas y compartidos no tienen pantalla de
    // detalle todavía; se abre su sección, donde el registro está a la vista.
    AttentionSource.WARRANTY,
    AttentionSource.COMMITMENT,
    AttentionSource.ROUTINE,
    AttentionSource.SHARED -> item.source.route
}
