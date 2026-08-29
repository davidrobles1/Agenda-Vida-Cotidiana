import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { useActiveMode, useModePath } from '../../core/user/ActiveModeContext'
import { MetricCard } from '../../core/ui/components/MetricCard'
import { DonutChart } from '../../core/ui/components/DonutChart'
import { HomeIllustration } from '../../core/ui/components/HomeIllustration'
import { WeekAgendaWidget } from './WeekAgendaWidget'
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
import { AlertList } from '../calendar/alerts/AlertList'
import { useDateAlerts } from '../calendar/alerts/useDateAlerts'
import { groupAlertsByDay } from '../calendar/alerts/dateAlerts'
import styles from './HomePage.module.css'

const ACTIVITY_ICON: Record<MockActivityKind, typeof IconTasks> = {
  task: IconCheckCircle,
  share: IconShared,
  warranty: IconShield,
  document: IconFolder,
}

export function HomePage() {
  const navigate = useNavigate()
  const state = useHomeData()
  // ADR-015/UX-012: reused as-is by both Personal and Laboral navbars
  // (FR-018) — these resolve to the mode-scoped route when reached that way,
  // or the legacy bare one otherwise, so a click never drops the user out of
  // the mode they were just in. Documentos/Familia stay bare — outside
  // ADR-015's scope, no mode-scoped equivalent exists.
  const activeMode = useActiveMode()
  const calendarPath = useModePath('calendar')
  const sharedPath = useModePath('shared')
  // 2026-08-28: `RemindersPage` se retiró de la aplicación junto con el
  // botón "Nuevo" (ver AppShell.tsx/AppRouter.tsx). Los dos accesos que
  // apuntaban ahí pasan al Calendario, que es donde de verdad se crea y se
  // consulta una tarea — la capacidad no se pierde, cambia de puerta.
  // Ojo: dos accesos apuntan al Calendario desde que `RemindersPage` se
  // retiró, así que la clave de React NO puede ser `to` — sería la misma
  // para ambos y React descartaría uno de los dos <li> (defecto real: la
  // lista perdía un botón en silencio). Se usa `label`, que sí es único.
  const quickLinks = [
    { to: calendarPath, label: 'Crear nueva tarea', icon: IconPlus },
    { to: calendarPath, label: 'Ver calendario', icon: IconCalendar },
    { to: '/documents', label: 'Agregar documento', icon: IconFolder },
    { to: '/family', label: 'Invitar a un familiar', icon: IconUsers },
  ] as const

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

  // ADR-018: Personal aterriza aquí al entrar, así que es donde deben verse
  // las alertas de fecha próximas, priorizadas — sin volverse tareas.
  // ADR-019: Inicio vive tanto en /personal como en /laboral; las alertas se
  // acotan al módulo en el que está el usuario.
  const { alerts } = useDateAlerts(30, activeMode)
  // Misma agrupación por día que usa el Calendario, para que la semana de
  // Inicio y la del Calendario muestren lo mismo (petición 2.2).
  const alertsByDay = useMemo(() => groupAlertsByDay(alerts), [alerts])

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
              onClick={() => navigate(calendarPath)}
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
              onClick={() => navigate(sharedPath)}
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
              onClick={() => navigate(calendarPath)}
            />

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
            03.5 — SEMANA ACTUAL (pedido explícito del usuario,
            2026-08-22: entre "Tu actividad" y "Actividad reciente";
            solo la semana actual, sin navegación, no es la vista
            completa del calendario — ver WeekAgendaWidget.tsx)
            =====================================================
            =====================================================
            04 — ACTIVIDAD RECIENTE (mock) + ACCESOS RÁPIDOS (real)
            ===================================================== */}

        <section className={styles.quickRow}>
          {/* =====================================================
              01 — ACTIVIDAD RECIENTE
              ===================================================== */}
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

                      <span className={styles.activityTime}>
                        {activity.timeLabel} · simulado
                      </span>
                    </span>
                  </li>
                )
              })}
            </ul>
          </div>

          {/* =====================================================
              02 — TUS PENDIENTES
              Este panel ocupa las dos filas de la columna derecha.
              ===================================================== */}
          <div className={styles.quickPanel}>
            <div className={styles.sectionHeading}>
              <div>
                <span className={styles.eyebrow}>ESTA SEMANA</span>
                <h2>Tus pendientes</h2>
              </div>
            </div>

            <WeekAgendaWidget thisWeek={state.thisWeek} alertsByDay={alertsByDay} />
          </div>

          {/* =====================================================
              03 — ACCESOS RÁPIDOS
              Queda debajo de Actividad reciente.
              ===================================================== */}
          <div className={styles.quickPanel}>
            <div className={styles.sectionHeading}>
              <div>
                <span className={styles.eyebrow}>ATAJOS</span>
                <h2>Accesos rápidos</h2>
              </div>
            </div>

            <ul className={styles.quickList}>
              {quickLinks.map((link) => (
                <li key={link.label}>
                  <button
                    type="button"
                    className={styles.quickLink}
                    onClick={() => navigate(link.to)}
                  >
                    <span className={styles.quickLinkIcon}>
                      <link.icon width={16} height={16} />
                    </span>

                    <span className={styles.quickLinkLabel}>
                      {link.label}
                    </span>

                    <IconChevronRight
                      className={styles.quickChevron}
                      width={16}
                      height={16}
                      aria-hidden="true"
                    />
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </section>

        
                {/* =====================================================
            02 — ALERTAS PRÓXIMAS (ADR-018)

            Seguimiento de fechas de Garantías, Mantenimiento y
            Suscripciones. NO son tareas: no se completan, no se crean y no
            aparecen en la lista de pendientes — solo avisan y llevan a su
            módulo. Se ordenan por importancia (alta primero).
            ===================================================== */}

        {alerts.length > 0 && (
          <section className={styles.alertsSection}>
            <div className={styles.sectionHeading}>
              <div>
                <span className={styles.eyebrow}>PRÓXIMAS</span>
                <h2>Alertas</h2>
              </div>
              <span className={styles.alertsCount}>
                {alerts.length} {alerts.length === 1 ? 'alerta' : 'alertas'} en 30 días
              </span>
            </div>

            <AlertList alerts={alerts} showDate limit={6} />

            {alerts.length > 6 && (
              <p className={styles.alertsFootnote}>y {alerts.length - 6} más.</p>
            )}
          </section>
        )}
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