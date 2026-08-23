import { useRef, useState, type DragEvent, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { Upload, UploadCloud } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { DOCUMENT_CATEGORIES, DOCUMENT_CATEGORY_LABELS, uploadDocument, type DocumentCategory, type VidaDocument } from './api'
import styles from './UploadDocumentDialog.module.css'

const MotionDialog = motion.create(Dialog)

interface UploadDocumentDialogProps {
  onUploaded: (document: VidaDocument) => void
}

/**
 * Pedido explícito del usuario (2026-08-22): "subir... todo esto bajo un
 * modal central preguntando si es Identificación, Comprobantes, Seguros,
 * Contratos, Otros" — el modal central es este, la pregunta de categoría es
 * el radiogroup de abajo (mismo patrón de teclado ARIA — Arrow/Home/End,
 * roving tabindex — que Bloque G ya estableció para Vision Board, reusado
 * aquí vía core/ui/keyboard/radiogroupKeyboard.ts en vez de reimplementarlo).
 */
export function UploadDocumentDialog({ onUploaded }: UploadDocumentDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [name, setName] = useState('')
  const [category, setCategory] = useState<DocumentCategory>('OTROS')
  const [file, setFile] = useState<File | null>(null)
  const [dragOver, setDragOver] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function reset() {
    setName('')
    setCategory('OTROS')
    setFile(null)
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  function pickFile(selected: File | null) {
    setFile(selected)
    setError(null)
    if (selected && !name.trim()) {
      // Precarga el nombre desde el archivo — el usuario puede editarlo,
      // no obliga a retiparlo si el nombre del archivo ya es razonable.
      setName(selected.name.replace(/\.[^.]+$/, ''))
    }
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragOver(false)
    const dropped = event.dataTransfer.files[0]
    if (dropped) pickFile(dropped)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!file || !name.trim() || uploading) return
    setUploading(true)
    setError(null)
    try {
      const uploaded = await uploadDocument(file, name.trim(), category)
      onUploaded(uploaded)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo subir el documento.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.uploadButton}>
        <Upload width={16} height={16} /> Subir documento
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
                    Subir documento
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

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
                      <span>Arrastra una imagen o PDF aquí, o haz clic para elegir un archivo</span>
                    </>
                  )}
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/png,image/jpeg,image/webp,image/gif,application/pdf"
                    className={styles.dropzoneInput}
                    onChange={(event) => pickFile(event.target.files?.[0] ?? null)}
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Nombre</span>
                  <input
                    className={shellStyles.textInput}
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    required
                  />
                </label>

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Categoría</span>
                  <div
                    className={styles.categoryGrid}
                    role="radiogroup"
                    aria-label="Categoría del documento"
                    onKeyDown={handleRadiogroupKeyDown}
                  >
                    {DOCUMENT_CATEGORIES.map((option, index) => (
                      <button
                        key={option}
                        type="button"
                        role="radio"
                        aria-checked={category === option}
                        tabIndex={radioTabIndex(category === option, index === 0, true)}
                        className={`${styles.categoryButton} ${category === option ? styles.categoryButtonActive : ''}`}
                        onClick={() => setCategory(option)}
                      >
                        {DOCUMENT_CATEGORY_LABELS[option]}
                      </button>
                    ))}
                  </div>
                </div>

                <div className={shellStyles.formActions}>
                  {uploading && <span className={shellStyles.savingHint}>Subiendo…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={uploading}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={uploading || !file || !name.trim()}>
                    {uploading ? 'Subiendo…' : 'Subir'}
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
