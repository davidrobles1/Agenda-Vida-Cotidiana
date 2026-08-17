package com.vidacotidiana.app.feature.warranties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.mock.WarrantyStatus
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.ListItemRow

/** UX-006: mock module (scaffolding only, MockData.kt) — matches the reference's Garantías home metric. */
@Composable
fun WarrantiesScreen(viewModel: WarrantiesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Garantías", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
            items(state.warranties, key = { it.id }) { warranty ->
                val (pillLabel, pillTone) = when (warranty.status) {
                    WarrantyStatus.VIGENTE -> "Vigente" to BadgeTone.Success
                    WarrantyStatus.POR_VENCER -> "Por vencer" to BadgeTone.Warning
                    WarrantyStatus.VENCIDA -> "Vencida" to BadgeTone.Error
                }
                ListItemRow(
                    title = warranty.product,
                    subtitle = "${warranty.category} · vence ${warranty.expiresLabel}",
                    icon = Icons.Filled.VerifiedUser,
                    tone = BadgeTone.Success,
                    pillLabel = pillLabel,
                    pillTone = pillTone,
                )
            }
        }
    }
}
