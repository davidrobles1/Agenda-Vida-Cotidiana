import { useState, type FormEvent } from 'react'
import { Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { completeReminder, createReminder, type Reminder } from '../reminders/api'
import { createNote, resolveNoteTaskSuggestion } from '../calendar/notes/api'
import type { Note } from '../calendar/notes/notesData'
import type { VidaDocument } from '../documents/api'
import {
  createResource,
  RESOURCE_TYPE_LABELS,
  type Resource,
  type ResourceType,
} from '../resources/api'
import type { Person } from '../people/api'
import type { Project } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import styles from './ProjectsPage.module.css'

const MotionDialog = motion.create(Dialog)

interface ProjectDetailDialogProps {
  /** Apertura controlada desde ProjectsPage (fila de la lista o `?open=`). */
  isOpen: boolean
  onOpenChange: (open: boolean) => void
  project: Project
  clientPerson?: Person
  tasks: Reminder[]
  notes: Note[]
  documents: VidaDocument[]
  /** ADR-016 Fase 3e4/FR-034 — recursos ya vinculados a este Proyecto. */
  resources: Resource[]
  onTaskCreated: (task: Reminder) => void
  onTaskUpdated: (task: Reminder) => void
  onNoteCreated: (note: Note) => void
  onResourceCreated: (resource: Resource) => void
  /** ADR-016 Fase 3d/FR-035 — refleja la nota tras resolver su sugerencia. */
  onNoteUpdated: (note: Note) => void
}

type ActiveForm = 'none' | 'task' | 'note' | 'resource'
type TaskView = 'list' | 'kanban'

function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

/**
 * UC-19. Diálogo de solo lectura + "Nueva tarea"/"Nueva nota" embebidas,
 * vinculadas automáticamente a este Proyecto. Mismo patrón que
 * PersonDetailDialog.tsx, incluida la apertura controlada: la fila de la
 * lista es el disparador, y `?open=<id>` permite abrirlo directamente —
 * es el destino de los chips de Proyecto desde una Tarea.
 */
export function ProjectDetailDialog({ project, isOpen, onOpenChange, clientPerson, tasks, notes, documents, resources, onTaskCreated, onTaskUpdated, onNoteCreated, onResourceCreated, onNoteUpdated }: ProjectDetailDialogProps) {
  const [activeForm, setActiveForm] = useState<ActiveForm>('none')
  const [taskView, setTaskView] = useState<TaskView>('list')
  const [movingTaskId, setMovingTaskId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [noteText, setNoteText] = useState('')
  const [resourceName, setResourceName] = useState('')
  const [resourceType, setResourceType] = useState<ResourceType>('ENLACE')
  const [resourceReference, setResourceReference] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  /** ADR-016 Fase 3d: id de la nota cuya sugerencia se está mostrando. */
  const [suggestingNoteId, setSuggestingNoteId] = useState<string | null>(null)
  const [suggestedTitle, setSuggestedTitle] = useState('')

  /**
   * ADR-016 Fase 3d/FR-035, UC-28. Disparador **manual** — solo se abre al
   * pulsar "Sugerir tarea" en una nota concreta. Nada automático.
   */
  function startSuggestion(note: Note) {
    setSuggestingNoteId(note.id)
    setSuggestedTitle(note.title)
    setError(null)
  }

  /** Convertir: crea la Tarea real y marca la sugerencia como resuelta. */
  async function handleConvertSuggestion(note: Note) {
    if (!suggestedTitle.trim() || saving) return
    setSaving(true)
    setError(null)
    try {
      const created = await createReminder({
        title: suggestedTitle.trim(),
        context: 'LABORAL',
        projectId: project.id,
        ...(note.personId ? { personId: note.personId } : {}),
      })
      onTaskCreated(created)
      const updated = await resolveNoteTaskSuggestion(note.id, note.version)
      onNoteUpdated(updated)
      setSuggestingNoteId(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo crear la tarea.')
    } finally {
      setSaving(false)
    }
  }

  /** Descartar: mismo camino, sin crear la Tarea. La sugerencia no vuelve. */
  async function handleDismissSuggestion(note: Note) {
    if (saving) return
    setSaving(true)
    setError(null)
    try {
      const updated = await resolveNoteTaskSuggestion(note.id, note.version)
      onNoteUpdated(updated)
      setSuggestingNoteId(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo descartar la sugerencia.')
    } finally {
      setSaving(false)
    }
  }

  function handleOpenChange(open: boolean) {
    onOpenChange(open)
    if (!open) {
      setActiveForm('none')
      setTitle('')
      setDueAtLocal('')
      setNoteText('')
      setResourceName('')
      setResourceType('ENLACE')
      setResourceReference('')
      setError(null)
    }
  }

  /** UC-27: alta embebida, vinculada automáticamente a este Proyecto. */
  async function handleCreateResource(event: FormEvent) {
    event.preventDefault()
    if (!resourceName.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const created = await createResource({
        name: resourceName.trim(),
        type: resourceType,
        reference: resourceReference.trim() || undefined,
        projectId: project.id,
      })
      onResourceCreated(created)
      setActiveForm('none')
      setResourceName('')
      setResourceReference('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el recurso.')
    } finally {
      setSaving(false)
    }
  }

  async function handleCreateTask(event: FormEvent) {
    event.preventDefault()
    if (!title.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const created = await createReminder({
        title: title.trim(),
        context: 'LABORAL',
        projectId: project.id,
        dueAt: dueAtLocal ? new Date(dueAtLocal).toISOString() : undefined,
      })
      onTaskCreated(created)
      setActiveForm('none')
      setTitle('')
      setDueAtLocal('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la tarea.')
    } finally {
      setSaving(false)
    }
  }

  /**
   * ADR-016 Fase 3f (candidato V4): vista Kanban de Tareas por estado — solo
   * UI, sobre el mismo `REMINDER.status` de siempre (PENDING/COMPLETED, sin
   * inventar estados intermedios como "en progreso" que el backend no
   * modela). "Mover" una tarjeta entre columnas reutiliza `completeReminder`
   * tal cual, mismo endpoint que el botón "Completar" de TareasPage.tsx.
   */
  async function handleMoveTask(task: Reminder) {
    setMovingTaskId(task.id)
    try {
      const updated = await completeReminder(task.id, task.version)
      onTaskUpdated(updated)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo mover la tarea.')
    } finally {
      setMovingTaskId(null)
    }
  }

  async function handleCreateNote(event: FormEvent) {
    event.preventDefault()
    if (!noteText.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const created = await createNote({ title: noteText.trim(), projectId: project.id })
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
                  {project.name}
                </Heading>
                <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                  ×
                </button>
              </div>

              <p className={styles.detailMeta}>
                {[clientPerson?.name, project.status, project.deadline && `Entrega: ${formatDate(project.deadline)}`]
                  .filter(Boolean)
                  .join(' · ') || 'Sin detalles adicionales.'}
              </p>

              <div className={styles.taskSectionHeader}>
                <h3 className={styles.detailSectionTitle}>Tareas</h3>
                {tasks.length > 0 && (
                  <div className={styles.taskViewToggle} role="tablist" aria-label="Vista de tareas">
                    <button
                      type="button"
                      role="tab"
                      aria-selected={taskView === 'list'}
                      className={taskView === 'list' ? styles.taskViewButtonActive : styles.taskViewButton}
                      onClick={() => setTaskView('list')}
                    >
                      Lista
                    </button>
                    <button
                      type="button"
                      role="tab"
                      aria-selected={taskView === 'kanban'}
                      className={taskView === 'kanban' ? styles.taskViewButtonActive : styles.taskViewButton}
                      onClick={() => setTaskView('kanban')}
                    >
                      Kanban
                    </button>
                  </div>
                )}
              </div>

              {tasks.length === 0 && <p className={styles.emptyHint}>Sin tareas todavía.</p>}

              {tasks.length > 0 && taskView === 'list' && (
                <ul className={styles.detailList}>
                  {tasks.map((t) => (
                    <li key={t.id}>
                      {t.title} {t.status === 'COMPLETED' ? '(completada)' : ''}
                    </li>
                  ))}
                </ul>
              )}

              {tasks.length > 0 && taskView === 'kanban' && (
                <div className={styles.kanbanBoard}>
                  {(['PENDING', 'COMPLETED'] as const).map((status) => (
                    <div key={status} className={styles.kanbanColumn}>
                      <div className={styles.kanbanColumnTitle}>
                        {status === 'PENDING' ? 'Pendientes' : 'Completadas'} ({tasks.filter((t) => t.status === status).length})
                      </div>
                      {tasks
                        .filter((t) => t.status === status)
                        .map((t) => (
                          <button
                            key={t.id}
                            type="button"
                            className={styles.kanbanCard}
                            disabled={movingTaskId === t.id}
                            onClick={() => void handleMoveTask(t)}
                            title={status === 'PENDING' ? 'Mover a Completadas' : 'Mover a Pendientes'}
                          >
                            {t.title}
                          </button>
                        ))}
                    </div>
                  ))}
                </div>
              )}

              <h3 className={styles.detailSectionTitle}>Notas</h3>
              {notes.length === 0 && <p className={styles.emptyHint}>Sin notas todavía.</p>}
              <ul className={styles.detailList}>
                {notes.map((n) => (
                  <li key={n.id}>
                    <div className={styles.noteRow}>
                      <span>{n.title}</span>
                      {/* ADR-016 Fase 3d/FR-035, UC-28: disparador manual. */}
                      {!n.taskSuggestionResolved && suggestingNoteId !== n.id && (
                        <button
                          type="button"
                          className={styles.suggestButton}
                          aria-label={`Sugerir tarea desde la nota: ${n.title}`}
                          onClick={() => startSuggestion(n)}
                        >
                          Sugerir tarea
                        </button>
                      )}
                    </div>

                    {suggestingNoteId === n.id && (
                      <div className={styles.suggestionBox}>
                        <label className={shellStyles.field}>
                          <span className={shellStyles.fieldLabel}>Tarea sugerida</span>
                          <input
                            className={shellStyles.textInput}
                            value={suggestedTitle}
                            onChange={(e) => setSuggestedTitle(e.target.value)}
                            autoFocus
                          />
                        </label>
                        <div className={shellStyles.formActions}>
                          {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                          <button
                            type="button"
                            data-variant="secondary"
                            disabled={saving}
                            onClick={() => void handleDismissSuggestion(n)}
                          >
                            Descartar
                          </button>
                          <button type="button" disabled={saving} onClick={() => void handleConvertSuggestion(n)}>
                            Crear tarea
                          </button>
                        </div>
                      </div>
                    )}
                  </li>
                ))}
              </ul>

              <h3 className={styles.detailSectionTitle}>Documentos</h3>
              {documents.length === 0 && <p className={styles.emptyHint}>Sin documentos vinculados.</p>}
              <ul className={styles.detailList}>
                {documents.map((d) => (
                  <li key={d.id}>{d.name}</li>
                ))}
              </ul>

              {/* ADR-016 Fase 3e4/FR-034, UC-27. Referencia de texto, nunca un archivo. */}
              <h3 className={styles.detailSectionTitle}>Recursos</h3>
              {resources.length === 0 && <p className={styles.emptyHint}>Sin recursos vinculados.</p>}
              <ul className={styles.detailList}>
                {resources.map((r) => (
                  <li key={r.id}>
                    {r.name} <span className={styles.resourceType}>({RESOURCE_TYPE_LABELS[r.type]})</span>
                    {r.reference && <> — {r.reference}</>}
                  </li>
                ))}
              </ul>

              {activeForm === 'none' && (
                <div className={shellStyles.formActions}>
                  <button type="button" data-variant="secondary" onClick={() => setActiveForm('note')}>
                    Nueva nota
                  </button>
                  <button type="button" data-variant="secondary" onClick={() => setActiveForm('resource')}>
                    Nuevo recurso
                  </button>
                  <button type="button" data-variant="secondary" onClick={() => setActiveForm('task')}>
                    Nueva tarea
                  </button>
                </div>
              )}

              {activeForm === 'resource' && (
                <form onSubmit={handleCreateResource}>
                  <h3 className={styles.detailSectionTitle}>Nuevo recurso</h3>
                  {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Nombre</span>
                    <input
                      className={shellStyles.textInput}
                      value={resourceName}
                      onChange={(e) => setResourceName(e.target.value)}
                      placeholder="Plantilla de acta de obra"
                      autoFocus
                    />
                  </label>

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Tipo</span>
                    <select
                      className={shellStyles.textInput}
                      value={resourceType}
                      onChange={(e) => setResourceType(e.target.value as ResourceType)}
                    >
                      {(Object.keys(RESOURCE_TYPE_LABELS) as ResourceType[]).map((t) => (
                        <option key={t} value={t}>
                          {RESOURCE_TYPE_LABELS[t]}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Referencia (opcional)</span>
                    <input
                      className={shellStyles.textInput}
                      value={resourceReference}
                      onChange={(e) => setResourceReference(e.target.value)}
                      placeholder="Una URL, una carpeta compartida, una ubicación…"
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
                      placeholder="Confirmar cuadrilla disponible desde el lunes…"
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

              {activeForm === 'task' && (
                <form onSubmit={handleCreateTask}>
                  <h3 className={styles.detailSectionTitle}>Nueva tarea</h3>
                  {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Título</span>
                    <input
                      className={shellStyles.textInput}
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder="Enviar documentación técnica"
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
