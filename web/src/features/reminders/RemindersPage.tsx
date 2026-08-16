import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { completeReminder, createReminder, listReminders, type Reminder } from './api'
import { logout } from '../../core/auth/authClient'
import { getCurrentUser } from '../../core/user/api'
import { ShareDialog } from '../sharing/ShareDialog'

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
      <header>
        <h1>Vida Cotidiana</h1>
        <Link to="/invitations">Invitations</Link>
        <Link to="/notifications">Notifications</Link>
        <button type="button" onClick={logout}>
          Log out
        </button>
        <button type="button" onClick={() => setDebugCrash(true)}>
          Debug: trigger error
        </button>
      </header>

      <form onSubmit={handleCreate}>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="New reminder"
        />
        <button type="submit">Add</button>
      </form>

      {error && <p role="alert">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : (
        <ul>
          {reminders.map((reminder) => (
            <li key={reminder.id}>
              <span>{reminder.title}</span>
              <span> — {reminder.status}</span>
              {reminder.status === 'PENDING' && (
                <button type="button" onClick={() => handleComplete(reminder)}>
                  Complete
                </button>
              )}
              {reminder.ownerUserId === currentUserId && (
                <button
                  type="button"
                  onClick={() =>
                    setSharingReminderId(sharingReminderId === reminder.id ? null : reminder.id)
                  }
                >
                  Share
                </button>
              )}
              {sharingReminderId === reminder.id && <ShareDialog reminderId={reminder.id} />}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
