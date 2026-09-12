package com.vidacotidiana.app.core.data

import com.vidacotidiana.app.core.network.DocumentApi
import com.vidacotidiana.app.core.network.DocumentDto
import com.vidacotidiana.app.core.network.LinkDocumentRequest
import com.vidacotidiana.app.core.network.MoodApi
import com.vidacotidiana.app.core.network.MoodDto
import com.vidacotidiana.app.core.network.ReminderStepApi
import com.vidacotidiana.app.core.network.RoutineProgressApi
import com.vidacotidiana.app.core.network.RoutineProgressDto
import com.vidacotidiana.app.core.network.RoutineProgressRequest
import com.vidacotidiana.app.core.network.RoutineTargetRequest
import com.vidacotidiana.app.core.network.StepDto
import com.vidacotidiana.app.core.network.StepReorderRequest
import com.vidacotidiana.app.core.network.StepTitleRequest
import com.vidacotidiana.app.core.network.UpsertMoodRequest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LAS TRES CAPACIDADES DEL ARTEFACTO, CONTRA LA API REAL.
 *
 * Vive aparte de `VidaRepository` por el mismo criterio que `DayNotesRepository`
 * y `VisionBoardRepository`: son dominios con su propio ciclo, que no entran en
 * el `loadAll` de la pantalla de Inicio y no deben hacerlo —el ánimo se
 * consulta al abrir Bienestar, los pasos al abrir una tarea, y meterlos en la
 * carga general costaría una petición por tarea en una lista de veinte.
 *
 * NINGÚN DATO SE INVENTA AQUÍ. Todo sale de PostgreSQL a través de los tres
 * controladores. Cuando la API falla, sube el `Result` en fallo y la pantalla
 * enseña su estado de error; no hay valor de reserva que disimule la caída.
 */
@Singleton
class WellbeingRepository @Inject constructor(
    private val moodApi: MoodApi,
    private val stepApi: ReminderStepApi,
    private val progressApi: RoutineProgressApi,
    private val documentApi: DocumentApi,
) {

    /* ── ADJUNTOS ───────────────────────────────────────────────────────── */

    /**
     * Los adjuntos de un recurso (V37).
     *
     * UNA SOLA MECÁNICA para los cinco tipos. No hay un método por módulo: una
     * garantía, un mantenimiento y una tarea preguntan igual, cambiando el
     * `type`. Es lo que evita que dentro de un año existan cinco formas
     * distintas de guardar un archivo.
     */
    suspend fun attachments(type: AttachTo, resourceId: String): Result<List<Attachment>> = runCatching {
        documentApi.attachments(type.name, resourceId).map { it.toAttachment() }
    }

    /** Colgar un documento ya existente de un recurso. */
    suspend fun attach(documentId: String, type: AttachTo, resourceId: String): Result<Attachment> = runCatching {
        documentApi.link(documentId, LinkDocumentRequest(type.name, resourceId)).toAttachment()
    }

    /**
     * Soltar el documento SIN borrarlo: deja de pertenecer al registro y sigue
     * estando en Documentos. Borrar al desenganchar sería destruir un archivo
     * por reordenar.
     */
    suspend fun detach(documentId: String): Result<Attachment> = runCatching {
        documentApi.link(documentId, LinkDocumentRequest(null, null)).toAttachment()
    }

    /* ── ÁNIMO ──────────────────────────────────────────────────────────── */

    /**
     * El ánimo de hoy. `null` NO es un error: es que el día aún no se ha
     * marcado, que es el estado normal de la primera vez que se abre.
     */
    suspend fun moodToday(): Result<Mood?> = runCatching {
        val today = LocalDate.now().toString()
        moodApi.list(from = today, to = today).firstOrNull()?.toDomain()
    }

    /** El rango que pinta la semana y el mes de Bienestar. */
    suspend fun moods(from: LocalDate, to: LocalDate): Result<List<Mood>> = runCatching {
        moodApi.list(from = from.toString(), to = to.toString()).map { it.toDomain() }
    }

    /**
     * Marcar el ánimo. Es un UPSERT: tocar otra carita el mismo día corrige la
     * de hoy en vez de apilar una segunda.
     */
    suspend fun setMood(value: Int, note: String?, tags: List<String>, date: LocalDate? = null): Result<Mood> =
        runCatching {
            moodApi.upsert(
                UpsertMoodRequest(
                    date = date?.toString(),
                    value = value,
                    note = note?.takeIf { it.isNotBlank() },
                    tags = tags,
                ),
            ).toDomain()
        }

    /** Borrado DURO de todo el historial. Devuelve cuántos cayeron. */
    suspend fun deleteAllMoods(): Result<Int> = runCatching { moodApi.deleteAll().deleted }

    /* ── PASOS DE UNA TAREA ─────────────────────────────────────────────── */

    suspend fun steps(reminderId: String): Result<List<TaskStep>> = runCatching {
        stepApi.list(reminderId).map { it.toDomain() }
    }

    suspend fun addStep(reminderId: String, title: String): Result<TaskStep> = runCatching {
        stepApi.add(reminderId, StepTitleRequest(title)).toDomain()
    }

    /** Marcar y desmarcar son el mismo gesto, igual que en el artefacto. */
    suspend fun toggleStep(reminderId: String, stepId: String): Result<TaskStep> = runCatching {
        stepApi.toggle(reminderId, stepId).toDomain()
    }

    suspend fun renameStep(reminderId: String, stepId: String, title: String): Result<TaskStep> = runCatching {
        stepApi.rename(reminderId, stepId, StepTitleRequest(title)).toDomain()
    }

    suspend fun deleteStep(reminderId: String, stepId: String): Result<Unit> = runCatching {
        stepApi.delete(reminderId, stepId)
    }

    suspend fun reorderSteps(reminderId: String, orderedIds: List<String>): Result<List<TaskStep>> = runCatching {
        stepApi.reorder(reminderId, StepReorderRequest(orderedIds)).map { it.toDomain() }
    }

    /* ── PROGRESO DE UN HÁBITO ──────────────────────────────────────────── */

    /**
     * Sumar al día. NO ejecuta la rutina: `execute` y `progress` son cosas
     * distintas y conviven, porque beber un vaso de agua no debe adelantar la
     * rutina a mañana.
     */
    suspend fun addProgress(routineId: String, delta: Int = 1): Result<DayProgress> = runCatching {
        progressApi.add(routineId, RoutineProgressRequest(delta = delta)).toDomain()
    }

    /** Fijar el valor exacto de un día, para corregir sin ir de uno en uno. */
    suspend fun setProgress(routineId: String, value: Int, date: LocalDate? = null): Result<DayProgress> =
        runCatching {
            progressApi.set(routineId, RoutineProgressRequest(date = date?.toString(), value = value)).toDomain()
        }

    suspend fun progress(routineId: String, from: LocalDate, to: LocalDate): Result<List<DayProgress>> =
        runCatching {
            progressApi.list(routineId, from.toString(), to.toString()).map { it.toDomain() }
        }

    /** Lo de hoy de una rutina. Ausente = cero, no un fallo. */
    suspend fun progressToday(routineId: String): Result<Int> = runCatching {
        val today = LocalDate.now()
        progressApi.list(routineId, today.toString(), today.toString()).firstOrNull()?.count ?: 0
    }

    /** Declarar la meta diaria — o retirarla con `null`, sin perder histórico. */
    suspend fun setTarget(routineId: String, targetCount: Int?, unit: String?): Result<Unit> = runCatching {
        progressApi.setTarget(routineId, RoutineTargetRequest(targetCount, unit))
        Unit
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   MODELOS DE DOMINIO
   Los mismos nombres que usan las pantallas, para que la UI no sepa de DTOs.
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * El ánimo de un día.
 *
 * `value` es la escala cerrada del artefacto: 0 genial · 1 bien · 2 normal ·
 * 3 regular · 4 bajo. No se guarda hora, solo el día.
 */
data class Mood(
    val id: String,
    val date: LocalDate,
    val value: Int,
    val note: String?,
    val tags: List<String>,
)

data class TaskStep(
    val id: String,
    val reminderId: String,
    val title: String,
    val done: Boolean,
    val position: Int,
)

data class DayProgress(
    val id: String,
    val routineId: String,
    val date: LocalDate,
    val count: Int,
)

/**
 * EL PORCENTAJE SE DERIVA, NO SE GUARDA.
 *
 * Ni aquí ni en el servidor existe una columna con esta cifra: se cuenta desde
 * los pasos cada vez que se necesita. Dos fuentes para el mismo número acaban
 * discrepando, y la que se ve sería la equivocada.
 */
fun List<TaskStep>.percentDone(): Int =
    if (isEmpty()) 0 else Math.round(count { it.done } * 100f / size)

private fun MoodDto.toDomain() = Mood(
    id = id,
    date = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() },
    value = value.coerceIn(0, 4),
    note = note,
    tags = tags,
)

private fun StepDto.toDomain() = TaskStep(id, reminderId, title, done, position)

private fun RoutineProgressDto.toDomain() = DayProgress(
    id = id,
    routineId = routineId,
    date = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() },
    count = count,
)

/** Los cinco recursos de los que puede colgar un documento (V37). */
enum class AttachTo { REMINDER, MAINTENANCE, WARRANTY, INVENTORY_ITEM, SUBSCRIPTION }

/**
 * Un documento visto COMO ADJUNTO.
 *
 * Mismo registro que en la sección Documentos: no es otra entidad, es la misma
 * mirada desde el recurso del que cuelga.
 */
data class Attachment(
    val id: String,
    val name: String,
    val contentType: String,
    val sizeBytes: Long,
    val resourceType: String?,
    val resourceId: String?,
) {
    /** Para elegir icono sin repetir la condición en cada pantalla. */
    val isImage: Boolean get() = contentType.startsWith("image/")
}

private fun DocumentDto.toAttachment() = Attachment(
    id = id,
    name = name,
    contentType = contentType,
    sizeBytes = sizeBytes,
    resourceType = resourceType,
    resourceId = resourceId,
)
