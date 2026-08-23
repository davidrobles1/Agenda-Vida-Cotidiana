import { useState } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { Trash2 } from 'lucide-react'
import { motionTokens } from '../../motion/tokens'
import shellStyles from './DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

interface SimpleDeleteConfirmProps {
  /** Nombre del tipo de recurso, en minúscula, para el título/cuerpo del
      diálogo (ej. "artículo", "garantía", "mantenimiento", "suscripción"). */
  resourceLabel: string
  /** Nombre concreto del ítem a borrar (ej. "Laptop Dell XPS 13"). */
  itemName: string
  onConfirm: () => Promise<void>
  ariaLabel: string
}

/** Confirmación de borrado genérica — mismo patrón exacto que
    ReminderDeleteConfirm.tsx/DocumentDeleteConfirm.tsx, extraído a
    core/ui/ una vez que un tercer módulo (Inventario) necesitó el mismo
    diálogo palabra por palabra salvo el texto — evita repetir el mismo
    componente 5 veces para Inventario/Garantías/Mantenimiento/
    Suscripciones. */
export function SimpleDeleteConfirm({ resourceLabel, itemName, onConfirm, ariaLabel }: SimpleDeleteConfirmProps) {
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
      setError(e instanceof Error ? e.message : `No se pudo eliminar ${resourceLabel === 'garantía' ? 'la' : 'el'} ${resourceLabel}.`)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button data-variant="destructive" className={shellStyles.deleteButton} aria-label={ariaLabel}>
        <Trash2 width={16} height={16} />
      </Button>
      <Modal isDismissable className={shellStyles.modalOverlay}>
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
                Eliminar {resourceLabel}
              </Heading>
              <p className={shellStyles.confirmBody}>
                "{itemName}" se eliminará permanentemente. Esta acción no se puede deshacer.
              </p>
              {error && <p className={shellStyles.formError} role="alert">{error}</p>}
              <div className={shellStyles.confirmActions}>
                <button type="button" data-variant="secondary" onClick={close} disabled={deleting}>
                  Cancelar
                </button>
                <button type="button" data-variant="destructive" onClick={() => handleConfirm(close)} disabled={deleting}>
                  {deleting ? 'Eliminando…' : 'Eliminar'}
                </button>
              </div>
            </>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
