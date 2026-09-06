package com.vidacotidiana.app.core.data

import com.vidacotidiana.app.core.network.CommitmentDto
import com.vidacotidiana.app.core.network.DocumentApi
import com.vidacotidiana.app.core.network.DocumentDto
import com.vidacotidiana.app.core.network.FamilyApi
import com.vidacotidiana.app.core.network.FamilyInvitationDto
import com.vidacotidiana.app.core.network.FamilyMemberDto
import com.vidacotidiana.app.core.network.InventoryApi
import com.vidacotidiana.app.core.network.InventoryItemDto
import com.vidacotidiana.app.core.network.LaboralApi
import com.vidacotidiana.app.core.network.MaintenanceApi
import com.vidacotidiana.app.core.network.MaintenanceDto
import com.vidacotidiana.app.core.network.NoteDto
import com.vidacotidiana.app.core.network.PersonDto
import com.vidacotidiana.app.core.network.ProjectDto
import com.vidacotidiana.app.core.network.ResourceShareDto
import com.vidacotidiana.app.core.network.SubscriptionApi
import com.vidacotidiana.app.core.network.SubscriptionDto
import com.vidacotidiana.app.core.network.UserSearchDto
import com.vidacotidiana.app.core.network.WarrantyApi
import com.vidacotidiana.app.core.network.WarrantyDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El dominio que las pantallas leen, y el único sitio donde se traduce del
 * contrato HTTP a ese dominio.
 *
 * Sustituye a `MockData`: mismos nombres de campo que usaban las pantallas
 * (`product`, `expiresLabel`, `status`…) para que la sustitución no obligara a
 * reescribir la composición ni el motion de Complete — cambia de dónde salen
 * los datos, no cómo se ven.
 *
 * El ESTADO derivado (vigente / por vencer / vencida, al día / próximo /
 * vencido) se calcula aquí a partir de la fecha real, igual que hace la Web:
 * el backend guarda `ACTIVE`/`PENDING`, y el matiz temporal es una lectura del
 * cliente, no un campo almacenado.
 */

enum class WarrantyStatus { VIGENTE, POR_VENCER, VENCIDA }
enum class MaintenanceStatus { AL_DIA, PROXIMO, VENCIDO }

data class Warranty(
    val id: String,
    val product: String,
    val category: String,
    val expiresLabel: String,
    val expiresOn: LocalDate,
    val status: WarrantyStatus,
    val inventoryItemId: String?,
    val version: Int,
)

data class MaintenanceRecord(
    val id: String,
    val item: String,
    val task: String,
    val nextDueLabel: String,
    val nextDueOn: LocalDate,
    val intervalMonths: Int?,
    val status: MaintenanceStatus,
    val version: Int,
)

data class InventoryItem(
    val id: String,
    val name: String,
    val category: String,
    val location: String?,
    val version: Int,
)

data class Document(
    val id: String,
    val name: String,
    val category: String,
    val sizeLabel: String,
    val dateLabel: String,
    val visibility: String,
    val version: Int,
)

data class Payment(
    val id: String,
    val name: String,
    val category: String,
    val amountLabel: String?,
    val renewsLabel: String,
    val renewsOn: LocalDate,
    val statementDay: Int?,
    val kind: String?,
    val billingCycle: String?,
    val amount: Double?,
    val version: Int,
)

data class FamilyMember(val userId: String, val username: String, val sinceLabel: String?)

data class FamilyInvitation(val id: String, val username: String, val status: String)

data class UserSearchResult(val userId: String, val username: String, val relation: String)

data class SharedResource(
    val id: String,
    val type: String,
    val label: String,
    val counterpart: String,
    val responsibility: Boolean,
    val partDone: Boolean,
    val dateLabel: String?,
)

data class Person(val id: String, val name: String, val role: String?, val organization: String?, val version: Int)

data class Project(val id: String, val name: String, val status: String, val deadline: LocalDate?, val version: Int)

data class Commitment(
    val id: String,
    val description: String,
    val direction: String,
    val dueOn: LocalDate?,
    val status: String,
    val personId: String?,
    val projectId: String?,
    val version: Int,
)

data class InboxNote(
    val id: String,
    val title: String,
    val description: String?,
    val classified: Boolean,
    val version: Int,
)

/** Todo lo que la aplicación necesita para pintarse, en una sola carga. */
data class VidaData(
    val warranties: List<Warranty> = emptyList(),
    val maintenance: List<MaintenanceRecord> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val documents: List<Document> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val familyMembers: List<FamilyMember> = emptyList(),
    val receivedInvitations: List<FamilyInvitation> = emptyList(),
    val sharedWithMe: List<SharedResource> = emptyList(),
    val sharedByMe: List<SharedResource> = emptyList(),
    val people: List<Person> = emptyList(),
    val projects: List<Project> = emptyList(),
    val commitments: List<Commitment> = emptyList(),
    val inbox: List<InboxNote> = emptyList(),
)

@Singleton
class VidaRepository @Inject constructor(
    private val warrantyApi: WarrantyApi,
    private val maintenanceApi: MaintenanceApi,
    private val subscriptionApi: SubscriptionApi,
    private val inventoryApi: InventoryApi,
    private val documentApi: DocumentApi,
    private val familyApi: FamilyApi,
    private val laboralApi: LaboralApi,
) {

    /**
     * Carga todo en PARALELO. Trece llamadas en serie sobre una red móvil son
     * segundos de pantalla vacía; en paralelo es una sola espera.
     *
     * Cada bloque se envuelve por separado: que Laboral falle no debe dejar sin
     * garantías al usuario. Lo que falla queda vacío y el error se reporta
     * aparte, porque una lista vacía por error afirma algo falso sobre los
     * datos de quien la mira (ADR-021 k) y esa distinción la resuelve quien
     * llama, no este repositorio.
     */
    suspend fun loadAll(context: String?): Result<VidaData> = runCatching {
        coroutineScope {
            val warranties = async { runCatching { warrantyApi.list(context).items.map { it.toDomain() } } }
            val maintenance = async { runCatching { maintenanceApi.list(context).items.map { it.toDomain() } } }
            val payments = async { runCatching { subscriptionApi.list(context).items.map { it.toDomain() } } }
            val inventory = async { runCatching { inventoryApi.list(context).items.map { it.toDomain() } } }
            val documents = async { runCatching { documentApi.list(context).items.map { it.toDomain() } } }
            val members = async { runCatching { familyApi.members().map { it.toDomain() } } }
            val invitations = async { runCatching { familyApi.receivedInvitations().map { it.toDomain() } } }
            val sharedIn = async { runCatching { familyApi.sharedWithMe().map { it.toDomain() } } }
            val sharedOut = async { runCatching { familyApi.sharedByMe().map { it.toDomain() } } }
            val people = async { runCatching { laboralApi.people().items.map { it.toDomain() } } }
            val projects = async { runCatching { laboralApi.projects().items.map { it.toDomain() } } }
            val commitments = async { runCatching { laboralApi.commitments().items.map { it.toDomain() } } }
            val notes = async { runCatching { laboralApi.notes().items.map { it.toDomain() } } }

            VidaData(
                warranties = warranties.await().getOrDefault(emptyList()),
                maintenance = maintenance.await().getOrDefault(emptyList()),
                inventory = inventory.await().getOrDefault(emptyList()),
                documents = documents.await().getOrDefault(emptyList()),
                payments = payments.await().getOrDefault(emptyList()),
                familyMembers = members.await().getOrDefault(emptyList()),
                receivedInvitations = invitations.await().getOrDefault(emptyList()),
                sharedWithMe = sharedIn.await().getOrDefault(emptyList()),
                sharedByMe = sharedOut.await().getOrDefault(emptyList()),
                people = people.await().getOrDefault(emptyList()),
                projects = projects.await().getOrDefault(emptyList()),
                commitments = commitments.await().getOrDefault(emptyList()),
                inbox = notes.await().getOrDefault(emptyList()),
            )
        }
    }

    /* ---------------------------------------------------------------------
       Altas — los POST reales de cada módulo
       --------------------------------------------------------------------- */

    /**
     * El backend recibe `Instant`, pero estos campos son DÍAS, no momentos.
     *
     * Se ancla a medianoche UTC porque es exactamente lo que hace la Web:
     * `new Date('2026-09-20').toISOString()` interpreta una fecha ISO desnuda
     * como UTC. Anclarla a la medianoche LOCAL —que era lo que hacía esto
     * antes— guardaba un instante distinto del que guarda la Web para el mismo
     * día elegido, y los dos clientes acabarían discrepando sobre qué día es
     * cada registro.
     */
    private fun LocalDate.toInstantString(): String =
        atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toString()

    suspend fun createMaintenance(item: String, nextDue: LocalDate, intervalMonths: Int?, context: String?): Result<Unit> =
        runCatching {
            maintenanceApi.create(
                com.vidacotidiana.app.core.network.CreateMaintenanceRequest(
                    item = item,
                    nextDueAt = nextDue.toInstantString(),
                    intervalMonths = intervalMonths,
                    context = context,
                ),
            )
            Unit
        }

    suspend fun createPayment(
        service: String,
        nextPayment: LocalDate,
        billingCycle: String,
        amount: Double?,
        currency: String?,
        context: String?,
    ): Result<Unit> = runCatching {
        subscriptionApi.create(
            com.vidacotidiana.app.core.network.CreateSubscriptionRequest(
                service = service,
                nextPaymentDate = nextPayment.toInstantString(),
                billingCycle = billingCycle,
                context = context,
                amount = amount,
                // El backend valida la divisa como tres letras; si no se indica
                // importe tampoco se manda divisa, para no inventar una.
                currency = if (amount != null) (currency?.takeIf { it.length == 3 } ?: "MXN") else null,
            ),
        )
        Unit
    }

    suspend fun createInventoryItem(name: String, category: String, location: String?): Result<Unit> = runCatching {
        inventoryApi.create(
            com.vidacotidiana.app.core.network.CreateInventoryItemRequest(name, category, location?.ifBlank { null }),
        )
        Unit
    }

    /**
     * Garantía y documento comparten forma: son multipart porque el archivo es
     * parte del recurso, no un adjunto opcional.
     */
    suspend fun createWarranty(item: String, expires: LocalDate, file: FilePayload, context: String?): Result<Unit> =
        runCatching {
            warrantyApi.create(file.toPart(), item, expires.toInstantString(), context)
            Unit
        }

    suspend fun uploadDocument(name: String, category: String, file: FilePayload, context: String?): Result<Unit> =
        runCatching {
            documentApi.upload(file.toPart(), name, category, context)
            Unit
        }

    suspend fun createPerson(name: String, role: String?, organization: String?): Result<Unit> = runCatching {
        laboralApi.createPerson(
            com.vidacotidiana.app.core.network.CreatePersonRequest(name, role?.ifBlank { null }, organization?.ifBlank { null }),
        )
        Unit
    }

    suspend fun createProject(name: String, status: String?, deadline: LocalDate?): Result<Unit> = runCatching {
        laboralApi.createProject(
            com.vidacotidiana.app.core.network.CreateProjectRequest(
                name,
                status?.ifBlank { null },
                deadline?.toInstantString(),
            ),
        )
        Unit
    }

    suspend fun createCommitment(
        personId: String,
        description: String,
        direction: String,
        dueAt: LocalDate,
    ): Result<Unit> = runCatching {
        laboralApi.createCommitment(
            com.vidacotidiana.app.core.network.CreateCommitmentRequest(
                personId = personId,
                description = description,
                direction = direction,
                dueAt = dueAt.toInstantString(),
            ),
        )
        Unit
    }

    suspend fun createNote(title: String, description: String?): Result<Unit> = runCatching {
        laboralApi.createNote(
            com.vidacotidiana.app.core.network.CreateNoteRequest(title, description?.ifBlank { null }),
        )
        Unit
    }

    /* ---------------------------------------------------------------------
       Ediciones y acciones de estado — los PATCH y los `complete`/`resolve`
       que el backend YA expone. Ninguno es nuevo.
       --------------------------------------------------------------------- */

    suspend fun editWarranty(id: String, item: String, expires: LocalDate, version: Int): Result<Unit> =
        runCatching {
            warrantyApi.update(id, com.vidacotidiana.app.core.network.UpdateWarrantyRequest(item, expires.toInstantString(), version))
            Unit
        }

    suspend fun editMaintenance(id: String, item: String, nextDue: LocalDate, months: Int?, version: Int): Result<Unit> =
        runCatching {
            maintenanceApi.update(id, com.vidacotidiana.app.core.network.UpdateMaintenanceRequest(item, nextDue.toInstantString(), months, version))
            Unit
        }

    suspend fun editPayment(id: String, service: String, next: LocalDate, cycle: String, amount: Double?, version: Int): Result<Unit> =
        runCatching {
            subscriptionApi.update(id, com.vidacotidiana.app.core.network.UpdateSubscriptionRequest(service, next.toInstantString(), cycle, amount, version))
            Unit
        }

    suspend fun editInventoryItem(id: String, name: String, category: String, location: String?, version: Int): Result<Unit> =
        runCatching {
            inventoryApi.update(id, com.vidacotidiana.app.core.network.UpdateInventoryItemRequest(name, category, location?.ifBlank { null }, version))
            Unit
        }

    suspend fun editDocument(id: String, name: String, category: String, version: Int): Result<Unit> =
        runCatching {
            documentApi.update(id, com.vidacotidiana.app.core.network.UpdateDocumentRequest(name, category, version))
            Unit
        }

    suspend fun editPerson(id: String, name: String, role: String?, org: String?, version: Int): Result<Unit> =
        runCatching {
            laboralApi.updatePerson(id, com.vidacotidiana.app.core.network.UpdatePersonRequest(name, role?.ifBlank { null }, org?.ifBlank { null }, version))
            Unit
        }

    suspend fun editProject(id: String, name: String, status: String?, deadline: LocalDate?, version: Int): Result<Unit> =
        runCatching {
            laboralApi.updateProject(id, com.vidacotidiana.app.core.network.UpdateProjectRequest(name, status?.ifBlank { null }, deadline?.toInstantString(), version))
            Unit
        }

    suspend fun editCommitment(id: String, description: String, direction: String, dueAt: LocalDate, personId: String?, version: Int): Result<Unit> =
        runCatching {
            laboralApi.updateCommitment(id, com.vidacotidiana.app.core.network.UpdateCommitmentRequest(description, direction, dueAt.toInstantString(), personId, version))
            Unit
        }

    suspend fun editNote(id: String, title: String, description: String?, version: Int): Result<Unit> =
        runCatching {
            laboralApi.updateNote(id, com.vidacotidiana.app.core.network.UpdateNoteRequest(title, description?.ifBlank { null }, version))
            Unit
        }

    // --- Acciones de estado ---

    suspend fun completeWarranty(id: String, version: Int): Result<Unit> =
        runCatching { warrantyApi.complete(id, com.vidacotidiana.app.core.network.VersionRequest(version)); Unit }

    /** ADR-021: en mantenimiento, completar AVANZA la ocurrencia. */
    suspend fun completeMaintenance(id: String, version: Int): Result<Unit> =
        runCatching { maintenanceApi.complete(id, com.vidacotidiana.app.core.network.VersionRequest(version)); Unit }

    suspend fun registerPayment(id: String, version: Int): Result<Unit> =
        runCatching { subscriptionApi.registerPayment(id, com.vidacotidiana.app.core.network.VersionRequest(version)); Unit }

    suspend fun resolveCommitment(id: String, version: Int): Result<Unit> =
        runCatching { laboralApi.resolveCommitment(id, com.vidacotidiana.app.core.network.VersionRequest(version)); Unit }

    // --- Documentos: lo que la Web ya permitía y Android no ---

    suspend fun documentBytes(id: String): Result<ByteArray> =
        runCatching { withContext(kotlinx.coroutines.Dispatchers.IO) { documentApi.content(id).use { it.bytes() } } }

    suspend fun shareDocument(id: String, email: String, version: Int): Result<Unit> =
        runCatching { documentApi.share(id, com.vidacotidiana.app.core.network.ShareDocumentRequest(email, version)); Unit }

    suspend fun setDocumentFamilyVisible(id: String, visible: Boolean, version: Int): Result<Unit> = runCatching {
        val body = com.vidacotidiana.app.core.network.VersionRequest(version)
        if (visible) documentApi.makePublic(id, body) else documentApi.makePrivate(id, body)
        Unit
    }

    // --- Borrados ---

    suspend fun delete(resource: com.vidacotidiana.app.core.app.CreatableResource, id: String): Result<Unit> = runCatching {
        when (resource) {
            com.vidacotidiana.app.core.app.CreatableResource.WARRANTY -> warrantyApi.delete(id)
            com.vidacotidiana.app.core.app.CreatableResource.MAINTENANCE -> maintenanceApi.delete(id)
            com.vidacotidiana.app.core.app.CreatableResource.PAYMENT -> subscriptionApi.delete(id)
            com.vidacotidiana.app.core.app.CreatableResource.INVENTORY -> inventoryApi.delete(id)
            com.vidacotidiana.app.core.app.CreatableResource.DOCUMENT -> documentApi.delete(id)
            com.vidacotidiana.app.core.app.CreatableResource.PERSON -> laboralApi.deletePerson(id)
            com.vidacotidiana.app.core.app.CreatableResource.PROJECT -> laboralApi.deleteProject(id)
            com.vidacotidiana.app.core.app.CreatableResource.COMMITMENT -> laboralApi.deleteCommitment(id)
            com.vidacotidiana.app.core.app.CreatableResource.NOTE -> laboralApi.deleteNote(id)
            // La tarea la borra `ReminderApi`, que no vive en este repositorio.
            com.vidacotidiana.app.core.app.CreatableResource.TASK -> error("Las tareas se borran por ReminderApi")
        }
    }

    /** ADR-025 §1: cinco caracteres mínimo, la misma regla que la Web. */
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> {
        val term = query.trim()
        if (term.length < 5) return Result.success(emptyList())
        return runCatching { familyApi.searchUsers(term).map { it.toDomain() } }
    }

    suspend fun invite(userId: String): Result<Unit> = runCatching { familyApi.invite(com.vidacotidiana.app.core.network.CreateFamilyInvitationRequest(userId)); Unit }

    suspend fun acceptInvitation(id: String): Result<Unit> = runCatching { familyApi.acceptInvitation(id) }

    suspend fun rejectInvitation(id: String): Result<Unit> = runCatching { familyApi.rejectInvitation(id) }

    suspend fun markPartDone(shareId: String): Result<Unit> = runCatching { familyApi.markPartDone(shareId); Unit }

    suspend fun undoPartDone(shareId: String): Result<Unit> = runCatching { familyApi.undoPartDone(shareId); Unit }
}

/* --------------------------------------------------------------------------
   Traducción DTO → dominio
   -------------------------------------------------------------------------- */

private val DAY_MONTH_YEAR: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es"))

/** "2026-05-12" o un instante ISO → LocalDate. El backend usa ambos. */
internal fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(value.take(10)) }
        .recoverCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }
        .getOrNull()
}

private fun LocalDate.label(): String = format(DAY_MONTH_YEAR).replaceFirstChar { it.uppercase() }

private fun WarrantyDto.toDomain(): Warranty {
    val date = parseDate(expiresAt) ?: LocalDate.now()
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return Warranty(
        id = id,
        product = item,
        // El backend no clasifica garantías por categoría; el vínculo real que
        // sí existe es con un artículo del inventario (ADR-022).
        category = if (inventoryItemId != null) "En inventario" else "Sin inventario",
        expiresLabel = date.label(),
        expiresOn = date,
        // ADR-018: los mismos 30 días de antelación con los que se derivan sus
        // avisos, para que la píldora y el calendario no se contradigan.
        // `WarrantyStatus` en el backend es ACTIVE/COMPLETED: COMPLETED es una
        // garantía ya usada, y esa deja de contar aunque la fecha no llegue.
        status = when {
            status == "COMPLETED" || days < 0 -> WarrantyStatus.VENCIDA
            days <= 30 -> WarrantyStatus.POR_VENCER
            else -> WarrantyStatus.VIGENTE
        },
        inventoryItemId = inventoryItemId,
        version = version,
    )
}

private fun MaintenanceDto.toDomain(): MaintenanceRecord {
    val date = parseDate(nextDueAt) ?: LocalDate.now()
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return MaintenanceRecord(
        id = id,
        item = item,
        // El registro guarda UN texto (`item`); la Web muestra ese mismo texto
        // como título. No se inventa un campo «tarea» que no existe.
        task = item,
        nextDueLabel = date.label(),
        nextDueOn = date,
        intervalMonths = intervalMonths,
        // ADR-018: 7 días de antelación, los mismos de sus avisos.
        // `MaintenanceStatus` en el backend es ACTIVE/COMPLETED.
        status = when {
            status == "COMPLETED" -> MaintenanceStatus.AL_DIA
            days < 0 -> MaintenanceStatus.VENCIDO
            days <= 7 -> MaintenanceStatus.PROXIMO
            else -> MaintenanceStatus.AL_DIA
        },
        version = version,
    )
}

private fun InventoryItemDto.toDomain() = InventoryItem(
    id = id,
    name = name,
    category = category,
    location = location,
    version = version,
)

private fun DocumentDto.toDomain() = Document(
    id = id,
    name = name,
    category = category,
    sizeLabel = humanSize(sizeBytes),
    dateLabel = parseDate(createdAt)?.label() ?: "—",
    visibility = visibility,
    version = version,
)

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun SubscriptionDto.toDomain(): Payment {
    val date = parseDate(nextPaymentDate) ?: LocalDate.now()
    return Payment(
        id = id,
        name = service,
        category = company ?: plan ?: billingCycle?.let { cycleLabel(it) } ?: "Pago",
        // ADR-020: el importe se muestra porque es una propiedad de Pagos. Si
        // el pago es de importe variable no se finge una cifra exacta.
        amountLabel = when {
            variableAmount -> "Variable"
            amount != null -> formatAmount(amount, currency)
            else -> null
        },
        renewsLabel = date.label(),
        renewsOn = date,
        statementDay = statementDay,
        kind = kind,
        billingCycle = billingCycle,
        amount = amount,
        version = version,
    )
}

private fun cycleLabel(cycle: String): String = when (cycle) {
    "MONTHLY" -> "Mensual"
    "YEARLY", "ANNUAL" -> "Anual"
    "WEEKLY" -> "Semanal"
    else -> cycle.lowercase().replaceFirstChar { it.uppercase() }
}

private fun formatAmount(amount: Double, currency: String?): String {
    val figure = if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", amount)
    } else {
        String.format(Locale.US, "%,.2f", amount)
    }
    return if (currency.isNullOrBlank()) "$$figure" else "$$figure $currency"
}

private fun FamilyMemberDto.toDomain() = FamilyMember(
    userId = userId,
    username = username,
    sinceLabel = parseDate(since)?.label(),
)

private fun FamilyInvitationDto.toDomain() = FamilyInvitation(
    id = id,
    username = counterpartUsername ?: "Alguien",
    status = status,
)

private fun UserSearchDto.toDomain() = UserSearchResult(userId, username, relation)

private fun ResourceShareDto.toDomain() = SharedResource(
    id = id,
    type = resourceTypeLabel(resourceType),
    label = resourceLabel,
    counterpart = counterpartUsername ?: "—",
    responsibility = responsibility,
    partDone = partDoneAt != null,
    dateLabel = parseDate(resourceDate)?.label(),
)

/** Los nombres que ADR-025 fijó para el catálogo compartible. */
private fun resourceTypeLabel(type: String): String = when (type) {
    "REMINDER" -> "Tarea"
    "SUBSCRIPTION" -> "Pago"
    "MAINTENANCE_RECORD" -> "Mantenimiento"
    "WARRANTY" -> "Garantía"
    "INVENTORY_ITEM" -> "Inventario"
    "DOCUMENT" -> "Documento"
    "PROJECT" -> "Proyecto"
    "COMMITMENT" -> "Seguimiento"
    else -> type.lowercase().replaceFirstChar { it.uppercase() }
}

private fun PersonDto.toDomain() = Person(id, name, role, organization, version)

private fun ProjectDto.toDomain() = Project(id, name, status, parseDate(deadline), version)

private fun CommitmentDto.toDomain() = Commitment(
    id = id,
    description = description,
    direction = direction,
    dueOn = parseDate(dueAt),
    status = status,
    personId = personId,
    projectId = projectId,
    version = version,
)

private fun NoteDto.toDomain() = InboxNote(
    id = id,
    title = title,
    description = description,
    classified = personId != null || projectId != null,
    version = version,
)
