package com.vidacotidiana.app.feature.documents

import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.core.ui.VidaSections
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import com.vidacotidiana.app.core.ui.components.softCard
import com.vidacotidiana.app.core.ui.components.ToneMark
import com.vidacotidiana.app.core.ui.components.DetailTopBar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «Vista de documento · Detalle» de la propuesta.
 *
 * Al tocar un documento ya no se abre directamente el visor: primero se ve
 * QUÉ es —nombre, categoría, fecha, tamaño y quién lo ve—, las acciones
 * rápidas y una vista previa. «Abrir» (o tocar la vista previa) lleva al
 * visor interno de siempre.
 *
 * Mapa de acciones, sin inventar ninguna:
 *  · Abrir       → visor interno (`DocumentViewerScreen`).
 *  · Descargar   → guardar una copia donde el usuario elija, con el selector
 *                  del sistema (`ACTION_CREATE_DOCUMENT`, sin permisos).
 *  · Compartir   → compartir dentro de Cotidiana (ADR-025, solo lectura).
 *  · Favorito    → marca local del dispositivo.
 *  · ⇪ (arriba)  → mandar el archivo a otra app con el selector de Android.
 */
@Composable
fun DocumentDetailScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    documentId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val context = LocalContext.current
    val actions = rememberDocumentActions(viewModel, navController)
    val doc = state.data.documents.firstOrNull { it.id == documentId }

    val save = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(doc?.let { viewModel.documentMime(it.name) } ?: "*/*"),
    ) { uri -> if (uri != null) viewModel.saveDocumentTo(context, documentId, uri) }

    var preview by remember(documentId) { mutableStateOf<ImageBitmap?>(null) }
    var previewLoading by remember(documentId) { mutableStateOf(true) }
    LaunchedEffect(documentId) {
        previewLoading = true
        viewModel.documentBytesFor(documentId).onSuccess { bytes ->
            preview = withContext(Dispatchers.IO) { runCatching { renderPreview(bytes) }.getOrNull() }
        }
        previewLoading = false
    }

    val snack = rememberDetailSnack()
    val saver = rememberInlineSaver(viewModel, CreatableResource.DOCUMENT, documentId, snack)
    // «Archivo» cambia el contenido: el selector del teléfono y, al elegir, se
    // guarda por el mismo camino de edición (que ya sustituye el archivo).
    val replaceFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val d = doc
        if (uri != null && d != null) {
            viewModel.readFile(uri) { payload ->
                if (payload == null) snack.show("No se pudo leer el archivo")
                else viewModel.saveField(CreatableResource.DOCUMENT, d.id, "name", d.name, file = payload) { ok ->
                    snack.show(if (ok) "Archivo sustituido" else "No se pudo guardar. Revisa tu conexión.")
                }
            }
        }
    }

    if (doc == null) {
        VidaDetailPage(title = "Documento", onBack = { navController.popBackStack() }, snack = snack) {
            if (state.loading) {
                LoadingRows(3)
            } else {
                VidaError(
                    title = "Este documento ya no está",
                    body = "Puede que se haya eliminado o que ya no tengas acceso.",
                    onRetry = { navController.popBackStack() },
                    retryLabel = "Volver",
                )
            }
        }
    } else {
        val visual = categoryVisual(doc.category)
        val favorite = doc.id in favorites
        val sharedWith = state.data.sharedByMe.count { it.kind == ShareableKind.DOCUMENT && it.resourceId == doc.id }
        val familyVisible = doc.visibility != "PRIVATE"
        val values = viewModel.editSnapshot(CreatableResource.DOCUMENT, documentId)?.first.orEmpty()
        val refs = infoRefsOf(state, viewModel)
        // ADR-019/ADR-025: «Quién lo ve» y Compartir son de FAMILIA, así que
        // solo existen en un documento de Personal. Uno de Laboral se abre, se
        // descarga o se manda a otra app, pero no se comparte con la familia.
        val personal = viewModel.moduleOf(CreatableResource.DOCUMENT, documentId) == AppContext.PERSONAL
        val visibility = FormField.Choice(
            "visibility", "Quién lo ve",
            listOf("PRIVATE" to "Solo yo", "FAMILY_PUBLIC" to "Toda la familia"),
        )

        VidaDetailPage(
            title = "Documento",
            onBack = { navController.popBackStack() },
            snack = snack,
            dock = {
                VidaDetailDock(
                    primary = DockAction("Abrir", Icons.Outlined.Visibility) { actions.open(doc) },
                    secondary = listOf(
                        DockAction("Descargar", Icons.Outlined.Download) { save.launch(doc.name) },
                        if (personal) {
                            // COHERENCIA CON «QUIÉN LO VE». Si ya lo ve toda la
                            // familia no hay a quién más compartírselo: el botón
                            // se apaga. Con «Solo yo» se vuelve a encender.
                            DockAction("Compartir", Icons.Outlined.Share, enabled = doc.visibility != "FAMILY_PUBLIC") { actions.share(doc) }
                        } else {
                            DockAction("Otra app", Icons.Outlined.IosShare) { actions.openExternal(doc) }
                        },
                    ),
                    more = listOfNotNull(
                        MoreAction(
                            if (favorite) "Quitar de favoritos" else "Favorito",
                            if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            "Aparece primero en Documentos",
                        ) { actions.toggleFavorite(doc) },
                        MoreAction("Abrir en otra app", Icons.Outlined.IosShare, "Con el selector de Android") {
                            actions.openExternal(doc)
                        }.takeIf { personal },
                        // Su propio diálogo de confirmación, el de siempre.
                        MoreAction("Eliminar", Icons.Outlined.DeleteOutline, "Se pide confirmación", danger = true) {
                            actions.askDelete(doc)
                        },
                    ),
                )
            },
        ) {
            VidaDetailHeroCard(
                section = VidaSections.Documentos,
                tile = { ToneMark(doc.glyph(), visual.tone, size = 48.dp, iconSize = 24.dp) },
                eyebrow = listOfNotNull(if (personal) AppContext.PERSONAL.label else AppContext.LABORAL.label, visual.label, doc.name.substringAfterLast('.', "").uppercase().takeIf { it.length in 2..4 })
                    .joinToString(" · "),
                title = doc.name,
                onRename = { saver.save("name", "Nombre", it, doc.name, flashKey = "title") },
                onEmptyTitle = { snack.show("El nombre no puede quedar vacío") },
                figure = doc.sizeLabel,
                status = (if (personal) visibilityLabel(doc.visibility, sharedWith) else "Solo tú") + if (favorite) " · Favorito" else "",
                statusColor = c.textTertiary,
            )

            VidaInfoSection(
                items = infoItemsFor(CreatableResource.DOCUMENT, values, refs, saver) + listOfNotNull(
                    InfoItem(
                        "file", "Archivo", doc.name,
                        InfoEdit.Action(Icons.Outlined.SwapHoriz) { replaceFile.launch("*/*") },
                    ),
                    if (personal) InfoItem(
                        "visibility", "Quién lo ve",
                        if (familyVisible) "Toda la familia" else visibilityLabel(doc.visibility, sharedWith),
                        InfoEdit.Field(visibility, if (familyVisible) "FAMILY_PUBLIC" else "PRIVATE"),
                    ) { v -> actions.setFamilyVisible(doc, v == "FAMILY_PUBLIC"); snack.show("Visibilidad actualizada") } else null,
                    InfoItem("size", "Tamaño", doc.sizeLabel, InfoEdit.ReadOnly("Del archivo")),
                    InfoItem("created", "Creado", doc.dateTimeLabel(), InfoEdit.ReadOnly("Al subirlo")),
                    doc.resourceType?.let { InfoItem("attached", "Adjunto a", attachedLabel(it), InfoEdit.ReadOnly()) },
                ),
                refs = refs,
                flash = saver.flash,
            )

            // ── Vista previa ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailZoneTitle("Vista previa")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(c.surfaceVariant, RoundedCornerShape(24.dp))
                        .border(1.dp, c.line, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button) { actions.open(doc) },
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = preview
                    when {
                        bmp != null -> Image(
                            bitmap = bmp,
                            contentDescription = "Vista previa de ${doc.name}",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, c.line, RoundedCornerShape(14.dp)),
                        )
                        previewLoading -> Text("Preparando vista previa…", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                        else -> Text(
                            "Sin vista previa. Toca para abrir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textTertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    actions.Dialogs(viewModel, onDeleted = { navController.popBackStack() })
}

/** ADR-025: un documento se comparte sólo para verlo, así que se dice QUIÉN lo ve. */
private fun visibilityLabel(visibility: String, sharedWith: Int): String = when {
    visibility == "FAMILY_PUBLIC" -> "Toda la familia"
    sharedWith > 0 -> if (sharedWith == 1) "Tú y 1 persona" else "Tú y $sharedWith personas"
    visibility == "PRIVATE" -> "Solo yo"
    else -> "Compartido"
}

private fun attachedLabel(type: String): String = when (type.uppercase()) {
    "WARRANTY" -> "Una garantía"
    "MAINTENANCE", "MAINTENANCE_RECORD" -> "Un mantenimiento"
    "REMINDER", "TASK" -> "Una tarea"
    "INVENTORY_ITEM" -> "Un artículo del inventario"
    "SUBSCRIPTION", "PAYMENT" -> "Un pago"
    else -> "Otro registro"
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
    val c = VidaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text)
    }
}

/** Abrir · Descargar · Compartir · Favorito. */
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = null,
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
private fun InfoRow(label: String, value: String, last: Boolean = false) {
    val c = VidaTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = c.text,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f),
        )
    }
    if (!last) HorizontalDivider(color = c.border.copy(alpha = 0.14f))
}
