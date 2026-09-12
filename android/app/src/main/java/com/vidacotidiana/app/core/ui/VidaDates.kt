package com.vidacotidiana.app.core.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * CÓMO SE ESCRIBE UNA FECHA EN ESTA APLICACIÓN. Una vez, aquí.
 *
 * EL PROBLEMA QUE RESUELVE. Había siete `DateTimeFormatter` privados repartidos
 * en siete archivos, más un par de funciones encerradas en `AttentionEngine`, y
 * ninguna pantalla podía consultar lo que hacía otra. Las consecuencias eran
 * visibles: el detalle de un mantenimiento mostraba el mismo día dos veces y de
 * dos maneras —«08 sept 2026» arriba y `2026-09-08` abajo, en formato de base
 * de datos—, y el detalle de una tarea anteponía «Hoy» a cualquier hora porque
 * la palabra estaba escrita a mano, sin mirar la fecha.
 *
 * LA REGLA. El texto de una fecha SE DERIVA de su relación con hoy; no se
 * decide en el sitio donde se pinta. Quien tenga una fecha pide aquí cómo se
 * escribe, y así una fecha pasada no puede titularse «próxima» ni una de la
 * semana pasada llamarse «hoy».
 */
enum class TemporalState { PASADO, HOY, FUTURO }

object VidaDates {

    /** «08 sept 2026». La forma larga, para cuando importa el día exacto. */
    private val ABSOLUTE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es"))

    /** «12 de agosto». Sin año, para lo que cae cerca. */
    private val DAY_MONTH: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"))

    /** Dónde cae la fecha respecto a hoy. El resto del objeto se apoya en esto. */
    fun stateOf(date: LocalDate, today: LocalDate = LocalDate.now()): TemporalState = when {
        date.isBefore(today) -> TemporalState.PASADO
        date == today -> TemporalState.HOY
        else -> TemporalState.FUTURO
    }

    /** «08 sept 2026», con la inicial en mayúscula como el resto de la interfaz. */
    fun absolute(date: LocalDate): String =
        date.format(ABSOLUTE).replaceFirstChar { it.uppercase() }

    /**
     * «Hoy», «Ayer», «Mañana», «Hace 3 días», «En 3 días», «El 12 de agosto».
     *
     * Cerca se cuenta en días porque es como se piensa; lejos se dice la fecha,
     * porque «hace 47 días» no sitúa a nadie.
     */
    fun relative(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days == 0L -> "Hoy"
            days == -1L -> "Ayer"
            days == 1L -> "Mañana"
            days in -6..-2 -> "Hace ${-days} días"
            days in 2..6 -> "En $days días"
            else -> "El ${date.format(DAY_MONTH)}"
        }
    }

    /**
     * La fecha Y su hora, con la palabra que de verdad le corresponde.
     *
     * Es lo que sustituye al `"Hoy " + hora` escrito a mano: aquí «Hoy» sale
     * de que la fecha SEA hoy, y una tarea de la semana pasada dice cuándo fue.
     */
    fun dateTime(date: LocalDate?, time: LocalTime?, today: LocalDate = LocalDate.now()): String {
        val hhmm = time?.toString()?.take(5)
        if (date == null) return hhmm ?: "Sin hora"
        val day = relative(date, today)
        return if (hhmm == null) day else "$day, $hhmm"
    }

    /**
     * Cómo se titula un plazo según ya haya pasado o no.
     *
     * Existe porque el detalle de mantenimiento rotulaba «PRÓXIMA» una fecha
     * pasada y luego añadía «ya pasó» detrás, de modo que el propio rótulo se
     * desmentía. El título es el estado; no hace falta corregirlo después.
     */
    fun dueTitle(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        when (stateOf(date, today)) {
            TemporalState.PASADO -> "TOCABA"
            TemporalState.HOY -> "ES HOY"
            TemporalState.FUTURO -> "PRÓXIMA"
        }
}
