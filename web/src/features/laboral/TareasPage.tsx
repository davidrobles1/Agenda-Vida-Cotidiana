import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconCheck, IconCheckSquare, IconFolder } from '../../core/ui/icons'
import { completeReminder, deleteReminder, listReminders, type Reminder } from '../reminders/api'
import { listPeople, type Person } from '../people/api'
import { listProjects, type Project } from '../projects/api'
import { listPlaces, type Place } from '../places/api'
import { useVocabulary } from '../../core/user/useVocabulary'
import { CreateTaskDialog } from './CreateTaskDialog'
import { initialsOf, relativeLabel } from './taskSummary'
import styles from './TareasPage.module.css'

/** Las cuatro pestañas del prototipo, en su orden. */
const FILTERS = ['todas', 'hoy', 'proximas', 'hechas'] as const
type TaskFilter = (typeof FILTERS)[number]

const FILTER_LABELS: Record<TaskFilter, string> = {
  todas: 'Todas',
  hoy: 'Hoy',
  proximas: 'Próximas',
  hechas: 'Completadas',
}

function todayKey(): string {
  return new Date().toISOString().slice(0, 10)
}


/**
 * ADR-016/FR-023, UC-17. "Tareas" del contexto Laboral — una vista propia
 * sobre REMINDER (context=LABORAL), separada de RemindersPage.tsx a
 * propósito: RemindersPage es compartido con Personal y no se tocó, para no
 * arriesgar ningún comportamiento existente ahí.
 *
 * RETIRADA Y RESTAURADA. Se quitó de la navegación el 2026-08-28 y se
 * devolvió el 2026-08-29, ambas veces por pedido explícito del usuario
 * ("debe estar siempre en este módulo"). Ocupa el mismo sitio que en el
 * prototipo: tercera sección del navbar de Laboral, entre Agenda y
 * Personas, con la casilla como icono. Durante ese paréntesis las Tareas
 * nunca dejaron de existir como dato (`REMINDER` con `context=LABORAL`):
 * seguían viéndose en la Agenda y en la línea de tiempo de Hoy.
 *
 * REDISEÑO (2026-08-28): la pantalla se rehízo siguiendo el prototipo
 * aprobado ("Agenda Laboral", artifact fca1566a, `pageTareas()`) — cabecera
 * con antetítulo "Núcleo" y la acción primaria a la derecha, pestañas de
 * filtro (Todas/Hoy/Próximas/Completadas), y la lista con casilla, título
 * con su fecha relativa y chips de Persona/Proyecto.
 *
 * SEGUNDA PASADA DE FIDELIDAD (2026-08-29): se corrigieron cuatro
 * desviaciones respecto al prototipo — la casilla no permitía desmarcar una
 * tarea ya hecha (el prototipo alterna, y el backend lo soporta: AC-005
 * "puede completar o revertir a pendiente"); la línea de detalle añadía la
 * ubicación, que el prototipo no muestra; el estado vacío colgaba fuera de
 * la lista; y la acción primaria heredaba el `button` global de la
 * aplicación (44px de alto, 15px, radio de 20px) en vez del `btn-primary`
 * del prototipo.
 *
 * Diferencias declaradas respecto al prototipo, y por qué:
 * - **Chips no navegables.** El artifact abre `/laboral/personas/:id` y
 *   `/laboral/proyectos/:id`; en la aplicación esos detalles son diálogos
 *   dentro de sus listas, no rutas propias. Los chips conservan su forma
 *   pero no navegan, en vez de inventar un destino.
 * - **El cuerpo de la fila no abre un detalle de tarea.** El artifact lleva
 *   a `/laboral/tareas/:id`, pantalla que no existe aquí.
 * - **Se conserva el borrado**, que el artifact no contempla en ninguna
 *   vista. Quitarlo no habría sido una decisión visual sino funcional: es la
 *   única forma de eliminar una tarea desde la interfaz. Usa el mismo
 *   vocabulario de la fila (`.iconButton`), sin introducir un patrón nuevo.
 */
export function TareasPage() {
  const navigate = useNavigate()
  const vocabulary = useVocabulary()
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [places, setPlaces] = useState<Place[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<TaskFilter>('todas')

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

  /** Mismos cuatro filtros y mismo orden por fecha que el prototipo. */
  const visibleTasks = useMemo(() => {
    const today = todayKey()
    const list = tasks.filter((task) => {
      const done = task.status === 'COMPLETED'
      const dueKey = task.dueAt?.slice(0, 10)
      if (filter === 'hoy') return !done && dueKey === today
      if (filter === 'proximas') return !done && dueKey !== today
      if (filter === 'hechas') return done
      return true
    })

    // Una tarea sin fecha no puede ordenarse por ella: va al final en vez de
    // colarse al principio como haría una cadena vacía.
    return list.sort((a, b) => (a.dueAt ?? '9999').localeCompare(b.dueAt ?? '9999'))
  }, [tasks, filter])

  return (
    <AppShell title="Tareas" subtitle="Acción pendiente, vinculada a la persona o el proyecto correcto.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <div className={styles.pageHead}>
        <div>
          <p className={styles.kicker}>Núcleo</p>
          <h2 className={styles.pageTitle}>Tareas</h2>
        </div>
        <CreateTaskDialog
          people={people}
          projects={projects}
          places={places}
          onCreated={handleCreated}
          onPlaceCreated={handlePlaceCreated}
        />
      </div>

      <div className={styles.tabs} role="tablist" aria-label="Filtrar tareas">
        {FILTERS.map((option) => (
          <button
            key={option}
            type="button"
            role="tab"
            aria-selected={filter === option}
            className={`${styles.tab} ${filter === option ? styles.tabActive : ''}`}
            onClick={() => setFilter(option)}
          >
            {FILTER_LABELS[option]}
          </button>
        ))}
      </div>

      <section className={styles.card}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}

        {!loading && (
          <div className={styles.list}>
            {visibleTasks.length === 0 && (
              <div className={styles.emptyState}>
                <IconCheck width={28} height={28} aria-hidden="true" />
                <p>No hay tareas en esta vista.</p>
              </div>
            )}

            {visibleTasks.map((task) => {
              const person = people.find((p) => p.id === task.personId)
              const project = projects.find((p) => p.id === task.projectId)
              const done = task.status === 'COMPLETED'

              return (
                <div key={task.id} className={styles.listItem}>
                  <button
                    type="button"
                    className={`${styles.iconButton} ${done ? styles.iconButtonDone : ''}`}
                    aria-label={
                      done ? `Marcar ${task.title} como pendiente` : `Marcar ${task.title} como completada`
                    }
                    onClick={() => void handleComplete(task)}
                  >
                    {done ? (
                      <IconCheck width={18} height={18} />
                    ) : (
                      <IconCheckSquare width={18} height={18} />
                    )}
                  </button>

                  {/* El prototipo abre el detalle desde el cuerpo de la
                      fila; la casilla y los chips tienen su propia acción. */}
                  <button
                    type="button"
                    className={styles.itemBody}
                    onClick={() => navigate(`/laboral/tasks/${task.id}`)}
                  >
                    <div className={`${styles.itemTitle} ${done ? styles.itemTitleDone : ''}`}>{task.title}</div>
                    <div className={styles.itemMeta}>{relativeLabel(task.dueAt)}</div>
                  </button>

                  {/* Los dos chips se distinguen a la vista por el avatar y el
                      icono; para un lector de pantalla, por su etiqueta —que
                      además respeta el vocabulario del perfil (UX-015). */}
                  {person && (
                    <button
                      type="button"
                      className={`${styles.chip} ${styles.chipLink}`}
                      aria-label={`${vocabulary.person}: ${person.name}`}
                      onClick={() => navigate(`/laboral/people?open=${person.id}`)}
                    >
                      <span className={styles.chipAvatar} aria-hidden="true">
                        {initialsOf(person.name)}
                      </span>
                      {person.name}
                    </button>
                  )}

                  {project && (
                    <button
                      type="button"
                      className={`${styles.chip} ${styles.chipPlain} ${styles.chipLink}`}
                      aria-label={`${vocabulary.project}: ${project.name}`}
                      onClick={() => navigate(`/laboral/projects?open=${project.id}`)}
                    >
                      <IconFolder width={13} height={13} aria-hidden="true" />
                      {project.name}
                    </button>
                  )}

                  <SimpleDeleteConfirm
                    resourceLabel="tarea"
                    itemName={task.title}
                    ariaLabel="Eliminar tarea"
                    onConfirm={() => handleDelete(task.id)}
                  />
                </div>
              )
            })}
          </div>
        )}
      </section>
    </AppShell>
  )
}
