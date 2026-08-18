package com.vidacotidiana.app.feature.calendar

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidacotidiana.app.core.mock.MockData
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.BadgeTone
import com.vidacotidiana.app.core.ui.components.CalendarView
import com.vidacotidiana.app.core.ui.components.ListItemRow
import com.vidacotidiana.app.core.ui.components.ListSectionCard
import com.vidacotidiana.app.core.ui.components.notebookBackground
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * UX-007: Calendario — month grid + a "Pendientes" list with checkboxes.
 * Real data: reminders (`GET /reminders`, marked on `dueAt`; completing one
 * here calls the exact same `POST /reminders/{id}/complete` RemindersScreen
 * uses, via `CalendarViewModel.completeReminder`) and pending invitations
 * (`GET /me/invitations`, listed only — accept/reject stays on
 * InvitationsScreen, not duplicated here). Mock data: Garantías/Mantenimiento
 * (`MockData`), visually distinguished with a "Simulado" pill + a different
 * icon/tone from real tasks, and their "completed" checkbox state is
 * local-only (`CalendarViewModel.mockCompletedIds`) — lost when the
 * ViewModel is recreated, never sent anywhere. Zero new backend endpoints.
 */
@Composable
fun CalendarScreen(
    onNavigateToInvitations: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }

    val remindersByDate = remember(state.reminders) {
        state.reminders.mapNotNull { r -> r.dueAt?.let { toLocalDate(it) to r } }.groupBy({ it.first }, { it.second })
    }

    val markersByDay = remember(remindersByDate) {
        buildMap<LocalDate, List<BadgeTone>> {
            remindersByDate.keys.forEach { put(it, listOf(BadgeTone.Primary)) }
            MockData.warranties.forEach { w ->
                merge(w.expiresOn, listOf(BadgeTone.Warning)) { a, b -> a + b }
            }
            MockData.maintenanceRecords.forEach { m ->
                merge(m.nextDueOn, listOf(BadgeTone.Info)) { a, b -> a + b }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .notebookBackground(VidaTheme.colors.border.copy(alpha = 0.2f))
            .padding(VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.lg),
    ) {
        item {
            Text("Calendario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VidaTheme.colors.text)
        }

        if (state.loading) {
            item { CircularProgressIndicator(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }

        state.error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }

        item {
            ListSectionCard(title = "Vista mensual") {
                Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                    CalendarView(
                        month = displayedMonth,
                        markersByDay = markersByDay,
                        onPrevMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                        onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                    )
                    CalendarLegend()
                }
            }
        }

        val pendingReminders = state.reminders.sortedBy { it.dueAt }
        val pendingWarranties = MockData.warranties.filter { it.id !in state.mockCompletedIds }
        val pendingMaintenance = MockData.maintenanceRecords.filter { it.id !in state.mockCompletedIds }

        if (pendingReminders.isNotEmpty() || pendingWarranties.isNotEmpty() || pendingMaintenance.isNotEmpty()) {
            item {
                ListSectionCard(title = "Pendientes") {
                    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                        pendingReminders.forEach { reminder ->
                            ListItemRow(
                                title = reminder.title,
                                subtitle = reminder.dueAt?.let { "Vence: ${formatDate(it)}" } ?: "",
                                icon = Icons.Filled.Description,
                                tone = BadgeTone.Primary,
                                trailing = {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { viewModel.completeReminder(reminder) },
                                        modifier = Modifier.semantics { contentDescription = "Marcar \"${reminder.title}\" como completada" },
                                    )
                                },
                            )
                        }
                        pendingWarranties.forEach { warranty ->
                            ListItemRow(
                                title = warranty.product,
                                subtitle = "Garantía · vence ${warranty.expiresLabel} · dato simulado",
                                icon = Icons.Filled.VerifiedUser,
                                tone = BadgeTone.Warning,
                                pillLabel = "Simulado",
                                pillTone = BadgeTone.Warning,
                                trailing = {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { viewModel.toggleMockComplete(warranty.id) },
                                        modifier = Modifier.semantics { contentDescription = "Marcar \"${warranty.product}\" como completada (dato simulado)" },
                                    )
                                },
                            )
                        }
                        pendingMaintenance.forEach { record ->
                            ListItemRow(
                                title = "${record.item} — ${record.task}",
                                subtitle = "Mantenimiento · próximo ${record.nextDueLabel} · dato simulado",
                                icon = Icons.Filled.Handyman,
                                tone = BadgeTone.Info,
                                pillLabel = "Simulado",
                                pillTone = BadgeTone.Info,
                                trailing = {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { viewModel.toggleMockComplete(record.id) },
                                        modifier = Modifier.semantics { contentDescription = "Marcar \"${record.item}\" como completado (dato simulado)" },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (state.invitations.isNotEmpty()) {
            item {
                ListSectionCard(title = "Invitaciones pendientes", onSeeAll = onNavigateToInvitations) {
                    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.md)) {
                        state.invitations.forEach { invitation ->
                            ListItemRow(
                                title = invitation.invitedEmail ?: "",
                                subtitle = "Invitación a compartir un recordatorio",
                                icon = Icons.Filled.Groups,
                                tone = BadgeTone.Info,
                                pillLabel = "Pendiente",
                                pillTone = BadgeTone.Warning,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.lg)) {
        LegendDot("Tareas", VidaTheme.colors.primary)
        LegendDot("Garantías (simulado)", VidaTheme.colors.warning)
        LegendDot("Mantenimiento (simulado)", VidaTheme.colors.info)
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.xs)) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = VidaTheme.colors.textSecondary)
    }
}

private fun toLocalDate(iso: String): LocalDate = Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()

private fun formatDate(iso: String): String =
    DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
