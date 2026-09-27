package com.vidacotidiana.app.feature.documents

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.sliceError
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.plural
import com.vidacotidiana.app.navigation.Routes
import kotlinx.coroutines.CoroutineScope

/**
 * DOCUMENTOS — «Pantalla principal · Resumen inteligente» de la propuesta
 * aprobada el 2026-09-26.
 *
 * El cambio de fondo: la sección ya no abre con la lista de TODOS los
 * archivos. Abre con un resumen —cuántos hay, cuánto ocupan, en cuántas
 * categorías—, los accesos rápidos, las categorías y los tres últimos. La
 * lista completa sigue existiendo, pero se llega a ella por categoría, por
 * acceso rápido o buscando.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: AppViewModel,
    @Suppress("UNUSED_PARAMETER") drawerState: DrawerState,
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val context = LocalContext.current
    val documents = state.data.documents
    val actions = rememberDocumentActions(viewModel, navController)
    val error = state.sliceError(DataSlice.DOCUMENTS)

    val sharedCounts = state.data.sharedByMe
        .filter { it.kind == ShareableKind.DOCUMENT }
        .groupingBy { it.resourceId }.eachCount()

    // BUSCAR POR VOZ con el reconocedor del teléfono: Cotidiana no graba ni
    // pide permiso de micrófono; el sistema escucha y devuelve el texto.
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) navController.navigate(Routes.documentSearchRoute(text))
    }
    val onMic = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Buscar documentos")
        runCatching { voice.launch(intent) }.onFailure {
            Toast.makeText(context, "El dictado por voz no está disponible en este teléfono", Toast.LENGTH_SHORT).show()
        }
        Unit
    }

    var allCategories by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = state.loading && documents.isNotEmpty(),
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize().background(c.surface),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Cabecera: título, «+» y campana ──────────────────────────────
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Documentos",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                        color = c.text,
                    )
                    Text("Todo lo importante, cuando lo necesitas.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                }
                VidaIconButton(Icons.Filled.Add, "Subir documento", tint = c.text) {
                    viewModel.requestCreate(CreatableResource.DOCUMENT)
                }
                VidaIconButton(
                    Icons.Outlined.Notifications, "Avisos", tint = c.text,
                    badge = viewModel.avisosSinLeer() > 0,
                ) { navController.navigate(Routes.NOTIFICATIONS) }
            }

            when {
                state.loading && documents.isEmpty() -> LoadingRows(4)
                error != null && documents.isEmpty() -> VidaError(
                    title = "No pudimos cargar tus documentos",
                    body = error,
                    onRetry = viewModel::refresh,
                )
                documents.isEmpty() -> EmptyState(
                    title = "Aún no hay documentos",
                    body = "Sube un documento para tenerlo siempre a mano.",
                    action = "Subir documento" to { viewModel.requestCreate(CreatableResource.DOCUMENT) },
                    section = VidaSections.Documentos,
                )
                else -> {
                    // ── Resumen: tres cifras ─────────────────────────────────
                    val (space, unit) = totalSizeParts(documents.sumOf { it.sizeBytes })
                    val categoriesInUse = documents.map { it.category }.distinct().size
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(Icons.Outlined.Description, documents.size.toString(),
                            if (documents.size == 1) "documento" else "documentos", Modifier.weight(1f))
                        StatTile(Icons.Outlined.Storage, "$space $unit", "usados", Modifier.weight(1f))
                        StatTile(Icons.Outlined.AttachFile, categoriesInUse.toString(),
                            if (categoriesInUse == 1) "categoría" else "categorías", Modifier.weight(1f))
                    }

                    // ── Buscador: atajo a la búsqueda global ─────────────────
                    DocSearchBox(
                        value = "",
                        placeholder = "Buscar documentos…",
                        onClick = { navController.navigate(Routes.documentSearchRoute()) },
                        trailing = { VidaIconButton(Icons.Outlined.Mic, "Buscar por voz", onClick = onMic) },
                    )

                    // ── Accesos rápidos ──────────────────────────────────────
                    DocSectionTitle("Accesos rápidos")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickTile(
                            Icons.Outlined.Schedule, "Recientes", documents.count { it.isRecent() },
                            DocTone(c.info, c.infoContainer), Modifier.weight(1f),
                        ) { navController.navigate(Routes.documentCollectionRoute(DocCollections.RECENT)) }
                        QuickTile(
                            Icons.Outlined.StarOutline, "Favoritos", documents.count { it.id in favorites },
                            DocTone(c.warning, c.warningContainer), Modifier.weight(1f),
                        ) { navController.navigate(Routes.documentCollectionRoute(DocCollections.FAVORITES)) }
                        QuickTile(
                            Icons.Outlined.Group, "Compartidos", documents.count { it.isShared(sharedCounts[it.id] ?: 0) },
                            DocTone(c.second, c.secondContainer), Modifier.weight(1f),
                        ) { navController.navigate(Routes.documentCollectionRoute(DocCollections.SHARED)) }
                    }

                    // ── Mis categorías ───────────────────────────────────────
                    // Las cinco del backend más cualquiera que llegue sin estar
                    // en el vocabulario; la más usada primero.
                    val known = VidaVocabulary.documentCategories.map { it.first }
                    val keys = (known + documents.map { it.category }).distinct()
                    val counts = documents.groupingBy { it.category }.eachCount()
                    val ordered = keys.sortedByDescending { counts[it] ?: 0 }
                    val visible = if (allCategories) ordered else ordered.take(4)
                    DocSectionTitle(
                        "Mis categorías",
                        action = if (ordered.size > 4) (if (allCategories) "Ver menos" else "Ver todas") else null,
                        onAction = { allCategories = !allCategories },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visible.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { key ->
                                    CategoryCard(categoryVisual(key), counts[key] ?: 0, Modifier.weight(1f)) {
                                        navController.navigate(Routes.documentCollectionRoute(key))
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // ── Recientes ────────────────────────────────────────────
                    DocSectionTitle("Recientes", action = "Ver todos") {
                        navController.navigate(Routes.documentCollectionRoute(DocCollections.ALL))
                    }
                    Column {
                        documents.sortedWith(NewestFirst).take(3).forEach { doc ->
                            val visual = categoryVisual(doc.category)
                            DocumentRow(
                                doc = doc,
                                meta = "${visual.label} · ${doc.sizeLabel} · ${doc.relativeLabel()}",
                                tone = visual.tone,
                                favorite = doc.id in favorites,
                                actions = actions,
                                onClick = { actions.details(doc) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    actions.Dialogs(viewModel)
}

/** Una cifra del resumen: icono, número y unidad. */
@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    Column(
        modifier.docCard().padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            color = c.text,
            maxLines = 1,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1)
    }
}

/** Recientes · Favoritos · Compartidos. */
@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    count: Int,
    tone: DocTone,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        modifier
            .docCard(tone.bg.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(24.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("($count)", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/** Tarjeta de categoría, en el color de su categoría. */
@Composable
private fun CategoryCard(visual: DocCategoryVisual, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(visual.tone.bg.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DocMark(visual.icon, DocTone(visual.tone.fg, c.surface.copy(alpha = 0.75f)), size = 38.dp, iconSize = 20.dp)
        Column {
            Text(
                visual.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(plural(count, "documento", "documentos"), style = MaterialTheme.typography.bodySmall, color = visual.tone.fg)
        }
    }
}
