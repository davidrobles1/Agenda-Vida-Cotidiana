import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { acceptInvitation, listMyInvitations, rejectInvitation, type Invitation } from './api'
import styles from './InvitationsPage.module.css'

export function InvitationsPage() {
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function refresh() {
    try {
      setInvitations(await listMyInvitations())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load invitations')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  async function handleAccept(id: string) {
    try {
      await acceptInvitation(id)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to accept invitation')
    }
  }

  async function handleReject(id: string) {
    try {
      await rejectInvitation(id)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to reject invitation')
    }
  }

  return (
    <div>
      <header className={styles.header}>
        <h1 className={styles.title}>Invitations</h1>
        <Link to="/reminders">Back</Link>
      </header>

      {error && <p role="alert">{error}</p>}

      {loading ? (
        <p>Loading…</p>
      ) : invitations.length === 0 ? (
        <div className="empty-state">
          <p>No pending invitations</p>
        </div>
      ) : (
        <ul className={styles.list}>
          {invitations.map((invitation) => (
            <li key={invitation.id} className={styles.card}>
              <span className={styles.email}>{invitation.invitedEmail}</span>
              <div className={styles.actions}>
                <button type="button" onClick={() => handleAccept(invitation.id)}>
                  Accept
                </button>
                <button type="button" data-variant="secondary" onClick={() => handleReject(invitation.id)}>
                  Reject
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
