import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconCheckCircle, IconRepeat } from '../../core/ui/icons'
import {
  deleteRoutine,
  executeRoutine,
  FREQUENCY_LABELS,
  listRoutines,
  updateRoutine,
  type Routine,
} from './api'
import { CreateRoutineDialog } from './CreateRoutineDialog'
import styles from './RoutinesPage.module.css'

function daysUntil(iso: string): number {
  return Math.round((new Date(iso).getTime() - Date.now()) / 86400000)
}

/**
 * ADR-016 Fase 3e2/FR-032, UC-25, AC-019. Página dedicada de Rutinas,
 * alcanzable desde "Ver todas" en Hoy — sin entrada propia en el navbar de
 * Laboral (las 7 secciones núcleo quedaron cerradas en la Fase 2/WEB-010),
 * mismo criterio que Objetivos.
 *
 * "Hecha" llama a `POST /routines/{id}/execute` real: el backend avanza
 * `nextExecutionDate` un periodo desde la fecha programada. El cliente no
 * calcula la fecha — y no crea ninguna Tarea ni Compromiso (FR-032).
 */
export function RoutinesPage() {
  const [routines, setRoutines] = useState<Routine[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listRoutines()
      setRoutines(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las rutinas.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(routine: Routine) {
    setRoutines((current) => [routine, ...current])
  }

  async function handleExecute(routine: Routine) {
    setBusyId(routine.id)
    setError(null)
    try {
      const updated = await executeRoutine(routine.id, routine.version)
      setRoutines((current) => current.map((r) => (r.id === updated.id ? updated : r)))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo marcar la rutina.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleToggleActive(routine: Routine) {
    setBusyId(routine.id)
    setError(null)
    try {
      const updated = await updateRoutine(routine.id, { active: !routine.active, version: routine.version })
      setRoutines((current) => current.map((r) => (r.id === updated.id ? updated : r)))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo actualizar la rutina.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete(id: string) {
    await deleteRoutine(id)
    setRoutines((current) => current.filter((r) => r.id !== id))
  }

  return (
    <AppShell title="Rutinas" subtitle="Lo que haces una y otra vez, sin que se te olvide.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Todas las rutinas" action={<CreateRoutineDialog onCreated={handleCreated} />}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && routines.length === 0 && <p className={styles.emptyHint}>Todavía no has creado ninguna rutina.</p>}
        {!loading &&
          routines.map((routine) => {
            const due = daysUntil(routine.nextExecutionDate)
            const overdue = routine.active && due < 0
            return (
              <ListItemRow
                key={routine.id}
                title={routine.title}
                subtitle={[
                  FREQUENCY_LABELS[routine.frequency],
                  `Próxima: ${routine.nextExecutionDate.slice(0, 10)}`,
                  routine.description,
                ]
                  .filter(Boolean)
                  .join(' · ')}
                icon={IconRepeat}
                tone={!routine.active ? 'info' : overdue ? 'error' : 'primary'}
                pillLabel={!routine.active ? 'En pausa' : overdue ? 'Atrasada' : undefined}
                pillTone={!routine.active ? 'info' : 'error'}
                trailing={
                  <div className={styles.rowActions}>
                    {routine.active && (
                      <button
                        type="button"
                        className={styles.completeButton}
                        aria-label={`Marcar ${routine.title} como hecha`}
                        disabled={busyId === routine.id}
                        onClick={() => void handleExecute(routine)}
                      >
                        <IconCheckCircle width={16} height={16} /> Hecha
                      </button>
                    )}
                    <button
                      type="button"
                      className={styles.pauseButton}
                      aria-label={`${routine.active ? 'Pausar' : 'Reanudar'} ${routine.title}`}
                      disabled={busyId === routine.id}
                      onClick={() => void handleToggleActive(routine)}
                    >
                      {routine.active ? 'Pausar' : 'Reanudar'}
                    </button>
                    <SimpleDeleteConfirm
                      resourceLabel="rutina"
                      itemName={routine.title}
                      ariaLabel="Eliminar rutina"
                      onConfirm={() => handleDelete(routine.id)}
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
