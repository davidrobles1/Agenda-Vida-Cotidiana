package com.vidacotidiana.app.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.app.Dialogo
import androidx.compose.foundation.ExperimentalFoundationApi
import com.vidacotidiana.app.core.data.AttachTo
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.data.paidThisPeriod
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.core.ui.components.DetailAttachments
import com.vidacotidiana.app.core.ui.components.DetailZoneCard
import com.vidacotidiana.app.core.ui.components.DetailZoneTitle
import com.vidacotidiana.app.core.ui.components.DockAction
import com.vidacotidiana.app.core.ui.components.InfoEdit
import com.vidacotidiana.app.core.ui.components.InfoItem
import com.vidacotidiana.app.core.ui.components.MoreAction
import com.vidacotidiana.app.core.ui.components.VidaDetailDock
import com.vidacotidiana.app.core.ui.components.VidaDetailHeroCard
import com.vidacotidiana.app.core.ui.components.VidaDetailPage
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaInfoSection
import com.vidacotidiana.app.core.ui.components.infoItemsFor
import com.vidacotidiana.app.core.ui.components.infoRefsOf
import com.vidacotidiana.app.core.ui.components.rememberDetailSnack
import com.vidacotidiana.app.core.ui.components.rememberInlineSaver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * DETALLE DE UN PAGO — ADR-020.
 *
 * ES EL ÚNICO DOMINIO CON IMPORTES, y esta pantalla no lo amplía: muestra qué
 * se paga, cuánto, cuándo y si ya se pagó este periodo. No hay saldo, no hay
 * movimiento, no hay presupuesto y no hay categoría de gasto.
 *
 * «VER TODO, EDITAR EN EL SITIO» (`VidaDetailKit`): el título y cada valor
 * llevan su lápiz y se guardan ahí mismo; «Ya pagué» vive en la barra fija de
 * abajo, a un toque desde cualquier punto del scroll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagoDetalleScreen(
    paymentId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val today = LocalDate.now()
    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, CreatableResource.PAYMENT, paymentId, snack)
    val acceso = rememberResourceAccess(viewModel)
    val scope = rememberCoroutineScope()
    val attachmentsView = remember { BringIntoViewRequester() }

    LaunchedEffect(paymentId) { viewModel.loadAttachments(AttachTo.SUBSCRIPTION, paymentId) }

    val payment = state.data.payments.firstOrNull { it.id == paymentId }
    if (payment == null) {
        VidaDetailPage(title = "Pago", onBack = { navController.popBackStack() }, snack = snack) {
            VidaError(
                title = "No encontramos ese pago",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
        }
        return
    }

    val paid = payment.id in state.data.paymentRecords.paidThisPeriod(today)
    val days = ChronoUnit.DAYS.between(today, payment.renewsOn)
    val busy = payment.id in state.busy
    // El tono sale del ESTADO real: pagado es verde, vencido sin pagar es rojo
    // —aquí sí, porque un pago que se pasó está mal—, lo cercano es aviso.
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
        days == 1L -> "Se paga mañana"
        else -> "Se paga en $days días"
    }
    val values = viewModel.editSnapshot(CreatableResource.PAYMENT, paymentId)?.first.orEmpty()
    val refs = infoRefsOf(state, viewModel)
    // ADR-019: el detalle sigue las reglas del módulo DEL REGISTRO. En Personal
    // se comparte con Familia (ADR-025); en Laboral no hay Familia, y este tipo
    // no tiene persona a la que vincularse, así que no se ofrece compartir.
    val module = viewModel.moduleOf(CreatableResource.PAYMENT, paymentId)
    val personal = module == AppContext.PERSONAL

    VidaDetailPage(
        title = "Pago",
        onBack = { navController.popBackStack() },
        snack = snack,
        dock = {
            VidaDetailDock(
                primary = when {
                    paid -> DockAction("Pagado", Icons.Outlined.CheckCircle, done = true, enabled = false) {}
                    busy -> DockAction("Registrando…", Icons.Outlined.CheckCircle, enabled = false) {}
                    else -> DockAction("Ya pagué", Icons.Outlined.CheckCircle) {
                        viewModel.completeResource(CreatableResource.PAYMENT, payment.id)
                    }
                },
                secondary = listOfNotNull(
                    DockAction("Compartir", Icons.Outlined.Share) {
                        acceso.abrir(ShareableKind.SUBSCRIPTION, paymentId, payment.name)
                    }.takeIf { personal },
                    DockAction("Adjuntar", Icons.Outlined.AttachFile) {
                        scope.launch { attachmentsView.bringIntoView() }
                    },
                ),
                more = listOfNotNull(
                    MoreAction(
                        "Deshacer el último pago", Icons.AutoMirrored.Outlined.Undo, "Si lo marcaste por error",
                    ) { viewModel.revertResource(CreatableResource.PAYMENT, payment.id) }.takeIf { paid },
                    MoreAction(
                        "Eliminar este pago", Icons.Outlined.DeleteOutline, "Se pide confirmación",
                        danger = true,
                        confirm = "«${payment.name}» y sus pagos registrados se borrarán para siempre.",
                    ) {
                        viewModel.deleteResource(CreatableResource.PAYMENT, payment.id)
                        navController.popBackStack()
                    },
                ),
            )
        },
    ) {
        VidaDetailHeroCard(
            section = VidaSections.Pagos,
            eyebrow = listOfNotNull(
                module.label,
                payment.category.takeIf { it.isNotBlank() },
                payment.billingCycle?.let(VidaVocabulary::human),
            ).joinToString(" · ").ifBlank { "Pago" },
            title = payment.name,
            onRename = { saver.save("service", "Nombre", it, payment.name, flashKey = "title") },
            onEmptyTitle = { snack.show("El nombre no puede quedar vacío") },
            figure = payment.amountLabel,
            status = estado,
            statusColor = tone,
        )

        VidaInfoSection(
            items = infoItemsFor(CreatableResource.PAYMENT, values, refs, saver) + listOfNotNull(
                payment.currency?.let { InfoItem("currency", "Divisa", it, InfoEdit.ReadOnly("La trae el servicio")) },
                // El día de corte solo existe en las tarjetas.
                payment.statementDay?.let { InfoItem("statement", "Día de corte", "Día $it", InfoEdit.ReadOnly("De la tarjeta")) },
            ),
            refs = refs,
            flash = saver.flash,
        )

        /* ── LOS CICLOS YA PAGADOS ───────────────────────────────────────── */
        val records = state.data.paymentRecords
            .filter { it.subscriptionId == payment.id }
            .sortedByDescending { it.paidOn }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailZoneTitle("Pagos registrados", records.size.toString())
            DetailZoneCard {
                if (records.isEmpty()) {
                    Text(
                        "Todavía no has registrado ningún pago de este compromiso.",
                        style = VidaTheme.type.body,
                        color = c.textTertiary,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
                records.take(6).forEachIndexed { i, record ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(20.dp).background(c.success, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(12.dp))
                        }
                        Text(record.paidOn, style = VidaTheme.type.body, color = c.text, modifier = Modifier.weight(1f))
                        Text(
                            payment.amountLabel.orEmpty(),
                            style = TextStyle(fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                            color = c.text,
                        )
                    }
                }
            }
        }

        DetailAttachments(state, viewModel, AttachTo.SUBSCRIPTION, paymentId, Modifier.bringIntoViewRequester(attachmentsView))

        /* ── EL LÍMITE, DICHO EN VOZ ALTA (ADR-020) ──────────────────────── */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.sunken, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
            Text(
                "Los importes viven solo en Pagos (ADR-020). Agenda organiza compromisos de pago; " +
                    "no lleva saldos, movimientos ni presupuestos.",
                style = VidaTheme.type.caption,
                color = c.textSecondary,
            )
        }
    }

    acceso.Dialogo(viewModel)
}
