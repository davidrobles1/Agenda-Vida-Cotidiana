package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Vision Board — tableros, elementos e imágenes, contra los servicios reales
 * (`/api/v1/vision-boards` y `/api/v1/vision-board-images`).
 *
 * El contrato de una imagen es el que fijó la Web y que el backend documenta:
 * el archivo se SUBE aparte y el elemento guarda `data.imageId`, nunca la URL
 * ni el archivo original. Así una imagen copiada desde Chrome deja de depender
 * de que ese enlace siga vivo, y el permiso de lectura temporal de la URI del
 * portapapeles deja de importar en cuanto se ha subido.
 */

@Serializable
data class VisionBoardDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val width: Int = 1200,
    val height: Int = 800,
    val theme: String = "LIGHT",
    val version: Int = 0,
    val elements: List<BoardElementDto> = emptyList(),
)

@Serializable
data class BoardElementDto(
    val id: String,
    val boardId: String,
    val type: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val rotation: Double = 0.0,
    val zIndex: Int = 0,
    val locked: Boolean = false,
    val visible: Boolean = true,
    val data: JsonObject = JsonObject(emptyMap()),
    val version: Int = 0,
)

@Serializable
data class CreateVisionBoardRequest(
    val name: String,
    val description: String? = null,
    val width: Int,
    val height: Int,
    val theme: String? = null,
)

@Serializable
data class CreateBoardElementRequest(
    val type: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val data: JsonObject = JsonObject(emptyMap()),
)

/**
 * Todos los campos son opcionales salvo `version` (bloqueo optimista).
 *
 * `rotation`, `zIndex`, `locked` y `visible` estaban en el contrato del
 * backend desde el principio (ver `UpdateElementRequest.java`) y este cliente
 * no los enviaba: por eso girar, bloquear u ocultar no existían en el móvil
 * aunque el servidor supiera guardarlos.
 */
@Serializable
data class UpdateBoardElementRequest(
    val x: Double? = null,
    val y: Double? = null,
    val width: Double? = null,
    val height: Double? = null,
    val rotation: Double? = null,
    val zIndex: Int? = null,
    val locked: Boolean? = null,
    val visible: Boolean? = null,
    val data: JsonObject? = null,
    val version: Int,
)

/** Actualización parcial del tablero — también es cómo se guarda el tema. */
@Serializable
data class UpdateVisionBoardRequest(
    val name: String? = null,
    val description: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val theme: String? = null,
    val version: Int,
)

/** `direction`: FRONT · BACK · RAISE · LOWER. */
@Serializable
data class ReorderElementRequest(
    val direction: String,
    val version: Int,
)

@Serializable
data class VisionBoardImageDto(val id: String, val contentType: String, val sizeBytes: Long)

/** Todas las imágenes de un tablero, en una sola respuesta. */
@Serializable
data class BoardImageBundleDto(val items: List<BoardImageItemDto> = emptyList())

@Serializable
data class BoardImageItemDto(val id: String, val contentType: String, val dataBase64: String)

interface VisionBoardApi {
    @GET("vision-boards")
    suspend fun list(@Query("size") size: Int = 20): Page<VisionBoardDto>

    /** Solo esta variante trae los elementos; el listado no. */
    @GET("vision-boards/{id}")
    suspend fun get(@Path("id") id: String): VisionBoardDto

    @POST("vision-boards")
    suspend fun create(@Body request: CreateVisionBoardRequest): VisionBoardDto

    @PUT("vision-boards/{id}")
    suspend fun updateBoard(@Path("id") id: String, @Body request: UpdateVisionBoardRequest): VisionBoardDto

    @DELETE("vision-boards/{id}")
    suspend fun deleteBoard(@Path("id") id: String)

    /**
     * TODAS las imágenes del tablero de una vez.
     *
     * Pedirlas una a una costaba una ida y vuelta por imagen: con veinte
     * fotos y una conexión normal, segundos enteros que no dependen del peso
     * sino de cuántas veces se pregunta. Es lo que hacía que fueran
     * apareciendo de una en una.
     */
    @GET("vision-boards/{id}/images")
    suspend fun boardImages(
        @Path("id") boardId: String,
        @Query("variant") variant: String = "thumb",
    ): BoardImageBundleDto

    @POST("vision-boards/{id}/elements")
    suspend fun addElement(@Path("id") boardId: String, @Body request: CreateBoardElementRequest): BoardElementDto

    @PUT("vision-boards/{id}/elements/{elementId}")
    suspend fun updateElement(
        @Path("id") boardId: String,
        @Path("elementId") elementId: String,
        @Body request: UpdateBoardElementRequest,
    ): BoardElementDto

    @DELETE("vision-boards/{id}/elements/{elementId}")
    suspend fun deleteElement(@Path("id") boardId: String, @Path("elementId") elementId: String)

    /**
     * Capas. Devuelve TODOS los elementos afectados —el backend renumera la
     * secuencia entera de `zIndex` en una sola operación— para no tener que
     * recargar el tablero después de subir o bajar uno.
     */
    @POST("vision-boards/{id}/elements/{elementId}/reorder")
    suspend fun reorderElement(
        @Path("id") boardId: String,
        @Path("elementId") elementId: String,
        @Body request: ReorderElementRequest,
    ): List<BoardElementDto>

    /** El backend acepta PNG, JPEG, WEBP y GIF, hasta 23 MB. */
    @Multipart
    @POST("vision-board-images")
    suspend fun uploadImage(@Part file: MultipartBody.Part): VisionBoardImageDto

    /**
     * Devuelve los bytes crudos: se decodifican en el cliente, sin librería.
     *
     * `variant=thumb` pide la rendición reducida que el servidor guarda al
     * subir (borde mayor 800 px). El lienzo nunca dibuja una foto a más que
     * eso, así que bajar el original era traerse megas para tirarlos al
     * decodificar. Si esa imagen no tiene miniatura el servidor devuelve el
     * original, así que se puede pedir siempre sin preguntar antes.
     */
    @Streaming
    @GET("vision-board-images/{id}")
    suspend fun imageBytes(
        @Path("id") id: String,
        @Query("variant") variant: String? = "thumb",
    ): ResponseBody
}
