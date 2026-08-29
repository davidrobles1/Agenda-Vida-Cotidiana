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
import { SEVERITY_TONE, SOURCE_LABELS } from './alerts/dateAlerts'
import { AlertList } from './alerts/AlertList'
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

  /** "Notas en el margen" (2026-08-23): la cabecera de la hoja muestra
      "N actividades · M notas", y el conteo de notas vive dentro de
      DayNotesCanvas — sube por callback. */
  const [dayNotesCount, setDayNotesCount] = useState(0)

  const scopedReminders =
    useMemo(
      () =>
        // ADR-019: el aislamiento real lo hace el servidor (`listReminders`
        // manda `?context=`). Esto se queda como segunda barrera, pero
        // ESTRICTA: antes dejaba pasar los recordatorios sin contexto en
        // AMBOS módulos, que era precisamente la fuga. La migración V25
        // rellenó esos nulos con PERSONAL, así que ya no existen.
        activeMode
          ? state.reminders.filter(
              (reminder) =>
                (reminder.context ?? 'PERSONAL') ===
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

  /** Mismo criterio que DayAgenda usa para su propio contador: la
      cabecera se movió fuera de ese componente (ver `hideHeader`), así
      que el conteo se recalcula aquí sobre los mismos datos. */
  const dayActivityCount = useMemo(() => {
    const reminders = scopedReminders.filter(
      (reminder) => reminder.dueAt && reminder.dueAt.slice(0, 10) === selectedDateKey,
    ).length

    // ADR-018: garantías y mantenimientos del día dejaron de sumarse aquí —
    // ahora se muestran como ALERTAS y se cuentan por separado. Sumarlos en
    // los dos sitios los contaría dos veces en la misma cabecera.
    return reminders
  }, [scopedReminders, selectedDateKey])

  const dayAlerts = useMemo(
    () => state.alertsByDay[selectedDateKey] ?? [],
    [state.alertsByDay, selectedDateKey],
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

  /**
   * 2026-08-29 (petición 2.1): "en las vistas Mensual y Semanal, al
   * seleccionar un día, abrir la vista Día correspondiente".
   *
   * Antes seleccionar un día solo movía `selectedDateKey` y la vista se
   * quedaba donde estaba, así que había que cambiar de pestaña a mano para
   * ver el detalle. Ahora hace las dos cosas: fija la fecha y cambia de
   * vista, en ese orden, para que la vista Día ya se monte sobre el día
   * elegido y no sobre el anterior.
   */
  function openDayView(dateKey: string) {
    setSelectedDateKey(dateKey)
    setRangeSelection(new Set(['day']))
  }

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

          // 2026-08-29 (petición 2.3): el estado también en el texto del
          // marcador. El tono ya distinguía completada/vencida por color,
          // pero el color solo no dice "terminada" — y en la vista mensual
          // el marcador es todo lo que se ve del registro.
          const state = reminder.status === 'COMPLETED' ? 'Terminada' : 'Pendiente'

          add(
            isoDateKey(dueAt),
            reminderTone(
              reminder,
            ),
            `${reminder.title} — ${state}`,
            activeMode
              ? undefined
              : reminderContextColor(
                  reminder,
                ),
          )
        },
      )

      /**
       * ADR-018: garantías, mantenimientos y suscripciones ya no se marcan
       * "a mano" cada uno por su fecha final. Ahora entran por las alertas
       * derivadas (features/calendar/alerts/dateAlerts.ts), que incluyen
       * ese día final Y los avisos previos que pidió el usuario. Marcar
       * además el registro por su cuenta duplicaría el punto del día de
       * vencimiento, que es justo lo que se pidió evitar.
       */
      state.alerts.forEach((alert) => {
        add(
          alert.dateKey,
          SEVERITY_TONE[alert.severity],
          `${SOURCE_LABELS[alert.source]}: ${alert.sourceLabel} — ${alert.message}`,
        )
      })

      return map
    }, [
      scopedReminders,
      state.alerts,
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
                  openDayView
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
                    alerts={
                      state.alertsByDay[day] ?? []
                    }
                    compact
                    selected={
                      day ===
                      selectedDateKey
                    }
                    onSelect={() =>
                      openDayView(day)
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

            {/* "Notas en el margen" (propuesta aprobada, 2026-08-23):
                agenda y notas comparten UNA hoja — un solo borde exterior
                y una cabecera con la fecha que cruza ambas columnas. Antes
                eran dos cajas independientes pegadas, que es justo lo que
                hacía sentir las notas como un componente externo. */}
            <div className={styles.daySheet}>
              <div className={styles.daySheetHead}>
                <div className={styles.daySheetDate}>
                  <span className={styles.daySheetDateNum}>
                    {Number(selectedDateKey.slice(-2))}
                  </span>

                  <span className={styles.daySheetDateMeta}>
                    <span className={styles.daySheetDow}>
                      {new Date(`${selectedDateKey}T12:00:00`).toLocaleDateString('es-MX', {
                        weekday: 'long',
                      })}
                    </span>

                    <span className={styles.daySheetMonth}>
                      {new Date(`${selectedDateKey}T12:00:00`).toLocaleDateString('es-MX', {
                        month: 'long',
                        year: 'numeric',
                      })}
                    </span>
                  </span>
                </div>

                <span className={styles.daySheetCount}>
                  {dayActivityCount}{' '}
                  {dayActivityCount === 1 ? 'actividad' : 'actividades'} · {dayNotesCount}{' '}
                  {dayNotesCount === 1 ? 'nota' : 'notas'}
                  {dayAlerts.length > 0 && (
                    <> · {dayAlerts.length} {dayAlerts.length === 1 ? 'alerta' : 'alertas'}</>
                  )}
                </span>
              </div>
                  {/* ADR-018: seguimiento de fechas próximas, NO tareas. Va
                    encima de la agenda y con su propio rótulo para que se
                    lea como otra cosa distinta a las actividades del día. */}
                {dayAlerts.length > 0 && (
                  <div className={styles.daySheetAlerts}>
                    <div className={styles.daySheetColLabel}>Alertas</div>
                    <AlertList alerts={dayAlerts} />
                  </div>
                )}
              <div className={styles.daySheetAgenda}>
                <div className={styles.daySheetColLabel}>Agenda</div>

                <DayAgenda
                  hideHeader
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

              <div className={styles.daySheetNotes}>
                <DayNotesCanvas
                  dateKey={selectedDateKey}
                  onCountChange={setDayNotesCount}
                />
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