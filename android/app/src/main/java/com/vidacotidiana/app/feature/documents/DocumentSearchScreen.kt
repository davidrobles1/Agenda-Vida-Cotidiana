package com.vidacotidiana.app.feature.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.data.Document
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.VidaIconButton

private enum class SearchScope(val label: String) {
    ALL("Todo"),
    DOCUMENTS("Documentos"),
    CATEGORIES("Categorías"),
    DATES("Fechas"),
}

private val FULL_MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** Todo lo que una fecha puede llamarse: «25 sep 2026», «septiembre», «2026-09-25». */
private fun Document.dateHaystack(): String {
    val d = createdAt?.toLocalDate() ?: return dateLabel.searchable()
    return "${shortDate(d)} ${d.dayOfMonth} ${FULL_MONTHS[d.monthValue - 1]} ${d.year} $d".searchable()
}

/**
 * «Búsqueda global · Resultados» de la propuesta.
 *
 * Busca por nombre, categoría y fecha, y los chips acotan a uno de los tres.
 * NO busca dentro del contenido del archivo: el backend no indexa el texto de
 * los documentos, y extraerlo en el teléfono supondría descargarlos todos.
 * Queda como TBD de producto, no como promesa en la interfaz.
 */
@Composable
fun DocumentSearchScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    initialQuery: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val actions = rememberDocumentActions(viewModel, navController)

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var scope by rememberSaveable { mutableStateOf(SearchScope.ALL) }
    var shown by remember(query, scope) { mutableIntStateOf(DOC_PAGE_SIZE) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initialQuery.isEmpty()) runCatching { focus.requestFocus() } }

    val needle = query.trim().searchable()
    val results = if (needle.isEmpty()) emptyList() else state.data.documents
        .filter { doc ->
            val byName = doc.name.searchable().contains(needle)
            val byCategory = VidaVocabulary.human(doc.category).searchable().contains(needle)
            val byDate = doc.dateHaystack().contains(needle)
            when (scope) {
                SearchScope.ALL -> byName || byCategory || byDate
                SearchScope.DOCUMENTS -> byName
                SearchScope.CATEGORIES -> byCategory
                SearchScope.DATES -> byDate
            }
        }
        .sortedWith(NewestFirst)

    Column(Modifier.fillMaxSize().background(c.surface)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VidaIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Atrás", tint = c.text) {
                navController.popBackStack()
            }
            DocSearchBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar por nombre, categoría o fecha",
                focusRequester = focus,
                modifier = Modifier.weight(1f),
                trailing = if (query.isNotEmpty()) ({ ClearButton { query = "" } }) else null,
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchScope.entries.forEach { option ->
                    DocChip(option.label, scope == option, onClick = { scope = option })
                }
            }

            when {
                needle.isEmpty() -> EmptyState(
                    title = "¿Qué documento buscas?",
                    body = "Escribe un nombre, una categoría como «Seguros» o una fecha como «septiembre».",
                )
                results.isEmpty() -> EmptyState(
                    title = "Sin resultados",
                    body = "No hay documentos que coincidan con «${query.trim()}».",
                )
                else -> {
                    Text(
                        "Resultados (${results.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.text,
                    )
                    Column {
                        results.take(shown).forEach { doc ->
                            val visual = categoryVisual(doc.category)
                            DocumentRow(
                                doc = doc,
                                meta = "${visual.label} · ${doc.sizeLabel} · ${doc.dayLabel()}",
                                tone = visual.tone,
                                favorite = doc.id in favorites,
                                actions = actions,
                                onClick = { actions.details(doc) },
                            )
                        }
                    }
                    if (results.size > DOC_PAGE_SIZE) {
                        LoadMoreFooter(minOf(shown, results.size), results.size) { shown += DOC_PAGE_SIZE }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    actions.Dialogs(viewModel)
}
