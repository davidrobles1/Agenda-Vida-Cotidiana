import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { CalendarView, type CalendarMarker } from '../../core/ui/components/CalendarView'
import { IconShared, IconShield, IconTasks, IconWrench } from '../../core/ui/icons'
import { useCalendarData } from './useCalendarData'
import styles from './CalendarPage.module.css'

function dateKeyFromIso(iso: string): string {
  return iso.slice(0, 10)
}

function formatDueAt(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const LEGEND: Array<{ label: string; tone: CalendarMarker['tone'] }> = [
  { label: 'Tareas', tone: 'primary' },
  { label: 'Garantías (simulado)', tone: 'warning' },
  { label: 'Mantenimiento (simulado)', tone: 'info' },
]

/**
 * UX-007: Calendario — month grid + a "Pendientes" list with checkboxes.
 * Real data: reminders (marked on `dueAt`; completing one here calls the
 * exact same `POST /reminders/{id}/complete` RemindersPage uses, via
 * `useCalendarData.completeReminderAction`) and pending invitations (listed
 * only — accept/reject stays on InvitationsPage, not duplicated here). Mock
 * data: Garantías/Mantenimiento, visually distinguished with a "Simulado"
 * pill + a different icon/tone from real tasks, and their "completed"
 * checkbox state is local-only (`mockCompletedIds`) — lost on reload, never
 * sent anywhere. Zero new backend endpoints.
 */
export function CalendarPage() {
  const navigate = useNavigate()
  const state = useCalendarData()
  const [displayedMonth, setDisplayedMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })

  const markersByDay = useMemo(() => {
    const map: Record<string, CalendarMarker[]> = {}
    const add = (key: string, tone: CalendarMarker['tone']) => {
      if (!map[key]) map[key] = []
      map[key].push({ tone })
    }
    state.reminders.forEach((r) => r.dueAt && add(dateKeyFromIso(r.dueAt), 'primary'))
    state.warranties.forEach((w) => add(w.expiresAt, 'warning'))
    state.maintenanceRecords.forEach((m) => add(m.nextDueAt, 'info'))
    return map
  }, [state.reminders, state.warranties, state.maintenanceRecords])

  const pendingReminders = [...state.reminders].sort((a, b) => (a.dueAt ?? '').localeCompare(b.dueAt ?? ''))
  const pendingWarranties = state.warranties.filter((w) => !state.mockCompletedIds.has(w.id))
  const pendingMaintenance = state.maintenanceRecords.filter((m) => !state.mockCompletedIds.has(m.id))
  const hasPendientes = pendingReminders.length > 0 || pendingWarranties.length > 0 || pendingMaintenance.length > 0

  function shiftMonth(delta: number) {
    setDisplayedMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + delta, 1))
  }

  return (
    <AppShell title="Calendario" subtitle="Tareas, garantías y mantenimiento en un solo vistazo.">
      <div className={`${styles.page} notebook-bg`}>
        {state.loading && <p className={styles.loading}>Loading…</p>}
        {state.error && <p role="alert">{state.error}</p>}

        <ListSectionCard title="Vista mensual">
          <CalendarView
            month={displayedMonth}
            markersByDay={markersByDay}
            onPrevMonth={() => shiftMonth(-1)}
            onNextMonth={() => shiftMonth(1)}
          />
          <div className={styles.legend}>
            {LEGEND.map((entry) => (
              <span key={entry.label} className={styles.legendEntry}>
                <span className={styles.legendDot} data-tone={entry.tone} />
                {entry.label}
              </span>
            ))}
          </div>
        </ListSectionCard>

        {hasPendientes && (
          <ListSectionCard title="Pendientes">
            {pendingReminders.map((reminder) => (
              <ListItemRow
                key={reminder.id}
                title={reminder.title}
                subtitle={reminder.dueAt ? `Vence: ${formatDueAt(reminder.dueAt)}` : ''}
                icon={IconTasks}
                tone="primary"
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
