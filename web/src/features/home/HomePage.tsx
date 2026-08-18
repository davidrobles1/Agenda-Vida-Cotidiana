import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { MetricCard } from '../../core/ui/components/MetricCard'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { DonutChart } from '../../core/ui/components/DonutChart'
import { HomeIllustration } from '../../core/ui/components/HomeIllustration'
import {
  IconAlert,
  IconCalendar,
  IconCheckCircle,
  IconChevronRight,
  IconFolder,
  IconPlus,
  IconShared,
  IconShield,
  IconTasks,
  IconUsers,
} from '../../core/ui/icons'
import { homeActivities, homeUpcomingEventsCount, type MockActivityKind } from '../../core/mock/mockData'
import { useHomeData } from './useHomeData'
import styles from './HomePage.module.css'

const ACTIVITY_ICON: Record<MockActivityKind, typeof IconTasks> = {
  task: IconCheckCircle,
  share: IconShared,
  warranty: IconShield,
  document: IconFolder,
}

const QUICK_LINKS = [
  { to: '/reminders', label: 'Crear nueva tarea', icon: IconPlus },
  { to: '/calendar', label: 'Ver calendario', icon: IconCalendar },
  { to: '/documents', label: 'Agregar documento', icon: IconFolder },
  { to: '/family', label: 'Invitar a un familiar', icon: IconUsers },
] as const

function formatDueAt(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function HomePage() {
  const navigate = useNavigate()
  const state = useHomeData()

  const hasActivity =
    state.overdue.length > 0 || state.upcoming.length > 0

  const totalTasks = state.pendingCount + state.completedCount
  // Real, not mocked: drives which illustration renders (sun vs. moon) —
  // the same signal a "Buenos días/tardes/noches" greeting would use, so
  // the two can never disagree with each other.
  const isNight = (() => {
    const hour = new Date().getHours()
    return hour < 6 || hour >= 19
  })()

  return (
    <AppShell
      title={
        state.greetingName
          ? `¡Hola, ${state.greetingName}!`
          : 'Inicio'
      }
      subtitle="Tu agenda de hoy."
    >
      <main className={styles.page}>

        {/* =====================================================
            01 — RESUMEN DEL DÍA
            ===================================================== */}

        <section className={styles.daySummary}>
          <div className={styles.daySummaryContent}>
            <div>
              <span className={styles.eyebrow}>
                TU DÍA
              </span>

              <h2>
                {hasActivity
                  ? 'Tienes algunas cosas por atender.'
                  : 'Tu día está en calma.'}
              </h2>

              <p>
                {hasActivity
                  ? 'Aquí tienes lo más importante para mantener tu día organizado.'
                  : 'Disfruta el momento. Tu agenda no tiene pendientes por ahora.'}
              </p>
            </div>

            <div className={styles.dayIllustration}>
              <HomeIllustration isNight={isNight} />
            </div>
          </div>
        </section>

        {/* =====================================================
            ERROR REAL
            ===================================================== */}

        {state.error && (
          <div className={styles.errorMessage} role="alert">
            <IconAlert width={20} height={20} />

            <span>{state.error}</span>
          </div>
        )}

        {/* =====================================================
            02 — ACTIVIDAD
            ===================================================== */}

        <section className={styles.activitySection}>

          <div className={styles.sectionHeading}>
            <div>
              <span className={styles.eyebrow}>
                AGENDA
              </span>

              <h2>Lo que tienes pendiente</h2>
            </div>

            <button
              type="button"
              className={styles.textButton}
              onClick={() => navigate('/reminders')}
            >
              Ver agenda
            </button>
          </div>

          {state.loading && (
            <div className={styles.emptyState}>
              <div className={styles.emptyIcon}>
                <IconTasks width={22} height={22} />
              </div>

              <div>
                <strong>Cargando tu agenda...</strong>
                <p>Estamos preparando tu día.</p>
              </div>
            </div>
          )}

          {!state.loading && state.overdue.length > 0 && (
            <ListSectionCard
              title="Necesita tu atención"
              onSeeAll={() => navigate('/reminders')}
            >
              {state.overdue.map((reminder) => (
                <ListItemRow
                  key={reminder.id}
                  title={reminder.title}
                  subtitle={
                    reminder.dueAt
                      ? formatDueAt(reminder.dueAt)
                      : ''
                  }
                  icon={IconAlert}
                  tone="error"
                  pillLabel="Vencida"
                  pillTone="error"
                />
              ))}
            </ListSectionCard>
          )}

          {!state.loading && state.upcoming.length > 0 && (
            <ListSectionCard
              title="Próximos días"
              onSeeAll={() => navigate('/reminders')}
            >
              {state.upcoming.map((reminder) => (
                <ListItemRow
                  key={reminder.id}
                  title={reminder.title}
                  subtitle={
                    reminder.dueAt
                      ? formatDueAt(reminder.dueAt)
                      : ''
                  }
                  icon={IconTasks}
                  tone="info"
                  pillLabel="Pendiente"
                  pillTone="warning"
                />
              ))}
            </ListSectionCard>
          )}

          {!state.loading && !hasActivity && (
            <div className={styles.emptyState}>
              <div className={styles.emptyIcon}>
                <IconCheckCircle width={22} height={22} />
              </div>

              <div>
                <strong>Todo en orden</strong>
                <p>
                  No tienes tareas pendientes por ahora.
                </p>
              </div>
            </div>
          )}

        </section>

        {/* =====================================================
            03 — RESUMEN PERSONAL
            ===================================================== */}

        <section className={styles.overviewSection}>

          <div className={styles.sectionHeading}>
            <div>
              <span className={styles.eyebrow}>
                RESUMEN
              </span>

              <h2>Tu actividad</h2>
            </div>
          </div>

          <div className={styles.metricsRow}>

            <MetricCard
              testId="metric_tasks"
              label="Tareas"
              value={state.pendingCount}
              subtitle={
                state.overdueCount > 0
                  ? `${state.overdueCount} vencidas`
                  : 'Todo al día'
              }
              subtitleTone={
                state.overdueCount > 0
                  ? 'error'
                  : undefined
              }
              icon={IconTasks}
              tone="primary"
              onClick={() => navigate('/reminders')}
            />

            <MetricCard
              testId="metric_shared"
              label="Compartidos"
              value={state.sharedCount}
              subtitle={
                state.sharedCount > 0
                  ? 'invitaciones pendientes'
                  : 'Todo al día'
              }
              icon={IconShared}
              tone="info"
              onClick={() => navigate('/invitations')}
            />

            {/* UX-009: mock — "evento" no es un concepto real de la API
                todavía (los recordatorios son "tareas"). Valor fijo,
                etiquetado explícitamente, no reinterpretado en silencio. */}
            <MetricCard
              testId="metric_events"
              label="Próximos eventos"
              value={homeUpcomingEventsCount}
              subtitle="Esta semana · simulado"
              subtitleTone="warning"
              icon={IconCalendar}
              tone="warning"
              onClick={() => navigate('/calendar')}
            />

            <div className={styles.progressCard}>

              <div className={styles.progressHeader}>
                <div>
                  <span className={styles.cardLabel}>
                    Progreso
                  </span>

                  <strong>
                    {state.completedCount}
                    <small>
                      {totalTasks > 0
                        ? ` de ${totalTasks}`
                        : ''}
                    </small>
                  </strong>
                </div>

                <span className={styles.progressCaption}>
                  {totalTasks > 0
                    ? `${Math.round(
                        (state.completedCount / totalTasks) * 100,
                      )}%`
                    : '—'}
                </span>
              </div>

              {totalTasks > 0 ? (
                <div className={styles.progressContent}>
                  <DonutChart
                    slices={[
                      {
                        value: state.completedCount,
                        color: 'var(--color-success)',
                      },
                      {
                        value: state.pendingCount,
                        color: 'var(--color-warning)',
                      },
                    ]}
                  />

                  <div className={styles.legend}>
                    <LegendRow
                      label="Completadas"
                      count={state.completedCount}
                      colorVar="--color-success"
                    />

                    <LegendRow
                      label="Pendientes"
                      count={state.pendingCount}
                      colorVar="--color-warning"
                    />
                  </div>
                </div>
              ) : (
                <div className={styles.noProgress}>
                  <IconTasks width={20} height={20} />
                  <span>
                    Aún no tienes actividad registrada.
                  </span>
                </div>
              )}

            </div>

          </div>
        </section>

        {/* =====================================================
            04 — ACTIVIDAD RECIENTE (mock) + ACCESOS RÁPIDOS (real)
            ===================================================== */}

        <section className={styles.quickRow}>
          <div className={styles.quickPanel}>
            <div className={styles.sectionHeading}>
              <div>
                <span className={styles.eyebrow}>RESUMEN</span>
                <h2>Actividad reciente</h2>
              </div>
            </div>
            <ul className={styles.activityList}>
              {homeActivities.map((activity) => {
                const Icon = ACTIVITY_ICON[activity.kind]
                return (
                  <li key={activity.id} className={styles.activityItem}>
                    <span className={styles.activityIcon}>
                      <Icon width={16} height={16} />
                    </span>
                    <span className={styles.activityText}>
                      <span>{activity.text}</span>
                      <span className={styles.activityTime}>{activity.timeLabel} · simulado</span>
                    </span>
                  </li>
                )
              })}
            </ul>
          </div>

          <div className={styles.quickPanel}>
            <div className={styles.sectionHeading}>
              <div>
                <span className={styles.eyebrow}>ATAJOS</span>
                <h2>Accesos rápidos</h2>
              </div>
            </div>
            <ul className={styles.quickList}>
              {QUICK_LINKS.map((link) => (
                <li key={link.to}>
                  <button type="button" className={styles.quickLink} onClick={() => navigate(link.to)}>
                    <span className={styles.quickLinkIcon}>
                      <link.icon width={16} height={16} />
                    </span>
                    <span className={styles.quickLinkLabel}>{link.label}</span>
                    <IconChevronRight className={styles.quickChevron} width={16} height={16} aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </section>

      </main>
    </AppShell>
  )
}

function LegendRow({
  label,
  count,
  colorVar,
}: {
  label: string
  count: number
  colorVar: string
}) {
  return (
    <div className={styles.legendRow}>
      <span
        className={styles.legendDot}
        style={{
          background: `var(${colorVar})`,
        }}
      />

      <span>
        {label}
        <strong>{count}</strong>
      </span>
    </div>
  )
}