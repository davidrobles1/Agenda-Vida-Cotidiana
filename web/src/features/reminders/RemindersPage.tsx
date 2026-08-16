import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { completeReminder, createReminder, listReminders, type Reminder } from './api'
import { logout } from '../../core/auth/authClient'
import { getCurrentUser } from '../../core/user/api'
import { ShareDialog } from '../sharing/ShareDialog'
import styles from './RemindersPage.module.css'

export function RemindersPage() {
  const [reminders, setReminders] = useState<Reminder[]>([])
  const [currentUserId, setCurrentUserId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [sharingReminderId, setSharingReminderId] = useState<string | null>(null)
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
      await createReminder(title.trim())
      setTitle('')
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create reminder')
    }
  }

  async function handleComplete(reminder: Reminder) {
    try {
      await completeReminder(reminder.id, reminder.version)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to complete reminder')
    }
  }

  return (
    <div>
      <header className={styles.header}>
        <h1 className={styles.title}>Vida Cotidiana</h1>
        <nav className={styles.nav}>
          <Link to="/invitations">Invitations</Link>
          <Link to="/notifications">Notifications</Link>
          <button type="button" data-variant="secondary" onClick={logout}>
            Log out
          </button>
          <button type="button" data-variant="secondary" onClick={() => setDebugCrash(true)}>
            Debug: trigger error
          </button>
        </nav>
      </header>

      <form className={styles.createForm} onSubmit={handleCreate}>
        <input
          className={styles.createInput}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="New reminder"
        />
        <button type="submit">Add</button>
      </form>

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
                onShareToggle={() =>
                  setSharingReminderId(sharingReminderId === reminder.id ? null : reminder.id)
                }
              />
              {sharingReminderId === reminder.id && (
                <div className={styles.shareSlot}>
                  <ShareDialog reminderId={reminder.id} />
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

interface ReminderCardProps {
  reminder: Reminder
  isOwner: boolean
  onComplete: () => void
  onShareToggle: () => void
}

/** UX-001: reminder card — priority #1 of the design-system.md rollout. */
function ReminderCard({ reminder, isOwner, onComplete, onShareToggle }: ReminderCardProps) {
  return (
    <div className={styles.card}>
      <div className={styles.cardTop}>
        <span className={styles.cardTitle}>{reminder.title}</span>
        <StatusBadge status={reminder.status} />
      </div>
      {(reminder.status === 'PENDING' || isOwner) && (
        <div className={styles.cardActions}>
          {reminder.status === 'PENDING' && (
            <button type="button" onClick={onComplete}>
              Complete
            </button>
          )}
          {isOwner && (
            <button type="button" data-variant="secondary" onClick={onShareToggle}>
              Share
            </button>
          )}
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
