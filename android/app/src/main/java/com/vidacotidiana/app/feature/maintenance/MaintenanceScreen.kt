package com.vidacotidiana.app.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.mock.MaintenanceStatus
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.ListItemRow

/** UX-006: mock module (scaffolding only, MockData.kt) — no reference screen covers this in the same detail, adapted reasonably from the Garantías/Inventario pattern. */
@Composable
fun MaintenanceScreen(viewModel: MaintenanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Mantenimiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
            items(state.records, key = { it.id }) { record ->
                val (pillLabel, pillTone) = when (record.status) {
                    MaintenanceStatus.AL_DIA -> "Al día" to BadgeTone.Success
                    MaintenanceStatus.PROXIMO -> "Próximo" to BadgeTone.Warning
                    MaintenanceStatus.VENCIDO -> "Vencido" to BadgeTone.Error
                }
                ListItemRow(
                    title = "${record.item} — ${record.task}",
                    subtitle = "Próximo: ${record.nextDueLabel}",
                    icon = Icons.Filled.Build,
                    tone = BadgeTone.Info,
                    pillLabel = pillLabel,
                    pillTone = pillTone,
                )
            }
        }
    }
}
