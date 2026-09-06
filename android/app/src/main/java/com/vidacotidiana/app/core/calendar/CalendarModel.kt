package com.vidacotidiana.app.core.calendar

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * ADR-018 — LAS ALERTAS SE DERIVAN, NO SE ALMACENAN.
 *
 * Calco funcional de `web/src/features/calendar/alerts/dateAlerts.ts`. No hay
 * tabla de alertas, ni endpoint, ni id de base de datos: una alerta es una
 * VISTA de una garantía, un mantenimiento o un pago. Esa decisión da gratis
 * cuatro propiedades que con alertas guardadas serían cuatro problemas: si
 * cambia la fecha del registro la alerta se mueve sola, no puede duplicarse,
 * queda vinculada a su origen, y nunca acaba en la lista de tareas.
 *
 * REGLA DE COLOR HEREDADA (2026-08-28, petición explícita del usuario): el
 * rojo NO se usa para una alerta. Está reservado a lo que está mal —una tarea
 * vencida, un fallo—; una alerta de fecha avisa de algo que todavía no ocurre.
 * La jerarquía se sostiene con tres severidades que sí se distinguen entre sí.
 */

enum class AlertSeverity(val rank: Int, val label: String) {
    HIGH(0, "Alta"),
    MEDIUM(1, "Media"),
    LOW(2, "Baja"),
}

enum class AlertSource(val label: String, val route: String) {
    WARRANTY("Garantía", "warranties"),
    MAINTENANCE("Mantenimiento", "maintenance"),
    SUBSCRIPTION("Pago", "payments"),
}

/** Una alerta derivada. `id` es determinista: mismo hecho y misma fecha, mismo id. */
data class DateAlert(
    val date: LocalDate,
    val severity: AlertSeverity,
    val source: AlertSource,
    /** Id del registro de origen, para volver a él. */
    val sourceId: String,
    val label: String,
    val message: String,
    val amount: String? = null,
) {
    val id: String get() = "${source.name}:$sourceId:$date:${severity.name}"
}

/** Registro de origen mínimo: lo único que la derivación necesita saber. */
data class AlertSourceRecord(
    val id: String,
    val label: String,
    val date: LocalDate,
    val amount: String? = null,
    /** Solo tarjetas de crédito: día de corte. */
    val statementDay: Int? = null,
    /** Una garantía ya usada deja de avisar. */
    val closed: Boolean = false,
)

/**
 * Los tres calendarios de aviso, juntos y en un solo sitio a propósito: son la
 * regla de negocio de esta funcionalidad y deben poder leerse de un vistazo.
 */
private val WARRANTY_OFFSETS = listOf(30 to AlertSeverity.MEDIUM, 15 to AlertSeverity.MEDIUM, 0 to AlertSeverity.HIGH)
private val MAINTENANCE_OFFSETS = listOf(7 to AlertSeverity.LOW, 3 to AlertSeverity.MEDIUM, 0 to AlertSeverity.HIGH)
private val SUBSCRIPTION_OFFSETS = listOf(5 to AlertSeverity.LOW, 2 to AlertSeverity.MEDIUM, 0 to AlertSeverity.HIGH)

object AlertEngine {

    /**
     * ADR-020: una tarjeta genera DOS avisos por ciclo, no uno. El corte no es
     * una fecha de pago —no hay que pagar nada ese día— sino el momento en que
     * ya se sabe cuánto se debe: avisa una sola vez, el mismo día, y en
     * severidad baja. La alta se reserva al límite, que es cuando no actuar
     * tiene consecuencias.
     */
    fun derive(
        warranties: List<AlertSourceRecord>,
        maintenance: List<AlertSourceRecord>,
        payments: List<AlertSourceRecord>,
    ): List<DateAlert> {
        val out = mutableListOf<DateAlert>()

        warranties.filter { !it.closed }.forEach { r ->
            WARRANTY_OFFSETS.forEach { (days, sev) ->
                out += DateAlert(
                    date = r.date.minusDays(days.toLong()), severity = sev, source = AlertSource.WARRANTY,
                    sourceId = r.id, label = r.label,
                    message = if (days == 0) "Vence hoy" else "Vence en $days días",
                )
            }
        }
        maintenance.filter { !it.closed }.forEach { r ->
            MAINTENANCE_OFFSETS.forEach { (days, sev) ->
                out += DateAlert(
                    date = r.date.minusDays(days.toLong()), severity = sev, source = AlertSource.MAINTENANCE,
                    sourceId = r.id, label = r.label,
                    message = if (days == 0) "Toca hoy" else "Toca en $days ${if (days == 1) "día" else "días"}",
                )
            }
        }
        payments.filter { !it.closed }.forEach { r ->
            SUBSCRIPTION_OFFSETS.forEach { (days, sev) ->
                out += DateAlert(
                    date = r.date.minusDays(days.toLong()), severity = sev, source = AlertSource.SUBSCRIPTION,
                    sourceId = r.id, label = r.label, amount = r.amount,
                    message = if (days == 0) "Se paga hoy" else "Se paga en $days ${if (days == 1) "día" else "días"}",
                )
            }
        }
        payments.filter { !it.closed }.forEach { r ->
            val day = r.statementDay ?: return@forEach
            // El corte anterior al límite, no el siguiente — misma regla que
            // `paymentsView.cardStatementDate` en la Web.
            val candidate = runCatching { r.date.withDayOfMonth(day) }.getOrNull() ?: return@forEach
            val statement = if (candidate.isAfter(r.date)) candidate.minusMonths(1) else candidate
            out += DateAlert(
                date = statement, severity = AlertSeverity.LOW, source = AlertSource.SUBSCRIPTION,
                sourceId = r.id, label = r.label, message = "Corte de la tarjeta",
            )
        }
        return out
    }
}

/** Una tarea del día, con o sin hora. */
data class DayTask(
    val id: String,
    val title: String,
    val time: LocalTime?,
    val meta: String,
    val done: Boolean = false,
    /** Compartida y con parte comprometida (ADR-025). */
    val shared: Boolean = false,
)

/** Lo que un día contiene: los tres ritmos que el calendario superpone. */
data class DayContent(
    val date: LocalDate,
    val tasks: List<DayTask> = emptyList(),
    val alerts: List<DateAlert> = emptyList(),
    val notes: List<String> = emptyList(),
) {
    val total: Int get() = tasks.size + alerts.size + notes.size
    val isEmpty: Boolean get() = total == 0
    val hasUrgent: Boolean get() = alerts.any { it.severity == AlertSeverity.HIGH }

    /** "3 avisos · 2 notas" — sin ceros y en singular cuando corresponde. */
    fun summary(): String {
        val parts = buildList {
            if (tasks.isNotEmpty()) add("${tasks.size} ${if (tasks.size == 1) "tarea" else "tareas"}")
            if (alerts.isNotEmpty()) add("${alerts.size} ${if (alerts.size == 1) "aviso" else "avisos"}")
            if (notes.isNotEmpty()) add("${notes.size} ${if (notes.size == 1) "nota" else "notas"}")
        }
        return if (parts.isEmpty()) "Día libre" else parts.joinToString(" · ")
    }
}

/**
 * Los tramos de la barra de carga de una celda. La celda del artefacto no es un
 * número en una caja: es una pila cuya altura y color dicen cuánto hay y qué es
 * lo más urgente. Se corta en cuatro porque más tramos dejan de leerse.
 */
enum class LoadSegment { TASK, HIGH, MEDIUM, LOW, NOTE }

fun DayContent.load(): List<LoadSegment> = buildList {
    tasks.filter { !it.done }.forEach { add(LoadSegment.TASK) }
    alerts.sortedBy { it.severity.rank }.forEach {
        add(
            when (it.severity) {
                AlertSeverity.HIGH -> LoadSegment.HIGH
                AlertSeverity.MEDIUM -> LoadSegment.MEDIUM
                AlertSeverity.LOW -> LoadSegment.LOW
            },
        )
    }
    if (notes.isNotEmpty()) add(LoadSegment.NOTE)
}.take(4)

/**
 * La matriz de un mes empezando en lunes, con los días de relleno del mes
 * anterior y siguiente. 42 celdas siempre: una rejilla que cambia de alto
 * entre meses hace saltar la composición al deslizar.
 */
data class MonthCell(val date: LocalDate, val inMonth: Boolean)

fun monthMatrix(month: YearMonth): List<MonthCell> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value + 6) % 7 // lunes = 0
    val start = first.minusDays(lead.toLong())
    return (0 until 42).map { i ->
        val d = start.plusDays(i.toLong())
        MonthCell(d, YearMonth.from(d) == month)
    }
}

/** La semana (lunes a domingo) que contiene una fecha. */
fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.minusDays(((date.dayOfWeek.value + 6) % 7).toLong())
    return (0 until 7).map { monday.plusDays(it.toLong()) }
}

/** Los siguientes días que traen algo. Alimenta la cinta de "lo que viene". */
fun upcomingDays(from: LocalDate, max: Int, content: (LocalDate) -> DayContent): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    var i = 1L
    while (i <= 60 && out.size < max) {
        val d = from.plusDays(i)
        if (!content(d).isEmpty) out += d
        i++
    }
    return out
}
