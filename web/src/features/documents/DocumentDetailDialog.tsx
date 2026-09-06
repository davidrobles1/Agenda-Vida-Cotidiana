import { useState, type FormEvent } from 'react'
import { Dialog, Heading, Modal, ModalOverlay } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import {
  DOCUMENT_CATEGORIES,
  DOCUMENT_CATEGORY_LABELS,
  updateDocument,
  type DocumentCategory,
  type VidaDocument,
} from './api'
import { VISIBILITY_LABELS, formatSize, formatUploadedAt } from './documentsView'
import styles from './DocumentDetailDialog.module.css'

const MotionDialog = motion.create(Dialog)

interface DocumentDetailDialogProps {
  document: VidaDocument
  onClose: () => void
  onSaved: (document: VidaDocument) => void
}

/**
 * ADR-022: renombrar y recategorizar un documento.
 *
 * CARENCIA QUE CUBRE: `PATCH /documents/{id}` existía en el backend desde el
 * principio y **el cliente ni siquiera tenía la función**. Un documento mal
 * nombrado o guardado en la categoría equivocada solo se podía arreglar
 * borrándolo y volviéndolo a subir — perdiendo de paso con quién estaba
 * compartido.
 *
 * El archivo en sí no se sustituye aquí: el backend no tiene endpoint para
 * reemplazar el contenido de un documento existente, así que ofrecerlo sería
 * prometer algo que no puede cumplir.
 */
export function DocumentDetailDialog({ document, onClose, onSaved }: DocumentDetailDialogProps) {
  const [name, setName] = useState(document.name)
  const [category, setCategory] = useState<DocumentCategory>(document.category)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim() || saving) return
    setSaving(true)
    setError(null)
    try {
      const saved = await updateDocument(document.id, name.trim(), category, document.version)
      onSaved(saved)
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el documento.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <ModalOverlay isOpen isDismissable onOpenChange={(open) => !open && onClose()} className={shellStyles.modalOverlay}>
      <Modal>
        <MotionDialog
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={motionTokens.smooth}
        >
          <div className={shellStyles.panelScroll}>
            <form onSubmit={handleSubmit}>
              <div className={shellStyles.headerRow}>
                <div>
                  <Heading slot="title" className={shellStyles.heading}>
                    {document.name}
                  </Heading>
                  <p className={styles.subtitle}>
                    {formatSize(document.sizeBytes)} · subido el {formatUploadedAt(document.createdAt)} ·{' '}
                    {VISIBILITY_LABELS[document.visibility]}
                  </p>
                </div>
                <button type="button" className={shellStyles.closeButton} onClick={onClose} aria-label="Cerrar">
                  ×
                </button>
              </div>

              {error && (
                <p className={shellStyles.formError} role="alert">
                  {error}
                </p>
              )}

              <label className={shellStyles.field}>
                <span className={shellStyles.fieldLabel}>Nombre</span>
                <input
                  className={shellStyles.textInput}
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  required
                />
              </label>

              <label className={shellStyles.field}>
                <span className={shellStyles.fieldLabel}>Categoría</span>
                <select
                  className={shellStyles.textInput}
                  value={category}
                  onChange={(event) => setCategory(event.target.value as DocumentCategory)}
                >
                  {DOCUMENT_CATEGORIES.map((option) => (
                    <option key={option} value={option}>
                      {DOCUMENT_CATEGORY_LABELS[option]}
                    </option>
                  ))}
                </select>
              </label>

              {/* La visibilidad se cambia desde "Compartir", que es donde
                  vive esa decisión y sus avisos. Duplicarla aquí daría dos
                  sitios para el mismo hecho. */}
              <p className={styles.helper}>
                Para cambiar quién puede verlo, usa la acción “Compartir” de la lista.
              </p>

              <div className={styles.actions}>
                <button type="submit" className={styles.primary} disabled={saving}>
                  {saving ? 'Guardando…' : 'Guardar cambios'}
                </button>
                <button type="button" className={styles.secondary} onClick={onClose}>
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </MotionDialog>
      </Modal>
    </ModalOverlay>
  )
}
