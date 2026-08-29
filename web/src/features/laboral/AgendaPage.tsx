import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { listReminders, type Reminder } from '../reminders/api'
import { listCommitments, type Commitment } from '../commitments/api'
import styles from './AgendaPage.module.css'

/** `fmtDay` del artifact, literal. */
function fmtDay(date: Date): string {
  return date.toLocaleDateString('es-MX', { weekday: 'long', day: 'numeric', month: 'short' })
}

/** `isSameDay` del artifact. */
function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  )
}

function dayKey(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

/** `--:--` es el marcador del artifact para lo que no tiene hora concreta. */
const NO_TIME = '--:--'

function timeOf(iso?: string): string {
  if (!iso) return NO_TIME
  return new Date(iso).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })
}

interface AgendaItem {
  id: string
  time: string
  /** `lab` y `per` son las dos únicas clases del artifact. */
  kind: 'lab' | 'per'
  label: string
  onOpen?: () => void
}

/** Los seis días del artifact: hoy y los cinco siguientes. */
const DAY_COUNT = 6

/**
 * ADR-016. "Agenda" del contexto Laboral (`/laboral/calendar`).
 *
 * Trasladada del prototipo aprobado por el usuario ("Agenda Laboral",
 * artifact fca1566a, `pageAgenda()`): antetítulo "Vista semanal", título, y
 * una sola tarjeta de papel rayado que apila seis grupos de día —cada uno
 * con su encabezado y su línea de tiempo— separados por un filete.
 *
 * **Por qué es una pantalla propia y no un cambio en `CalendarPage`:** esa
 * página la comparten Personal (`/personal/calendar`) y el Calendario
 * general (`/calendar`), y el propio artifact las modela como pantallas
 * distintas — "Agenda" es la vista semanal de Laboral, mientras que
 * "Calendario general" es otra vista y Personal aparece explícitamente
 * como "sin cambios". Rehacer `CalendarPage` con este diseño habría
 * cambiado los tres a la vez. `CalendarPage` queda intacta; solo cambia a
 * dónde apunta la ruta de Laboral.
 *
 * **Datos: todos reales, ninguno simulado.** El artifact inventa dos
 * entradas personales fijas ("Bloque de trabajo profundo", "Cita personal")
 * para poblar su maqueta; aquí NO se replican. Lo que se muestra sale de
 * `listReminders` y `listCommitments`:
 * - una "reunión" no es una entidad propia (ADR-016(b)): es un REMINDER con
 *   `location`, y por eso su ubicación se muestra junto al título;
 * - el filete distingue el origen real de cada elemento por
 *   `REMINDER.context`, igual que ya hace "Hoy".
 *
 * **Elementos sin destino:** el artifact abre detalles
 * (`/laboral/eventos/:id`, `/laboral/tareas/:id`) que no existen en la
 * aplicación — "Tareas" se retiró de la navegación y nunca hubo pantalla de
 * evento. No se inventa ninguna: esos elementos se renderizan como texto no
 * interactivo, que es justo lo que el propio artifact hace cuando un item
 * no tiene `onClick`. Los compromisos sí navegan, porque
 * `/laboral/commitments` existe de verdad.
 */
export function AgendaPage() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [remindersPage, commitmentsPage] = await Promise.all([listReminders(), listCommitments()])
        if (cancelled) return
        setTasks(remindersPage.items)
        setCommitments(commitmentsPage.items)
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'No se pudo cargar tu agenda.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [])

  const today = useMemo(() => new Date(), [])

  const days = useMemo(() => {
    return Array.from({ length: DAY_COUNT }, (_, offset) => {
      const date = new Date(today)
      date.setDate(date.getDate() + offset)
      return date
    })
  }, [today])

  /** `itemsFor(dt)` del artifact, con datos reales. */
  const itemsByDay = useMemo(() => {
    const byDay = new Map<string, AgendaItem[]>()

    for (const date of days) {
      const key = dayKey(date)
      const items: AgendaItem[] = []

      for (const task of tasks) {
        if (task.status !== 'PENDING' || task.dueAt?.slice(0, 10) !== key) continue
        items.push({
          id: task.id,
          time: timeOf(task.dueAt),
          kind: task.context === 'PERSONAL' ? 'per' : 'lab',
          // Una reunión es un REMINDER con `location` (ADR-016(b)): cuando la
          // tiene, es el dato que dice a dónde hay que ir.
          label: [task.title, task.location].filter(Boolean).join(' · '),
        })
      }

      for (const commitment of commitments) {
        if (commitment.status !== 'OPEN' || commitment.dueAt.slice(0, 10) !== key) continue
        items.push({
          id: `commitment-${commitment.id}`,
          time: NO_TIME,
          kind: 'lab',
          label: `${commitment.direction === 'MINE' ? 'Seguimiento' : 'Esperando'} · ${commitment.description}`,
          onOpen: () => navigate('/laboral/commitments'),
        })
      }

      items.sort((a, b) => a.time.localeCompare(b.time))
      byDay.set(key, items)
    }

    return byDay
  }, [days, tasks, commitments, navigate])

  return (
    <AppShell title="Agenda" subtitle="Semana actual">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <header className={styles.pageHead}>
        <p className={styles.kicker}>Vista semanal</p>
        <h2 className={styles.pageTitle}>Agenda</h2>
        <p className={styles.pageLead}>
          Laboral y Personal comparten el mismo reloj: aquí conviven sin mezclarse.
        </p>
      </header>

      <section className={`${styles.card} ${styles.notebookBg}`}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}

        {!loading &&
          days.map((date, index) => {
            const key = dayKey(date)
            const items = itemsByDay.get(key) ?? []
            const isToday = isSameDay(date, today)

            return (
              <div key={key}>
                {index > 0 && <hr className={styles.divider} />}

                <div className={styles.dayGroup}>
                  <h3 className={`${styles.dayHeading} ${isToday ? styles.dayHeadingToday : ''}`}>
                    {isToday ? 'Hoy · ' : ''}
                    {fmtDay(date)}
                  </h3>

                  {items.length === 0 ? (
                    <p className={styles.dayEmpty}>Sin elementos.</p>
                  ) : (
                    <div className={styles.timeline}>
                      {items.map((item) => (
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
                </div>
              </div>
            )
          })}
      </section>

      <div className={styles.legend}>
        <span className={styles.legendItem}>
          <span className={styles.legendDot} data-kind="lab" aria-hidden="true" /> Laboral
        </span>
        <span className={styles.legendItem}>
          <span className={styles.legendDot} data-kind="per" aria-hidden="true" /> Personal
        </span>
      </div>
    </AppShell>
  )
}
