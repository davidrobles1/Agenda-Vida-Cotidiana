import { useEffect, useState } from 'react'
import { completeReminder, createReminder, listReminders, type Reminder } from './api'
import { getCurrentUser } from '../../core/user/api'
import { ShareDialog } from '../sharing/ShareDialog'
import { cancelLocalReminder, scheduleLocalReminder } from '../../core/notifications/localReminderTimer'
import { AppShell } from '../../core/ui/layout/AppShell'
import { IconCheckCircle, IconTasks } from '../../core/ui/icons'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import styles from './RemindersPage.module.css'

/**
 * RETIRADA DE LA APLICACIÓN (2026-08-28, pedido explícito del usuario: el
 * botón "Nuevo" del `AppShell`, su destino y su contenido dejan de existir a
 * nivel visual).
 *
 * Ese botón era el ÚNICO acceso a esta pantalla — no tenía ítem propio en
 * ningún navbar —, así que al quitarlo se retiraron también sus tres rutas
 * (`/reminders`, `/personal/reminders`, `/laboral/reminders`) en
 * `AppRouter.tsx`. Este componente ya no se monta en ninguna parte.
 *
 * El archivo se conserva a propósito (regla del proyecto: no se borran
 * componentes) y, sobre todo, los `REMINDER` **siguen siendo datos vivos**:
 * se ven en el Calendario y en la línea de tiempo de Hoy, y se crean desde
 * el alta rápida del Calendario y desde el detalle de Persona/Proyecto. Lo
 * que desapareció es esta pantalla, no la funcionalidad.
 */
export function RemindersPage() {
  // FR-019/ADR-015: context is inferred from which navbar this screen was
  // reached through — null when reached via the legacy bare /reminders
  // route (no context sent, backend defaults to PERSONAL, same as today).
  const activeMode = useActiveMode()
  const [reminders, setReminders] = useState<Reminder[]>([])
  const [currentUserId, setCurrentUserId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [debugCrash, setDebugCrash] = useState(false)

  // WEB-006 real verification: thrown during render (not inside the click
  // handler) so Sentry.ErrorBoundary in main.tsx actually catches it and
  // reports to GlitchTip — an error boundary never sees exceptions thrown
  // from an event handler, only from rendering.
  if (debugCrash) {
    throw new Error('WEB-006 debug crash: manually triggered from RemindersPage')
  }

  async function refresh() {
    try {
      const page = await listReminders()
      setReminders(page.items)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load reminders')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
    getCurrentUser()
      .then((user) => setCurrentUserId(user.id))
      .catch(() => {
        /* Share button just won't show if this fails — not fatal to the reminders list. */
      })
  }, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!title.trim()) return
    try {
      // datetime-local's value has no timezone designator, so `new Date(...)`
      // correctly parses it as the browser's local time — matches what the
      // user actually picked, not UTC.
      const dueAtDate = dueAtLocal ? new Date(dueAtLocal) : null
      const created = await createReminder({
        title: title.trim(),
        dueAt: dueAtDate ? dueAtDate.toISOString() : undefined,
        context: activeMode ?? undefined,
      })
      // WEB-007: best-effort, tab-must-stay-open local reminder — see
      // core/notifications/localReminderTimer.ts for the real platform limit.
      if (dueAtDate) scheduleLocalReminder(created.id, created.title, dueAtDate.getTime())
      setTitle('')
      setDueAtLocal('')
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create reminder')
    }
  }

  async function handleComplete(reminder: Reminder) {
    try {
      await completeReminder(reminder.id, reminder.version)
      // Nothing to notify about once it's done — a no-op if this reminder
      // never had a scheduled timer (same fail-closed pattern as Android's
      // ReminderAlarmScheduler.cancel()).
      cancelLocalReminder(reminder.id)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to complete reminder')
    }
  }

  return (
    <AppShell title="Tareas" subtitle="Administra tus recordatorios.">
      <div className={styles.debugRow}>
        <button type="button" data-variant="secondary" onClick={() => setDebugCrash(true)}>
          Debug: trigger error
        </button>
      </div>

      <form className={styles.createForm} onSubmit={handleCreate}>
        <input
          className={styles.createInput}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="New reminder"
        />
        <button type="submit">Add</button>
      </form>

      <div className={styles.dueDateRow}>
        <label htmlFor="due-at-input" className={styles.dueDateSummary}>
          Due date (optional)
        </label>
        <input
          id="due-at-input"
          type="datetime-local"
          value={dueAtLocal}
          onChange={(e) => setDueAtLocal(e.target.value)}
        />
      </div>

      {error && <p role="alert">{error}</p>}
      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : reminders.length === 0 ? (
        <div className="empty-state">
          <p>No reminders yet</p>
          <p>Add one above to get started.</p>
        </div>
      ) : (
        <ul className={styles.list}>
          {reminders.map((reminder) => (
            <li key={reminder.id}>
              <ReminderCard
                reminder={reminder}
                isOwner={reminder.ownerUserId === currentUserId}
                onComplete={() => handleComplete(reminder)}
              />
            </li>
          ))}
        </ul>
      )}
    </AppShell>
  )
}

interface ReminderCardProps {
  reminder: Reminder
  isOwner: boolean
  onComplete: () => void
}

/** UX-001: reminder card — priority #1 of the design-system.md rollout. */
function ReminderCard({ reminder, isOwner, onComplete }: ReminderCardProps) {
  return (
    <div className={styles.card}>
      <div className={styles.cardTop}>
        <span className={styles.cardIcon} data-tone={reminder.status === 'COMPLETED' ? 'success' : 'primary'}>
          {reminder.status === 'COMPLETED' ? <IconCheckCircle width={16} height={16} /> : <IconTasks width={16} height={16} />}
        </span>
        <span className={styles.cardTitle}>{reminder.title}</span>
        <StatusBadge status={reminder.status} />
      </div>
      {reminder.dueAt && (
        <span className={styles.cardDueAt}>Due: {new Date(reminder.dueAt).toLocaleString()}</span>
      )}
      {(reminder.status === 'PENDING' || isOwner) && (
        <div className={styles.cardActions}>
          {reminder.status === 'PENDING' && (
            <button type="button" onClick={onComplete}>
              Complete
            </button>
          )}
          {isOwner && <ShareDialog reminderId={reminder.id} />}
        </div>
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  if (status === 'COMPLETED') return <span className="badge badge-success">Completed</span>
  if (status === 'PENDING') return <span className="badge badge-warning">Pending</span>
  return <span className="badge">{status}</span>
}
