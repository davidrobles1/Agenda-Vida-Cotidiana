import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { Eye } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { completeReminder, createReminder, type Reminder } from '../reminders/api'
import { createNote } from '../calendar/notes/api'
import type { Note } from '../calendar/notes/notesData'
import type { VidaDocument } from '../documents/api'
import type { Person } from '../people/api'
import type { Project } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import styles from './ProjectsPage.module.css'

const MotionDialog = motion.create(Dialog)

interface ProjectDetailDialogProps {
  project: Project
  clientPerson?: Person
  tasks: Reminder[]
  notes: Note[]
  documents: VidaDocument[]
  onTaskCreated: (task: Reminder) => void
  onTaskUpdated: (task: Reminder) => void
  onNoteCreated: (note: Note) => void
}

type ActiveForm = 'none' | 'task' | 'note'
type TaskView = 'list' | 'kanban'

function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

/**
 * UC-19. Diálogo de solo lectura + "Nueva tarea"/"Nueva nota" embebidas,
 * vinculadas automáticamente a este Proyecto. Mismo patrón que
 * PersonDetailDialog.tsx.
 */
export function ProjectDetailDialog({ project, clientPerson, tasks, notes, documents, onTaskCreated, onTaskUpdated, onNoteCreated }: ProjectDetailDialogProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [activeForm, setActiveForm] = useState<ActiveForm>('none')
  const [taskView, setTaskView] = useState<TaskView>('list')
  const [movingTaskId, setMovingTaskId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [noteText, setNoteText] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) {
      setActiveForm('none')
      setTitle('')
      setDueAtLocal('')
      setNoteText('')
      setError(null)
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
      <Button className={styles.iconButton} aria-label={`Ver ${project.name}`}>
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
                  <button type="button" data-variant="secondary" onClick={() => setActiveForm('task')}>
                    Nueva tarea
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
