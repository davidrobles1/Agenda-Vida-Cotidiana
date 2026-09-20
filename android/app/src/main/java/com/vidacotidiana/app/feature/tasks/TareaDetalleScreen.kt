package com.vidacotidiana.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.AttachTo
import com.vidacotidiana.app.core.data.percentDone
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaMark
import com.vidacotidiana.app.core.ui.components.VidaPill
import com.vidacotidiana.app.core.ui.components.VidaProp
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.VidaTextField
import com.vidacotidiana.app.core.ui.components.VidaAttachments
import com.vidacotidiana.app.core.ui.components.VidaFile
import com.vidacotidiana.app.core.ui.components.VidaTick
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.core.ui.components.vidaSoftShadow
import com.vidacotidiana.app.core.ui.VidaDates

/**
 * DETALLE DE UNA TAREA — la pantalla del artefacto maestro.
 *
 * Composición transcrita: héroe con degradado y anillo blanco de avance,
 * propiedades como píldoras de color que SON el control, y la lista de pasos
 * con casillas redondas.
 *
 * EL PORCENTAJE SE DERIVA, aquí también. `percentDone()` cuenta sobre la lista
 * que ya está en memoria; no hay un segundo número guardado ni en el cliente ni
 * en el servidor, así que marcar un paso mueve el anillo sin que nadie tenga
 * que acordarse de recalcular nada.
 *
 * DATOS REALES: `/api/v1/reminders/{id}/steps` (V34).
 */
@Composable
fun TareaDetalleScreen(
    taskId: String,
    viewModel: AppViewModel,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec

    LaunchedEffect(taskId) {
        viewModel.loadSteps(taskId)
        // Los adjuntos usan la mecánica genérica (V37): el mismo endpoint que
        // garantías, mantenimientos, artículos y pagos, cambiando el tipo.
        viewModel.loadAttachments(AttachTo.REMINDER, taskId)
    }

    val task = remember(state.reminders, taskId) {
        viewModel.allTasks().firstOrNull { it.id == taskId }
    }
    val steps = if (state.stepsFor == taskId) state.steps else emptyList()
    val pct = steps.percentDone()
    val doneCount = steps.count { it.done }

    var newStep by remember(taskId) { mutableStateOf("") }

    /**
     * Añadir el paso, escrito UNA vez.
     *
     * Lo piden dos sitios —el «+» de al lado y el «Realizado» del teclado— y
     * tienen que hacer exactamente lo mismo. Duplicar el cuerpo es cómo se
     * acaba con dos botones que dicen añadir y añaden cosas distintas: uno que
     * limpia el campo y otro que no, por ejemplo.
     */
    fun anadirPaso() {
        val texto = newStep.trim()
        if (texto.isNotBlank()) {
            viewModel.addStep(taskId, texto)
            newStep = ""
        }
    }

    // El tono del héroe sale del estado real de la tarea, no de un color fijo:
    // una tarea atrasada y una hecha no pueden presentarse igual.
    val tone = when {
        task?.done == true -> c.success
        task?.time != null -> c.primary
        else -> c.second
    }

    VidaScreen(
        title = "Tarea",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            VidaSmallButton("Editar", { viewModel.requestEdit(CreatableResource.TASK, taskId) }, ghost = true)
        },
    ) {
        if (task == null) {
            VidaError(
                title = "No encontramos esa tarea",
                body = "Puede que se haya borrado desde otro dispositivo.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
            return@VidaScreen
        }

        /* ── HÉROE ───────────────────────────────────────────────────────── */
        StaggeredAppear(0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .vidaSoftShadow(30.dp, color = tone, keyBlur = 32.dp, keyOffsetY = 14.dp, keyAlpha = 0.30f)
                    .background(tone, RoundedCornerShape(30.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // El anillo del héroe va en BLANCO sobre el tono, no en el tono
                // sobre blanco: es el mismo componente, pero aquí el fondo ya
                // lleva el color y repetirlo lo haría desaparecer.
                Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.size(76.dp)) {
                        val w = 7.dp.toPx()
                        val inset = w / 2f
                        val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                        drawArc(
                            color = Color.White.copy(alpha = 0.3f),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w),
                        )
                        if (pct > 0) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f, sweepAngle = 360f * pct / 100f, useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = w,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                ),
                            )
                        }
                    }
                    Text(
                        "$pct%",
                        style = t.heroFigure.copy(fontSize = 17.sp, lineHeight = 20.sp),
                        color = Color.White,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        task.title,
                        style = t.sectionTitle.copy(fontSize = 21.sp, lineHeight = 27.sp),
                        color = Color.White,
                    )
                    Text(
                        buildString {
                            if (steps.isNotEmpty()) append("$doneCount de ${steps.size} pasos · ")
                            append(task.meta)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }
        }

        /* ── PROPIEDADES ─────────────────────────────────────────────────── */
        StaggeredAppear(1) {
            Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing_sm)) {
                // EL ESTADO SE CAMBIA EN LOS DOS SENTIDOS.
                //
                // Esta propiedad ya se podía tocar, pero siempre llamaba a
                // `toggleTask`, que se plantaba sobre una tarea hecha
                // (`if (status == "COMPLETED") return`). Es decir: sobre la
                // tarea cerrada el gesto existía, invitaba a pulsarlo y no
                // hacía absolutamente nada.
                VidaProp(
                    name = "Estado",
                    value = if (task.done) "Hecha" else "Pendiente",
                    valueBackground = if (task.done) c.successContainer else c.primaryContainer,
                    valueForeground = if (task.done) c.successText else c.primaryDeep,
                    onClick = {
                        if (task.done) viewModel.reopenTask(task.id) else viewModel.toggleTask(task.id)
                    },
                )
                VidaProp(
                    name = "Cuándo",
                    // «Hoy» era un literal: la palabra estaba escrita a mano y
                    // no miraba `task.date`, así que una tarea de la semana
                    // pasada afirmaba ser de hoy. Ahora la frase se deriva de
                    // la fecha real, en el mismo sitio que el resto de la app.
                    value = VidaDates.dateTime(task.date, task.time),
                    valueBackground = c.warningContainer,
                    valueForeground = c.warningText,
                    onClick = { viewModel.requestEdit(CreatableResource.TASK, taskId) },
                )
                task.location?.takeIf { it.isNotBlank() }?.let { place ->
                    VidaProp(
                        name = "Dónde",
                        value = place,
                        valueBackground = c.sunken,
                        valueForeground = c.textSecondary,
                        onClick = { viewModel.requestEdit(CreatableResource.TASK, taskId) },
                    )
                }
            }
        }

        /* ── PASOS ───────────────────────────────────────────────────────── */
        StaggeredAppear(2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PASOS", style = t.eyebrow, color = c.textTertiary, modifier = Modifier.weight(1f))
                if (steps.isNotEmpty()) {
                    Text(
                        "$doneCount/${steps.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = tone,
                    )
                }
            }
        }

        StaggeredAppear(3) {
            when {
                state.stepsError != null && steps.isEmpty() -> VidaError(
                    title = "No se pudieron cargar los pasos",
                    body = state.stepsError ?: "",
                    onRetry = { viewModel.loadSteps(taskId) },
                )

                state.stepsLoading && steps.isEmpty() -> LoadingRows(2)

                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .vidaSoftShadow(spec.radii.card)
                        .background(c.surfaceVariant, RoundedCornerShape(spec.radii.card))
                        .border(spec.borderWidth, c.line, RoundedCornerShape(spec.radii.card))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (steps.isEmpty()) {
                        Text(
                            "Una tarea no necesita pasos. Añádelos solo si partirla en trozos te ayuda.",
                            style = t.body,
                            color = c.textSecondary,
                        )
                    }
                    steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .vidaClickable(onClick = { viewModel.toggleStep(taskId, step.id) }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            VidaTick(checked = step.done, tone = tone)
                            Text(
                                step.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                                color = if (step.done) c.textTertiary else c.text,
                                textDecoration = if (step.done) {
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                } else null,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Borrar paso",
                                tint = c.textTertiary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .vidaClickable(onClick = { viewModel.deleteStep(taskId, step.id) }),
                            )
                        }
                    }

                    // Añadir: campo + acción, sin salir de la pantalla.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            VidaTextField(
                                newStep, { newStep = it }, "Añadir paso",
                                // «Realizado» hace lo MISMO que el «+» de al
                                // lado, no algo parecido: una sola definición
                                // de qué significa añadir un paso.
                                onDone = { anadirPaso() },
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(VidaLayout.touchTarget)
                                .background(
                                    if (newStep.isBlank()) c.sunken else c.primary,
                                    RoundedCornerShape(spec.radii.control),
                                )
                                .vidaClickable(
                                    onClick = { anadirPaso() },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Añadir",
                                tint = if (newStep.isBlank()) c.textTertiary else c.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        state.stepsError?.takeIf { steps.isNotEmpty() }?.let { msg ->
            StaggeredAppear(4) {
                Text(msg, style = MaterialTheme.typography.bodySmall, color = c.error)
            }
        }

        /* ── ADJUNTOS (V37) ──────────────────────────────────────────────── */
        StaggeredAppear(4) {
            Text("ADJUNTOS", style = t.eyebrow, color = c.textTertiary)
        }
        StaggeredAppear(5) {
            // La sección entera —lo colgado y el gesto de colgar— vive en
            // `VidaAttachments`. Aquí solo se traducen los dos modelos de datos
            // a la fila que pinta el componente, y se elige QUÉ puede colgarse:
            // los documentos sueltos, nunca los que ya cuelgan de otro registro.
            val files = if (state.attachmentsFor == taskId) state.attachments else emptyList()
            val attachedIds = files.map { it.id }.toSet()
            val docs = state.data.documents.filterNot { it.id in attachedIds }
            VidaAttachments(
                attached = files.map {
                    VidaFile(
                        it.id, it.name,
                        "${it.contentType.substringAfterLast('/').uppercase()} · ${it.sizeBytes / 1024} KB",
                        it.isImage,
                    )
                },
                loose = docs.filter { it.resourceType == null }.map {
                    VidaFile(it.id, it.name, "${it.category} · ${it.sizeLabel}", false)
                },
                hangingElsewhere = docs.count { it.resourceType != null },
                loading = state.attachmentsLoading,
                onAttach = { viewModel.attachDocument(it, AttachTo.REMINDER, taskId) },
                onDetach = { viewModel.detachDocument(it, AttachTo.REMINDER, taskId) },
            )
        }

        /* ── ACCIONES ────────────────────────────────────────────────────── */
        StaggeredAppear(6) {
            Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing_sm)) {
                VidaPill(if (task.done) "Hecha" else "Pendiente", if (task.done) PillTone.OK else PillTone.NEUTRAL)
                if (task.shared) VidaPill("Compartida", PillTone.WARN)
            }
        }

        Spacer(Modifier.height(VidaLayout.sectionGap))
    }
}

/** Alias local: el hueco entre piezas hermanas del artefacto. */
private val VidaSpacing_sm = 10.dp
