package com.vidacotidiana.app.core.data

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vidacotidiana.app.core.network.BoardElementDto
import com.vidacotidiana.app.core.network.CreateBoardElementRequest
import com.vidacotidiana.app.core.network.CreateVisionBoardRequest
import com.vidacotidiana.app.core.network.ReorderElementRequest
import com.vidacotidiana.app.core.network.UpdateBoardElementRequest
import com.vidacotidiana.app.core.network.UpdateVisionBoardRequest
import com.vidacotidiana.app.core.network.VisionBoardApi
import com.vidacotidiana.app.core.network.VisionBoardDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vision Board: el lienzo real del usuario, con sus imágenes.
 *
 * SIN COIL, y no por limitación sino porque no hace falta: las imágenes de
 * este módulo no vienen de URLs públicas sino de dos sitios que Android ya
 * sabe leer —el `ContentResolver` (portapapeles y selector de fotos) y un
 * endpoint autenticado que devuelve bytes—. Decodificar esos bytes es
 * `BitmapFactory` y una caché en memoria; añadir una librería de red de
 * imágenes solo para eso habría traído su propio cliente HTTP sin nuestro
 * `Authorization`, que es justo el problema que no queremos.
 *
 * EL `data` VIAJA ENTERO, sin desmontarlo en campos sueltos. Antes esta capa
 * extraía tres cadenas (`text`, `shape`, `imageId`) y tiraba el resto, así
 * que un elemento creado en la Web con caligrafía, color o marco perdía todo
 * eso en cuanto el móvil lo guardaba de nuevo: el backend reemplaza el
 * `data` completo en cada edición. Ahora se conserva el objeto y se leen sus
 * claves cuando hacen falta.
 */

data class BoardElement(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val zIndex: Int = 0,
    val locked: Boolean = false,
    val visible: Boolean = true,
    val version: Int,
    /** El JSON libre del elemento, tal cual lo guarda el servidor. */
    val data: JsonObject = JsonObject(emptyMap()),
) {
    fun str(key: String): String? = data[key]?.jsonPrimitive?.contentOrNull
    fun int(key: String): Int? = data[key]?.jsonPrimitive?.intOrNull
    fun num(key: String): Float? = data[key]?.jsonPrimitive?.floatOrNull
    fun bool(key: String): Boolean? = data[key]?.jsonPrimitive?.booleanOrNull

    val text: String? get() = str("text")
    val shape: String? get() = str("shape")
    val imageId: String? get() = str("imageId")
}

data class Board(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val theme: String,
    val version: Int,
    val elements: List<BoardElement> = emptyList(),
)

/** Lo que cambia de un elemento en una edición. `null` = no se toca. */
data class ElementPatch(
    val x: Float? = null,
    val y: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    val rotation: Float? = null,
    val locked: Boolean? = null,
    val visible: Boolean? = null,
    val data: JsonObject? = null,
)

@Singleton
class VisionBoardRepository @Inject constructor(
    private val api: VisionBoardApi,
    private val snapshot: VisionBoardSnapshot,
    @ApplicationContext private val context: Context,
) {

    private companion object {
        /**
         * A lo que se reduce una imagen antes de subirla. El backend acepta
         * 23 MB, pero una foto de móvil moderna los roza ella sola y el lienzo
         * jamás la muestra a más de unos cientos de puntos: subir el original
         * gasta datos del usuario para nada.
         */
        const val MAX_UPLOAD_EDGE = 1600
        const val JPEG_QUALITY = 88

        /**
         * A cuánto se reduce una imagen AL DECODIFICARLA para el lienzo.
         *
         * Medido en el teléfono: con 1280 px, un tablero de diecinueve fotos
         * dejaba 155 MB en el heap nativo —cada bitmap son ancho×alto×4
         * bytes— cuando en pantalla ninguna de esas fotos pasa de unos 450 px
         * con el tablero encuadrado.
         *
         * 640 px deja cada bitmap en ~1,6 MB, así que un tablero de veinte
         * fotos cabe ENTERO en la caché sin que ninguna tenga que expulsar a
         * otra — que era el problema de verdad: con el techo justo, abrir el
         * tablero decodificaba, expulsaba y volvía a decodificar las mismas
         * imágenes una y otra vez. Sigue por encima de lo que el lienzo
         * dibuja, con margen para acercarse.
         */
        const val MAX_DECODE_EDGE = 640

        /**
         * Techo de la caché de imágenes, en bytes.
         *
         * Sin techo, un usuario que abre cuatro tableros con fotos acumula
         * las de los cuatro hasta que el sistema mata el proceso. Se sueltan
         * las menos usadas, que es justo lo que no se está mirando.
         *
         * El techo tiene que dar para UN tablero completo con holgura: si no,
         * las imágenes de la misma composición se expulsan entre ellas y cada
         * repintado vuelve a decodificar. 80 MB son unas cincuenta fotos al
         * tamaño al que se decodifican.
         */
        const val IMAGE_CACHE_BUDGET = 80L * 1024 * 1024
    }

    /**
     * Las imágenes ya decodificadas, para no volver a pedirlas al desplazar.
     *
     * `LinkedHashMap` en modo acceso: la más antigua SIN USAR queda la
     * primera, y es la que se suelta cuando se pasa del presupuesto.
     */
    private val imageCache = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean {
            if (bytesOf() <= IMAGE_CACHE_BUDGET) return false
            return size > 1
        }

        private fun bytesOf(): Long = values.sumOf { it.width.toLong() * it.height.toLong() * 4L }
    }

    /* ---------------- tableros ---------------- */

    /** Todos los tableros del usuario. El listado NO trae los elementos. */
    suspend fun boards(): Result<List<Board>> = runCatching {
        api.list().items.map { it.toDomain() }
    }

    suspend fun board(id: String): Result<Board> = runCatching {
        val dto = api.get(id)
        snapshot.save(dto)
        dto.toDomain()
    }

    /**
     * Devuelve el tablero con el que abrir, creándolo la primera vez.
     *
     * Se conserva la red de seguridad de siempre —si no hay ninguno se crea
     * uno— porque una sección de tablero sin tablero no tiene nada que
     * enseñar. Lo que cambia es que ya no se queda con el primero para
     * siempre: ahora hay una lista y se puede cambiar.
     */
    /** Lo último que se vio, leído del disco. Pinta antes que la red. */
    suspend fun lastKnownBoard(): Board? = snapshot.read()?.toDomain()

    /**
     * Devuelve el tablero con el que abrir, creándolo la primera vez.
     *
     * UN SOLO VIAJE cuando ya se ha abierto antes: si hay copia local se va
     * derecho a `GET /vision-boards/{id}` con ese id, sin pedir la lista
     * primero. Pedirla era un viaje entero de red para acabar eligiendo el
     * mismo tablero de siempre. Si ese id ya no existe —borrado desde otro
     * sitio— se cae con elegancia a la lista, que es el camino de antes.
     */
    suspend fun openingBoard(): Result<Board> = runCatching {
        val remembered = snapshot.read()?.id
        if (remembered != null) {
            val direct = runCatching { api.get(remembered) }.getOrNull()
            if (direct != null) {
                snapshot.save(direct)
                return@runCatching direct.toDomain()
            }
            snapshot.clear()
        }
        val existing = api.list().items.firstOrNull()
        val id = existing?.id ?: api.create(
            CreateVisionBoardRequest(name = "Mi tablero", width = 1200, height = 800, theme = "LIGHT"),
        ).id
        val full = api.get(id)
        snapshot.save(full)
        full.toDomain()
    }

    suspend fun createBoard(name: String, theme: String): Result<Board> = runCatching {
        val created = api.create(CreateVisionBoardRequest(name = name, width = 1200, height = 800, theme = theme))
        api.get(created.id).toDomain()
    }

    suspend fun updateBoardTheme(board: Board, theme: String): Result<Board> = runCatching {
        api.updateBoard(board.id, UpdateVisionBoardRequest(theme = theme, version = board.version)).toDomain()
    }

    suspend fun renameBoard(board: Board, name: String): Result<Board> = runCatching {
        api.updateBoard(board.id, UpdateVisionBoardRequest(name = name, version = board.version)).toDomain()
    }

    suspend fun deleteBoard(board: Board): Result<Unit> = runCatching {
        api.deleteBoard(board.id)
        // La copia local apunta a algo que ya no existe: se tira, o la
        // siguiente apertura intentaría abrir un tablero borrado.
        snapshot.clear()
    }

    /* ---------------- elementos ---------------- */

    suspend fun addElement(
        boardId: String,
        type: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        data: JsonObject,
    ): Result<BoardElement> = runCatching {
        api.addElement(
            boardId,
            CreateBoardElementRequest(
                type = type,
                x = x.toDouble(), y = y.toDouble(),
                width = width.toDouble(), height = height.toDouble(),
                data = data,
            ),
        ).toDomain()
    }

    /**
     * Sube una imagen del dispositivo y devuelve su id en el almacén.
     *
     * `uri` puede venir del portapapeles (una imagen copiada en Chrome) o del
     * selector de fotos: en ambos casos es un `content://` que el
     * `ContentResolver` sabe abrir, sin permisos de almacenamiento.
     */
    suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        val (bytes, mime) = withContext(Dispatchers.IO) { readAndShrink(uri) }
        val part = MultipartBody.Part.createFormData(
            "file",
            "board-image.${if (mime == "image/png") "png" else "jpg"}",
            bytes.toRequestBody(mime.toMediaTypeOrNull()),
        )
        api.uploadImage(part).id
    }

    /**
     * Sube una de las fotografías que vienen empaquetadas con la aplicación
     * (las de las plantillas del portal, en `assets/board-templates`).
     *
     * Van por el MISMO endpoint que una foto de la galería: la plantilla no
     * reparte imágenes ajenas, le da a cada usuario su propia copia. Por eso
     * la plantilla sigue funcionando aunque se borre el tablero del que
     * salió.
     */
    suspend fun uploadTemplateAsset(fileName: String): Result<String> = runCatching {
        val bytes = withContext(Dispatchers.IO) {
            context.assets.open("board-templates/$fileName").use { it.readBytes() }
        }
        val mime = if (fileName.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
        val part = MultipartBody.Part.createFormData(
            "file", fileName, bytes.toRequestBody(mime.toMediaTypeOrNull()),
        )
        val id = api.uploadImage(part).id
        // LOS BYTES YA ESTÁN AQUÍ: se decodifican y se siembran en la caché en
        // vez de volver a pedirle al servidor la misma imagen que le acabamos
        // de mandar. Eran DIECINUEVE descargas por plantilla, todas de algo que
        // ya teníamos en la mano.
        withContext(Dispatchers.IO) { decodeSampled(bytes) }?.let { imageCache[id] = it }
        id
    }

    /**
     * Decodifica reduciendo de entrada.
     *
     * Una plantilla son hasta diecinueve fotos a la vez: a resolución completa
     * son decenas de MB de bitmaps vivos, y el lienzo no las enseña ni a la
     * mitad de eso. `inSampleSize` hace el recorte al decodificar, que es
     * donde no cuesta nada.
     */
    private fun decodeSampled(bytes: ByteArray, maxEdge: Int = MAX_DECODE_EDGE): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }

    /** La proporción real de la imagen, para que el elemento entre con su forma. */
    fun imageAspect(uri: Uri): Float? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
    }

    suspend fun updateElement(boardId: String, element: BoardElement, patch: ElementPatch): Result<BoardElement> =
        runCatching {
            api.updateElement(
                boardId,
                element.id,
                UpdateBoardElementRequest(
                    x = patch.x?.toDouble(),
                    y = patch.y?.toDouble(),
                    width = patch.width?.toDouble(),
                    height = patch.height?.toDouble(),
                    rotation = patch.rotation?.toDouble(),
                    locked = patch.locked,
                    visible = patch.visible,
                    data = patch.data,
                    version = element.version,
                ),
            ).toDomain()
        }

    /** Capas: devuelve TODOS los elementos renumerados por el servidor. */
    suspend fun reorder(boardId: String, element: BoardElement, direction: String): Result<List<BoardElement>> =
        runCatching {
            api.reorderElement(boardId, element.id, ReorderElementRequest(direction, element.version))
                .map { it.toDomain() }
        }

    suspend fun deleteElement(boardId: String, element: BoardElement): Result<Unit> = runCatching {
        api.deleteElement(boardId, element.id)
    }

    /** Un elemento recién pedido al servidor, para reconciliar tras un conflicto. */
    suspend fun refreshElement(boardId: String, elementId: String): BoardElement? =
        runCatching { api.get(boardId).elements.firstOrNull { it.id == elementId }?.toDomain() }.getOrNull()

    /* ---------------- imágenes ---------------- */

    /**
     * Todas las imágenes del tablero, en UN viaje.
     *
     * Antes se pedía una por una: veinte fotos eran veinte idas y vueltas, y
     * con 300 ms de latencia eso son seis segundos que no bajan por mucho que
     * se encojan las imágenes. Ahora el servidor las manda juntas y aquí solo
     * queda decodificarlas, que se hace en paralelo y sin red de por medio.
     *
     * Devuelve `null` si el servidor no conoce el endpoint todavía —una
     * versión anterior a este cambio— y entonces el que llama usa el camino
     * de siempre, imagen a imagen. Nada se rompe por desplegar el móvil
     * antes que el backend.
     */
    suspend fun allImages(boardId: String, sizes: Map<String, Float>): Map<String, ImageBitmap>? {
        val bundle = runCatching { api.boardImages(boardId) }.getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            bundle.items.mapNotNull { item ->
                val bytes = runCatching { Base64.decode(item.dataBase64, Base64.DEFAULT) }.getOrNull()
                    ?: return@mapNotNull null
                val edge = sizes[item.id]?.let { (it * 2f).toInt().coerceIn(120, MAX_DECODE_EDGE) } ?: MAX_DECODE_EDGE
                decodeSampled(bytes, edge)?.let { bitmap ->
                    imageCache[item.id] = bitmap
                    item.id to bitmap
                }
            }.toMap()
        }
    }

    /** Lo que ya está decodificado, sin ir a la red. */
    fun cached(imageId: String): ImageBitmap? = imageCache[imageId]

    /**
     * Los bytes del servidor, decodificados a LO QUE ESE ELEMENTO MIDE.
     *
     * `targetEdge` es el borde mayor del elemento en puntos del tablero. Con
     * el tablero encuadrado en un teléfono, un punto del tablero es más o
     * menos un píxel de pantalla, así que decodificar por encima de eso es
     * gastar memoria en detalle que nadie ve. Se dobla para que acercarse un
     * paso siga viéndose nítido, y se topa en `MAX_DECODE_EDGE`.
     *
     * Antes todas se decodificaban al mismo tamaño, la grande y la pequeña:
     * una foto de 175 puntos ocupaba en memoria lo mismo que una de 1600.
     *
     * Si la misma imagen se pide luego más grande —está en dos elementos de
     * distinto tamaño— se vuelve a decodificar y se queda la mayor.
     */
    suspend fun image(imageId: String, targetEdge: Float? = null): ImageBitmap? {
        val wanted = targetEdge?.let { (it * 2f).toInt().coerceIn(120, MAX_DECODE_EDGE) } ?: MAX_DECODE_EDGE
        imageCache[imageId]?.let { cached ->
            if (maxOf(cached.width, cached.height) >= wanted * 0.75f) return cached
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                api.imageBytes(imageId).use { body -> decodeSampled(body.bytes(), wanted) }
            }
        }.getOrNull()?.also { imageCache[imageId] = it }
    }

    /**
     * La imagen del portapapeles, si la hay.
     *
     * Chrome deja una URI `content://` al usar «Copiar imagen»; otras
     * aplicaciones dejan solo texto, y entonces no hay nada que pegar aquí.
     * Se comprueba el tipo MIME real del recorte en vez de fiarse de que la
     * URI parezca una imagen.
     */
    fun clipboardImage(): Uri? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri ?: continue
            val type = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            if (type?.startsWith("image/") == true) return uri
        }
        return null
    }

    /**
     * Lee la imagen y la reduce si hace falta.
     *
     * Se decodifica primero solo el encabezado (`inJustDecodeBounds`) para
     * saber su tamaño sin cargarla entera en memoria — una foto de 50 MP
     * cargada a pelo tumba el proceso antes de llegar a redimensionarla.
     */
    private fun readAndShrink(uri: Uri): Pair<ByteArray, String> {
        val resolver = context.contentResolver
        val declared = resolver.getType(uri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longestEdge / sample > MAX_UPLOAD_EDGE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo leer la imagen")

        // PNG se conserva en PNG para no perder la transparencia; el resto sale
        // como JPEG, que es lo que el backend admite y ocupa mucho menos.
        val png = declared == "image/png"
        val out = ByteArrayOutputStream()
        bitmap.compress(
            if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
            JPEG_QUALITY,
            out,
        )
        bitmap.recycle()
        return out.toByteArray() to if (png) "image/png" else "image/jpeg"
    }
}

/** Atajo para construir un `data` de una sola clave de texto. */
fun jsonOf(key: String, value: String): JsonObject = buildJsonObject { put(key, value) }

private fun VisionBoardDto.toDomain() = Board(
    id = id,
    name = name,
    width = width,
    height = height,
    theme = theme,
    version = version,
    elements = elements.filter { it.visible }.sortedBy { it.zIndex }.map { it.toDomain() },
)

private fun BoardElementDto.toDomain() = BoardElement(
    id = id,
    type = type,
    x = x.toFloat(),
    y = y.toFloat(),
    width = width.toFloat(),
    height = height.toFloat(),
    rotation = rotation.toFloat(),
    zIndex = zIndex,
    locked = locked,
    visible = visible,
    version = version,
    data = data,
)
