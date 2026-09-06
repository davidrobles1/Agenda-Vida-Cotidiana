package com.vidacotidiana.app.feature.board

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidacotidiana.app.core.data.Board
import com.vidacotidiana.app.core.data.BoardElement
import com.vidacotidiana.app.core.data.VisionBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * El lienzo del usuario: sus elementos reales y las imágenes ya decodificadas.
 *
 * Las imágenes se guardan aparte del tablero (`images`) porque su ciclo de
 * vida es distinto: el tablero llega en una petición y cada imagen en la suya,
 * y el lienzo debe poder pintarse con las que ya tiene mientras las demás
 * siguen bajando.
 */
data class VisionBoardUiState(
    val board: Board? = null,
    val images: Map<String, ImageBitmap> = emptyMap(),
    val loading: Boolean = true,
    val uploading: Boolean = false,
    val error: String? = null,
    /** Se muestra tras pegar o subir, y se limpia al tocar el lienzo. */
    val notice: String? = null,
)

@HiltViewModel
class VisionBoardViewModel @Inject constructor(
    private val repository: VisionBoardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VisionBoardUiState())
    val state: StateFlow<VisionBoardUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repository.loadBoard()
                .onSuccess { board ->
                    _state.update { it.copy(board = board, loading = false, error = null) }
                    fetchImages(board)
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar el tablero") }
                }
        }
    }

    private fun fetchImages(board: Board) {
        val pending = board.elements.mapNotNull { it.imageId }.distinct() - _state.value.images.keys
        pending.forEach { id ->
            viewModelScope.launch {
                repository.image(id)?.let { bitmap ->
                    _state.update { it.copy(images = it.images + (id to bitmap)) }
                }
            }
        }
    }

    /** ¿Hay una imagen copiada ahora mismo? Decide si el botón «Pegar» sirve. */
    fun clipboardHasImage(): Boolean = repository.clipboardImage() != null

    /**
     * Pega la imagen del portapapeles — el caso que motivó todo esto: copiar
     * una imagen en Chrome y traerla al tablero.
     */
    fun pasteFromClipboard(x: Float, y: Float) {
        val uri = repository.clipboardImage()
        if (uri == null) {
            _state.update { it.copy(notice = "No hay ninguna imagen copiada. Copia una desde Chrome y vuelve.") }
            return
        }
        addImage(uri, x, y)
    }

    /** Añade una imagen elegida en el selector de fotos del sistema. */
    fun addImage(uri: Uri, x: Float, y: Float) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, notice = null) }
            repository.addImage(board.id, uri, x, y)
                .onSuccess { element ->
                    _state.update { s ->
                        s.copy(
                            board = s.board?.let { b -> b.copy(elements = b.elements + element) },
                            uploading = false,
                            notice = "Imagen añadida. Arrástrala donde quieras.",
                        )
                    }
                    element.imageId?.let { id ->
                        repository.image(id)?.let { bitmap ->
                            _state.update { it.copy(images = it.images + (id to bitmap)) }
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(uploading = false, error = e.message ?: "No se pudo subir la imagen") }
                }
        }
    }

    fun addText(text: String, x: Float, y: Float) {
        val board = _state.value.board ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addText(board.id, text.trim(), x, y)
                .onSuccess { element -> appendElement(element) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun addShape(shape: String, x: Float, y: Float) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            repository.addShape(board.id, shape, x, y)
                .onSuccess { element -> appendElement(element) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    /**
     * Guarda la posición al SOLTAR, no durante el arrastre.
     *
     * Mientras se arrastra, la posición vive en la propia composición: mandar
     * una petición por cada píxel llenaría la red y encima cada respuesta
     * traería una `version` nueva que invalidaría la siguiente.
     */
    fun onDropped(element: BoardElement, x: Float, y: Float) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            repository.move(board.id, element, x, y)
                .onSuccess { moved -> replaceElement(moved) }
                .onFailure { e ->
                    // Si el guardado falla se recarga: dejar el elemento donde
                    // el dedo lo soltó sería mentirle al usuario sobre lo que
                    // quedó guardado.
                    _state.update { it.copy(error = e.message ?: "No se pudo mover") }
                    load()
                }
        }
    }

    fun delete(element: BoardElement) {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            repository.delete(board.id, element)
                .onSuccess {
                    _state.update { s ->
                        s.copy(board = s.board?.let { b -> b.copy(elements = b.elements.filterNot { it.id == element.id }) })
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun dismissNotice() {
        _state.update { it.copy(notice = null, error = null) }
    }

    private fun appendElement(element: BoardElement) {
        _state.update { s -> s.copy(board = s.board?.let { it.copy(elements = it.elements + element) }) }
    }

    private fun replaceElement(element: BoardElement) {
        _state.update { s ->
            s.copy(
                board = s.board?.let { b ->
                    b.copy(elements = b.elements.map { if (it.id == element.id) element else it })
                },
            )
        }
    }
}
