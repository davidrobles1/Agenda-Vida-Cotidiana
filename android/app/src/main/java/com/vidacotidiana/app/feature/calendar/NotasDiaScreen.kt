package com.vidacotidiana.app.feature.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaScreen

/**
 * NOTAS DEL DÍA.
 *
 * Reglas del producto que esta pantalla respeta al pie de la letra, y que son
 * el motivo de que no sea una lista corriente:
 *
 *  · UN CLIC NO CREA NI EDITA. Abre el día, nada más. Una nota que se abre en
 *    edición al rozarla acaba llena de cambios accidentales.
 *  · EL DOBLE CLIC EDITA. Es el gesto deliberado.
 *  · EL TEXTO SE VE ENTERO, y si no cabe la nota hace scroll DENTRO de su
 *    propia caja — no se recorta con puntos suspensivos, que es justo lo que
 *    convierte una nota en un adorno.
 *
 * Datos reales de `/api/v1/day-notes` a través de `DayNotesRepository`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotasDiaScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec

    // El filete de color rota por posición: sin él, cinco notas seguidas son
    // cinco rectángulos iguales y cuesta distinguir dónde acaba una.
    val rails = listOf(c.second, c.primary, c.success, c.warning)

    VidaScreen(
        title = "Notas del día",
        subtitle = state.selectedDate.toString(),
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        when {
            state.notesLoading && state.notes.isEmpty() -> LoadingRows(3)

            state.notes.isEmpty() -> EmptyState(
                title = "Sin notas este día",
                body = "Lo que no quieras olvidar de hoy. Se queda pegado al día, no a una lista aparte.",
                action = "Escribir una" to { viewModel.addNote("") },
            )

            else -> {
                state.notes.forEachIndexed { i, note ->
                    StaggeredAppear(i) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(c.surfaceVariant, RoundedCornerShape(spec.radii.card))
                                .border(spec.borderWidth, c.line, RoundedCornerShape(spec.radii.card))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = c.primary),
                                    // Un clic NO edita: solo confirma qué día es.
                                    onClick = { viewModel.selectDate(state.selectedDate) },
                                    onDoubleClick = { viewModel.editNote(note, note.text) },
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .heightIn(min = 40.dp)
                                    .background(rails[i % rails.size], RoundedCornerShape(2.dp)),
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(state.selectedDate.toString(), style = t.micro, color = c.textTertiary)
                                // El texto ENTERO. Si no cabe, la caja se
                                // desplaza; no se trunca.
                                Box(
                                    Modifier
                                        .heightIn(max = 120.dp)
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        note.text.ifBlank { "Nota vacía" },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = spec.fonts.hand ?: spec.fonts.body,
                                            fontSize = 22.sp,
                                            lineHeight = 26.sp,
                                        ),
                                        color = if (note.text.isBlank()) c.textTertiary else c.textSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                StaggeredAppear(state.notes.size) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surfaceElevated, RoundedCornerShape(20.dp))
                            .padding(horizontal = 17.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Un clic abre el día. El doble clic edita la nota — nunca se crea sola.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
