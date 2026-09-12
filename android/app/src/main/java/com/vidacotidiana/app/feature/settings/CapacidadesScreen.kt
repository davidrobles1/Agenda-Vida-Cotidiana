package com.vidacotidiana.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaMark
import com.vidacotidiana.app.core.ui.components.VidaPill
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaTile
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.core.ui.components.vidaSoftShadow

/**
 * CAPACIDADES — el estado real del propio proyecto, dentro de la aplicación.
 *
 * Es la pantalla del artefacto que declara qué exige el diseño y qué tiene el
 * backend. Existe por una razón concreta: cuando una pantalla no puede mostrar
 * algo porque falta una capacidad, lo HONESTO es decirlo aquí, no rellenarlo
 * con un dato inventado.
 *
 * El contenido está transcrito de
 * `Documentacion/35-artefacto-maestro-matriz-capacidades.md`, y se actualiza
 * cuando ese documento cambia — no al revés.
 */
private data class Capability(
    val name: String,
    val exists: Boolean,
    val action: String,
    val detail: String,
)

private val CAPABILITIES = listOf(
    Capability(
        "Ánimo del día (Bienestar)", true, "creado",
        "Módulo `mood` (V36): tablas mood_entry y mood_entry_tag, cuatro endpoints bajo /api/v1/moods. " +
            "Dato de salud: no se comparte, no se exporta y se borra en duro.",
    ),
    Capability(
        "Prioridad de una tarea", true, "creado",
        "Columna `priority` en reminders (V33) con DEFAULT NORMAL e índice por (owner, priority).",
    ),
    Capability(
        "Pasos dentro de una tarea", true, "creado",
        "Tabla reminder_steps (V34) y endpoints bajo /reminders/{id}/steps. " +
            "El porcentaje se DERIVA de done/total; no se almacena en ninguna parte.",
    ),
    Capability(
        "Contador diario de un hábito", true, "creado",
        "target_count y unit en routines más la tabla routine_progress (V35). " +
            "Con target_count nulo la rutina sigue siendo de sí/no, como antes.",
    ),
    Capability(
        "Adjuntos en cualquier recurso", true, "adaptado",
        "resource_type y resource_id en documents (V37), y V38 copia el blob de garantías a documents. " +
            "Las columnas antiguas se conservan: /warranties/{id}/content sigue funcionando.",
    ),
    Capability(
        "Tareas, pagos, garantías, mantenimiento, inventario, documentos, lugares, rutinas", true, "existía",
        "CRUD completo en los 23 controladores del backend. Ninguno se duplicó.",
    ),
    Capability(
        "Compartidos, Familia e invitaciones", true, "existía",
        "shared-resources (received/sent/part-done) y family. La búsqueda de usuarios exige 5 caracteres.",
    ),
    Capability(
        "Laboral: personas, proyectos, seguimientos, objetivos, recursos, Inbox", true, "existía",
        "people, projects (+participants), commitments, objectives, resources y notes.",
    ),
    Capability(
        "Deep link desde una notificación", false, "pendiente",
        "El envío existe y Android registra el dispositivo, pero el payload de FCM lleva solo tipo y mensaje. " +
            "Abrir el registro concreto exige añadirle el id — cambio de backend.",
    ),
)

@Composable
fun CapacidadesScreen(navController: NavHostController) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec
    var open by remember { mutableIntStateOf(-1) }

    val done = CAPABILITIES.count { it.exists }
    val pending = CAPABILITIES.size - done

    VidaScreen(
        title = "Capacidades",
        subtitle = "Lo que el diseño exige y lo que el backend tiene.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) {
            Row(
                Modifier.fillMaxWidth().height(86.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VidaTile(
                    kicker = "Implementadas", figure = done.toString(), caption = "de ${CAPABILITIES.size}",
                    background = SolidColor(c.successContainer),
                    figureColor = c.successText, kickerColor = c.successText,
                    captionColor = c.successText.copy(alpha = .75f),
                    figureSize = 26.dp, modifier = Modifier.weight(1.32f),
                )
                VidaTile(
                    kicker = "Pendientes", figure = pending.toString(), caption = "por crear",
                    background = SolidColor(c.warningContainer),
                    figureColor = c.warningText, kickerColor = c.warningText,
                    captionColor = c.warningText.copy(alpha = .75f),
                    figureSize = 26.dp, modifier = Modifier.weight(1f),
                )
            }
        }

        CAPABILITIES.forEachIndexed { i, cap ->
            StaggeredAppear(i + 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .vidaSoftShadow(spec.radii.card)
                        .background(c.surfaceVariant, RoundedCornerShape(spec.radii.card))
                        .border(spec.borderWidth, c.line, RoundedCornerShape(spec.radii.card))
                        .vidaClickable(onClick = { open = if (open == i) -1 else i })
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (open == i) 10.dp else 0.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VidaMark(
                            background = if (cap.exists) c.successContainer else c.warningContainer,
                            contentColor = if (cap.exists) c.successText else c.warningText,
                            boxSize = 32.dp,
                        ) {
                            Icon(
                                if (cap.exists) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(cap.name, style = t.cardTitle.copy(fontSize = 15.sp), color = c.text)
                            Text(
                                if (cap.exists) "Existe en el backend" else "No existe todavía",
                                style = t.caption,
                                color = c.textTertiary,
                            )
                        }
                        VidaPill(cap.action, if (cap.exists) PillTone.OK else PillTone.WARN)
                    }
                    if (open == i) {
                        Text(
                            cap.detail,
                            style = t.body,
                            color = c.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        )
                    }
                }
            }
        }

        StaggeredAppear(CAPABILITIES.size + 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.primaryContainer, RoundedCornerShape(20.dp))
                    .padding(horizontal = 17.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
                Text(
                    "Cuando el diseño y el backend no coinciden, gana el diseño y se implementa el backend. " +
                        "Ninguna pantalla finge datos para tapar un hueco.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.primaryDeep,
                )
            }
        }
    }
}
