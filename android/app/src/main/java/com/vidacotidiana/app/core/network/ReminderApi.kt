package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Aligned with components.schemas.Reminder in Documentacion/openapi/openapi.yaml. */
@Serializable
data class Reminder(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val description: String? = null,
    val dueAt: String? = null,
    val status: String,
    val version: Int,
)

@Serializable
data class RemindersPage(
    val items: List<Reminder>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
)

@Serializable
data class CreateReminderRequest(
    val title: String,
    val dueAt: String? = null,
    // El endpoint ya aceptaba descripción; el cliente simplemente no la
    // mandaba, así que lo escrito en «Detalle» se perdía al guardar.
    val description: String? = null,
)

@Serializable
data class CompleteReminderRequest(val version: Int? = null)

interface ReminderApi {
    @GET("reminders")
    suspend fun listReminders(): RemindersPage

    @POST("reminders")
    suspend fun createReminder(@Body request: CreateReminderRequest): Reminder

    @POST("reminders/{id}/complete")
    suspend fun completeReminder(@Path("id") id: String, @Body request: CompleteReminderRequest): Reminder

    /**
     * Edición parcial: solo viajan los campos que cambian, y `version` es
     * obligatoria (bloqueo optimista). Es el mismo endpoint que usa la Web
     * para editar un recordatorio; no se inventa ninguno nuevo.
     */
    @PATCH("reminders/{id}")
    suspend fun updateReminder(@Path("id") id: String, @Body request: UpdateReminderRequest): Reminder
}

@Serializable
data class UpdateReminderRequest(
    val title: String? = null,
    val description: String? = null,
    val dueAt: String? = null,
    val version: Int,
)
