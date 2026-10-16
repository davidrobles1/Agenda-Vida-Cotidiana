package com.vidacotidiana.app.feature.sharing

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
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DetailTopBar
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.ListChip
import com.vidacotidiana.app.core.ui.components.ListSearchBox
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.SearchClearButton
import com.vidacotidiana.app.core.ui.components.ToneColors
import com.vidacotidiana.app.core.ui.components.plural
import com.vidacotidiana.app.feature.resources.identity

/**
 * Compartidos de UNA categoría o de un acceso rápido — la «vista de archivos
 * por secciones» de Documentos, aplicada a lo compartido (ADR-028).
 *
 * La dirección viaja en la ruta: «Te toca» recibiendo es trabajo mío, y
 * enviando es trabajo que espero de otra persona. Mezclarlas cambiaría quién
 * tiene que actuar.
 */
@Composable
fun SharedCollectionScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    receiving: Boolean,
    key: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val title = ShareCollections.title(key, receiving)
    val all = if (receiving) state.data.sharedWithMe else state.data.sharedByMe
    val base = all.filter { ShareCollections.matches(key, it) }

    val chips = listOf(
        ShareCollections.ALL to "Todos",
        ShareCollections.PENDING to ShareCollections.title(ShareCollections.PENDING, receiving),
        ShareCollections.DONE to "Hecho",
        ShareCollections.VIEW_ONLY to "Solo ver",
    )
    var chip by rememberSaveable { mutableStateOf(ShareCollections.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val filtered = base
        .filter { ShareCollections.matches(chip, it) }
        .filter {
            needle.isEmpty() || it.label.lowercase().contains(needle) ||
                it.counterpart.lowercase().contains(needle) ||
                kindPlural(it.kind).lowercase().contains(needle)
        }

    val kind = ShareableKind.entries.firstOrNull { it.wire == key }
    val (icon, tone) = when {
        kind != null -> kind.resource.identity.section.icon to kindTone(kind)
        key == ShareCollections.PENDING -> Icons.Outlined.PendingActions to ToneColors(c.warning, c.warningContainer)
        key == ShareCollections.DONE -> Icons.Outlined.TaskAlt to ToneColors(c.success, c.successContainer)
        key == ShareCollections.VIEW_ONLY -> Icons.Outlined.Visibility to ToneColors(c.textSecondary, c.sunken)
        else -> Icons.Outlined.Share to ToneColors(c.second, c.secondContainer)
    }

    Column(Modifier.fillMaxSize().background(c.surface)) {
        DetailTopBar(
            title = title,
            subtitle = "${plural(base.size, "compartido", "compartidos")} · ${if (receiving) "recibidos" else "enviados"}",
            onBack = { navController.popBackStack() },
            trailing = {
                // La lámina de la esquina: dos hojas en el tono de la categoría.
                Box(Modifier.size(width = 68.dp, height = 56.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.offset(x = 8.dp, y = (-4).dp).size(width = 40.dp, height = 46.dp)
                            .background(tone.bg.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                    )
                    Box(
                        Modifier.offset(x = (-4).dp, y = 4.dp).size(width = 40.dp, height = 46.dp)
                            .background(tone.bg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(24.dp)) }
                }
            },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            ListSearchBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar por nombre o persona…",
                trailing = if (query.isNotEmpty()) ({ SearchClearButton { query = "" } }) else null,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { (k, label) -> ListChip(label, chip == k, onClick = { chip = k }) }
            }
            when {
                state.loading && all.isEmpty() -> LoadingRows(3)
                filtered.isEmpty() -> EmptyState(
                    title = if (base.isEmpty()) "Nada en $title" else "Sin coincidencias",
                    body = when {
                        base.isNotEmpty() -> "Prueba con otro nombre o quita el filtro."
                        receiving -> "Cuando alguien de tu familia te comparta algo de este tipo, aparecerá aquí."
                        else -> "Abre el recurso y pulsa «Compartir» para elegir con quién."
                    },
                )
                else -> ResourceBoard(
                    entries = filtered.map { shareEntry(it, receiving, viewModel, navController) },
                    onOpenDetail = { entry ->
                        filtered.firstOrNull { it.id == entry.id }?.let { openShare(it, receiving, navController) }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
