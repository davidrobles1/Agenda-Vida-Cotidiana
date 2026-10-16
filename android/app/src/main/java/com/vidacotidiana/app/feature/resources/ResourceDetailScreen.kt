package com.vidacotidiana.app.feature.resources

import com.vidacotidiana.app.navigation.go
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.core.ui.components.rememberInfoOpener
import androidx.compose.material.icons.outlined.PersonAddAlt
import com.vidacotidiana.app.core.ui.components.DetailZoneTitle
import com.vidacotidiana.app.core.ui.components.DockAction
import com.vidacotidiana.app.core.ui.components.MoreAction
import com.vidacotidiana.app.core.ui.components.VidaDetailDock
import com.vidacotidiana.app.core.ui.components.VidaDetailHeroCard
import com.vidacotidiana.app.core.ui.components.VidaDetailPage
import com.vidacotidiana.app.core.ui.components.VidaInfoSection
import com.vidacotidiana.app.core.ui.components.infoItemsFor
import com.vidacotidiana.app.core.ui.components.infoRefsOf
import com.vidacotidiana.app.core.ui.components.infoDate
import com.vidacotidiana.app.core.ui.components.infoLabel
import com.vidacotidiana.app.core.ui.components.rememberDetailSnack
import com.vidacotidiana.app.core.ui.components.rememberInlineSaver
import com.vidacotidiana.app.core.ui.VidaVocabulary
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppUiState
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.Dialogo
import com.vidacotidiana.app.core.app.FieldIcon
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.core.app.ReferenceSource
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DetailTopBar
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.ToneColors
import com.vidacotidiana.app.core.ui.components.ToneMark
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaHeroArt
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.shortDateEs
import com.vidacotidiana.app.core.ui.components.shortDateTimeEs
import com.vidacotidiana.app.core.ui.components.softCard
import com.vidacotidiana.app.feature.documents.renderPreview
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LA FICHA DE UN RECURSO — «ver todo, editar en el sitio» (`VidaDetailKit`).
 *
 * Una sola pantalla para los tipos que no tienen detalle propio (garantías,
 * inventario, seguimientos, notas, objetivos, rutinas, recursos de trabajo y
 * lugares). Cabecera con el título y su lápiz; «Información» con TODOS los
 * campos que el endpoint guarda —los mismos que declara `CreatableResource`—,
 * cada valor con su lápiz; y la barra de acción fija abajo con la acción que
 * ese tipo tiene de verdad (resolver, cumplir, hecha hoy…).
 *
 * Antes la ficha se leía y «Editar» abría el formulario completo en otra
 * pantalla. Ahora cada valor se cambia en su fila y se puede deshacer.
 */
@Composable
fun ResourceDetailScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    resource: CreatableResource,
    resourceId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val acceso = rememberResourceAccess(viewModel)
    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, resource, resourceId, snack)
    val identity = resource.identity

    val snapshot = viewModel.editSnapshot(resource, resourceId)
    val titulo = viewModel.resourceTitle(resource, resourceId)
    val kind = resource.shareableKind
    val adjunto = viewModel.attachedDocument(resource, resourceId)

    if (snapshot == null) {
        VidaDetailPage(title = identity.label, onBack = { navController.popBackStack() }, snack = snack) {
            if (state.loading) {
                LoadingRows(3)
            } else {
                VidaError(
                    title = "Este registro ya no está",
                    body = "Puede que se haya borrado o que ya no tengas acceso.",
                    onRetry = { navController.popBackStack() },
                    retryLabel = "Volver",
                )
            }
        }
        return
    }

    val values = snapshot.first
    val refs = infoRefsOf(state, viewModel)
    val head = resource.headField
    val facts = factsFor(resource, resourceId, state)
    val attach: (() -> Unit)? = resource.attachWire?.let { wire -> { viewModel.attachDocumentTo(wire, resourceId) } }
    val busy = resourceId in state.busy
    // ADR-019: las reglas del MÓDULO DEL REGISTRO, sin mezclarlas.
    //  · Personal → se comparte con Familia (ADR-025).
    //  · Laboral  → no hay Familia; se vincula a una Persona si el tipo la tiene.
    val module = viewModel.moduleOf(resource, resourceId)
    val familyShare = module == AppContext.PERSONAL && kind != null
    val personLink = module == AppContext.LABORAL && resource.fields.any { it.key == "personId" }
    val opener = rememberInfoOpener()

    val primary: DockAction? = facts.primary?.let { p ->
        when {
            p.done -> DockAction(p.doneLabel, Icons.Outlined.CheckCircle, done = true, enabled = false) {}
            busy -> DockAction("Guardando…", Icons.Outlined.CheckCircle, enabled = false) {}
            else -> DockAction(p.label, Icons.Outlined.CheckCircle) { viewModel.completeResource(resource, resourceId) }
        }
    } ?: when {
        adjunto != null -> DockAction("Ver documento", Icons.Outlined.Description) {
            navController.go(Routes.documentRoute(adjunto.first))
        }
        attach != null -> DockAction("Adjuntar", Icons.Outlined.UploadFile, onClick = attach)
        familyShare && kind != null -> DockAction("Compartir", Icons.Outlined.Share) { acceso.abrir(kind, resourceId, titulo) }
        personLink -> DockAction("Persona", Icons.Outlined.PersonAddAlt) { opener.open("personId") }
        else -> null
    }
    val secondary = listOfNotNull(
        kind?.takeIf { familyShare && primary?.label != "Compartir" }?.let { k ->
            DockAction("Compartir", Icons.Outlined.Share) { acceso.abrir(k, resourceId, titulo) }
        },
        DockAction("Persona", Icons.Outlined.PersonAddAlt) { opener.open("personId") }
            .takeIf { personLink && primary?.label != "Persona" },
        attach?.takeIf { facts.primary != null && adjunto == null }?.let { DockAction("Adjuntar", Icons.Outlined.UploadFile, onClick = it) },
    )
    val more = listOfNotNull(
        facts.primary?.takeIf { it.done && viewModel.puedeVolverAtras(resource) }?.let {
            MoreAction(it.undoLabel, Icons.AutoMirrored.Outlined.Undo, "Vuelve a quedar pendiente") {
                viewModel.revertResource(resource, resourceId)
            }
        },
        facts.extra,
        MoreAction(
            "Eliminar", Icons.Outlined.DeleteOutline, "Se pide confirmación",
            danger = true,
            confirm = "«$titulo» se borrará para siempre. No se puede deshacer.",
        ) {
            viewModel.deleteResource(resource, resourceId)
            navController.popBackStack()
        },
    )

    VidaDetailPage(
        title = identity.label,
        onBack = { navController.popBackStack() },
        snack = snack,
        dock = primary?.let { p -> { VidaDetailDock(primary = p, secondary = secondary, more = more) } },
        actions = {
            // Sin acción principal no hay barra abajo: «Eliminar» sigue a mano.
            if (primary == null) {
                var confirmar by remember { mutableStateOf(false) }
                VidaIconButton(Icons.Outlined.DeleteOutline, "Eliminar", tint = c.textSecondary) { confirmar = true }
                if (confirmar) {
                    AlertDialog(
                        onDismissRequest = { confirmar = false },
                        title = { Text("¿Eliminar?") },
                        text = { Text("«$titulo» se borrará para siempre. No se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmar = false
                                viewModel.deleteResource(resource, resourceId)
                                navController.popBackStack()
                            }) { Text("Eliminar", color = c.error) }
                        },
                        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
                    )
                }
            }
        },
    ) {
        VidaDetailHeroCard(
            section = identity.section,
            eyebrow = listOfNotNull(module.label, identity.label, facts.eyebrow).joinToString(" · "),
            title = titulo.ifBlank { identity.label },
            onRename = head?.let { h ->
                { nuevo: String -> saver.save(h.key, h.infoLabel().takeIf { it.length <= 14 } ?: "Título", nuevo, values[h.key].orEmpty(), flashKey = "title") }
            },
            onEmptyTitle = { snack.show("El título no puede quedar vacío") },
            figure = facts.figure,
            figureCaption = facts.figureCaption,
            status = facts.status,
            statusColor = facts.statusTone,
        )

        VidaInfoSection(
            items = infoItemsFor(resource, values, refs, saver, laboral = module == AppContext.LABORAL),
            refs = refs,
            flash = saver.flash,
            opener = opener,
        )

        if (adjunto != null || resource.admiteDocumento) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailZoneTitle("Documento")
                AttachmentPreview(
                    viewModel = viewModel,
                    adjunto = adjunto,
                    onOpen = { adjunto?.let { navController.go(Routes.documentRoute(it.first)) } },
                    onAttach = { attach?.invoke() },
                )
            }
        }
    }

    acceso.Dialogo(viewModel)
}

/** La acción principal de un tipo y cómo se dice hecha y deshecha. */
private data class PrimaryFact(val label: String, val doneLabel: String, val undoLabel: String, val done: Boolean)

/** Lo que cada tipo cuenta de sí mismo en la cabecera y en la barra. */
private data class Facts(
    val eyebrow: String? = null,
    val figure: String? = null,
    val figureCaption: String? = null,
    val status: String? = null,
    val statusTone: Color? = null,
    val primary: PrimaryFact? = null,
    val extra: MoreAction? = null,
)

@Composable
private fun factsFor(resource: CreatableResource, id: String, state: AppUiState): Facts {
    val c = VidaTheme.colors
    val today = java.time.LocalDate.now()
    val d = state.data
    return when (resource) {
        CreatableResource.COMMITMENT -> d.commitments.firstOrNull { it.id == id }?.let { cmt ->
            val done = cmt.status == "DONE"
            Facts(
                eyebrow = if (cmt.direction == "MINE") "Me toca a mí" else "Lo espero",
                status = when {
                    done -> "Resuelto"
                    cmt.dueOn == null -> "Pendiente"
                    cmt.dueOn.isBefore(today) -> "Atrasado"
                    else -> "Pendiente"
                },
                statusTone = if (done) c.success else if (cmt.dueOn?.isBefore(today) == true) c.warning else c.primary,
                primary = PrimaryFact("Resolver", "Resuelto", "Reabrir", done),
            )
        } ?: Facts()

        CreatableResource.OBJECTIVE -> d.objectives.firstOrNull { it.id == id }?.let { o ->
            Facts(
                figure = o.targetValue?.let { "${o.currentValue}/$it" } ?: o.currentValue.toString(),
                figureCaption = "avance",
                status = if (o.completed) "Cumplido" else "En camino",
                statusTone = if (o.completed) c.success else c.primary,
                primary = PrimaryFact("Cumplir", "Cumplido", "Reabrir", o.completed),
            )
        } ?: Facts()

        CreatableResource.ROUTINE -> d.routines.firstOrNull { it.id == id }?.let { r ->
            Facts(
                eyebrow = VidaVocabulary.human(r.frequency),
                status = if (r.active) "Próxima: " + infoDate(r.nextExecutionDate) else "En pausa",
                statusTone = if (r.active) c.primary else c.textTertiary,
                // Una rutina no se «termina»: se hace hoy y vuelve.
                primary = PrimaryFact("Hecha hoy", "Hecha", "Deshacer", false).takeIf { r.active },
            )
        } ?: Facts()

        CreatableResource.WARRANTY -> d.warranties.firstOrNull { it.id == id }?.let { w ->
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, w.expiresOn)
            Facts(
                eyebrow = w.category.takeIf { it.isNotBlank() },
                figure = if (days >= 0) "$days" else "${-days}",
                figureCaption = if (days >= 0) "días" else "días vencida",
                status = when (w.status) {
                    com.vidacotidiana.app.core.data.WarrantyStatus.VIGENTE -> "Vigente"
                    com.vidacotidiana.app.core.data.WarrantyStatus.POR_VENCER -> "Por vencer"
                    com.vidacotidiana.app.core.data.WarrantyStatus.VENCIDA -> "Vencida"
                },
                statusTone = when (w.status) {
                    com.vidacotidiana.app.core.data.WarrantyStatus.VIGENTE -> c.success
                    com.vidacotidiana.app.core.data.WarrantyStatus.POR_VENCER -> c.warning
                    com.vidacotidiana.app.core.data.WarrantyStatus.VENCIDA -> c.textTertiary
                },
            )
        } ?: Facts()

        else -> Facts()
    }
}

/** Un valor del formulario tal como lo lee una persona. Nulo = sin valor. */
private fun humanValue(field: FormField, raw: String?, state: AppUiState): String? {
    val v = raw?.trim().orEmpty()
    if (v.isEmpty()) return null
    return when (field) {
        is FormField.Choice -> field.options.firstOrNull { it.first == v }?.second ?: v
        is FormField.Date -> runCatching { shortDateEs(java.time.LocalDate.parse(v.take(10))) }.getOrDefault(v)
        is FormField.DateTime -> runCatching { shortDateTimeEs(java.time.LocalDateTime.parse(v.take(16))) }
            .recoverCatching { shortDateEs(java.time.LocalDate.parse(v.take(10))) }
            .getOrDefault(v)
        is FormField.Reference -> when (field.source) {
            ReferenceSource.PEOPLE -> state.data.people.firstOrNull { it.id == v }?.name
            ReferenceSource.PROJECTS -> state.data.projects.firstOrNull { it.id == v }?.name
            ReferenceSource.INVENTORY -> state.data.inventory.firstOrNull { it.id == v }?.name
        } ?: v
        else -> v
    }
}

private fun fieldIcon(icon: FieldIcon): ImageVector = when (icon) {
    FieldIcon.PEN -> Icons.Outlined.Edit
    FieldIcon.CLOCK -> Icons.Outlined.Schedule
    FieldIcon.CALENDAR -> Icons.Outlined.CalendarToday
    FieldIcon.PIN -> Icons.Outlined.Place
    FieldIcon.DOC -> Icons.Outlined.Description
    FieldIcon.COIN -> Icons.Outlined.Payments
    FieldIcon.BOX -> Icons.Outlined.Inventory2
    FieldIcon.PERSON -> Icons.Outlined.Person
    FieldIcon.REPEAT -> Icons.Outlined.Autorenew
    FieldIcon.LINK -> Icons.Outlined.Link
    FieldIcon.CLIP -> Icons.Outlined.AttachFile
    FieldIcon.FLAG -> Icons.Outlined.Flag
    FieldIcon.TAG -> Icons.AutoMirrored.Outlined.Label
    FieldIcon.HASH -> Icons.Outlined.Tag
    FieldIcon.FOLDER -> Icons.Outlined.Folder
}

/**
 * EL DOCUMENTO DEL REGISTRO: su vista previa real si lo tiene —la primera
 * página, como en Documentos— o el recuadro para adjuntarlo si puede tenerlo.
 */
@Composable
private fun AttachmentPreview(
    viewModel: AppViewModel,
    adjunto: Pair<String, String>?,
    onOpen: () -> Unit,
    onAttach: () -> Unit,
) {
    val c = VidaTheme.colors
    var preview by remember(adjunto?.first) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(adjunto?.first) { mutableStateOf(adjunto != null) }
    LaunchedEffect(adjunto?.first) {
        val id = adjunto?.first ?: return@LaunchedEffect
        loading = true
        viewModel.documentBytesFor(id).onSuccess { bytes ->
            preview = withContext(Dispatchers.IO) { runCatching { renderPreview(bytes) }.getOrNull() }
        }
        loading = false
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (adjunto != null) 170.dp else 110.dp)
            .softCard(c.sunken)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button) { if (adjunto != null) onOpen() else onAttach() },
        contentAlignment = Alignment.Center,
    ) {
        val bmp = preview
        when {
            adjunto == null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(26.dp))
                Text("Adjuntar un documento", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = c.textSecondary)
                Text("PDF o imagen", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            bmp != null -> Image(
                bitmap = bmp,
                contentDescription = "Vista previa de ${adjunto.second}",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, c.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            )
            loading -> Text("Preparando vista previa…", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            else -> Text(
                "«${adjunto.second}» · Toca para abrir",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun MetaItem(icon: ImageVector, text: String) {
    val c = VidaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        modifier
            .softCard(corner = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: c.primary, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.text, maxLines = 1)
    }
}

@Composable
internal fun InfoRow(label: String, value: String, last: Boolean = false) {
    val c = VidaTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = c.text,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f),
        )
    }
    if (!last) HorizontalDivider(color = c.border.copy(alpha = 0.14f))
}
