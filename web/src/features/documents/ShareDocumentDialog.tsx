import { useRef, useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { Globe, Lock, Share2 } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { makeDocumentPrivate, makeDocumentPublic, shareDocument, type VidaDocument } from './api'
import styles from './ShareDocumentDialog.module.css'

const MotionDialog = motion.create(Dialog)

interface ShareDocumentDialogProps {
  document: VidaDocument
  onChanged: (document: VidaDocument) => void
}

/**
 * Pedido explícito del usuario (2026-08-22): "compartir con algún
 * integrante de la familia o hacerlo público entre la familia." Compartir
 * por correo resuelve de inmediato contra una cuenta existente — sin
 * invitación pendiente ni correo real, aclaración explícita del usuario
 * ("necesitamos que a quien se comparta documentos visualice de inmediato
 * en sus documentos y no que tenga que revisar el correo") — ver
 * DocumentService#shareWithEmail (backend) para el porqué la respuesta
 * nunca revela si el correo coincidió con una cuenta real (SEC-001).
 */
export function ShareDocumentDialog({ document, onChanged }: ShareDocumentDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [email, setEmail] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Real bug found in live testing: make-public/make-private swap which
  // branch renders (FAMILY_PUBLIC vs. not), which un-mounts whichever
  // button the user had just clicked/focused — the browser's default
  // "focused element removed → focus falls to <body>" behavior then leaves
  // it *outside* the popover's own focus scope, silently breaking Escape-
  // to-dismiss (React Aria's dismissal only fires relative to focus still
  // being inside) and leaving an invisible-but-click-blocking overlay
  // behind. Refocusing this dialog's own root (stable across either
  // branch, `tabIndex={-1}` since a plain `role="dialog"` isn't natively
  // focusable) after any action that can flip `visibility` keeps focus
  // inside the popover regardless of which conditional content survives.
  const dialogRef = useRef<HTMLElement | null>(null)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) {
      setEmail('')
      setError(null)
    }
  }

  async function handleShare(event: FormEvent) {
    event.preventDefault()
    if (!email.trim() || saving) return
    setSaving(true)
    setError(null)
    try {
      const updated = await shareDocument(document.id, email.trim(), document.version)
      onChanged(updated)
      setEmail('')
      dialogRef.current?.focus()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo compartir el documento.')
    } finally {
      setSaving(false)
    }
  }

  async function handleMakePublic() {
    setSaving(true)
    setError(null)
    try {
      onChanged(await makeDocumentPublic(document.id, document.version))
      dialogRef.current?.focus()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo hacer público el documento.')
    } finally {
      setSaving(false)
    }
  }

  async function handleMakePrivate() {
    setSaving(true)
    setError(null)
    try {
      onChanged(await makeDocumentPrivate(document.id, document.version))
      dialogRef.current?.focus()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo hacer privado el documento.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.trigger} aria-label="Compartir documento">
        <Share2 width={16} height={16} />
      </Button>
      <Popover placement="bottom end" offset={8} className={styles.popover}>
        <MotionDialog
          ref={dialogRef}
          className={styles.dialog}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          <Heading slot="title" className={shellStyles.heading}>
            Compartir
          </Heading>

          {error && <p className={shellStyles.formError} role="alert">{error}</p>}

          {document.visibility === 'FAMILY_PUBLIC' ? (
            <div className={styles.stateRow}>
              <Globe width={16} height={16} aria-hidden="true" />
              <span>Visible para toda la familia</span>
              <button type="button" data-variant="secondary" onClick={handleMakePrivate} disabled={saving}>
                Hacer privado
              </button>
            </div>
          ) : (
            <>
              {document.visibility === 'SHARED' && document.sharedWithEmail && (
                <p className={styles.currentShare}>Compartido con: {document.sharedWithEmail}</p>
              )}
              <form onSubmit={handleShare} className={styles.shareForm}>
                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Correo de un integrante de la familia</span>
                  <input
                    type="email"
                    className={shellStyles.textInput}
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="nombre@ejemplo.com"
                    required
                  />
                </label>
                <button type="submit" disabled={saving || !email.trim()}>
                  {saving ? 'Compartiendo…' : 'Compartir'}
                </button>
              </form>
              <button type="button" className={styles.publicButton} onClick={handleMakePublic} disabled={saving}>
                <Globe width={14} height={14} aria-hidden="true" />
                Hacer público entre la familia
              </button>
            </>
          )}

          {document.visibility === 'PRIVATE' && (
            <p className={styles.privateHint}>
              <Lock width={14} height={14} aria-hidden="true" />
              Actualmente solo visible para ti.
            </p>
          )}
        </MotionDialog>
      </Popover>
    </DialogTrigger>
  )
}
