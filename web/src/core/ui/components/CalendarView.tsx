import { IconChevronLeft, IconChevronRight } from '../icons'
import { dateKey } from './calendarDate'
import styles from './CalendarView.module.css'
import type { Tone } from './MetricCard'

export interface CalendarMarker {
  tone: Tone
}

export interface CalendarLegendEntry {
  label: string
  tone: Tone
}

interface CalendarViewProps {
  month: Date
  markersByDay: Record<string, CalendarMarker[]>
  selectedDateKey: string
  onSelectDate: (dateKey: string) => void
  onPrevMonth: () => void
  onNextMonth: () => void
  onToday: () => void
}

const WEEKDAYS = ['L', 'M', 'M', 'J', 'V', 'S', 'D']

function monthLabel(date: Date): string {
  const label = date.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

/**
 * UX-007/UX-010: reusable month-grid calendar — Web counterpart of Android's
 * `CalendarView.kt` (`design-system.md` §7/§10). Purely presentational: month
 * navigation + a "Hoy" shortcut + a 7-column day grid, each day a real button
 * (keyboard/AT accessible, not just a styled `<span>`) that can be selected.
 * Up to 3 tone-colored dots per day mark what's happening. The caller
 * (`CalendarPage.tsx`) owns what a marker/selection *means* — this component
 * only renders the grid and reports interaction, it never fetches or
 * interprets data. No date-library dependency: plain `Date` math, same as
 * the rest of Web.
 */
export function CalendarView({
  month,
  markersByDay,
  selectedDateKey,
  onSelectDate,
  onPrevMonth,
  onNextMonth,
  onToday,
}: CalendarViewProps) {
  const year = month.getFullYear()
  const monthIndex = month.getMonth()
  const firstOfMonth = new Date(year, monthIndex, 1)
  const leadingBlanks = (firstOfMonth.getDay() + 6) % 7 // Sunday=0..Saturday=6 -> Monday-first 0..6
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const now = new Date()
  const todayKey = dateKey(now.getFullYear(), now.getMonth(), now.getDate())

  const cells: Array<{ day: number; key: string } | null> = []
  for (let i = 0; i < leadingBlanks; i++) cells.push(null)
  for (let day = 1; day <= daysInMonth; day++) cells.push({ day, key: dateKey(year, monthIndex, day) })
  while (cells.length % 7 !== 0) cells.push(null)

  return (
    <div className={styles.calendar}>
      <div className={styles.header}>
        <button type="button" className={styles.todayButton} onClick={onToday}>
          Hoy
        </button>
        <div className={styles.monthNav}>
          <button type="button" className={styles.navButton} onClick={onPrevMonth} aria-label="Mes anterior">
            <IconChevronLeft width={18} height={18} />
          </button>
          <span className={styles.monthLabel}>{monthLabel(month)}</span>
          <button type="button" className={styles.navButton} onClick={onNextMonth} aria-label="Mes siguiente">
            <IconChevronRight width={18} height={18} />
          </button>
        </div>
      </div>

      <div className={styles.grid} key={`${year}-${monthIndex}`}>
        {WEEKDAYS.map((weekday, i) => (
          <span key={i} className={styles.weekday}>
            {weekday}
          </span>
        ))}
        {cells.map((cell, i) => {
          if (!cell) return <span key={i} className={styles.dayCell} />
          const markers = markersByDay[cell.key] ?? []
          const isToday = cell.key === todayKey
          const isSelected = cell.key === selectedDateKey
          return (
            <button
              type="button"
              key={i}
              className={`${styles.dayCell} ${styles.dayButton} ${isToday ? styles.today : ''} ${isSelected ? styles.selected : ''}`}
              onClick={() => onSelectDate(cell.key)}
              aria-pressed={isSelected}
              aria-current={isToday ? 'date' : undefined}
            >
              <span className={styles.dayNumber}>{cell.day}</span>
              {markers.length > 0 && (
                <span className={styles.markers}>
                  {markers.slice(0, 3).map((marker, m) => (
                    <span key={m} className={styles.marker} data-tone={marker.tone} />
                  ))}
                </span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
