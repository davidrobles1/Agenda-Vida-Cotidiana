import { dateKey } from '../../core/ui/components/calendarDate'
import type { CalendarMarker } from '../../core/ui/components/CalendarView'
import type { Reminder } from '../reminders/api'

export function isoDateKey(iso: string): string {
  return iso.slice(0, 10)
}

export function formatDueAtTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-MX', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function reminderTone(reminder: Reminder): CalendarMarker['tone'] {
  if (reminder.status === 'COMPLETED') {
    return 'success'
  }

  if (
    reminder.dueAt &&
    new Date(reminder.dueAt).getTime() < Date.now()
  ) {
    return 'error'
  }

  return 'primary'
}

/**
 * Convierte una fecha dateKey en ISO manteniendo el día seleccionado
 * y aplicando la hora indicada.
 *
 * Se mantiene como comportamiento local/mock para drag & drop.
 */
export function buildDateTime(
  dateKeyValue: string,
  hour: number,
  minute: number,
): string {
  const [year, month, day] = dateKeyValue.split('-').map(Number)

  const date = new Date(
    year,
    month - 1,
    day,
    hour,
    minute,
    0,
    0,
  )

  return date.toISOString()
}

export function formatHour(hour: number): string {
  return `${String(hour).padStart(2, '0')}:00`
}

export function getMinutesFromMidnight(iso: string): number {
  const date = new Date(iso)

  return (
    date.getHours() * 60 +
    date.getMinutes()
  )
}

export interface TimeSlot {
  hour: number
  minute: number
  key: string
}

/**
 * Genera las franjas horarias utilizadas por las vistas Día y Semana.
 *
 * Se mantiene el rango existente:
 * 06:00 -> 22:00
 * cada 30 minutos.
 */
export function generateTimeSlots(
  dateKeyValue: string,
): TimeSlot[] {
  const slots: TimeSlot[] = []

  for (
    let minutes = 6 * 60;
    minutes <= 22 * 60;
    minutes += 30
  ) {
    const hour = Math.floor(
      minutes / 60,
    )

    const minute =
      minutes % 60

    slots.push({
      hour,
      minute,
      key: `${dateKeyValue}-${hour}-${minute}`,
    })
  }

  return slots
}

/**
 * Obtiene los 7 días de la semana que contiene
 * el dateKey indicado.
 *
 * Lunes -> domingo.
 */
export function weekDatesFor(
  dateKeyValue: string,
): string[] {
  const [
    year,
    month,
    day,
  ] = dateKeyValue
    .split('-')
    .map(Number)

  const date = new Date(
    year,
    month - 1,
    day,
  )

  const jsDay =
    date.getDay()

  const mondayOffset =
    jsDay === 0
      ? -6
      : 1 - jsDay

  const monday =
    new Date(date)

  monday.setDate(
    date.getDate() +
      mondayOffset,
  )

  const days: string[] = []

  for (
    let i = 0;
    i < 7;
    i += 1
  ) {
    const current =
      new Date(monday)

    current.setDate(
      monday.getDate() + i,
    )

    days.push(
      dateKey(
        current.getFullYear(),
        current.getMonth(),
        current.getDate(),
      ),
    )
  }

  return days
}

/**
 * Desplaza una fecha por cantidad de días.
 */
export function shiftDateKey(
  dateKeyValue: string,
  deltaDays: number,
): string {
  const [
    year,
    month,
    day,
  ] = dateKeyValue
    .split('-')
    .map(Number)

  const date = new Date(
    year,
    month - 1,
    day,
  )

  date.setDate(
    date.getDate() +
      deltaDays,
  )

  return dateKey(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
  )
}