package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Aligned with components.schemas.Reminder in Documentacion/openapi/openapi.yaml. */
@Serializable
data class Reminder(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val description: String? = null,
    val dueAt: String? = null,
    val status: String,
    /**
     * ADR-016/FR-024. TEXTO LIBRE, no coordenadas — el backend lo declara así
     * («Nullable free text») y lo guarda en un `VARCHAR(500)` sin ninguna
     * validación de formato.
     *
     * El backend ya lo devolvía; este cliente simplemente no lo declaraba, y
     * como el `Json` está configurado con `ignoreUnknownKeys = true`, se
     * descartaba en silencio: una tarea creada en la Web con ubicación la
     * perdía de vista al abrirla en el móvil.
     */
    val location: String? = null,
    /**
     * V33: `LOW` | `NORMAL` | `URGENT`, nunca nulo en el backend.
     *
     * El artefacto pone la prioridad como ANTETÍTULO de cada tarea —«ALTA»,
     * «MEDIA», «BAJA»— y le da su color, que es lo que permite ojear la lista
     * sin leerla. El backend lo devuelve desde V33; este cliente no lo
     * declaraba, así que `ignoreUnknownKeys` lo descartaba en silencio y la
     * lista salía toda del mismo color.
     */
    val priority: String? = null,
    /**
     * EL AVANCE DE SUS PASOS — de dónde sale el anillo del artefacto.
     *
     * El diseño aprobado pone un anillo con el porcentaje en CADA fila de
     * Tareas. Derivarlo exigía pedir los pasos de cada tarea, una petición por
     * fila, así que la lista no lo tenía. El backend los resuelve ahora con una
     * agregación por página.
     *
     * NULO no es cero: `null` es «no se consultó» y cero es «no tiene pasos».
     * Sin esa distinción, una tarea sin pasos y una tarea sin mirar pintarían
     * el mismo anillo vacío.
     */
    val stepCount: Int? = null,
    val stepsDone: Int? = null,
    /**
     * SE LEEN PARA PODER DEVOLVERLOS, no para mostrarlos.
     *
     * `Reminder#applyEdit` en el backend los aplica SIEMPRE tal como llegan
     * —a diferencia de título, fecha y descripción, donde nulo significa "no
     * tocar"—, así que omitirlos en un PATCH los BORRA. Sin leerlos aquí, este
     * cliente no tendría qué reenviar y editar una tarea desde el móvil
     * borraría el icono y la pegatina puestos desde la Web.
     *
     * Es el mismo contrato que ya cumple la Web, cuyo `UpdateReminderInput`
     * documenta: "The caller always resubmits the reminder's complete current
     * icon/sticker selection".
     */
    val iconId: String? = null,
    val stickerId: String? = null,
    /**
     * El backend SIEMPRE lo envía; este cliente no lo declaraba y se descartaba
     * en silencio por `ignoreUnknownKeys`. Se declara ahora porque «hechas esta
     * semana» necesita saber CUÁNDO se completó una tarea: por fecha de
     * vencimiento saldría mal, ya que una tarea vencida hace un mes y cerrada
     * ayer se contaría en el mes pasado.
     */
    val updatedAt: String? = null,
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
    /**
     * ADR-019: el módulo al que pertenece la tarea. SIN ESTE CAMPO el backend
     * la crea en PERSONAL (`ModuleContext.fromNullable`), así que una tarea
     * creada desde Laboral acababa en Personal y desaparecía de donde el
     * usuario la había escrito.
     */
    val context: String? = null,
    /** ADR-016/FR-024: dónde es. Texto libre y opcional, máximo 500. */
    val location: String? = null,
)

@Serializable
data class CompleteReminderRequest(val version: Int? = null)

interface ReminderApi {
    /**
     * ADR-019: `context` FILTRA por módulo. Sin él el backend devuelve todo
     * mezclado —que es lo que necesita el Calendario general del Portal, pero
     * no Personal ni Laboral—, así que en Laboral se veían también las tareas
     * personales y al revés.
     *
     * `size` también faltaba: sin él la página era de 20 y las tareas a partir
     * de la vigésima simplemente no existían para la aplicación.
     */
    @GET("reminders")
    suspend fun listReminders(
        @Query("context") context: String? = null,
        @Query("size") size: Int = 100,
    ): RemindersPage

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

/**
 * Edición parcial, con DOS semánticas distintas que el backend define y este
 * cliente debe respetar:
 *
 *  - `title`, `description`, `dueAt` y `location`: nulo = **no tocar**. No se
 *    puede vaciar una ubicación ya guardada, y el propio backend lo declara
 *    como "an accepted limitation for V3".
 *  - `iconId` y `stickerId`: se aplican **siempre tal como llegan**, así que
 *    omitirlos los BORRA. Por eso quien edita debe reenviar los actuales.
 */
@Serializable
data class UpdateReminderRequest(
    val title: String? = null,
    val description: String? = null,
    val dueAt: String? = null,
    val location: String? = null,
    val iconId: String? = null,
    val stickerId: String? = null,
    val version: Int,
)
