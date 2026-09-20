package com.vidacotidiana.app.core.attention

import com.vidacotidiana.app.core.data.MaintenanceStatus
import com.vidacotidiana.app.core.data.VidaData
import com.vidacotidiana.app.core.data.paidThisPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * QUÉ NECESITA AL USUARIO, LEÍDO DE TODO LO QUE LA APLICACIÓN YA SABE.
 *
 * EL PROBLEMA QUE RESUELVE. Cada módulo sabía perfectamente lo suyo y no lo
 * contaba fuera. Inicio miraba únicamente `contentFor(hoy).alerts`, que son los
 * avisos derivados de ADR-018 — y esos solo existen EN LAS FECHAS EXACTAS de
 * antelación (30/15/0 en garantías, 7/3/0 en mantenimiento, 5/2/0 en pagos).
 * Consecuencia: una tarea que venció ayer, un pago cuyo día pasó sin pagarse o
 * un mantenimiento atrasado NO llegaban a Inicio. El usuario tenía que entrar
 * módulo por módulo a comprobarlo, que es exactamente el trabajo que la
 * aplicación debería ahorrarle.
 *
 * Aquí se lee el estado REAL de cada registro —su fecha y su condición— en vez
 * de esperar a que caiga una fecha de aviso. Nada de esto es información nueva:
 * `Warranty.status`, `MaintenanceRecord.status`, `Payment.renewsOn` con
 * `paidThisPeriod()`, `Commitment.dueOn`, `Routine.nextExecutionDate` y
 * `SharedResource.responsibility` ya existían y ya se calculaban. Lo único que
 * no existía era alguien que los leyera juntos.
 *
 * NO INVENTA REGLAS. Cada condición de abajo es la que su propio módulo ya usa
 * para pintar su píldora de estado. Si un dato no tiene fecha, no aparece: una
 * tarea sin fecha no puede estar atrasada, y un objetivo sin plazo no reclama
 * nada.
 *
 * SOBRE EL CONTEXTO (ADR-019). No filtra por Personal/Laboral, y es a propósito:
 * `AppViewModel.loadAll` ya pide los datos con el contexto activo, así que
 * `VidaData` viene filtrado del servidor. En Portal llega todo junto, que es lo
 * que Portal significa. Volver a filtrar aquí duplicaría la regla y podría
 * contradecirla.
 */

/** Cuándo reclama. El orden del enum ES la prioridad. */
enum class AttentionUrgency { OVERDUE, TODAY, SOON }

/** De dónde sale y a dónde lleva. Las rutas son las de `Routes`, como en `AlertSource`. */
enum class AttentionSource(val label: String, val plural: String, val route: String) {
    TASK("Tarea", "tareas", "tasks"),
    PAYMENT("Pago", "pagos", "payments"),
    WARRANTY("Garantía", "garantías", "warranties"),
    MAINTENANCE("Mantenimiento", "mantenimientos", "maintenance"),
    COMMITMENT("Seguimiento", "seguimientos", "seguimientos"),
    ROUTINE("Rutina", "rutinas", "rutinas"),
    SHARED("Compartido", "compartidos", "shared"),
}

/**
 * Una cosa que reclama al usuario.
 *
 * `reason` es la pieza que cambia la percepción: no es «Garantías — 5», es «La
 * garantía vence en 3 días». Dice POR QUÉ está en pantalla, que es lo que
 * convierte un dato en un motivo.
 */
data class AttentionItem(
    val id: String,
    val title: String,
    val reason: String,
    val source: AttentionSource,
    val urgency: AttentionUrgency,
    val date: LocalDate,
    val amount: String? = null,
) {
    /**
     * El id del registro, sin el prefijo del módulo.
     *
     * `id` es «maintenance:8f3a-…» para que dos módulos no colisionen dentro
     * de la misma lista; navegar hasta el registro necesita el uuid a secas.
     */
    val resourceId: String get() = id.substringAfter(':')

    /**
     * La clave con la que se recuerda que este aviso ya se ha visto.
     *
     * LLEVA LA FECHA. Cuando un mantenimiento vuelva a tocar dentro de seis
     * meses será un aviso nuevo y tiene que aparecer sin leer; con el id solo,
     * marcarlo leído una vez lo callaría para siempre.
     */
    val noticeKey: String get() = "$id@$date"
}

/** Ventana de «lo que viene»: una semana. Más allá deja de ser algo que atender. */
private const val SOON_DAYS = 7L

private val DAY_MONTH: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"))

object AttentionEngine {

    /**
     * Todo lo que reclama, ordenado por cuánto reclama.
     *
     * Lo atrasado va primero y, dentro de lo atrasado, lo más antiguo: llevar
     * cinco días esperando pesa más que llevar uno.
     */
    fun scan(
        data: VidaData,
        /**
         * Las tareas van aparte porque `VidaData` no las lleva: viven en
         * `AppUiState.reminders` y llegan ya normalizadas por `allTasks()`.
         */
        tasks: List<com.vidacotidiana.app.core.calendar.DayTask>,
        today: LocalDate = LocalDate.now(),
    ): List<AttentionItem> {
        val out = mutableListOf<AttentionItem>()
        out += tasks(tasks, today)
        out += payments(data, today)
        out += warranties(data, today)
        out += maintenance(data, today)
        out += commitments(data, today)
        out += routines(data, today)
        out += shared(data, today)
        return out.sortedWith(compareBy({ it.urgency.ordinal }, { it.date }))
    }

    /** Lo que no puede esperar: vencido o de hoy. */
    fun now(items: List<AttentionItem>): List<AttentionItem> =
        items.filter { it.urgency != AttentionUrgency.SOON }

    /**
     * SOLO LO DE HOY, y solo lo de hoy.
     *
     * Existe porque «para hoy» y «lo que no puede esperar» NO son el mismo
     * conjunto, y durante un tiempo Inicio los confundió: contaba [now] bajo
     * la etiqueta «Para hoy», de modo que lo atrasado entraba en la cifra y
     * además volvía a contarse en el mosaico de al lado. Separarlos aquí —y no
     * en la pantalla— es lo que garantiza que quien pregunte por hoy reciba
     * hoy, venga de donde venga.
     */
    fun today(items: List<AttentionItem>): List<AttentionItem> =
        items.filter { it.urgency == AttentionUrgency.TODAY }

    /** Lo que ya se pasó de fecha, de cualquiera de las siete fuentes. */
    fun overdue(items: List<AttentionItem>): List<AttentionItem> =
        items.filter { it.urgency == AttentionUrgency.OVERDUE }

    /** El conjunto que corresponde a una urgencia, para quien la reciba como parámetro. */
    fun of(items: List<AttentionItem>, urgency: AttentionUrgency): List<AttentionItem> =
        items.filter { it.urgency == urgency }

    /** Lo que viene dentro de la semana. */
    fun soon(items: List<AttentionItem>): List<AttentionItem> =
        items.filter { it.urgency == AttentionUrgency.SOON }

    /**
     * «1 tarea · 2 pagos · 1 mantenimiento» — el desglose por módulo.
     *
     * Es la frase que hace visible el trabajo que la aplicación acaba de hacer:
     * ha mirado en seis sitios distintos y ha traído lo que reclamaba de cada
     * uno. Sin ella, un «3 cosas necesitan tu atención» no dice de dónde salen
     * y podría ser de un solo módulo.
     */
    fun breakdown(items: List<AttentionItem>): String =
        items.groupBy { it.source }
            .toList()
            .sortedBy { (source, _) -> source.ordinal }
            .joinToString(" · ") { (source, list) ->
                "${list.size} ${if (list.size == 1) source.label.lowercase() else source.plural}"
            }

    // --- Una función por fuente, con la condición que su módulo ya usa. ---

    /** Una tarea sin fecha no puede estar atrasada, así que no entra. */
    private fun tasks(
        tasks: List<com.vidacotidiana.app.core.calendar.DayTask>,
        today: LocalDate,
    ): List<AttentionItem> =
        tasks.mapNotNull { r ->
            if (r.done) return@mapNotNull null
            val date = r.date ?: return@mapNotNull null
            val urgency = urgencyOf(date, today) ?: return@mapNotNull null
            AttentionItem(
                id = "task:${r.id}",
                title = r.title,
                reason = when (urgency) {
                    AttentionUrgency.OVERDUE -> "Se quedó pendiente ${past(date, today)}"
                    AttentionUrgency.TODAY -> r.time?.let { "Hoy a las ${it.toString().take(5)}" } ?: "Es para hoy"
                    AttentionUrgency.SOON -> "Toca ${future(date, today)}"
                },
                source = AttentionSource.TASK,
                urgency = urgency,
                date = date,
            )
        }

    /**
     * Un pago reclama cuando llegó su día Y NO se ha pagado este mes.
     *
     * `paidThisPeriod()` es la misma regla que usa la pantalla de Pagos —y antes
     * la Web—, así que Inicio no puede decir que debes algo que allí figura
     * pagado.
     */
    private fun payments(data: VidaData, today: LocalDate): List<AttentionItem> {
        val paid = data.paymentRecords.paidThisPeriod(today)
        return data.payments.mapNotNull { p ->
            if (p.id in paid) return@mapNotNull null
            val urgency = urgencyOf(p.renewsOn, today) ?: return@mapNotNull null
            AttentionItem(
                id = "payment:${p.id}",
                title = p.name,
                reason = when (urgency) {
                    AttentionUrgency.OVERDUE -> "Se pagaba ${past(p.renewsOn, today)}"
                    AttentionUrgency.TODAY -> "Se paga hoy"
                    AttentionUrgency.SOON -> "Se paga ${future(p.renewsOn, today)}"
                },
                source = AttentionSource.PAYMENT,
                urgency = urgency,
                date = p.renewsOn,
                amount = p.amountLabel,
            )
        }
    }

    /**
     * Una garantía ya vencida NO entra: no hay nada que hacer con ella, y una
     * lista de atención que incluye lo irremediable deja de ser accionable.
     * Solo reclama mientras aún se puede usar.
     */
    private fun warranties(data: VidaData, today: LocalDate): List<AttentionItem> =
        data.warranties.mapNotNull { w ->
            val urgency = urgencyOf(w.expiresOn, today) ?: return@mapNotNull null
            if (urgency == AttentionUrgency.OVERDUE) return@mapNotNull null
            AttentionItem(
                id = "warranty:${w.id}",
                title = w.product,
                reason = if (urgency == AttentionUrgency.TODAY) {
                    "Último día de garantía"
                } else {
                    "La garantía vence ${future(w.expiresOn, today)}"
                },
                source = AttentionSource.WARRANTY,
                urgency = urgency,
                date = w.expiresOn,
            )
        }

    /**
     * Ni lo que está al día ni lo ya terminado reclaman nada. Son dos estados
     * distintos —uno vuelve, el otro no— pero coinciden en lo único que decide
     * si entran aquí: no hay nada que hacer con ellos.
     */
    private fun maintenance(data: VidaData, today: LocalDate): List<AttentionItem> =
        data.maintenance.mapNotNull { m ->
            if (m.status == MaintenanceStatus.AL_DIA || m.status == MaintenanceStatus.HECHO) {
                return@mapNotNull null
            }
            val urgency = urgencyOf(m.nextDueOn, today) ?: return@mapNotNull null
            AttentionItem(
                id = "maintenance:${m.id}",
                title = m.item,
                reason = when (urgency) {
                    AttentionUrgency.OVERDUE -> "Tocaba ${past(m.nextDueOn, today)}"
                    AttentionUrgency.TODAY -> "Toca hoy"
                    AttentionUrgency.SOON -> "Toca ${future(m.nextDueOn, today)}"
                },
                source = AttentionSource.MAINTENANCE,
                urgency = urgency,
                date = m.nextDueOn,
            )
        }

    /**
     * Un seguimiento tiene DIRECCIÓN, y eso cambia quién tiene que moverse: uno
     * es algo que debes tú, el otro algo que estás esperando. Decirlo igual
     * borraría la única distinción que hace útil a la sección.
     */
    private fun commitments(data: VidaData, today: LocalDate): List<AttentionItem> =
        data.commitments.mapNotNull { cmt ->
            if (cmt.status == "DONE") return@mapNotNull null
            val date = cmt.dueOn ?: return@mapNotNull null
            val urgency = urgencyOf(date, today) ?: return@mapNotNull null
            val mine = cmt.direction == "MINE"
            AttentionItem(
                id = "commitment:${cmt.id}",
                title = cmt.description,
                reason = when {
                    urgency == AttentionUrgency.OVERDUE && mine -> "Lo debías ${past(date, today)}"
                    urgency == AttentionUrgency.OVERDUE -> "Lo esperas desde ${past(date, today)}"
                    urgency == AttentionUrgency.TODAY && mine -> "Lo debes hoy"
                    urgency == AttentionUrgency.TODAY -> "Lo esperas para hoy"
                    mine -> "Lo debes ${future(date, today)}"
                    else -> "Lo esperas ${future(date, today)}"
                },
                source = AttentionSource.COMMITMENT,
                urgency = urgency,
                date = date,
            )
        }

    /** Una rutina en pausa no reclama: pausarla es justamente decir «ahora no». */
    private fun routines(data: VidaData, today: LocalDate): List<AttentionItem> =
        data.routines.mapNotNull { r ->
            if (!r.active) return@mapNotNull null
            val urgency = urgencyOf(r.nextExecutionDate, today) ?: return@mapNotNull null
            AttentionItem(
                id = "routine:${r.id}",
                title = r.title,
                reason = when (urgency) {
                    AttentionUrgency.OVERDUE -> "Tocaba ${past(r.nextExecutionDate, today)}"
                    AttentionUrgency.TODAY -> "Toca hoy"
                    AttentionUrgency.SOON -> "Toca ${future(r.nextExecutionDate, today)}"
                },
                source = AttentionSource.ROUTINE,
                urgency = urgency,
                date = r.nextExecutionDate,
            )
        }

    /**
     * Lo que OTRA PERSONA espera de ti (ADR-025).
     *
     * Es la única fuente sin fecha propia: `SharedResource` guarda la etiqueta
     * del recurso, no un plazo. Por eso no se le inventa una — entra siempre
     * como TODAY, que es lo que significa «te toca»: está esperando desde que
     * te lo compartieron. Sin fecha no se puede decir «lleva tres días», y
     * decirlo sería inventar.
     */
    private fun shared(data: VidaData, today: LocalDate): List<AttentionItem> =
        data.sharedWithMe.mapNotNull { s ->
            if (!s.responsibility || s.partDone) return@mapNotNull null
            AttentionItem(
                id = "shared:${s.id}",
                title = s.label,
                reason = "${s.counterpart} espera tu parte",
                source = AttentionSource.SHARED,
                urgency = AttentionUrgency.TODAY,
                date = today,
            )
        }

    /** Fuera de la ventana no es atención: es archivo. */
    private fun urgencyOf(date: LocalDate, today: LocalDate): AttentionUrgency? = when {
        date.isBefore(today) -> AttentionUrgency.OVERDUE
        date == today -> AttentionUrgency.TODAY
        ChronoUnit.DAYS.between(today, date) <= SOON_DAYS -> AttentionUrgency.SOON
        else -> null
    }
}

/** «ayer», «hace 3 días», «el 12 de agosto» — cuánto lleva esperando. */
private fun past(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days == 1L -> "ayer"
        days <= 6L -> "hace $days días"
        else -> "el ${date.format(DAY_MONTH)}"
    }
}

/** «mañana», «en 3 días» — cuánto falta. Dentro de la semana siempre. */
private fun future(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return if (days == 1L) "mañana" else "en $days días"
}
