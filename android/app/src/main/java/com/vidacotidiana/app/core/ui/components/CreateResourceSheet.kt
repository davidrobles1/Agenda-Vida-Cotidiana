package com.vidacotidiana.app.core.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.R
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.DateShortcut
import com.vidacotidiana.app.core.app.FieldIcon
import com.vidacotidiana.app.core.app.FieldTone
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.core.app.ReferenceSource
import com.vidacotidiana.app.core.data.FilePayload
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ══════════════════════════════════════════════════════════════════════════
   MEDIDAS. Una sola vez, arriba, porque son las que hacen que nada se salga
   de su caja: la cabecera reserva sitio para la hoja, el título se limita a
   dos tercios del ancho para no meterse debajo de ella, y el icono, el
   chevron y el texto de cada fila suman exactamente el ancho disponible.
   ══════════════════════════════════════════════════════════════════════════ */
private val SHEET_PAD = 18.dp
/**
 * Alto de la cabecera. Doce dedos más que la hoja a propósito: con el recurso
 * midiendo 120 dp de alto, sobra margen y el recorte de la caja no llega a
 * tocarla nunca.
 */
private val HEADER_H = 132.dp
/** 156 × 120 dp — la proporción exacta del recurso (820 × 631), sin deformarla. */
private val LEAF_W = 156.dp
private val TITLE_WIDTH_FRACTION = 0.66f
private val HERO_MIN_H = 96.dp
private val ROW_ICON = 38.dp
private val CHEVRON = 18.dp
/** Sangría del cajón: alinea su contenido bajo el texto de la fila, no bajo el icono. */
private val DRAWER_INDENT = ROW_ICON + 12.dp

private val PRETTY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es"))
private val PRETTY_SHORT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMM yyyy", Locale.forLanguageTag("es"))

/** Las horas que de verdad se usan. «Otra…» abre el reloj de siempre. */
private val HORAS = listOf("09:00", "13:00", "18:00", "21:00")

/**
 * El formulario de alta, uno solo para las catorce cosas que se pueden crear.
 *
 * Se construye a partir de `CreatableResource.fields`, que declara los campos
 * que el endpoint realmente exige. Escribir catorce hojas a mano habría dado
 * catorce interpretaciones de qué es obligatorio, y es exactamente así como un
 * formulario acaba mandando algo que el backend rechaza —o, peor, mostrando un
 * "Guardar" que no guarda.
 *
 * ── LA FORMA, aprobada ────────────────────────────────────────────────────
 *
 * UN CAMPO ABIERTO Y EL RESTO EN FILAS. Antes los campos se dibujaban todos
 * iguales —rótulo en versalitas y caja hundida—, así que una tarea que solo
 * necesita un título enseñaba cuatro bloques idénticos y el ojo contaba cuatro
 * deberes antes de leer ninguno. Ahora el principal va abierto, grande y con el
 * cursor dentro, y cada campo restante es una fila de una línea con su icono y
 * su respuesta. No se esconde nada: se nombra y se abre de un toque.
 *
 * SE RESPONDE EN EL SITIO. Ningún campo abre un diálogo salvo dos en los que el
 * diálogo es lo correcto: el calendario para una fecha lejana y el selector de
 * archivos, que es del teléfono. Poner fecha y hora eran siete toques y dos
 * diálogos; ahora son tres y ninguno.
 *
 * EL RÓTULO, UNA SOLA VEZ. El marcador de posición recibía `field.label`, así
 * que cada campo decía su nombre dos veces. Ahora lleva un ejemplo real
 * (`FormField.Text.example`), que enseña el grano esperado sin gastar una línea
 * de ayuda.
 *
 * EL PIE NO SE DESPLAZA, y mientras falte algo el botón lo nombra y lleva a
 * ello. Antes decía «Completa los campos marcados» sin señalar cuál.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateResourceSheet(
    resource: CreatableResource,
    /** Valores con los que abrir la hoja: el borrador al crear, los del recurso al editar. */
    initialValues: Map<String, String> = emptyMap(),
    /** Cambia el título y el verbo del botón; los campos son los mismos. */
    editing: Boolean = false,
    people: List<Pair<String, String>>,
    projects: List<Pair<String, String>>,
    inventory: List<Pair<String, String>>,
    saving: Boolean,
    error: String?,
    onSubmit: (Map<String, String>, FilePayload?) -> Unit,
    onPickFile: (Uri, (FilePayload?) -> Unit) -> Unit,
    /**
     * Crear AQUÍ el registro del que depende este formulario.
     *
     * Un vínculo obligatorio que exige que ya exista otro registro es una
     * trampa: sin ningún artículo, "Nueva garantía" no se puede guardar, y sin
     * ninguna persona tampoco un seguimiento. Sigue estando, ahora dentro de la
     * fila de ese vínculo, junto a las fichas.
     *
     * Misma forma que `onPickFile`: el último parámetro recibe el id creado, o
     * `null` si falló.
     */
    onQuickCreate: (ReferenceSource, String, String?, (String?) -> Unit) -> Unit,
    /** Lo tecleado, para que sobreviva a cerrar la hoja. Nulo al editar. */
    onDraft: ((Map<String, String>) -> Unit)? = null,
    /** Tirar el borrador y empezar en blanco. Nulo si no hay ninguno. */
    onDiscardDraft: (() -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val values = remember(resource, initialValues) {
        mutableStateMapOf<String, String>().apply { putAll(initialValues) }
    }
    var file by remember(resource) { mutableStateOf<FilePayload?>(null) }
    var readingFile by remember(resource) { mutableStateOf(false) }
    /** Qué fila está abierta, por clave. Solo una: dos abiertas serían dos formularios. */
    var openKey by remember(resource) { mutableStateOf<String?>(null) }
    var datePickerFor by remember { mutableStateOf<String?>(null) }
    var timePickerFor by remember { mutableStateOf<String?>(null) }
    var pickFileFor by remember { mutableStateOf<String?>(null) }
    /**
     * El foco del campo principal, izado hasta aquí porque lo necesitan dos:
     * la apertura de la hoja y el botón del pie. Cuando lo que falta es ESE
     * campo no hay ninguna fila que abrir, y sin esto el botón habría sido un
     * gesto mudo — justo el tipo de botón que esta revisión viene a quitar.
     */
    val heroFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickFileFor = null
        if (uri != null) {
            readingFile = true
            onPickFile(uri) { payload ->
                file = payload
                readingFile = false
            }
        }
    }

    // El borrador se guarda al dejar de teclear, no en cada tecla: escribir un
    // título de cuarenta letras no tiene por qué producir cuarenta escrituras
    // de estado que recompongan la hoja entera.
    if (onDraft != null) {
        LaunchedEffect(resource) {
            snapshotFlow { values.toMap() }.collect { actuales ->
                delay(400)
                onDraft(actuales)
            }
        }
    }

    // Un campo obligatorio de tipo archivo se cumple con el archivo, no con el
    // mapa de textos: si no se comprobara aparte, "Crear" quedaría activo sin
    // nada que subir.
    fun respondido(f: FormField): Boolean = when (f) {
        is FormField.File -> file != null
        // Una fecha sin hora NO es válida en un campo de fecha y hora, aunque el
        // campo entero sea opcional: media selección produciría un registro que
        // aparece en el calendario a una hora que el usuario nunca eligió. Con
        // los atajos esto casi no puede pasar —día y hora se eligen seguidos—,
        // pero el camino largo del calendario sigue pudiendo quedarse a medias.
        is FormField.DateTime -> values[f.key].orEmpty().substringAfter('T', "").isNotBlank()
        else -> values[f.key]?.isNotBlank() == true
    }

    val falta = resource.fields.firstOrNull { it.required && !respondido(it) }
    val mediaFecha = resource.fields.filterIsInstance<FormField.DateTime>().firstOrNull { f ->
        val raw = values[f.key].orEmpty()
        raw.isNotBlank() && raw.substringAfter('T', "").isBlank()
    }
    val completo = falta == null && mediaFecha == null

    Column(
        Modifier
            .fillMaxWidth()
            // La hoja vive en SU PROPIA ventana, así que no hereda el hueco que
            // `VidaScreen` hace para el teclado: tiene que pedirlo ella. Va
            // ANTES del scroll para que el hueco lo haga el contenedor y la
            // zona desplazable se encoja — que es lo que permite al campo
            // enfocado subir hasta quedar a la vista.
            .imePadding(),
    ) {
        Column(
            Modifier
                // `fill = false`: ocupa lo que necesite y, si el contenido no
                // llega, la hoja se queda baja en vez de estirarse hasta el
                // borde con un hueco vacío bajo la última fila.
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            SheetHeader(
                title = if (editing) "Editar" else resource.title,
                // LA HOJA SE DESVANECE AL ABRIR UNA FILA. Mientras se responde
                // algo, el adorno deja paso; al cerrar, vuelve. No desaparece
                // del todo —seguiría estando, apagada— porque un adorno que
                // parpadea entre existir y no existir llama más la atención que
                // el contenido que venía a acompañar.
                leafAlpha = if (openKey == null) 1f else 0.14f,
                onClose = onCancel,
            )

            Column(Modifier.padding(horizontal = SHEET_PAD)) {
                val head = resource.headField
                if (head != null) {
                    HeroField(
                        field = head,
                        value = values[head.key].orEmpty(),
                        onValueChange = { values[head.key] = it },
                        // El cursor entra solo al crear. Editando no: la hoja se
                        // abre llena y lo que el usuario quiere tocar casi nunca
                        // es la primera línea.
                        autoFocus = !editing,
                        focusRequester = heroFocus,
                    )
                }

                resource.rowFields.forEach { field ->
                    FieldRow(
                        field = field,
                        open = openKey == field.key,
                        summary = resumen(field, values, file, readingFile, people, projects, inventory),
                        answered = respondido(field),
                        onToggle = {
                            // Un archivo no tiene nada que desplegar: la fila ES
                            // el botón, y abre el selector del teléfono directo.
                            // Una escala menos que antes.
                            if (field is FormField.File) {
                                pickFileFor = field.key
                                picker.launch(field.mime)
                            } else {
                                // SE SUELTA EL FOCO AL ABRIR UNA FILA, y con él
                                // se va el teclado. Sin esto seguía abierto —el
                                // campo principal lo retenía— y comía media
                                // pantalla justo cuando aparecían las fichas que
                                // hay que ver para elegir.
                                //
                                // Las filas que SÍ se escriben no pierden nada:
                                // su caja pide el foco al desplegarse y el
                                // teclado vuelve por su cuenta.
                                focusManager.clearFocus()
                                openKey = if (openKey == field.key) null else field.key
                            }
                        },
                    ) {
                        FieldDrawer(
                            field = field,
                            values = values,
                            people = people,
                            projects = projects,
                            inventory = inventory,
                            onAnswer = { key, value ->
                                values[key] = value
                                // Responder CIERRA la fila. Es lo que hace que
                                // la hoja vuelva a caber de un vistazo en vez de
                                // ir creciendo con cada campo contestado.
                                openKey = null
                            },
                            onPartial = { key, value -> values[key] = value },
                            onOpenCalendar = { datePickerFor = it },
                            onOpenClock = { timePickerFor = it },
                            onQuickCreate = onQuickCreate,
                        )
                    }
                }

                // Solo lo que el usuario necesita saber, y solo cuando lo
                // necesita. Antes había una línea fija diciendo «Completa los
                // campos marcados» incluso con todo puesto.
                error?.let {
                    Spacer(Modifier.height(VidaSpacing.xs))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.error,
                    )
                }
                // La media fecha NO lleva párrafo propio: la fila ya dice «falta
                // la hora» en su segunda línea y el botón del pie lo repite, así
                // que un tercer aviso aquí abajo solo añadía un texto que además
                // caía justo contra el pie y se veía cortado.
                // Solo aparece cuando de verdad se retomó algo. Decirlo importa:
                // encontrarse la hoja medio llena sin explicación parecería un
                // fallo, y sin salida obligaría a borrar campo por campo.
                onDiscardDraft?.let { descartar ->
                    Spacer(Modifier.height(VidaSpacing.sm))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                    ) {
                        Text(
                            "Seguimos donde lo dejaste.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        OptionChip("Empezar de cero", escape = true, onClick = descartar)
                    }
                }
                Spacer(Modifier.height(VidaSpacing.md))
            }
        }

        /* EL PIE NO SE DESPLAZA. Antes «Guardar» vivía al final del scroll, así
           que en un recurso de trabajo —seis campos— había que recorrer la hoja
           entera para llegar a él aunque los dos obligatorios ya estuvieran. */
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.surfaceVariant)
                .padding(horizontal = SHEET_PAD, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VidaSmallButton("Cancelar", onCancel, ghost = true, enabled = !saving)
            Box(Modifier.weight(1f)) {
                VidaSmallButton(
                    text = when {
                        saving -> "Guardando…"
                        falta != null -> "Falta: " + falta.label.trim('¿', '?').lowercase()
                        mediaFecha != null -> "Falta la hora"
                        else -> resource.verb
                    },
                    onClick = {
                        when {
                            saving -> Unit
                            // El botón LLEVA a lo que falta en vez de no hacer
                            // nada. Con seis campos, «completa los marcados»
                            // obligaba a revisarlos uno a uno.
                            falta != null ->
                                if (falta.key == resource.headField?.key) {
                                    // El principal no es una fila: se le da el
                                    // cursor, que es su forma de «abrirse».
                                    runCatching { heroFocus.requestFocus() }
                                } else {
                                    openKey = falta.key
                                }
                            mediaFecha != null -> openKey = mediaFecha.key
                            else -> onSubmit(values.toMap(), file)
                        }
                    },
                    enabled = !saving,
                    ghost = !completo,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(VidaSpacing.xs))
    }

    /* ── Los dos únicos diálogos, y los dos están justificados ───────────── */

    datePickerFor?.let { key ->
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = values[key]?.substringBefore('T')
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
                        val esFechaHora = resource.fields.any { it is FormField.DateTime && it.key == key }
                        if (esFechaHora) {
                            // Cambiar el día no borra la hora ya elegida.
                            val hora = values[key]?.substringAfter('T', "").orEmpty()
                            values[key] = if (hora.isBlank()) day else "$day" + "T" + hora
                            // Sin hora todavía: la fila se queda abierta en la
                            // tira de horas, que es el paso que falta.
                            openKey = if (hora.isBlank()) key else null
                        } else {
                            values[key] = day
                            openKey = null
                        }
                    }
                    datePickerFor = null
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { datePickerFor = null }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    timePickerFor?.let { key ->
        val existente = values[key]?.substringAfter('T', "").orEmpty()
        val timeState = rememberTimePickerState(
            initialHour = existente.substringBefore(':').toIntOrNull() ?: 9,
            initialMinute = existente.substringAfter(':', "").toIntOrNull() ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val dia = values[key]?.substringBefore('T').orEmpty()
                    if (dia.isNotBlank()) {
                        values[key] = dia + "T" + "%02d:%02d".format(timeState.hour, timeState.minute)
                        openKey = null
                    }
                    timePickerFor = null
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { timePickerFor = null }) { Text("Cancelar") } },
            text = { TimePicker(state = timeState) },
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   CABECERA
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Cerrar, el nombre del alta y la hoja de la identidad.
 *
 * La hoja va DENTRO de una caja de altura fija con `clipToBounds`, así que no
 * puede desbordar por arriba ni por los lados por mucho que se agrande, y se va
 * con el desplazamiento en vez de quedarse flotando sobre las filas. Se dibuja
 * con `ContentScale.Fit`, que conserva su proporción: estirarla la deformaría,
 * y es una ilustración, no una textura.
 *
 * El título se limita a dos tercios del ancho, que es lo que garantiza que
 * «Nuevo mantenimiento» parta en dos líneas antes de meterse debajo de la hoja.
 */
@Composable
private fun SheetHeader(title: String, leafAlpha: Float, onClose: () -> Unit) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val alpha by animateFloatAsState(
        targetValue = leafAlpha,
        animationSpec = tween(durationMillis = 420),
        label = "hoja",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(HEADER_H)
            .clipToBounds(),
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.hoja_cotidiana),
            contentDescription = null,
            // `Fit` conserva la proporción: estirarla la deformaría, y es una
            // ilustración, no una textura.
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Entra ENTERA. Antes iba desplazada 30 dp a la derecha y 22
                // hacia arriba, así que la caja le cortaba las hojas de arriba
                // y la de la derecha — se veía un manchón, no una rama. Ahora
                // solo asoma un poco por el canto, y lo que asoma es halo
                // transparente: el recurso se desvanece a cero en sus bordes.
                .offset(x = 12.dp, y = 4.dp)
                .width(LEAF_W)
                .alpha(alpha * if (spec.isDark) 0.95f else 0.85f)
                .rotate(5f),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = SHEET_PAD - 6.dp, end = SHEET_PAD, bottom = 14.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .vidaClickable(onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "Cerrar", tint = c.textSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (spec.fonts.displayUppercase) title.uppercase() else title,
                fontFamily = spec.fonts.display,
                fontWeight = spec.fonts.displayWeight,
                fontSize = 27.sp,
                lineHeight = 31.sp,
                color = c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .fillMaxWidth(TITLE_WIDTH_FRACTION),
            )
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   EL CAMPO PRINCIPAL
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Abierto, grande y con el cursor dentro al aparecer la hoja.
 *
 * Es el único campo que las catorce altas tienen en común —un texto obligatorio
 * como primer campo—, y por eso puede abrirse siempre sin excepciones por
 * recurso.
 */
@Composable
private fun HeroField(
    field: FormField.Text,
    value: String,
    onValueChange: (String) -> Unit,
    autoFocus: Boolean,
    focusRequester: FocusRequester,
) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val focus = focusRequester

    if (autoFocus) {
        // Con retraso: pedir el foco mientras la hoja todavía está entrando
        // hace que el teclado y la animación de apertura se peleen, y el
        // resultado es un salto.
        LaunchedEffect(field.key) {
            delay(280)
            runCatching { focus.requestFocus() }
        }
    }

    Eyebrow(field.label, rule = false)
    Spacer(Modifier.height(6.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .background(c.sunken, shape)
            .border(1.6.dp, c.primary, shape)
            .defaultMinSize(minHeight = HERO_MIN_H)
            .padding(horizontal = 15.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                // EL EJEMPLO, no el rótulo otra vez. Antes aquí iba
                // `field.label`, así que la pregunta se leía dos veces seguidas.
                field.example.ifBlank { field.label },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
                color = c.textTertiary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
            ),
            textStyle = LocalTextStyle.current
                .merge(MaterialTheme.typography.bodyLarge)
                .copy(color = c.text, fontSize = 15.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            tint = c.textTertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(17.dp),
        )
    }
    Spacer(Modifier.height(VidaSpacing.md))
}

/* ══════════════════════════════════════════════════════════════════════════
   LA FILA
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Una fila de campo: icono, nombre, respuesta y chevron.
 *
 * NADA SE SALE. El icono y el chevron tienen ancho fijo, el texto ocupa el
 * resto con `weight(1f)` y las dos líneas van a `maxLines = 1` con puntos
 * suspensivos. Con eso, ni un nombre largo ni una respuesta larga pueden
 * empujar al chevron fuera de la tarjeta.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FieldRow(
    field: FormField,
    open: Boolean,
    summary: String,
    answered: Boolean,
    onToggle: () -> Unit,
    drawer: @Composable () -> Unit,
) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val giro by animateFloatAsState(if (open) 180f else 0f, tween(200), label = "chevron")

    // LA FILA ABIERTA SUBE HASTA VERSE ENTERA. Abrir la última de un recurso de
    // trabajo —seis campos— desplegaba sus fichas justo debajo del borde
    // visible, así que había que desplazarse a mano para ver lo que uno acababa
    // de pedir. Se espera a que el despliegue termine antes de pedir sitio: si
    // no, se pide para la altura vieja y falta justo el trozo nuevo.
    val enVista = remember { BringIntoViewRequester() }
    LaunchedEffect(open) {
        if (open) {
            delay(220)
            runCatching { enVista.bringIntoView() }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(enVista)
            .padding(bottom = 9.dp)
            .background(if (open) c.surface else c.sunken, shape)
            .then(if (open) Modifier.border(1.4.dp, c.primary, shape) else Modifier)
            .clipToBounds(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .vidaClickable(onToggle)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val (fondo, tinta) = toneColors(field.tone)
            Box(
                Modifier.size(ROW_ICON).background(fondo, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconFor(field.icon), null, tint = tinta, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    field.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 18.sp),
                    fontWeight = FontWeight.Medium,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 15.sp),
                    // La respuesta se distingue de la invitación: en color de
                    // acento cuando ya hay algo, apagada cuando es «Opcional» y
                    // en ámbar cuando es un «Obligatorio» todavía sin responder.
                    color = when {
                        answered -> c.primary
                        field.required -> c.warningText
                        else -> c.textTertiary
                    },
                    fontWeight = if (answered) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                null,
                tint = c.textTertiary,
                modifier = Modifier.size(CHEVRON).rotate(giro),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(160)) + expandVertically(tween(200)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(180)),
        ) {
            Column(
                Modifier.padding(start = DRAWER_INDENT + 13.dp, end = 13.dp, bottom = 13.dp),
            ) { drawer() }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   EL CAJÓN — lo que hay dentro de una fila abierta, según su tipo
   ══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldDrawer(
    field: FormField,
    values: MutableMap<String, String>,
    people: List<Pair<String, String>>,
    projects: List<Pair<String, String>>,
    inventory: List<Pair<String, String>>,
    onAnswer: (String, String) -> Unit,
    onPartial: (String, String) -> Unit,
    onOpenCalendar: (String) -> Unit,
    onOpenClock: (String) -> Unit,
    onQuickCreate: (ReferenceSource, String, String?, (String?) -> Unit) -> Unit,
) {
    val c = VidaTheme.colors
    val hoy = LocalDate.now()

    when (field) {
        is FormField.DateTime -> {
            val raw = values[field.key].orEmpty()
            val dia = raw.substringBefore('T').takeIf { it.isNotBlank() }
            val hora = raw.substringAfter('T', "").takeIf { it.isNotBlank() }

            DrawerLabel("Qué día")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                field.shortcuts.forEach { atajo ->
                    val fecha = atajo.dateFrom(hoy)
                    OptionChip(atajo.label, seleccionado = dia == fecha.toString()) {
                        // Cambiar de día conserva la hora ya elegida; si aún no
                        // hay hora, la fila se queda abierta en el paso que falta.
                        val nuevo = fecha.toString() + (hora?.let { "T$it" } ?: "")
                        if (hora == null) onPartial(field.key, nuevo) else onAnswer(field.key, nuevo)
                    }
                }
                OptionChip("Otro día…", escape = true) { onOpenCalendar(field.key) }
            }
            dia?.let {
                DrawerCaption(
                    runCatching { LocalDate.parse(it).format(PRETTY) }.getOrDefault(it),
                )
            }

            // La hora solo se ofrece cuando ya hay día: elegir las 14:30 de
            // ningún día no significa nada.
            if (dia != null) {
                Spacer(Modifier.height(10.dp))
                DrawerLabel("A qué hora")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HORAS.forEach { h ->
                        OptionChip(h, seleccionado = hora == h) { onAnswer(field.key, "$dia" + "T" + h) }
                    }
                    OptionChip("Otra…", escape = true) { onOpenClock(field.key) }
                }
            }
        }

        is FormField.Date -> {
            val actual = values[field.key].orEmpty()
            DrawerLabel("Cuándo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                field.shortcuts.forEach { atajo ->
                    val fecha = atajo.dateFrom(hoy)
                    OptionChip(atajo.label, seleccionado = actual == fecha.toString()) {
                        onAnswer(field.key, fecha.toString())
                    }
                }
                OptionChip("Otro día…", escape = true) { onOpenCalendar(field.key) }
            }
            actual.takeIf { it.isNotBlank() }?.let {
                DrawerCaption(runCatching { LocalDate.parse(it).format(PRETTY) }.getOrDefault(it))
            }
        }

        is FormField.Choice -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                field.options.forEach { (valor, etiqueta) ->
                    OptionChip(etiqueta, seleccionado = values[field.key] == valor) {
                        onAnswer(field.key, valor)
                    }
                }
            }
        }

        is FormField.Reference -> {
            val fuente = when (field.source) {
                ReferenceSource.PEOPLE -> people
                ReferenceSource.PROJECTS -> projects
                ReferenceSource.INVENTORY -> inventory
            }
            ReferenceDrawer(
                field = field,
                source = fuente,
                selected = values[field.key],
                onPick = { onAnswer(field.key, it) },
                onQuickCreate = onQuickCreate,
            )
        }

        is FormField.Number -> {
            if (field.suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    field.suggestions.forEach { s ->
                        OptionChip(s, seleccionado = values[field.key] == s) { onAnswer(field.key, s) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            DrawerInput(
                value = values[field.key].orEmpty(),
                placeholder = field.example.ifBlank { field.label },
                numeric = true,
                onValueChange = { entrada ->
                    // Se filtra al teclear: un importe con letras solo se
                    // descubriría al fallar el guardado.
                    val permitido = if (field.decimal) "0123456789.," else "0123456789"
                    onPartial(field.key, entrada.filter { it in permitido })
                },
            )
        }

        is FormField.Text -> DrawerInput(
            value = values[field.key].orEmpty(),
            placeholder = field.example.ifBlank { field.label },
            multiline = field.multiline,
            onValueChange = { onPartial(field.key, it) },
        )

        // El archivo no llega aquí: su fila abre el selector directamente.
        is FormField.File -> Unit
    }
}

/**
 * El selector de una referencia.
 *
 * HASTA OCHO, FICHAS. A partir de ahí un campo que filtra según escribes: con
 * cuarenta artículos la tira de fichas era una fila horizontal por la que había
 * que arrastrar leyendo. El umbral es de la lista, no del tipo de campo — el
 * mismo selector se comporta distinto con seis artículos que con cuarenta,
 * porque el problema aparece con cuarenta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferenceDrawer(
    field: FormField.Reference,
    source: List<Pair<String, String>>,
    selected: String?,
    onPick: (String) -> Unit,
    onQuickCreate: (ReferenceSource, String, String?, (String?) -> Unit) -> Unit,
) {
    val c = VidaTheme.colors
    var filtro by remember(field.key) { mutableStateOf("") }
    var creando by remember(field.key) { mutableStateOf(source.isEmpty()) }
    var nombre by remember(field.key) { mutableStateOf("") }
    var categoria by remember(field.key) { mutableStateOf(INVENTORY_CATEGORIES.first().first) }

    if (source.isNotEmpty()) {
        if (source.size > 8) {
            DrawerInput(
                value = filtro,
                placeholder = "Busca entre tus ${source.size}…",
                onValueChange = { filtro = it },
            )
            Spacer(Modifier.height(8.dp))
        }
        val visibles = source.filter { filtro.isBlank() || it.second.contains(filtro, ignoreCase = true) }
        if (visibles.isEmpty()) {
            DrawerCaption("Nada coincide con «$filtro».")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Con muchas, se enseñan las primeras y el filtro hace el resto:
                // volcar cuarenta fichas dentro de la fila la haría más alta que
                // la pantalla.
                visibles.take(if (source.size > 8) 6 else visibles.size).forEach { (id, etiqueta) ->
                    OptionChip(etiqueta, seleccionado = selected == id) { onPick(id) }
                }
            }
            if (source.size > 8 && visibles.size > 6) {
                DrawerCaption("y ${visibles.size - 6} más — escribe para acotar")
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // El atajo de crear lo que falta sin salir de la hoja. Se conserva tal cual
    // estaba; solo cambia de sitio, a dentro de la fila del vínculo.
    if (!creando) {
        OptionChip(
            if (field.source == ReferenceSource.INVENTORY) "¿No está? Añádelo" else "Añadir a alguien",
            escape = true,
        ) { creando = true }
        return
    }

    if (source.isEmpty()) {
        DrawerCaption(
            when (field.source) {
                ReferenceSource.PEOPLE -> "Un seguimiento es con alguien. Añade a esa persona aquí mismo."
                ReferenceSource.PROJECTS -> "Todavía no tienes proyectos."
                ReferenceSource.INVENTORY -> "Una garantía cubre un artículo. Añádelo aquí mismo."
            },
        )
        Spacer(Modifier.height(8.dp))
    }
    DrawerInput(
        value = nombre,
        placeholder = if (field.source == ReferenceSource.INVENTORY) "Nombre del artículo" else "Nombre de la persona",
        onValueChange = { nombre = it },
    )
    if (field.source == ReferenceSource.INVENTORY) {
        Spacer(Modifier.height(8.dp))
        // La categoría se PREGUNTA en vez de suponerse: elegir una por defecto
        // sería inventarse el dato, y además es la que agrupa el inventario.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            INVENTORY_CATEGORIES.forEach { (valor, etiqueta) ->
                OptionChip(etiqueta, seleccionado = categoria == valor) { categoria = valor }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (source.isNotEmpty()) {
            OptionChip("Cancelar", escape = true) { creando = false; nombre = "" }
        }
        OptionChip("Añadir", seleccionado = nombre.isNotBlank()) {
            if (nombre.isNotBlank()) {
                onQuickCreate(
                    field.source,
                    nombre.trim(),
                    if (field.source == ReferenceSource.INVENTORY) categoria else null,
                ) { nuevoId -> if (nuevoId != null) onPick(nuevoId) }
                nombre = ""
                creando = false
            }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   PIEZAS PEQUEÑAS
   ══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun DrawerLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.1.sp),
        color = VidaTheme.colors.textTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 5.dp),
    )
}

@Composable
private fun DrawerCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp),
        color = VidaTheme.colors.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** Una ficha. Se ajusta a su texto y, si no cabe, el `FlowRow` la baja de línea. */
@Composable
private fun OptionChip(
    text: String,
    seleccionado: Boolean = false,
    escape: Boolean = false,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .padding(bottom = 6.dp)
            .background(if (seleccionado) c.primary else Color.Transparent, shape)
            .border(1.dp, if (seleccionado) c.primary else c.border, shape)
            .vidaClickable(onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 15.sp),
            color = when {
                seleccionado -> c.onPrimary
                escape -> c.textSecondary
                else -> c.text
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** La caja de texto de dentro de una fila. Más baja que la principal a propósito. */
@Composable
private fun DrawerInput(
    value: String,
    placeholder: String,
    multiline: Boolean = false,
    numeric: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(11.dp)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(180)
        runCatching { focus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(c.sunken, shape)
            .heightIn(min = if (multiline) 62.dp else 42.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
                color = c.textTertiary,
                maxLines = if (multiline) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = !multiline,
            maxLines = if (multiline) 3 else 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) {
                    androidx.compose.ui.text.input.KeyboardType.Number
                } else {
                    androidx.compose.ui.text.input.KeyboardType.Text
                },
                capitalization = if (numeric) {
                    androidx.compose.ui.text.input.KeyboardCapitalization.None
                } else {
                    androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                },
            ),
            textStyle = LocalTextStyle.current
                .merge(MaterialTheme.typography.bodyMedium)
                .copy(color = c.text, fontSize = 14.sp, lineHeight = 18.sp),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   TRADUCCIONES — de lo que declara el contrato a lo que se ve
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Qué dice la segunda línea de la fila.
 *
 * LA RESPUESTA cuando la hay, y si no, si el campo hace falta o no. Es lo que
 * permite comprobar lo contestado sin reabrir nada, y por eso la fecha se
 * escribe entera —«19 de octubre de 2026»— en vez de repetir el atajo que se
 * pulsó: el atajo dice cuánto adelanta, la fila dice qué día es.
 */
@Composable
private fun resumen(
    field: FormField,
    values: Map<String, String>,
    file: FilePayload?,
    readingFile: Boolean,
    people: List<Pair<String, String>>,
    projects: List<Pair<String, String>>,
    inventory: List<Pair<String, String>>,
): String {
    val raw = values[field.key].orEmpty()
    val vacio = if (field.required) "Obligatorio" else "Opcional"
    return when (field) {
        is FormField.File -> when {
            readingFile -> "Leyendo…"
            file != null -> file.fileName
            else -> vacio
        }
        is FormField.DateTime -> {
            val dia = raw.substringBefore('T').takeIf { it.isNotBlank() } ?: return vacio
            val hora = raw.substringAfter('T', "")
            val fecha = runCatching { LocalDate.parse(dia).format(PRETTY_SHORT) }.getOrDefault(dia)
            if (hora.isBlank()) "$fecha · falta la hora" else "$fecha, $hora"
        }
        is FormField.Date -> raw.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it).format(PRETTY) }.getOrDefault(it) }
            ?: vacio
        is FormField.Choice -> field.options.firstOrNull { it.first == raw }?.second ?: vacio
        is FormField.Reference -> {
            val fuente = when (field.source) {
                ReferenceSource.PEOPLE -> people
                ReferenceSource.PROJECTS -> projects
                ReferenceSource.INVENTORY -> inventory
            }
            fuente.firstOrNull { it.first == raw }?.second ?: vacio
        }
        else -> raw.takeIf { it.isNotBlank() } ?: vacio
    }
}

/** El día al que lleva cada atajo, contado desde hoy. */
private fun DateShortcut.dateFrom(hoy: LocalDate): LocalDate = when (this) {
    DateShortcut.HOY -> hoy
    DateShortcut.MANANA -> hoy.plusDays(1)
    DateShortcut.SEMANA -> hoy.plusWeeks(1)
    DateShortcut.MES -> hoy.plusMonths(1)
    DateShortcut.TRES_MESES -> hoy.plusMonths(3)
    DateShortcut.SEIS_MESES -> hoy.plusMonths(6)
    DateShortcut.ANIO -> hoy.plusYears(1)
    DateShortcut.DOS_ANIOS -> hoy.plusYears(2)
}

/** Lo que el atajo dice que hace. Y lo hace: no hay letra pequeña. */
private val DateShortcut.label: String
    get() = when (this) {
        DateShortcut.HOY -> "Hoy"
        DateShortcut.MANANA -> "Mañana"
        DateShortcut.SEMANA -> "En 1 semana"
        DateShortcut.MES -> "En 1 mes"
        DateShortcut.TRES_MESES -> "En 3 meses"
        DateShortcut.SEIS_MESES -> "En 6 meses"
        DateShortcut.ANIO -> "En 1 año"
        DateShortcut.DOS_ANIOS -> "En 2 años"
    }

private fun iconFor(icon: FieldIcon): ImageVector = when (icon) {
    FieldIcon.PEN -> Icons.Outlined.Edit
    FieldIcon.CLOCK -> Icons.Outlined.Schedule
    FieldIcon.CALENDAR -> Icons.Outlined.CalendarMonth
    FieldIcon.PIN -> Icons.Outlined.Place
    FieldIcon.DOC -> Icons.Outlined.Description
    FieldIcon.COIN -> Icons.Outlined.Payments
    FieldIcon.BOX -> Icons.Outlined.Inventory2
    FieldIcon.PERSON -> Icons.Outlined.Person
    FieldIcon.REPEAT -> Icons.Outlined.Repeat
    FieldIcon.LINK -> Icons.Outlined.Link
    FieldIcon.CLIP -> Icons.Outlined.AttachFile
    FieldIcon.FLAG -> Icons.Outlined.Flag
    FieldIcon.TAG -> Icons.Outlined.LocalOffer
    FieldIcon.HASH -> Icons.Outlined.Numbers
    FieldIcon.FOLDER -> Icons.Outlined.FolderOpen
}

/**
 * Fondo y tinta de cada tono.
 *
 * Los cinco salen de tokens que los nueve temas ya definen (ADR-023): no hay ni
 * un color nuevo, así que la hoja se ve correcta en Papel, en Neo y en Lumen
 * sin tocar nada. `error` no se usa para ningún tono — el rojo significa que
 * algo va mal, y «Comprobante» no va mal.
 */
@Composable
private fun toneColors(tone: FieldTone): Pair<Color, Color> {
    val c = VidaTheme.colors
    return when (tone) {
        FieldTone.TIME -> c.warningContainer to c.warningText
        FieldTone.PLACE -> c.secondContainer to c.second
        FieldTone.TEXT -> c.successContainer to c.successText
        FieldTone.DATA -> c.infoContainer to c.infoText
        FieldTone.FILE -> c.primaryContainer to c.primaryDeep
    }
}

/** Las mismas de `CreatableResource.INVENTORY`: una sola lista, un solo sitio. */
private val INVENTORY_CATEGORIES: List<Pair<String, String>> =
    (CreatableResource.INVENTORY.fields.first { it.key == "category" } as FormField.Choice).options
