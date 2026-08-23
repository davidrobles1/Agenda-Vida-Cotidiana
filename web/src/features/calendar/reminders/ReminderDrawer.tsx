import { useEffect, useState, type FormEvent } from 'react'
import { Dialog, Heading, Modal } from 'react-aria-components'
import { AnimatePresence, motion } from 'motion/react'
import { motionTokens } from '../../../core/motion/tokens'
import { EDIT_ICON, findIconOption, findStickerOption } from '../../../core/ui/pickers/pickerCatalog'
import type { Reminder } from '../../reminders/api'
import { ReminderFormFields } from './ReminderFormFields'
import { ReminderDeleteConfirm } from './ReminderDeleteConfirm'
import type { CreateReminderInput } from './CreateReminderDialog'
import shellStyles from '../../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

/** `datetime-local` no tiene designador de zona horaria — mismo formato
    que produce el propio input, para poder precargarlo en modo edición. */
function toDueAtLocal(iso: string | undefined): string {
  if (!iso) return ''
  const date = new Date(iso)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

interface ReminderDrawerProps {
  reminder: Reminder | null
  onClose: () => void
  onSave: (reminder: Reminder, input: CreateReminderInput) => Promise<void>
  onDelete: (reminder: Reminder) => Promise<void>
}

/**
 * Ver + Editar tarea de Agenda — mismo panel para las dos, mismo patrón
 * exacto que `notes/NoteDrawer.tsx` (ver ese archivo para el porqué
 * completo del cambio de panel lateral a modal centrado, pedido explícito
 * del usuario 2026-08-22: "un modal en medio con estilo extravagante
 * basado al tema actual"). Incluye fecha/hora además de
 * título/descripción/icono/emoji/sticker.
 */
export function ReminderDrawer({ reminder, onClose, onSave, onDelete }: ReminderDrawerProps) {
  const [mode, setMode] = useState<'view' | 'edit'>('view')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [iconId, setIconId] = useState<string | undefined>(undefined)
  const [stickerId, setStickerId] = useState<string | undefined>(undefined)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (reminder) {
      setMode('view')
      setTitle(reminder.title)
      setDescription(reminder.description ?? '')
      setDueAtLocal(toDueAtLocal(reminder.dueAt))
      setIconId(reminder.iconId)
      setStickerId(reminder.stickerId)
      setError(null)
    }
  }, [reminder])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!reminder || !title.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const dueAtDate = dueAtLocal ? new Date(dueAtLocal) : null
      await onSave(reminder, {
        title: title.trim(),
        description,
        dueAt: dueAtDate ? dueAtDate.toISOString() : undefined,
        iconId,
        stickerId,
      })
      setMode('view')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron guardar los cambios.')
    } finally {
      setSaving(false)
    }
  }

  const activeIcon = findIconOption(reminder?.iconId ?? iconId)
  const sticker = findStickerOption(reminder?.stickerId)

  return (
    <Modal
      isOpen={reminder !== null}
      isDismissable
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
      className={shellStyles.modalOverlay}
    >
      <AnimatePresence>
      {reminder && (
        <MotionDialog
          key="reminder-modal"
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 8 }}
          transition={motionTokens.smooth}
        >
          <div className={shellStyles.panelScroll}>
          <AnimatePresence mode="wait" initial={false}>
            {mode === 'view' ? (
              <motion.div
                key="view"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={motionTokens.smooth}
              >
                <div className={shellStyles.drawerHeaderRow}>
                  <Heading slot="title" className={shellStyles.heading}>
                    Tarea
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={onClose} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {(sticker || activeIcon) && (
                  <div className={shellStyles.drawerBadges}>
                    {sticker && <img src={sticker.asset} alt="" className={shellStyles.drawerSticker} />}
                    {activeIcon && (
                      <span className={shellStyles.drawerIconBadge}>
                        <activeIcon.Icon width={20} height={20} />
                      </span>
                    )}
                  </div>
                )}

                <h3 className={shellStyles.drawerTitle}>{reminder.title}</h3>
                {reminder.dueAt && (
                  <p className={shellStyles.confirmBody}>
                    {new Date(reminder.dueAt).toLocaleString('es-MX', {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'long',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                )}
                <p className={shellStyles.drawerDescription}>{reminder.description || 'Sin descripción.'}</p>

                <div className={shellStyles.drawerActions}>
                  <button type="button" className={shellStyles.editButton} onClick={() => setMode('edit')}>
                    <EDIT_ICON width={16} height={16} />
                    Editar
                  </button>
                  <ReminderDeleteConfirm reminderTitle={reminder.title} onConfirm={() => onDelete(reminder)} />
                  <button type="button" data-variant="secondary" onClick={onClose}>
                    Cerrar
                  </button>
                </div>
              </motion.div>
            ) : (
              <motion.form
                key="edit"
                onSubmit={handleSubmit}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={motionTokens.smooth}
              >
                <div className={shellStyles.drawerHeaderRow}>
                  <Heading slot="title" className={shellStyles.heading}>
                    Editar tarea
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={onClose} aria-label="Cerrar">
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

                <div className={shellStyles.drawerActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={() => setMode('view')} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving}>
                    Guardar cambios
                  </button>
                </div>
              </motion.form>
            )}
          </AnimatePresence>
          </div>
        </MotionDialog>
      )}
      </AnimatePresence>
    </Modal>
  )
}
