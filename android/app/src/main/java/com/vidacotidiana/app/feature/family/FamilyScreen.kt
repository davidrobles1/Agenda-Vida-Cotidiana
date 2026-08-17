package com.vidacotidiana.app.feature.family

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

/** UX-006: mock module (scaffolding only, MockData.kt) — matches the reference's "Mi familia" card. */
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg), verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        Text("Familia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            items(state.members, key = { it.id }) { member ->
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
                        val tone = BadgeTone.Primary.resolve()
                        Box(
                            modifier = Modifier.size(40.dp).background(tone.container, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(member.name.take(1), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tone.on)
                        }
                        Column {
                            Text(member.name, style = MaterialTheme.typography.bodyLarge, color = VidaTheme.colors.text)
                            Text(member.relationship, style = MaterialTheme.typography.labelMedium, color = VidaTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
