package com.vidacotidiana.app.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.AttachTo
import com.vidacotidiana.app.core.data.MaintenanceStatus
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaDatePair
import com.vidacotidiana.app.core.ui.components.VidaDetailHero
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaHeroFigure
import com.vidacotidiana.app.core.ui.components.VidaProperty
import com.vidacotidiana.app.core.ui.components.VidaPropertyBlock
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaAttachments
import com.vidacotidiana.app.core.ui.components.VidaFile
import com.vidacotidiana.app.core.ui.components.VidaSectionLabel
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.vidacotidiana.app.core.ui.VidaDates

/**
 * DETALLE DE UN MANTENIMIENTO.
 *
 * Las alertas de ADR-018 (7, 3 y 0 días) se DERIVAN de `nextDueOn`: no hay una
 * lista de avisos que alguien escriba. El tono del héroe sale del estado real,
 * y la severidad alta va en ÁMBAR, no en rojo — el rojo queda para lo que está
 * mal, y un mantenimiento que toca pronto no lo está.
 *
 * Los adjuntos usan la mecánica genérica (V37), la misma de tareas: no hay un
 * almacén de archivos por módulo.
 */
@Composable
fun MantDetalleScreen(
    recordId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val today = LocalDate.now()

    LaunchedEffect(recordId) { viewModel.loadAttachments(AttachTo.MAINTENANCE, recordId) }

    val record = state.data.maintenance.firstOrNull { it.id == recordId }

    VidaScreen(
        title = "Mantenimiento",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            if (record != null) {
                VidaSmallButton(
                    "Editar",
                    { viewModel.requestEdit(CreatableResource.MAINTENANCE, recordId) },
                    ghost = true,
                )
            }
        },
    ) {
        if (record == null) {
            VidaError(
                title = "No encontramos ese mantenimiento",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaScreen
        }

        val days = ChronoUnit.DAYS.between(today, record.nextDueOn)
        // HECHO y AL_DIA no son lo mismo y esta pantalla los confundía: usaba
        // AL_DIA como «ya está hecho» y por eso deshabilitaba «Marcar hecho»
        // sobre cualquier mantenimiento que todavía no tocase. Adelantar un
        // cambio de aceite es normal, y el backend lo contempla —si la fecha ya
        // pasó cuenta el siguiente intervalo desde hoy.
        val terminado = record.status == MaintenanceStatus.HECHO
        val tone = when {
            terminado -> c.success
            days < 0 -> c.warning   // atrasado es AVISO, no error
            days <= 3 -> c.warning
            else -> c.primary
        }
        val estado = when {
            terminado -> "Terminado"
            days < 0 -> "Tocaba hace ${-days} días"
            days == 0L -> "Toca hoy"
            else -> "Toca en $days días"
        }

        StaggeredAppear(0) {
            VidaDetailHero(
                tone = tone,
                eyebrow = record.task,
                title = record.item,
                subtitle = estado,
                figure = { VidaHeroFigure(if (days < 0) "${-days}d" else "${days}d") },
            )
        }

        StaggeredAppear(1) {
            VidaPropertyBlock(
                listOfNotNull(
                    VidaProperty(
                        "Estado",
                        when (record.status) {
                            MaintenanceStatus.HECHO -> "Terminado"
                            MaintenanceStatus.AL_DIA -> "Al día"
                            MaintenanceStatus.PROXIMO -> "Próximo"
                            MaintenanceStatus.VENCIDO -> "Toca ya"
                        },
                        if (terminado) c.successContainer else c.warningContainer,
                        if (terminado) c.successText else c.warningText,
                    ),
                    record.intervalMonths?.let {
                        VidaProperty("Cada cuánto", "Cada $it meses", c.primaryContainer, c.primaryDeep)
                    },
                    // El artículo solo si de verdad cuelga de uno: un
                    // mantenimiento suelto no debe mostrar un vínculo vacío.
                    record.inventoryItemId?.let { itemId ->
                        state.data.inventory.firstOrNull { it.id == itemId }?.let { item ->
                            VidaProperty("Artículo", item.name, c.sunken, c.textSecondary)
                        }
                    },
                ),
            )
        }

        StaggeredAppear(2) {
            // LAS DOS MITADES DECÍAN LO MISMO.
            //
            // La izquierda mostraba «08 sept 2026» y la derecha el MISMO día
            // otra vez, pero en crudo —`2026-09-08`, tal como sale de la base—,
            // así que el usuario veía la misma fecha dos veces y una de ellas
            // en un formato que no es de la aplicación. Y el rótulo decía
            // «Próxima» sobre una fecha ya pasada, para desmentirse justo
            // debajo con un «ya pasó»: el título no puede contradecir al dato
            // que titula.
            //
            // Ahora cada mitad responde una pregunta distinta —cuándo toca y
            // cómo está— y el rótulo lo decide la propia fecha.
            VidaDatePair(
                leftLabel = VidaDates.dueTitle(record.nextDueOn, today),
                leftValue = VidaDates.absolute(record.nextDueOn),
                leftCaption = VidaDates.relative(record.nextDueOn, today).lowercase(),
                rightLabel = "Estado",
                rightValue = estado,
                // Vacio cuando el ritmo ya es una propiedad de arriba: si no,
                // «Cada 6 meses» aparecia dos veces en la misma pantalla.
                rightCaption = if (record.intervalMonths != null) "" else "sin ritmo fijo",
                rightTone = tone,
                rightBackground = if (days < 0) c.warningContainer else c.primaryContainer,
            )
        }

        /* ── ADJUNTOS — mecánica genérica V37 ────────────────────────────── */
        StaggeredAppear(3) { VidaSectionLabel("Adjuntos") }
        StaggeredAppear(4) {
            // Aquí se podía SOLTAR pero no COLGAR, igual que en la tarea, y
            // además la fila era otra distinta: sin icono y con el tamaño en
            // crudo. Una sola sección compartida cierra las dos cosas.
            val files = if (state.attachmentsFor == recordId) state.attachments else emptyList()
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
                onAttach = { viewModel.attachDocument(it, AttachTo.MAINTENANCE, recordId) },
                onDetach = { viewModel.detachDocument(it, AttachTo.MAINTENANCE, recordId) },
            )
        }

        StaggeredAppear(5) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement
                    .spacedBy(com.vidacotidiana.app.core.ui.VidaSpacing.sm),
            ) {
                // Volver atrás está SIEMPRE, no solo sobre lo terminado:
                // adelantar un repetible por error avanza su fecha meses
                // enteros, y hasta ahora no había forma de recuperarla.
                VidaSmallButton(
                    if (terminado) "Volver a programarlo" else "Deshacer el último",
                    { viewModel.revertResource(CreatableResource.MAINTENANCE, record.id) },
                    ghost = true,
                    enabled = record.id !in state.busy,
                )
                if (!terminado) {
                    VidaSmallButton(
                        "Marcar hecho",
                        { viewModel.completeResource(CreatableResource.MAINTENANCE, record.id) },
                        enabled = record.id !in state.busy,
                    )
                }
            }
        }
    }
}
