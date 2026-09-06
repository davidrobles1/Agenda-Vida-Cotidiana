package com.vidacotidiana.app.core.data

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vidacotidiana.app.core.network.BoardElementDto
import com.vidacotidiana.app.core.network.CreateBoardElementRequest
import com.vidacotidiana.app.core.network.CreateVisionBoardRequest
import com.vidacotidiana.app.core.network.UpdateBoardElementRequest
import com.vidacotidiana.app.core.network.VisionBoardApi
import com.vidacotidiana.app.core.network.VisionBoardDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
 */

data class BoardElement(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val version: Int,
    val text: String? = null,
    val shape: String? = null,
    val imageId: String? = null,
)

data class Board(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val version: Int,
    val elements: List<BoardElement> = emptyList(),
)

@Singleton
class VisionBoardRepository @Inject constructor(
    private val api: VisionBoardApi,
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

        /** Tamaño con el que entra una imagen nueva al lienzo. */
        const val DEFAULT_IMAGE_SIZE = 180f
    }

    /** Las imágenes ya decodificadas, para no volver a pedirlas al desplazar. */
    private val imageCache = mutableMapOf<String, ImageBitmap>()

    /**
     * Devuelve el tablero del usuario, creándolo la primera vez.
     *
     * El producto no tiene todavía una pantalla de gestión de tableros, así que
     * aquí hay UNO: si no existe se crea, en vez de dejar la sección inservible
     * hasta que alguien cree uno desde la Web.
     */
    suspend fun loadBoard(): Result<Board> = runCatching {
        val existing = api.list().items.firstOrNull()
        val full = if (existing != null) {
            api.get(existing.id)
        } else {
            val created = api.create(
                CreateVisionBoardRequest(name = "Mi tablero", width = 1200, height = 800, theme = "LIGHT"),
            )
            api.get(created.id)
        }
        full.toDomain()
    }

    /**
     * Sube una imagen del dispositivo y la coloca en el lienzo.
     *
     * `uri` puede venir del portapapeles (una imagen copiada en Chrome) o del
     * selector de fotos: en ambos casos es un `content://` que el
     * `ContentResolver` sabe abrir, sin permisos de almacenamiento.
     */
    suspend fun addImage(boardId: String, uri: Uri, x: Float, y: Float): Result<BoardElement> = runCatching {
        val (bytes, mime) = withContext(Dispatchers.IO) { readAndShrink(uri) }
        val part = MultipartBody.Part.createFormData(
            "file",
            "board-image.${if (mime == "image/png") "png" else "jpg"}",
            bytes.toRequestBody(mime.toMediaTypeOrNull()),
        )
        val uploaded = api.uploadImage(part)
        val element = api.addElement(
            boardId,
            CreateBoardElementRequest(
                type = "IMAGE",
                x = x.toDouble(),
                y = y.toDouble(),
                width = DEFAULT_IMAGE_SIZE.toDouble(),
                height = DEFAULT_IMAGE_SIZE.toDouble(),
                // El mismo contrato que la Web: el elemento apunta al id de la
                // imagen subida, nunca al archivo ni a la URL original.
                data = buildJsonObject { put("imageId", uploaded.id) },
            ),
        )
        element.toDomain()
    }

    suspend fun addText(boardId: String, text: String, x: Float, y: Float): Result<BoardElement> = runCatching {
        api.addElement(
            boardId,
            CreateBoardElementRequest(
                type = "TEXT",
                x = x.toDouble(), y = y.toDouble(), width = 150.0, height = 60.0,
                data = buildJsonObject { put("text", text) },
            ),
        ).toDomain()
    }

    suspend fun addShape(boardId: String, shape: String, x: Float, y: Float): Result<BoardElement> = runCatching {
        api.addElement(
            boardId,
            CreateBoardElementRequest(
                type = "SHAPE",
                x = x.toDouble(), y = y.toDouble(), width = 120.0, height = 100.0,
                data = buildJsonObject { put("shape", shape) },
            ),
        ).toDomain()
    }

    /** Guarda dónde quedó un elemento tras arrastrarlo. */
    suspend fun move(boardId: String, element: BoardElement, x: Float, y: Float): Result<BoardElement> = runCatching {
        api.updateElement(
            boardId,
            element.id,
            UpdateBoardElementRequest(x = x.toDouble(), y = y.toDouble(), version = element.version),
        ).toDomain()
    }

    suspend fun delete(boardId: String, element: BoardElement): Result<Unit> = runCatching {
        api.deleteElement(boardId, element.id)
    }

    /** Los bytes del servidor, decodificados una sola vez por imagen. */
    suspend fun image(imageId: String): ImageBitmap? {
        imageCache[imageId]?.let { return it }
        return runCatching {
            withContext(Dispatchers.IO) {
                api.imageBytes(imageId).use { body ->
                    val bytes = body.bytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
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

private fun VisionBoardDto.toDomain() = Board(
    id = id,
    name = name,
    width = width,
    height = height,
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
    version = version,
    text = data.string("text"),
    shape = data.string("shape"),
    imageId = data.string("imageId"),
)

private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
