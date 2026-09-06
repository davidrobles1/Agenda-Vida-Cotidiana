import { useEffect, useState } from 'react'
import { listPaymentRecordsFor, type PaymentRecord } from './api'
import { formatAmount } from './paymentsView'
import styles from './SubscriptionDialog.module.css'

/**
 * Historial de pagos de un compromiso.
 *
 * CARENCIA CORREGIDA (2026-08-29): los registros de pago se guardaban desde
 * que existe `payment_records`, pero ninguna pantalla los mostraba. El
 * usuario no podía responder "¿ya pagué esto el mes pasado?", que es
 * justamente lo que un historial resuelve.
 *
 * Vive dentro del diálogo de edición y NO en una pantalla nueva: ese
 * diálogo ya es la superficie de detalle del registro, y añadir una ruta
 * solo para leer cinco fechas sería navegación de más.
 *
 * Se carga al abrir y no antes: en una lista de veinte pagos, traer el
 * historial de todos sería veinte consultas para mostrar ninguna.
 */
export function PaymentHistory({ subscriptionId }: { subscriptionId: string }) {
  const [records, setRecords] = useState<PaymentRecord[] | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let cancelled = false
    listPaymentRecordsFor(subscriptionId)
      .then((data) => {
        if (!cancelled) setRecords(data)
      })
      .catch(() => {
        // El historial es contexto, no el contenido del diálogo: si falla,
        // se dice y el formulario sigue siendo usable.
        if (!cancelled) setFailed(true)
      })
    return () => {
      cancelled = true
    }
  }, [subscriptionId])

  if (failed) {
    return <p className={styles.historyEmpty}>No se pudo cargar el historial.</p>
  }

  if (records === null) {
    return <p className={styles.historyEmpty}>Cargando historial…</p>
  }

  if (records.length === 0) {
    return <p className={styles.historyEmpty}>Todavía no has marcado ningún pago de este compromiso.</p>
  }

  return (
    <div className={styles.history}>
      <span className={styles.historyTitle}>Pagos registrados</span>
      <ul className={styles.historyList}>
        {records.slice(0, 6).map((record) => (
          <li key={record.id} className={styles.historyRow}>
            <span>
              {new Date(`${record.periodDate}T12:00:00`).toLocaleDateString('es-MX', {
                day: 'numeric',
                month: 'long',
                year: 'numeric',
              })}
            </span>
            <span className={styles.historyAmount}>
              {record.amount != null ? formatAmount(record.amount, record.currency) : 'Sin importe'}
            </span>
          </li>
        ))}
      </ul>
      {records.length > 6 && (
        <p className={styles.historyEmpty}>y {records.length - 6} pagos anteriores.</p>
      )}
    </div>
  )
}
