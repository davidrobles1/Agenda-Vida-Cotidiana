import { useState, type DragEvent, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { Plus, UploadCloud } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { createWarranty, type Warranty } from './api'
import styles from './CreateWarrantyDialog.module.css'
import { useActiveMode } from '../../core/user/ActiveModeContext'

const MotionDialog = motion.create(Dialog)

interface CreateWarrantyDialogProps {
  onCreated: (warranty: Warranty) => void
}

/** Pedido explícito del usuario (2026-08-21): "al registrar una garantía
    subir el archivo de la garantía en formato imagen o pdf en un modal
    central será el registro." Mismo dropzone que documents/UploadDocumentDialog.tsx —
    el archivo es obligatorio aquí (WarrantyService#create lo exige). */
export function CreateWarrantyDialog({ onCreated }: CreateWarrantyDialogProps) {
  // ADR-019: el recurso nace en el módulo desde el que se crea, y la
  // lista solo pide los de ese módulo. Fuera de /personal y /laboral
  // `activeMode` es null: se devuelve todo y las altas nacen PERSONAL.
  const activeMode = useActiveMode()
  const [isOpen, setIsOpen] = useState(false)
  const [item, setItem] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [dragOver, setDragOver] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setItem('')
    setExpiresAt('')
    setFile(null)
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragOver(false)
    const dropped = event.dataTransfer.files[0]
    if (dropped) setFile(dropped)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!item.trim() || !expiresAt || !file || saving) return
    setSaving(true)
    setError(null)
    try {
      const created = await createWarranty(item.trim(), new Date(expiresAt).toISOString(), file, activeMode)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo registrar la garantía.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.addButton}>
        <Plus width={16} height={16} /> Nueva garantía
      </Button>
      <Modal isDismissable className={shellStyles.modalOverlay}>
        <MotionDialog
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : 8 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            <div className={shellStyles.panelScroll}>
              <form onSubmit={handleSubmit}>
                <div className={shellStyles.headerRow}>
                  <Heading slot="title" className={shellStyles.heading}>
                    Nueva garantía
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Artículo</span>
                  <input
                    className={shellStyles.textInput}
                    value={item}
                    onChange={(event) => setItem(event.target.value)}
                    required
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Vence el</span>
                  <input
                    type="date"
                    className={shellStyles.textInput}
                    value={expiresAt}
                    onChange={(event) => setExpiresAt(event.target.value)}
                    required
                  />
                </label>

                <label
                  className={`${styles.dropzone} ${dragOver ? styles.dropzoneActive : ''}`}
                  onDragOver={(event) => {
                    event.preventDefault()
                    setDragOver(true)
                  }}
                  onDragLeave={() => setDragOver(false)}
                  onDrop={handleDrop}
                >
                  {file ? (
                    <span className={styles.dropzoneFileName}>{file.name}</span>
                  ) : (
                    <>
                      <UploadCloud width={22} height={22} aria-hidden="true" />
                      <span>Arrastra el comprobante de garantía (imagen o PDF), o haz clic para elegirlo</span>
                    </>
                  )}
                  <input
                    type="file"
                    accept="image/png,image/jpeg,image/webp,image/gif,application/pdf"
                    className={styles.dropzoneInput}
                    onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                  />
                </label>

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving || !item.trim() || !expiresAt || !file}>
                    Guardar
                  </button>
                </div>
              </form>
            </div>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
