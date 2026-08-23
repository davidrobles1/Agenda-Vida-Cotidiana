import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import {
  cancelInvitation,
  createInvitation,
  listSharesAndInvitations,
  revokeShare,
  type Invitation,
  type ReminderShare,
} from './api'
import { IconUsers } from '../../core/ui/icons'
import styles from './ShareDialog.module.css'

interface ShareDialogProps {
  reminderId: string
}

const MotionPopover = motion.create(Popover)
const MotionDialog = motion.create(Dialog)

/**
 * UX-011 Fase 2: real popover dialog (`DialogTrigger` + `Popover` + `Dialog`)
 * instead of the previous inline expand-in-place panel — that version had 3
 * real gaps: no focus trap, no Escape-to-close, no `role="dialog"` semantics
 * (and no way to close it besides re-pressing "Share"). React Aria's
 * DialogTrigger/Popover give focus management, outside-click and Escape
 * handling, and correct ARIA wiring for free — exactly the "hard to hand-
 * build correctly" case that justifies using it here.
 *
 * Motion drives open/close (`smooth` token) directly on the Popover's own
 * DOM node via `motion.create` — React Aria's own exit-animation detection
 * (`Element.getAnimations()`) picks that up natively, so there's no fight
 * between Motion's timing and React Aria's unmount timing; no
 * `AnimatePresence` needed.
 */
export function ShareDialog({ reminderId }: ShareDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [shares, setShares] = useState<ReminderShare[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [recipient, setRecipient] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

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

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open) {
      setLoading(true)
      refresh()
    }
  }

  async function handleInvite(e: FormEvent) {
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
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button data-variant="secondary">Share</Button>
      <MotionPopover
        offset={8}
        placement="bottom start"
        initial={{ opacity: 0, scale: 0.96, y: -4 }}
        animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
        transition={motionTokens.smooth}
      >
        <Dialog data-testid="share-dialog" className={styles.panel}>
          {({ close }) => (
            <>
              <div className={styles.headerRow}>
                <Heading slot="title" className={styles.heading}>
                  Share this reminder
                </Heading>
                <button type="button" className={styles.closeButton} onClick={close} aria-label="Close">
                  ×
                </button>
              </div>

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
                          <span className={styles.rowIcon}>
                            <IconUsers width={14} height={14} />
                          </span>
                          <span className={styles.rowText}>
                            {share.collaboratorUserId} — {share.status}
                          </span>
                          {share.status === 'ACTIVE' && (
                            <RevokeConfirmButton
                              collaboratorLabel={share.collaboratorUserId}
                              onConfirm={() => handleRevoke(share.id)}
                            />
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
                          <span className={styles.rowIcon}>
                            <IconUsers width={14} height={14} />
                          </span>
                          <span className={styles.rowText}>
                            {invitation.invitedEmail} — {invitation.status}
                          </span>
                          <button type="button" data-variant="destructive" onClick={() => handleCancel(invitation.id)}>
                            Cancel
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                </>
              )}
            </>
          )}
        </Dialog>
      </MotionPopover>
    </DialogTrigger>
  )
}

interface RevokeConfirmButtonProps {
  collaboratorLabel: string
  onConfirm: () => void
}

/**
 * UX-011 Fase 4: a real confirmation step for the one destructive action in
 * this dialog with an immediate, real-world consequence for someone else —
 * revoking an *active* collaborator's access. Scoped deliberately narrow:
 * "Cancel" (a still-pending invitation, never affected anyone's real
 * access) and Accept/Reject in `InvitationsPage.tsx` are intentionally left
 * as single-click actions — adding a confirm step there would be inventing
 * friction nothing asked for, not closing a real gap. `role="alertdialog"`
 * + `Modal` (blocking, unlike ShareDialog's own non-modal `Popover`) is the
 * correct real pattern here: a confirmation must interrupt, a share panel
 * shouldn't.
 *
 * Motion animates the `Dialog` panel itself, not `Modal` — found by real
 * testing, not assumed: `Modal`'s own internal enter/exit-animation
 * bookkeeping (it separately tracks its backdrop and content refs, unlike
 * the simpler `Popover`) re-renders shortly after mount and stomps the
 * inline style Motion had just applied, leaving the panel functionally
 * open but stuck invisible at its `initial` values. `Dialog` has no such
 * competing animation machinery, so it's a safe `motion.create` target;
 * the backdrop (`Modal`) stays a plain, instant show/hide — acceptable,
 * the panel's own motion is what needed to read as intentional here.
 */
function RevokeConfirmButton({ collaboratorLabel, onConfirm }: RevokeConfirmButtonProps) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={setIsOpen}>
      <Button data-variant="destructive">Revoke</Button>
      <Modal isDismissable className={styles.modalOverlay}>
        <MotionDialog
          role="alertdialog"
          className={styles.confirmPanel}
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.95 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            <>
              <Heading slot="title" className={styles.heading}>
                Revoke access?
              </Heading>
              <p className={styles.confirmBody}>
                {collaboratorLabel} will lose access to this shared reminder.
              </p>
              <div className={styles.confirmActions}>
                <button type="button" data-variant="secondary" onClick={close}>
                  Cancel
                </button>
                <button
                  type="button"
                  data-variant="destructive"
                  onClick={() => {
                    onConfirm()
                    close()
                  }}
                >
                  Revoke
                </button>
              </div>
            </>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
