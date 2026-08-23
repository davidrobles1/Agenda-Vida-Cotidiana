import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { IconRepeat } from '../../core/ui/icons'
import { listPeople, type Person } from '../people/api'
import { listProjects, type Project } from '../projects/api'
import { deleteCommitment, listCommitments, resolveCommitment, type Commitment, type CommitmentDirection } from './api'
import { CreateCommitmentDialog } from './CreateCommitmentDialog'
import styles from './CommitmentsPage.module.css'

function daysUntil(iso: string): number {
  const due = new Date(iso)
  const now = new Date()
  return Math.round((due.getTime() - now.getTime()) / 86400000)
}

function relativeLabel(iso: string): string {
  const n = daysUntil(iso)
  if (n === 0) return 'Hoy'
  if (n === 1) return 'Mañana'
  if (n < 0) return `Hace ${-n} día${-n === 1 ? '' : 's'} · atrasado`
  return `En ${n} días`
}

/**
 * ADR-016/FR-025/FR-027, UC-18/UC-20. "Seguimientos" (direction=MINE) /
 * "Esperando" (direction=THEIRS) — misma entidad, dos pestañas. No se usó
 * `ToggleButtonGroup`/`FilterChip` (requieren selección múltiple o un
 * contexto que este toggle de 2 valores no necesita) — dos botones simples
 * con estado activo, mismo patrón visual que el `modePill` de `AppShell`.
 */
export function CommitmentsPage() {
  const [direction, setDirection] = useState<CommitmentDirection>('MINE')
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function refresh(currentDirection: CommitmentDirection) {
    setLoading(true)
    setError(null)
    try {
      const [commitmentsPage, peoplePage, projectsPage] = await Promise.all([
        listCommitments(currentDirection),
        listPeople(),
        listProjects(),
      ])
      setCommitments(commitmentsPage.items)
      setPeople(peoplePage.items)
      setProjects(projectsPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los seguimientos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh(direction)
  }, [direction])

  function handleCreated(commitment: Commitment) {
    if (commitment.direction === direction) {
      setCommitments((current) => [commitment, ...current])
    }
  }

  async function handleResolve(commitment: Commitment) {
    const updated = await resolveCommitment(commitment.id, commitment.version)
    setCommitments((current) => current.filter((c) => c.id !== updated.id))
  }

  async function handleDelete(id: string) {
    await deleteCommitment(id)
    setCommitments((current) => current.filter((c) => c.id !== id))
  }

  const openCommitments = commitments.filter((c) => c.status === 'OPEN')

  return (
    <AppShell title="Seguimientos" subtitle="Una sola idea — Compromiso — vista según quién debe actuar.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <div className={styles.tabs} role="tablist" aria-label="Dirección del compromiso">
        <button
          type="button"
          role="tab"
          aria-selected={direction === 'MINE'}
          className={`${styles.tab} ${direction === 'MINE' ? styles.tabActive : ''}`}
          onClick={() => setDirection('MINE')}
        >
          Mías
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={direction === 'THEIRS'}
          className={`${styles.tab} ${direction === 'THEIRS' ? styles.tabActive : ''}`}
          onClick={() => setDirection('THEIRS')}
        >
          Esperando
        </button>
      </div>

      <ListSectionCard
        title={direction === 'MINE' ? 'Mías' : 'Esperando'}
        action={<CreateCommitmentDialog people={people} projects={projects} onCreated={handleCreated} />}
      >
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && openCommitments.length === 0 && (
          <p className={styles.emptyHint}>Nada por aquí — buen momento para respirar.</p>
        )}
        {!loading &&
          openCommitments.map((commitment) => {
            const person = people.find((p) => p.id === commitment.personId)
            const project = projects.find((p) => p.id === commitment.projectId)
            const overdue = daysUntil(commitment.dueAt) < 0
            return (
              <ListItemRow
                key={commitment.id}
                title={`${person?.name ?? '—'} ${direction === 'MINE' ? '→' : '←'} ${commitment.description}`}
                subtitle={[relativeLabel(commitment.dueAt), project?.name].filter(Boolean).join(' · ')}
                icon={IconRepeat}
                tone={overdue ? 'error' : 'info'}
                pillLabel={overdue ? 'Atrasado' : undefined}
                pillTone="error"
                trailing={
                  <div className={styles.rowActions}>
                    <button type="button" className={styles.resolveButton} onClick={() => void handleResolve(commitment)}>
                      Resuelto
                    </button>
                    <SimpleDeleteConfirm
                      resourceLabel="seguimiento"
                      itemName={commitment.description}
                      ariaLabel="Eliminar seguimiento"
                      onConfirm={() => handleDelete(commitment.id)}
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
