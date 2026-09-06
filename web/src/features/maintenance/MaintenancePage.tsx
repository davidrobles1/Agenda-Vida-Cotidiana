import { useCallback, useEffect, useMemo, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import {
  completeOccurrence,
  deleteMaintenanceRecord,
  listMaintenanceLog,
  listMaintenanceRecords,
  undoLastOccurrence,
  type MaintenanceLogEntry,
  type MaintenanceRecord,
} from './api'
import {
  STATE_MARKS,
  everyLabel,
  formatDate,
  groupRecords,
  maintenanceState,
  summarize,
  whenLabel,
  type MaintenanceState,
} from './maintenanceView'
import { CreateMaintenanceDialog } from './CreateMaintenanceDialog'
import { MaintenanceDetailDialog } from './MaintenanceDetailDialog'
import styles from './MaintenancePage.module.css'

/**
 * ADR-021: Mantenimiento con recurrencia real.
 *
 * Implementa el prototipo aprobado (artifact 9b1318d8): resumen de tres
 * datos, filtros, lista agrupada por cercanía y filas que muestran la
 * periodicidad y la última vez que se hizo.
 *
 * El cambio de fondo no es visual: completar un mantenimiento recurrente
 * ahora AVANZA a su siguiente fecha en vez de darlo por terminado para
 * siempre, y queda registrado en el historial. Antes, `intervalMonths` se
 * guardaba y no servía para nada.
 */

type Filter = 'all' | 'pending' | 'done' | 'recurring'

const FILTERS: Array<{ id: Filter; label: string }> = [
  { id: 'all', label: 'Todos' },
  { id: 'pending', label: 'Pendientes' },
  { id: 'done', label: 'Hechos' },
  { id: 'recurring', label: 'Recurrentes' },
]

export function MaintenancePage() {
  // ADR-019: el recurso nace en el módulo desde el que se crea.
  const activeMode = useActiveMode()
  const [records, setRecords] = useState<MaintenanceRecord[]>([])
  const [log, setLog] = useState<MaintenanceLogEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<Filter>('all')
  const [busyId, setBusyId] = useState<string | null>(null)
  /** Confirmación de lo que acaba de pasar — en el prototipo es la pieza
      que explica el avance de fecha, que si no sería invisible. */
  const [toast, setToast] = useState<{ kind: 'done' | 'undo'; text: string } | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [page, entries] = await Promise.all([
        listMaintenanceRecords(activeMode),
        listMaintenanceLog(),
      ])
      setRecords(page.items)
      setLog(entries)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los mantenimientos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeMode])

  const logByRecord = useMemo(() => {
    const map = new Map<string, MaintenanceLogEntry[]>()
    for (const entry of log) {
      const list = map.get(entry.maintenanceRecordId) ?? []
      list.push(entry)
      map.set(entry.maintenanceRecordId, list)
    }
    for (const list of map.values()) {
      list.sort((a, b) => b.completedDate.localeCompare(a.completedDate))
    }
    return map
  }, [log])

  /** Estable respecto a `logByRecord`, que es de lo único que depende: así
      los memos que la usan declaran su dependencia real. */
  const stateOf = useCallback(
    (record: MaintenanceRecord): MaintenanceState =>
      maintenanceState(record, logByRecord.get(record.id) ?? []),
    [logByRecord],
  )

  const summary = useMemo(() => summarize(records, stateOf), [records, stateOf])

  const filtered = useMemo(() => {
    if (filter === 'recurring') return records.filter((record) => !!record.intervalMonths)
    if (filter === 'pending') {
      return records.filter((record) => ['ok', 'soon', 'overdue'].includes(stateOf(record)))
    }
    if (filter === 'done') {
      return records.filter((record) => ['done', 'closed'].includes(stateOf(record)))
    }
    return records
  }, [records, filter, stateOf])

  const groups = useMemo(() => groupRecords(filtered, stateOf), [filtered, stateOf])

  async function handleComplete(record: MaintenanceRecord) {
    setBusyId(record.id)
    setError(null)
    try {
      const updated = await completeOccurrence(record.id)
      setRecords((current) => current.map((r) => (r.id === updated.id ? updated : r)))
      setLog(await listMaintenanceLog())
      setToast({
        kind: 'done',
        text: updated.intervalMonths
          ? `“${updated.item}” hecho. El siguiente toca el ${formatDate(updated.nextDueAt)}.`
          : `“${updated.item}” cerrado. No tenía periodicidad, así que no vuelve.`,
      })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo marcar el mantenimiento.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleUndo(record: MaintenanceRecord) {
    setBusyId(record.id)
    setError(null)
    try {
      const updated = await undoLastOccurrence(record.id)
      setRecords((current) => current.map((r) => (r.id === updated.id ? updated : r)))
      setLog(await listMaintenanceLog())
      setToast({
        kind: 'undo',
        text: `Se deshizo. “${updated.item}” vuelve a estar pendiente para el ${formatDate(updated.nextDueAt)}.`,
      })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo deshacer.')
    } finally {
      setBusyId(null)
    }
  }

  function handleSaved(saved: MaintenanceRecord) {
    setRecords((current) => {
      const exists = current.some((record) => record.id === saved.id)
      return exists ? current.map((r) => (r.id === saved.id ? saved : r)) : [saved, ...current]
    })
    setToast(null)
  }

  async function handleDelete(id: string) {
    await deleteMaintenanceRecord(id)
    setRecords((current) => current.filter((record) => record.id !== id))
  }

  const detailRecord = records.find((record) => record.id === detailId)

  return (
    <AppShell title="Mantenimiento" subtitle="Lo que toca revisar, y cuándo vuelve."
      actions={<><CreateMaintenanceDialog onCreated={handleSaved} /></>}
    >
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      {!loading && records.length > 0 && (
        <>
          <section className={styles.summary}>
            <div className={`${styles.summaryCard} ${styles.summaryLead}`}>
              <span className={styles.summaryLabel}>Lo siguiente</span>
              <p className={styles.summaryValue}>{summary.next ? summary.next.item : 'Nada pendiente'}</p>
              <p className={styles.summaryMeta}>
                {(() => {
                  if (!summary.next) return 'Todo al día'
                  const date = formatDate(summary.next.nextDueAt)
                  const when = whenLabel(summary.next, stateOf(summary.next))
                  // Lejos en el tiempo, `whenLabel` ya ES la fecha absoluta.
                  return when === date ? date : `${date} · ${when}`
                })()}
              </p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Pendientes</span>
              <p className={styles.summaryValue}>{summary.pendingCount}</p>
              <p className={styles.summaryMeta}>
                {summary.overdueCount > 0 ? `${summary.overdueCount} vencido(s)` : 'Ninguno vencido'}
              </p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Recurrentes</span>
              <p className={styles.summaryValue}>{summary.recurringCount}</p>
              <p className={styles.summaryMeta}>de {summary.totalCount} registros</p>
            </div>
          </section>

          <div className={styles.filters} role="group" aria-label="Filtrar mantenimientos">
            {FILTERS.map((option) => (
              <button
                key={option.id}
                type="button"
                className={styles.chip}
                data-on={filter === option.id ? 'true' : undefined}
                aria-pressed={filter === option.id}
                onClick={() => setFilter(option.id)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </>
      )}

      {loading && <p className={styles.hint}>Cargando…</p>}

      {/* `!error` es imprescindible: si la carga falla, `records` queda
          vacío y el vacío afirmaba "Aún no registras ningún mantenimiento"
          teniendo el usuario registros. Cuando hay error manda el aviso de
          error, que es lo cierto. */}
      {!loading && !error && records.length === 0 && (
        <div className={styles.empty}>
          <h3>Aún no registras ningún mantenimiento</h3>
          <p>Agrega el primero y sabrás cuándo toca revisarlo — y cuándo vuelve a tocar.</p>
          <div className={styles.emptyAction}>
            <CreateMaintenanceDialog onCreated={handleSaved} />
          </div>
        </div>
      )}

      {!loading &&
        groups.map((group) => (
          <section key={group.id} className={styles.group}>
            <h3 className={styles.groupLabel}>{group.label}</h3>

            <div className={styles.rows}>
              {group.items.map((record) => {
                const state = stateOf(record)
                const entries = logByRecord.get(record.id) ?? []
                const last = entries[0]

                return (
                  <article key={record.id} className={styles.row} data-state={state}>
                    <span className={styles.rowMark} aria-hidden="true">
                      {STATE_MARKS[state]}
                    </span>

                    <div className={styles.rowBody}>
                      <p className={styles.rowName}>
                        {record.item}
                        {state === 'overdue' && (
                          <span className={styles.tag} data-t="overdue">
                            Vencido
                          </span>
                        )}
                        {state === 'soon' && (
                          <span className={styles.tag} data-t="soon">
                            Próximo
                          </span>
                        )}
                        {state === 'done' && (
                          <span className={styles.tag} data-t="done">
                            Hecho hoy
                          </span>
                        )}
                        {/* La periodicidad, visible por fin: es el dato que
                            explica por qué un mantenimiento vuelve. */}
                        <span className={styles.tag} data-t={record.intervalMonths ? 'rep' : 'once'}>
                          {everyLabel(record.intervalMonths)}
                        </span>
                      </p>

                      <p className={styles.rowMeta}>
                        {state === 'closed'
                          ? `Se hizo el ${formatDate(last ? last.completedDate : record.nextDueAt)}`
                          : `Próximo: ${formatDate(record.nextDueAt)}`}
                        {last ? ` · Última vez: ${formatDate(last.completedDate)}` : ' · Nunca registrado'}
                      </p>
                    </div>

                    <span className={styles.rowWhen}>{whenLabel(record, state)}</span>

                    <div className={styles.rowActions}>
                      {state === 'done' || state === 'closed' ? (
                        <button
                          type="button"
                          className={styles.actionButton}
                          disabled={busyId === record.id}
                          onClick={() => void handleUndo(record)}
                        >
                          Deshacer
                        </button>
                      ) : (
                        <button
                          type="button"
                          className={`${styles.actionButton} ${styles.actionPrimary}`}
                          disabled={busyId === record.id}
                          onClick={() => void handleComplete(record)}
                        >
                          Hecho
                        </button>
                      )}

                      <button
                        type="button"
                        className={styles.actionButton}
                        onClick={() => setDetailId(record.id)}
                      >
                        Detalle
                      </button>

                      <SimpleDeleteConfirm
                        resourceLabel="mantenimiento"
                        itemName={record.item}
                        ariaLabel={`Eliminar ${record.item}`}
                        onConfirm={() => handleDelete(record.id)}
                      />
                    </div>
                  </article>
                )
              })}
            </div>
          </section>
        ))}

      {toast && (
        <p className={styles.toast} data-kind={toast.kind} role="status">
          {toast.text}
        </p>
      )}

      {detailRecord && (
        <MaintenanceDetailDialog
          record={detailRecord}
          onClose={() => setDetailId(null)}
          onSaved={handleSaved}
          onCompleted={handleSaved}
          onLogChanged={() => void listMaintenanceLog().then(setLog)}
        />
      )}
    </AppShell>
  )
}
