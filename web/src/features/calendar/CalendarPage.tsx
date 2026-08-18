import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { CalendarView, type CalendarMarker } from '../../core/ui/components/CalendarView'
import { dateKey } from '../../core/ui/components/calendarDate'
import { IconAlert, IconCheckCircle, IconPlus, IconShared, IconShield, IconTasks, IconWrench } from '../../core/ui/icons'
import { useCalendarData } from './useCalendarData'
import type { Reminder } from '../reminders/api'
import styles from './CalendarPage.module.css'

function isoDateKey(iso: string): string {
  return iso.slice(0, 10)
}

function formatDueAtTime(iso: string): string {
  return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

function formatLongDate(key: string): string {
  const [y, m, d] = key.split('-').map(Number)
  const label = new Date(y, m - 1, d).toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

/** UX-010: a reminder's real visual state — pending/overdue/completed, all
    derived from data GET /reminders already returns, nothing invented. */
function reminderTone(r: Reminder): CalendarMarker['tone'] {
  if (r.status === 'COMPLETED') return 'success'
  if (r.dueAt && new Date(r.dueAt).getTime() < Date.now()) return 'error'
  return 'primary'
}

function reminderIcon(r: Reminder) {
  const tone = reminderTone(r)
  if (tone === 'success') return IconCheckCircle
  if (tone === 'error') return IconAlert
  return IconTasks
}

function reminderPillLabel(r: Reminder): string {
  const tone = reminderTone(r)
  if (tone === 'success') return 'Completada'
  if (tone === 'error') return 'Vencida'
  return 'Pendiente'
}

const LEGEND: Array<{ label: string; tone: CalendarMarker['tone'] }> = [
  { label: 'Pendiente', tone: 'primary' },
  { label: 'Vencida', tone: 'error' },
  { label: 'Completada', tone: 'success' },
  { label: 'Garantías (simulado)', tone: 'warning' },
  { label: 'Mantenimiento (simulado)', tone: 'info' },
]

/**
 * UX-010: Calendario — rediseñado para que organizar el día sea la
 * experiencia principal, no un grid administrativo. Mismo dato real de
 * siempre (`useCalendarData`): recordatorios reales (`GET /reminders`,
 * ahora incluyendo completados para mostrar ese estado también — antes se
 * descartaban), invitaciones pendientes reales (solo listadas), Garantías/
 * Mantenimiento mock. Lo nuevo es 100% interacción/presentación:
 * selección de día (estado de cliente), un panel de detalle del día
 * elegido, y una creación rápida que llama al mismo POST /reminders real
 * que ya usa RemindersPage — no hay backend nuevo.
 */
export function CalendarPage() {
  const navigate = useNavigate()
  const state = useCalendarData()
  const today = new Date()
  const [displayedMonth, setDisplayedMonth] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1))
  const [selectedDateKey, setSelectedDateKey] = useState(() => dateKey(today.getFullYear(), today.getMonth(), today.getDate()))
  const [quickAddTitle, setQuickAddTitle] = useState('')
  const [adding, setAdding] = useState(false)

  const markersByDay = useMemo(() => {
    const map: Record<string, CalendarMarker[]> = {}
    const add = (key: string, tone: CalendarMarker['tone']) => {
      if (!map[key]) map[key] = []
      map[key].push({ tone })
    }
    state.reminders.forEach((r) => r.dueAt && add(isoDateKey(r.dueAt), reminderTone(r)))
    state.warranties.forEach((w) => {
      if (!state.mockCompletedIds.has(w.id)) add(w.expiresAt, 'warning')
    })
    state.maintenanceRecords.forEach((m) => {
      if (!state.mockCompletedIds.has(m.id)) add(m.nextDueAt, 'info')
    })
    return map
  }, [state.reminders, state.warranties, state.maintenanceRecords, state.mockCompletedIds])

  function shiftMonth(delta: number) {
    setDisplayedMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + delta, 1))
  }

  function goToday() {
    const now = new Date()
    setDisplayedMonth(new Date(now.getFullYear(), now.getMonth(), 1))
    setSelectedDateKey(dateKey(now.getFullYear(), now.getMonth(), now.getDate()))
  }

  const dayReminders = state.reminders.filter((r) => r.dueAt && isoDateKey(r.dueAt) === selectedDateKey)
  const dayWarranties = state.warranties.filter((w) => w.expiresAt === selectedDateKey && !state.mockCompletedIds.has(w.id))
  const dayMaintenance = state.maintenanceRecords.filter((m) => m.nextDueAt === selectedDateKey && !state.mockCompletedIds.has(m.id))
  const hasDayItems = dayReminders.length > 0 || dayWarranties.length > 0 || dayMaintenance.length > 0

  const pendingReminders = state.reminders
    .filter((r) => r.status === 'PENDING')
    .sort((a, b) => (a.dueAt ?? '').localeCompare(b.dueAt ?? ''))
  const pendingWarranties = state.warranties.filter((w) => !state.mockCompletedIds.has(w.id))
  const pendingMaintenance = state.maintenanceRecords.filter((m) => !state.mockCompletedIds.has(m.id))
  const hasPendientes = pendingReminders.length > 0 || pendingWarranties.length > 0 || pendingMaintenance.length > 0

  async function handleQuickAdd(e: FormEvent) {
    e.preventDefault()
    if (!quickAddTitle.trim() || adding) return
    setAdding(true)
    try {
      await state.createReminderForDay(quickAddTitle, selectedDateKey)
      setQuickAddTitle('')
    } finally {
      setAdding(false)
    }
  }

  return (
    <AppShell title="Calendario" subtitle="Organiza tu día, tus garantías y tu mantenimiento en un solo lugar.">
      <div className={`${styles.page} notebook-bg`}>
        {state.loading && <p className={styles.loading}>Loading…</p>}
        {state.error && <p role="alert">{state.error}</p>}

        {/* UX-010: title stays "Vista mensual", distinct from AppShell's own
            "Calendario" page heading above it — a duplicate "Calendario"
            text node here made getByText('Calendario')-style lookups
            ambiguous (real regression caught running the real e2e specs). */}
        <ListSectionCard title="Vista mensual">
          <div className={styles.layout}>
            <div className={styles.gridPane}>
              <CalendarView
                month={displayedMonth}
                markersByDay={markersByDay}
                selectedDateKey={selectedDateKey}
                onSelectDate={setSelectedDateKey}
                onPrevMonth={() => shiftMonth(-1)}
                onNextMonth={() => shiftMonth(1)}
                onToday={goToday}
              />
              <div className={styles.legend}>
                {LEGEND.map((entry) => (
                  <span key={entry.label} className={styles.legendEntry}>
                    <span className={styles.legendDot} data-tone={entry.tone} />
                    {entry.label}
                  </span>
                ))}
              </div>
            </div>

            <div className={styles.divider} aria-hidden="true" />

            <div className={styles.dayPane} key={selectedDateKey}>
              <h3 className={styles.dayTitle}>{formatLongDate(selectedDateKey)}</h3>

              {hasDayItems ? (
                <div className={styles.dayItems}>
                  {dayReminders.map((reminder) => (
                    <ListItemRow
                      key={reminder.id}
                      title={reminder.title}
                      subtitle={reminder.dueAt ? formatDueAtTime(reminder.dueAt) : ''}
                      icon={reminderIcon(reminder)}
                      tone={reminderTone(reminder)}
                      pillLabel={reminderPillLabel(reminder)}
                      pillTone={reminderTone(reminder)}
                      trailing={
                        reminder.status === 'PENDING' ? (
                          <input
                            type="checkbox"
                            className={styles.checkbox}
                            aria-label={`Marcar "${reminder.title}" como completada`}
                            onChange={() => state.completeReminderAction(reminder)}
                          />
                        ) : undefined
                      }
                    />
                  ))}
                  {dayWarranties.map((warranty) => (
                    <ListItemRow
                      key={warranty.id}
                      title={warranty.item}
                      subtitle="Garantía · dato simulado"
                      icon={IconShield}
                      tone="warning"
                      pillLabel="Simulado"
                      pillTone="warning"
                      trailing={
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          aria-label={`Marcar "${warranty.item}" como completada (dato simulado)`}
                          onChange={() => state.toggleMockComplete(warranty.id)}
                        />
                      }
                    />
                  ))}
                  {dayMaintenance.map((record) => (
                    <ListItemRow
                      key={record.id}
                      title={record.item}
                      subtitle="Mantenimiento · dato simulado"
                      icon={IconWrench}
                      tone="info"
                      pillLabel="Simulado"
                      pillTone="info"
                      trailing={
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          aria-label={`Marcar "${record.item}" como completado (dato simulado)`}
                          onChange={() => state.toggleMockComplete(record.id)}
                        />
                      }
                    />
                  ))}
                </div>
              ) : (
                <p className={styles.dayEmpty}>Nada programado para este día. Aprovecha para planear algo.</p>
              )}

              <form className={styles.quickAdd} onSubmit={handleQuickAdd}>
                <input
                  className={styles.quickAddInput}
                  value={quickAddTitle}
                  onChange={(e) => setQuickAddTitle(e.target.value)}
                  placeholder="Agregar tarea para este día…"
                  aria-label="Agregar tarea para este día"
                />
                <button type="submit" className={styles.quickAddButton} aria-label="Agregar tarea" disabled={adding}>
                  <IconPlus width={16} height={16} />
                </button>
              </form>
            </div>
          </div>
        </ListSectionCard>

        {hasPendientes && (
          <ListSectionCard title="Todos los pendientes">
            {pendingReminders.map((reminder) => (
              <ListItemRow
                key={reminder.id}
                title={reminder.title}
                subtitle={reminder.dueAt ? `Vence: ${formatDueAtTime(reminder.dueAt)} · ${isoDateKey(reminder.dueAt)}` : ''}
                icon={reminderIcon(reminder)}
                tone={reminderTone(reminder)}
                trailing={
                  <input
                    type="checkbox"
                    className={styles.checkbox}
                    aria-label={`Marcar "${reminder.title}" como completada`}
                    onChange={() => state.completeReminderAction(reminder)}
                  />
                }
              />
            ))}
            {pendingWarranties.map((warranty) => (
              <ListItemRow
                key={warranty.id}
                title={warranty.item}
                subtitle={`Garantía · vence ${warranty.expiresAt} · dato simulado`}
                icon={IconShield}
                tone="warning"
                pillLabel="Simulado"
                pillTone="warning"
                trailing={
                  <input
                    type="checkbox"
                    className={styles.checkbox}
                    aria-label={`Marcar "${warranty.item}" como completada (dato simulado)`}
                    onChange={() => state.toggleMockComplete(warranty.id)}
                  />
                }
              />
            ))}
            {pendingMaintenance.map((record) => (
              <ListItemRow
                key={record.id}
                title={record.item}
                subtitle={`Mantenimiento · próximo ${record.nextDueAt} · dato simulado`}
                icon={IconWrench}
                tone="info"
                pillLabel="Simulado"
                pillTone="info"
                trailing={
                  <input
                    type="checkbox"
                    className={styles.checkbox}
                    aria-label={`Marcar "${record.item}" como completado (dato simulado)`}
                    onChange={() => state.toggleMockComplete(record.id)}
                  />
                }
              />
            ))}
          </ListSectionCard>
        )}

        {state.invitations.length > 0 && (
          <ListSectionCard title="Invitaciones pendientes" onSeeAll={() => navigate('/invitations')}>
            {state.invitations.map((invitation) => (
              <ListItemRow
                key={invitation.id}
                title={invitation.invitedEmail ?? ''}
                subtitle="Invitación a compartir un recordatorio"
                icon={IconShared}
                tone="info"
                pillLabel="Pendiente"
                pillTone="warning"
              />
            ))}
          </ListSectionCard>
        )}
      </div>
    </AppShell>
  )
}
