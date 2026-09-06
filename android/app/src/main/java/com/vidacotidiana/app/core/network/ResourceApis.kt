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
)

@Serializable
data class CreateMaintenanceRequest(
    val item: String,
    val nextDueAt: String,
    val intervalMonths: Int? = null,
    val context: String? = null,
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

    /** ADR-021: completar AVANZA la ocurrencia, no cierra el registro. */
    @POST("maintenance-records/{id}/complete")
    suspend fun complete(@Path("id") id: String, @Body request: VersionRequest): MaintenanceDto

    @DELETE("maintenance-records/{id}")
    suspend fun delete(@Path("id") id: String)
}

@Serializable
data class UpdateMaintenanceRequest(
    val item: String? = null,
    val nextDueAt: String? = null,
    val intervalMonths: Int? = null,
    val version: Int,
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

    @POST("inventory-items")
    suspend fun create(@Body request: CreateInventoryItemRequest): InventoryItemDto

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
    val version: Int = 0,
    val createdAt: String? = null,
)

interface DocumentApi {
    @GET("documents")
    suspend fun list(
        @Query("context") context: String? = null,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): Page<DocumentDto>

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
    val status: String,
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

interface LaboralApi {
    @GET("people")
    suspend fun people(@Query("size") size: Int = MAX_PAGE_SIZE): Page<PersonDto>

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
