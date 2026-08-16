import { useEffect, useState } from 'react'
import {
  cancelInvitation,
  createInvitation,
  listSharesAndInvitations,
  revokeShare,
  type Invitation,
  type ReminderShare,
} from './api'
import styles from './ShareDialog.module.css'

interface ShareDialogProps {
  reminderId: string
}

export function ShareDialog({ reminderId }: ShareDialogProps) {
  const [shares, setShares] = useState<ReminderShare[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [recipient, setRecipient] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function refresh() {
    try {
      const result = await listSharesAndInvitations(reminderId)
      setShares(result.shares)
      setInvitations(result.invitations)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load shares')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reminderId])

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault()
    const value = recipient.trim()
    if (!value) return
    try {
      const recipientPayload = value.includes('@') ? { email: value } : { username: value }
      await createInvitation(reminderId, recipientPayload)
      setRecipient('')
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create invitation')
    }
  }

  async function handleRevoke(shareId: string) {
    try {
      await revokeShare(reminderId, shareId)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to revoke share')
    }
  }

  async function handleCancel(invitationId: string) {
    try {
      await cancelInvitation(invitationId)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to cancel invitation')
    }
  }

  const pendingInvitations = invitations.filter((invitation) => invitation.status === 'PENDING')

  return (
    <div data-testid="share-dialog" className={styles.panel}>
      <h2 className={styles.heading}>Share this reminder</h2>

      <form className={styles.inviteForm} onSubmit={handleInvite}>
        <input
          className={styles.inviteInput}
          value={recipient}
          onChange={(e) => setRecipient(e.target.value)}
          placeholder="Email or username"
        />
        <button type="submit">Invite</button>
      </form>

      {error && <p role="alert">{error}</p>}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
          <div className={styles.section}>
            <p className={styles.sectionLabel}>Collaborators</p>
            {shares.length === 0 && <p className={styles.emptyRow}>No collaborators yet.</p>}
            <ul className={styles.list} aria-label="Collaborators">
              {shares.map((share) => (
                <li key={share.id} className={styles.row}>
                  <span>{share.collaboratorUserId} — {share.status}</span>
                  {share.status === 'ACTIVE' && (
                    <button type="button" data-variant="destructive" onClick={() => handleRevoke(share.id)}>
                      Revoke
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>

          <div className={styles.section}>
            <p className={styles.sectionLabel}>Pending invitations</p>
            {pendingInvitations.length === 0 && <p className={styles.emptyRow}>No pending invitations.</p>}
            <ul className={styles.list} aria-label="Pending invitations">
              {pendingInvitations.map((invitation) => (
                <li key={invitation.id} className={styles.row}>
                  <span>{invitation.invitedEmail} — {invitation.status}</span>
                  <button type="button" data-variant="destructive" onClick={() => handleCancel(invitation.id)}>
                    Cancel
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </div>
  )
}
