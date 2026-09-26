package com.vidacotidiana.app.feature.board

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.data.Board
import com.vidacotidiana.app.core.data.BoardElement
import com.vidacotidiana.app.core.data.ElementPatch
import com.vidacotidiana.app.core.data.VisionBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/** Qué está pasando con el guardado, para poder decirlo en la cabecera. */
sealed interface SaveStatus {
    data object Saved : SaveStatus
    data object Saving : SaveStatus
    data class Failed(val message: String) : SaveStatus
}

/**
 * Un paso deshacible. Solo se apilan los que tienen inverso EXACTO.
 *
 * «Traer al frente» y «enviar al fondo» quedan deliberadamente fuera: su
 * inverso no es otra de las cuatro operaciones —depende de dónde estaba el
 * elemento antes, y el servidor renumera toda la secuencia— así que
 * apilarlos sería ofrecer un «deshacer» que deja el tablero en otro sitio.
 * Subir y bajar una capa sí son inversos el uno del otro.
 */
sealed interface HistoryEntry {
    data class Patched(val elementId: String, val before: ElementPatch, val after: ElementPatch) : HistoryEntry
    data class Created(val elementId: String) : HistoryEntry
    data class Deleted(val snapshot: BoardElement) : HistoryEntry
    data class Layered(val elementId: String, val direction: String) : HistoryEntry
}

data class VisionBoardUiState(
    val boards: List<Board> = emptyList(),
    val board: Board? = null,
    val images: Map<String, ImageBitmap> = emptyMap(),
    val selectedId: String? = null,
    val loading: Boolean = true,
    val working: Boolean = false,
    val save: SaveStatus = SaveStatus.Saved,
    val error: String? = null,
    /** Aviso efímero al pie del lienzo; el de borrado trae «Deshacer». */
    val notice: String? = null,
    val noticeUndoable: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
) {
    val elements: List<BoardElement> get() = board?.elements.orEmpty()
    val selected: BoardElement? get() = elements.firstOrNull { it.id == selectedId }
}

@HiltViewModel
class VisionBoardViewModel @Inject constructor(
    private val repository: VisionBoardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VisionBoardUiState())
    val state: StateFlow<VisionBoardUiState> = _state.asStateFlow()

    /**
     * UNA PETICIÓN EN VUELO POR ELEMENTO.
     *
     * Sin esto, dos toques seguidos sobre «subir una capa» salen a la vez con
     * la MISMA `version`, y el segundo vuelve con un 409 aunque el usuario no
     * haya hecho nada raro. El candado por id serializa las operaciones de un
     * elemento sin frenar las de los demás.
     */
    private val locks = mutableMapOf<String, Mutex>()
    private fun lockFor(id: String): Mutex = synchronized(locks) { locks.getOrPut(id) { Mutex() } }

    private val undoStack = ArrayDeque<HistoryEntry>()
    private val redoStack = ArrayDeque<HistoryEntry>()
    private val historyLimit = 30

    init {
        load()
    }

    /* =====================================================
       CARGA
       ===================================================== */

    /**
     * Abre el tablero: PRIMERO lo que ya se sabe, después lo que diga el
     * servidor.
     *
     * Antes se enseñaba «Abriendo tu tablero…» hasta que contestaban dos
     * peticiones seguidas. Ahora la copia en disco se pinta de inmediato —y
     * sus imágenes salen de la caché de disco de OkHttp, sin red— así que el
     * panel aparece entero y se puede tocar mientras la red confirma por
     * detrás. Cuando contesta, se sustituye por lo que hay de verdad.
     *
     * La primera vez en el dispositivo no hay copia y se comporta como
     * siempre: indicador de carga y a esperar.
     */
    fun load() {
        viewModelScope.launch {
            val cached = repository.lastKnownBoard()
            if (cached != null && _state.value.board == null) {
                _state.update { it.copy(board = cached, loading = false, error = null) }
                fetchImages(cached)
            } else {
                _state.update { it.copy(loading = true) }
            }

            repository.openingBoard()
                .onSuccess { board ->
                    _state.update { it.copy(board = board, loading = false, error = null) }
                    fetchImages(board)
                    refreshBoardList()
                }
                .onFailure { e ->
                    // Con copia local ya hay algo en pantalla: el fallo se
                    // cuenta como aviso, no como pantalla de error.
                    if (_state.value.board != null) {
                        _state.update { it.copy(loading = false, notice = "Sin conexión: se muestra la última versión guardada.") }
                    } else {
                        _state.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar el tablero") }
                    }
                }
        }
    }

    private fun refreshBoardList() {
        viewModelScope.launch {
            repository.boards().onSuccess { list -> _state.update { it.copy(boards = list) } }
        }
    }

    /**
     * Trae las imágenes que el tablero necesita, de CUATRO en cuatro.
     *
     * Antes se lanzaba una corrutina por imagen: con veinte fotos eran veinte
     * descargas simultáneas peleándose por la misma conexión y veinte
     * decodificaciones a la vez. Tardaba más que hacerlo con cabeza y dejaba
     * la interfaz a tirones.
     *
     * Van en bloques y cada bloque entra en el estado de una vez, en vez de
     * repintar el lienzo veinte veces seguidas.
     */
    private fun fetchImages(board: Board) {
        // Cada imagen se pide al tamaño del elemento MÁS GRANDE que la usa:
        // así una foto pequeña no ocupa en memoria lo que una que llena el
        // tablero.
        val wanted = LinkedHashMap<String, Float>()
        board.elements.forEach { el ->
            val id = el.imageId ?: return@forEach
            val edge = maxOf(el.width, el.height)
            wanted[id] = maxOf(wanted[id] ?: 0f, edge)
        }
        val pending = wanted.keys - _state.value.images.keys
        if (pending.isEmpty()) return
        viewModelScope.launch {
            // UN VIAJE PARA TODAS. Si el servidor todavía no conoce el
            // endpoint, se cae al camino de siempre sin que se note.
            val bundle = repository.allImages(board.id, wanted)
            if (bundle != null) {
                if (bundle.isNotEmpty()) _state.update { it.copy(images = it.images + bundle) }
                return@launch
            }

            val gate = Semaphore(4)
            pending.chunked(8).forEach { chunk ->
                val loaded = coroutineScope {
                    chunk.map { id ->
                        async { gate.withPermit { repository.image(id, wanted[id])?.let { id to it } } }
                    }.awaitAll().filterNotNull()
                }
                if (loaded.isNotEmpty()) _state.update { it.copy(images = it.images + loaded) }
            }
        }
    }

    /* =====================================================
       TABLEROS
       ===================================================== */

    fun switchBoard(id: String) {
        val current = _state.value.board
        if (current?.id == id) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, selectedId = null) }
            repository.board(id)
                .onSuccess { board ->
                    clearHistory()
                    _state.update { it.copy(board = board, loading = false, notice = "Tablero «${board.name}»") }
                    fetchImages(board)
                }
                .onFailure { e -> fail(e, "No se pudo abrir el tablero") }
        }
    }

    fun createBoard(name: String, theme: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true) }
            repository.createBoard(name.ifBlank { "Nuevo tablero" }, theme)
                .onSuccess { board ->
                    clearHistory()
                    _state.update {
                        it.copy(board = board, working = false, selectedId = null, notice = "Tablero «${board.name}» creado")
                    }
                    refreshBoardList()
                }
                .onFailure { e -> fail(e, "No se pudo crear el tablero") }
        }
    }

    fun changeTheme(theme: String) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            _state.update { it.copy(save = SaveStatus.Saving) }
            repository.updateBoardTheme(board, theme)
                .onSuccess { updated ->
                    // La respuesta del PUT no trae los elementos: se conservan
                    // los que ya están en pantalla y solo sube el tema/versión.
                    _state.update {
                        it.copy(
                            board = it.board?.copy(theme = updated.theme, version = updated.version),
                            save = SaveStatus.Saved,
                            notice = "Tema «${boardThemeOf(theme).name}»",
                        )
                    }
                }
                .onFailure { e -> fail(e, "No se pudo cambiar el tema") }
        }
    }

    fun deleteCurrentBoard() {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true) }
            repository.deleteBoard(board)
                .onSuccess {
                    clearHistory()
                    val remaining = _state.value.boards.filterNot { it.id == board.id }
                    _state.update { it.copy(boards = remaining, working = false, selectedId = null, board = null) }
                    // Abrir el siguiente, o crear uno: la sección nunca se queda
                    // sin tablero, igual que en la primera carga.
                    load()
                }
                .onFailure { e -> fail(e, "No se pudo eliminar el tablero") }
        }
    }

    /* =====================================================
       SELECCIÓN
       ===================================================== */

    fun select(id: String?) {
        _state.update { it.copy(selectedId = id) }
    }

    /* =====================================================
       ALTA DE ELEMENTOS
       ===================================================== */

    fun addElement(type: String, x: Float, y: Float, data: JsonObject, size: Pair<Float, Float>? = null) {
        val board = _state.value.board ?: return
        val (w, h) = size ?: defaultSizeFor(type)
        viewModelScope.launch {
            _state.update { it.copy(save = SaveStatus.Saving) }
            repository.addElement(board.id, type, x - w / 2f, y - h / 2f, w, h, data)
                .onSuccess { element ->
                    appendElement(element)
                    pushHistory(HistoryEntry.Created(element.id))
                    _state.update { it.copy(save = SaveStatus.Saved, selectedId = element.id) }
                }
                .onFailure { e -> fail(e, "No se pudo agregar") }
        }
    }

    fun addText(text: String, x: Float, y: Float, font: String, fontSize: Int) {
        if (text.isBlank()) return
        addElement(
            "TEXT", x, y,
            buildJsonObject {
                put("text", text.trim())
                put("font", font)
                put("fontSize", fontSize)
            },
        )
    }

    fun addNote(text: String, x: Float, y: Float) {
        if (text.isBlank()) return
        addElement("NOTE", x, y, buildJsonObject { put("text", text.trim()) })
    }

    fun addShape(shapeId: String, fill: String?, textColor: String?, x: Float, y: Float) {
        addElement(
            "SHAPE", x, y,
            buildJsonObject {
                put("shape", shapeId)
                if (fill != null) put("fill", fill)
                if (textColor != null) put("textColor", textColor)
            },
        )
    }

    fun addSticker(stickerId: String, x: Float, y: Float) =
        addElement("STICKER", x, y, buildJsonObject { put("stickerId", stickerId) })

    fun addEmoji(emojiId: String, x: Float, y: Float) =
        addElement("STICKER", x, y, buildJsonObject { put("emojiId", emojiId) })

    fun addChart(chartType: String, x: Float, y: Float) =
        addElement("CHART", x, y, buildJsonObject { put("chartType", chartType) })

    fun addGrid(layout: String, x: Float, y: Float) =
        addElement("GRID", x, y, buildJsonObject { put("layout", layout) })

    fun addTable(x: Float, y: Float) =
        addElement("TABLE", x, y, buildJsonObject { put("sample", true) })

    /** Pega lo que haya copiado — el caso que motivó esta sección. */
    fun pasteFromClipboard(x: Float, y: Float) {
        val uri = repository.clipboardImage()
        if (uri == null) {
            _state.update { it.copy(notice = "No hay ninguna imagen copiada. Copia una y vuelve.") }
            return
        }
        addImage(uri, x, y, frameStyle = null)
    }

    fun addImage(uri: Uri, x: Float, y: Float, frameStyle: String?) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true, save = SaveStatus.Saving, notice = null) }
            val aspect = repository.imageAspect(uri) ?: 1.5f
            // La forma del elemento sale de la imagen, no de un tamaño fijo:
            // una foto vertical no debe entrar recortada en una caja apaisada.
            val (baseW, baseH) = defaultSizeFor("IMAGE")
            val w: Float
            val h: Float
            if (aspect >= 1f) {
                w = baseW; h = baseW / aspect
            } else {
                h = baseH; w = baseH * aspect
            }
            repository.uploadImage(uri)
                .onSuccess { imageId ->
                    repository.addElement(
                        board.id, "IMAGE", x - w / 2f, y - h / 2f, w, h,
                        buildJsonObject {
                            put("imageId", imageId)
                            if (frameStyle != null) put("frameStyle", frameStyle)
                        },
                    )
                        .onSuccess { element ->
                            appendElement(element)
                            pushHistory(HistoryEntry.Created(element.id))
                            _state.update { it.copy(working = false, save = SaveStatus.Saved, selectedId = element.id) }
                            repository.image(imageId)?.let { bmp ->
                                _state.update { it.copy(images = it.images + (imageId to bmp)) }
                            }
                        }
                        .onFailure { e -> fail(e, "No se pudo colocar la imagen") }
                }
                .onFailure { e -> fail(e, "No se pudo subir la imagen") }
        }
    }

    /**
     * «Reemplazar» de la barra de propiedades: sube la foto nueva y cambia a
     * qué imagen apunta el elemento, sin tocar su sitio, su tamaño ni su
     * marco. Es lo que distingue reemplazar de borrar y volver a poner.
     */
    fun replaceImage(elementId: String, uri: Uri) {
        val element = elementById(elementId) ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true, save = SaveStatus.Saving) }
            repository.uploadImage(uri)
                .onSuccess { imageId ->
                    val data = buildJsonObject {
                        put("imageId", imageId)
                        element.str("frameStyle")?.let { put("frameStyle", it) }
                        element.str("entrance")?.let { put("entrance", it) }
                    }
                    repository.image(imageId)?.let { bmp ->
                        _state.update { it.copy(images = it.images + (imageId to bmp)) }
                    }
                    _state.update { it.copy(working = false) }
                    applyPatch(
                        elementId,
                        ElementPatch(data = data),
                        ElementPatch(data = element.data),
                        record = true,
                        notice = "Imagen reemplazada",
                    )
                }
                .onFailure { e -> fail(e, "No se pudo subir la imagen") }
        }
    }

    /**
     * Aplica una plantilla.
     *
     * Las de partida son puro texto y formas y entran de una. Las cuatro del
     * portal traen fotografías empaquetadas: cada una se SUBE primero al
     * almacén de imágenes del usuario y el elemento se crea ya con su
     * `imageId` (ver el doc de `VisionBoardTemplates.kt` sobre por qué no
     * pueden compartirse ids). Son hasta veintidós subidas, así que el aviso
     * va contando: una plantilla que tarda sin decir nada parece colgada.
     *
     * Un fichero repetido se sube UNA vez y se reutiliza su id.
     */
    fun applyTemplate(template: VisionBoardTemplate) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true, save = SaveStatus.Saving) }
            val fitted = fitTemplate(template, board.width.toFloat(), board.height.toFloat())
            var failed: Throwable? = null

            // PRIMERO LAS FOTOS, Y EN PARALELO.
            //
            // Eran diecinueve subidas EN FILA, cada una esperando a que
            // terminara la anterior, y detrás de cada una su elemento. Con
            // cuatro a la vez el tiempo total lo marca la más lenta de cada
            // tanda, no la suma de todas. Cuatro y no veinte: un teléfono con
            // datos móviles no gana nada abriendo veinte conexiones, y sí
            // pierde memoria y batería.
            val files = fitted.mapNotNull { it.data["assetFile"]?.jsonPrimitive?.contentOrNull }.distinct()
            val done = AtomicInteger(0)
            val uploaded: Map<String, String> = if (files.isEmpty()) {
                emptyMap()
            } else {
                val gate = Semaphore(4)
                coroutineScope {
                    files.map { file ->
                        async {
                            gate.withPermit {
                                val id = repository.uploadTemplateAsset(file)
                                    .onFailure { e -> failed = failed ?: e }
                                    .getOrNull()
                                val n = done.incrementAndGet()
                                _state.update { it.copy(notice = "Preparando la plantilla… $n de ${files.size}") }
                                id?.let { file to it }
                            }
                        }
                    }.awaitAll().filterNotNull().toMap()
                }
            }

            // Las fotos ya están decodificadas en la caché del repositorio (se
            // sembraron con los bytes que ya teníamos al subirlas), así que
            // aquí solo se recogen: ni una descarga más.
            _state.update { s ->
                s.copy(images = s.images + uploaded.values.mapNotNull { id -> repository.cached(id)?.let { id to it } })
            }

            // Y LOS ELEMENTOS EN ORDEN, uno detrás de otro: el orden de alta es
            // el orden de pintado, y crearlos en paralelo barajaría la
            // composición que la plantilla define.
            fitted.forEach { el ->
                val assetFile = el.data["assetFile"]?.jsonPrimitive?.contentOrNull
                val data = if (assetFile == null) {
                    el.data
                } else {
                    val imageId = uploaded[assetFile] ?: return@forEach
                    buildJsonObject {
                        put("imageId", imageId)
                        el.data["frameStyle"]?.let { put("frameStyle", it) }
                    }
                }
                repository.addElement(board.id, el.type, el.x, el.y, el.width, el.height, data)
                    .onSuccess { created -> appendElement(created) }
                    .onFailure { e -> failed = failed ?: e }
            }

            clearHistory()
            if (failed != null) {
                fail(failed!!, "La plantilla se aplicó a medias")
            } else {
                _state.update {
                    it.copy(
                        working = false,
                        save = SaveStatus.Saved,
                        notice = "Plantilla «${template.name}» aplicada",
                        noticeUndoable = false,
                    )
                }
            }
        }
    }

    /* =====================================================
       EDICIÓN
       ===================================================== */

    /**
     * Guarda dónde quedó un elemento tras arrastrarlo, redimensionarlo o
     * girarlo. Se llama al SOLTAR, no durante el gesto: mandar una petición
     * por cada píxel llenaría la red y cada respuesta traería una `version`
     * nueva que invalidaría la siguiente.
     */
    fun commitGeometry(element: BoardElement, x: Float, y: Float, width: Float, height: Float, rotation: Float) {
        val before = ElementPatch(
            x = element.x, y = element.y,
            width = element.width, height = element.height, rotation = element.rotation,
        )
        val after = ElementPatch(x = x, y = y, width = width, height = height, rotation = rotation)
        if (before == after) return
        applyPatch(element.id, after, before, record = true)
    }

    fun editData(elementId: String, data: JsonObject) {
        val element = elementById(elementId) ?: return
        applyPatch(elementId, ElementPatch(data = data), ElementPatch(data = element.data), record = true)
    }

    fun toggleLock(elementId: String) {
        val element = elementById(elementId) ?: return
        val next = !element.locked
        applyPatch(
            elementId,
            ElementPatch(locked = next),
            ElementPatch(locked = element.locked),
            record = true,
            notice = if (next) "Bloqueado: no se mueve ni se borra" else "Desbloqueado",
        )
    }

    fun duplicate(elementId: String) {
        val board = _state.value.board ?: return
        val element = elementById(elementId) ?: return
        viewModelScope.launch {
            _state.update { it.copy(save = SaveStatus.Saving) }
            repository.addElement(
                board.id, element.type,
                element.x + 24f, element.y + 24f, element.width, element.height, element.data,
            )
                .onSuccess { copy ->
                    appendElement(copy)
                    pushHistory(HistoryEntry.Created(copy.id))
                    _state.update { it.copy(save = SaveStatus.Saved, selectedId = copy.id, notice = "Duplicado") }
                }
                .onFailure { e -> fail(e, "No se pudo duplicar") }
        }
    }

    fun changeLayer(elementId: String, direction: String) {
        val board = _state.value.board ?: return
        val element = elementById(elementId) ?: return
        if (element.locked) return
        viewModelScope.launch {
            lockFor(elementId).withLock {
                _state.update { it.copy(save = SaveStatus.Saving) }
                repository.reorder(board.id, element, direction)
                    .onSuccess { updated ->
                        _state.update { s ->
                            val byId = updated.associateBy { it.id }
                            s.copy(
                                board = s.board?.let { b ->
                                    b.copy(elements = b.elements.map { byId[it.id] ?: it }.sortedBy { it.zIndex })
                                },
                                save = SaveStatus.Saved,
                            )
                        }
                        if (direction == "RAISE" || direction == "LOWER") {
                            pushHistory(HistoryEntry.Layered(elementId, direction))
                        }
                    }
                    .onFailure { e -> recover(e, elementId, "No se pudo cambiar la capa") }
            }
        }
    }

    fun deleteElement(elementId: String) {
        val board = _state.value.board ?: return
        val element = elementById(elementId) ?: return
        if (element.locked) return
        viewModelScope.launch {
            _state.update { it.copy(save = SaveStatus.Saving) }
            repository.deleteElement(board.id, element)
                .onSuccess {
                    _state.update { s ->
                        s.copy(
                            board = s.board?.let { b -> b.copy(elements = b.elements.filterNot { it.id == elementId }) },
                            selectedId = null,
                            save = SaveStatus.Saved,
                            notice = "Elemento eliminado",
                            noticeUndoable = true,
                        )
                    }
                    pushHistory(HistoryEntry.Deleted(element))
                }
                .onFailure { e -> fail(e, "No se pudo eliminar") }
        }
    }

    /**
     * El corazón del guardado: una operación por elemento, en cola, y un
     * conflicto recarga SOLO ese elemento.
     *
     * Antes, cualquier fallo al mover disparaba una recarga del tablero
     * entero y todo el lienzo daba un salto. Aquí el resto de los elementos
     * ni se entera.
     */
    private fun applyPatch(
        elementId: String,
        patch: ElementPatch,
        inverse: ElementPatch,
        record: Boolean,
        notice: String? = null,
    ) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            lockFor(elementId).withLock {
                val element = elementById(elementId) ?: return@withLock
                _state.update { it.copy(save = SaveStatus.Saving) }
                repository.updateElement(board.id, element, patch)
                    .onSuccess { updated ->
                        replaceElement(updated)
                        _state.update { it.copy(save = SaveStatus.Saved, notice = notice ?: it.notice) }
                        if (record) pushHistory(HistoryEntry.Patched(elementId, inverse, patch))
                    }
                    .onFailure { e -> recover(e, elementId, "No se pudo guardar el cambio") }
            }
        }
    }

    /**
     * Un conflicto de versión (409) no es un error del usuario: alguien —otra
     * pantalla, la Web— tocó ese elemento antes. Se vuelve a pedir ESE
     * elemento y se deja el tablero coherente, sin recargarlo entero.
     */
    private suspend fun recover(e: Throwable, elementId: String, fallback: String) {
        val board = _state.value.board
        val conflict = (e as? HttpException)?.code() == 409
        if (conflict && board != null) {
            val fresh = repository.refreshElement(board.id, elementId)
            if (fresh != null) replaceElement(fresh)
            _state.update {
                it.copy(save = SaveStatus.Saved, notice = "Ese elemento había cambiado: se actualizó con lo que hay guardado.")
            }
            return
        }
        _state.update { it.copy(save = SaveStatus.Failed(e.message ?: fallback), error = e.message ?: fallback) }
    }

    /** Reintenta lo último que falló volviendo a pedir el tablero. */
    fun retrySave() {
        _state.update { it.copy(save = SaveStatus.Saving) }
        val id = _state.value.board?.id
        viewModelScope.launch {
            if (id == null) {
                load()
                return@launch
            }
            repository.board(id)
                .onSuccess { board ->
                    _state.update { it.copy(board = board, save = SaveStatus.Saved, error = null) }
                    fetchImages(board)
                }
                .onFailure { e -> _state.update { it.copy(save = SaveStatus.Failed(e.message ?: "Sigue sin guardar")) } }
        }
    }

    /* =====================================================
       DESHACER / REHACER
       ===================================================== */

    fun undo() {
        val entry = undoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            val inverse = revert(entry)
            if (inverse != null) {
                redoStack.addLast(inverse)
                if (redoStack.size > historyLimit) redoStack.removeFirst()
            }
            publishHistory()
        }
    }

    fun redo() {
        val entry = redoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            val inverse = revert(entry)
            if (inverse != null) {
                undoStack.addLast(inverse)
                if (undoStack.size > historyLimit) undoStack.removeFirst()
            }
            publishHistory()
        }
    }

    /** Aplica el inverso de un paso y devuelve el paso que lo desharía. */
    private suspend fun revert(entry: HistoryEntry): HistoryEntry? {
        val board = _state.value.board ?: return null
        return when (entry) {
            is HistoryEntry.Patched -> {
                val element = elementById(entry.elementId) ?: return null
                var result: HistoryEntry? = null
                repository.updateElement(board.id, element, entry.before)
                    .onSuccess {
                        replaceElement(it)
                        result = HistoryEntry.Patched(entry.elementId, entry.after, entry.before)
                    }
                    .onFailure { e -> recover(e, entry.elementId, "No se pudo deshacer") }
                result
            }

            is HistoryEntry.Created -> {
                val element = elementById(entry.elementId) ?: return null
                var result: HistoryEntry? = null
                repository.deleteElement(board.id, element)
                    .onSuccess {
                        _state.update { s ->
                            s.copy(
                                board = s.board?.let { b ->
                                    b.copy(elements = b.elements.filterNot { it.id == entry.elementId })
                                },
                                selectedId = null,
                            )
                        }
                        result = HistoryEntry.Deleted(element)
                    }
                    .onFailure { e -> fail(e, "No se pudo deshacer") }
                result
            }

            is HistoryEntry.Deleted -> {
                val s = entry.snapshot
                var result: HistoryEntry? = null
                repository.addElement(board.id, s.type, s.x, s.y, s.width, s.height, s.data)
                    .onSuccess { restored ->
                        appendElement(restored)
                        // El id cambia al recrearlo: los pasos que apuntaban al
                        // viejo se reescriben, o «rehacer» borraría algo que ya
                        // no existe.
                        remapId(s.id, restored.id)
                        _state.update { it.copy(selectedId = restored.id, notice = null, noticeUndoable = false) }
                        result = HistoryEntry.Created(restored.id)
                    }
                    .onFailure { e -> fail(e, "No se pudo recuperar el elemento") }
                result
            }

            is HistoryEntry.Layered -> {
                val element = elementById(entry.elementId) ?: return null
                val opposite = if (entry.direction == "RAISE") "LOWER" else "RAISE"
                var result: HistoryEntry? = null
                repository.reorder(board.id, element, opposite)
                    .onSuccess { updated ->
                        val byId = updated.associateBy { it.id }
                        _state.update { st ->
                            st.copy(
                                board = st.board?.let { b ->
                                    b.copy(elements = b.elements.map { byId[it.id] ?: it }.sortedBy { it.zIndex })
                                },
                            )
                        }
                        result = HistoryEntry.Layered(entry.elementId, opposite)
                    }
                    .onFailure { e -> recover(e, entry.elementId, "No se pudo deshacer la capa") }
                result
            }
        }
    }

    /** El «Deshacer» del aviso: solo tiene sentido tras un borrado. */
    fun undoNotice() {
        if (_state.value.noticeUndoable) undo()
        _state.update { it.copy(notice = null, noticeUndoable = false) }
    }

    private fun pushHistory(entry: HistoryEntry) {
        undoStack.addLast(entry)
        if (undoStack.size > historyLimit) undoStack.removeFirst()
        redoStack.clear()
        publishHistory()
    }

    private fun remapId(oldId: String, newId: String) {
        fun map(entry: HistoryEntry): HistoryEntry = when {
            entry is HistoryEntry.Patched && entry.elementId == oldId -> entry.copy(elementId = newId)
            entry is HistoryEntry.Created && entry.elementId == oldId -> entry.copy(elementId = newId)
            entry is HistoryEntry.Layered && entry.elementId == oldId -> entry.copy(elementId = newId)
            else -> entry
        }
        val u = undoStack.map(::map); undoStack.clear(); undoStack.addAll(u)
        val r = redoStack.map(::map); redoStack.clear(); redoStack.addAll(r)
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        publishHistory()
    }

    private fun publishHistory() {
        _state.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
    }

    /* =====================================================
       AVISOS Y UTILIDADES
       ===================================================== */

    fun dismissNotice() {
        _state.update { it.copy(notice = null, noticeUndoable = false, error = null) }
    }

    private fun fail(e: Throwable, fallback: String) {
        _state.update {
            it.copy(
                working = false,
                save = SaveStatus.Failed(e.message ?: fallback),
                error = e.message ?: fallback,
            )
        }
    }

    private fun elementById(id: String): BoardElement? = _state.value.elements.firstOrNull { it.id == id }

    private fun appendElement(element: BoardElement) {
        _state.update { s ->
            s.copy(board = s.board?.let { it.copy(elements = (it.elements + element).sortedBy { e -> e.zIndex }) })
        }
    }

    private fun replaceElement(element: BoardElement) {
        _state.update { s ->
            s.copy(
                board = s.board?.let { b ->
                    b.copy(elements = b.elements.map { if (it.id == element.id) element else it }.sortedBy { it.zIndex })
                },
            )
        }
    }
}
