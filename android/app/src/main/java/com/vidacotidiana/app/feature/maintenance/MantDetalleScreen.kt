package com.vidacotidiana.app.feature.maintenance

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
import com.vidacotidiana.app.core.data.MaintenanceStatus
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.core.ui.components.infoValue
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
 * DETALLE DE UN MANTENIMIENTO.
 *
 * Las alertas de ADR-018 se DERIVAN de `nextDueOn`. El tono sale del estado
 * real, y lo atrasado va en ÁMBAR: un mantenimiento que toca no está «mal».
 *
 * «VER TODO, EDITAR EN EL SITIO» (`VidaDetailKit`): cada valor con su lápiz;
 * «Completar» en la barra fija de abajo; «Deshacer el último» y «Eliminar»
 * en «Más».
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MantDetalleScreen(
    recordId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val today = LocalDate.now()
    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, CreatableResource.MAINTENANCE, recordId, snack)
    val acceso = rememberResourceAccess(viewModel)
    val scope = rememberCoroutineScope()
    val attachmentsView = remember { BringIntoViewRequester() }

    LaunchedEffect(recordId) { viewModel.loadAttachments(AttachTo.MAINTENANCE, recordId) }

    val record = state.data.maintenance.firstOrNull { it.id == recordId }
    if (record == null) {
        VidaDetailPage(title = "Mantenimiento", onBack = { navController.popBackStack() }, snack = snack) {
            VidaError(
                title = "No encontramos ese mantenimiento",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
        }
        return
    }

    val days = ChronoUnit.DAYS.between(today, record.nextDueOn)
    // HECHO y AL_DIA no son lo mismo: adelantar un mantenimiento es normal, así
    // que «Completar» solo se apaga sobre uno terminado de verdad.
    val terminado = record.status == MaintenanceStatus.HECHO
    val busy = record.id in state.busy
    val tone = when {
        terminado -> c.success
        days <= 3 -> c.warning   // atrasado es AVISO, no error
        else -> c.primary
    }
    val estado = when {
        terminado -> "Terminado"
        days < 0 -> "Tocaba hace ${-days} días"
        days == 0L -> "Toca hoy"
        days == 1L -> "Toca mañana"
        else -> "Toca en $days días"
    }
    val values = viewModel.editSnapshot(CreatableResource.MAINTENANCE, recordId)?.first.orEmpty()
    val refs = infoRefsOf(state, viewModel)
    // ADR-019: el detalle sigue las reglas del módulo DEL REGISTRO. En Personal
    // se comparte con Familia (ADR-025); en Laboral no hay Familia, y este tipo
    // no tiene persona a la que vincularse, así que no se ofrece compartir.
    val module = viewModel.moduleOf(CreatableResource.MAINTENANCE, recordId)
    val personal = module == AppContext.PERSONAL

    VidaDetailPage(
        title = "Mantenimiento",
        onBack = { navController.popBackStack() },
        snack = snack,
        dock = {
            VidaDetailDock(
                primary = when {
                    terminado -> DockAction("Terminado", Icons.Outlined.CheckCircle, done = true, enabled = false) {}
                    busy -> DockAction("Guardando…", Icons.Outlined.CheckCircle, enabled = false) {}
                    else -> DockAction("Completar", Icons.Outlined.CheckCircle) {
                        viewModel.completeResource(CreatableResource.MAINTENANCE, record.id)
                    }
                },
                secondary = listOfNotNull(
                    DockAction("Compartir", Icons.Outlined.Share) {
                        acceso.abrir(ShareableKind.MAINTENANCE, recordId, record.item)
                    }.takeIf { personal },
                    DockAction("Adjuntar", Icons.Outlined.AttachFile) {
                        scope.launch { attachmentsView.bringIntoView() }
                    },
                ),
                more = listOf(
                    // Volver atrás está SIEMPRE: adelantar un repetible por error
                    // avanza su fecha meses enteros.
                    MoreAction(
                        if (terminado) "Volver a programarlo" else "Deshacer el último",
                        Icons.AutoMirrored.Outlined.Undo,
                        "Devuelve la fecha anterior",
                    ) { viewModel.revertResource(CreatableResource.MAINTENANCE, record.id) },
                    MoreAction(
                        "Eliminar", Icons.Outlined.DeleteOutline, "Se pide confirmación",
                        danger = true,
                        confirm = "«${record.item}» se borrará para siempre.",
                    ) {
                        viewModel.deleteResource(CreatableResource.MAINTENANCE, record.id)
                        navController.popBackStack()
                    },
                ),
            )
        },
    ) {
        VidaDetailHeroCard(
            section = VidaSections.Mantenimiento,
            eyebrow = listOfNotNull(
                module.label,
                "Mantenimiento",
                record.intervalMonths?.let { "Cada $it meses" },
            ).joinToString(" · "),
            title = record.item,
            onRename = { saver.save("item", "Nombre", it, record.item, flashKey = "title") },
            onEmptyTitle = { snack.show("El nombre no puede quedar vacío") },
            figure = when {
                terminado -> "✓"
                days < 0 -> "${-days} días"
                days == 0L -> "Hoy"
                else -> "$days días"
            },
            figureCaption = when {
                terminado -> "al día"
                days < 0 -> "atrasado"
                days == 0L -> "toca"
                else -> "faltan"
            },
            figureTone = if (!terminado && days < 0) c.warningText else null,
            status = estado,
            statusColor = tone,
        )

        VidaInfoSection(
            items = infoItemsFor(CreatableResource.MAINTENANCE, values, refs, saver, format = { f, raw ->
                if (f.key == "intervalMonths") raw.takeIf { it.isNotBlank() }?.let { "Cada $it meses" }
                else infoValue(f, raw, refs)
            }),
            refs = refs,
            flash = saver.flash,
        )

        DetailAttachments(state, viewModel, AttachTo.MAINTENANCE, recordId, Modifier.bringIntoViewRequester(attachmentsView))
    }

    acceso.Dialogo(viewModel)
}
