package com.vidacotidiana.app.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.AttachTo
import com.vidacotidiana.app.core.data.paidThisPeriod
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaAttachments
import com.vidacotidiana.app.core.ui.components.VidaFile
import com.vidacotidiana.app.core.ui.components.VidaDetailHero
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaHeroFigure
import com.vidacotidiana.app.core.ui.components.VidaProperty
import com.vidacotidiana.app.core.ui.components.VidaPropertyBlock
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSectionLabel
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.vidacotidiana.app.core.ui.VidaVocabulary

/**
 * DETALLE DE UN PAGO — ADR-020.
 *
 * ES EL ÚNICO DOMINIO CON IMPORTES, y esta pantalla no lo amplía: muestra qué
 * se paga, cuánto, cuándo y si ya se pagó este periodo. No hay saldo, no hay
 * movimiento, no hay presupuesto y no hay categoría de gasto. Agenda organiza
 * pagos; no lleva la contabilidad de nadie.
 *
 * El héroe lleva el IMPORTE como cifra, no un anillo: en un pago lo que domina
 * es cuánto es, no un porcentaje de avance que no existe.
 *
 * Datos reales de `/api/v1/subscriptions` y sus `payments`.
 */
@Composable
fun PagoDetalleScreen(
    paymentId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    androidx.compose.runtime.LaunchedEffect(paymentId) {
        viewModel.loadAttachments(AttachTo.SUBSCRIPTION, paymentId)
    }

    val payment = state.data.payments.firstOrNull { it.id == paymentId }

    VidaScreen(
        title = "Pago",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            if (payment != null) {
                VidaSmallButton(
                    "Editar",
                    { viewModel.requestEdit(CreatableResource.PAYMENT, paymentId) },
                    ghost = true,
                )
            }
        },
    ) {
        if (payment == null) {
            VidaError(
                title = "No encontramos ese pago",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaScreen
        }

        val paidIds = state.data.paymentRecords.paidThisPeriod(today)
        val paid = payment.id in paidIds
        val days = ChronoUnit.DAYS.between(today, payment.renewsOn)

        // El tono sale del ESTADO real: pagado es verde, vencido sin pagar es
        // rojo —aquí sí, porque un pago que se pasó está mal—, y lo que viene
        // en unos días es aviso.
        val tone = when {
            paid -> c.success
            days < 0 -> c.error
            days <= 2 -> c.warning
            else -> c.primary
        }
        val estado = when {
            paid -> "Pagado este periodo"
            days < 0 -> "Se pagaba hace ${-days} días"
            days == 0L -> "Se paga hoy"
            else -> "Se paga en $days días"
        }

        StaggeredAppear(0) {
            VidaDetailHero(
                tone = tone,
                eyebrow = payment.category,
                title = payment.name,
                subtitle = estado,
                figure = payment.amountLabel?.let { { VidaHeroFigure(it) } },
            )
        }

        StaggeredAppear(1) {
            VidaPropertyBlock(
                listOf(
                    VidaProperty(
                        "Estado",
                        if (paid) "Pagado" else "Pendiente",
                        if (paid) c.successContainer else c.warningContainer,
                        if (paid) c.successText else c.warningText,
                    ),
                    VidaProperty(
                        "Ciclo",
                        // El encabezado ya decía «MENSUAL» y esta fila decía
                        // «MONTHLY»: el mismo valor traducido arriba y crudo
                        // abajo, a dos centímetros.
                        payment.billingCycle?.let(VidaVocabulary::human) ?: "Sin ciclo",
                        c.primaryContainer,
                        c.primaryDeep,
                        onClick = { viewModel.requestEdit(CreatableResource.PAYMENT, paymentId) },
                    ),
                    VidaProperty("Próximo", payment.renewsLabel, c.sunken, c.textSecondary),
                ) + listOfNotNull(
                    // El día de corte solo existe en las tarjetas: enseñarlo
                    // vacío en una suscripción sería inventar una propiedad.
                    payment.statementDay?.let {
                        VidaProperty("Día de corte", "Día $it", c.secondContainer, c.second)
                    },
                    payment.currency?.let { VidaProperty("Divisa", it, c.sunken, c.textSecondary) },
                ),
            )
        }

        /* ── LOS CICLOS YA PAGADOS ───────────────────────────────────────── */
        StaggeredAppear(2) {
            val records = state.data.paymentRecords.filter { it.subscriptionId == payment.id }
            VidaSectionLabel("Pagos registrados", trailing = "${records.size}")
        }
        StaggeredAppear(3) {
            val records = state.data.paymentRecords
                .filter { it.subscriptionId == payment.id }
                .sortedByDescending { it.paidOn }
            if (records.isEmpty()) {
                Text(
                    "Todavía no has registrado ningún pago de este compromiso.",
                    style = t.body,
                    color = c.textTertiary,
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(c.surfaceVariant, RoundedCornerShape(VidaTheme.spec.radii.card))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    records.take(6).forEach { record ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            androidx.compose.foundation.layout.Box(
                                Modifier
                                    .size(22.dp)
                                    .background(c.success, RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = c.onPrimary,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            Text(
                                record.paidOn,
                                style = t.body,
                                color = c.text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(payment.amountLabel.orEmpty(), style = t.caption, color = c.textSecondary)
                        }
                    }
                }
            }
        }

        /* ── ADJUNTOS — la misma mecánica genérica de V37 ────────────────── */
        //
        // El recibo de un pago es justo el documento que se busca al mes
        // siguiente. `AttachTo.SUBSCRIPTION` ya existía y el endpoint también;
        // lo que faltaba era esta sección, igual que en tarea y mantenimiento.
        StaggeredAppear(4) { VidaSectionLabel("Adjuntos") }
        StaggeredAppear(5) {
            val files = if (state.attachmentsFor == paymentId) state.attachments else emptyList()
            val attachedIds = files.map { it.id }.toSet()
            val docs = state.data.documents.filterNot { it.id in attachedIds }
            VidaAttachments(
                attached = files.map {
                    VidaFile(
                        it.id, it.name,
                        "${it.contentType.substringAfterLast('/').uppercase()} · ${it.sizeBytes / 1024} KB",
                        it.isImage,
                    )
                },
                loose = docs.filter { it.resourceType == null }.map {
                    VidaFile(it.id, it.name, "${it.category} · ${it.sizeLabel}", false)
                },
                hangingElsewhere = docs.count { it.resourceType != null },
                loading = state.attachmentsLoading,
                onAttach = { viewModel.attachDocument(it, AttachTo.SUBSCRIPTION, paymentId) },
                onDetach = { viewModel.detachDocument(it, AttachTo.SUBSCRIPTION, paymentId) },
            )
        }

        StaggeredAppear(6) {
            VidaSmallButton(
                if (paid) "Ya registrado este periodo" else "Marcar pagado",
                { if (!paid) viewModel.completeResource(CreatableResource.PAYMENT, payment.id) },
                enabled = !paid,
            )
        }

        /* ── EL LÍMITE, DICHO EN VOZ ALTA ────────────────────────────────── */
        StaggeredAppear(7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceElevated, RoundedCornerShape(20.dp))
                    .padding(horizontal = 17.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Los importes viven solo en Pagos (ADR-020). Agenda organiza compromisos de pago; " +
                        "no lleva saldos, movimientos ni presupuestos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }
}
