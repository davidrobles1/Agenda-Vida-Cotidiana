package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import okhttp3.ResponseBody

/**
 * Los servicios REALES que ya expone el backend y que la Web consume desde
 * hace tiempo. Android deja de leer `MockData` y pasa por aquí.
 *
 * Nada de esto inventa un endpoint: cada ruta, verbo y nombre de parámetro se
 * transcribió del controlador Java correspondiente. Los dos detalles que es
 * fácil equivocar y que aquí están comprobados:
 *
 *  - Notas del día filtra por `?date=`, no por `?noteDate=`.
 *  - Sus escrituras son PUT (`/{id}/position`, `/{id}/data`), no PATCH.
 *
 * Los listados devuelven `PageResponse<T>` y el backend limita `size` a 100.
 */

/** El sobre de paginación común a todos los listados. */
@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

/** Tope real del backend (`Math.min(size, 100)`): pedir más no trae más. */
const val MAX_PAGE_SIZE = 100

/* --------------------------------------------------------------------------
   Garantías
   -------------------------------------------------------------------------- */
@Serializable
data class WarrantyDto(
    val id: String,
    val item: String,
    val expiresAt: String,
    val status: String,
    val version: Int = 0,
    val documentContentType: String? = null,
    val context: String? = null,
    val inventoryItemId: String? = null,
)

/** Cuerpo mínimo de las acciones que solo necesitan bloqueo optimista. */
@Serializable
data class VersionRequest(val version: Int)

@Serializable
data class UpdateWarrantyRequest(val item: String? = null, val expiresAt: String? = null, val version: Int)

interface WarrantyApi {
    @GET("warranties")
    suspend fun list(
        @Query("context") context: String? = null,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): Page<WarrantyDto>

    @PATCH("warranties/{id}")
    suspend fun update(@Path("id") id: String, @Body request: UpdateWarrantyRequest): WarrantyDto

    /** Marca la garantía como usada. Es el `complete` que ya expone el backend. */
    @POST("warranties/{id}/complete")
    suspend fun complete(@Path("id") id: String, @Body request: VersionRequest): WarrantyDto

    @DELETE("warranties/{id}")
    suspend fun delete(@Path("id") id: String)

    /**
     * Una garantía SIEMPRE lleva su comprobante: el backend la crea por
     * multipart y exige el archivo. No es un extra opcional — una garantía sin
     * documento no sirve para reclamar nada, y por eso el contrato lo pide.
     *
     * Los campos van como parámetros de consulta porque el controlador los lee
     * con `@RequestParam`; solo el archivo viaja como parte del multipart.
     */
    @Multipart
    @POST("warranties")
    suspend fun create(
        @Part file: MultipartBody.Part,
        @Query("item") item: String,
        @Query("expiresAt") expiresAt: String,
        /** Obligatorio: el backend rechaza el alta sin artículo (2026-09-06). */
        @Query("inventoryItemId") inventoryItemId: String,
        @Query("context") context: String? = null,
    ): WarrantyDto
}

/* --------------------------------------------------------------------------
   Mantenimiento
   -------------------------------------------------------------------------- */
@Serializable
data class MaintenanceDto(
    val id: String,
    val item: String,
    val nextDueAt: String,
    val intervalMonths: Int? = null,
    val status: String,
    val version: Int = 0,
    val context: String? = null,
    /** V31: artículo al que se le hace. Nulo = sin enlazar. */
    val inventoryItemId: String? = null,
)

@Serializable
data class CreateMaintenanceRequest(
    val item: String,
    val nextDueAt: String,
    val intervalMonths: Int? = null,
    val context: String? = null,
    /** V31: opcional, a diferencia del de la garantía. */
    val inventoryItemId: String? = null,
)

interface MaintenanceApi {
    @GET("maintenance-records")
    suspend fun list(
        @Query("context") context: String? = null,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): Page<MaintenanceDto>

    @POST("maintenance-records")
    suspend fun create(@Body request: CreateMaintenanceRequest): MaintenanceDto

    @PATCH("maintenance-records/{id}")
    suspend fun update(@Path("id") id: String, @Body request: UpdateMaintenanceRequest): MaintenanceDto

    /**
     * ADR-021: completar AVANZA la ocurrencia, no cierra el registro.
     *
     * OJO CON LA RUTA. Esto apuntaba a `POST /{id}/complete`, que es el
     * contrato ANTIGUO y solo invierte un booleano ACTIVE/COMPLETED: la fecha
     * no se movía, no se escribía historial, y como el cliente traduce
     * COMPLETED a «Al día» el usuario marcaba «Hecho» y la pantalla se quedaba
     * exactamente igual. El comentario ya decía «avanza la ocurrencia» — la
     * ruta no.
     *
     * `/occurrences` es el endpoint de ADR-021: avanza `nextDueAt` según el
     * intervalo, escribe la entrada de historial y es idempotente por fecha
     * programada. La nota es opcional; aquí no se manda ninguna porque la
     * pantalla no la pide todavía.
     */
    @POST("maintenance-records/{id}/occurrences")
    suspend fun completeOccurrence(
        @Path("id") id: String,
        @Body request: CompleteOccurrenceRequest = CompleteOccurrenceRequest(),
    ): MaintenanceDto

    /** Deshace la última ejecución y devuelve el registro a su fecha anterior. */
    @DELETE("maintenance-records/{id}/occurrences/last")
    suspend fun undoLastOccurrence(@Path("id") id: String): MaintenanceDto

    @DELETE("maintenance-records/{id}")
    suspend fun delete(@Path("id") id: String)
}

/** El cuerpo de `/occurrences`. La nota es opcional (`@Size(max = 500)`). */
@Serializable
data class CompleteOccurrenceRequest(val note: String? = null)

@Serializable
data class UpdateMaintenanceRequest(
    val item: String? = null,
    val nextDueAt: String? = null,
    val intervalMonths: Int? = null,
    val version: Int,
    val inventoryItemId: String? = null,
    /** V31: sin esta bandera, `null` sería "no tocar" y desenlazar imposible. */
    val linkInventoryItem: Boolean? = null,
)

/* --------------------------------------------------------------------------
   Pagos (ADR-020). Los importes viven solo en esta sección.
   -------------------------------------------------------------------------- */
@Serializable
data class SubscriptionDto(
    val id: String,
    val service: String,
    val company: String? = null,
    val plan: String? = null,
    val nextPaymentDate: String,
    val billingCycle: String? = null,
    val version: Int = 0,
    val context: String? = null,
    val kind: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val variableAmount: Boolean = false,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val institution: String? = null,
    val lastFour: String? = null,
    val totalInstallments: Int? = null,
    val currentInstallment: Int? = null,
)

/**
 * ADR-020: solo los campos que un alta necesita. El resto del contrato
 * (día de corte, plazos, institución…) pertenece a los tipos CARD y CREDIT y
 * se edita después; pedirlo todo en el alta convertiría un gesto de treinta
 * segundos en un formulario de veinte campos.
 */
@Serializable
data class CreateSubscriptionRequest(
    val service: String,
    val nextPaymentDate: String,
    val billingCycle: String,
    val company: String? = null,
    val context: String? = null,
    val kind: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
)

/**
 * Un ciclo ya pagado (ADR-020(f)).
 *
 * `periodDate` es QUÉ ciclo cubre; `paidOn`, CUÁNDO lo marcó el usuario. La
 * distinción importa y no es cosmética — ver `isPaidThisPeriod` en el
 * repositorio.
 */
@Serializable
data class PaymentRecordDto(
    val id: String,
    val subscriptionId: String,
    val periodDate: String,
    val paidOn: String,
)

interface SubscriptionApi {
    @GET("subscriptions")
    suspend fun list(
        @Query("context") context: String? = null,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): Page<SubscriptionDto>

    @POST("subscriptions")
    suspend fun create(@Body request: CreateSubscriptionRequest): SubscriptionDto

    @PATCH("subscriptions/{id}")
    suspend fun update(@Path("id") id: String, @Body request: UpdateSubscriptionRequest): SubscriptionDto

    /** Registrar el pago del ciclo: el «ya lo pagué» que ya existe. */
    @POST("subscriptions/{id}/payments")
    suspend fun registerPayment(@Path("id") id: String, @Body request: VersionRequest): SubscriptionDto

    /**
     * Deshacer el último pago registrado: borra su `PaymentRecord` y devuelve
     * la fecha de cobro al ciclo anterior. Registrar un pago por error adelanta
     * la fecha un mes entero, así que sin esto la única salida era editar la
     * fecha a mano — y eso deja el registro del pago inventado en el historial.
     */
    @DELETE("subscriptions/{id}/payments/last")
    suspend fun undoLastPayment(@Path("id") id: String): SubscriptionDto

    /**
     * ADR-020(f): todos los ciclos ya pagados. EN BLOQUE y no por compromiso —
     * pedirlo uno por uno sería una consulta por fila. Es lo que permite saber
     * qué está pagado ya, y sin ello el total del mes no puede distinguir lo
     * cubierto de lo pendiente.
     */
    @GET("subscriptions/payments")
    suspend fun paymentRecords(): List<PaymentRecordDto>

    @DELETE("subscriptions/{id}")
    suspend fun delete(@Path("id") id: String)
}

@Serializable
data class UpdateSubscriptionRequest(
    val service: String? = null,
    val nextPaymentDate: String? = null,
    val billingCycle: String? = null,
    val amount: Double? = null,
    val version: Int,
)

/* --------------------------------------------------------------------------
   Inventario
   -------------------------------------------------------------------------- */
@Serializable
data class InventoryItemDto(
    val id: String,
    val name: String,
    val category: String,
    val location: String? = null,
    val context: String? = null,
    val version: Int = 0,
)

@Serializable
data class CreateInventoryItemRequest(
    val name: String,
    val category: String,
    val location: String? = null,
)

interface InventoryApi {
    @GET("inventory-items")
    suspend fun list(
        @Query("context") context: String? = null,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): Page<InventoryItemDto>

    /**
     * ADR-022: el módulo va como parámetro de CONSULTA, no en el cuerpo — el
     * controlador lo lee con `@RequestParam`. Mandarlo dentro del JSON no
     * fallaba: se ignoraba en silencio y el artículo se creaba en Personal.
     */
    @POST("inventory-items")
    suspend fun create(
        @Body request: CreateInventoryItemRequest,
        @Query("context") context: String? = null,
    ): InventoryItemDto

    @PATCH("inventory-items/{id}")
    suspend fun update(@Path("id") id: String, @Body request: UpdateInventoryItemRequest): InventoryItemDto

    @DELETE("inventory-items/{id}")
    suspend fun delete(@Path("id") id: String)
}

@Serializable
data class UpdateInventoryItemRequest(
    val name: String? = null,
    val category: String? = null,
    val location: String? = null,
    val version: Int,
)

/* --------------------------------------------------------------------------
   Documentos
   -------------------------------------------------------------------------- */
@Serializable
data class DocumentDto(
    val id: String,
    val name: String,
    val category: String,
    val contentType: String,
    val sizeBytes: Long = 0,
    val visibility: String,
    val sharedWithEmail: String? = null,
    val context: String? = null,
    /**
     * V37 — de qué recurso cuelga este documento, si cuelga de alguno.
     *
     * Nulos = documento suelto, que es el caso de todos los anteriores a esa
     * migración. Es la mecánica ÚNICA de adjuntos: garantías, mantenimientos,
     * tareas, artículos y pagos pasan por aquí, no por un almacén propio.
     */
    val resourceType: String? = null,
    val resourceId: String? = null,
    val version: Int = 0,
    val createdAt: String? = null,
)

/** Colgar de un recurso — o soltar, con los dos nulos. No borra el documento. */
@Serializable
data class LinkDocumentRequest(
    val resourceType: String? = null,
    val resourceId: String? = null,
)

/**
 * `ids` vacío o ausente significa "todos los del contexto" — es el contrato del
 * backend (`DownloadDocumentsRequest`), pensado para que "Descargar todos" no
 * dependa de que el cliente enumere una lista paginada.
 */
@Serializable
data class DownloadDocumentsRequest(
    val ids: List<String>? = null,
    val context: String? = null,
)

interface DocumentApi {
    @GET("documents")
    suspend fun list(
        @Query("context") context: String? = null,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): Page<DocumentDto>

    /**
     * Los adjuntos de UN recurso. Devuelve lista, no página: un registro tiene
     * unos pocos adjuntos, y paginarlos sería pedir una segunda vuelta por algo
     * que cabe entero.
     */
    @GET("documents/attachments")
    suspend fun attachments(
        @Query("resourceType") resourceType: String,
        @Query("resourceId") resourceId: String,
    ): List<DocumentDto>

    /** Colgar o soltar. Endpoint propio: enganchar no es editar el documento. */
    @POST("documents/{id}/link")
    suspend fun link(@Path("id") id: String, @Body request: LinkDocumentRequest): DocumentDto

    /** Multipart, como garantías: un documento ES su archivo. */
    @Multipart
    @POST("documents")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Query("name") name: String,
        @Query("category") category: String,
        @Query("context") context: String? = null,
    ): DocumentDto

    @PATCH("documents/{id}")
    suspend fun update(@Path("id") id: String, @Body request: UpdateDocumentRequest): DocumentDto

    /** Los bytes del documento, para abrirlo o compartirlo con otra app. */
    @Streaming
    @GET("documents/{id}/content")
    suspend fun content(@Path("id") id: String): ResponseBody

    /**
     * Varios documentos en un zip. POST y no GET porque la selección viaja en
     * el cuerpo: unas decenas de ids en la query se topan con el límite de
     * longitud de URL. Para UNO solo se sigue usando `content`.
     */
    @Streaming
    @POST("documents/download")
    suspend fun downloadZip(@Body request: DownloadDocumentsRequest): ResponseBody

    /** ADR-025: compartir por correo con alguien concreto. */
    @POST("documents/{id}/share")
    suspend fun share(@Path("id") id: String, @Body request: ShareDocumentRequest): DocumentDto

    @POST("documents/{id}/make-public")
    suspend fun makePublic(@Path("id") id: String, @Body request: VersionRequest): DocumentDto

    @POST("documents/{id}/make-private")
    suspend fun makePrivate(@Path("id") id: String, @Body request: VersionRequest): DocumentDto

    @DELETE("documents/{id}")
    suspend fun delete(@Path("id") id: String)
}

@Serializable
data class UpdateDocumentRequest(
    val name: String? = null,
    val category: String? = null,
    val version: Int,
)

@Serializable
data class ShareDocumentRequest(val email: String, val version: Int)

/* --------------------------------------------------------------------------
   Familia y recursos compartidos (ADR-025)
   -------------------------------------------------------------------------- */
@Serializable
data class FamilyMemberDto(val userId: String, val username: String, val since: String? = null)

@Serializable
data class FamilyInvitationDto(
    val id: String,
    val counterpartUserId: String? = null,
    val counterpartUsername: String? = null,
    val status: String,
    val createdAt: String? = null,
)

/** `relation` dice si ya es familia, si hay invitación en curso, o ninguna. */
@Serializable
data class UserSearchDto(val userId: String, val username: String, val relation: String)

@Serializable
data class ResourceShareDto(
    val id: String,
    val resourceType: String,
    val resourceId: String,
    val resourceLabel: String,
    val resourceDate: String? = null,
    val counterpartUserId: String? = null,
    val counterpartUsername: String? = null,
    val responsibility: Boolean = false,
    val partDoneAt: String? = null,
    val version: Int = 0,
)

@Serializable
data class CreateFamilyInvitationRequest(val userId: String)

interface FamilyApi {
    /** ADR-025 §1: el backend exige 5 caracteres; la UI no consulta antes. */
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserSearchDto>

    @GET("family/members")
    suspend fun members(): List<FamilyMemberDto>

    @GET("family/invitations/received")
    suspend fun receivedInvitations(): List<FamilyInvitationDto>

    @GET("family/invitations/sent")
    suspend fun sentInvitations(): List<FamilyInvitationDto>

    @POST("family/invitations")
    suspend fun invite(@Body request: CreateFamilyInvitationRequest): FamilyInvitationDto

    @POST("family/invitations/{id}/accept")
    suspend fun acceptInvitation(@Path("id") id: String)

    @POST("family/invitations/{id}/reject")
    suspend fun rejectInvitation(@Path("id") id: String)

    @GET("shared-resources/received")
    suspend fun sharedWithMe(): List<ResourceShareDto>

    @GET("shared-resources/sent")
    suspend fun sharedByMe(): List<ResourceShareDto>

    /** «Ya hice mi parte» — DEC-001: vive en la fila de compartición. */
    @POST("shared-resources/shares/{shareId}/part-done")
    suspend fun markPartDone(@Path("shareId") shareId: String): ResourceShareDto

    @DELETE("shared-resources/shares/{shareId}/part-done")
    suspend fun undoPartDone(@Path("shareId") shareId: String): ResourceShareDto
}

/* --------------------------------------------------------------------------
   Laboral (ADR-016): personas, proyectos, seguimientos e inbox de notas
   -------------------------------------------------------------------------- */
@Serializable
data class PersonDto(
    val id: String,
    val name: String,
    val role: String? = null,
    val organization: String? = null,
    val version: Int = 0,
)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val clientPersonId: String? = null,
    // Opcional en el contrato: `components.schemas.Project` solo exige
    // [id, name, version], y `ProjectResponse` es `@JsonInclude(NON_NULL)`,
    // así que un proyecto creado sin estado llega SIN la clave `status`.
    // Declararlo no nulable rompía la deserialización y el proyecto recién
    // creado se volvía invisible.
    val status: String? = null,
    val deadline: String? = null,
    val version: Int = 0,
)

@Serializable
data class CommitmentDto(
    val id: String,
    val personId: String? = null,
    val projectId: String? = null,
    val description: String,
    val direction: String,
    val dueAt: String,
    val status: String,
    val version: Int = 0,
)

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val personId: String? = null,
    val projectId: String? = null,
    val taskSuggestionResolved: Boolean = false,
    val version: Int = 0,
)

@Serializable
data class CreatePersonRequest(val name: String, val role: String? = null, val organization: String? = null)

@Serializable
data class CreateProjectRequest(val name: String, val status: String? = null, val deadline: String? = null)

/** `personId` es obligatorio: un seguimiento sin contraparte no es seguimiento. */
@Serializable
data class CreateCommitmentRequest(
    val personId: String,
    val description: String,
    val direction: String,
    val dueAt: String,
    val projectId: String? = null,
)

@Serializable
data class CreateNoteRequest(val title: String, val description: String? = null)

/* --------------------------------------------------------------------------
   Objetivos (ADR-016 Fase 3e1 / FR-031)
   -------------------------------------------------------------------------- */

/**
 * Un objetivo.
 *
 * `targetValue` y `deadline` pueden VENIR AUSENTES: el backend serializa con
 * `@JsonInclude(NON_NULL)`. `currentValue` y `completed` son primitivos allí,
 * así que siempre llegan.
 *
 * REGLA DEL DOMINIO (AC-018), que este cliente no debe romper: `completed` y
 * `currentValue` son INDEPENDIENTES. Llegar a la meta no cumple el objetivo, y
 * cumplirlo no toca el progreso. Ninguna de las dos cosas se deriva de la otra.
 */
@Serializable
data class ObjectiveDto(
    val id: String,
    val title: String,
    val targetValue: Int? = null,
    val currentValue: Int = 0,
    val deadline: String? = null,
    val completed: Boolean = false,
    val version: Int = 0,
)

/** Solo `title` es obligatorio. No acepta `completed`: nace siempre en curso. */
@Serializable
data class CreateObjectiveRequest(
    val title: String,
    val targetValue: Int? = null,
    val currentValue: Int? = null,
    val deadline: String? = null,
)

/**
 * Edición parcial. `version` es obligatoria.
 *
 * Con `encodeDefaults = false` (ver `NetworkModule`), los campos que se dejan
 * en su valor por defecto NO viajan: marcar cumplido manda literalmente
 * `{"completed":true,"version":N}` y el resto del objetivo queda intacto. Es lo
 * que permite que cumplir y reabrir usen este mismo PATCH — no existe
 * `/objectives/{id}/complete`.
 */
@Serializable
data class UpdateObjectiveRequest(
    val title: String? = null,
    val targetValue: Int? = null,
    val currentValue: Int? = null,
    val deadline: String? = null,
    val completed: Boolean? = null,
    val version: Int,
)

/* --------------------------------------------------------------------------
   Rutinas (ADR-016 Fase 3e2 / FR-032)
   -------------------------------------------------------------------------- */

/**
 * Una rutina.
 *
 * NO tiene `completed`, y es deliberado (FR-032): la misma rutina se hace una y
 * otra vez, así que su estado permanente es `active` y la ocurrencia en curso se
 * expresa avanzando `nextExecutionDate`.
 *
 * Una rutina NUNCA genera una tarea ni un seguimiento —decisión explícita del
 * Product Owner— y no hay planificador detrás: ejecutarla solo mueve su fecha.
 */
@Serializable
data class RoutineDto(
    val id: String,
    val title: String,
    val description: String? = null,
    /** DAILY | WEEKLY | MONTHLY. Los tres únicos valores aprobados. */
    val frequency: String,
    val nextExecutionDate: String,
    val active: Boolean = true,
    /**
     * V35 — meta diaria del hábito y su unidad. Nulos = rutina de sí/no, que es
     * como se comportan todas las anteriores a esa migración. Es lo que
     * distingue «Sacar la basura» (hecha o no) de «Agua: 4 de 8».
     */
    val targetCount: Int? = null,
    val unit: String? = null,
    val version: Int = 0,
)

/** Los tres campos obligatorios son reales: `@NotBlank` + dos `@NotNull`. */
@Serializable
data class CreateRoutineRequest(
    val title: String,
    val description: String? = null,
    val frequency: String,
    val nextExecutionDate: String,
)

/**
 * Edición parcial. `version` obligatoria.
 *
 * `active` SE EDITA POR AQUÍ: pausar y reanudar usan este mismo PATCH —
 * verificado de extremo a extremo (DTO → controlador → `Routine#applyEdit`)—,
 * así que no hace falta ningún endpoint nuevo.
 *
 * No lleva `completed`: no existe en el dominio.
 */
@Serializable
data class UpdateRoutineRequest(
    val title: String? = null,
    val description: String? = null,
    val frequency: String? = null,
    val nextExecutionDate: String? = null,
    val active: Boolean? = null,
    val version: Int,
)

/* --------------------------------------------------------------------------
   Recursos de trabajo (ADR-016 Fase 3e4 / FR-034)

   Se llaman `WorkResource` y no `Resource` A PROPÓSITO: en este cliente
   "resource" ya significa "cualquier cosa creable" (`CreatableResource`,
   `ResourceEntry`, `ResourceListScreen`) y además existe `SharedResource`.
   Un tercer significado del mismo nombre haría el código ilegible. Para el
   usuario la sección se sigue llamando «Recursos», igual que en la Web.
   -------------------------------------------------------------------------- */

/**
 * Un recurso de trabajo: metadatos y una referencia de TEXTO. Nunca un archivo
 * — los documentos reales viven en Documentos (FR-030), y este módulo no
 * almacena nada.
 *
 * `reference` es un único campo libre (DECISION del Product Owner, 2026-08-28):
 * una URL, la ruta de una carpeta compartida o cualquier puntero textual.
 * Varios tipos aprobados (MANUAL, PLANTILLA, HERRAMIENTA) no tienen URL, y
 * partir el campo les dejaría una columna vacía para siempre.
 *
 * `personId` y `projectId` son opcionales y NO son excluyentes: el backend los
 * valida con dos comprobaciones independientes, sin ninguna regla que obligue
 * a elegir. Un recurso sin ninguno de los dos es válido.
 */
@Serializable
data class WorkResourceDto(
    val id: String,
    val name: String,
    /** DOCUMENTO | ENLACE | PLANTILLA | MANUAL | HERRAMIENTA | OTRO. */
    val type: String,
    val reference: String? = null,
    val description: String? = null,
    val personId: String? = null,
    val projectId: String? = null,
    val version: Int = 0,
)

/** Solo `name` y `type` son obligatorios (`@NotBlank` + `@NotNull`). */
@Serializable
data class CreateWorkResourceRequest(
    val name: String,
    val type: String,
    val reference: String? = null,
    val description: String? = null,
    val personId: String? = null,
    val projectId: String? = null,
)

/** Edición parcial; `version` obligatoria. Los seis campos son editables. */
@Serializable
data class UpdateWorkResourceRequest(
    val name: String? = null,
    val type: String? = null,
    val reference: String? = null,
    val description: String? = null,
    val personId: String? = null,
    val projectId: String? = null,
    val version: Int,
)

/* --------------------------------------------------------------------------
   Lugares (ADR-016 Fase 3e3 / FR-033)
   -------------------------------------------------------------------------- */

/**
 * Un lugar guardado.
 *
 * CATÁLOGO, NO RELACIÓN. La entidad del backend lo declara: *"A catalogue
 * entry, not a relation: FR-033 explicitly keeps `reminders.place_id` out of
 * scope"*. Ninguna tabla referencia a un lugar, y por eso elegir uno al crear
 * una tarea en la Web solo COPIA su texto al campo `location` de siempre.
 *
 * NO TIENE COORDENADAS. Ni latitud, ni longitud, ni nada geográfico: un lugar
 * es un nombre y, si acaso, una dirección escrita a mano.
 *
 * `personId` es opcional — "la oficina de ACME". No hay relación con Proyecto:
 * el backend no la tiene y no se inventa.
 */
@Serializable
data class PlaceDto(
    val id: String,
    val name: String,
    val address: String? = null,
    val personId: String? = null,
    val version: Int = 0,
)

/** Solo `name` es obligatorio (`@NotBlank`, máximo 200). */
@Serializable
data class CreatePlaceRequest(
    val name: String,
    val address: String? = null,
    val personId: String? = null,
)

/** Edición parcial; `version` obligatoria. `null` = no tocar (pendiente #4). */
@Serializable
data class UpdatePlaceRequest(
    val name: String? = null,
    val address: String? = null,
    val personId: String? = null,
    val version: Int,
)

/**
 * V32: una persona participando en un proyecto. No trae la persona: el cliente
 * ya tiene la lista cargada y resolver el nombre por su lado evita que una
 * copia quede vieja cuando la persona se renombre.
 */
@Serializable
data class ProjectParticipantDto(
    val id: String,
    val projectId: String,
    val personId: String,
    val role: String,
)

interface LaboralApi {
    @GET("people")
    suspend fun people(@Query("size") size: Int = MAX_PAGE_SIZE): Page<PersonDto>

    /** V32. Devuelve una lista, no una página: son un puñado por proyecto. */
    @GET("projects/participations")
    suspend fun participations(): List<ProjectParticipantDto>

    /* ---- Objetivos. Van en LaboralApi y no en una interfaz aparte: son otro
       recurso del mismo módulo, y separarlos multiplicaría el cableado de Hilt
       sin ganar nada. Objetivo NO acepta `?context=`: su backend no conoce
       ModuleContext. ---- */

    @GET("objectives")
    suspend fun objectives(@Query("size") size: Int = MAX_PAGE_SIZE): Page<ObjectiveDto>

    @POST("objectives")
    suspend fun createObjective(@Body request: CreateObjectiveRequest): ObjectiveDto

    /** Cumplir, reabrir y editar son ESTE mismo endpoint: no existe `/complete`. */
    @PATCH("objectives/{id}")
    suspend fun updateObjective(@Path("id") id: String, @Body request: UpdateObjectiveRequest): ObjectiveDto

    @DELETE("objectives/{id}")
    suspend fun deleteObjective(@Path("id") id: String)

    /* ---- Rutinas. Mismo criterio que Objetivos: van en LaboralApi. Tampoco
       aceptan `?context=`. ---- */

    @GET("routines")
    suspend fun routines(@Query("size") size: Int = MAX_PAGE_SIZE): Page<RoutineDto>

    @POST("routines")
    suspend fun createRoutine(@Body request: CreateRoutineRequest): RoutineDto

    /** Editar, y también pausar/reanudar: `active` viaja por aquí. */
    @PATCH("routines/{id}")
    suspend fun updateRoutine(@Path("id") id: String, @Body request: UpdateRoutineRequest): RoutineDto

    /**
     * «Hecha»: registra UNA ocurrencia y avanza la fecha desde la PROGRAMADA,
     * no desde hoy. Endpoint propio — esto NO es un PATCH.
     *
     * El cliente no calcula la fecha siguiente en ninguna parte: la manda el
     * servidor en la respuesta. Es lo que hace que MONTHLY con un 31 de enero
     * caiga en el 28 de febrero y no en una fecha inválida.
     */
    @POST("routines/{id}/execute")
    suspend fun executeRoutine(@Path("id") id: String, @Body request: VersionRequest): RoutineDto

    @DELETE("routines/{id}")
    suspend fun deleteRoutine(@Path("id") id: String)

    /* ---- Recursos de trabajo. Mismo criterio que Objetivos y Rutinas: van en
       LaboralApi y no aceptan `?context=`.

       NO hay endpoint de acción: un recurso no se completa, ni se ejecuta, ni
       se resuelve. Solo CRUD. ---- */

    @GET("resources")
    suspend fun workResources(@Query("size") size: Int = MAX_PAGE_SIZE): Page<WorkResourceDto>

    @POST("resources")
    suspend fun createWorkResource(@Body request: CreateWorkResourceRequest): WorkResourceDto

    @PATCH("resources/{id}")
    suspend fun updateWorkResource(
        @Path("id") id: String,
        @Body request: UpdateWorkResourceRequest,
    ): WorkResourceDto

    @DELETE("resources/{id}")
    suspend fun deleteWorkResource(@Path("id") id: String)

    /* ---- Lugares. Mismo criterio que los tres anteriores: en LaboralApi, sin
       `?context=`. Solo CRUD: un lugar no se completa ni se ejecuta. ---- */

    @GET("places")
    suspend fun places(@Query("size") size: Int = MAX_PAGE_SIZE): Page<PlaceDto>

    @POST("places")
    suspend fun createPlace(@Body request: CreatePlaceRequest): PlaceDto

    @PATCH("places/{id}")
    suspend fun updatePlace(@Path("id") id: String, @Body request: UpdatePlaceRequest): PlaceDto

    /** Seguro siempre: ninguna tabla referencia a `places`. */
    @DELETE("places/{id}")
    suspend fun deletePlace(@Path("id") id: String)

    @GET("projects")
    suspend fun projects(@Query("size") size: Int = MAX_PAGE_SIZE): Page<ProjectDto>

    @GET("commitments")
    suspend fun commitments(@Query("size") size: Int = MAX_PAGE_SIZE): Page<CommitmentDto>

    @GET("notes")
    suspend fun notes(@Query("size") size: Int = MAX_PAGE_SIZE): Page<NoteDto>

    @POST("people")
    suspend fun createPerson(@Body request: CreatePersonRequest): PersonDto

    @POST("projects")
    suspend fun createProject(@Body request: CreateProjectRequest): ProjectDto

    @POST("commitments")
    suspend fun createCommitment(@Body request: CreateCommitmentRequest): CommitmentDto

    @POST("notes")
    suspend fun createNote(@Body request: CreateNoteRequest): NoteDto

    @PATCH("people/{id}")
    suspend fun updatePerson(@Path("id") id: String, @Body request: UpdatePersonRequest): PersonDto

    @PATCH("projects/{id}")
    suspend fun updateProject(@Path("id") id: String, @Body request: UpdateProjectRequest): ProjectDto

    @PATCH("commitments/{id}")
    suspend fun updateCommitment(@Path("id") id: String, @Body request: UpdateCommitmentRequest): CommitmentDto

    /** Cerrar un seguimiento. El backend lo llama `resolve`, no `complete`. */
    @POST("commitments/{id}/resolve")
    suspend fun resolveCommitment(@Path("id") id: String, @Body request: VersionRequest): CommitmentDto

    /** Reabrirlo: se borra la resolución, que es el recurso que creó el POST. */
    @DELETE("commitments/{id}/resolve")
    suspend fun reopenCommitment(@Path("id") id: String): CommitmentDto

    @PATCH("notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body request: UpdateNoteRequest): NoteDto

    @DELETE("people/{id}")
    suspend fun deletePerson(@Path("id") id: String)

    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") id: String)

    @DELETE("commitments/{id}")
    suspend fun deleteCommitment(@Path("id") id: String)

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String)
}

@Serializable
data class UpdatePersonRequest(
    val name: String? = null,
    val role: String? = null,
    val organization: String? = null,
    val version: Int,
)

@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val status: String? = null,
    val deadline: String? = null,
    val version: Int,
)

@Serializable
data class UpdateCommitmentRequest(
    val description: String? = null,
    val direction: String? = null,
    val dueAt: String? = null,
    val personId: String? = null,
    val version: Int,
)

@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val description: String? = null,
    val version: Int,
)

/* --------------------------------------------------------------------------
   Notas del día — el servicio REAL que la Web ya usa (`/api/v1/day-notes`).
   -------------------------------------------------------------------------- */

/**
 * `data` es un objeto JSON libre: la Web guarda ahí `text` (String) junto a
 * `bold`/`italic` (Boolean) y `font`/`shape` (String). Se modela como
 * `JsonObject` y no como `Map<String, String>` justamente por eso — una nota
 * escrita en la Web lleva booleanos, y un mapa de cadenas fallaría al
 * deserializarla. Android lee `text`, y al editar reenvía el objeto completo
 * con `text` sustituido, de modo que el formato de la Web sobrevive intacto.
 */
@Serializable
data class DayNoteElementDto(
    val id: String,
    val noteDate: String,
    val type: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val zIndex: Int = 0,
    val data: JsonObject = JsonObject(emptyMap()),
    val version: Int = 0,
    val context: String? = null,
)

@Serializable
data class CreateDayNoteRequest(
    val noteDate: String,
    val type: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val data: JsonObject,
    val context: String? = null,
)

@Serializable
data class UpdateDayNoteDataRequest(val data: JsonObject, val version: Int)

@Serializable
data class MoveDayNoteRequest(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val version: Int,
)

interface DayNoteApi {
    @GET("day-notes")
    suspend fun listForDay(
        @Query("date") date: String,
        @Query("context") context: String? = null,
    ): List<DayNoteElementDto>

    @POST("day-notes")
    suspend fun create(@Body request: CreateDayNoteRequest): DayNoteElementDto

    @PUT("day-notes/{id}/data")
    suspend fun editData(@Path("id") id: String, @Body request: UpdateDayNoteDataRequest): DayNoteElementDto

    @PUT("day-notes/{id}/position")
    suspend fun move(@Path("id") id: String, @Body request: MoveDayNoteRequest): DayNoteElementDto

    @DELETE("day-notes/{id}")
    suspend fun delete(@Path("id") id: String)
}
