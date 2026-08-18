import { IconChevronLeft, IconChevronRight } from '../icons'
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
  onPrevMonth: () => void
  onNextMonth: () => void
}

const WEEKDAYS = ['L', 'M', 'M', 'J', 'V', 'S', 'D']

function dateKey(year: number, month: number, day: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function monthLabel(date: Date): string {
  const label = date.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

/**
 * UX-007: reusable month-grid calendar — Web counterpart of Android's
 * `CalendarView.kt` (`design-system.md` §7). Purely presentational: month
 * navigation + a 7-column day grid with up to 3 small tone-colored dots per
 * day. The caller (`CalendarPage.tsx`) owns what a marker *means* (real
 * reminder vs. mock garantía/mantenimiento) — this component only draws
 * dots, it never fetches or interprets data. No date-library dependency:
 * plain `Date` math, the same call already made for the rest of Web
 * (`useHomeData.ts`, `RemindersPage.tsx`).
 */
export function CalendarView({ month, markersByDay, onPrevMonth, onNextMonth }: CalendarViewProps) {
  const year = month.getFullYear()
  const monthIndex = month.getMonth()
  const firstOfMonth = new Date(year, monthIndex, 1)
  const leadingBlanks = (firstOfMonth.getDay() + 6) % 7 // Sunday=0..Saturday=6 -> Monday-first 0..6
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const todayKey = dateKey(new Date().getFullYear(), new Date().getMonth(), new Date().getDate())

  const cells: Array<{ day: number; key: string } | null> = []
  for (let i = 0; i < leadingBlanks; i++) cells.push(null)
  for (let day = 1; day <= daysInMonth; day++) cells.push({ day, key: dateKey(year, monthIndex, day) })
  while (cells.length % 7 !== 0) cells.push(null)

  return (
    <div className={styles.calendar}>
      <div className={styles.header}>
        <button type="button" className={styles.navButton} onClick={onPrevMonth} aria-label="Mes anterior">
          <IconChevronLeft width={18} height={18} />
        </button>
        <span className={styles.monthLabel}>{monthLabel(month)}</span>
        <button type="button" className={styles.navButton} onClick={onNextMonth} aria-label="Mes siguiente">
          <IconChevronRight width={18} height={18} />
        </button>
      </div>

      <div className={styles.grid}>
        {WEEKDAYS.map((weekday, i) => (
          <span key={i} className={styles.weekday}>
            {weekday}
          </span>
        ))}
        {cells.map((cell, i) => {
          if (!cell) return <span key={i} className={styles.dayCell} />
          const markers = markersByDay[cell.key] ?? []
          const isToday = cell.key === todayKey
          return (
            <span key={i} className={`${styles.dayCell} ${isToday ? styles.today : ''}`}>
              <span className={styles.dayNumber}>{cell.day}</span>
              {markers.length > 0 && (
                <span className={styles.markers}>
                  {markers.slice(0, 3).map((marker, m) => (
                    <span key={m} className={styles.marker} data-tone={marker.tone} />
                  ))}
                </span>
              )}
            </span>
          )
        })}
      </div>
    </div>
  )
}
