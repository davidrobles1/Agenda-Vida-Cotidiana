import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import type { Person } from '../people/api'
import type { Project } from '../projects/api'
import { createReminder, type CreateReminderInput, type Reminder } from '../reminders/api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

interface CreateTaskDialogProps {
  people: Person[]
  projects: Project[]
  onCreated: (task: Reminder) => void
}

/** ADR-016/FR-023, UC-17. Tarea = REMINDER context=LABORAL con vínculo opcional a Persona/Proyecto. */
export function CreateTaskDialog({ people, projects, onCreated }: CreateTaskDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [personId, setPersonId] = useState('')
  const [projectId, setProjectId] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setTitle('')
    setDueAtLocal('')
    setPersonId('')
    setProjectId('')
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!title.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const input: CreateReminderInput = { title: title.trim(), context: 'LABORAL' }
      if (dueAtLocal) input.dueAt = new Date(dueAtLocal).toISOString()
      if (personId) input.personId = personId
      if (projectId) input.projectId = projectId
      const created = await createReminder(input)
      onCreated(created)
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
      <Button>
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

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Título</span>
                  <input
                    className={shellStyles.textInput}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Enviar propuesta comercial"
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Fecha (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    type="date"
                    value={dueAtLocal}
                    onChange={(e) => setDueAtLocal(e.target.value)}
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Persona (opcional)</span>
                  <select className={shellStyles.textInput} value={personId} onChange={(e) => setPersonId(e.target.value)}>
                    <option value="">— Ninguna —</option>
                    {people.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Proyecto (opcional)</span>
                  <select className={shellStyles.textInput} value={projectId} onChange={(e) => setProjectId(e.target.value)}>
                    <option value="">— Ninguno —</option>
                    {projects.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </label>

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
