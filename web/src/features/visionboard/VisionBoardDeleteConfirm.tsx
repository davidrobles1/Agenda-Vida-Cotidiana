import { useState, type ReactNode } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal, ModalOverlay } from 'react-aria-components'
import { motion } from 'motion/react'
import { Trash2 } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import styles from './VisionBoardToolbar.module.css'

const MotionDialog = motion.create(Dialog)

interface VisionBoardDeleteConfirmProps {
  /** 0 disables the trigger — nothing selected, nothing to delete. Ignored
      when `disabled` is passed explicitly (the board-delete case below has
      no "count" concept). */
  count: number
  onConfirm: () => Promise<void>
  /** FASE 17: optionally controlled from outside — VisionBoardCanvas.tsx's
      Delete/Backspace keyboard shortcut needs to open this same dialog
      (never bypass confirmation, per "eliminar después de confirmación"),
      which requires lifting the open state. Falls back to fully
      self-contained internal state when omitted, so every other existing
      call site (none currently pass these) keeps working unchanged. */
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
  /** BLOQUE A: overrides for a non-"selected elements" use of this same
      confirm pattern (deleting the whole board). All optional and default
      to the original elements-count-based copy — the existing "Eliminar"
      (selected elements) call site in VisionBoardToolbar.tsx needs zero
      changes. */
  triggerLabel?: string
  triggerIcon?: ReactNode
  heading?: string
  body?: string
  disabled?: boolean
}

/**
 * FASE 11: same real alertdialog pattern already used by
 * NoteDeleteConfirm/ReminderDeleteConfirm (core/ui/dialogs/DialogShell —
 * Modal bloqueante + Dialog role="alertdialog", Motion animates the Dialog,
 * not the Modal) — deleting a persisted Vision Board element is exactly the
 * "immediate, already-committed" kind of destructive action that pattern's
 * own convention reserves confirmation for (see ShareDialog.tsx's doc
 * comment: skipped for actions with no real effect yet, like cancelling a
 * pending invitation; shown for anything that deletes real, already-saved
 * data). Unlike that pattern's original wording, this one doesn't claim the
 * action "no se puede deshacer" — it can, via FASE 9's Undo.
 */
export function VisionBoardDeleteConfirm({
  count,
  onConfirm,
  isOpen: isOpenProp,
  onOpenChange: onOpenChangeProp,
  triggerLabel,
  triggerIcon,
  heading,
  body,
  disabled,
}: VisionBoardDeleteConfirmProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const isOpen = isOpenProp ?? internalOpen
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const resolvedDisabled = disabled ?? count === 0
  const resolvedLabel = triggerLabel ?? 'Eliminar'
  const resolvedHeading = heading ?? (count === 1 ? 'Eliminar elemento' : `Eliminar ${count} elementos`)
  const resolvedBody =
    body ??
    (count === 1
      ? 'El elemento seleccionado se eliminará del Vision Board.'
      : `Los ${count} elementos seleccionados se eliminarán del Vision Board.`)

  function handleOpenChange(open: boolean) {
    if (onOpenChangeProp) {
      onOpenChangeProp(open)
    } else {
      setInternalOpen(open)
    }
    if (!open) setError(null)
  }

  async function handleConfirm(close: () => void) {
    setDeleting(true)
    setError(null)
    try {
      await onConfirm()
      close()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo eliminar.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.actionButton} isDisabled={resolvedDisabled} aria-label={resolvedLabel}>
        {triggerIcon ?? <Trash2 width={16} height={16} />}
        
      </Button>
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
                  {resolvedHeading}
                </Heading>
                <p className={shellStyles.confirmBody}>{resolvedBody}</p>
                {error && (
                  <p className={shellStyles.formError} role="alert">
                    {error}
                  </p>
                )}
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
