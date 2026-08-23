import {
  useMemo,
  useState,
  type DragEvent,
} from 'react'

import {
  ToggleButtonGroup,
  type Key,
} from 'react-aria-components'

import { AppShell } from '../../core/ui/layout/AppShell'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { FilterChip } from '../../core/ui/components/FilterChip'

import {
  CalendarView,
  type CalendarMarker,
} from '../../core/ui/components/CalendarView'

import { MonthSelector } from '../../core/ui/components/MonthSelector'

import { dateKey } from '../../core/ui/components/calendarDate'

import { getLocalTimeZone, today } from '@internationalized/date'

import {
  IconChevronLeft,
  IconChevronRight,
} from '../../core/ui/icons'

import { useCalendarData } from './useCalendarData'
import { DayAgenda } from './DayAgenda'
import { useActiveMode } from '../../core/user/ActiveModeContext'

import { DayNotesCanvas } from './daynotes/DayNotesCanvas'

import { CreateReminderDialog } from './reminders/CreateReminderDialog'
import { ReminderDrawer } from './reminders/ReminderDrawer'

import {
  buildDateTime,
  isoDateKey,
  reminderTone,
  shiftDateKey,
  weekDatesFor,
} from './calendarHelpers'

import type { Reminder } from '../reminders/api'

import styles from './CalendarPage.module.css'

/* =====================================================
   REMINDER PRESENTATION
   ===================================================== */

function reminderContextColor(
  reminder: Reminder,
): string | undefined {
  if (
    reminder.context === 'PERSONAL'
  ) {
    return 'var(--color-terracotta-text)'
  }

  if (
    reminder.context === 'LABORAL'
  ) {
    return 'var(--color-laboral-accent)'
  }

  return undefined
}

/* =====================================================
   RANGE LABELS
   ===================================================== */

function formatWeekRangeLabel(
  weekDates: string[],
): string {
  const start =
    new Date(
      `${weekDates[0]}T12:00:00`,
    )

  const end =
    new Date(
      `${weekDates[6]}T12:00:00`,
    )

  const startLabel =
    start.toLocaleDateString(
      'es-MX',
      {
        day: 'numeric',
        month: 'short',
      },
    )

  const endLabel =
    end.toLocaleDateString(
      'es-MX',
      {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      },
    )

  return `${startLabel} – ${endLabel}`
}

function formatDayNavLabel(
  dateKeyValue: string,
): string {
  return new Date(
    `${dateKeyValue}T12:00:00`,
  ).toLocaleDateString(
    'es-MX',
    {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    },
  )
}

type CalendarRange =
  | 'day'
  | 'week'
  | 'month'

/* =====================================================
   PAGE
   ===================================================== */

export function CalendarPage() {
  const activeMode =
    useActiveMode()

  const state =
    useCalendarData()

  // Pedido explícito del usuario (2026-08-22): selector de mes junto a
  // "Vista mensual" — estado levantado hasta aquí (antes vivía solo
  // dentro de CalendarView) para que el nuevo MonthSelector pueda
  // saltar directo a un mes elegido; CalendarView sigue funcionando
  // exactamente igual para todo lo demás (flechas, swipe, "Hoy").
  const [focusedMonthDate, setFocusedMonthDate] = useState(() => today(getLocalTimeZone()))

  const [activeReminderId, setActiveReminderId] = useState<string | null>(null)
  const activeReminder = state.reminders.find((reminder) => reminder.id === activeReminderId) ?? null

  const scopedReminders =
    useMemo(
      () =>
        activeMode
          ? state.reminders.filter(
              (reminder) =>
                !reminder.context ||
                reminder.context ===
                  activeMode,
            )
          : state.reminders,
      [
        state.reminders,
        activeMode,
      ],
    )

  const todayDate =
    new Date()

  const todayKey =
    dateKey(
      todayDate.getFullYear(),
      todayDate.getMonth(),
      todayDate.getDate(),
    )

  const [
    selectedDateKey,
    setSelectedDateKey,
  ] = useState(
    () => todayKey,
  )

  const [
    rangeSelection,
    setRangeSelection,
  ] = useState<Set<Key>>(
    () =>
      new Set(['month']),
  )

  const activeRange: CalendarRange =
    rangeSelection.has('day')
      ? 'day'
      : rangeSelection.has(
            'week',
          )
        ? 'week'
        : 'month'

  const [
    draggedReminderId,
    setDraggedReminderId,
  ] = useState<
    string | null
  >(null)

  const [
    dragOverSlot,
    setDragOverSlot,
  ] = useState<
    string | null
  >(null)

  /* =====================================================
     MONTH MARKERS
     ===================================================== */

  const markersByDay =
    useMemo(() => {
      const map: Record<
        string,
        CalendarMarker[]
      > = {}

      const add = (
        key: string,
        tone: CalendarMarker['tone'],
        label: string,
        contextColor?: string,
      ) => {
        if (!map[key]) {
          map[key] = []
        }

        map[key].push({
          tone,
          label,
          contextColor,
        })
      }

      scopedReminders.forEach(
        (reminder) => {
          const dueAt = reminder.dueAt

          if (!dueAt) {
            return
          }

          add(
            isoDateKey(dueAt),
            reminderTone(
              reminder,
            ),
            reminder.title,
            activeMode
              ? undefined
              : reminderContextColor(
                  reminder,
                ),
          )
        },
      )

      state.warranties.forEach(
        (warranty) => {
          if (
            warranty.status !==
            'COMPLETADO'
          ) {
            add(
              warranty.expiresAt,
              'warning',
              warranty.item,
            )
          }
        },
      )

      state.maintenanceRecords.forEach(
        (record) => {
          if (
            record.status !==
            'COMPLETADO'
          ) {
            add(
              record.nextDueAt,
              'info',
              record.item,
            )
          }
        },
      )

      return map
    }, [
      scopedReminders,
      state.warranties,
      state.maintenanceRecords,
      activeMode,
    ])

  /* =====================================================
     DRAG & DROP — MOCK
     ===================================================== */

  function handleDragStart(
    event: DragEvent<HTMLDivElement>,
    reminderId: string,
  ) {
    setDraggedReminderId(
      reminderId,
    )

    event.dataTransfer.effectAllowed =
      'move'

    event.dataTransfer.setData(
      'text/plain',
      reminderId,
    )
  }

  function handleDragEnd() {
    setDraggedReminderId(null)
    setDragOverSlot(null)
  }

  function handleDragOver(
    event: DragEvent<HTMLDivElement>,
    slotKey: string,
  ) {
    event.preventDefault()

    event.dataTransfer.dropEffect =
      'move'

    setDragOverSlot(slotKey)
  }

  function handleDrop(
    event: DragEvent<HTMLDivElement>,
    dateKeyValue: string,
    hour: number,
    minute: number,
  ) {
    event.preventDefault()

    const reminderId =
      event.dataTransfer.getData(
        'text/plain',
      )

    if (!reminderId) {
      return
    }

    const reminder =
      state.reminders.find(
        (item) =>
          item.id ===
          reminderId,
      )

    if (!reminder) {
      return
    }

    const newDueAt =
      buildDateTime(
        dateKeyValue,
        hour,
        minute,
      )

    // Real reschedule: same update endpoint the Drawer's edit-save flow
    // already uses (PUT /reminders/{id}, `dueAt` included) — no separate
    // "reschedule" endpoint exists or is needed. Other fields are passed
    // through unchanged since the backend always overwrites the full set.
    state.updateReminderAction(
      reminder,
      {
        title: reminder.title,
        description: reminder.description,
        dueAt: newDueAt,
        iconId: reminder.iconId,
        stickerId: reminder.stickerId,
      },
    )

    setDraggedReminderId(null)
    setDragOverSlot(null)
  }

  /* =====================================================
     WEEK
     ===================================================== */

  const weekDates =
    useMemo(
      () =>
        weekDatesFor(
          selectedDateKey,
        ),
      [selectedDateKey],
    )

  /* =====================================================
     QUICK ADD SECTION — ahora abre el diálogo completo de creación
     (título, descripción, fecha/hora, icono, emoji, sticker) en vez de
     capturar solo un título suelto.
     ===================================================== */

  const quickAddSection = (
    <>
      <div className={styles.quickAdd}>
        <CreateReminderDialog
          defaultDateKey={selectedDateKey}
          onCreate={state.createReminderAction}
        />
      </div>
    </>
  )

  /* =====================================================
     RENDER
     ===================================================== */

  return (
    <AppShell
      title="Calendario"
      subtitle="Organiza tu día, tus garantías y tu mantenimiento en un solo lugar."
    >
      <div
        className={`${styles.page} notebook-bg`}
      >
        {state.loading && (
          <p
            className={
              styles.loading
            }
          >
            Cargando…
          </p>
        )}

        {state.error && (
          <p role="alert">
            {state.error}
          </p>
        )}

        <ToggleButtonGroup
          aria-label="Vista de calendario"
          selectionMode="single"
          disallowEmptySelection
          selectedKeys={
            rangeSelection
          }
          onSelectionChange={
            setRangeSelection
          }
          className={
            styles.viewSelector
          }
        >
          <FilterChip
            id="day"
            label="Día"
          />

          <FilterChip
            id="week"
            label="Semana"
          />

          <FilterChip
            id="month"
            label="Mes"
          />
        </ToggleButtonGroup>

        {/* =====================================================
            MONTH
            ===================================================== */}

        {activeRange ===
          'month' && (
          <ListSectionCard
            title="Vista mensual"
            action={
              <MonthSelector
                focusedDate={focusedMonthDate}
                onSelectMonth={setFocusedMonthDate}
              />
            }
          >
            <div
              className={
                styles.gridPaneFull
              }
            >
              <CalendarView
                markersByDay={
                  markersByDay
                }
                selectedDateKey={
                  selectedDateKey
                }
                onSelectDate={
                  setSelectedDateKey
                }
                focusedDate={focusedMonthDate}
                onFocusedDateChange={setFocusedMonthDate}
              />
            </div>

            {quickAddSection}
          </ListSectionCard>
        )}

        {/* =====================================================
            WEEK
            ===================================================== */}

        {activeRange ===
          'week' && (
          <ListSectionCard title="Vista semanal">
            <div
              className={
                styles.rangeNav
              }
            >
              <button
                type="button"
                className={
                  styles.rangeNavButton
                }
                aria-label="Semana anterior"
                onClick={() =>
                  setSelectedDateKey(
                    shiftDateKey(
                      selectedDateKey,
                      -7,
                    ),
                  )
                }
              >
                <IconChevronLeft
                  width={16}
                  height={16}
                />
              </button>

              <span
                className={
                  styles.rangeNavLabel
                }
              >
                {formatWeekRangeLabel(
                  weekDates,
                )}
              </span>

              <button
                type="button"
                className={
                  styles.rangeNavButton
                }
                aria-label="Semana siguiente"
                onClick={() =>
                  setSelectedDateKey(
                    shiftDateKey(
                      selectedDateKey,
                      7,
                    ),
                  )
                }
              >
                <IconChevronRight
                  width={16}
                  height={16}
                />
              </button>
            </div>

            <div
              className={
                styles.weekGrid
              }
            >
              {weekDates.map(
                (day) => (
                  <DayAgenda
                    key={day}
                    dateKey={day}
                    reminders={
                      scopedReminders
                    }
                    warranties={
                      state.warranties
                    }
                    maintenanceRecords={
                      state.maintenanceRecords
                    }
                    draggedReminderId={
                      draggedReminderId
                    }
                    dragOverSlot={
                      dragOverSlot
                    }
                    onDragStart={
                      handleDragStart
                    }
                    onDragOver={
                      handleDragOver
                    }
                    onDrop={
                      handleDrop
                    }
                    onDragEnd={
                      handleDragEnd
                    }
                    completeReminderAction={
                      state.completeReminderAction
                    }
                    onOpenReminder={(reminder) =>
                      setActiveReminderId(reminder.id)
                    }
                    compact
                    selected={
                      day ===
                      selectedDateKey
                    }
                    onSelect={() =>
                      setSelectedDateKey(
                        day,
                      )
                    }
                  />
                ),
              )}
            </div>

            <p
              className={
                styles.rangeNavHint
              }
            >
              Agregando para{' '}
              <strong>
                {formatDayNavLabel(
                  selectedDateKey,
                )}
              </strong>{' '}
              — toca otro día de la
              semana para cambiarlo.
            </p>

            {quickAddSection}
          </ListSectionCard>
        )}

        {/* =====================================================
            DAY
            ===================================================== */}

        {activeRange ===
          'day' && (
          <ListSectionCard title="Vista diaria">
            <div
              className={
                styles.rangeNav
              }
            >
              <button
                type="button"
                className={
                  styles.rangeNavButton
                }
                aria-label="Día anterior"
                onClick={() =>
                  setSelectedDateKey(
                    shiftDateKey(
                      selectedDateKey,
                      -1,
                    ),
                  )
                }
              >
                <IconChevronLeft
                  width={16}
                  height={16}
                />
              </button>

              <button
                type="button"
                className={
                  styles.rangeNavToday
                }
                onClick={() =>
                  setSelectedDateKey(
                    todayKey,
                  )
                }
              >
                Ir a hoy
              </button>

              <button
                type="button"
                className={
                  styles.rangeNavButton
                }
                aria-label="Día siguiente"
                onClick={() =>
                  setSelectedDateKey(
                    shiftDateKey(
                      selectedDateKey,
                      1,
                    ),
                  )
                }
              >
                <IconChevronRight
                  width={16}
                  height={16}
                />
              </button>
            </div>

            <div
              className={
                styles.dayFullPane
              }
            >
              <div className={styles.dayAgendaColumn}>
                <DayAgenda
                  dateKey={
                    selectedDateKey
                  }
                  reminders={
                    scopedReminders
                  }
                  warranties={
                    state.warranties
                  }
                  maintenanceRecords={
                    state.maintenanceRecords
                  }
                  draggedReminderId={
                    draggedReminderId
                  }
                  dragOverSlot={
                    dragOverSlot
                  }
                  onDragStart={
                    handleDragStart
                  }
                  onDragOver={
                    handleDragOver
                  }
                  onDrop={
                    handleDrop
                  }
                  onDragEnd={
                    handleDragEnd
                  }
                  completeReminderAction={
                    state.completeReminderAction
                  }
                  onOpenReminder={(reminder) =>
                    setActiveReminderId(reminder.id)
                  }
                />

                {quickAddSection}
              </div>

              {/* Pedido explícito del usuario (2026-08-22): "reservar
                  aproximadamente el 40% del espacio derecho para las
                  notas" — Canvas único, reemplaza la vista de notas
                  anterior por completo (ver daynotes/DayNotesCanvas.tsx). */}
              <div className={styles.dayNotesColumn}>
                <DayNotesCanvas dateKey={selectedDateKey} />
              </div>
            </div>
          </ListSectionCard>
        )}

        <ReminderDrawer
          reminder={activeReminder}
          onClose={() => setActiveReminderId(null)}
          onSave={state.updateReminderAction}
          onDelete={state.deleteReminderAction}
        />
      </div>
    </AppShell>
  )
}