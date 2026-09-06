package com.vidacotidiana.app.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Un archivo elegido por el usuario, ya leído y listo para subir.
 *
 * Garantías y documentos son multipart en el backend porque el archivo ES el
 * recurso: una garantía sin comprobante no sirve para reclamar nada. Se lee
 * completo a memoria a propósito — el backend tiene su propio límite de
 * tamaño y responde 413, que es un error claro para el usuario; sostener el
 * `InputStream` abierto mientras dura la petición, en cambio, se rompe si el
 * proveedor del `content://` revoca el permiso a mitad de la subida.
 */
data class FilePayload(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
) {
    fun toPart(): MultipartBody.Part = MultipartBody.Part.createFormData(
        "file",
        fileName,
        bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
    )

    // `ByteArray` compara por identidad, y esta clase viaja dentro de estados
    // de Compose que sí comparan por igualdad. Sin esto, dos lecturas del
    // mismo archivo se considerarían distintas y recompondrían de más.
    override fun equals(other: Any?): Boolean =
        this === other || (other is FilePayload && fileName == other.fileName && mimeType == other.mimeType && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * (31 * bytes.contentHashCode() + fileName.hashCode()) + mimeType.hashCode()
}

/** Lee un `content://` del selector del sistema. No necesita permisos. */
@Singleton
class FileReader @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun read(uri: Uri): Result<FilePayload> = runCatching {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("No se pudo abrir el archivo")

            // El nombre real del archivo, cuando el proveedor lo publica: es lo
            // que el usuario reconoce después en la lista de documentos.
            var name = "archivo"
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst() && cursor.columnCount > 0) {
                        cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                }
            }

            FilePayload(
                bytes = bytes,
                fileName = name,
                mimeType = resolver.getType(uri) ?: "application/octet-stream",
            )
        }
    }
}
