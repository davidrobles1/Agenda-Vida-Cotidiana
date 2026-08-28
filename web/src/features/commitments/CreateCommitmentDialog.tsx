import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import { useVocabulary } from '../../core/user/useVocabulary'
import type { Person } from '../people/api'
import type { Project } from '../projects/api'
import { createCommitment, type Commitment, type CommitmentDirection, type CreateCommitmentInput } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import styles from './CommitmentsPage.module.css'

const MotionDialog = motion.create(Dialog)

interface CreateCommitmentDialogProps {
  people: Person[]
  projects: Project[]
  onCreated: (commitment: Commitment) => void
}

/** ADR-016/FR-025, UC-18. "Seguimientos"/"Esperando" son el mismo formulario — solo cambia `direction`. */
export function CreateCommitmentDialog({ people, projects, onCreated }: CreateCommitmentDialogProps) {
  // UX-014/UX-015: solo la etiqueta del selector de Proyecto cambia.
  const vocabulary = useVocabulary()
  const [isOpen, setIsOpen] = useState(false)
  const [personId, setPersonId] = useState('')
  const [description, setDescription] = useState('')
  const [direction, setDirection] = useState<CommitmentDirection>('MINE')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [projectId, setProjectId] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setPersonId('')
    setDescription('')
    setDirection('MINE')
    setDueAtLocal('')
    setProjectId('')
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open) {
      setPersonId((current) => current || people[0]?.id || '')
    } else {
      reset()
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!personId || !description.trim() || !dueAtLocal || saving) return

    setSaving(true)
    setError(null)
    try {
      const input: CreateCommitmentInput = {
        personId,
        description: description.trim(),
        direction,
        dueAt: new Date(dueAtLocal).toISOString(),
      }
      if (projectId) input.projectId = projectId
      const created = await createCommitment(input)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el seguimiento.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button>
        <IconPlus width={16} height={16} /> Nuevo seguimiento
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
                    Nuevo seguimiento
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                {people.length === 0 ? (
                  <p className={shellStyles.formError} role="alert">
                    Primero necesitas crear al menos una Persona.
                  </p>
                ) : (
                  <>
                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Persona</span>
                      <select className={shellStyles.textInput} value={personId} onChange={(e) => setPersonId(e.target.value)}>
                        {people.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.name}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Próxima acción</span>
                      <input
                        className={shellStyles.textInput}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="Confirmar aprobación de la propuesta"
                      />
                    </label>

                    <fieldset className={styles.directionFieldset}>
                      <label>
                        <input type="radio" name="direction" checked={direction === 'MINE'} onChange={() => setDirection('MINE')} />
                        Yo debo actuar
                      </label>
                      <label>
                        <input type="radio" name="direction" checked={direction === 'THEIRS'} onChange={() => setDirection('THEIRS')} />
                        Espero a la otra persona
                      </label>
                    </fieldset>

                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Fecha</span>
                      <input
                        className={shellStyles.textInput}
                        type="date"
                        value={dueAtLocal}
                        onChange={(e) => setDueAtLocal(e.target.value)}
                      />
                    </label>

                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>{vocabulary.project} (opcional)</span>
                      <select className={shellStyles.textInput} value={projectId} onChange={(e) => setProjectId(e.target.value)}>
                        <option value="">— Ninguno —</option>
                        {projects.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  </>
                )}

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving || people.length === 0}>
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
