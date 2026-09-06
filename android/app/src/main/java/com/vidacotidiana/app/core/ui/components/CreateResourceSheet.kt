package com.vidacotidiana.app.core.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.core.app.ReferenceSource
import com.vidacotidiana.app.core.data.FilePayload
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * El formulario de alta, uno solo para las diez cosas que se pueden crear.
 *
 * Se construye a partir de `CreatableResource.fields`, que declara los campos
 * que el endpoint realmente exige. Escribir diez hojas a mano habría dado diez
 * interpretaciones distintas de qué es obligatorio, y es exactamente así como
 * un formulario acaba mandando algo que el backend rechaza —o, peor, mostrando
 * un "Guardar" que no guarda.
 *
 * Guardar se habilita solo cuando los campos obligatorios están completos: el
 * usuario ve la razón antes de pulsar, en vez de recibir un 400 después.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateResourceSheet(
    resource: CreatableResource,
    /** Valores con los que abrir la hoja: vacío al crear, los del recurso al editar. */
    initialValues: Map<String, String> = emptyMap(),
    /** Cambia el título y el verbo del botón; los campos son los mismos. */
    editing: Boolean = false,
    people: List<Pair<String, String>>,
    projects: List<Pair<String, String>>,
    saving: Boolean,
    error: String?,
    onSubmit: (Map<String, String>, FilePayload?) -> Unit,
    onPickFile: (Uri, (FilePayload?) -> Unit) -> Unit,
    onCancel: () -> Unit,
) {
    val c = VidaTheme.colors
    val values = remember(resource, initialValues) {
        mutableStateMapOf<String, String>().apply { putAll(initialValues) }
    }
    var file by remember(resource) { mutableStateOf<FilePayload?>(null) }
    var readingFile by remember(resource) { mutableStateOf(false) }
    var datePickerFor by remember { mutableStateOf<String?>(null) }
    var timePickerFor by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            readingFile = true
            onPickFile(uri) { payload ->
                file = payload
                readingFile = false
            }
        }
    }

    // Un campo obligatorio de tipo archivo se cumple con el archivo, no con el
    // mapa de textos: si no se comprobara aparte, "Guardar" quedaría activo
    // sin nada que subir.
    // Una fecha sin hora NO es válida en un campo de fecha y hora, aunque el
    // campo entero sea opcional: media selección produciría un registro que
    // aparece en el calendario a una hora que el usuario nunca eligió. La
    // alternativa —rellenar 00:00— es exactamente inventarse el dato.
    val halfPicked = resource.fields.filterIsInstance<FormField.DateTime>().any { field ->
        val raw = values[field.key].orEmpty()
        raw.isNotBlank() && raw.substringAfter('T', "").isBlank()
    }

    val complete = !halfPicked && resource.fields.all { field ->
        !field.required || when (field) {
            is FormField.File -> file != null
            else -> values[field.key]?.isNotBlank() == true
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VidaSpacing.lg)
            .padding(bottom = VidaSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        Text(
            if (editing) "Editar" else resource.title,
            style = MaterialTheme.typography.headlineSmall,
            color = c.text,
        )

        resource.fields.forEach { field ->
            when (field) {
                is FormField.Text -> LabeledField(field.label, field.required) {
                    VidaTextField(
                        values[field.key].orEmpty(),
                        { values[field.key] = it },
                        field.label,
                        multiline = field.multiline,
                    )
                }

                is FormField.Number -> LabeledField(field.label, field.required) {
                    VidaTextField(
                        values[field.key].orEmpty(),
                        { input ->
                            // Se filtra al teclear: un importe con letras solo
                            // se descubriría al fallar el guardado.
                            val allowed = if (field.decimal) "0123456789.," else "0123456789"
                            values[field.key] = input.filter { it in allowed }
                        },
                        field.label,
                        numeric = true,
                    )
                }

                is FormField.Date -> LabeledField(field.label, field.required) {
                    val shown = values[field.key]?.let { prettyDate(it) } ?: "Elegir fecha"
                    VidaSmallButton(shown, { datePickerFor = field.key }, ghost = true)
                }

                is FormField.DateTime -> LabeledField(field.label, field.required) {
                    val raw = values[field.key].orEmpty()
                    val datePart = raw.substringBefore('T')
                    val timePart = raw.substringAfter('T', "")
                    Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
                        VidaSmallButton(
                            if (datePart.isNotBlank()) prettyDate(datePart) else "Elegir fecha",
                            { datePickerFor = field.key },
                            ghost = true,
                        )
                        // La hora solo se ofrece cuando ya hay día: elegir las
                        // 14:30 de ningún día no significa nada.
                        VidaSmallButton(
                            if (timePart.isNotBlank()) timePart else "Elegir hora",
                            { timePickerFor = field.key },
                            ghost = true,
                            enabled = datePart.isNotBlank(),
                        )
                    }
                }

                is FormField.Choice -> LabeledField(field.label, field.required) {
                    VidaChipRow(
                        field.options.map { it.second },
                        field.options.firstOrNull { it.first == values[field.key] }?.second ?: "",
                        { label -> values[field.key] = field.options.first { it.second == label }.first },
                    )
                }

                is FormField.Reference -> {
                    val source = if (field.source == ReferenceSource.PEOPLE) people else projects
                    LabeledField(field.label, field.required) {
                        if (source.isEmpty()) {
                            // Se dice qué falta y por qué, en vez de mostrar un
                            // selector vacío que parece roto.
                            Text(
                                if (field.source == ReferenceSource.PEOPLE) {
                                    "Primero registra a una persona: un seguimiento es con alguien."
                                } else {
                                    "Todavía no tienes proyectos."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        } else {
                            VidaChipRow(
                                source.map { it.second },
                                source.firstOrNull { it.first == values[field.key] }?.second ?: "",
                                { label -> values[field.key] = source.first { it.second == label }.first },
                            )
                        }
                    }
                }

                is FormField.File -> LabeledField(field.label, field.required) {
                    VidaSmallButton(
                        when {
                            readingFile -> "Leyendo…"
                            file != null -> file!!.fileName
                            else -> "Elegir archivo"
                        },
                        { picker.launch(field.mime) },
                        ghost = true,
                    )
                }
            }
        }

        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.error)
        }

        if (!complete) {
            Text(
                if (halfPicked) {
                    "Elige también la hora, o quita la fecha: una tarea con día pero sin hora aparecería en el calendario a una hora que no elegiste."
                } else {
                    "Completa los campos marcados para guardar."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (halfPicked) c.warningText else c.textSecondary,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            VidaSmallButton("Cancelar", onCancel, ghost = true)
            VidaSmallButton(
                if (saving) "Guardando…" else "Guardar",
                { if (complete && !saving) onSubmit(values.toMap(), file) },
                enabled = complete && !saving,
            )
        }
    }

    datePickerFor?.let { key ->
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = values[key]
                ?.let { runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() }
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // El selector devuelve medianoche UTC: se lee en UTC para
                        // no desplazar el día en zonas negativas.
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        val isDateTime = resource.fields.any { it is FormField.DateTime && it.key == key }
                        values[key] = if (isDateTime) {
                            // Cambiar el día no debe borrar la hora ya elegida.
                            val time = values[key]?.substringAfter('T', "").orEmpty()
                            if (time.isBlank()) day else day + "T" + time
                        } else {
                            day
                        }
                    }
                    datePickerFor = null
                }) { Text("Elegir") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFor = null }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    timePickerFor?.let { key ->
        val existing = values[key]?.substringAfter('T', "").orEmpty()
        val timeState = rememberTimePickerState(
            initialHour = existing.substringBefore(':').toIntOrNull() ?: 9,
            initialMinute = existing.substringAfter(':', "").toIntOrNull() ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val day = values[key]?.substringBefore('T').orEmpty()
                    if (day.isNotBlank()) {
                        values[key] = day + "T" + "%02d:%02d".format(timeState.hour, timeState.minute)
                    }
                    timePickerFor = null
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { timePickerFor = null }) { Text("Cancelar") } },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun LabeledField(label: String, required: Boolean, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VidaSpacing.xs)) {
        Eyebrow(if (required) label else "$label · opcional", rule = false)
        Box(Modifier.fillMaxWidth()) { content() }
    }
}

private val PRETTY: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es"))

private fun prettyDate(iso: String): String =
    runCatching { LocalDate.parse(iso).format(PRETTY) }.getOrDefault(iso)
