import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import { createRoutine, FREQUENCY_LABELS, type CreateRoutineInput, type Routine, type RoutineFrequency } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

interface CreateRoutineDialogProps {
  onCreated: (routine: Routine) => void
}

function todayLocal(): string {
  return new Date().toISOString().slice(0, 10)
}

/** ADR-016 Fase 3e2/FR-032, UC-25. Mismo patrón exacto que CreatePersonDialog.tsx. */
export function CreateRoutineDialog({ onCreated }: CreateRoutineDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [frequency, setFrequency] = useState<RoutineFrequency>('WEEKLY')
  // El usuario elige la primera ocurrencia — el backend no la deriva (AC-019).
  const [nextExecutionLocal, setNextExecutionLocal] = useState(todayLocal())
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setTitle('')
    setDescription('')
    setFrequency('WEEKLY')
    setNextExecutionLocal(todayLocal())
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!title.trim() || !nextExecutionLocal || saving) return

    setSaving(true)
    setError(null)
    try {
      const input: CreateRoutineInput = {
        title: title.trim(),
        frequency,
        nextExecutionDate: new Date(nextExecutionLocal).toISOString(),
      }
      if (description.trim()) input.description = description.trim()
      const created = await createRoutine(input)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la rutina.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button>
        <IconPlus width={16} height={16} /> Nueva rutina
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
                    Nueva rutina
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Rutina</span>
                  <input
                    className={shellStyles.textInput}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Revisar correo"
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Descripción (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Bandeja de entrada a cero"
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Frecuencia</span>
                  <select
                    className={shellStyles.textInput}
                    value={frequency}
                    onChange={(e) => setFrequency(e.target.value as RoutineFrequency)}
                  >
                    {(Object.keys(FREQUENCY_LABELS) as RoutineFrequency[]).map((f) => (
                      <option key={f} value={f}>
                        {FREQUENCY_LABELS[f]}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Próxima ejecución</span>
                  <input
                    className={shellStyles.textInput}
                    type="date"
                    value={nextExecutionLocal}
                    onChange={(e) => setNextExecutionLocal(e.target.value)}
                  />
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
