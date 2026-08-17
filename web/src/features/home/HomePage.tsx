import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { MetricCard } from '../../core/ui/components/MetricCard'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { DonutChart } from '../../core/ui/components/DonutChart'
import { IconAlert, IconShared, IconTasks } from '../../core/ui/icons'
import { useHomeData } from './useHomeData'
import styles from './HomePage.module.css'

function formatDueAt(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

/**
 * UX-006: Home dashboard — matches the reference's layout (metric cards +
 * "Próximas tareas"/"Recordatorios importantes" + a status donut), but only
 * with real data (useHomeData). No Documentos/Garantías/Gastos metric
 * cards — those are mock modules or excluded entirely (Finanzas).
 */
export function HomePage() {
  const navigate = useNavigate()
  const state = useHomeData()

  return (
    <AppShell title={state.greetingName ? `¡Hola, ${state.greetingName}!` : 'Inicio'} subtitle="Aquí tienes un resumen de tu día.">
      {state.loading && <p className={styles.loading}>Loading…</p>}
      {state.error && <p role="alert">{state.error}</p>}

      <div className={styles.metricsRow}>
        <MetricCard
          testId="metric_tasks"
          label="Tareas"
          value={state.pendingCount}
          subtitle={state.overdueCount > 0 ? `${state.overdueCount} vencidas` : 'al día'}
          subtitleTone={state.overdueCount > 0 ? 'error' : undefined}
          icon={IconTasks}
          tone="primary"
          onClick={() => navigate('/reminders')}
        />
        <MetricCard
          testId="metric_shared"
          label="Compartidos"
          value={state.sharedCount}
          subtitle="invitaciones pendientes"
          icon={IconShared}
          tone="info"
          onClick={() => navigate('/invitations')}
        />
      </div>

      {state.upcoming.length > 0 && (
        <ListSectionCard title="Próximas tareas" onSeeAll={() => navigate('/reminders')}>
          {state.upcoming.map((reminder) => (
            <ListItemRow
              key={reminder.id}
              title={reminder.title}
              subtitle={reminder.dueAt ? formatDueAt(reminder.dueAt) : ''}
              icon={IconTasks}
              tone="info"
              pillLabel="Pendiente"
              pillTone="warning"
            />
          ))}
        </ListSectionCard>
      )}

      {state.overdue.length > 0 && (
        <ListSectionCard title="Recordatorios importantes" onSeeAll={() => navigate('/reminders')}>
          {state.overdue.map((reminder) => (
            <ListItemRow
              key={reminder.id}
              title={reminder.title}
              subtitle={reminder.dueAt ? formatDueAt(reminder.dueAt) : ''}
              icon={IconAlert}
              tone="error"
              pillLabel="Vencida"
              pillTone="error"
            />
          ))}
        </ListSectionCard>
      )}

      {state.pendingCount + state.completedCount > 0 && (
        <ListSectionCard title="Progreso de tareas">
          <div className={styles.progressRow}>
            <DonutChart
              slices={[
                { value: state.completedCount, color: 'var(--color-success)' },
                { value: state.pendingCount, color: 'var(--color-warning)' },
              ]}
            />
            <div className={styles.legend}>
              <LegendRow label="Completadas" count={state.completedCount} colorVar="--color-success" />
              <LegendRow label="Pendientes" count={state.pendingCount} colorVar="--color-warning" />
            </div>
          </div>
        </ListSectionCard>
      )}
    </AppShell>
  )
}

function LegendRow({ label, count, colorVar }: { label: string; count: number; colorVar: string }) {
  return (
    <div className={styles.legendRow}>
      <span className={styles.legendDot} style={{ background: `var(${colorVar})` }} />
      <span>
        {label}: {count}
      </span>
    </div>
  )
}
