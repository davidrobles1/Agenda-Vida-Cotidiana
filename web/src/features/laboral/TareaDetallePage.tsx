import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { IconCheck, IconChevronLeft, IconFolder, IconRepeat } from '../../core/ui/icons'
import { completeReminder, listReminders, type Reminder } from '../reminders/api'
import { listPeople, type Person } from '../people/api'
import { listProjects, type Project } from '../projects/api'
import { useVocabulary } from '../../core/user/useVocabulary'
import { formatShortDate, initialsOf, isOverdue, relativeLabel } from './taskSummary'
import styles from './TareaDetallePage.module.css'

/**
 * ADR-016/FR-023, UC-17. Detalle de una Tarea.
 *
 * Trasladado del prototipo aprobado ("Agenda Laboral", artifact fca1566a,
 * `pageTareaDetalle()`), con su misma estructura y en su mismo orden:
 * breadcrumb → tarjeta con título y badge de estado → fila de indicadores
 * con la fecha límite → chips de Persona/Proyecto → fila de acciones.
 *
 * **Por qué es una página y no un diálogo:** el prototipo la modela con
 * breadcrumb (`Laboral / Tareas / <título>`), y un breadcrumb solo tiene
 * sentido sobre navegación real — es lo que dice al usuario dónde está y lo
 * devuelve al listado. Un modal no puede cumplir esa función.
 *
 * **Datos y lógica: los existentes.** El estado se alterna con el mismo
 * `completeReminder` que usa la lista (AC-005: completar o revertir a
 * pendiente). No se añadió ningún endpoint: la tarea se localiza en la
 * misma respuesta de `listReminders` que ya consumen las demás pantallas.
 *
 * **"Crear seguimiento"** existe en el prototipo cuando la tarea tiene
 * Persona. Aquí lleva al detalle de esa Persona, que es donde vive el
 * formulario real de alta de compromisos (`PersonDetailDialog`) — en vez de
 * duplicar ese formulario en una pantalla nueva.
 */
export function TareaDetallePage() {
  const { taskId } = useParams<{ taskId: string }>()
  const navigate = useNavigate()
  const vocabulary = useVocabulary()

  const [task, setTask] = useState<Reminder | null>(null)
  const [person, setPerson] = useState<Person | null>(null)
  const [project, setProject] = useState<Project | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [remindersPage, peoplePage, projectsPage] = await Promise.all([
          listReminders(),
          listPeople(),
          listProjects(),
        ])
        if (cancelled) return

        const found = remindersPage.items.find((r) => r.id === taskId) ?? null
        setTask(found)
        setPerson(found?.personId ? (peoplePage.items.find((p) => p.id === found.personId) ?? null) : null)
        setProject(found?.projectId ? (projectsPage.items.find((p) => p.id === found.projectId) ?? null) : null)
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'No se pudo cargar la tarea.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [taskId])

  async function handleToggle() {
    if (!task || saving) return
    setSaving(true)
    setError(null)
    try {
      setTask(await completeReminder(task.id, task.version))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo actualizar la tarea.')
    } finally {
      setSaving(false)
    }
  }

  const done = task?.status === 'COMPLETED'

  /* Badge del prototipo: "Completada" si lo está; si no, la fecha relativa,
     en rojo cuando ya pasó. */
  const badge = done
    ? { label: 'Completada', tone: 'success' }
    : { label: relativeLabel(task?.dueAt), tone: isOverdue(task?.dueAt) ? 'error' : 'info' }

  return (
    <AppShell title="Tarea" subtitle={task?.title}>
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      {/* Breadcrumb del prototipo: siempre arranca en "Laboral" con un
          galón, y el último tramo —el título— es la posición actual. */}
      <nav className={styles.breadcrumb} aria-label="Ruta de navegación">
        <button type="button" className={styles.breadcrumbLink} onClick={() => navigate('/laboral/hoy')}>
          <IconChevronLeft width={14} height={14} aria-hidden="true" />
          Laboral
        </button>
        <span className={styles.breadcrumbSep} aria-hidden="true">
          /
        </span>
        <button type="button" className={styles.breadcrumbLink} onClick={() => navigate('/laboral/tasks')}>
          Tareas
        </button>
        {task && (
          <>
            <span className={styles.breadcrumbSep} aria-hidden="true">
              /
            </span>
            <span className={styles.breadcrumbCurrent} aria-current="page">
              {task.title}
            </span>
          </>
        )}
      </nav>

      {loading && <p className={styles.emptyHint}>Cargando…</p>}

      {!loading && !task && (
        <div className={styles.emptyState}>
          <IconCheck width={28} height={28} aria-hidden="true" />
          <p>No se encontró esa tarea.</p>
        </div>
      )}

      {!loading && task && (
        <section className={styles.card}>
          <div className={styles.cardHeader}>
            <h2 className={styles.taskTitle}>{task.title}</h2>
            <span className={styles.badge} data-tone={badge.tone}>
              {badge.label}
            </span>
          </div>

          <div className={styles.statRow}>
            <div className={styles.stat}>
              <div className={styles.statLbl}>Fecha límite</div>
              <div className={styles.statNum}>{formatShortDate(task.dueAt)}</div>
            </div>
          </div>

          {/* Persona y Proyecto: chips que llevan a su propio detalle. Las
              listas abren ese detalle desde `?open=<id>`, así que se
              reutiliza el diálogo que ya existe en cada una. */}
          <div className={styles.linkRow}>
            {person ? (
              <button
                type="button"
                className={`${styles.chip} ${styles.chipLink}`}
                onClick={() => navigate(`/laboral/people?open=${person.id}`)}
              >
                <span className={styles.chipAvatar} aria-hidden="true">
                  {initialsOf(person.name)}
                </span>
                {person.name}
              </button>
            ) : (
              <span className={styles.linkEmpty}>
                Sin {vocabulary.person.toLowerCase()} asociad{vocabulary.personGender === 'f' ? 'a' : 'o'}
              </span>
            )}

            {project ? (
              <button
                type="button"
                className={`${styles.chip} ${styles.chipPlain} ${styles.chipLink}`}
                onClick={() => navigate(`/laboral/projects?open=${project.id}`)}
              >
                <IconFolder width={13} height={13} aria-hidden="true" />
                {project.name}
              </button>
            ) : (
              <span className={styles.linkEmpty}>
                Sin {vocabulary.project.toLowerCase()} asociad{vocabulary.projectGender === 'f' ? 'a' : 'o'}
              </span>
            )}
          </div>

          <div className={styles.actionRow}>
            <button type="button" className={styles.ghostButton} disabled={saving} onClick={() => void handleToggle()}>
              <IconCheck width={16} height={16} aria-hidden="true" />
              {done ? 'Marcar como pendiente' : 'Marcar como completada'}
            </button>

            {person && (
              <button
                type="button"
                className={styles.ghostButton}
                onClick={() => navigate(`/laboral/people?open=${person.id}`)}
              >
                <IconRepeat width={16} height={16} aria-hidden="true" />
                Crear seguimiento
              </button>
            )}
          </div>
        </section>
      )}
    </AppShell>
  )
}
