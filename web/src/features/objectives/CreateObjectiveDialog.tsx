import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import { createObjective, type CreateObjectiveInput, type Objective } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

interface CreateObjectiveDialogProps {
  onCreated: (objective: Objective) => void
}

/** ADR-016 Fase 3e1/FR-031, UC-24. Mismo patrón exacto que CreatePersonDialog.tsx. */
export function CreateObjectiveDialog({ onCreated }: CreateObjectiveDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [targetValue, setTargetValue] = useState('')
  const [deadlineLocal, setDeadlineLocal] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setTitle('')
    setTargetValue('')
    setDeadlineLocal('')
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
      const input: CreateObjectiveInput = { title: title.trim() }
      const parsedTarget = Number.parseInt(targetValue, 10)
      if (Number.isFinite(parsedTarget) && parsedTarget >= 0) input.targetValue = parsedTarget
      if (deadlineLocal) input.deadline = new Date(deadlineLocal).toISOString()
      const created = await createObjective(input)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el objetivo.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button>
        <IconPlus width={16} height={16} /> Nuevo objetivo
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
                    Nuevo objetivo
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Objetivo</span>
                  <input
                    className={shellStyles.textInput}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Cerrar 3 proyectos este trimestre"
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Meta numérica (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    type="number"
                    min={0}
                    value={targetValue}
                    onChange={(e) => setTargetValue(e.target.value)}
                    placeholder="3"
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Fecha límite (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    type="date"
                    value={deadlineLocal}
                    onChange={(e) => setDeadlineLocal(e.target.value)}
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
