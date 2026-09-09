package com.vidacotidiana.app.feature.subscriptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            subtitle = "${it.category} · toca el ${it.renewsLabel}",
            typeTag = it.category,
            amount = it.amountLabel,
            icon = Icons.Outlined.Autorenew,
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
        title = "Pagos",
        subtitle = "Lo que pagas cada mes, y cuándo.",
        eyebrow = plural(entries.size, "compromiso", "compromisos"),
        entries = entries,
        filters = listOf("Todos", "Pendientes", "Este mes", "Vencidos", "Tarjetas", "Créditos"),
        metrics = listOfNotNull(
            totalsByCurrency.firstOrNull()?.let { (currency, total) ->
                Triple(
                    "Total del mes",
                    formatTotal(total, currency),
                    // El pie dice cuánto FALTA, no cuántos hay: es la pregunta
                    // que se le hace a un total del mes. Mismo criterio que la
                    // tarjeta equivalente de la Web.
                    when {
                        pending.isEmpty() -> "Todo pagado"
                        pendingByCurrency.isNotEmpty() ->
                            "Te faltan " + pendingByCurrency.first()
                                .let { (c, t) -> formatTotal(t, c) }
                        sinImporte > 0 -> "$sinImporte sin importe"
                        else -> "${inPeriod.size} en el periodo"
                    },
                )
            },
            Triple(
                "Pendientes",
                pending.size.toString(),
                if (overdue > 0) "$overdue vencido${if (overdue == 1) "" else "s"}" else "Ninguno vencido",
            ),
            next?.let { Triple("Más próximo", it.renewsLabel, it.name) },
        ),
        addLabel = "Agregar pago",
        emptyBody = "Registra un pago para saber cuándo toca y cuánto representa.",
        loading = state.loading,
        error = state.error,
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
        onNotifications = {},
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
