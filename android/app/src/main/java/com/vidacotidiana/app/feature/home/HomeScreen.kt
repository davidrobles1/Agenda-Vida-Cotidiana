package com.vidacotidiana.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.AppTopBar
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.DonutChart
import com.vidacotidiana.app.core.ui.components.DonutSlice
import com.vidacotidiana.app.core.ui.components.ListItemRow
import com.vidacotidiana.app.core.ui.components.ListSectionCard
import com.vidacotidiana.app.core.ui.components.MetricCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * UX-006: Home dashboard — matches the reference's layout (metric cards +
 * "Próximas tareas"/"Recordatorios importantes" + a status donut), but only
 * with real data (HomeViewModel). No Documentos/Garantías/Gastos metric
 * cards — those are mock modules or excluded entirely (Finanzas), see
 * design-system.md's note on this Home-specific decision.
 */
@Composable
fun HomeScreen(
    onNavigateToTasks: () -> Unit,
    onNavigateToShared: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg),
    ) {
        item {
            AppTopBar(greetingName = state.greetingName.ifBlank { "…" }, subtitle = "Resumen de tu día")
        }

        if (state.loading) {
            item {
                CircularProgressIndicator(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }

        state.error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                MetricCard(
                    modifier = Modifier.weight(1f).testTag("metric_tasks"),
                    label = "Tareas",
                    value = state.pendingCount.toString(),
                    subtitle = if (state.overdueCount > 0) "${state.overdueCount} vencidas" else "al día",
                    subtitleTone = if (state.overdueCount > 0) BadgeTone.Error else null,
                    icon = Icons.Filled.Description,
                    tone = BadgeTone.Primary,
                    onClick = onNavigateToTasks,
                )
                MetricCard(
                    modifier = Modifier.weight(1f).testTag("metric_shared"),
                    label = "Compartidos",
                    value = state.sharedCount.toString(),
                    subtitle = "invitaciones pendientes",
                    icon = Icons.Filled.Groups,
                    tone = BadgeTone.Info,
                    onClick = onNavigateToShared,
                )
            }
        }

        if (state.upcoming.isNotEmpty()) {
            item {
                ListSectionCard(title = "Próximas tareas", onSeeAll = onNavigateToTasks) {
                    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                        state.upcoming.forEach { reminder ->
                            ListItemRow(
                                title = reminder.title,
                                subtitle = reminder.dueAt?.let(::formatDueAt) ?: "",
                                icon = Icons.Filled.Description,
                                tone = BadgeTone.Info,
                                pillLabel = "Pendiente",
                                pillTone = BadgeTone.Warning,
                            )
                        }
                    }
                }
            }
        }

        if (state.overdue.isNotEmpty()) {
            item {
                ListSectionCard(title = "Recordatorios importantes", onSeeAll = onNavigateToTasks) {
                    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                        state.overdue.forEach { reminder ->
                            ListItemRow(
                                title = reminder.title,
                                subtitle = reminder.dueAt?.let(::formatDueAt) ?: "",
                                icon = Icons.Filled.Warning,
                                tone = BadgeTone.Error,
                                pillLabel = "Vencida",
                                pillTone = BadgeTone.Error,
                            )
                        }
                    }
                }
            }
        }

        if (state.pendingCount + state.completedCount > 0) {
            item {
                ListSectionCard(title = "Progreso de tareas") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
                        DonutChart(
                            slices = listOf(
                                DonutSlice(state.completedCount.toFloat(), VidaTheme.colors.success),
                                DonutSlice(state.pendingCount.toFloat(), VidaTheme.colors.warning),
                            ),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.xs)) {
                            LegendRow("Completadas", state.completedCount, VidaTheme.colors.success)
                            LegendRow("Pendientes", state.pendingCount, VidaTheme.colors.warning)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VidaSpacing.xs)) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text("$label: $count", style = MaterialTheme.typography.bodyMedium, color = VidaTheme.colors.textSecondary)
    }
}

private fun formatDueAt(iso: String): String =
    DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
