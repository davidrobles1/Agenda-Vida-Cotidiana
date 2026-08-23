import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { Eye } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { createCommitment, type Commitment, type CommitmentDirection } from '../commitments/api'
import type { Reminder } from '../reminders/api'
import type { Project } from '../projects/api'
import { createNote } from '../calendar/notes/api'
import type { Note } from '../calendar/notes/notesData'
import type { VidaDocument } from '../documents/api'
import type { Person } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import styles from './PeoplePage.module.css'

const MotionDialog = motion.create(Dialog)

interface PersonDetailDialogProps {
  person: Person
  commitments: Commitment[]
  tasks: Reminder[]
  projects: Project[]
  notes: Note[]
  documents: VidaDocument[]
  onCommitmentCreated: (commitment: Commitment) => void
  onNoteCreated: (note: Note) => void
}

type ActiveForm = 'none' | 'commitment' | 'note'

function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

/**
 * ADR-016 Fase 3c (candidato V4, `34-laboral-module-proposal.md` §"Agenda
 * como memoria externa"): "última interacción" no es un campo propio de
 * Persona — se deriva del `updatedAt` más reciente entre sus Compromisos,
 * Tareas y Notas ya vinculados (todos disponibles como props, sin fetch
 * adicional). Sin endpoint nuevo, sin campo nuevo en el backend.
 */
function computeLastInteraction(commitments: Commitment[], tasks: Reminder[], notes: Note[]): string | null {
  const timestamps = [
    ...commitments.map((c) => c.updatedAt),
    ...tasks.map((t) => t.updatedAt),
    ...notes.map((n) => n.updatedAt),
  ]
  if (timestamps.length === 0) return null
  return timestamps.reduce((latest, current) => (current > latest ? current : latest))
}

function formatRelativeDate(iso: string): string {
  const then = new Date(iso)
  const days = Math.round((Date.now() - then.getTime()) / 86400000)
  if (days <= 0) return 'hoy'
  if (days === 1) return 'ayer'
  if (days < 30) return `hace ${days} días`
  const months = Math.round(days / 30)
  if (months < 12) return `hace ${months} mes${months === 1 ? '' : 'es'}`
  return formatDate(iso)
}

/**
 * UC-18/UC-19 (parcial, vista desde Persona). Diálogo de solo lectura +
 * formulario embebido para "Crear seguimiento" (UC-18) — se evitó anidar un
 * segundo Modal dentro de este (no había necesidad real de esa complejidad
 * para un formulario de 3 campos). No existía un patrón de página con tabs
 * en el código para "ver contexto de proyecto/persona"; este Modal
 * reutiliza el mismo shell que los diálogos de creación
 * (`DialogShell.module.css`), sin inventar un componente nuevo.
 */
export function PersonDetailDialog({ person, commitments, tasks, projects, notes, documents, onCommitmentCreated, onNoteCreated }: PersonDetailDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [activeForm, setActiveForm] = useState<ActiveForm>('none')
  const [description, setDescription] = useState('')
  const [direction, setDirection] = useState<CommitmentDirection>('MINE')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [noteText, setNoteText] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const openCommitments = commitments.filter((c) => c.status === 'OPEN')
  const lastInteractionAt = computeLastInteraction(commitments, tasks, notes)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) {
      setActiveForm('none')
      setDescription('')
      setDirection('MINE')
      setDueAtLocal('')
      setNoteText('')
      setError(null)
    }
  }

  async function handleCreateCommitment(event: FormEvent) {
    event.preventDefault()
    if (!description.trim() || !dueAtLocal || saving) return

    setSaving(true)
    setError(null)
    try {
      const created = await createCommitment({
        personId: person.id,
        description: description.trim(),
        direction,
        dueAt: new Date(dueAtLocal).toISOString(),
      })
      onCommitmentCreated(created)
      setActiveForm('none')
      setDescription('')
      setDueAtLocal('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el seguimiento.')
    } finally {
      setSaving(false)
    }
  }

  async function handleCreateNote(event: FormEvent) {
    event.preventDefault()
    if (!noteText.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const created = await createNote({ title: noteText.trim(), personId: person.id })
      onNoteCreated(created)
      setActiveForm('none')
      setNoteText('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la nota.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.iconButton} aria-label={`Ver ${person.name}`}>
        <Eye width={16} height={16} />
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
              <div className={shellStyles.headerRow}>
                <Heading slot="title" className={shellStyles.heading}>
                  {person.name}
                </Heading>
                <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                  ×
                </button>
              </div>

              {(person.role || person.organization) && (
                <p className={styles.detailMeta}>
                  {[person.role, person.organization].filter(Boolean).join(' · ')}
                </p>
              )}

              <p className={styles.detailMeta}>
                {lastInteractionAt ? `Última interacción: ${formatRelativeDate(lastInteractionAt)}` : 'Sin interacciones registradas todavía.'}
              </p>

              <h3 className={styles.detailSectionTitle}>Compromisos abiertos</h3>
              {openCommitments.length === 0 && <p className={styles.emptyHint}>Sin compromisos abiertos.</p>}
              <ul className={styles.detailList}>
                {openCommitments.map((c) => (
                  <li key={c.id}>
                    {c.direction === 'MINE' ? 'Tú → ' : '← '}
                    {c.description} — {formatDate(c.dueAt)}
                  </li>
                ))}
              </ul>

              <h3 className={styles.detailSectionTitle}>Proyectos</h3>
              {projects.length === 0 && <p className={styles.emptyHint}>Sin proyectos vinculados.</p>}
              <ul className={styles.detailList}>
                {projects.map((p) => (
                  <li key={p.id}>{p.name}</li>
                ))}
              </ul>

              <h3 className={styles.detailSectionTitle}>Tareas</h3>
              {tasks.length === 0 && <p className={styles.emptyHint}>Sin tareas vinculadas.</p>}
              <ul className={styles.detailList}>
                {tasks.map((t) => (
                  <li key={t.id}>{t.title}</li>
                ))}
              </ul>

              <h3 className={styles.detailSectionTitle}>Notas</h3>
              {notes.length === 0 && <p className={styles.emptyHint}>Sin notas vinculadas.</p>}
              <ul className={styles.detailList}>
                {notes.map((n) => (
                  <li key={n.id}>{n.title}</li>
                ))}
              </ul>

              <h3 className={styles.detailSectionTitle}>Documentos</h3>
              {documents.length === 0 && <p className={styles.emptyHint}>Sin documentos vinculados.</p>}
              <ul className={styles.detailList}>
                {documents.map((d) => (
                  <li key={d.id}>{d.name}</li>
                ))}
              </ul>

              {activeForm === 'none' && (
                <div className={shellStyles.formActions}>
                  <button type="button" data-variant="secondary" onClick={() => setActiveForm('note')}>
                    Nueva nota
                  </button>
                  <button type="button" data-variant="secondary" onClick={() => setActiveForm('commitment')}>
                    Crear seguimiento
                  </button>
                </div>
              )}

              {activeForm === 'note' && (
                <form onSubmit={handleCreateNote}>
                  <h3 className={styles.detailSectionTitle}>Nueva nota</h3>
                  {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Texto</span>
                    <input
                      className={shellStyles.textInput}
                      value={noteText}
                      onChange={(e) => setNoteText(e.target.value)}
                      placeholder="Pidió agregar un módulo de inventario…"
                      autoFocus
                    />
                  </label>

                  <div className={shellStyles.formActions}>
                    {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                    <button type="button" data-variant="secondary" onClick={() => setActiveForm('none')} disabled={saving}>
                      Cancelar
                    </button>
                    <button type="submit" disabled={saving}>
                      Guardar
                    </button>
                  </div>
                </form>
              )}

              {activeForm === 'commitment' && (
                <form onSubmit={handleCreateCommitment}>
                  <h3 className={styles.detailSectionTitle}>Nuevo seguimiento</h3>
                  {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Próxima acción</span>
                    <input
                      className={shellStyles.textInput}
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Confirmar aprobación de la propuesta"
                      autoFocus
                    />
                  </label>

                  <fieldset className={styles.directionFieldset}>
                    <label>
                      <input type="radio" name="direction" checked={direction === 'MINE'} onChange={() => setDirection('MINE')} />
                      Yo debo actuar
                    </label>
                    <label>
                      <input type="radio" name="direction" checked={direction === 'THEIRS'} onChange={() => setDirection('THEIRS')} />
                      Espero a {person.name.split(' ')[0]}
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

                  <div className={shellStyles.formActions}>
                    {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                    <button type="button" data-variant="secondary" onClick={() => setActiveForm('none')} disabled={saving}>
                      Cancelar
                    </button>
                    <button type="submit" disabled={saving}>
                      Guardar
                    </button>
                  </div>
                </form>
              )}
            </div>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
