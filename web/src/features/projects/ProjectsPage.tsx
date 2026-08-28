import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconFolder } from '../../core/ui/icons'
import { listPeople, type Person } from '../people/api'
import { listReminders, type Reminder } from '../reminders/api'
import { listNotes } from '../calendar/notes/api'
import type { Note } from '../calendar/notes/notesData'
import { listDocuments, type VidaDocument } from '../documents/api'
import { listResources, type Resource } from '../resources/api'
import { useVocabulary } from '../../core/user/useVocabulary'
import { article } from '../../core/user/vocabulary'
import { deleteProject, listProjects, type Project } from './api'
import { CreateProjectDialog } from './CreateProjectDialog'
import { ProjectDetailDialog } from './ProjectDetailDialog'
import styles from './ProjectsPage.module.css'

/** ADR-016/FR-022, UC-19. Núcleo del Módulo Laboral. */
export function ProjectsPage() {
  // UX-014/UX-015: solo cambian las palabras (design-system.md §12).
  const vocabulary = useVocabulary()
  const [projects, setProjects] = useState<Project[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [notes, setNotes] = useState<Note[]>([])
  const [documents, setDocuments] = useState<VidaDocument[]>([])
  const [resources, setResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [projectsPage, peoplePage, remindersPage, notesPage, documentsPage, resourcesPage] = await Promise.all([
        listProjects(),
        listPeople(),
        listReminders(),
        listNotes(),
        listDocuments(),
        listResources(),
      ])
      setProjects(projectsPage.items)
      setPeople(peoplePage.items)
      setTasks(remindersPage.items)
      setNotes(notesPage.items)
      setDocuments(documentsPage.items)
      setResources(resourcesPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los proyectos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(project: Project) {
    setProjects((current) => [project, ...current])
  }

  function handleTaskCreated(task: Reminder) {
    setTasks((current) => [task, ...current])
  }

  function handleTaskUpdated(task: Reminder) {
    setTasks((current) => current.map((t) => (t.id === task.id ? task : t)))
  }

  function handleNoteCreated(note: Note) {
    setNotes((current) => [note, ...current])
  }

  function handleResourceCreated(resource: Resource) {
    setResources((current) => [resource, ...current])
  }

  /** ADR-016 Fase 3d: la nota vuelve con su sugerencia ya resuelta. */
  function handleNoteUpdated(note: Note) {
    setNotes((current) => current.map((n) => (n.id === note.id ? note : n)))
  }

  async function handleDelete(id: string) {
    await deleteProject(id)
    setProjects((current) => current.filter((p) => p.id !== id))
  }

  return (
    <AppShell
      title={vocabulary.projectPlural}
      subtitle="Obra, caso, cuenta u oportunidad — el mismo modelo, distinto vocabulario."
    >
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard
        title={`Tod${vocabulary.projectGender === 'f' ? 'a' : 'o'}s ${article.definitePlural(vocabulary.projectGender)} ${vocabulary.projectPlural.toLowerCase()}`}
        action={<CreateProjectDialog people={people} onCreated={handleCreated} />}
      >
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && projects.length === 0 && (
          <p className={styles.emptyHint}>
            Todavía no has creado {article.none(vocabulary.projectGender)} {vocabulary.project.toLowerCase()}.
          </p>
        )}
        {!loading &&
          projects.map((project) => {
            const clientPerson = people.find((p) => p.id === project.clientPersonId)
            const projectTasks = tasks.filter((t) => t.projectId === project.id)
            const doneCount = projectTasks.filter((t) => t.status === 'COMPLETED').length
            return (
              <ListItemRow
                key={project.id}
                title={project.name}
                subtitle={clientPerson?.name}
                icon={IconFolder}
                tone="info"
                pillLabel={project.status || (projectTasks.length ? `${doneCount}/${projectTasks.length} tareas` : undefined)}
                pillTone="info"
                trailing={
                  <div className={styles.rowActions}>
                    <ProjectDetailDialog
                      project={project}
                      clientPerson={clientPerson}
                      tasks={projectTasks}
                      notes={notes.filter((n) => n.projectId === project.id)}
                      documents={documents.filter((d) => d.projectId === project.id)}
                      resources={resources.filter((r) => r.projectId === project.id)}
                      onTaskCreated={handleTaskCreated}
                      onTaskUpdated={handleTaskUpdated}
                      onNoteCreated={handleNoteCreated}
                      onResourceCreated={handleResourceCreated}
                      onNoteUpdated={handleNoteUpdated}
                    />
                    <SimpleDeleteConfirm
                      resourceLabel={vocabulary.project.toLowerCase()}
                      itemName={project.name}
                      ariaLabel={`Eliminar ${vocabulary.project.toLowerCase()}`}
                      onConfirm={() => handleDelete(project.id)}
                    />
                  </div>
                }
              />
            )
          })}
      </ListSectionCard>
    </AppShell>
  )
}
