import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconUsers } from '../../core/ui/icons'
import { deletePerson, listPeople, type Person } from './api'
import { listCommitments, type Commitment } from '../commitments/api'
import { listReminders, type Reminder } from '../reminders/api'
import { listProjects, type Project } from '../projects/api'
import { listNotes } from '../calendar/notes/api'
import type { Note } from '../calendar/notes/notesData'
import { listDocuments, type VidaDocument } from '../documents/api'
import { CreatePersonDialog } from './CreatePersonDialog'
import { PersonDetailDialog } from './PersonDetailDialog'
import styles from './PeoplePage.module.css'

/**
 * ADR-016/FR-021, UC-18. Núcleo del Módulo Laboral — lista de Personas con
 * detalle (compromisos/proyectos/tareas vinculados, ver PersonDetailDialog)
 * y creación de seguimiento embebida (UC-18).
 */
export function PeoplePage() {
  const [people, setPeople] = useState<Person[]>([])
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [notes, setNotes] = useState<Note[]>([])
  const [documents, setDocuments] = useState<VidaDocument[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [peoplePage, commitmentsPage, remindersPage, projectsPage, notesPage, documentsPage] = await Promise.all([
        listPeople(),
        listCommitments(),
        listReminders(),
        listProjects(),
        listNotes(),
        listDocuments(),
      ])
      setPeople(peoplePage.items)
      setCommitments(commitmentsPage.items)
      setTasks(remindersPage.items)
      setProjects(projectsPage.items)
      setNotes(notesPage.items)
      setDocuments(documentsPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las personas.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(person: Person) {
    setPeople((current) => [person, ...current])
  }

  function handleCommitmentCreated(commitment: Commitment) {
    setCommitments((current) => [commitment, ...current])
  }

  function handleNoteCreated(note: Note) {
    setNotes((current) => [note, ...current])
  }

  async function handleDelete(id: string) {
    await deletePerson(id)
    setPeople((current) => current.filter((p) => p.id !== id))
  }

  return (
    <AppShell title="Personas" subtitle="Clientes, colegas y proveedores — el mismo modelo para cualquier profesión.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Todas las personas" action={<CreatePersonDialog onCreated={handleCreated} />}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && people.length === 0 && <p className={styles.emptyHint}>Todavía no has agregado ninguna persona.</p>}
        {!loading &&
          people.map((person) => {
            const openCount = commitments.filter((c) => c.personId === person.id && c.status === 'OPEN').length
            return (
              <ListItemRow
                key={person.id}
                title={person.name}
                subtitle={[person.role, person.organization].filter(Boolean).join(' · ') || undefined}
                icon={IconUsers}
                tone={openCount > 0 ? 'warning' : 'success'}
                pillLabel={openCount > 0 ? `${openCount} pendiente${openCount === 1 ? '' : 's'}` : 'Sin pendientes'}
                pillTone={openCount > 0 ? 'warning' : 'success'}
                trailing={
                  <div className={styles.rowActions}>
                    <PersonDetailDialog
                      person={person}
                      commitments={commitments.filter((c) => c.personId === person.id)}
                      tasks={tasks.filter((t) => t.personId === person.id)}
                      projects={projects.filter((p) => p.clientPersonId === person.id)}
                      notes={notes.filter((n) => n.personId === person.id)}
                      documents={documents.filter((d) => d.personId === person.id)}
                      onCommitmentCreated={handleCommitmentCreated}
                      onNoteCreated={handleNoteCreated}
                    />
                    <SimpleDeleteConfirm
                      resourceLabel="persona"
                      itemName={person.name}
                      ariaLabel="Eliminar persona"
                      onConfirm={() => handleDelete(person.id)}
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
