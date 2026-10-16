package com.vidacotidiana.app.feature.tasks

import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.core.app.ReferenceSource
import com.vidacotidiana.app.core.ui.components.infoLabel
import com.vidacotidiana.app.core.ui.components.infoValue
import com.vidacotidiana.app.core.ui.components.rememberInfoOpener
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.AttachTo
import com.vidacotidiana.app.core.data.percentDone
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaMark
import com.vidacotidiana.app.core.ui.components.VidaPill
import com.vidacotidiana.app.core.ui.components.VidaProp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.VidaTextField
import com.vidacotidiana.app.core.ui.components.VidaAttachments
import com.vidacotidiana.app.core.ui.components.VidaFile
import com.vidacotidiana.app.core.ui.components.VidaTick
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.core.ui.components.vidaSoftShadow
import com.vidacotidiana.app.core.ui.VidaDates
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.app.Dialogo
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.ExperimentalFoundationApi
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.VidaSections
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
import com.vidacotidiana.app.core.ui.components.VidaInfoSection
import com.vidacotidiana.app.core.ui.components.infoItemsFor
import com.vidacotidiana.app.core.ui.components.infoRefsOf
import com.vidacotidiana.app.core.ui.components.rememberDetailSnack
import com.vidacotidiana.app.core.ui.components.rememberInlineSaver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.vidacotidiana.app.core.ui.components.readOnly
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.foundation.border

/**
 * DETALLE DE UNA TAREA.
 *
 * «VER TODO, EDITAR EN EL SITIO» (`VidaDetailKit`): el título, el día, la hora,
 * el dónde y el detalle llevan su lápiz y se guardan ahí mismo —el día abre el
 * calendario de siempre y la hora el reloj de siempre—. «Completar» vive en la
 * barra fija de abajo. Antes había cuatro botones de texto en la barra de
 * arriba (Acceso, Invitar, Editar, Eliminar) y marcar la tarea como hecha
 * pasaba por tocar la píldora «Estado».
 *
 * EL PORCENTAJE SE DERIVA de los pasos que ya están en memoria.
 * DATOS REALES: `/api/v1/reminders/{id}/steps` (V34).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TareaDetalleScreen(
    taskId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, CreatableResource.TASK, taskId, snack)
    val acceso = rememberResourceAccess(viewModel)
    val scope = rememberCoroutineScope()
    val attachmentsView = remember { BringIntoViewRequester() }

    LaunchedEffect(taskId) {
        viewModel.loadSteps(taskId)
        viewModel.loadAttachments(AttachTo.REMINDER, taskId)
    }

    val task = remember(state.reminders, taskId) {
        viewModel.allTasks().firstOrNull { it.id == taskId }
    }
    val steps = if (state.stepsFor == taskId) state.steps else emptyList()
    val doneCount = steps.count { it.done }

    var newStep by remember(taskId) { mutableStateOf("") }
    fun anadirPaso() {
        val texto = newStep.trim()
        if (texto.isNotBlank()) {
            viewModel.addStep(taskId, texto)
            newStep = ""
        }
    }

    var compartirAbierto by remember { mutableStateOf(false) }
    var destinatario by remember { mutableStateOf("") }
    LaunchedEffect(compartirAbierto) { if (compartirAbierto) viewModel.loadShares(taskId) }

    if (compartirAbierto) {
        AlertDialog(
            onDismissRequest = { compartirAbierto = false },
            title = { Text("Compartir la tarea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                    Text(
                        "Escribe el correo o el nombre de usuario de quien quieras. Le llegará una " +
                            "invitación y podrá ver la tarea cuando la acepte.",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                    )
                    VidaTextField(destinatario, { destinatario = it }, "correo@ejemplo.com o usuario")

                    if (state.invitations.isNotEmpty()) {
                        Text("Invitaciones pendientes", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                        state.invitations.forEach { inv ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    inv.invitedEmail ?: "Invitación",
                                    style = MaterialTheme.typography.bodySmall, color = c.text,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.cancelInvitation(taskId, inv.id) }) { Text("Cancelar") }
                            }
                        }
                    }
                    if (state.shares.isNotEmpty()) {
                        Text("Compartida con", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                        state.shares.forEach { sh ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    sh.status, style = MaterialTheme.typography.bodySmall, color = c.text,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.revokeShare(taskId, sh.id) }) { Text("Quitar") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = destinatario.isNotBlank(),
                    onClick = { viewModel.shareTask(taskId, destinatario) { ok -> if (ok) destinatario = "" } },
                ) { Text("Invitar") }
            },
            dismissButton = { TextButton(onClick = { compartirAbierto = false }) { Text("Cerrar") } },
            containerColor = c.surfaceVariant,
        )
    }


    if (task == null) {
        VidaDetailPage(title = "Tarea", onBack = { navController.popBackStack() }, snack = snack) {
            VidaError(
                title = "No encontramos esa tarea",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
        }
        return
    }

    val tone = when {
        task.done -> c.success
        task.time != null -> c.primary
        else -> c.second
    }
    // Una tarea que no es tuya (compartida contigo) no trae valores editables:
    // se ve entera, sin lápices.
    val snapshot = viewModel.editSnapshot(CreatableResource.TASK, taskId)
    val editable = snapshot != null
    val refs = infoRefsOf(state, viewModel)
    // ADR-019: la tarea sigue las reglas de SU módulo, no del que está activo.
    //  · Personal → se comparte con Familia (ADR-025).
    //  · Laboral  → se vincula a una Persona y a un Proyecto (ADR-016), y a una
    //               reunión se invita por correo (REMINDER_SHARE). Sin Familia.
    // Nunca las dos a la vez.
    val laboral = viewModel.moduleOf(CreatableResource.TASK, taskId) == AppContext.LABORAL
    val opener = rememberInfoOpener()
    val baseValues = snapshot?.first.orEmpty()
    val items = infoItemsFor(CreatableResource.TASK, baseValues, refs, saver, laboral = laboral)

    VidaDetailPage(
        title = "Tarea",
        onBack = { navController.popBackStack() },
        snack = snack,
        dock = {
            VidaDetailDock(
                primary = if (task.done) {
                    DockAction("Completada", Icons.Outlined.CheckCircle, done = true, enabled = false) {}
                } else {
                    DockAction("Completar", Icons.Outlined.CheckCircle) { viewModel.toggleTask(task.id) }
                },
                secondary = listOf(
                    if (laboral) {
                        DockAction("Persona", Icons.Outlined.PersonAddAlt, enabled = editable) { opener.open("personId") }
                    } else {
                        DockAction("Compartir", Icons.Outlined.Share) {
                            acceso.abrir(ShareableKind.REMINDER, taskId, task.title)
                        }
                    },
                    DockAction("Adjuntar", Icons.Outlined.AttachFile) {
                        scope.launch { attachmentsView.bringIntoView() }
                    },
                ),
                more = listOfNotNull(
                    MoreAction("Reabrir la tarea", Icons.Outlined.Replay, "Vuelve a quedar pendiente") {
                        viewModel.reopenTask(task.id)
                    }.takeIf { task.done },
                    MoreAction("Invitar por correo", Icons.Outlined.Mail, "A una reunión, aunque no tenga cuenta") {
                        compartirAbierto = true
                    }.takeIf { laboral },
                    MoreAction(
                        "Eliminar la tarea", Icons.Outlined.DeleteOutline, "Se pide confirmación",
                        danger = true,
                        confirm = "«${task.title}» se borrará para siempre, con sus pasos. No se puede deshacer.",
                    ) {
                        viewModel.deleteResource(CreatableResource.TASK, taskId)
                        navController.popBackStack()
                    },
                ),
            )
        },
    ) {
        VidaDetailHeroCard(
            section = VidaSections.Tareas,
            eyebrow = listOfNotNull(
                if (laboral) AppContext.LABORAL.label else AppContext.PERSONAL.label,
                "Tarea",
                "Compartida".takeIf { task.shared },
            ).joinToString(" · "),
            title = task.title,
            onRename = if (editable) {
                { saver.save("title", "Título", it, task.title, flashKey = "title") }
            } else null,
            onEmptyTitle = { snack.show("El título no puede quedar vacío") },
            figure = task.time?.toString()?.take(5),
            figureCaption = task.date?.let { com.vidacotidiana.app.core.ui.components.infoDate(it).substringBefore(" ·").lowercase() },
            status = when {
                task.done -> "Hecha"
                steps.isNotEmpty() -> "Pendiente · $doneCount de ${steps.size} pasos"
                else -> "Pendiente"
            },
            statusColor = tone,
        )

        VidaInfoSection(
            items = if (editable) items else items.readOnly(),
            refs = refs,
            flash = saver.flash,
            opener = opener,
        )

        /* ── PASOS ───────────────────────────────────────────────────────── */
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailZoneTitle("Pasos", if (steps.isEmpty()) null else "$doneCount/${steps.size}")
            when {
                state.stepsError != null && steps.isEmpty() -> VidaError(
                    title = "No se pudieron cargar los pasos",
                    body = state.stepsError ?: "",
                    onRetry = { viewModel.loadSteps(taskId) },
                )
                state.stepsLoading && steps.isEmpty() -> LoadingRows(2)
                else -> DetailZoneCard {
                    if (steps.isEmpty()) {
                        Text(
                            "Una tarea no necesita pasos. Añádelos solo si partirla en trozos te ayuda.",
                            style = VidaTheme.type.body,
                            color = c.textSecondary,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    steps.forEachIndexed { i, step ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .vidaClickable(onClick = { viewModel.toggleStep(taskId, step.id) })
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            VidaTick(checked = step.done, tone = tone)
                            Text(
                                step.title,
                                style = VidaTheme.type.body,
                                color = if (step.done) c.textTertiary else c.text,
                                textDecoration = if (step.done) {
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                } else null,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Borrar paso",
                                tint = c.textTertiary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .vidaClickable(onClick = { viewModel.deleteStep(taskId, step.id) }),
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(
                        Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            VidaTextField(newStep, { newStep = it }, "Añadir paso", onDone = { anadirPaso() })
                        }
                        Box(
                            modifier = Modifier
                                .size(VidaLayout.touchTarget)
                                .background(
                                    if (newStep.isBlank()) c.sunken else com.vidacotidiana.app.core.ui.components.vidaBrandInk(),
                                    RoundedCornerShape(14.dp),
                                )
                                .vidaClickable(onClick = { anadirPaso() }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Añadir",
                                tint = if (newStep.isBlank()) c.textTertiary else c.surfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            state.stepsError?.takeIf { steps.isNotEmpty() }?.let { msg ->
                Text(msg, style = VidaTheme.type.caption, color = c.error)
            }
        }

        DetailAttachments(state, viewModel, AttachTo.REMINDER, taskId, Modifier.bringIntoViewRequester(attachmentsView))
    }

    acceso.Dialogo(viewModel)
}

