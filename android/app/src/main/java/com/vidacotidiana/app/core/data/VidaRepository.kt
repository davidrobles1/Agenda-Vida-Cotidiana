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
/**
 * HECHO existe porque sin él un mantenimiento PUNTUAL —sin intervalo— no tenía
 * forma de decir que había terminado: el backend lo deja en COMPLETED y aquí se
 * traducía a AL_DIA, indistinguible de uno que simplemente no toca todavía. El
 * usuario marcaba «Hecho» y la fila se quedaba igual.
 *
 * Un mantenimiento REPETIBLE nunca llega a HECHO: al completarlo, el backend
 * avanza su fecha y vuelve a ACTIVE. Esa es la diferencia entre los dos, y es
 * la que el estado tenía que reflejar.
 */
enum class MaintenanceStatus { AL_DIA, PROXIMO, VENCIDO, HECHO }

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
    /** V31: artículo del inventario al que se le hace. Nulo = sin enlazar. */
    val inventoryItemId: String?,
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
    /**
     * El tamaño en crudo, además de la etiqueta ya formateada.
     *
     * `sizeLabel` sirve para pintar «240 KB» en una fila, pero no para SUMAR:
     * el total de espacio de la sección necesita el número. El DTO ya lo traía
     * y el dominio lo descartaba.
     */
    val sizeBytes: Long = 0,
    /** V37: de qué recurso cuelga, si cuelga de alguno. Nulo = suelto. */
    val resourceType: String? = null,
    val resourceId: String? = null,
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
    /** Necesaria para sumar: no se pueden sumar pesos con dólares. */
    val currency: String?,
    /** Una tarjeta de importe variable no aporta cifra hasta su corte. */
    val variableAmount: Boolean,
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

data class Project(
    val id: String,
    val name: String,
    /** Opcional: el estado es texto libre y un proyecto puede no tener ninguno. */
    val status: String?,
    val deadline: LocalDate?,
    /** El cliente. Vive aquí y NO en `participations` (DECISION 2026-09-06). */
    val clientPersonId: String?,
    val version: Int,
)

/**
 * Un objetivo (FR-031).
 *
 * Suelto por diseño: sin persona, sin proyecto y sin módulo — su backend no
 * conoce `ModuleContext`, así que no se filtra ni se le pasa contexto.
 *
 * `completed` y `currentValue` son INDEPENDIENTES (AC-018): llegar a la meta no
 * cumple el objetivo, y cumplirlo no toca el progreso. Este cliente no deriva
 * ninguno del otro en ninguna parte.
 */
data class Objective(
    val id: String,
    val title: String,
    val targetValue: Int?,
    val currentValue: Int,
    val deadline: LocalDate?,
    val completed: Boolean,
    val version: Int,
)

/**
 * Una rutina (FR-032).
 *
 * SIN `completed`, y no es un olvido: la misma rutina se hace una y otra vez,
 * así que su estado permanente es `active` y la ocurrencia en curso se expresa
 * con `nextExecutionDate`.
 *
 * `nextExecutionDate` se lee con `parseDate`, la misma política que el resto de
 * fechas del proyecto: se toma el DÍA CALENDARIO literal del ISO, sin convertir
 * zonas. Es exactamente lo que hace la Web (`iso.slice(0, 10)`), y por eso los
 * dos clientes coinciden siempre en qué día es cada registro.
 */
data class Routine(
    val id: String,
    val title: String,
    val description: String?,
    /** DAILY | WEEKLY | MONTHLY. */
    val frequency: String,
    val nextExecutionDate: LocalDate,
    val active: Boolean,
    /**
     * V35 — meta diaria y unidad. Nulos = rutina de sí/no.
     *
     * Es lo que separa «Sacar la basura», que se marca hecha, de «Agua», que
     * lleva un anillo con 4 de 8. Sin meta no hay contador y el artefacto
     * dibuja la rutina como casilla, no como anillo.
     */
    val targetCount: Int? = null,
    val unit: String? = null,
    val version: Int,
)

/**
 * Un recurso de trabajo (FR-034).
 *
 * `WorkResource` y no `Resource`: en este cliente "resource" ya significa
 * "cualquier cosa creable", y además existe `SharedResource`. La etiqueta que
 * ve el usuario sigue siendo «Recursos».
 *
 * Guarda metadatos y una referencia de TEXTO libre — nunca un archivo. Los dos
 * vínculos son opcionales y NO excluyentes: puede tener persona, proyecto, los
 * dos, o ninguno. Un recurso sin ninguno es válido, y hacerlo visible es
 * justamente lo que la Web no consigue hoy.
 *
 * Sin estado: no se completa, no se ejecuta, no se pausa. Su backend no expone
 * ninguna acción, solo CRUD.
 */
data class WorkResource(
    val id: String,
    val name: String,
    /** DOCUMENTO | ENLACE | PLANTILLA | MANUAL | HERRAMIENTA | OTRO. */
    val type: String,
    val reference: String?,
    val description: String?,
    val personId: String?,
    val projectId: String?,
    val version: Int,
)

/**
 * Un lugar guardado (FR-033).
 *
 * CATÁLOGO, NO RELACIÓN: ninguna tarea lo referencia, y no es un olvido sino
 * una decisión de producto escrita en la propia entidad del backend. Elegir un
 * lugar al crear una tarea (en la Web) COPIA su texto; no lo enlaza.
 *
 * Consecuencia declarada, no disimulada: renombrar un lugar NO cambia las
 * tareas ya creadas. Cada una guarda la cadena que se copió aquel día.
 *
 * Sin coordenadas: el backend no las tiene. Un lugar es un nombre y, si acaso,
 * una dirección de texto.
 */
data class Place(
    val id: String,
    val name: String,
    val address: String?,
    val personId: String?,
    val version: Int,
)

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
/**
 * CADA COLECCIÓN QUE SE CARGA POR SEPARADO.
 *
 * Existe porque `loadAll` lanza diecinueve peticiones independientes y hasta
 * ahora las resolvía todas en el mismo saco: una lista vacía significaba a la
 * vez «no tienes nada» y «no pude preguntarlo». Con una porción por colección,
 * el fallo de cada una sobrevive hasta la pantalla que la enseña, y ninguna
 * otra sección tiene que fingir que también falló.
 */
enum class DataSlice {
    WARRANTIES, MAINTENANCE, INVENTORY, DOCUMENTS, PAYMENTS,
    FAMILY_MEMBERS, INVITATIONS, SHARED_WITH_ME, SHARED_BY_ME,
    PEOPLE, PROJECTS, COMMITMENTS, OBJECTIVES, ROUTINES,
    WORK_RESOURCES, PLACES, INBOX, PARTICIPATIONS, PAYMENT_RECORDS,
}

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
    val objectives: List<Objective> = emptyList(),
    val routines: List<Routine> = emptyList(),
    /** FR-034: recursos de trabajo. TODOS, incluidos los que no cuelgan de nada. */
    val workResources: List<WorkResource> = emptyList(),
    /** FR-033: lugares guardados. */
    val places: List<Place> = emptyList(),
    val inbox: List<InboxNote> = emptyList(),
    /** V32: quién participa en qué proyecto. Ver `ProjectParticipation`. */
    val participations: List<ProjectParticipation> = emptyList(),
    /** ADR-020(f): ciclos ya pagados. Ver `PaymentRecord`. */
    val paymentRecords: List<PaymentRecord> = emptyList(),
    /**
     * QUÉ COLECCIONES NO SE PUDIERON CARGAR.
     *
     * Es la pieza que impide que un vacío mienta (ADR-021 k). La lista de una
     * porción que está aquí NO dice nada sobre lo que el usuario tiene: dice
     * que no pudimos preguntarlo. Quien pinte un estado vacío o una cifra
     * tiene que mirar esto antes, y `AppUiState.sliceError` es el atajo para
     * hacerlo en una línea.
     */
    val failed: Set<DataSlice> = emptySet(),
) {
    /**
     * TODAS las colecciones, una sola vez y en un solo sitio.
     *
     * «En total» sumaba a mano ocho de estos campos, así que dejaba fuera
     * objetivos, proyectos, personas y lo compartido —y cada colección nueva
     * quedaba fuera también, salvo que alguien se acordara de editar aquella
     * línea—. Enumerarlas aquí, junto a su declaración, hace que olvidarse sea
     * mucho más difícil: quien añada un campo arriba lo tiene a la vista.
     *
     * No incluye `participations` ni `paymentRecords`: no son cosas que el
     * usuario haya dado de alta, sino relaciones y registros derivados de
     * otras. Contarlas inflaría el total con filas que él nunca creó.
     */
    val collections: List<List<Any>>
        get() = listOf(
            warranties, maintenance, inventory, documents, payments,
            familyMembers, sharedWithMe, sharedByMe, people, projects,
            commitments, objectives, routines, workResources, places, inbox,
        )
}

/**
 * Un ciclo de pago ya registrado (ADR-020(f)).
 *
 * `paidOn` es CUÁNDO el usuario lo marcó; `periodDate`, QUÉ ciclo cubre. Para
 * responder "¿qué llevo cubierto este mes?" manda `paidOn` — ver
 * `isPaidThisPeriod`.
 */
data class PaymentRecord(
    val id: String,
    val subscriptionId: String,
    val periodDate: String,
    val paidOn: String,
)

/**
 * Un compromiso está pagado cuando existe un registro marcado ESTE MES.
 *
 * Regla portada literalmente de la Web (`isPaidThisPeriod` en
 * `paymentsView.ts`), incluida la razón por la que mira `paidOn` y no
 * `periodDate`: al pagar por adelantado un recibo de septiembre estando en
 * agosto, el registro guarda `periodDate = 2026-09`, y comparar contra el mes
 * en curso daría "no pagado" con el pago ya hecho. "Pagado este mes" significa
 * lo que YO cubrí este mes.
 *
 * Vive aquí y no en la pantalla para que los dos clientes no puedan discrepar
 * sobre qué está pagado.
 */
fun List<PaymentRecord>.paidThisPeriod(today: LocalDate = LocalDate.now()): Set<String> {
    val prefix = "%04d-%02d".format(today.year, today.monthValue)
    return filter { it.paidOn.startsWith(prefix) }.map { it.subscriptionId }.toSet()
}

/**
 * V32. Una persona participando en un proyecto, con su rol.
 *
 * NO incluye al cliente: ese sigue siendo `Project.clientPersonId` (DECISION
 * del Product Owner, 2026-09-06). Cliente y participantes son dos listas
 * disjuntas, y quien cuenta "¿en cuántos proyectos está esta persona?" tiene
 * que sumar las dos.
 */
data class ProjectParticipation(
    val id: String,
    val projectId: String,
    val personId: String,
    val role: String,
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
            val participations = async { runCatching { laboralApi.participations().map { it.toDomain() } } }
            // Sin `context`: los endpoints de objetivos y rutinas no lo aceptan.
            val objectives = async { runCatching { laboralApi.objectives().items.map { it.toDomain() } } }
            val routines = async { runCatching { laboralApi.routines().items.map { it.toDomain() } } }
            // Sin filtrar por persona ni proyecto: los recursos sueltos también
            // se cargan, que es lo que la pantalla propia viene a hacer visible.
            val workResources = async { runCatching { laboralApi.workResources().items.map { it.toDomain() } } }
            val places = async { runCatching { laboralApi.places().items.map { it.toDomain() } } }
            val paymentRecords = async { runCatching { subscriptionApi.paymentRecords().map { it.toDomain() } } }

            // EL FALLO DE CADA PORCIÓN SE ANOTA, NO SE TIRA.
            //
            // Antes esto era `getOrDefault(emptyList())` diecinueve veces, y
            // ahí moría la única información que distinguía «no tienes nada»
            // de «no pude preguntarlo». La lista vacía sigue estando —las
            // pantallas necesitan algo que recorrer mientras se resuelve la
            // pantalla de error— pero ahora viene acompañada de la verdad.
            val failed = mutableSetOf<DataSlice>()
            fun <T> Result<List<T>>.recorded(slice: DataSlice): List<T> {
                if (isFailure) failed += slice
                return getOrDefault(emptyList())
            }

            // Se resuelven en variables antes de construir `VidaData` para que
            // `failed` esté completo cuando se lee: si se anotara dentro de la
            // propia llamada al constructor, el orden de evaluación decidiría
            // en silencio cuántos fallos se ven.
            val vWarranties = warranties.await().recorded(DataSlice.WARRANTIES)
            val vMaintenance = maintenance.await().recorded(DataSlice.MAINTENANCE)
            val vInventory = inventory.await().recorded(DataSlice.INVENTORY)
            val vDocuments = documents.await().recorded(DataSlice.DOCUMENTS)
            val vPayments = payments.await().recorded(DataSlice.PAYMENTS)
            val vMembers = members.await().recorded(DataSlice.FAMILY_MEMBERS)
            val vInvitations = invitations.await().recorded(DataSlice.INVITATIONS)
            val vSharedIn = sharedIn.await().recorded(DataSlice.SHARED_WITH_ME)
            val vSharedOut = sharedOut.await().recorded(DataSlice.SHARED_BY_ME)
            val vPeople = people.await().recorded(DataSlice.PEOPLE)
            val vProjects = projects.await().recorded(DataSlice.PROJECTS)
            val vCommitments = commitments.await().recorded(DataSlice.COMMITMENTS)
            val vObjectives = objectives.await().recorded(DataSlice.OBJECTIVES)
            val vRoutines = routines.await().recorded(DataSlice.ROUTINES)
            val vWorkResources = workResources.await().recorded(DataSlice.WORK_RESOURCES)
            val vPlaces = places.await().recorded(DataSlice.PLACES)
            val vInbox = notes.await().recorded(DataSlice.INBOX)
            val vParticipations = participations.await().recorded(DataSlice.PARTICIPATIONS)
            val vPaymentRecords = paymentRecords.await().recorded(DataSlice.PAYMENT_RECORDS)

            VidaData(
                warranties = vWarranties,
                maintenance = vMaintenance,
                inventory = vInventory,
                documents = vDocuments,
                payments = vPayments,
                familyMembers = vMembers,
                receivedInvitations = vInvitations,
                sharedWithMe = vSharedIn,
                sharedByMe = vSharedOut,
                people = vPeople,
                projects = vProjects,
                commitments = vCommitments,
                objectives = vObjectives,
                routines = vRoutines,
                workResources = vWorkResources,
                places = vPlaces,
                inbox = vInbox,
                participations = vParticipations,
                paymentRecords = vPaymentRecords,
                failed = failed,
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

    suspend fun createMaintenance(
        item: String,
        nextDue: LocalDate,
        intervalMonths: Int?,
        context: String?,
        inventoryItemId: String?,
    ): Result<Unit> =
        runCatching {
            maintenanceApi.create(
                com.vidacotidiana.app.core.network.CreateMaintenanceRequest(
                    item = item,
                    nextDueAt = nextDue.toInstantString(),
                    intervalMonths = intervalMonths,
                    context = context,
                    inventoryItemId = inventoryItemId?.ifBlank { null },
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

    /**
     * Devuelve el id, no `Unit`: el alta desde el propio formulario de una
     * garantía necesita seleccionar el artículo recién creado, y volver a
     * buscarlo por nombre elegiría mal en cuanto hubiera dos que se llaman
     * igual.
     */
    suspend fun createInventoryItem(
        name: String,
        category: String,
        location: String?,
        context: String?,
    ): Result<String> = runCatching {
        inventoryApi.create(
            com.vidacotidiana.app.core.network.CreateInventoryItemRequest(name, category, location?.ifBlank { null }),
            context,
        ).id
    }

    /**
     * Garantía y documento comparten forma: son multipart porque el archivo es
     * parte del recurso, no un adjunto opcional.
     */
    suspend fun createWarranty(
        item: String,
        expires: LocalDate,
        file: FilePayload,
        inventoryItemId: String,
        context: String?,
    ): Result<Unit> =
        runCatching {
            warrantyApi.create(file.toPart(), item, expires.toInstantString(), inventoryItemId, context)
            Unit
        }

    suspend fun uploadDocument(name: String, category: String, file: FilePayload, context: String?): Result<Unit> =
        runCatching {
            documentApi.upload(file.toPart(), name, category, context)
            Unit
        }

    /** Devuelve el id por el mismo motivo que `createInventoryItem`. */
    suspend fun createPerson(name: String, role: String?, organization: String?): Result<String> = runCatching {
        laboralApi.createPerson(
            com.vidacotidiana.app.core.network.CreatePersonRequest(name, role?.ifBlank { null }, organization?.ifBlank { null }),
        ).id
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

    /**
     * `currentValue` viaja solo si el usuario lo escribió: omitirlo deja que el
     * backend ponga 0, en vez de que este cliente decida el valor inicial.
     */
    suspend fun createObjective(
        title: String,
        targetValue: Int?,
        currentValue: Int?,
        deadline: LocalDate?,
    ): Result<Unit> = runCatching {
        laboralApi.createObjective(
            com.vidacotidiana.app.core.network.CreateObjectiveRequest(
                title = title,
                targetValue = targetValue,
                currentValue = currentValue,
                deadline = deadline?.toInstantString(),
            ),
        )
        Unit
    }

    /**
     * `nextExecutionDate` lo elige el usuario y NO lo deriva este cliente:
     * AC-019 deja explícitamente como TBD de dónde saldría una fecha inicial
     * automática, así que inventarla aquí sería decidir por el producto.
     */
    suspend fun createRoutine(
        title: String,
        description: String?,
        frequency: String,
        nextExecutionDate: LocalDate,
    ): Result<Unit> = runCatching {
        laboralApi.createRoutine(
            com.vidacotidiana.app.core.network.CreateRoutineRequest(
                title = title,
                description = description?.ifBlank { null },
                frequency = frequency,
                nextExecutionDate = nextExecutionDate.toInstantString(),
            ),
        )
        Unit
    }

    /**
     * Los dos vínculos son opcionales y no excluyentes. No se valida nada aquí:
     * el backend comprueba que la persona y el proyecto sean del mismo dueño, y
     * duplicar esa regla en el cliente solo abriría la puerta a que discrepen.
     */
    suspend fun createWorkResource(
        name: String,
        type: String,
        reference: String?,
        description: String?,
        personId: String?,
        projectId: String?,
    ): Result<Unit> = runCatching {
        laboralApi.createWorkResource(
            com.vidacotidiana.app.core.network.CreateWorkResourceRequest(
                name = name,
                type = type,
                reference = reference?.ifBlank { null },
                description = description?.ifBlank { null },
                personId = personId?.ifBlank { null },
                projectId = projectId?.ifBlank { null },
            ),
        )
        Unit
    }

    /**
     * LIMITACIÓN CONOCIDA, declarada y fuera de alcance (pendiente #4 y #9): un
     * vínculo ya guardado NO se puede quitar. `null` significa "no tocar" en el
     * PATCH, y el selector de la interfaz tampoco permite deseleccionar.
     */
    suspend fun editWorkResource(
        id: String,
        name: String,
        type: String,
        reference: String?,
        description: String?,
        personId: String?,
        projectId: String?,
        version: Int,
    ): Result<Unit> = runCatching {
        laboralApi.updateWorkResource(
            id,
            com.vidacotidiana.app.core.network.UpdateWorkResourceRequest(
                name = name,
                type = type,
                reference = reference?.ifBlank { null },
                description = description?.ifBlank { null },
                personId = personId?.ifBlank { null },
                projectId = projectId?.ifBlank { null },
                version = version,
            ),
        )
        Unit
    }

    suspend fun createPlace(name: String, address: String?, personId: String?): Result<Unit> =
        runCatching {
            laboralApi.createPlace(
                com.vidacotidiana.app.core.network.CreatePlaceRequest(
                    name = name,
                    address = address?.ifBlank { null },
                    personId = personId?.ifBlank { null },
                ),
            )
            Unit
        }

    /** `null` = no tocar: una dirección o una persona ya guardadas no se pueden
        quitar desde aquí. Limitación transversal declarada (pendiente #4). */
    suspend fun editPlace(
        id: String,
        name: String,
        address: String?,
        personId: String?,
        version: Int,
    ): Result<Unit> = runCatching {
        laboralApi.updatePlace(
            id,
            com.vidacotidiana.app.core.network.UpdatePlaceRequest(
                name = name,
                address = address?.ifBlank { null },
                personId = personId?.ifBlank { null },
                version = version,
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

    suspend fun editMaintenance(
        id: String,
        item: String,
        nextDue: LocalDate,
        months: Int?,
        version: Int,
        inventoryItemId: String?,
    ): Result<Unit> =
        runCatching {
            maintenanceApi.update(
                id,
                com.vidacotidiana.app.core.network.UpdateMaintenanceRequest(
                    item = item,
                    nextDueAt = nextDue.toInstantString(),
                    intervalMonths = months,
                    version = version,
                    inventoryItemId = inventoryItemId?.ifBlank { null },
                    // Siempre explícito: así dejar el campo vacío DESENLAZA de
                    // verdad, en vez de interpretarse como "no tocar".
                    linkInventoryItem = true,
                ),
            )
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

    /**
     * Editar un objetivo. NO toca `completed`: cambiar el progreso no cumple ni
     * reabre nada (AC-018), y mandarlo aquí sería derivar uno del otro por la
     * puerta de atrás.
     *
     * LIMITACIÓN CONOCIDA, no un olvido: un `deadline` o un `targetValue` ya
     * guardados NO se pueden vaciar. `null` significa "no tocar" en el PATCH de
     * los cuatro módulos de Laboral, y resolverlo exige una bandera explícita
     * —como `linkInventoryItem` en Garantías— que esta fase no toca.
     */
    suspend fun editObjective(
        id: String,
        title: String,
        targetValue: Int?,
        currentValue: Int?,
        deadline: LocalDate?,
        version: Int,
    ): Result<Unit> = runCatching {
        laboralApi.updateObjective(
            id,
            com.vidacotidiana.app.core.network.UpdateObjectiveRequest(
                title = title,
                targetValue = targetValue,
                currentValue = currentValue,
                deadline = deadline?.toInstantString(),
                version = version,
            ),
        )
        Unit
    }

    /**
     * Editar una rutina. NO toca `active`: pausar y reanudar son su propia
     * acción, no un efecto de editar.
     *
     * Cambiar `frequency` NO recalcula `nextExecutionDate` — el backend tampoco
     * lo hace (`applyEdit` asigna cada campo por separado), así que este cliente
     * no inventa esa regla. La fecha sigue siendo del usuario.
     */
    suspend fun editRoutine(
        id: String,
        title: String,
        description: String?,
        frequency: String,
        nextExecutionDate: LocalDate,
        version: Int,
    ): Result<Unit> = runCatching {
        laboralApi.updateRoutine(
            id,
            com.vidacotidiana.app.core.network.UpdateRoutineRequest(
                title = title,
                description = description?.ifBlank { null },
                frequency = frequency,
                nextExecutionDate = nextExecutionDate.toInstantString(),
                version = version,
            ),
        )
        Unit
    }

    // --- Acciones de estado ---

    /**
     * «Hecha»: registra UNA ocurrencia.
     *
     * Llama al endpoint propio y NO calcula ninguna fecha. El avance lo decide
     * el servidor desde la fecha PROGRAMADA, no desde hoy, así que una rutina
     * atrasada varios periodos sigue atrasada tras un clic — es el contrato, no
     * un fallo, y este cliente no lo compensa con más llamadas.
     */
    suspend fun executeRoutine(id: String, version: Int): Result<Routine> =
        runCatching {
            laboralApi.executeRoutine(id, com.vidacotidiana.app.core.network.VersionRequest(version)).toDomain()
        }

    /** Pausar y reanudar: el PATCH de edición admite `active` (verificado). */
    suspend fun setRoutineActive(id: String, active: Boolean, version: Int): Result<Unit> =
        runCatching {
            laboralApi.updateRoutine(
                id,
                com.vidacotidiana.app.core.network.UpdateRoutineRequest(active = active, version = version),
            )
            Unit
        }

    /**
     * Cumplir y reabrir son el MISMO PATCH con distinto valor: no existe
     * `POST /objectives/{id}/complete`.
     *
     * Solo viajan `completed` y `version` —el resto se queda en su valor por
     * defecto y `encodeDefaults = false` no lo serializa—, así que el título, la
     * meta, el progreso y la fecha quedan exactamente como estaban.
     */
    suspend fun setObjectiveCompleted(id: String, completed: Boolean, version: Int): Result<Objective> =
        runCatching {
            laboralApi.updateObjective(
                id,
                com.vidacotidiana.app.core.network.UpdateObjectiveRequest(
                    completed = completed,
                    version = version,
                ),
            ).toDomain()
        }

    suspend fun completeWarranty(id: String, version: Int): Result<Warranty> =
        runCatching { warrantyApi.complete(id, com.vidacotidiana.app.core.network.VersionRequest(version)).toDomain() }

    /** ADR-021: en mantenimiento, completar AVANZA la ocurrencia. */
    suspend fun completeMaintenance(id: String): Result<MaintenanceRecord> =
        runCatching { maintenanceApi.completeOccurrence(id).toDomain() }

    /* ------------------------------------------------------------------
       VOLVER ATRÁS.
       Marcar algo por error es trivial y hasta ahora solo Objetivos tenía
       vuelta: el resto obligaba a borrar y volver a crear, perdiendo fecha e
       historial. Ninguno de estos endpoints es nuevo salvo el del seguimiento
       —el backend ya los tenía y nadie los llamaba.
       ------------------------------------------------------------------ */

    // La tarea y la garantía NO aparecen aquí: su `complete` YA es un toggle
    // en el backend (PENDING↔COMPLETED, ACTIVE↔COMPLETED), así que reabrir es
    // literalmente la misma llamada. Darles un método propio habría sido
    // duplicar `completeWarranty` bajo otro nombre.

    /** Borra la última ejecución y devuelve la fecha que tenía antes. */
    suspend fun undoMaintenance(id: String): Result<MaintenanceRecord> =
        runCatching { maintenanceApi.undoLastOccurrence(id).toDomain() }

    /**
     * Borra el último pago registrado. Devuelve además los registros, por la
     * misma razón que `registerPayment`: sin ellos la pantalla seguiría
     * creyendo que este ciclo está cubierto.
     */
    suspend fun undoPayment(id: String): Result<Pair<Payment, List<PaymentRecord>>> =
        runCatching {
            val payment = subscriptionApi.undoLastPayment(id).toDomain()
            payment to subscriptionApi.paymentRecords().map { it.toDomain() }
        }

    suspend fun reopenCommitment(id: String): Result<Commitment> =
        runCatching { laboralApi.reopenCommitment(id).toDomain() }

    /**
     * Registrar el pago del ciclo devuelve el compromiso con su FECHA NUEVA…
     * pero no el registro que acaba de crearse, y sin él la pantalla no sabría
     * que este periodo ya está cubierto. Se piden los registros —UNA llamada—
     * en vez de recargar las veinte colecciones.
     */
    suspend fun registerPayment(id: String, version: Int): Result<Pair<Payment, List<PaymentRecord>>> =
        runCatching {
            val payment = subscriptionApi
                .registerPayment(id, com.vidacotidiana.app.core.network.VersionRequest(version))
                .toDomain()
            payment to subscriptionApi.paymentRecords().map { it.toDomain() }
        }

    suspend fun resolveCommitment(id: String, version: Int): Result<Commitment> =
        runCatching { laboralApi.resolveCommitment(id, com.vidacotidiana.app.core.network.VersionRequest(version)).toDomain() }

    // --- Documentos: lo que la Web ya permitía y Android no ---

    suspend fun documentBytes(id: String): Result<ByteArray> =
        runCatching { withContext(kotlinx.coroutines.Dispatchers.IO) { documentApi.content(id).use { it.bytes() } } }

    /**
     * Varios documentos en un zip. `ids` vacío = todos los del módulo activo,
     * que es el contrato del backend y evita enumerar desde el cliente una
     * lista que puede estar paginada.
     */
    suspend fun documentsZip(ids: Set<String>, context: String?): Result<ByteArray> =
        runCatching {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                documentApi.downloadZip(
                    com.vidacotidiana.app.core.network.DownloadDocumentsRequest(
                        ids = ids.takeIf { it.isNotEmpty() }?.toList(),
                        context = context,
                    ),
                ).use { it.bytes() }
            }
        }

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
            com.vidacotidiana.app.core.app.CreatableResource.OBJECTIVE -> laboralApi.deleteObjective(id)
            com.vidacotidiana.app.core.app.CreatableResource.ROUTINE -> laboralApi.deleteRoutine(id)
            com.vidacotidiana.app.core.app.CreatableResource.WORK_RESOURCE -> laboralApi.deleteWorkResource(id)
            com.vidacotidiana.app.core.app.CreatableResource.PLACE -> laboralApi.deletePlace(id)
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
        // `MaintenanceStatus` en el backend es ACTIVE/COMPLETED, y COMPLETED
        // solo ocurre en los puntuales: los repetibles vuelven a ACTIVE con la
        // fecha ya avanzada.
        status = when {
            status == "COMPLETED" -> MaintenanceStatus.HECHO
            days < 0 -> MaintenanceStatus.VENCIDO
            days <= 7 -> MaintenanceStatus.PROXIMO
            else -> MaintenanceStatus.AL_DIA
        },
        inventoryItemId = inventoryItemId,
        version = version,
    )
}

private fun com.vidacotidiana.app.core.network.PlaceDto.toDomain() = Place(
    id = id,
    name = name,
    address = address,
    personId = personId,
    version = version,
)

private fun com.vidacotidiana.app.core.network.WorkResourceDto.toDomain() = WorkResource(
    id = id,
    name = name,
    type = type,
    reference = reference,
    description = description,
    personId = personId,
    projectId = projectId,
    version = version,
)

private fun com.vidacotidiana.app.core.network.RoutineDto.toDomain() = Routine(
    id = id,
    title = title,
    description = description,
    frequency = frequency,
    // `parseDate` no debería fallar aquí —el campo es NOT NULL en el backend—,
    // pero si el ISO viniera ilegible es preferible una fecha de hoy a tirar
    // toda la lista de rutinas por un registro.
    nextExecutionDate = parseDate(nextExecutionDate) ?: LocalDate.now(),
    active = active,
    targetCount = targetCount,
    unit = unit,
    version = version,
)

private fun com.vidacotidiana.app.core.network.ObjectiveDto.toDomain() = Objective(
    id = id,
    title = title,
    targetValue = targetValue,
    currentValue = currentValue,
    deadline = parseDate(deadline),
    completed = completed,
    version = version,
)

private fun com.vidacotidiana.app.core.network.PaymentRecordDto.toDomain() = PaymentRecord(
    id = id,
    subscriptionId = subscriptionId,
    periodDate = periodDate,
    paidOn = paidOn,
)

private fun com.vidacotidiana.app.core.network.ProjectParticipantDto.toDomain() = ProjectParticipation(
    id = id,
    projectId = projectId,
    personId = personId,
    role = role,
)

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
    sizeBytes = sizeBytes,
    resourceType = resourceType,
    resourceId = resourceId,
    version = version,
)

/**
 * El tamaño con la unidad que le corresponde: B, KB o MB según la magnitud.
 *
 * Deja de ser privado porque la cifra de «Espacio» de Documentos formateaba
 * por su cuenta y siempre en megas, así que un archivo de 1 KB se anunciaba
 * como «0.0 MB» —un contador diciendo cero justo al lado de la lista que
 * mostraba el archivo y su tamaño real—. Ya había un formateador correcto en
 * la aplicación; sólo no se podía llamar desde fuera.
 */
fun humanSize(bytes: Long): String {
    val (value, unit) = humanSizeParts(bytes)
    return "$value $unit"
}

/** Lo mismo, partido, para quien pinta la cifra y la unidad por separado. */
fun humanSizeParts(bytes: Long): Pair<String, String> = when {
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f", bytes / 1_048_576.0) to "MB"
    bytes >= 1024 -> String.format(Locale.US, "%.0f", bytes / 1024.0) to "KB"
    else -> "$bytes" to "B"
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
        currency = currency,
        variableAmount = variableAmount,
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

private fun ProjectDto.toDomain() = Project(id, name, status, parseDate(deadline), clientPersonId, version)

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

/**
 * Sustituir UN elemento de una lista por su versión nueva, dejando el resto
 * exactamente como estaba — misma instancia, mismo orden.
 *
 * Es la pieza que permite que dar algo por hecho actualice esa fila y nada más.
 * Antes cualquier acción terminaba en una recarga completa porque no había
 * forma de decir «solo este cambió»; con esto, las demás colecciones conservan
 * su instancia y Compose no vuelve a dibujar lo que no se ha movido.
 */
fun <T> List<T>.replacing(updated: T, idOf: (T) -> String): List<T> {
    val id = idOf(updated)
    return map { if (idOf(it) == id) updated else it }
}
