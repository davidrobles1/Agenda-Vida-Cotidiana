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

/** Todos los campos son opcionales salvo `version` (bloqueo optimista). */
@Serializable
data class UpdateBoardElementRequest(
    val x: Double? = null,
    val y: Double? = null,
    val width: Double? = null,
    val height: Double? = null,
    val data: JsonObject? = null,
    val version: Int,
)

@Serializable
data class VisionBoardImageDto(val id: String, val contentType: String, val sizeBytes: Long)

interface VisionBoardApi {
    @GET("vision-boards")
    suspend fun list(@Query("size") size: Int = 20): Page<VisionBoardDto>

    /** Solo esta variante trae los elementos; el listado no. */
    @GET("vision-boards/{id}")
    suspend fun get(@Path("id") id: String): VisionBoardDto

    @POST("vision-boards")
    suspend fun create(@Body request: CreateVisionBoardRequest): VisionBoardDto

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

    /** El backend acepta PNG, JPEG, WEBP y GIF, hasta 23 MB. */
    @Multipart
    @POST("vision-board-images")
    suspend fun uploadImage(@Part file: MultipartBody.Part): VisionBoardImageDto

    /** Devuelve los bytes crudos: se decodifican en el cliente, sin librería. */
    @Streaming
    @GET("vision-board-images/{id}")
    suspend fun imageBytes(@Path("id") id: String): ResponseBody
}
