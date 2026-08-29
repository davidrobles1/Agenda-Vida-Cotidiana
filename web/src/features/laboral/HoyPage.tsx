import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { IconBell, IconCheckCircle, IconPlus } from '../../core/ui/icons'
import { listReminders, type Reminder } from '../reminders/api'
import { listCommitments, type Commitment } from '../commitments/api'
import { listPeople, type Person } from '../people/api'
import { addInboxItem } from './inboxStorage'
import styles from './HoyPage.module.css'

function todayKey(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysUntil(iso: string): number {
  return Math.round((new Date(iso).getTime() - Date.now()) / 86400000)
}

/** Etiqueta relativa del artifact (`relativeLabel`), literal. */
function relativeLabel(iso: string): string {
  const days = daysUntil(iso)
  if (days === 0) return 'Hoy'
  if (days === 1) return 'Mañana'
  if (days === -1) return 'Ayer · 1 día de atraso'
  if (days < -1) return `Hace ${-days} días · atrasado`
  if (days > 1 && days <= 6) return `En ${days} días`
  return new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short' })
}

/** Badge del artifact (`commitmentBadge`), con los mismos cuatro estados. */
function commitmentBadge(commitment: Commitment): { label: string; tone: string } {
  if (commitment.status !== 'OPEN') return { label: 'Resuelto', tone: 'success' }
  if (daysUntil(commitment.dueAt) < 0) return { label: 'Atrasado', tone: 'error' }
  if (daysUntil(commitment.dueAt) <= 2) return { label: 'Próximo', tone: 'warning' }
  return { label: 'En curso', tone: 'info' }
}

function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

function timeOf(iso?: string): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })
}

interface TimelineItem {
  id: string
  time: string
  /** `lab` y `per` son las dos únicas clases del artifact: el filete azul
      del trabajo y el filete de acento de lo personal. */
  kind: 'lab' | 'per'
  label: string
  onOpen?: () => void
}

/**
 * ADR-016/FR-026. Vista "Hoy" del contexto Laboral.
 *
 * Rehecha (2026-08-28) para seguir el prototipo aprobado por el usuario
 * ("Agenda Laboral", artifact fca1566a): cabecera con antetítulo y título de
 * página, banner de compromisos atrasados, y una rejilla 2fr/1fr donde la
 * columna ancha es la línea de tiempo "Tu día" sobre papel rayado, y la
 * estrecha apila "Requiere tu atención" y "Captura rápida".
 *
 * Cambio de contenido respecto a la versión anterior, tomado del artifact:
 * "Tareas de hoy" era una lista suelta de tareas laborales; ahora es una
 * LÍNEA DE TIEMPO que mezcla lo laboral y lo personal del día — la premisa
 * del prototipo es "sin perder de vista lo personal", y por eso el filete
 * de color distingue el origen de cada elemento en vez de esconderlo.
 *
 * ALCANCE FINAL (2026-08-28, recorte del propio usuario sobre esta
 * pantalla): Hoy queda **exactamente** con las secciones del artifact —
 * cabecera, banner, y la rejilla "Tu día" / ["Requiere tu atención",
 * "Captura rápida"]. Se retiraron de aquí las tarjetas de Rutinas (FR-032)
 * y Objetivos (FR-031) y la de Alertas próximas (ADR-018); con ellas se
 * retiraron también sus peticiones, que ya no alimentaban nada. Ninguna
 * funcionalidad desapareció del producto: Rutinas y Objetivos conservan sus
 * páginas (`/laboral/routines`, `/laboral/objectives`) y las alertas de
 * fecha siguen en Inicio y en el Calendario.
 *
 * No se introduce ningún dato simulado: todo sale de `listReminders`,
 * `listCommitments` y `listPeople`.
 */
export function HoyPage() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [quickCapture, setQuickCapture] = useState('')
  const [captured, setCaptured] = useState(false)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [remindersPage, commitmentsPage, peoplePage] = await Promise.all([
        listReminders(),
        listCommitments(),
        listPeople(),
      ])
      setTasks(remindersPage.items)
      setCommitments(commitmentsPage.items)
      setPeople(peoplePage.items)
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
  const dueToday = tasks.filter((t) => t.status === 'PENDING' && t.dueAt?.slice(0, 10) === today)

  /** `combinedTimelineToday()` del artifact, con datos reales: lo laboral y
      lo personal del día en un solo hilo, ordenado por hora. */
  const timeline: TimelineItem[] = dueToday
    .map((task) => ({
      id: task.id,
      time: timeOf(task.dueAt),
      kind: (task.context === 'PERSONAL' ? 'per' : 'lab') as 'lab' | 'per',
      label: task.context === 'PERSONAL' ? `${task.title} (Personal)` : task.title,
      // "Tareas" se retiró de la navegación (2026-08-28): una tarea del día
      // se abre desde la Agenda, que es donde vive su fecha.
      onOpen: () => navigate('/laboral/calendar'),
    }))
    .concat(
      commitments
        .filter((c) => c.status === 'OPEN' && c.dueAt.slice(0, 10) === today)
        .map((c) => ({
          id: `commitment-${c.id}`,
          time: '—',
          kind: 'lab' as const,
          label: `${c.direction === 'MINE' ? 'Seguimiento' : 'Esperando'} · ${c.description}`,
          onOpen: () => navigate('/laboral/commitments'),
        })),
    )
    .sort((a, b) => (a.time === '—' ? '99:99' : a.time).localeCompare(b.time === '—' ? '99:99' : b.time))

  const attentionCommitments = commitments
    .filter((c) => c.status === 'OPEN')
    .sort((a, b) => daysUntil(a.dueAt) - daysUntil(b.dueAt))
    .slice(0, 5)

  /** Banner del artifact: lo atrasado que depende de OTRA persona, porque es
      lo único que no se resuelve trabajando más. */
  const waitingOverdue = commitments.filter(
    (c) => c.status === 'OPEN' && c.direction === 'THEIRS' && daysUntil(c.dueAt) < 0,
  )

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

      {/* Cabecera de página del artifact: antetítulo con la fecha, título y
          una línea que explica de qué va la pantalla. */}
      <header className={styles.pageHead}>
        <p className={styles.kicker}>
          {new Date().toLocaleDateString('es-MX', {
            weekday: 'long',
            day: 'numeric',
            month: 'long',
          })}
        </p>
        <h2 className={styles.pageTitle}>Hoy</h2>
        <p className={styles.pageLead}>
          Todo lo que necesitas saber para arrancar el día, sin perder de vista lo personal.
        </p>
      </header>

      {waitingOverdue.length > 0 && (
        <div className={styles.banner}>
          <IconBell width={18} height={18} aria-hidden="true" />
          <div>
            <b>
              {waitingOverdue.length} compromiso{waitingOverdue.length === 1 ? '' : 's'} atrasado
              {waitingOverdue.length === 1 ? '' : 's'}
            </b>{' '}
            depende{waitingOverdue.length === 1 ? '' : 'n'} de otras personas. Revisa "Esperando" para dar
            seguimiento.
          </div>
        </div>
      )}

      <div className={styles.grid2}>
        {/* ---------- Columna ancha: la línea de tiempo del día ---------- */}
        <section className={`${styles.card} ${styles.notebookBg}`}>
          <div className={styles.cardHeader}>
            <h3>Tu día</h3>
            <button
              type="button"
              className={styles.ghostButton}
              onClick={() => navigate('/laboral/calendar')}
            >
              Ver agenda completa
            </button>
          </div>

          {loading && <p className={styles.emptyHint}>Cargando…</p>}

          {!loading && timeline.length === 0 && (
            <div className={styles.emptyState}>
              <IconCheckCircle width={28} height={28} aria-hidden="true" />
              <p>Nada agendado para hoy.</p>
            </div>
          )}

          {!loading && timeline.length > 0 && (
            <div className={styles.timeline}>
              {timeline.map((item) => (
                <div key={item.id} className={styles.timelineItem}>
                  <div className={styles.timelineTime}>{item.time}</div>
                  <div className={styles.timelineBar} data-kind={item.kind} />
                  <div className={styles.timelineBody}>
                    {item.onOpen ? (
                      <button type="button" className={styles.timelineLink} onClick={item.onOpen}>
                        {item.label}
                      </button>
                    ) : (
                      <span>{item.label}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* ---------- Columna estrecha ---------- */}
        <div className={styles.sideColumn}>
          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <h3>Requiere tu atención</h3>
            </div>

            {loading && <p className={styles.emptyHint}>Cargando…</p>}

            {!loading && attentionCommitments.length === 0 && (
              <div className={styles.emptyState}>
                <IconCheckCircle width={28} height={28} aria-hidden="true" />
                <p>Nada pendiente por ahora.</p>
              </div>
            )}

            {!loading && attentionCommitments.length > 0 && (
              <div className={styles.list}>
                {attentionCommitments.map((commitment) => {
                  const person = people.find((p) => p.id === commitment.personId)
                  const badge = commitmentBadge(commitment)

                  return (
                    <button
                      key={commitment.id}
                      type="button"
                      className={styles.listItem}
                      onClick={() => navigate('/laboral/commitments')}
                    >
                      <span className={styles.avatar} aria-hidden="true">
                        {person ? initialsOf(person.name) : '?'}
                      </span>

                      <span className={styles.listItemBody}>
                        <span className={styles.listItemTitle}>{commitment.description}</span>
                        <span className={styles.listItemMeta}>
                          {commitment.direction === 'MINE' ? 'Tú → ' : '← '}
                          {person?.name ?? '—'} · {relativeLabel(commitment.dueAt)}
                        </span>
                      </span>

                      <span className={styles.badge} data-tone={badge.tone}>
                        {badge.label}
                      </span>
                    </button>
                  )
                })}
              </div>
            )}
          </section>

          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <h3>Captura rápida</h3>
            </div>

            <form className={styles.captureForm} onSubmit={handleQuickCapture}>
              <input
                className={styles.captureInput}
                value={quickCapture}
                onChange={(e) => setQuickCapture(e.target.value)}
                placeholder="Anota algo antes de que se te olvide…"
                aria-label="Anota algo antes de que se te olvide"
              />
              <button type="submit" className={styles.captureButton} aria-label="Agregar al Inbox">
                <IconPlus width={16} height={16} />
              </button>
            </form>

            <p className={styles.cardFootnote}>
              Va directo a tu Inbox — decides después si es tarea, seguimiento o nota.
            </p>

            {captured && <p className={styles.captureHint}>Guardado en tu Inbox.</p>}
          </section>
        </div>
      </div>
    </AppShell>
  )
}
