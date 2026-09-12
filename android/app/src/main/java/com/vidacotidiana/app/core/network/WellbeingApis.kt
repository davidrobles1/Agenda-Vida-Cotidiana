package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * LAS TRES CAPACIDADES QUE EL ARTEFACTO PIDIÓ Y EL BACKEND YA EXPONE.
 *
 * Ánimo del día (V36), pasos de una tarea (V34) y progreso de un hábito (V35).
 * Cada ruta, verbo y nombre de campo está transcrito del controlador Java
 * correspondiente — `MoodController`, `ReminderStepController` y
 * `RoutineProgressController`—, no supuesto.
 *
 * Tres detalles que son fáciles de equivocar y que aquí están comprobados:
 *
 *  · El ánimo es un UPSERT y devuelve 200, no 201: marcar dos veces el mismo
 *    día corrige, no apila.
 *  · Marcar y desmarcar un paso es el MISMO endpoint (`/toggle`), porque en el
 *    artefacto es el mismo gesto.
 *  · `execute` y `progress` de una rutina son cosas distintas y conviven: uno
 *    cierra la ocurrencia y mueve la fecha, el otro suma dentro del día. Aquí
 *    solo vive `progress`; `execute` sigue en `LaboralApi`.
 */

/* ══════════════════════════════════════════════════════════════════════════
   ÁNIMO DEL DÍA — dato de salud
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * NO trae `ownerUserId`, al contrario que el resto de respuestas.
 *
 * No es asimetría por descuido: el único que puede leerlo es su dueño, así que
 * el identificador no aporta nada y sí amplía lo que viaja y acaba en un log.
 */
@Serializable
data class MoodDto(
    val id: String,
    /** ISO-8601, día sin hora: el ánimo es de un día, no de un instante. */
    val date: String,
    /** 0 genial · 1 bien · 2 normal · 3 regular · 4 bajo. Escala cerrada. */
    val value: Int,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val version: Int = 0,
)

@Serializable
data class UpsertMoodRequest(
    /** Ausente = hoy, que es el gesto normal desde Inicio. */
    val date: String? = null,
    val value: Int,
    val note: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class DeletedCountDto(val deleted: Int = 0)

interface MoodApi {

    /** Sin parámetros, el backend devuelve los últimos 30 días. */
    @GET("moods")
    suspend fun list(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<MoodDto>

    @GET("moods/{date}")
    suspend fun get(@Path("date") date: String): MoodDto

    /** Upsert por (usuario, día). Idempotente. */
    @POST("moods")
    suspend fun upsert(@Body request: UpsertMoodRequest): MoodDto

    /**
     * Borrado DURO de todo el historial (Ajustes → Privacidad).
     *
     * Devuelve cuántos registros cayeron, para poder decir «se borraron 21» en
     * vez de un «listo» que no prueba nada.
     */
    @DELETE("moods")
    suspend fun deleteAll(): DeletedCountDto
}

/* ══════════════════════════════════════════════════════════════════════════
   PASOS DE UNA TAREA
   ══════════════════════════════════════════════════════════════════════════ */

@Serializable
data class StepDto(
    val id: String,
    val reminderId: String,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0,
)

@Serializable
data class StepTitleRequest(val title: String)

/** La lista COMPLETA de ids en su orden nuevo, no un «de aquí a allá». */
@Serializable
data class StepReorderRequest(val orderedIds: List<String>)

@Serializable
data class PercentDto(val percent: Int = 0)

interface ReminderStepApi {

    @GET("reminders/{id}/steps")
    suspend fun list(@Path("id") reminderId: String): List<StepDto>

    @POST("reminders/{id}/steps")
    suspend fun add(@Path("id") reminderId: String, @Body request: StepTitleRequest): StepDto

    /** Marcar y desmarcar: un solo gesto, un solo endpoint. */
    @POST("reminders/{id}/steps/{stepId}/toggle")
    suspend fun toggle(@Path("id") reminderId: String, @Path("stepId") stepId: String): StepDto

    @PATCH("reminders/{id}/steps/{stepId}")
    suspend fun rename(
        @Path("id") reminderId: String,
        @Path("stepId") stepId: String,
        @Body request: StepTitleRequest,
    ): StepDto

    @DELETE("reminders/{id}/steps/{stepId}")
    suspend fun delete(@Path("id") reminderId: String, @Path("stepId") stepId: String)

    @POST("reminders/{id}/steps/reorder")
    suspend fun reorder(@Path("id") reminderId: String, @Body request: StepReorderRequest): List<StepDto>

    /**
     * El avance DERIVADO que calcula el servidor.
     *
     * Android NO guarda su propio porcentaje: lo pide o lo deduce de la lista
     * de pasos que ya tiene. Dos fuentes para la misma cifra acaban discrepando.
     */
    @GET("reminders/{id}/steps/percent")
    suspend fun percent(@Path("id") reminderId: String): PercentDto
}

/* ══════════════════════════════════════════════════════════════════════════
   PROGRESO DE UN HÁBITO
   ══════════════════════════════════════════════════════════════════════════ */

@Serializable
data class RoutineProgressDto(
    val id: String,
    val routineId: String,
    val date: String,
    val count: Int = 0,
)

@Serializable
data class RoutineProgressRequest(
    val date: String? = null,
    /** Ausente = 1, que es tocar el anillo una vez. */
    val delta: Int? = null,
    /** Solo para el PUT, que fija el valor exacto del día. */
    val value: Int? = null,
)

@Serializable
data class RoutineTargetRequest(
    /** `null` retira la meta y devuelve la rutina a sí/no, sin perder histórico. */
    val targetCount: Int? = null,
    val unit: String? = null,
)

@Serializable
data class RoutineTargetDto(
    val routineId: String,
    val targetCount: Int? = null,
    val unit: String? = null,
    val counted: Boolean = false,
)

interface RoutineProgressApi {

    /** Sumar al día. Sin cuerpo, suma uno. NO ejecuta la rutina. */
    @POST("routines/{id}/progress")
    suspend fun add(
        @Path("id") routineId: String,
        @Body request: RoutineProgressRequest = RoutineProgressRequest(),
    ): RoutineProgressDto

    /** Fijar el valor exacto de un día, para corregir sin ir de uno en uno. */
    @PUT("routines/{id}/progress")
    suspend fun set(@Path("id") routineId: String, @Body request: RoutineProgressRequest): RoutineProgressDto

    @GET("routines/{id}/progress")
    suspend fun list(
        @Path("id") routineId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<RoutineProgressDto>

    @PUT("routines/{id}/target")
    suspend fun setTarget(@Path("id") routineId: String, @Body request: RoutineTargetRequest): RoutineTargetDto
}
