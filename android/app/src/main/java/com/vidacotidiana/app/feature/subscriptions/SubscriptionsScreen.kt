package com.vidacotidiana.app.feature.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.ListItemRow

/** UX-006: mock module (scaffolding only, MockData.kt) — not directly shown in either reference, adapted reasonably from the Garantías/Inventario list pattern. */
@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Suscripciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
            items(state.subscriptions, key = { it.id }) { sub ->
                ListItemRow(
                    title = sub.name,
                    subtitle = "${sub.category} · renueva ${sub.renewsLabel}",
                    icon = Icons.Filled.Autorenew,
                    tone = BadgeTone.Primary,
                    pillLabel = sub.priceLabel,
                    pillTone = BadgeTone.Info,
                )
            }
        }
    }
}
