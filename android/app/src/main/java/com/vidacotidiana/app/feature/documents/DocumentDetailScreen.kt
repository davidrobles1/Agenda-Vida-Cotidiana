package com.vidacotidiana.app.feature.documents

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

    var menuOpen by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    // La barra de arriba, con atrás, ⇪ y ⋮. Flota sobre la lámina de la
    // cabecera; `solid` la vuelve opaca cuando el contenido pasa por debajo.
    val topBar: @Composable (solid: Float) -> Unit = { solid ->
        Box(Modifier.fillMaxWidth().background(c.surface.copy(alpha = solid))) {
            DocTopBar(
                title = if (solid > 0.9f) doc?.name else null,
                onBack = { navController.popBackStack() },
                trailing = {
                    if (doc != null) {
                        VidaIconButton(Icons.Outlined.IosShare, "Abrir en otra app", tint = c.text) {
                            actions.openExternal(doc)
                        }
                        Box {
                            VidaIconButton(Icons.Outlined.MoreVert, "Más acciones", tint = c.text) { menuOpen = true }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                val familyVisible = doc.visibility != "PRIVATE"
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    onClick = { menuOpen = false; actions.edit(doc) },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (familyVisible) "Ocultar a la familia" else "Ver toda la familia") },
                                    onClick = { menuOpen = false; actions.setFamilyVisible(doc, !familyVisible) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = c.error) },
                                    onClick = { menuOpen = false; actions.askDelete(doc) },
                                )
                            }
                        }
                    }
                },
            )
        }
    }

    if (doc == null) {
        Column(Modifier.fillMaxSize().background(c.surface)) {
            topBar(1f)
            if (state.loading) {
                Box(Modifier.padding(18.dp)) { LoadingRows(3) }
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
        val density = LocalDensity.current
        val fadeDistance = with(density) { 140.dp.toPx() }

        Box(Modifier.fillMaxSize().background(c.surface)) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                // ── Cabecera con lámina de fondo ─────────────────────────────
                // La ilustración ocupa TODA la cabecera —detrás de la barra,
                // del icono, del nombre y de los datos— y el texto va encima.
                Box(Modifier.fillMaxWidth()) {
                    DocumentHeroArt(doc.category, Modifier.matchParentSize())
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 60.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DocMark(doc.glyph(), visual.tone, size = 76.dp, iconSize = 36.dp, circle = true)
                        Text(
                            doc.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                            color = c.text,
                            modifier = Modifier.fillMaxWidth(0.62f),
                        )
                        Text(
                            visual.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = visual.tone.fg,
                            modifier = Modifier
                                .background(visual.tone.bg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            MetaItem(Icons.Outlined.CalendarToday, doc.dayLabel())
                            MetaItem(Icons.Outlined.InsertDriveFile, doc.sizeLabel)
                        }
                        MetaItem(
                            if (doc.visibility == "PRIVATE" && sharedWith == 0) Icons.Outlined.Lock else Icons.Outlined.Groups,
                            visibilityLabel(doc.visibility, sharedWith),
                        )
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HorizontalDivider(color = c.border.copy(alpha = 0.18f))

                    // ── Acciones rápidas ─────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(Icons.Outlined.Visibility, "Abrir", Modifier.weight(1f)) { actions.open(doc) }
                        ActionTile(Icons.Outlined.Download, "Descargar", Modifier.weight(1f)) { save.launch(doc.name) }
                        ActionTile(Icons.Outlined.Share, "Compartir", Modifier.weight(1f)) { actions.share(doc) }
                        ActionTile(
                            if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            "Favorito",
                            Modifier.weight(1f),
                            tint = if (favorite) c.warning else null,
                        ) { actions.toggleFavorite(doc) }
                    }

                    // ── Información ──────────────────────────────────────────
                    Text("Información", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.text)
                    Column(Modifier.fillMaxWidth().docCard().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        InfoRow("Nombre", doc.name)
                        InfoRow("Categoría", visual.label)
                        InfoRow("Fecha de creación", doc.dateTimeLabel())
                        InfoRow("Tamaño", doc.sizeLabel, last = doc.resourceType == null)
                        if (doc.resourceType != null) {
                            InfoRow("Adjunto a", attachedLabel(doc.resourceType), last = true)
                        }
                    }

                    // ── Vista previa ─────────────────────────────────────────
                    Text("Vista previa", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.text)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .docCard(c.sunken)
                            .clip(RoundedCornerShape(18.dp))
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, c.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
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
                    Spacer(Modifier.height(24.dp))
                }
            }
            topBar((scroll.value / fadeDistance).coerceIn(0f, 1f))
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
            .docCard(corner = 16.dp)
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
