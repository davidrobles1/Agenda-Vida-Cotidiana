import { useState } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal, ModalOverlay } from 'react-aria-components'
import { motion } from 'motion/react'
import { Trash2 } from 'lucide-react'
import { motionTokens } from '../../../core/motion/tokens'
import shellStyles from '../../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

interface ReminderDeleteConfirmProps {
  reminderTitle: string
  onConfirm: () => Promise<void>
}

/**
 * Confirmación destructiva real antes de eliminar una tarea — mismo patrón
 * exacto que `notes/NoteDeleteConfirm.tsx` (a su vez tomado de
 * `ShareDialog.tsx`'s `RevokeConfirmButton`, UX-011 Fase 4).
 */
export function ReminderDeleteConfirm({ reminderTitle, onConfirm }: ReminderDeleteConfirmProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) setError(null)
  }

  async function handleConfirm(close: () => void) {
    setDeleting(true)
    setError(null)
    try {
      await onConfirm()
      close()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo eliminar la tarea.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button data-variant="destructive" className={shellStyles.deleteButton}>
        <Trash2 width={16} height={16} />
        Eliminar
      </Button>
      {/* Real bug found in browser testing: this confirm dialog is nested
          inside ReminderDrawer's own already-open `<Modal>`. A bare `<Modal>`
          here detects the ancestor Drawer's internal "already wrapped in a
          ModalOverlay" context and — wrongly, since that context is
          per-library-design not reset by `DialogTrigger` — treats itself as
          the *same* logical modal, skipping its own isOpen/isExiting gating
          entirely and rendering (and focusing) unconditionally, the instant
          the Drawer opens. Explicit `<ModalOverlay><Modal>` composition
          establishes this dialog's *own* modal context instead of inheriting
          the ancestor's, so it correctly gates on `isOpen` again. */}
      <ModalOverlay isDismissable className={shellStyles.confirmOverlay}>
        <Modal>
          <MotionDialog
            role="alertdialog"
            className={shellStyles.confirmPanel}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.95 }}
            transition={motionTokens.smooth}
          >
            {({ close }) => (
              <>
                <Heading slot="title" className={shellStyles.heading}>
                  Eliminar tarea
                </Heading>
                <p className={shellStyles.confirmBody}>
                  "{reminderTitle}" se eliminará permanentemente. Esta acción no se puede deshacer.
                </p>
                {error && <p className={shellStyles.formError} role="alert">{error}</p>}
                <div className={shellStyles.confirmActions}>
                  <button type="button" data-variant="secondary" onClick={close} disabled={deleting}>
                    Cancelar
                  </button>
                  <button
                    type="button"
                    data-variant="destructive"
                    onClick={() => handleConfirm(close)}
                    disabled={deleting}
                  >
                    {deleting ? 'Eliminando…' : 'Eliminar'}
                  </button>
                </div>
              </>
            )}
          </MotionDialog>
        </Modal>
      </ModalOverlay>
    </DialogTrigger>
  )
}
