import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../../core/motion/tokens'
import { IconPlus } from '../../../core/ui/icons'
import { ReminderFormFields } from './ReminderFormFields'
import notesStyles from '../notes/Notes.module.css'
import shellStyles from '../../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

export interface CreateReminderInput {
  title: string
  description?: string
  dueAt?: string
  iconId?: string
  stickerId?: string
}

interface CreateReminderDialogProps {
  /** `YYYY-MM-DD` del día seleccionado en el calendario — precarga la hora
      a mediodía de ese día, mismo default que el quick-add anterior. */
  defaultDateKey: string
  onCreate: (input: CreateReminderInput) => Promise<void>
}

/**
 * Crear tarea de Agenda — mismo patrón exacto que
 * `notes/CreateNoteDialog.tsx` (React Aria `DialogTrigger` + `Modal` +
 * `Dialog`, Motion anima el `Dialog` interno, no el `Modal`). Reemplaza el
 * quick-add de solo título: ahora la creación de una tarea siempre pasa por
 * este formulario completo (título, descripción, fecha/hora, icono, emoji,
 * sticker) — la única forma de "agregar rápido" que quedaba (un input de
 * texto suelto) no podía capturar icono/emoji/sticker/descripción.
 */
export function CreateReminderDialog({ defaultDateKey, onCreate }: CreateReminderDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [iconId, setIconId] = useState<string | undefined>(undefined)
  const [stickerId, setStickerId] = useState<string | undefined>(undefined)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setTitle('')
    setDescription('')
    setDueAtLocal('')
    setIconId(undefined)
    setStickerId(undefined)
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open) {
      setDueAtLocal(`${defaultDateKey}T12:00`)
    } else {
      reset()
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!title.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const dueAtDate = dueAtLocal ? new Date(dueAtLocal) : null
      await onCreate({
        title: title.trim(),
        description,
        dueAt: dueAtDate ? dueAtDate.toISOString() : undefined,
        iconId,
        stickerId,
      })
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la tarea.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={notesStyles.newNoteButton}>
        <IconPlus width={16} height={16} /> Nueva tarea
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
                  Nueva tarea
                </Heading>
                <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                  ×
                </button>
              </div>

              {error && <p className={shellStyles.formError} role="alert">{error}</p>}

              <ReminderFormFields
                title={title}
                onTitleChange={setTitle}
                description={description}
                onDescriptionChange={setDescription}
                dueAtLocal={dueAtLocal}
                onDueAtLocalChange={setDueAtLocal}
                iconId={iconId}
                onIconChange={setIconId}
                stickerId={stickerId}
                onStickerChange={setStickerId}
              />

              <div className={shellStyles.formActions}>
                {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                  Cancelar
                </button>
                <button type="submit" disabled={saving}>
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
