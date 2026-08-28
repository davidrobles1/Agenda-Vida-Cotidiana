import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconCheckCircle, IconTarget } from '../../core/ui/icons'
import { deleteObjective, listObjectives, updateObjective, type Objective } from './api'
import { CreateObjectiveDialog } from './CreateObjectiveDialog'
import styles from './ObjectivesPage.module.css'

function progressLabel(objective: Objective): string | undefined {
  if (objective.targetValue === undefined) return undefined
  return `${objective.currentValue}/${objective.targetValue}`
}

/**
 * ADR-016 Fase 3e1/FR-031, UC-24, AC-018. Página dedicada de Objetivos,
 * alcanzable desde "Ver todos" en Hoy — deliberadamente sin entrada propia
 * en el navbar de Laboral (las 7 secciones núcleo quedaron cerradas en la
 * Fase 2/WEB-010, mismo patrón de ruta-real-sin-enlace que ya usan
 * Documentos/Inventario en Personal).
 *
 * El progreso es 100% manual (FR-031): los botones +/- hacen un PATCH real
 * de `currentValue`, y "Cumplido" hace un PATCH de `completed` — nunca se
 * deriva uno del otro, ni en el cliente ni en el backend.
 */
export function ObjectivesPage() {
  const [objectives, setObjectives] = useState<Objective[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listObjectives()
      setObjectives(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los objetivos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(objective: Objective) {
    setObjectives((current) => [objective, ...current])
  }

  async function patchObjective(objective: Objective, changes: { currentValue?: number; completed?: boolean }) {
    setBusyId(objective.id)
    setError(null)
    try {
      const updated = await updateObjective(objective.id, { ...changes, version: objective.version })
      setObjectives((current) => current.map((o) => (o.id === updated.id ? updated : o)))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo actualizar el objetivo.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete(id: string) {
    await deleteObjective(id)
    setObjectives((current) => current.filter((o) => o.id !== id))
  }

  return (
    <AppShell title="Objetivos" subtitle="Las metas que le dan sentido a tus proyectos y tareas.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Todos los objetivos" action={<CreateObjectiveDialog onCreated={handleCreated} />}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && objectives.length === 0 && <p className={styles.emptyHint}>Todavía no has definido ningún objetivo.</p>}
        {!loading &&
          objectives.map((objective) => (
            <ListItemRow
              key={objective.id}
              title={objective.title}
              subtitle={objective.deadline ? `Fecha límite: ${objective.deadline.slice(0, 10)}` : undefined}
              icon={IconTarget}
              tone={objective.completed ? 'success' : 'primary'}
              pillLabel={objective.completed ? 'Cumplido' : progressLabel(objective)}
              pillTone={objective.completed ? 'success' : 'info'}
              trailing={
                <div className={styles.rowActions}>
                  {!objective.completed && objective.targetValue !== undefined && (
                    <div className={styles.progressControls}>
                      <button
                        type="button"
                        className={styles.stepButton}
                        aria-label={`Restar progreso a ${objective.title}`}
                        disabled={busyId === objective.id || objective.currentValue === 0}
                        onClick={() => void patchObjective(objective, { currentValue: objective.currentValue - 1 })}
                      >
                        −
                      </button>
                      <button
                        type="button"
                        className={styles.stepButton}
                        aria-label={`Sumar progreso a ${objective.title}`}
                        disabled={busyId === objective.id}
                        onClick={() => void patchObjective(objective, { currentValue: objective.currentValue + 1 })}
                      >
                        +
                      </button>
                    </div>
                  )}
                  {!objective.completed && (
                    <button
                      type="button"
                      className={styles.completeButton}
                      disabled={busyId === objective.id}
                      onClick={() => void patchObjective(objective, { completed: true })}
                    >
                      <IconCheckCircle width={16} height={16} /> Cumplido
                    </button>
                  )}
                  <SimpleDeleteConfirm
                    resourceLabel="objetivo"
                    itemName={objective.title}
                    ariaLabel="Eliminar objetivo"
                    onConfirm={() => handleDelete(objective.id)}
                  />
                </div>
              }
            />
          ))}
      </ListSectionCard>
    </AppShell>
  )
}
