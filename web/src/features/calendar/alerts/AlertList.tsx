import { Link } from 'react-router-dom'
import { AlertTriangle, CalendarClock, CreditCard, ShieldCheck, Wrench } from 'lucide-react'
import {
  SEVERITY_LABELS,
  SOURCE_LABELS,
  type AlertSource,
  type DateAlert,
} from './dateAlerts'
import styles from './AlertList.module.css'

/**
 * Lista de alertas de fecha (ADR-018).
 *
 * Restricción explícita del usuario, y la razón de que este componente sea
 * tan pequeño: "las alertas NO son tareas". Aquí no hay botón de completar,
 * ni de crear tarea, ni casilla de verificación, ni menú contextual — y no
 * debe añadirse ninguno. Lo único que ofrece es ir al registro de origen,
 * que es lo que el usuario sí puede hacer al respecto.
 */

const SOURCE_ICONS: Record<AlertSource, typeof ShieldCheck> = {
  warranty: ShieldCheck,
  maintenance: Wrench,
  subscription: CreditCard,
}

interface AlertListProps {
  alerts: DateAlert[]
  /** Muestra la fecha de la alerta; útil fuera de la vista diaria, donde el
      día no se deduce del contexto. */
  showDate?: boolean
  emptyLabel?: string
  /** Corta la lista; el resto se resume en el pie que decida quien llama. */
  limit?: number
}

function formatDate(dateKey: string): string {
  return new Date(`${dateKey}T12:00:00`).toLocaleDateString('es-MX', {
    day: 'numeric',
    month: 'short',
  })
}

export function AlertList({ alerts, showDate = false, emptyLabel, limit }: AlertListProps) {
  const visible = typeof limit === 'number' ? alerts.slice(0, limit) : alerts

  if (visible.length === 0) {
    return emptyLabel ? <p className={styles.empty}>{emptyLabel}</p> : null
  }

  return (
    <ul className={styles.list}>
      {visible.map((alert) => {
        const Icon = SOURCE_ICONS[alert.source] ?? CalendarClock

        return (
          <li key={alert.id} className={styles.item} data-severity={alert.severity}>
            <Link
              to={alert.href}
              className={styles.link}
              aria-label={`${SOURCE_LABELS[alert.source]}: ${alert.sourceLabel} — ${alert.message}. Alerta ${SEVERITY_LABELS[alert.severity].toLowerCase()}`}
            >
              <span className={styles.icon} aria-hidden="true">
                {alert.severity === 'high' ? (
                  <AlertTriangle width={15} height={15} />
                ) : (
                  <Icon width={15} height={15} />
                )}
              </span>

              <span className={styles.body}>
                <span className={styles.title}>{alert.sourceLabel}</span>
                <span className={styles.meta}>
                  {SOURCE_LABELS[alert.source]}
                  <span className={styles.metaSeparator}> · </span>
                  {alert.message}
                  {showDate && (
                    <>
                      <span className={styles.metaSeparator}> · </span>
                      {formatDate(alert.dateKey)}
                    </>
                  )}
                </span>
              </span>

              <span className={styles.badge}>{SEVERITY_LABELS[alert.severity]}</span>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
