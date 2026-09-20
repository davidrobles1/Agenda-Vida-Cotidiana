package com.vidacotidiana.app.feature.subscriptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.paidThisPeriod
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.app.sliceError
import com.vidacotidiana.app.core.ui.VidaTones
import androidx.compose.ui.graphics.Brush
import com.vidacotidiana.app.core.ui.components.ResourceShape
import com.vidacotidiana.app.core.ui.VidaDates
import com.vidacotidiana.app.core.ui.VidaVocabulary

/**
 * Pagos (ADR-020). Es destino prioritario de la barra inferior en Personal, así
 * que su barra superior muestra el menú, no una flecha de retroceso.
 *
 * Los importes viven aquí y solo aquí: ADR-020 los acota a esta sección y no
 * los convierte en una capacidad transversal del producto. Esta pantalla
 * responde «qué pagos tengo, cuánto representan y cuándo» — no suma saldos ni
 * clasifica gasto, que es justo lo que sigue prohibido.
 */
@Composable
fun PaymentsScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val payments = state.data.payments.sortedBy { it.renewsOn }
    val today = LocalDate.now()
    val thisMonth = YearMonth.from(today)
    // ADR-020(f): qué compromisos ya cubrí este mes. Sin este dato la pantalla
    // no podía distinguir lo pagado de lo pendiente —la Web sí— y su total del
    // mes sumaba las dos cosas como si nada estuviera pagado.
    val paid = state.data.paymentRecords.paidThisPeriod(today)

    val entries = payments.map {
        val isPaid = it.id in paid
        ResourceEntry(
            id = it.id,
            title = it.name,
            // El antetítulo es el TIPO de compromiso, como en el artefacto
            // («SERVICIO», «TARJETA», «CRÉDITO»), y el subtítulo dice cuándo.
            // Antes los dos mostraban `category` —el ciclo— así que la fila
            // repetía «Mensual» dos veces y el subtítulo se cortaba.
            // El ciclo, como en el artefacto («Servicio · mensual»). El
            // CUÁNDO ya lo dice el dato del antetítulo, así que repetir la
            // fecha aquí solo servía para que se cortara.
            subtitle = it.billingCycle?.let(VidaVocabulary::human) ?: it.category,
            typeTag = kindLabel(it.kind),
            busy = it.id in state.busy,
            // A la derecha del antetítulo, cuánto falta o cuánto lleva
            // esperando, en el tono de la fila. Es el `p.day` del artefacto.
            highlight = VidaDates.relative(it.renewsOn, today).lowercase(),
            amount = it.amountLabel,
            icon = Icons.Outlined.Autorenew,
            // EL TONO SALE DEL TIPO DE COMPROMISO, como en el artefacto: un
            // servicio, una tarjeta y un crédito se reconocen por su color
            // antes de leerlos. Antes las seis filas compartían el mismo, y la
            // lista perdía justo esa lectura de un vistazo.
            // …salvo cuando ya venció: en el artefacto el pago atrasado va en
            // rosa aunque su categoría sea otra. La urgencia manda sobre la
            // categoría, porque es lo único que exige actuar hoy.
            tone = when {
                isPaid -> VidaTheme.colors.successText
                it.renewsOn.isBefore(today) -> VidaTheme.colors.error
                else -> VidaTones.payment(it.kind).accent
            },
            // Lo ya pagado deja de pedir acción: se marca y pierde el botón.
            pill = if (isPaid) "Pagado" to PillTone.OK else null,
            // Un pago se organiza por proximidad: lo vencido primero, porque
            // es lo único que exige actuar hoy.
            group = when {
                isPaid -> "Pagados"
                it.renewsOn.isBefore(today) -> "Vencidos"
                YearMonth.from(it.renewsOn) == thisMonth -> "Este mes"
                else -> "Más adelante"
            },
            onEdit = { viewModel.requestEdit(CreatableResource.PAYMENT, it.id) },
            // «Ya lo pagué» es `POST /subscriptions/{id}/payments`: registra el
            // pago del ciclo y avanza la próxima fecha. No es un borrado.
            // Se retira en lo ya pagado: volver a pulsarlo registraría un
            // segundo ciclo y adelantaría la fecha un mes de más.
            onComplete = if (isPaid) null else {
                { viewModel.completeResource(CreatableResource.PAYMENT, it.id) }
            },
            completeLabel = "Ya lo pagué",
            // Justo lo que faltaba: registrar un pago adelanta la fecha un
            // ciclo entero, así que un toque de más dejaba el pago perdido un
            // mes en el futuro sin forma de traerlo. `DELETE .../payments/last`
            // borra el registro Y devuelve la fecha.
            onRevert = if (isPaid) {
                { viewModel.revertResource(CreatableResource.PAYMENT, it.id) }
            } else null,
            revertLabel = "No lo pagué",
            onDelete = { viewModel.deleteResource(CreatableResource.PAYMENT, it.id) },
        )
    }

    val next = payments.firstOrNull { !it.renewsOn.isBefore(today) }

    /**
     * ADR-020: la suma de los compromisos del periodo está DENTRO del alcance
     * («qué pagos tengo, cuánto representan y cuándo»). Lo que sigue prohibido
     * son saldos, presupuestos, categorías de gasto y reportes: esto es un
     * total, no un tablero financiero.
     *
     * Regla del periodo copiada de la Web (`summarize` en `paymentsView.ts`):
     * cuenta lo que vence ESTE MES **o antes**, comparando la clave `YYYY-MM`.
     * Incluir lo atrasado es deliberado: un pago que venció en agosto y sigue
     * sin pagarse pesa este mes, y dejarlo fuera haría que el total mintiera.
     */
    val periodKey = YearMonth.from(today)
    val inPeriod = payments.filter { YearMonth.from(it.renewsOn) <= periodKey }

    // Se agrupa por divisa porque sumar pesos con dólares daría una cifra que
    // no significa nada. Se muestra la divisa de mayor peso.
    val totalsByCurrency = inPeriod
        .filter { !it.variableAmount && it.amount != null }
        .groupBy { it.currency ?: "MXN" }
        .mapValues { (_, list) -> list.sumOf { it.amount ?: 0.0 } }
        .toList()
        .sortedByDescending { it.second }

    // Los de importe variable —una tarjeta antes de su corte— no tienen cifra
    // todavía. Se cuentan aparte para que el total no aparente ser completo.
    val sinImporte = inPeriod.count { it.variableAmount || it.amount == null }

    // PENDIENTES: lo del periodo que todavía no está pagado, y cuánto de eso ya
    // venció. Es la mitad que le faltaba al resumen — el "Total del mes" dice
    // cuánto cuesta el mes, y esto cuánto queda por cubrir.
    val pending = inPeriod.filter { it.id !in paid }
    val overdue = pending.count { it.renewsOn.isBefore(today) }
    val pendingByCurrency = pending
        .filter { !it.variableAmount && it.amount != null }
        .groupBy { it.currency ?: "MXN" }
        .mapValues { (_, list) -> list.sumOf { it.amount ?: 0.0 } }
        .toList()
        .sortedByDescending { it.second }

    ResourceListScreen(
        // El artefacto le da pantalla propia a este registro.
        onOpenRoute = { navController.navigate(Routes.paymentRoute(it.id)) },
        title = "Pagos",
        subtitle = "Lo que pagas cada mes, y cuándo.",
        eyebrow = plural(entries.size, "compromiso", "compromisos"),
        entries = entries,
        filters = listOf("Todos", "Pendientes", "Este mes", "Vencidos", "Tarjetas", "Créditos"),
        // LA RETÍCULA DEL ARTEFACTO en lugar de la tira que se desplazaba.
        //
        // Dos piezas y no cuatro: en Pagos lo que manda es cuánto hay este mes,
        // y a su lado cuánto queda sin pagar. La tira anterior dejaba la
        // segunda cifra fuera de pantalla, y una cifra que hay que desplazar
        // para ver no cumple su función.
        //
        // ADR-020: importes SOLO aquí. Esta retícula no habilita saldos,
        // movimientos ni presupuestos.
        header = {
            val totalLabel = totalsByCurrency.firstOrNull()
                ?.let { (currency, total) -> formatTotal(total, currency) } ?: "—"
            VidaTileRow(
                listOf(
                    // La pieza principal del artefacto va en DEGRADADO índigo
                    // con texto blanco, no en relleno pálido: es la cifra que
                    // manda en la pantalla y su peso visual lo dice.
                    VidaTileSpec(
                        "Este mes", totalLabel,
                        plural(entries.size, "compromiso", "compromisos"),
                        VidaTheme.colors.primary,
                        VidaTheme.colors.onPrimary,
                        VidaTheme.colors.onPrimary,
                        weight = 1.32f,
                        backgroundBrush = Brush.linearGradient(
                            listOf(VidaTheme.colors.primary, VidaTheme.colors.primaryDeep),
                        ),
                    ),
                    VidaTileSpec(
                        "Sin pagar", pending.size.toString(),
                        if (pending.isEmpty()) "todo al día" else "este periodo",
                        if (pending.isEmpty()) VidaTheme.colors.successContainer else VidaTheme.colors.errorContainer,
                        if (pending.isEmpty()) VidaTheme.colors.successText else VidaTheme.colors.error,
                        if (pending.isEmpty()) VidaTheme.colors.successText else VidaTheme.colors.error,
                    ),
                ),
            )
        },
        // FILA, como el artefacto: el nombre manda y el importe se alinea a la
        // derecha para poder compararlo entre filas. Con la forma deducida, el
        // importe se convertía en el titular y el nombre pasaba a segunda línea.
        shape = ResourceShape.LIST,
        addLabel = "Agregar pago",
        emptyBody = "Registra un pago para saber cuándo toca y cuánto representa.",
        loading = state.loading,
        error = state.sliceError(DataSlice.PAYMENTS),
        onRetry = viewModel::refresh,
        onAdd = { viewModel.requestCreate(CreatableResource.PAYMENT) },
        // Los chips de Pagos no miran una píldora: cruzan la fecha con hoy y el
        // tipo de pago que el registro ya trae.
        matchesFilter = { entry, f ->
            val payment = payments.firstOrNull { it.id == entry.id }
            when {
                f == "Todos" || payment == null -> true
                // Lo que aún hay que cubrir, que es lo que se busca al abrir
                // Pagos. Antes no había forma de aislarlo.
                f == "Pendientes" -> payment.id !in paid
                f == "Este mes" -> YearMonth.from(payment.renewsOn) == thisMonth
                f == "Vencidos" -> payment.renewsOn.isBefore(today)
                // Los dos valores reales de `PaymentKind` que ADR-020(d)
                // distingue estructuralmente del resto.
                f == "Tarjetas" -> payment.kind == "CARD"
                f == "Créditos" -> payment.kind == "CREDIT"
                else -> true
            }
        },
        drawerState = drawerState,
        scope = scope,
        showBack = false,
        onBack = {},
        // La campana lleva de verdad a los avisos, y el punto sale de
        // cuántos quedan sin leer. Antes era `{}` con `badge = true`.
        onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        notificationsBadge = viewModel.avisosSinLeer(),
    )
}

/** Sin decimales cuando no los hay: «$2,340 MXN» se lee mejor que «$2,340.00». */
private fun formatTotal(total: Double, currency: String): String {
    val figure = if (total % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", total)
    } else {
        String.format(Locale.US, "%,.2f", total)
    }
    return "$" + figure + " " + currency
}

/** El TIPO de compromiso, escrito como lo rotula el artefacto. */
private fun kindLabel(kind: String?): String = when (kind?.trim()?.uppercase()) {
    "SERVICE" -> "SERVICIO"
    "SUBSCRIPTION" -> "DIGITAL"   // la palabra del artefacto para lo que se renueva solo
    "MEMBERSHIP" -> "SOCIO"
    "CARD" -> "TARJETA"
    "CREDIT" -> "CRÉDITO"
    else -> "PAGO"
}
