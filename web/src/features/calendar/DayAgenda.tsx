import { useState, type DragEvent } from 'react'

import type { Warranty } from '../warranties/api'
import type { MaintenanceRecord } from '../maintenance/api'
import type { Reminder } from '../reminders/api'
import { findIconOption, findStickerOption } from '../../core/ui/pickers/pickerCatalog'
import { dateKey as buildDateKey } from '../../core/ui/components/calendarDate'
import { useVisualTheme } from '../../core/theme/VisualThemeContext'

import {
  formatDueAtTime,
  generateTimeSlots,
  getMinutesFromMidnight,
  reminderTone,
} from './calendarHelpers'

import styles from './CalendarPage.module.css'

interface DayAgendaProps {
  dateKey: string

  reminders: Reminder[]

  warranties?: Warranty[]
  maintenanceRecords?: MaintenanceRecord[]

  draggedReminderId: string | null
  dragOverSlot: string | null

  onDragStart: (
    event: DragEvent<HTMLDivElement>,
    reminderId: string,
  ) => void

  onDragOver: (
    event: DragEvent<HTMLDivElement>,
    slotKey: string,
  ) => void

  onDrop: (
    event: DragEvent<HTMLDivElement>,
    dateKeyValue: string,
    hour: number,
    minute: number,
  ) => void

  onDragEnd: () => void

  completeReminderAction: (
    reminder: Reminder,
  ) => void

  onOpenReminder?: (
    reminder: Reminder,
  ) => void

  compact?: boolean

  selected?: boolean

  onSelect?: () => void
}

export function DayAgenda({
  dateKey,
  reminders,
  warranties = [],
  maintenanceRecords = [],
  draggedReminderId,
  dragOverSlot,
  onDragStart,
  onDragOver,
  onDrop,
  onDragEnd,
  completeReminderAction,
  onOpenReminder,
  compact = false,
  selected = false,
  onSelect,
}: DayAgendaProps) {
  const { theme } = useVisualTheme()

  // UX-014: presentational-only — which reminder (if any) is mid "complete"
  // micro-animation right now. Purely a CSS class toggle for the Organic
  // theme's ripple-bloom; never touches completeReminderAction's own
  // request/refresh cycle, which still runs exactly as before.
  const [bloomingId, setBloomingId] = useState<string | null>(null)

  function handleComplete(reminder: Reminder) {
    if (theme === 'organic' && reminder.status !== 'COMPLETED') {
      setBloomingId(reminder.id)
      window.setTimeout(() => {
        setBloomingId((current) => (current === reminder.id ? null : current))
      }, 650)
    }
    completeReminderAction(reminder)
  }

  const today = new Date()
  const todayKey = buildDateKey(today.getFullYear(), today.getMonth(), today.getDate())
  const isToday = dateKey === todayKey

  const dayReminders =
    reminders.filter(
      (reminder) => {
        const dueAt = reminder.dueAt

        return (
          dueAt &&
          dueAt.slice(0, 10) ===
            dateKey
        )
      },
    )

  const dayWarranties =
    warranties.filter(
      (warranty) =>
        warranty.expiresAt ===
          dateKey &&
        warranty.status !==
          'COMPLETADO',
    )

  const dayMaintenance =
    maintenanceRecords.filter(
      (record) =>
        record.nextDueAt ===
          dateKey &&
        record.status !==
          'COMPLETADO',
    )

  const dayItemsCount =
    dayReminders.length +
    dayWarranties.length +
    dayMaintenance.length

  const timeSlots =
    generateTimeSlots(
      dateKey,
    )

  /**
   * Corrección de visualización (2026-08-18): Semana ya no recorre las 32
   * franjas fijas de 30 min (06:00-22:00) — eso era la "estructura de
   * horarios permanente" que se pidió quitar. En su lugar, lista
   * directamente las tareas del día, ordenadas por hora, con la hora como
   * parte del propio texto de la tarea ("08:00 — Título"). Día no se toca:
   * sigue usando `timeSlots` más abajo, sin cambios.
   */
  const sortedDayReminders = compact
    ? [...dayReminders].sort((a, b) => {
        const aDue = a.dueAt ?? ''
        const bDue = b.dueAt ?? ''
        return aDue.localeCompare(bDue)
      })
    : []

  const fullLabel =
    new Date(
      `${dateKey}T12:00:00`,
    ).toLocaleDateString(
      'es-MX',
      {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
      },
    )

  const shortLabel =
    new Date(
      `${dateKey}T12:00:00`,
    ).toLocaleDateString(
      'es-MX',
      {
        weekday: 'short',
        day: 'numeric',
      },
    )

  // UX-014: only consumed by the Editorial/Premium Minimal themes'
  // "page-date hero" (a real standalone day-of-month numeral, styled far
  // larger than any other element on the page) — CSS hides this block
  // entirely for the other two themes, which keep the existing
  // eyebrow+title treatment unchanged.
  const dayOfMonth = Number(dateKey.slice(-2))
  const weekdayLabel = new Date(`${dateKey}T12:00:00`).toLocaleDateString('es-MX', { weekday: 'long' })
  const monthLabel = new Date(`${dateKey}T12:00:00`).toLocaleDateString('es-MX', { month: 'long', year: 'numeric' })

  return (
    <div
      data-today={isToday ? 'true' : undefined}
      className={`
        ${styles.dayColumn}
        ${
          compact
            ? styles.dayColumnCompact
            : ''
        }
        ${
          compact &&
          selected
            ? styles.dayColumnSelected
            : ''
        }
      `}
    >
      {compact ? (
        <button
          type="button"
          data-today={isToday ? 'true' : undefined}
          className={
            styles.weekColumnHeader
          }
          onClick={onSelect}
          aria-pressed={
            selected
          }
          aria-current={
            selected
              ? 'date'
              : undefined
          }
        >
          <span
            className={
              styles.weekColumnWeekday
            }
          >
            {shortLabel}
          </span>

          {dayItemsCount >
            0 && (
            <span
              className={
                styles.weekColumnCount
              }
            >
              {dayItemsCount}
            </span>
          )}
        </button>
      ) : (
        <div
          className={
            styles.dayHeader
          }
        >
          <div>
            <div className={styles.pageDate}>
              <span className={styles.pageDateNum}>{dayOfMonth}</span>
              <div className={styles.pageDateMeta}>
                <span className={styles.pageDateDow}>{weekdayLabel}</span>
                <span className={styles.pageDateMonth}>{monthLabel}</span>
              </div>
            </div>

            <p
              className={
                styles.dayEyebrow
              }
            >
              AGENDA DEL DÍA
            </p>

            <h2
              className={
                styles.dayTitle
              }
            >
              {fullLabel}
            </h2>
          </div>

          <span
            className={
              styles.dayCount
            }
          >
            {dayItemsCount}{' '}
            {dayItemsCount ===
            1
              ? 'actividad'
              : 'actividades'}
          </span>
        </div>
      )}

      {!compact &&
        dayReminders.length >
          0 && (
          <div
            className={
              styles.dragHint
            }
          >
            <span>↕</span>

            Arrastra una tarea para
            cambiar su horario
          </div>
        )}

      {compact ? (
        <div className={styles.agendaList}>
          {sortedDayReminders.map((reminder) => {
            const dueAt = reminder.dueAt
            const activeIcon = findIconOption(reminder.iconId)
            const sticker = findStickerOption(reminder.stickerId)

            return (
              <div
                key={reminder.id}
                data-tone={reminder.status === 'COMPLETED' ? 'success' : reminderTone(reminder)}
                data-context={reminder.context ?? undefined}
                className={`
                  ${styles.agendaItem}
                  ${reminder.status === 'COMPLETED' ? styles.timelineCompleted : ''}
                `}
              >
                <span className={styles.agendaItemTime}>
                  {dueAt ? formatDueAtTime(dueAt) : '--:--'}
                </span>

                <span className={styles.agendaItemDash} aria-hidden="true">
                  —
                </span>

                {sticker && <img src={sticker.asset} alt="" className={styles.agendaItemSticker} />}
                {!sticker && activeIcon && (
                  <span className={styles.agendaItemIcon}>
                    <activeIcon.Icon width={13} height={13} />
                  </span>
                )}

                {onOpenReminder ? (
                  <button
                    type="button"
                    className={styles.agendaItemTitleButton}
                    onClick={() => onOpenReminder(reminder)}
                  >
                    {reminder.title}
                  </button>
                ) : (
                  <h3 className={styles.agendaItemTitle}>{reminder.title}</h3>
                )}

                {reminder.status !== 'COMPLETED' && (
                  <button
                    type="button"
                    className={`${styles.completeButtonCompact} ${bloomingId === reminder.id ? styles.blooming : ''}`}
                    aria-label={`Completar "${reminder.title}"`}
                    onClick={(event) => {
                      event.stopPropagation()
                      handleComplete(reminder)
                    }}
                  >
                    ✓
                  </button>
                )}
              </div>
            )
          })}
        </div>
      ) : (
      <div
        className={styles.timeline}
      >
        {timeSlots.map(
          (slot) => {
            const slotMinutes =
              slot.hour * 60 +
              slot.minute

            const reminder =
              dayReminders.find(
                (item) => {
                  const dueAt = item.dueAt

                  if (!dueAt) {
                    return false
                  }

                  const itemMinutes =
                    getMinutesFromMidnight(
                      dueAt,
                    )

                  return (
                    itemMinutes >=
                      slotMinutes &&
                    itemMinutes <
                      slotMinutes +
                        30
                  )
                },
              )

            const isDropTarget =
              dragOverSlot ===
              slot.key

            return (
              <div
                key={
                  slot.key
                }
                className={`
                  ${styles.timeSlot}
                  ${
                    compact
                      ? styles.timeSlotCompact
                      : ''
                  }
                  ${
                    isDropTarget
                      ? styles.timeSlotActive
                      : ''
                  }
                `}
                onDragOver={(
                  event,
                ) =>
                  onDragOver(
                    event,
                    slot.key,
                  )
                }
                onDrop={(
                  event,
                ) =>
                  onDrop(
                    event,
                    dateKey,
                    slot.hour,
                    slot.minute,
                  )
                }
              >
                <div
                  className={
                    styles.timeLabel
                  }
                >
                  {slot.minute ===
                    0 &&
                    `${String(
                      slot.hour,
                    ).padStart(
                      2,
                      '0',
                    )}:00`}
                </div>

                <div
                  className={
                    styles.timeRail
                  }
                >
                  <span
                    className={
                      styles.timeRailLine
                    }
                  />
                </div>

                <div
                  className={
                    styles.timeContent
                  }
                >
                  {reminder && (
                    <div
                      draggable
                      data-tone={reminder.status === 'COMPLETED' ? 'success' : reminderTone(reminder)}
                      data-context={reminder.context ?? undefined}
                      onDragStart={(
                        event,
                      ) =>
                        onDragStart(
                          event,
                          reminder.id,
                        )
                      }
                      onDragEnd={
                        onDragEnd
                      }
                      className={`
                        ${styles.timelineCard}
                        ${
                          compact
                            ? styles.timelineCardCompact
                            : ''
                        }
                        ${
                          draggedReminderId ===
                          reminder.id
                            ? styles.timelineCardDragging
                            : ''
                        }
                        ${
                          reminder.status ===
                          'COMPLETED'
                            ? styles.timelineCompleted
                            : ''
                        }
                      `}
                    >
                      {!compact && (
                        <div
                          className={
                            styles.timelineCardHandle
                          }
                          aria-hidden="true"
                        >
                          ⋮⋮
                        </div>
                      )}

                      <div
                        className={
                          styles.timelineCardContent
                        }
                      >
                        <div
                          className={
                            styles.timelineCardTime
                          }
                        >
                          {reminder.dueAt
                            ? formatDueAtTime(reminder.dueAt)
                            : '--:--'}
                        </div>

                        <div className={styles.timelineCardTitleRow}>
                          {(() => {
                            const sticker = findStickerOption(reminder.stickerId)
                            const activeIcon = findIconOption(reminder.iconId)
                            if (sticker) {
                              return <img src={sticker.asset} alt="" className={styles.timelineCardSticker} />
                            }
                            if (activeIcon) {
                              return (
                                <span className={styles.timelineCardIcon}>
                                  <activeIcon.Icon width={14} height={14} />
                                </span>
                              )
                            }
                            return null
                          })()}
                          {onOpenReminder ? (
                            <button
                              type="button"
                              className={styles.timelineCardTitleButton}
                              onClick={(event) => {
                                event.stopPropagation()
                                onOpenReminder(reminder)
                              }}
                            >
                              {reminder.title}
                            </button>
                          ) : (
                            <h3>{reminder.title}</h3>
                          )}
                        </div>

                        {!compact && reminder.description && (
                          <p className={styles.timelineCardDescription}>
                            {reminder.description}
                          </p>
                        )}

                        {!compact && (
                          <div
                            className={
                              styles.timelineCardMeta
                            }
                          >
                            {reminder.status ===
                            'COMPLETED'
                              ? '✓ Completada'
                              : reminderTone(
                                    reminder,
                                  ) ===
                                  'error'
                                ? 'Vencida'
                                : 'Pendiente'}
                          </div>
                        )}
                      </div>

                      {reminder.status !==
                        'COMPLETED' && (
                        <button
                          type="button"
                          className={`
                            ${styles.completeButton}
                            ${
                              compact
                                ? styles.completeButtonCompact
                                : ''
                            }
                            ${bloomingId === reminder.id ? styles.blooming : ''}
                          `}
                          aria-label={
                            compact
                              ? `Completar "${reminder.title}"`
                              : undefined
                          }
                          onClick={(
                            event,
                          ) => {
                            event.stopPropagation()

                            handleComplete(
                              reminder,
                            )
                          }}
                        >
                          {compact
                            ? '✓'
                            : 'Completar'}
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            )
          },
        )}
      </div>
      )}

      {!compact &&
        dayItemsCount ===
          0 && (
          <div
            className={
              styles.dayEmptyState
            }
          >
            <div
              className={
                styles.emptyIcon
              }
            >
              ✓
            </div>

            <h3>
              Un día tranquilo
            </h3>

            <p>
              No tienes actividades
              programadas para este
              día.
            </p>
          </div>
        )}
    </div>
  )
}