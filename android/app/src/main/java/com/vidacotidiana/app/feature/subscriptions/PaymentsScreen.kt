package com.vidacotidiana.app.feature.subscriptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ResourceListScreen
import com.vidacotidiana.app.core.ui.components.plural
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.YearMonth

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

    val entries = payments.map {
        ResourceEntry(
            id = it.id,
            title = it.name,
            subtitle = "${it.category} · toca el ${it.renewsLabel}",
            typeTag = it.category,
            amount = it.amountLabel,
            icon = Icons.Outlined.Autorenew,
            onEdit = { viewModel.requestEdit(CreatableResource.PAYMENT, it.id) },
            // «Ya lo pagué» es `POST /subscriptions/{id}/payments`: registra el
            // pago del ciclo y avanza la próxima fecha. No es un borrado.
            onComplete = { viewModel.completeResource(CreatableResource.PAYMENT, it.id) },
            completeLabel = "Ya lo pagué",
            onDelete = { viewModel.deleteResource(CreatableResource.PAYMENT, it.id) },
        )
    }

    val next = payments.firstOrNull { !it.renewsOn.isBefore(today) }

    ResourceListScreen(
        title = "Pagos",
        subtitle = "Lo que pagas cada mes, y cuándo.",
        eyebrow = plural(entries.size, "compromiso", "compromisos"),
        entries = entries,
        filters = listOf("Todos", "Este mes", "Vencidos", "Tarjetas", "Créditos"),
        metrics = listOfNotNull(
            Triple("Compromisos", entries.size.toString(), "activos"),
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
