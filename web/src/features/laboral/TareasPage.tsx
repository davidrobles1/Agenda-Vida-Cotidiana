import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconCheckCircle, IconTasks } from '../../core/ui/icons'
import { completeReminder, deleteReminder, listReminders, type Reminder } from '../reminders/api'
import { listPeople, type Person } from '../people/api'
import { listProjects, type Project } from '../projects/api'
import { listPlaces, type Place } from '../places/api'
import { CreateTaskDialog } from './CreateTaskDialog'
import styles from './TareasPage.module.css'

/**
 * ADR-016/FR-023, UC-17. "Tareas" del contexto Laboral — una vista propia
 * sobre REMINDER (context=LABORAL), separada de RemindersPage.tsx a
 * propósito: RemindersPage es compartido con Personal y no se tocó, para no
 * arriesgar ningún comportamiento existente ahí (pedido explícito del
 * usuario de no modificar nada fuera de lo necesario).
 */
export function TareasPage() {
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [places, setPlaces] = useState<Place[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [remindersPage, peoplePage, projectsPage, placesPage] = await Promise.all([
        listReminders(),
        listPeople(),
        listProjects(),
        listPlaces(),
      ])
      setTasks(remindersPage.items.filter((t) => t.context === 'LABORAL'))
      setPeople(peoplePage.items)
      setProjects(projectsPage.items)
      setPlaces(placesPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las tareas.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(task: Reminder) {
    setTasks((current) => [task, ...current])
  }

  function handlePlaceCreated(place: Place) {
    setPlaces((current) => [place, ...current])
  }

  async function handleComplete(task: Reminder) {
    const updated = await completeReminder(task.id, task.version)
    setTasks((current) => current.map((t) => (t.id === updated.id ? updated : t)))
  }

  async function handleDelete(id: string) {
    await deleteReminder(id)
    setTasks((current) => current.filter((t) => t.id !== id))
  }

  return (
    <AppShell title="Tareas" subtitle="Acción pendiente, vinculada a la persona o el proyecto correcto.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard
        title="Todas las tareas"
        action={
          <CreateTaskDialog
            people={people}
            projects={projects}
            places={places}
            onCreated={handleCreated}
            onPlaceCreated={handlePlaceCreated}
          />
        }
      >
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && tasks.length === 0 && <p className={styles.emptyHint}>Todavía no hay tareas laborales.</p>}
        {!loading &&
          tasks.map((task) => {
            const person = people.find((p) => p.id === task.personId)
            const project = projects.find((p) => p.id === task.projectId)
            const done = task.status === 'COMPLETED'
            return (
              <ListItemRow
                key={task.id}
                title={task.title}
                subtitle={[task.dueAt?.slice(0, 10), person?.name, project?.name, task.location].filter(Boolean).join(' · ') || undefined}
                icon={IconTasks}
                tone={done ? 'success' : 'primary'}
                pillLabel={done ? 'Completada' : undefined}
                pillTone="success"
                trailing={
                  <div className={styles.rowActions}>
                    {!done && (
                      <button type="button" className={styles.completeButton} onClick={() => void handleComplete(task)}>
                        <IconCheckCircle width={16} height={16} /> Completar
                      </button>
                    )}
                    <SimpleDeleteConfirm
                      resourceLabel="tarea"
                      itemName={task.title}
                      ariaLabel="Eliminar tarea"
                      onConfirm={() => handleDelete(task.id)}
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
