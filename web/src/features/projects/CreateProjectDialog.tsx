import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import { useVocabulary } from '../../core/user/useVocabulary'
import type { Person } from '../people/api'
import { PersonPicker } from '../people/PersonPicker'
import { createProject, type CreateProjectInput, type Project } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { DatePicker } from '../../core/ui/pickers/DatePicker'

const MotionDialog = motion.create(Dialog)

interface CreateProjectDialogProps {
  people: Person[]
  onCreated: (project: Project) => void
  /** Persona creada desde este formulario, para que la pantalla la incorpore. */
  onPersonCreated?: (person: Person) => void
}

/** ADR-016/FR-022, UC-19. Mismo patrón que CreatePersonDialog.tsx. */
export function CreateProjectDialog({ people, onCreated, onPersonCreated }: CreateProjectDialogProps) {
  // UX-014/UX-015: mismo formulario, mismos campos — solo el nombre cambia.
  const vocabulary = useVocabulary()
  const [isOpen, setIsOpen] = useState(false)
  const [name, setName] = useState('')
  const [clientPersonId, setClientPersonId] = useState('')
  const [status, setStatus] = useState('')
  const [deadline, setDeadline] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setName('')
    setClientPersonId('')
    setStatus('')
    setDeadline('')
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const input: CreateProjectInput = { name: name.trim() }
      if (clientPersonId) input.clientPersonId = clientPersonId
      if (status.trim()) input.status = status.trim()
      if (deadline) input.deadline = new Date(deadline).toISOString()
      const created = await createProject(input)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el proyecto.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button>
        <IconPlus width={16} height={16} /> {vocabulary.projectGender === 'f' ? 'Nueva' : 'Nuevo'}{' '}
        {vocabulary.project.toLowerCase()}
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
                    {vocabulary.projectGender === 'f' ? 'Nueva' : 'Nuevo'} {vocabulary.project.toLowerCase()}
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Nombre</span>
                  <input
                    className={shellStyles.textInput}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Implementación ERP"
                    autoFocus
                  />
                </label>

                {/* El cliente sigue siendo campo del proyecto, no un
                    participante (DECISION 2026-09-06): son dos listas
                    disjuntas. Con cero personas, antes el desplegable salía
                    vacío y no había salida; ahora se crea aquí. */}
                <PersonPicker
                  people={people}
                  value={clientPersonId}
                  onChange={setClientPersonId}
                  onCreated={(person) => onPersonCreated?.(person)}
                  label={`${vocabulary.person} cliente`}
                  emptyOptionLabel="— Ninguna —"
                />

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Estado</span>
                  <input
                    className={shellStyles.textInput}
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    placeholder="En curso"
                  />
                </label>

                <DatePicker
                    label="Fecha límite (opcional)"
                    value={deadline}
                    onChange={(next) => setDeadline(next)}
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
