package com.vidacotidiana.app.core.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.calendar.AlertEngine
import com.vidacotidiana.app.core.calendar.AlertSourceRecord
import com.vidacotidiana.app.core.calendar.DateAlert
import com.vidacotidiana.app.core.calendar.DayContent
import com.vidacotidiana.app.core.calendar.DayTask
import com.vidacotidiana.app.core.data.DataSlice
import com.vidacotidiana.app.core.data.DayNote
import com.vidacotidiana.app.core.data.DayNotesRepository
import com.vidacotidiana.app.core.data.FilePayload
import com.vidacotidiana.app.core.data.AttachTo
import com.vidacotidiana.app.core.data.Attachment
import com.vidacotidiana.app.core.data.Mood
import com.vidacotidiana.app.core.network.CurrentUser
import com.vidacotidiana.app.core.network.UserApi
import com.vidacotidiana.app.core.data.TaskStep
import com.vidacotidiana.app.core.data.WellbeingRepository
import com.vidacotidiana.app.core.data.FileReader
import com.vidacotidiana.app.core.data.UserSearchResult
import com.vidacotidiana.app.core.data.VidaData
import com.vidacotidiana.app.core.data.VidaRepository
import com.vidacotidiana.app.core.network.CompleteReminderRequest
import com.vidacotidiana.app.core.network.CreateReminderRequest
import com.vidacotidiana.app.core.network.Reminder
import com.vidacotidiana.app.core.network.ReminderApi
import com.vidacotidiana.app.core.notifications.ReminderAlarmScheduler
import com.vidacotidiana.app.core.prefs.AppPreferences
import com.vidacotidiana.app.core.ui.VisualTheme
import com.vidacotidiana.app.core.vocabulary.ProfessionalProfile
import com.vidacotidiana.app.feature.auth.AuthManager
import com.vidacotidiana.app.navigation.AppContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Estado transversal de la aplicación: contexto activo, tema, perfil, fecha
 * elegida del calendario, la vista de densidad y TODOS los datos del usuario.
 *
 * Vive en un solo `ViewModel` porque son decisiones que atraviesan pantallas —
 * cambiar de contexto reconfigura la barra inferior y vuelve a pedir los datos
 * de ese contexto— y tenerlas repartidas obligaría a sincronizarlas a mano.
 * Sigue el mismo patrón `StateFlow` + `data class` que el resto del proyecto.
 *
 * Todo lo que se ve aquí viene de los endpoints REALES que la Web ya usa: los
 * recordatorios de `/reminders`, las notas del día de `/day-notes`, y los ocho
 * módulos de recurso a través de `VidaRepository`. No queda ningún dato de
 * maqueta.
 */
data class AppUiState(
    val context: AppContext = AppContext.PERSONAL,
    val theme: VisualTheme = VisualTheme.DEFAULT,
    val profile: ProfessionalProfile = ProfessionalProfile.DEFAULT,
    val laboralEnabled: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val calendarDensity: String = "Mes",
    val reminders: List<Reminder> = emptyList(),
    val data: VidaData = VidaData(),
    val loading: Boolean = true,
    /**
     * El fallo de la carga de TAREAS. Vive aparte de `data.failed` porque los
     * recordatorios no viajan dentro de `VidaData`: se piden por su cuenta.
     *
     * Antes esta era la ÚNICA petición cuyo fallo llegaba a la interfaz, y por
     * eso mandaba sobre todas las demás: si caía, las diecinueve secciones
     * decían haber fallado aunque hubieran cargado bien; y si respondía, el
     * fallo de las otras diecinueve no lo contaba nadie.
     */
    val remindersFailed: Boolean = false,
    /**
     * Algo falló en la última carga, sea lo que sea. Sirve para lo transversal
     * —Inicio, que mira las siete fuentes— no para que una sección concreta
     * decida: para eso está [sliceError], que sólo mira lo suyo.
     */
    val error: String? = null,
    /** Notas del día seleccionado, tal como las devuelve el servicio. */
    val notes: List<DayNote> = emptyList(),
    val notesLoading: Boolean = false,

    /* ── Las tres capacidades del artefacto (V34/V35/V36) ────────────────── */

    /**
     * El ánimo de HOY. `null` no es un error: es que el día aún no se ha
     * marcado, que es el estado normal al abrir por primera vez.
     */
    /** La cuenta de quien ha entrado. `/api/v1/me`, no un dato inventado. */
    val user: CurrentUser? = null,

    val moodToday: Mood? = null,
    /** La semana que pinta Bienestar. */
    val moodWeek: List<Mood> = emptyList(),
    val moodLoading: Boolean = false,
    val moodError: String? = null,

    /**
     * Los pasos de la tarea abierta. Se cargan al entrar en el detalle y no en
     * `loadAll`: pedirlos para las veinte tareas de una lista serían veinte
     * peticiones para pintar dos líneas.
     */
    val steps: List<TaskStep> = emptyList(),
    val stepsFor: String? = null,
    val stepsLoading: Boolean = false,
    val stepsError: String? = null,

    /** Los adjuntos del recurso abierto (V37). Mecánica única para los cinco tipos. */
    val attachments: List<Attachment> = emptyList(),
    val attachmentsFor: String? = null,
    val attachmentsLoading: Boolean = false,

    /** Lo hecho HOY de cada hábito, por id de rutina. */
    val habitToday: Map<String, Int> = emptyMap(),
    /** Búsqueda de usuarios de Familia (ADR-025 §1). */
    val userSearch: List<UserSearchResult> = emptyList(),
    val searching: Boolean = false,
    /** Un alta en curso: la hoja se bloquea para no duplicar el recurso. */
    val saving: Boolean = false,
    /**
     * Qué alta está pidiendo el usuario. Vive en el estado y no en cada
     * pantalla porque hay DOS caminos hasta el mismo formulario —el «+» de la
     * barra y el «+» de cada sección— y tenerlo en un sitio es lo que evita
     * dos formularios que se comporten distinto.
     */
    val pendingCreate: CreatableResource? = null,
    /**
     * Qué se está editando, si es una edición y no un alta. La hoja es la
     * MISMA: crear y editar comparten campos y validación porque comparten
     * dominio, y dos formularios distintos acabarían admitiendo cosas
     * distintas.
     */
    val editing: EditTarget? = null,
    /** Valores con los que abrir la hoja ya rellena. */
    val formInitialValues: Map<String, String> = emptyMap(),
)

/** El texto con el que se cuenta un fallo de carga. Uno solo, en un sitio. */
const val LOAD_FAILED = "No se pudo cargar"

/**
 * ¿Puede ESTA sección afirmar algo sobre sus datos?
 *
 * Devuelve el error si su porción falló, y `null` si cargó —aunque otras
 * porciones hayan fallado—. Es lo que permite que Garantías diga la verdad
 * sobre garantías mientras Pagos dice la verdad sobre pagos, en vez de que un
 * único error global las obligue a todas a la misma respuesta.
 */
fun AppUiState.sliceError(slice: DataSlice): String? =
    if (slice in data.failed) LOAD_FAILED else null

/** Lo mismo para las tareas, que se piden fuera de `VidaData`. */
val AppUiState.tasksError: String?
    get() = if (remindersFailed) LOAD_FAILED else null

/**
 * ¿Falló ALGO de la última carga?
 *
 * Lo usa lo transversal —Inicio, que mira las siete fuentes a la vez y no
 * puede responder por ninguna si le falta cualquiera—. Mira los datos, no
 * `error`: ese campo es una bolsa de «lo último que salió mal» que también
 * recoge fallos de guardado y de compartir, y un alta fallida no debe hacer
 * que Inicio afirme que no pudo cargar.
 */
val AppUiState.loadFailed: Boolean
    get() = remindersFailed || data.failed.isNotEmpty()

/** El recurso concreto que se está editando. */
data class EditTarget(val resource: CreatableResource, val id: String, val version: Int)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val reminderApi: ReminderApi,
    private val repository: VidaRepository,
    private val dayNotes: DayNotesRepository,
    private val wellbeing: WellbeingRepository,
    private val userApi: UserApi,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val authManager: AuthManager,
    private val fileReader: FileReader,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppUiState(
            theme = prefs.theme.value,
            profile = prefs.profile.value,
            laboralEnabled = prefs.laboralEnabled.value,
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    /** Las alertas derivadas, recalculadas cuando cambian los registros. */
    private var derivedAlerts: List<DateAlert> = emptyList()

    private var searchJob: Job? = null

    /**
     * La carga la manda la SESIÓN, no el momento en que se construye este
     * ViewModel.
     *
     * Este objeto nace con el grafo de navegación, es decir cuando el usuario
     * todavía está en la pantalla de login. Cargar aquí de forma incondicional
     * lanzaba catorce peticiones sin token, las catorce fallaban con 401, y al
     * volver del callback nadie las repetía: la aplicación se quedaba con ese
     * error de antes de existir la sesión.
     *
     * Observando la sesión, el primer `true` dispara la carga —da igual si
     * llega al arrancar con sesión guardada o tres pantallas después— y un
     * `false` limpia los datos, que es lo correcto al cerrar sesión: los datos
     * son de quien inició sesión, no de la aplicación.
     */
    init {
        viewModelScope.launch {
            authManager.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    refresh()
                    loadNotes(_state.value.selectedDate)
                } else {
                    _state.update { it.copy(data = VidaData(), notes = emptyList(), reminders = emptyList(), loading = false, error = null) }
                }
            }
        }
    }

    /**
     * ADR-019: el contexto activo se envía como filtro. Portal («GENERAL») no
     * filtra — es la vista transversal, y ahí el usuario espera verlo todo.
     */
    private fun contextParam(): String? = when (_state.value.context) {
        AppContext.PERSONAL -> "PERSONAL"
        AppContext.LABORAL -> "LABORAL"
        AppContext.PORTAL -> null
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val context = contextParam()

            // ADR-019: el mismo filtro que el resto de recursos. Sin él,
            // Laboral mostraba también las tareas de Personal y al revés;
            // Portal sigue recibiendo `null`, que es «sin filtrar», y por eso
            // es el único que las ve todas.
            val remindersResult = runCatching { reminderApi.listReminders(context) }
            val dataResult = repository.loadAll(context)

            val reminders = remindersResult.getOrNull()?.items
            val data = dataResult.getOrNull()

            // Las alertas se derivan ANTES de publicar el estado: si se
            // derivaran después, la recomposición que dispara el estado nuevo
            // leería todavía las alertas del ciclo anterior y el calendario
            // pintaría un mes de datos frescos con avisos viejos.
            if (data != null) deriveAlerts(data)

            _state.update {
                // El vacío afirma un hecho sobre los datos del usuario; si la
                // carga falló no sabemos nada de ellos (ADR-021 k), así que el
                // fallo se conserva y la pantalla lo dice.
                //
                // Y se conserva POR PORCIÓN. Antes bastaba con que fallaran
                // las tareas para que las diecinueve secciones restantes
                // dijeran haber fallado, y bastaba con que las tareas
                // respondieran para que el fallo de las otras diecinueve no lo
                // contara nadie. Ahora cada una responde de lo suyo.
                val slicesFailed = data?.failed ?: it.data.failed
                it.copy(
                    reminders = reminders ?: it.reminders,
                    data = data ?: it.data,
                    loading = false,
                    remindersFailed = remindersResult.isFailure,
                    error = when {
                        remindersResult.isFailure || dataResult.isFailure -> LOAD_FAILED
                        slicesFailed.isNotEmpty() -> LOAD_FAILED
                        else -> null
                    },
                )
            }
        }
    }

    /**
     * ADR-018 — las alertas se DERIVAN de los registros reales del usuario:
     * garantías a 30/15/0 días, mantenimientos a 7/3/0, pagos a 5/2/0 y el
     * corte de una tarjeta el mismo día. No hay una tabla de alertas.
     */
    private fun deriveAlerts(data: VidaData) {
        derivedAlerts = AlertEngine.derive(
            warranties = data.warranties.map {
                AlertSourceRecord(
                    id = it.id,
                    label = it.product,
                    date = it.expiresOn,
                    // Una garantía ya vencida dejó de tener aviso que dar.
                    closed = it.status == com.vidacotidiana.app.core.data.WarrantyStatus.VENCIDA,
                )
            },
            maintenance = data.maintenance.map {
                AlertSourceRecord(id = it.id, label = it.item, date = it.nextDueOn)
            },
            payments = data.payments.map {
                AlertSourceRecord(
                    id = it.id,
                    label = it.name,
                    date = it.renewsOn,
                    amount = it.amountLabel,
                    // Solo las tarjetas traen día de corte; el resto va nulo y
                    // el motor no genera ese aviso.
                    statementDay = it.statementDay,
                )
            },
        )
    }

    /* ---------------------------------------------------------------------
       Notas del día — servicio real (`/api/v1/day-notes`), igual que la Web
       --------------------------------------------------------------------- */

    private fun loadNotes(date: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(notesLoading = true) }
            dayNotes.list(date, contextParam())
                .onSuccess { notes -> _state.update { it.copy(notes = notes, notesLoading = false) } }
                .onFailure { e -> _state.update { it.copy(notesLoading = false, error = e.message) } }
        }
    }

    fun addNote(text: String) {
        if (text.isBlank()) return
        val date = _state.value.selectedDate
        viewModelScope.launch {
            dayNotes.create(date, text, _state.value.notes, contextParam())
                .onSuccess { created ->
                    // Solo se añade si seguimos en el mismo día: si el usuario
                    // cambió de fecha mientras se guardaba, la nota es de otro
                    // día y aparecería donde no le toca.
                    if (_state.value.selectedDate == date) {
                        _state.update { it.copy(notes = it.notes + created) }
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo guardar la nota") } }
        }
    }

    fun editNote(note: DayNote, text: String) {
        if (text.isBlank()) {
            deleteNote(note)
            return
        }
        viewModelScope.launch {
            dayNotes.editText(note, text)
                .onSuccess { updated ->
                    _state.update { s -> s.copy(notes = s.notes.map { if (it.id == updated.id) updated else it }) }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo editar la nota") } }
        }
    }

    fun deleteNote(note: DayNote) {
        viewModelScope.launch {
            dayNotes.delete(note)
                .onSuccess { _state.update { s -> s.copy(notes = s.notes.filterNot { it.id == note.id }) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo borrar la nota") } }
        }
    }

    /* ---------------------------------------------------------------------
       Altas
       --------------------------------------------------------------------- */

    /**
     * Da de alta un recurso con lo que el formulario recogió.
     *
     * El `when` es exhaustivo sobre `CreatableResource` a propósito: añadir un
     * tipo nuevo rompe la compilación aquí hasta que se declara su POST, en
     * vez de dejar en la interfaz un botón que no guarda nada — que es justo
     * el problema que esto viene a arreglar.
     */
    fun create(resource: CreatableResource, values: Map<String, String>, file: FilePayload?) {
        val context = contextParam() ?: "PERSONAL"
        fun text(key: String): String = values[key].orEmpty().trim()
        fun date(key: String): LocalDate? =
            values[key]?.takeIf { it.isNotBlank() }?.substringBefore('T')?.let { LocalDate.parse(it) }

        /**
         * Un momento con hora: se interpreta en la zona del usuario y se
         * convierte a UTC — lo mismo que hace `new Date('2026-09-20T14:30')`
         * en la Web, donde una cadena con hora sí se lee como local.
         *
         * Devuelve null si falta la hora. NUNCA rellena `00:00`: una hora
         * inventada es un dato falso que después aparece en el calendario.
         */
        fun instant(key: String): String? {
            val raw = values[key]?.takeIf { it.isNotBlank() } ?: return null
            val time = raw.substringAfter('T', "")
            if (time.isBlank()) return null
            return runCatching {
                java.time.LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toString()
            }.getOrNull()
        }

        val editing = _state.value.editing

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            // Editar usa el MISMO formulario y la misma validación que crear:
            // lo único que cambia es el verbo y que hay que mandar `version`.
            // El `when` es exhaustivo, así que un recurso nuevo no puede
            // quedarse con un «Guardar» que no guarde.
            if (editing != null) {
                val result: Result<Unit> = when (resource) {
                    CreatableResource.TASK -> runCatching {
                        // El icono y la pegatina se REENVÍAN tal como están.
                        //
                        // No es adorno: `Reminder#applyEdit` los aplica siempre
                        // como llegan, así que omitirlos los BORRA. Editar la
                        // fecha de una tarea desde el móvil estaba borrando el
                        // icono y la pegatina que se le habían puesto en la Web.
                        // Se corrige con el contrato que ya existe —el mismo que
                        // cumple la Web—, sin tocar backend.
                        val current = _state.value.reminders.firstOrNull { it.id == editing.id }
                        val updated = reminderApi.updateReminder(
                            editing.id,
                            com.vidacotidiana.app.core.network.UpdateReminderRequest(
                                title = text("title"),
                                description = values["description"]?.ifBlank { null },
                                dueAt = instant("dueAt"),
                                // Nulo = "no tocar": dejar el campo vacío NO
                                // borra una ubicación guardada. Limitación
                                // declarada del backend, no de esta pantalla.
                                location = values["location"]?.ifBlank { null },
                                iconId = current?.iconId,
                                stickerId = current?.stickerId,
                                version = editing.version,
                            ),
                        )
                        // La alarma sigue a la fecha nueva: si se movió la hora
                        // y el aviso se quedara en la vieja, sonaría a destiempo.
                        alarmScheduler.cancel(updated.id)
                        updated.dueAt
                            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                            ?.let { millis -> runCatching { alarmScheduler.schedule(updated.id, updated.title, millis) } }
                        Unit
                    }

                    CreatableResource.WARRANTY -> repository.editWarranty(
                        editing.id, text("item"), date("expiresAt") ?: LocalDate.now(), editing.version,
                    )

                    CreatableResource.MAINTENANCE -> repository.editMaintenance(
                        editing.id, text("item"), date("nextDueAt") ?: LocalDate.now(),
                        values["intervalMonths"]?.toIntOrNull(), editing.version,
                        values["inventoryItemId"],
                    )

                    CreatableResource.PAYMENT -> repository.editPayment(
                        editing.id, text("service"), date("nextPaymentDate") ?: LocalDate.now(),
                        text("billingCycle").ifBlank { "MONTHLY" },
                        values["amount"]?.replace(',', '.')?.toDoubleOrNull(), editing.version,
                    )

                    CreatableResource.INVENTORY -> repository.editInventoryItem(
                        editing.id, text("name"), text("category").ifBlank { "HOGAR" },
                        values["location"], editing.version,
                    )

                    // El ARCHIVO no se reemplaza al editar: el backend solo
                    // acepta nombre y categoría en su PATCH, igual que la Web.
                    CreatableResource.DOCUMENT -> repository.editDocument(
                        editing.id, text("name"), text("category").ifBlank { "OTROS" }, editing.version,
                    )

                    CreatableResource.PERSON -> repository.editPerson(
                        editing.id, text("name"), values["role"], values["organization"], editing.version,
                    )

                    CreatableResource.PROJECT -> repository.editProject(
                        editing.id, text("name"), values["status"], date("deadline"), editing.version,
                    )

                    CreatableResource.COMMITMENT -> repository.editCommitment(
                        editing.id, text("description"), text("direction").ifBlank { "MINE" },
                        date("dueAt") ?: LocalDate.now(), values["personId"], editing.version,
                    )

                    CreatableResource.NOTE -> repository.editNote(
                        editing.id, text("title"), values["description"], editing.version,
                    )

                    // `completed` NO viaja aquí: cambiar el progreso no cumple
                    // ni reabre nada (AC-018). El estado se cambia con su propia
                    // acción, nunca como efecto de editar.
                    CreatableResource.OBJECTIVE -> repository.editObjective(
                        editing.id,
                        text("title"),
                        values["targetValue"]?.toIntOrNull(),
                        values["currentValue"]?.toIntOrNull(),
                        date("deadline"),
                        editing.version,
                    )

                    // `active` NO viaja aquí: pausar es su propia acción. Y
                    // cambiar la frecuencia NO recalcula la fecha — el backend
                    // tampoco lo hace, así que no se inventa esa regla.
                    CreatableResource.ROUTINE -> repository.editRoutine(
                        editing.id,
                        text("title"),
                        values["description"],
                        text("frequency").ifBlank { "WEEKLY" },
                        date("nextExecutionDate") ?: LocalDate.now(),
                        editing.version,
                    )

                    CreatableResource.WORK_RESOURCE -> repository.editWorkResource(
                        editing.id,
                        text("name"),
                        text("type").ifBlank { "OTRO" },
                        values["reference"],
                        values["description"],
                        values["personId"],
                        values["projectId"],
                        editing.version,
                    )

                    CreatableResource.PLACE -> repository.editPlace(
                        editing.id,
                        text("name"),
                        values["address"],
                        values["personId"],
                        editing.version,
                    )
                }

                result
                    .onSuccess {
                        _state.update { it.copy(saving = false, pendingCreate = null, editing = null, formInitialValues = emptyMap()) }
                        refresh()
                    }
                    .onFailure { e ->
                        _state.update { it.copy(saving = false, error = e.message ?: "No se pudo guardar") }
                    }
                return@launch
            }

            val result: Result<Unit> = when (resource) {
                CreatableResource.TASK -> runCatching {
                    val created = reminderApi.createReminder(
                        CreateReminderRequest(
                            title = text("title"),
                            // Sin hora elegida NO viaja fecha: la Web permite
                            // una tarea sin fecha, y es preferible a colocarla
                            // en el calendario a una hora que nadie eligió.
                            dueAt = instant("dueAt"),
                            description = values["description"]?.ifBlank { null },
                            // ADR-019: la tarea nace en el módulo desde el que
                            // se crea. Omitirlo la mandaba siempre a Personal.
                            context = context,
                            // Texto tal cual, sin validar ni transformar.
                            location = values["location"]?.ifBlank { null },
                        ),
                    )
                    // Una tarea con fecha merece su aviso local: es la misma
                    // alarma exacta que ya programa el resto de la aplicación.
                    created.dueAt
                        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                        ?.let { millis -> runCatching { alarmScheduler.schedule(created.id, created.title, millis) } }
                    Unit
                }

                CreatableResource.PAYMENT -> repository.createPayment(
                    service = text("service"),
                    nextPayment = date("nextPaymentDate") ?: LocalDate.now(),
                    billingCycle = text("billingCycle").ifBlank { "MONTHLY" },
                    amount = values["amount"]?.replace(',', '.')?.toDoubleOrNull(),
                    currency = null,
                    context = context,
                )

                CreatableResource.MAINTENANCE -> repository.createMaintenance(
                    item = text("item"),
                    nextDue = date("nextDueAt") ?: LocalDate.now(),
                    intervalMonths = values["intervalMonths"]?.toIntOrNull(),
                    context = context,
                    inventoryItemId = values["inventoryItemId"],
                )

                CreatableResource.WARRANTY -> {
                    val itemId = values["inventoryItemId"].orEmpty()
                    when {
                        file == null ->
                            Result.failure(IllegalStateException("Una garantía necesita su comprobante."))
                        // El backend lo rechaza igual; decirlo aquí evita un
                        // viaje de red para recibir el mismo "no" en inglés.
                        itemId.isBlank() ->
                            Result.failure(IllegalStateException("Elige el artículo del inventario que cubre esta garantía."))
                        else -> repository.createWarranty(
                            text("item"), date("expiresAt") ?: LocalDate.now(), file, itemId, context,
                        )
                    }
                }

                CreatableResource.INVENTORY -> repository.createInventoryItem(
                    name = text("name"),
                    category = text("category").ifBlank { "HOGAR" },
                    location = values["location"],
                    context = context,
                ).map { }

                CreatableResource.DOCUMENT -> {
                    if (file == null) {
                        Result.failure(IllegalStateException("Elige el archivo que quieres subir."))
                    } else {
                        repository.uploadDocument(text("name"), text("category").ifBlank { "OTROS" }, file, context)
                    }
                }

                CreatableResource.PERSON -> repository.createPerson(
                    text("name"), values["role"], values["organization"],
                ).map { }

                CreatableResource.PROJECT -> repository.createProject(
                    text("name"), values["status"], date("deadline"),
                )

                CreatableResource.COMMITMENT -> repository.createCommitment(
                    personId = text("personId"),
                    description = text("description"),
                    direction = text("direction").ifBlank { "MINE" },
                    dueAt = date("dueAt") ?: LocalDate.now(),
                )

                CreatableResource.NOTE -> repository.createNote(text("title"), values["description"])

                // Nace siempre en curso: `CreateObjectiveRequest` ni siquiera
                // acepta `completed`.
                CreatableResource.OBJECTIVE -> repository.createObjective(
                    title = text("title"),
                    targetValue = values["targetValue"]?.toIntOrNull(),
                    currentValue = values["currentValue"]?.toIntOrNull(),
                    deadline = date("deadline"),
                )

                // Nace siempre activa: `CreateRoutineRequest` no acepta
                // `active`, y el backend la crea con `active = true`.
                CreatableResource.ROUTINE -> repository.createRoutine(
                    title = text("title"),
                    description = values["description"],
                    frequency = text("frequency").ifBlank { "WEEKLY" },
                    nextExecutionDate = date("nextExecutionDate") ?: LocalDate.now(),
                )

                // Persona y proyecto viajan solo si el usuario los eligió. Que
                // no haya ninguno es un alta válida, no un formulario a medias.
                CreatableResource.WORK_RESOURCE -> repository.createWorkResource(
                    name = text("name"),
                    type = text("type").ifBlank { "OTRO" },
                    reference = values["reference"],
                    description = values["description"],
                    personId = values["personId"],
                    projectId = values["projectId"],
                )

                CreatableResource.PLACE -> repository.createPlace(
                    name = text("name"),
                    address = values["address"],
                    personId = values["personId"],
                )
            }

            result
                .onSuccess {
                    // La hoja se cierra sola al guardar: dejarla abierta invita
                    // a pulsar otra vez y duplicar el recurso. Que la lista
                    // aparezca actualizada detrás ya es la confirmación; un
                    // aviso encima diría lo mismo tapándolo.
                    _state.update { it.copy(saving = false, pendingCreate = null, formInitialValues = emptyMap()) }
                    // Se recarga en vez de insertar a mano: el servidor puede
                    // haber derivado cosas (avisos, estado) que el cliente no
                    // sabe calcular, y adivinarlas dejaría la pantalla mintiendo.
                    refresh()
                }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = e.message ?: "No se pudo guardar") }
                }
        }
    }

    fun requestCreate(resource: CreatableResource) {
        _state.update {
            it.copy(pendingCreate = resource, editing = null, formInitialValues = emptyMap(), error = null)
        }
    }

    /**
     * Abrir una tarea existente para editarla, con sus valores reales.
     *
     * La fecha se devuelve al formulario en `YYYY-MM-DDTHH:mm` local, que es
     * el formato con el que trabaja el campo de fecha y hora: así el usuario
     * ve la hora que puso, no una conversión.
     */
    fun requestEditTask(taskId: String) {
        val reminder = _state.value.reminders.firstOrNull { it.id == taskId } ?: return
        val local = reminder.dueAt?.let {
            runCatching {
                Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString()
            }.getOrNull()
        }
        _state.update {
            it.copy(
                pendingCreate = CreatableResource.TASK,
                editing = EditTarget(CreatableResource.TASK, reminder.id, reminder.version),
                formInitialValues = buildMap {
                    put("title", reminder.title)
                    local?.let { value -> put("dueAt", value) }
                    reminder.location?.let { l -> put("location", l) }
                    reminder.description?.let { d -> put("description", d) }
                },
                error = null,
            )
        }
    }

    /**
     * Abrir cualquier recurso para editarlo, con sus valores REALES.
     *
     * Es genérico a propósito: los `PATCH` del backend aceptan exactamente los
     * mismos campos que sus `POST` más `version`, así que la especificación de
     * `CreatableResource` sirve igual para crear y para editar. Un formulario
     * de edición por recurso habría acabado admitiendo cosas distintas de las
     * que admite su alta, que es justo el fallo que esto evita.
     */
    fun requestEdit(resource: CreatableResource, id: String) {
        val data = _state.value.data
        val values: Map<String, String>
        val version: Int

        when (resource) {
            CreatableResource.TASK -> {
                requestEditTask(id)
                return
            }
            CreatableResource.WARRANTY -> {
                val w = data.warranties.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("item", w.product)
                    put("expiresAt", w.expiresOn.toString())
                    // Las garantías anteriores a la obligación pueden no tener
                    // artículo. Se abren igual: la regla es del alta, no del
                    // ciclo de vida, y `editWarranty` no lo manda.
                    w.inventoryItemId?.let { put("inventoryItemId", it) }
                }
                version = w.version
            }
            CreatableResource.MAINTENANCE -> {
                val m = data.maintenance.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("item", m.item)
                    put("nextDueAt", m.nextDueOn.toString())
                    m.intervalMonths?.let { put("intervalMonths", it.toString()) }
                    m.inventoryItemId?.let { put("inventoryItemId", it) }
                }
                version = m.version
            }
            CreatableResource.PAYMENT -> {
                val p = data.payments.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("service", p.name)
                    put("nextPaymentDate", p.renewsOn.toString())
                    p.billingCycle?.let { put("billingCycle", it) }
                    // `349.0` era el `Double` volcado tal cual, mientras el
                    // resto de Pagos dice «$349 MXN». El tipo no cambia —el
                    // formulario sigue enviando un número— pero lo que se lee
                    // es la misma cifra que se leía antes de abrirlo.
                    p.amount?.let { amount ->
                        put("amount", if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString())
                    }
                }
                version = p.version
            }
            CreatableResource.INVENTORY -> {
                val i = data.inventory.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("name", i.name)
                    put("category", i.category)
                    i.location?.let { put("location", it) }
                }
                version = i.version
            }
            CreatableResource.DOCUMENT -> {
                val d = data.documents.firstOrNull { it.id == id } ?: return
                values = mapOf("name" to d.name, "category" to d.category)
                version = d.version
            }
            CreatableResource.PERSON -> {
                val p = data.people.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("name", p.name)
                    p.role?.let { put("role", it) }
                    p.organization?.let { put("organization", it) }
                }
                version = p.version
            }
            CreatableResource.PROJECT -> {
                val p = data.projects.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("name", p.name)
                    // Sin estado no se pone la clave, igual que con la entrega:
                    // el campo aparece vacío en vez de con una cadena inventada.
                    p.status?.let { put("status", it) }
                    p.deadline?.let { put("deadline", it.toString()) }
                }
                version = p.version
            }
            CreatableResource.COMMITMENT -> {
                val cmt = data.commitments.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("description", cmt.description)
                    put("direction", cmt.direction)
                    cmt.dueOn?.let { put("dueAt", it.toString()) }
                    cmt.personId?.let { put("personId", it) }
                }
                version = cmt.version
            }
            CreatableResource.NOTE -> {
                val n = data.inbox.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("title", n.title)
                    n.description?.let { put("description", it) }
                }
                version = n.version
            }
            CreatableResource.OBJECTIVE -> {
                val o = data.objectives.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("title", o.title)
                    o.targetValue?.let { put("targetValue", it.toString()) }
                    // El progreso SÍ se precarga aunque sea 0: es el valor real
                    // del objetivo y es el campo desde el que se actualiza.
                    put("currentValue", o.currentValue.toString())
                    o.deadline?.let { put("deadline", it.toString()) }
                }
                version = o.version
            }
            CreatableResource.ROUTINE -> {
                val r = data.routines.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("title", r.title)
                    put("frequency", r.frequency)
                    put("nextExecutionDate", r.nextExecutionDate.toString())
                    r.description?.let { put("description", it) }
                }
                version = r.version
            }
            CreatableResource.WORK_RESOURCE -> {
                val w = data.workResources.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("name", w.name)
                    put("type", w.type)
                    w.reference?.let { put("reference", it) }
                    w.description?.let { put("description", it) }
                    w.personId?.let { put("personId", it) }
                    w.projectId?.let { put("projectId", it) }
                }
                version = w.version
            }
            CreatableResource.PLACE -> {
                val p = data.places.firstOrNull { it.id == id } ?: return
                values = buildMap {
                    put("name", p.name)
                    p.address?.let { put("address", it) }
                    p.personId?.let { put("personId", it) }
                }
                version = p.version
            }
        }

        _state.update {
            it.copy(
                pendingCreate = resource,
                editing = EditTarget(resource, id, version),
                formInitialValues = values,
                error = null,
            )
        }
    }

    /**
     * Completar / resolver / registrar pago. Cada recurso usa el verbo que su
     * propio backend define; no hay un «completar» genérico inventado.
     */
    fun completeResource(resource: CreatableResource, id: String) {
        val data = _state.value.data
        viewModelScope.launch {
            val result = when (resource) {
                CreatableResource.TASK -> { toggleTask(id); return@launch }
                CreatableResource.WARRANTY ->
                    data.warranties.firstOrNull { it.id == id }?.let { repository.completeWarranty(id, it.version) }
                CreatableResource.MAINTENANCE ->
                    data.maintenance.firstOrNull { it.id == id }?.let { repository.completeMaintenance(id, it.version) }
                CreatableResource.PAYMENT ->
                    data.payments.firstOrNull { it.id == id }?.let { repository.registerPayment(id, it.version) }
                CreatableResource.COMMITMENT ->
                    data.commitments.firstOrNull { it.id == id }?.let { repository.resolveCommitment(id, it.version) }
                // Objetivos es el ÚNICO que cambia de estado con un PATCH: su
                // backend no expone `/complete`. Y es el único que ALTERNA —
                // cumplir y reabrir son la misma llamada con distinto valor—,
                // por eso lee `completed` en vez de mandar `true` fijo.
                CreatableResource.OBJECTIVE ->
                    data.objectives.firstOrNull { it.id == id }
                        ?.let { repository.setObjectiveCompleted(id, !it.completed, it.version) }
                // «Hecha» de una rutina NO es "completar": registra UNA
                // ocurrencia y avanza la fecha. Endpoint propio, no un PATCH, y
                // deliberadamente NADA parecido a la alternancia de Objetivos —
                // una rutina no se cumple, siempre vuelve.
                CreatableResource.ROUTINE ->
                    data.routines.firstOrNull { it.id == id }
                        ?.let { repository.executeRoutine(id, it.version) }
                // Inventario, documentos, personas, proyectos, notas del Inbox,
                // RECURSOS DE TRABAJO y LUGARES no tienen estado de completado
                // en el backend: no se inventa.
                //
                // Que WORK_RESOURCE y PLACE caigan aquí es DELIBERADO, no un
                // olvido: sus backends no exponen ninguna acción —ni complete,
                // ni execute, ni resolve—, solo CRUD. Sus tarjetas pasan
                // `onComplete = null` y por eso no muestran ningún botón que no
                // llevaría a nada.
                else -> null
            } ?: return@launch

            result
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo actualizar") } }
        }
    }

    /**
     * Abrir el documento con la aplicación del sistema que sepa leerlo.
     *
     * Descarga los bytes de `GET /documents/{id}/content`, los escribe en la
     * caché y los entrega por `FileProvider`. NO se construye un visor propio:
     * un lector de PDF dentro de Cotidiana sería la funcionalidad paralela que
     * el criterio de integración descarta, y ningún visor propio va a superar
     * al que el usuario ya eligió.
     */
    fun openDocument(context: android.content.Context, id: String, name: String) {
        withDocumentFile(context, id, name) { uri, mime ->
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(intent) }
                .onFailure { _state.update { s -> s.copy(error = "No hay ninguna aplicación que pueda abrirlo.") } }
        }
    }

    /**
     * Varios documentos en un zip, entregado al selector del sistema.
     *
     * SE COMPARTE, NO SE "DESCARGA A DESCARGAS". Guardar en el almacenamiento
     * público exigiría permisos que Cotidiana no pide, y el selector ya deja al
     * usuario mandarlo a Archivos, Drive o donde quiera — que es coexistir con
     * el teléfono en vez de duplicar su gestor de descargas.
     *
     * `ids` vacío significa TODOS los del módulo activo: es el contrato del
     * backend, no un descuido.
     */
    fun downloadDocumentsZip(context: android.content.Context, ids: Set<String>) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            repository.documentsZip(ids, contextParam())
                .onSuccess { bytes ->
                    val prepared = runCatching {
                        val dir = java.io.File(context.cacheDir, "documentos").apply { mkdirs() }
                        val file = java.io.File(dir, "documentos.zip")
                        file.writeBytes(bytes)
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".files",
                            file,
                        )
                    }
                    _state.update { it.copy(saving = false) }
                    prepared
                        .onSuccess { uri ->
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(android.content.Intent.createChooser(send, "documentos.zip"))
                            }.onFailure {
                                _state.update { s -> s.copy(error = "No se pudo compartir el archivo.") }
                            }
                        }
                        .onFailure { e ->
                            _state.update { it.copy(error = e.message ?: "No se pudo preparar el zip") }
                        }
                }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = e.message ?: "No se pudo descargar") }
                }
        }
    }

    /** Entregar el archivo al selector de Android. */
    fun shareDocumentFile(context: android.content.Context, id: String, name: String) {
        withDocumentFile(context, id, name) { uri, mime ->
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mime
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(android.content.Intent.createChooser(send, name)) }
                .onFailure { _state.update { s -> s.copy(error = "No se pudo compartir el archivo.") } }
        }
    }

    private fun withDocumentFile(
        context: android.content.Context,
        id: String,
        name: String,
        onReady: (android.net.Uri, String) -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            repository.documentBytes(id)
                .onSuccess { bytes ->
                    val result = runCatching {
                        val dir = java.io.File(context.cacheDir, "documentos").apply { mkdirs() }
                        val file = java.io.File(dir, name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                        file.writeBytes(bytes)
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".files",
                            file,
                        )
                    }
                    _state.update { it.copy(saving = false) }
                    result
                        .onSuccess { uri ->
                            val mime = _state.value.data.documents.firstOrNull { d -> d.id == id }
                                ?.let { guessMime(it.name) } ?: "*/*"
                            onReady(uri, mime)
                        }
                        .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo preparar el archivo") } }
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "No se pudo descargar") } }
        }
    }

    /**
     * El tipo se deduce de la extensión porque el listado no trae
     * `contentType` al dominio. Son los cinco que el backend admite; cualquier
     * otro cae a genérico y deja que el sistema decida.
     */
    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "*/*"
    }

    /** Visible para toda la familia, o solo para mí (ADR-025). */
    fun setDocumentFamilyVisible(id: String, visible: Boolean) {
        val doc = _state.value.data.documents.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.setDocumentFamilyVisible(id, visible, doc.version)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo cambiar la visibilidad") } }
        }
    }

    fun deleteResource(resource: CreatableResource, id: String) {
        viewModelScope.launch {
            repository.delete(resource, id)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo eliminar") } }
        }
    }

    fun cancelCreate() {
        _state.update {
            it.copy(pendingCreate = null, editing = null, formInitialValues = emptyMap(), error = null)
        }
    }

    /**
     * «Cómo llegar»: abre la aplicación de mapas que el usuario ya tenga.
     *
     * COEXISTIR, NO REEMPLAZAR: se delega en el sistema con un `geo:` y no se
     * construye mapa propio ni se añade SDK. No hace falta ningún permiso —no
     * se pide la ubicación del usuario, solo se declara el destino— y no se
     * elige aplicación: resuelve Android con lo que haya instalado.
     *
     * `location` es TEXTO LIBRE, así que el destino se pasa como consulta
     * (`?q=`) sobre coordenadas nulas. Nunca se geocodifica aquí ni se inventan
     * unas coordenadas que el dato no tiene.
     *
     * El texto se codifica con `Uri.encode` en vez de concatenarse: una coma o
     * un espacio en «Av. Reforma 123, CDMX» romperían la URI a mano.
     */
    fun openDirections(context: android.content.Context, location: String) {
        val query = android.net.Uri.encode(location.trim())
        if (query.isEmpty()) return
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("geo:0,0?q=$query"),
        )
        runCatching { context.startActivity(intent) }
            .onFailure {
                _state.update { it.copy(error = "No hay ninguna aplicación de mapas instalada.") }
            }
    }

    /**
     * Pausar o reanudar una rutina.
     *
     * Va aparte de `completeResource` porque NO es la acción principal: la
     * tarjeta conserva un solo gesto rápido, «Hecha», y esto vive en la hoja de
     * detalle. Usa el PATCH de edición, que admite `active` — comprobado de
     * extremo a extremo antes de implementarlo, no supuesto por que exista un
     * PATCH.
     *
     * No toca `nextExecutionDate`: el backend tampoco lo hace al pausar, y
     * moverla aquí sería inventar una regla de recurrencia.
     */
    fun setRoutineActive(id: String, active: Boolean) {
        val routine = _state.value.data.routines.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.setRoutineActive(id, active, routine.version)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo actualizar") } }
        }
    }

    /**
     * Crea, desde el propio formulario, el registro del que ese formulario
     * depende: el artículo que cubre una garantía, la persona con la que es un
     * seguimiento.
     *
     * Existe porque un vínculo obligatorio que exige otro registro previo deja
     * al usuario atascado: sin artículos, "Nueva garantía" no se podía guardar
     * y la hoja solo explicaba qué faltaba. Y es el caso NORMAL, no el raro —
     * registras la garantía porque acabas de comprar el artículo.
     *
     * El registro nuevo nace en el módulo activo, el mismo en el que va a
     * nacer el recurso que se está creando: el backend rechaza enlazar recursos
     * de módulos distintos (ADR-019 regla 2).
     *
     * `refresh()` después de crear, no inserción a mano: es lo que hace que la
     * lista del selector incluya el registro nuevo sin duplicarlo.
     */
    fun quickCreate(
        source: ReferenceSource,
        name: String,
        extra: String?,
        onCreated: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val result = when (source) {
                ReferenceSource.INVENTORY -> repository.createInventoryItem(
                    name = name,
                    category = extra?.ifBlank { null } ?: "HOGAR",
                    location = null,
                    context = contextParam(),
                )
                ReferenceSource.PEOPLE -> repository.createPerson(name, null, null)
                // Ningún campo obligatorio depende de un proyecto, así que la
                // hoja no ofrece esta puerta y aquí no se inventa.
                ReferenceSource.PROJECTS -> Result.failure(
                    IllegalStateException("Los proyectos se crean desde su propia sección."),
                )
            }
            result
                .onSuccess { newId ->
                    onCreated(newId)
                    refresh()
                }
                .onFailure { e ->
                    onCreated(null)
                    _state.update { it.copy(error = e.message ?: "No se pudo crear") }
                }
        }
    }

    /** Lee el archivo elegido en el selector del sistema, fuera del hilo principal. */
    fun readFile(uri: android.net.Uri, onResult: (FilePayload?) -> Unit) {
        viewModelScope.launch {
            fileReader.read(uri)
                .onSuccess(onResult)
                .onFailure { e ->
                    onResult(null)
                    _state.update { it.copy(error = e.message ?: "No se pudo leer el archivo") }
                }
        }
    }

    /* ---------------------------------------------------------------------
       Familia (ADR-025)
       --------------------------------------------------------------------- */

    /**
     * Búsqueda con freno: cinco caracteres mínimo y 350 ms de espera. Sin esto
     * cada tecla sería una consulta sobre el padrón de usuarios.
     */
    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 5) {
            _state.update { it.copy(userSearch = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            delay(350)
            repository.searchUsers(query)
                .onSuccess { results -> _state.update { it.copy(userSearch = results, searching = false) } }
                .onFailure { e -> _state.update { it.copy(searching = false, error = e.message) } }
        }
    }

    fun invite(userId: String) {
        viewModelScope.launch {
            repository.invite(userId)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo invitar") } }
        }
    }

    fun acceptInvitation(id: String) {
        viewModelScope.launch {
            repository.acceptInvitation(id)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun rejectInvitation(id: String) {
        viewModelScope.launch {
            repository.rejectInvitation(id)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    /** «Ya hice mi parte» (DEC-001): estado de la fila de compartición. */
    fun togglePartDone(shareId: String, done: Boolean) {
        viewModelScope.launch {
            val result = if (done) repository.undoPartDone(shareId) else repository.markPartDone(shareId)
            result
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    /* ---------------------------------------------------------------------
       Calendario
       --------------------------------------------------------------------- */

    /** Lo que un día contiene: los tres ritmos que el calendario superpone. */
    fun contentFor(date: LocalDate): DayContent {
        val zone = ZoneId.systemDefault()
        val tasks = _state.value.reminders.mapNotNull { r ->
            val due = r.dueAt?.let { runCatching { Instant.parse(it).atZone(zone) }.getOrNull() }
            if (due == null || due.toLocalDate() != date) {
                null
            } else {
                DayTask(
                    id = r.id,
                    title = r.title,
                    time = due.toLocalTime(),
                    meta = if (r.status == "COMPLETED") "Completada" else "Recordatorio",
                    done = r.status == "COMPLETED",
                    location = r.location?.ifBlank { null },
                    date = date,
                )
            }
        }
        return DayContent(
            date = date,
            tasks = tasks,
            alerts = derivedAlerts.filter { it.date == date },
            // Solo el día seleccionado tiene sus notas cargadas; el resto de la
            // rejilla no las pide para no lanzar treinta peticiones por mes.
            notes = if (date == _state.value.selectedDate) _state.value.notes.map { it.text } else emptyList(),
        )
    }

    /** Las tareas sin fecha también existen: se ven en Tareas, no en el calendario. */
    fun allTasks(): List<DayTask> {
        val zone = ZoneId.systemDefault()
        return _state.value.reminders.map { r ->
            val due = r.dueAt?.let { runCatching { Instant.parse(it).atZone(zone) }.getOrNull() }
            DayTask(
                id = r.id,
                title = r.title,
                time = due?.toLocalTime(),
                meta = due?.toLocalDate()?.toString() ?: "Sin fecha",
                done = r.status == "COMPLETED",
                location = r.location?.ifBlank { null },
                date = due?.toLocalDate(),
            )
        }
    }

    fun setContext(context: AppContext) {
        if (_state.value.context == context) return
        _state.update { it.copy(context = context) }
        // El contexto filtra los datos en el servidor (ADR-019): al cambiarlo
        // hay que volver a pedirlos, o se vería lo del contexto anterior.
        refresh()
        loadNotes(_state.value.selectedDate)
    }

    fun setTheme(theme: VisualTheme) {
        prefs.setTheme(theme)
        _state.update { it.copy(theme = theme) }
    }

    fun setProfile(profile: ProfessionalProfile) {
        prefs.setProfile(profile)
        _state.update { it.copy(profile = profile) }
    }

    fun setLaboralEnabled(enabled: Boolean) {
        prefs.setLaboralEnabled(enabled)
        val next = _state.value.copy(laboralEnabled = enabled)
        // Si se apaga estando dentro, el contexto vuelve a Personal en vez de
        // dejar al usuario en una navegación que ya no existe.
        _state.value = if (!enabled && next.context == AppContext.LABORAL) {
            next.copy(context = AppContext.PERSONAL)
        } else {
            next
        }
    }

    fun selectDate(date: LocalDate) {
        if (_state.value.selectedDate == date) return
        _state.update { it.copy(selectedDate = date, visibleMonth = YearMonth.from(date), notes = emptyList()) }
        loadNotes(date)
    }

    /**
     * Semana anterior o siguiente.
     *
     * Mueve la FECHA SELECCIONADA siete días, que es lo que la vista semanal
     * usa para calcular su semana (`weekOf(selected)`). Así el día elegido
     * mantiene su posición dentro de la semana —de un martes se pasa al martes
     * siguiente—, en vez de saltar al lunes y perder la referencia. El mes
     * visible se ajusta solo para que volver a la vista mensual no aterrice en
     * otro mes del que se estaba mirando.
     */
    fun shiftWeek(delta: Long) {
        val target = _state.value.selectedDate.plusWeeks(delta)
        _state.update { it.copy(selectedDate = target, visibleMonth = YearMonth.from(target), notes = emptyList()) }
        loadNotes(target)
    }

    fun shiftMonth(delta: Long) {
        val month = _state.value.visibleMonth.plusMonths(delta)
        val today = LocalDate.now()
        // Al cambiar de mes se elige un día de ese mes: dejar la selección
        // fuera del mes visible dejaría el panel colgando en ninguna fila.
        val target = if (YearMonth.from(today) == month) today else month.atDay(1)
        _state.update { it.copy(visibleMonth = month, selectedDate = target, notes = emptyList()) }
        loadNotes(target)
    }

    /**
     * Vuelve a hoy. No delega en `selectDate` porque ese atajo se corta si la
     * fecha ya es la de hoy, y entonces no devolvería la rejilla a este mes
     * cuando el usuario se ha ido a navegar meses adelante.
     */
    fun goToToday() {
        val today = LocalDate.now()
        val wasElsewhere = _state.value.selectedDate != today
        _state.update { it.copy(selectedDate = today, visibleMonth = YearMonth.from(today)) }
        if (wasElsewhere) {
            _state.update { it.copy(notes = emptyList()) }
            loadNotes(today)
        }
    }

    fun setDensity(value: String) {
        _state.update { it.copy(calendarDensity = value) }
    }

    /** Completa o reabre una tarea contra el endpoint real, con su alarma. */
    fun toggleTask(taskId: String) {
        val reminder = _state.value.reminders.firstOrNull { it.id == taskId } ?: return
        if (reminder.status == "COMPLETED") return
        viewModelScope.launch {
            runCatching { reminderApi.completeReminder(reminder.id, CompleteReminderRequest(reminder.version)) }
                .onSuccess {
                    alarmScheduler.cancel(reminder.id)
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       LAS TRES CAPACIDADES DEL ARTEFACTO — contra la API real
       ══════════════════════════════════════════════════════════════════════ */

    /**
     * Carga el ánimo de hoy y el de la semana.
     *
     * DOS PETICIONES Y NO UNA: el de hoy se necesita en Inicio, donde la cara
     * vive en una tarjeta pequeña; la semana solo en Bienestar. Pedir siempre
     * las dos haría que abrir Inicio trajera seis días que nadie va a mirar.
     */
    fun loadMood(includeWeek: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(moodLoading = true, moodError = null) }
            val today = wellbeing.moodToday()
            val week = if (includeWeek) {
                val to = LocalDate.now()
                wellbeing.moods(to.minusDays(6), to)
            } else {
                null
            }
            _state.update { st ->
                st.copy(
                    moodLoading = false,
                    moodToday = today.getOrNull() ?: st.moodToday,
                    moodWeek = week?.getOrNull() ?: st.moodWeek,
                    // Si falla, se dice. No se rellena con un valor de reserva
                    // que aparentaría un ánimo que el usuario nunca marcó.
                    moodError = (today.exceptionOrNull() ?: week?.exceptionOrNull())?.let { friendly(it) },
                )
            }
        }
    }

    /**
     * Marcar cómo te sientes. Upsert: volver a tocar el mismo día corrige.
     *
     * Se pinta en cuanto responde el servidor y no antes: el ánimo es el dato
     * más sensible del producto, y mostrarlo guardado antes de estarlo sería
     * decirle al usuario que quedó registrado algo que quizá no quedó.
     */
    fun setMood(value: Int, note: String? = null, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            _state.update { it.copy(moodLoading = true, moodError = null) }
            wellbeing.setMood(value, note, tags)
                .onSuccess { saved ->
                    _state.update { st ->
                        st.copy(
                            moodLoading = false,
                            moodToday = saved,
                            moodWeek = st.moodWeek.filterNot { it.date == saved.date } + saved,
                            moodError = null,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(moodLoading = false, moodError = friendly(e)) } }
        }
    }

    /** Borrado DURO del historial (Ajustes → Privacidad). */
    fun deleteMoodHistory(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            wellbeing.deleteAllMoods()
                .onSuccess { n ->
                    _state.update { it.copy(moodToday = null, moodWeek = emptyList(), moodError = null) }
                    onDone(n)
                }
                .onFailure { e -> _state.update { it.copy(moodError = friendly(e)) } }
        }
    }

    /* ── Pasos de una tarea ─────────────────────────────────────────────── */

    fun loadSteps(reminderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(stepsLoading = true, stepsError = null, stepsFor = reminderId) }
            wellbeing.steps(reminderId)
                .onSuccess { list -> _state.update { it.copy(stepsLoading = false, steps = list) } }
                .onFailure { e ->
                    _state.update { it.copy(stepsLoading = false, steps = emptyList(), stepsError = friendly(e)) }
                }
        }
    }

    fun addStep(reminderId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            wellbeing.addStep(reminderId, title)
                .onSuccess { step -> _state.update { it.copy(steps = it.steps + step) } }
                .onFailure { e -> _state.update { it.copy(stepsError = friendly(e)) } }
        }
    }

    /**
     * Marcar o desmarcar. El porcentaje NO se toca aquí: se deriva de la lista
     * con `percentDone()`, así que actualizar el paso ya mueve el anillo.
     */
    fun toggleStep(reminderId: String, stepId: String) {
        viewModelScope.launch {
            wellbeing.toggleStep(reminderId, stepId)
                .onSuccess { updated ->
                    _state.update { st ->
                        st.copy(steps = st.steps.map { if (it.id == updated.id) updated else it })
                    }
                }
                .onFailure { e -> _state.update { it.copy(stepsError = friendly(e)) } }
        }
    }

    fun renameStep(reminderId: String, stepId: String, title: String) {
        viewModelScope.launch {
            wellbeing.renameStep(reminderId, stepId, title)
                .onSuccess { updated ->
                    _state.update { st ->
                        st.copy(steps = st.steps.map { if (it.id == updated.id) updated else it })
                    }
                }
                .onFailure { e -> _state.update { it.copy(stepsError = friendly(e)) } }
        }
    }

    fun deleteStep(reminderId: String, stepId: String) {
        viewModelScope.launch {
            wellbeing.deleteStep(reminderId, stepId)
                .onSuccess { _state.update { st -> st.copy(steps = st.steps.filterNot { it.id == stepId }) } }
                .onFailure { e -> _state.update { it.copy(stepsError = friendly(e)) } }
        }
    }

    /** Reordenar mandando la lista COMPLETA: el resultado no depende del orden. */
    fun reorderSteps(reminderId: String, orderedIds: List<String>) {
        viewModelScope.launch {
            wellbeing.reorderSteps(reminderId, orderedIds)
                .onSuccess { list -> _state.update { it.copy(steps = list) } }
                .onFailure { e -> _state.update { it.copy(stepsError = friendly(e)) } }
        }
    }

    /* ── Progreso de un hábito ──────────────────────────────────────────── */

    /**
     * Lo hecho hoy de cada rutina CON META. Las de sí/no no se consultan: no
     * tienen contador y preguntarlo sería una petición por nada.
     */
    fun loadHabitsToday() {
        viewModelScope.launch {
            val counted = _state.value.data.routines.filter { it.targetCount != null }
            if (counted.isEmpty()) return@launch
            val today = counted.associate { r ->
                r.id to (wellbeing.progressToday(r.id).getOrNull() ?: 0)
            }
            _state.update { it.copy(habitToday = it.habitToday + today) }
        }
    }

    /**
     * Sumar uno al hábito. NO ejecuta la rutina: `execute` cierra la ocurrencia
     * y mueve la fecha, `progress` suma dentro del día. Fundirlos haría que
     * beber un vaso de agua adelantara la rutina a mañana.
     */
    fun addHabitProgress(routineId: String, delta: Int = 1) {
        viewModelScope.launch {
            wellbeing.addProgress(routineId, delta)
                .onSuccess { p -> _state.update { it.copy(habitToday = it.habitToday + (routineId to p.count)) } }
                .onFailure { e -> _state.update { it.copy(error = friendly(e)) } }
        }
    }

    fun setHabitProgress(routineId: String, value: Int) {
        viewModelScope.launch {
            wellbeing.setProgress(routineId, value)
                .onSuccess { p -> _state.update { it.copy(habitToday = it.habitToday + (routineId to p.count)) } }
                .onFailure { e -> _state.update { it.copy(error = friendly(e)) } }
        }
    }

    /** Declarar la meta diaria de un hábito, o retirarla con `null`. */
    fun setHabitTarget(routineId: String, targetCount: Int?, unit: String?) {
        viewModelScope.launch {
            wellbeing.setTarget(routineId, targetCount, unit)
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = friendly(e)) } }
        }
    }


    /**
     * El mensaje que ve el usuario cuando algo falla.
     *
     * Un solo sitio y no `e.message ?: "..."` repetido: así el texto de red
     * caída es el mismo en Bienestar, en los pasos y en los hábitos, en vez de
     * tres redacciones que el usuario interpreta como tres fallos distintos.
     */
    private fun friendly(e: Throwable): String =
        e.message?.takeIf { it.isNotBlank() } ?: "No se pudo conectar. Inténtalo otra vez."


    /**
     * Quién ha entrado. Lo necesita Perfil y el saludo de Portal.
     *
     * Si falla NO se rellena con un nombre de relleno: la pantalla enseña su
     * estado y el usuario sabe que no se pudo leer, en vez de ver un nombre que
     * no es el suyo.
     */
    fun loadUser() {
        if (_state.value.user != null) return
        viewModelScope.launch {
            runCatching { userApi.getCurrentUser() }
                .onSuccess { u -> _state.update { it.copy(user = u) } }
                .onFailure { e -> _state.update { it.copy(error = friendly(e)) } }
        }
    }


    /* ── ADJUNTOS (V37) ─────────────────────────────────────────────────── */

    /**
     * Los adjuntos de un recurso. UNA sola función para los cinco tipos: una
     * garantía y una tarea preguntan igual cambiando `type`.
     *
     * Un fallo deja la lista vacía y no rompe la pantalla: los adjuntos son
     * accesorios del registro, no el registro.
     */
    fun loadAttachments(type: AttachTo, resourceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(attachmentsLoading = true, attachmentsFor = resourceId) }
            wellbeing.attachments(type, resourceId)
                .onSuccess { list -> _state.update { it.copy(attachmentsLoading = false, attachments = list) } }
                .onFailure { _state.update { it.copy(attachmentsLoading = false, attachments = emptyList()) } }
        }
    }

    /** Colgar un documento existente de este recurso. */
    fun attachDocument(documentId: String, type: AttachTo, resourceId: String) {
        viewModelScope.launch {
            wellbeing.attach(documentId, type, resourceId)
                .onSuccess { loadAttachments(type, resourceId) }
                .onFailure { e -> _state.update { it.copy(error = friendly(e)) } }
        }
    }

    /** Soltar SIN borrar: el documento sigue existiendo en Documentos. */
    fun detachDocument(documentId: String, type: AttachTo, resourceId: String) {
        viewModelScope.launch {
            wellbeing.detach(documentId)
                .onSuccess { loadAttachments(type, resourceId) }
                .onFailure { e -> _state.update { it.copy(error = friendly(e)) } }
        }
    }

}
