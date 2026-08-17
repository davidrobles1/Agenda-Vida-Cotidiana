package com.vidacotidiana.app.feature.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.resolve

/**
 * UX-006: mock module (scaffolding only, MockData.kt) — matches the
 * reference's Documentos screen (category folders + flat recent-files list).
 */
@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Documentos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
            items(state.categories) { category ->
                CategoryFolderCard(category.name, category.count)
            }
        }

        Text("Recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = VidaTheme.colors.text)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            items(state.documents, key = { it.id }) { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VidaShape.card),
                    colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(VidaSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val tone = BadgeTone.Info.resolve()
                        Box(
                            modifier = Modifier.size(36.dp).background(tone.container, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = tone.on, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.name, style = MaterialTheme.typography.bodyLarge, color = VidaTheme.colors.text)
                            Text(
                                "${doc.date} · ${doc.sizeLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VidaTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFolderCard(name: String, count: Int) {
    Card(
        shape = RoundedCornerShape(VidaShape.card),
        colors = CardDefaults.cardColors(containerColor = VidaTheme.colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(VidaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tone = BadgeTone.Info.resolve()
            Box(modifier = Modifier.size(36.dp).background(tone.container, RoundedCornerShape(VidaShape.control)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = tone.on, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = VidaTheme.colors.text)
                Text("$count documentos", style = MaterialTheme.typography.labelMedium, color = VidaTheme.colors.textSecondary)
            }
        }
    }
}
