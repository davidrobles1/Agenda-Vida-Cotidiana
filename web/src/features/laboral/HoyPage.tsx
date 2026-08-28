import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconCheckCircle, IconPlus, IconRepeat, IconTarget } from '../../core/ui/icons'
import { listReminders, type Reminder } from '../reminders/api'
import { listCommitments, type Commitment } from '../commitments/api'
import { listPeople, type Person } from '../people/api'
import { listObjectives, type Objective } from '../objectives/api'
import { addInboxItem } from './inboxStorage'
import styles from './HoyPage.module.css'

function todayKey(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysUntil(iso: string): number {
  return Math.round((new Date(iso).getTime() - Date.now()) / 86400000)
}

/**
 * ADR-016/FR-026. Vista "Hoy" del contexto Laboral: tareas (REMINDER
 * context=LABORAL) que vencen hoy + Compromisos que requieren atención
 * (hoy o atrasados) + captura rápida hacia el Inbox. No agrega eventos
 * `PERSONAL` aquí (eso vive en "Agenda"/Calendario general, ya resuelto por
 * ADR-015) — FR-026 solo pide un resumen diario del contexto Laboral.
 */
export function HoyPage() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [objectives, setObjectives] = useState<Objective[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [quickCapture, setQuickCapture] = useState('')
  const [captured, setCaptured] = useState(false)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [remindersPage, commitmentsPage, peoplePage, objectivesPage] = await Promise.all([
        listReminders(),
        listCommitments(),
        listPeople(),
        listObjectives(),
      ])
      setTasks(remindersPage.items)
      setCommitments(commitmentsPage.items)
      setPeople(peoplePage.items)
      setObjectives(objectivesPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cargar tu día.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  const today = todayKey()
  const todaysTasks = tasks.filter((t) => t.context === 'LABORAL' && t.status === 'PENDING' && t.dueAt?.slice(0, 10) === today)
  const attentionCommitments = commitments
    .filter((c) => c.status === 'OPEN')
    .sort((a, b) => daysUntil(a.dueAt) - daysUntil(b.dueAt))
    .slice(0, 5)
  // FR-031/AC-018: un Objetivo cumplido deja de aparecer en Hoy.
  const openObjectives = objectives.filter((o) => !o.completed).slice(0, 4)

  function handleQuickCapture(event: FormEvent) {
    event.preventDefault()
    if (!quickCapture.trim()) return
    addInboxItem(quickCapture.trim())
    setQuickCapture('')
    setCaptured(true)
    setTimeout(() => setCaptured(false), 2000)
  }

  return (
    <AppShell title="Hoy" subtitle="Lo que necesitas saber para arrancar el día.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <div className={styles.grid}>
        <ListSectionCard title="Tareas de hoy">
          {loading && <p className={styles.emptyHint}>Cargando…</p>}
          {!loading && todaysTasks.length === 0 && <p className={styles.emptyHint}>Nada pendiente para hoy.</p>}
          {!loading &&
            todaysTasks.map((task) => (
              <ListItemRow key={task.id} title={task.title} icon={IconCheckCircle} tone="success" />
            ))}
        </ListSectionCard>

        <ListSectionCard title="Requiere tu atención">
          {loading && <p className={styles.emptyHint}>Cargando…</p>}
          {!loading && attentionCommitments.length === 0 && <p className={styles.emptyHint}>Nada pendiente por ahora.</p>}
          {!loading &&
            attentionCommitments.map((c) => {
              const person = people.find((p) => p.id === c.personId)
              const overdue = daysUntil(c.dueAt) < 0
              return (
                <ListItemRow
                  key={c.id}
                  title={`${person?.name ?? '—'} ${c.direction === 'MINE' ? '→' : '←'} ${c.description}`}
                  icon={IconRepeat}
                  tone={overdue ? 'error' : 'warning'}
                  pillLabel={overdue ? 'Atrasado' : 'Próximo'}
                  pillTone={overdue ? 'error' : 'warning'}
                />
              )
            })}
        </ListSectionCard>
      </div>

      {/* ADR-016 Fase 3e1/FR-031: resumen de Objetivos abiertos. "Ver todos"
          lleva a la página dedicada (/laboral/objectives), que no tiene
          entrada propia en el navbar — ver AppRouter.tsx. */}
      <ListSectionCard title="Objetivos" onSeeAll={() => navigate('/laboral/objectives')}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && openObjectives.length === 0 && (
          <p className={styles.emptyHint}>Sin objetivos abiertos.</p>
        )}
        {!loading &&
          openObjectives.map((objective) => (
            <ListItemRow
              key={objective.id}
              title={objective.title}
              subtitle={objective.deadline ? `Fecha límite: ${objective.deadline.slice(0, 10)}` : undefined}
              icon={IconTarget}
              tone="primary"
              pillLabel={objective.targetValue !== undefined ? `${objective.currentValue}/${objective.targetValue}` : undefined}
              pillTone="info"
            />
          ))}
      </ListSectionCard>

      <ListSectionCard title="Captura rápida">
        <form className={styles.captureForm} onSubmit={handleQuickCapture}>
          <input
            className={styles.captureInput}
            value={quickCapture}
            onChange={(e) => setQuickCapture(e.target.value)}
            placeholder="Anota algo antes de que se te olvide…"
          />
          <button type="submit" aria-label="Agregar al Inbox">
            <IconPlus width={16} height={16} />
          </button>
        </form>
        {captured && <p className={styles.captureHint}>Guardado en tu Inbox.</p>}
      </ListSectionCard>
    </AppShell>
  )
}
