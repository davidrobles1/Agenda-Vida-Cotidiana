package com.vidacotidiana.app.feature.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.data.Document
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.plural

/** Cuántas filas se pintan antes de «Cargar más». */
internal const val DOC_PAGE_SIZE = 8

private enum class DocSort(val label: String, val comparator: Comparator<Document>) {
    NEWEST("Más recientes", NewestFirst),
    OLDEST("Más antiguos", NewestFirst.reversed()),
    NAME("Nombre (A–Z)", compareBy { it.name.lowercase() }),
    SIZE("Tamaño (mayor primero)", compareByDescending { it.sizeBytes }),
}

/** Lo que cuelga del chip «Más». */
private enum class DocMoreFilter(val label: String) {
    SHARED("Compartidos"),
    PRIVATE("Solo yo"),
    ATTACHED("Adjuntos a un recurso"),
}

private const val CHIP_ALL = "Todos"
private const val CHIP_RECENT = "Recientes"
private const val CHIP_FAVORITES = "Favoritos"
private const val CHIP_MORE = "Más"

/**
 * «Categoría · Vista de archivos por secciones» de la propuesta.
 *
 * Sirve a las cinco categorías y a los accesos rápidos (Recientes, Favoritos,
 * Compartidos y «Ver todos»): lo que cambia entre ellos es sólo qué documentos
 * entran, así que es una pantalla con su clave y no cinco pantallas.
 *
 * El filtrado, el orden y la paginación son LOCALES: la sección ya tiene todos
 * los documentos en memoria (`GET /documents` no pagina), y paginar aquí sólo
 * evita pintar cien filas de golpe.
 */
@Composable
fun DocumentCollectionScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    key: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val actions = rememberDocumentActions(viewModel, navController)
    val isCategory = !key.startsWith("_")
    val title = DocCollections.title(key)

    val sharedCounts = state.data.sharedByMe
        .filter { it.kind == ShareableKind.DOCUMENT }
        .groupingBy { it.resourceId }.eachCount()

    var query by rememberSaveable { mutableStateOf("") }
    var chip by rememberSaveable { mutableStateOf(CHIP_ALL) }
    var more by rememberSaveable { mutableStateOf<DocMoreFilter?>(null) }
    var sort by rememberSaveable { mutableStateOf(DocSort.NEWEST) }
    var moreOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var shown by remember(key, query, chip, more, sort) { mutableIntStateOf(DOC_PAGE_SIZE) }

    val base = state.data.documents.filter {
        when (key) {
            DocCollections.ALL -> true
            DocCollections.RECENT -> it.isRecent()
            DocCollections.FAVORITES -> it.id in favorites
            DocCollections.SHARED -> it.isShared(sharedCounts[it.id] ?: 0)
            else -> it.category == key
        }
    }
    val needle = query.trim().searchable()
    val filtered = base
        .filter {
            when (chip) {
                CHIP_RECENT -> it.isRecent()
                CHIP_FAVORITES -> it.id in favorites
                CHIP_MORE -> when (more) {
                    DocMoreFilter.SHARED -> it.isShared(sharedCounts[it.id] ?: 0)
                    DocMoreFilter.PRIVATE -> it.visibility == "PRIVATE"
                    DocMoreFilter.ATTACHED -> it.resourceId != null
                    null -> true
                }
                else -> true
            }
        }
        .filter { needle.isEmpty() || it.name.searchable().contains(needle) }
        .sortedWith(sort.comparator)

    val (space, unit) = totalSizeParts(base.sumOf { it.sizeBytes })

    Column(Modifier.fillMaxSize().background(c.surface)) {
        DocTopBar(
            title = title,
            subtitle = "${plural(base.size, "documento", "documentos")} · $space $unit",
            onBack = { navController.popBackStack() },
            trailing = { CollectionIllustration(key) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            DocSearchBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar en $title…",
                trailing = {
                    Box {
                        VidaIconButton(Icons.Outlined.Tune, "Ordenar", tint = c.textSecondary) { sortOpen = true }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            DocSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    trailingIcon = {
                                        if (option == sort) Icon(Icons.Filled.Check, null, tint = c.primary)
                                    },
                                    onClick = { sort = option; sortOpen = false },
                                )
                            }
                        }
                    }
                },
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(CHIP_ALL, CHIP_RECENT, CHIP_FAVORITES).forEach { option ->
                    DocChip(option, chip == option, onClick = { chip = option; more = null })
                }
                Box {
                    DocChip(
                        more?.label ?: CHIP_MORE,
                        selected = chip == CHIP_MORE,
                        dropdown = true,
                        onClick = { moreOpen = true },
                    )
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DocMoreFilter.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                trailingIcon = {
                                    if (option == more) Icon(Icons.Filled.Check, null, tint = c.primary)
                                },
                                onClick = { chip = CHIP_MORE; more = option; moreOpen = false },
                            )
                        }
                    }
                }
            }

            when {
                state.loading && state.data.documents.isEmpty() -> LoadingRows(4)
                filtered.isEmpty() -> EmptyState(
                    title = if (base.isEmpty()) "Nada en $title todavía" else "Sin coincidencias",
                    body = if (base.isEmpty()) emptyBodyFor(key) else "Prueba con otro nombre o quita el filtro.",
                )
                else -> {
                    Column {
                        filtered.take(shown).forEach { doc ->
                            val visual = categoryVisual(doc.category)
                            DocumentRow(
                                doc = doc,
                                meta = if (isCategory) "${doc.dayLabel()} · ${doc.sizeLabel}"
                                else "${visual.label} · ${doc.sizeLabel} · ${doc.dayLabel()}",
                                tone = if (isCategory) typeTone(doc) else visual.tone,
                                favorite = doc.id in favorites,
                                actions = actions,
                                onClick = { actions.details(doc) },
                            )
                        }
                    }
                    if (filtered.size > DOC_PAGE_SIZE) {
                        LoadMoreFooter(minOf(shown, filtered.size), filtered.size) { shown += DOC_PAGE_SIZE }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    actions.Dialogs(viewModel)
}

private fun emptyBodyFor(key: String): String = when (key) {
    DocCollections.FAVORITES -> "Marca un documento con la estrella para tenerlo aquí."
    DocCollections.SHARED -> "Aquí aparecen los documentos que ve alguien más además de ti."
    DocCollections.RECENT -> "Aquí aparecen los documentos subidos en los últimos ${DocCollections.RECENT_DAYS} días."
    else -> "Sube un documento con el botón «+» para tenerlo a mano."
}

/**
 * La lámina de la esquina: dos hojas desplazadas en el tono de la colección,
 * como el montón de papeles de la propuesta.
 */
@Composable
private fun CollectionIllustration(key: String) {
    val c = VidaTheme.colors
    val (icon, tone) = when (key) {
        DocCollections.RECENT -> Icons.Outlined.Schedule to DocTone(c.info, c.infoContainer)
        DocCollections.FAVORITES -> Icons.Outlined.StarOutline to DocTone(c.warning, c.warningContainer)
        DocCollections.SHARED -> Icons.Outlined.Group to DocTone(c.second, c.secondContainer)
        DocCollections.ALL -> Icons.Outlined.Description to DocTone(c.primary, c.primaryContainer)
        else -> categoryVisual(key).let { it.icon to it.tone }
    }
    Box(Modifier.size(width = 68.dp, height = 56.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .offset(x = 8.dp, y = (-4).dp)
                .size(width = 40.dp, height = 46.dp)
                .background(tone.bg.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
        )
        Box(
            Modifier
                .offset(x = (-4).dp, y = 4.dp)
                .size(width = 40.dp, height = 46.dp)
                .background(tone.bg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(24.dp))
        }
    }
}
