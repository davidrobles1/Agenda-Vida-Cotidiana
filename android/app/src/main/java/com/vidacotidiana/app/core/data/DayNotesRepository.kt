package com.vidacotidiana.app.core.data

import com.vidacotidiana.app.core.network.CreateDayNoteRequest
import com.vidacotidiana.app.core.network.DayNoteApi
import com.vidacotidiana.app.core.network.DayNoteElementDto
import com.vidacotidiana.app.core.network.UpdateDayNoteDataRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notas del día — el MISMO servicio que la Web (`/api/v1/day-notes`), con la
 * misma semántica.
 *
 * La Web guarda cada nota como un elemento de un lienzo: `x`/`y`/`width`/
 * `height` y un `data` libre. En la lista de "notas en el margen" que la Web
 * pinta hoy, `y` no es una coordenada visual sino el ORDEN, en saltos de 100,
 * y el tamaño es fijo. Android escribe exactamente esas mismas constantes
 * (`STORED_SIZE`, `ORDER_STEP` de `DayNotesCanvas.tsx`) para que una nota
 * creada en el teléfono aparezca bien colocada en el navegador y al revés.
 *
 * Esto no es un backend nuevo ni un endpoint nuevo: es el que ya existía y
 * que Android no estaba usando.
 */

data class DayNote(
    val id: String,
    val date: LocalDate,
    val text: String,
    val version: Int,
    /** Orden dentro del día — el `y` del elemento. */
    val order: Double,
    /** El `data` original, para reenviarlo intacto al editar. */
    internal val raw: JsonObject,
    internal val bold: Boolean = false,
    internal val italic: Boolean = false,
)

@Singleton
class DayNotesRepository @Inject constructor(private val api: DayNoteApi) {

    private companion object {
        // Idénticas a `DayNotesCanvas.tsx`: STORED_SIZE y ORDER_STEP.
        const val WIDTH = 280.0
        const val HEIGHT = 84.0
        const val ORDER_STEP = 100.0
        const val TYPE_TEXT = "TEXT"
    }

    /**
     * NO se filtra por tipo. La Web dibuja también como texto los elementos
     * guardados en su día como `BANNER`, y filtrarlos aquí escondería en el
     * teléfono notas que el navegador sí muestra. El tipo se conserva intacto
     * en el servidor; solo cambia cómo se dibuja, igual que allí.
     */
    suspend fun list(date: LocalDate, context: String?): Result<List<DayNote>> = runCatching {
        api.listForDay(date.toString(), context)
            .map { it.toDomain() }
            .sortedBy { it.order }
    }

    /**
     * Crea una nota al final del día, igual que el compositor de la Web:
     * `y = max(y existente) + 100`.
     */
    suspend fun create(date: LocalDate, text: String, existing: List<DayNote>, context: String?): Result<DayNote> =
        runCatching {
            val y = (existing.maxOfOrNull { it.order } ?: 0.0) + ORDER_STEP
            api.create(
                CreateDayNoteRequest(
                    noteDate = date.toString(),
                    type = TYPE_TEXT,
                    x = 0.0,
                    y = y,
                    width = WIDTH,
                    height = HEIGHT,
                    // Los mismos cuatro campos que escribe la Web al crear.
                    data = buildJsonObject {
                        put("text", text.trim())
                        put("bold", false)
                        put("italic", false)
                        put("font", "sans")
                    },
                    context = context,
                ),
            ).toDomain()
        }

    /**
     * Edita el texto CONSERVANDO el resto de `data`. Reenviar solo `text`
     * borraría la tipografía y el énfasis que la nota tuviera puestos desde la
     * Web, porque el endpoint reemplaza el objeto entero.
     */
    suspend fun editText(note: DayNote, text: String): Result<DayNote> = runCatching {
        val merged = buildJsonObject {
            note.raw.forEach { (key, value) -> put(key, value) }
            put("text", text.trim())
        }
        api.editData(note.id, UpdateDayNoteDataRequest(merged, note.version)).toDomain()
    }

    suspend fun delete(note: DayNote): Result<Unit> = runCatching { api.delete(note.id) }
}

private fun DayNoteElementDto.toDomain() = DayNote(
    id = id,
    date = LocalDate.parse(noteDate.take(10)),
    text = data["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    version = version,
    order = y,
    raw = data,
    bold = data["bold"]?.jsonPrimitive?.booleanOrNull ?: false,
    italic = data["italic"]?.jsonPrimitive?.booleanOrNull ?: false,
)
