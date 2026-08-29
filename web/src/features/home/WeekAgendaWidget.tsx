import { useState } from 'react'
import type { Reminder } from '../reminders/api'
import type { DateAlert } from '../calendar/alerts/dateAlerts'
import { IconCheckCircle } from '../../core/ui/icons'
import styles from './WeekAgendaWidget.module.css'

interface WeekAgendaWidgetProps {
  thisWeek: Reminder[]
  /**
   * 2026-08-29 (petición 2.2): "aplicar la misma consistencia a la vista
   * Semanal de Inicio". Este widget solo mostraba recordatorios, así que una
   * garantía o una suscripción que vencía esa semana se veía en el
   * Calendario y aquí no. Agrupadas por día (`YYYY-MM-DD`), igual que en
   * `useCalendarData.alertsByDay`.
   */
  alertsByDay?: Record<string, DateAlert[]>
}

const WEEKDAY_LABELS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const WEEKDAY_FULL_LABELS = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
  'Domingo',
]

function mondayOf(date: Date): Date {
  const day = date.getDay()
  const diffToMonday = day === 0 ? -6 : 1 - day

  return new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate() + diffToMonday,
  )
}

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

function getActivityCountForDay(
  reminders: Reminder[],
  date: Date,
): number {
  return reminders.filter((reminder) => {
    if (!reminder.dueAt) {
      return false
    }

    return isSameDay(new Date(reminder.dueAt), date)
  }).length
}

function getVisibleDotCount(count: number): number {
  return Math.min(count, 3)
}

function dateKeyOf(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function WeekAgendaWidget({
  thisWeek,
  alertsByDay = {},
}: WeekAgendaWidgetProps) {
  const today = new Date()
  const monday = mondayOf(today)

  const [selectedDate, setSelectedDate] = useState<Date>(today)

  /*
  * ─────────────────────────────────────────────
  * Días de la semana actual
  * ─────────────────────────────────────────────
  */

  const weekDays = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(
      monday.getFullYear(),
      monday.getMonth(),
      monday.getDate() + index,
    )

    const count =
      getActivityCountForDay(thisWeek, date) + (alertsByDay[dateKeyOf(date)]?.length ?? 0)

    return {
      date,
      count,
      isToday: isSameDay(date, today),
      label: WEEKDAY_LABELS[index],
      fullLabel: WEEKDAY_FULL_LABELS[index],
    }
  })

  /*
  * ─────────────────────────────────────────────
  * Recordatorios filtrados para el día seleccionado
  * ─────────────────────────────────────────────
  */

  const selectedDayReminders = thisWeek.filter((reminder) => {
    if (!reminder.dueAt) {
      return false
    }

    return isSameDay(new Date(reminder.dueAt), selectedDate)
  })

  const selectedDayInfo = weekDays.find((day) =>
    isSameDay(day.date, selectedDate),
  )

  const selectedDayAlerts = alertsByDay[dateKeyOf(selectedDate)] ?? []

  return (
    <div className={styles.widget}>
      {/* NAVEGACIÓN HORIZONTAL POR DÍAS */}
      <div
        className={styles.weekHeader}
        role="tablist"
        aria-label="Seleccionar día de la semana"
      >
        <div className={styles.days}>
          {weekDays.map((day) => {
            const isSelected = isSameDay(day.date, selectedDate)
            const visibleDotCount = getVisibleDotCount(day.count)

            return (
              <button
                key={day.date.toISOString()}
                type="button"
                role="tab"
                aria-selected={isSelected}
                className={`${styles.dayCell} ${
                  day.isToday ? styles.dayCellToday : ''
                } ${isSelected ? styles.dayCellSelected : ''}`}
                onClick={() => setSelectedDate(day.date)}
                aria-label={`${day.fullLabel} ${day.date.getDate()}${
                  day.count > 0
                    ? `, ${day.count} ${
                        day.count === 1 ? 'pendiente' : 'pendientes'
                      }`
                    : ''
                }`}
              >
                <span className={styles.dayLabel}>{day.label}</span>

                <span className={styles.dayNumber}>
                  {day.date.getDate()}
                </span>

                {visibleDotCount > 0 && (
                  <span className={styles.dayDots} aria-hidden="true">
                    {Array.from(
                      { length: visibleDotCount },
                      (_, dotIndex) => (
                        <span
                          key={dotIndex}
                          className={`${styles.dayDot} ${
                            day.isToday ? styles.dayDotToday : ''
                          }`}
                        />
                      ),
                    )}
                  </span>
                )}
              </button>
            )
          })}
        </div>
      </div>

      {/* VISTA DE TAREAS DEL DÍA SELECCIONADO */}
      <div className={styles.agenda}>
        <header className={styles.selectedDayHeader}>
          <div className={styles.selectedDateGroup}>
            <span className={styles.selectedWeekday}>
              {selectedDayInfo?.fullLabel}
            </span>
            <span className={styles.selectedDayNumber}>
              {selectedDate.getDate()}
            </span>
          </div>

          <span className={styles.groupCount}>
            {selectedDayReminders.length}{' '}
            {selectedDayReminders.length === 1
              ? 'pendiente'
              : 'pendientes'}
          </span>
        </header>

        {selectedDayReminders.length === 0 && selectedDayAlerts.length === 0 ? (
          <div className={styles.emptyHint}>
            <IconCheckCircle
              width={18}
              height={18}
              aria-hidden="true"
            />
            <span>Sin pendientes para este día.</span>
          </div>
        ) : (
          <ul className={styles.list}>
            {selectedDayReminders.map((reminder) => (
              <li key={reminder.id} className={styles.listItem}>
                <span className={styles.itemMarker} aria-hidden="true" />
                <span
                  className={styles.listItemTitle}
                  title={reminder.title}
                >
                  {reminder.title}
                </span>
                {/* Petición 2.3: el estado, en texto. */}
                <span className={styles.listItemState}>
                  {reminder.status === 'COMPLETED' ? 'Terminada' : 'Pendiente'}
                </span>
              </li>
            ))}

            {selectedDayAlerts.map((alert) => (
              <li key={alert.id} className={styles.listItem} data-alert="true">
                <span className={styles.itemMarker} aria-hidden="true" />
                <span className={styles.listItemTitle} title={alert.sourceLabel}>
                  {alert.sourceLabel}
                </span>
                <span className={styles.listItemState}>{alert.message}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}