import { useCallback, useEffect, useMemo, useState } from 'react'
import { ToggleButtonGroup, type Key } from 'react-aria-components'
import { Eye } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { FileViewerModal } from '../../core/ui/viewers/FileViewerModal'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { FilterChip } from '../../core/ui/components/FilterChip'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import styles from '../../core/ui/patterns/SectionList.module.css'
import { listInventoryItems, type InventoryItem } from '../inventory/api'
import { completeWarranty, deleteWarranty, listWarranties, type Warranty } from './api'
import {
  STATE_LABELS,
  STATE_MARKS,
  WARRANTY_FILTERS,
  fileLabel,
  formatDate,
  groupWarranties,
  summarizeWarranties,
  warrantyState,
  whenLabel,
  type WarrantyViewState,
} from './warrantiesView'
import { CreateWarrantyDialog } from './CreateWarrantyDialog'
import { WarrantyDetailDialog } from './WarrantyDetailDialog'

/**
 * ADR-022: Garantías conforme al prototipo aprobado (artifact ebd2e1a2).
 *
 * Lo que cambia de fondo, más allá de lo visual:
 *
 *  1. **Se puede editar.** `PATCH /warranties/{id}` y `updateWarranty()`
 *     existían y ninguna pantalla los llamaba.
 *  2. **Cambiar de módulo recarga la lista.** El `useEffect` no dependía de
 *     `activeMode`, así que al pasar de Personal a Laboral seguían viéndose
 *     las garantías del módulo anterior — ADR-019 roto en el último paso.
 *  3. **"Completar" pasa a "Usada"**, y se puede deshacer: el endpoint
 *     alterna desde siempre, pero la interfaz escondía el botón.
 *  4. **El estado vacío ya no aparece cuando falla la carga.**
 *  5. **La fecha se formatea** en vez de mostrarse en crudo.
 */
type Filter = 'all' | WarrantyViewState

export function WarrantiesPage() {
  // ADR-019: el recurso nace en el módulo desde el que se crea, y la lista
  // solo pide los de ese módulo.
  const activeMode = useActiveMode()
  const [warranties, setWarranties] = useState<Warranty[]>([])
  const [inventory, setInventory] = useState<InventoryItem[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<Filter>('all')
  const [busyId, setBusyId] = useState<string | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [viewingWarranty, setViewingWarranty] = useState<Warranty | null>(null)
  const [toast, setToast] = useState<{ kind: 'done' | 'undo'; text: string } | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      // El inventario se carga a la vez para poder nombrar el artículo
      // enlazado en la fila — mismo patrón con el que MaintenancePage carga
      // su historial.
      const [page, inventoryPage] = await Promise.all([
        listWarranties(activeMode),
        listInventoryItems(activeMode).catch(() => ({ items: [] as InventoryItem[], totalElements: 0 })),
      ])
      setWarranties(page.items)
      setTotal(page.totalElements)
      setInventory(inventoryPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las garantías.')
    } finally {
      setLoading(false)
    }
  }, [activeMode])

  // ADR-022: la dependencia de `activeMode` es la corrección — sin ella,
  // cambiar de módulo dejaba en pantalla los datos del anterior.
  useEffect(() => {
    void refresh()
  }, [refresh])

  const itemNameById = useMemo(() => {
    const map = new Map<string, InventoryItem>()
    for (const item of inventory) map.set(item.id, item)
    return map
  }, [inventory])

  const stateOf = useCallback((warranty: Warranty): WarrantyViewState => warrantyState(warranty), [])
  const summary = useMemo(() => summarizeWarranties(warranties, stateOf), [warranties, stateOf])
  const filtered = useMemo(
    () => (filter === 'all' ? warranties : warranties.filter((warranty) => stateOf(warranty) === filter)),
    [warranties, filter, stateOf],
  )
  const groups = useMemo(() => groupWarranties(filtered, stateOf), [filtered, stateOf])

  function handleSaved(saved: Warranty) {
    setWarranties((current) => {
      const exists = current.some((warranty) => warranty.id === saved.id)
      return exists ? current.map((w) => (w.id === saved.id ? saved : w)) : [saved, ...current]
    })
    setTotal((current) => (warranties.some((w) => w.id === saved.id) ? current : current + 1))
    setToast(null)
  }

  async function handleToggleUsed(warranty: Warranty) {
    setBusyId(warranty.id)
    setError(null)
    const wasUsed = stateOf(warranty) === 'used'
    try {
      const updated = await completeWarranty(warranty.id, warranty.version)
      setWarranties((current) => current.map((w) => (w.id === updated.id ? updated : w)))
      setToast({
        kind: wasUsed ? 'undo' : 'done',
        text: wasUsed
          ? `“${updated.item}” vuelve a contar como vigente.`
          : `“${updated.item}” queda registrada como usada. Deja de avisarte y de aparecer en el calendario.`,
      })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cambiar el estado de la garantía.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete(id: string) {
    await deleteWarranty(id)
    setWarranties((current) => current.filter((warranty) => warranty.id !== id))
    setTotal((current) => Math.max(0, current - 1))
  }

  const detailWarranty = warranties.find((warranty) => warranty.id === detailId)
  const hasMore = total > warranties.length

  return (
    <AppShell title="Garantías" subtitle="Lo que aún está cubierto, y hasta cuándo."
      actions={<><CreateWarrantyDialog onCreated={handleSaved} /></>}
    >
      {/* Un error de carga es un hecho distinto de "no hay garantías": si se
          confunden, la pantalla afirma algo falso sobre los datos del
          usuario. Ver ADR-021(k). */}
      {error && (
        <p role="alert" className={styles.error}>
          <strong>No se pudieron cargar las garantías.</strong>
          {error}
        </p>
      )}

      {!loading && !error && warranties.length > 0 && (
        <>
          <section className={styles.summary}>
            <div className={`${styles.summaryCard} ${styles.summaryLead}`}>
              <span className={styles.summaryLabel}>Lo siguiente en vencer</span>
              <p className={styles.summaryValue}>{summary.next ? summary.next.item : 'Nada por vencer'}</p>
              <p className={styles.summaryMeta}>
                {summary.next
                  ? `${formatDate(summary.next.expiresAt)} · ${whenLabel(summary.next, stateOf(summary.next))}`
                  : 'Ninguna garantía vigente por vencer'}
              </p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Por vencer</span>
              <p className={styles.summaryValue}>{summary.soonCount}</p>
              <p className={styles.summaryMeta}>en los próximos 30 días</p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Vigentes</span>
              <p className={styles.summaryValue}>{summary.activeCount}</p>
              <p className={styles.summaryMeta}>de {summary.totalCount} registradas</p>
            </div>
          </section>

          <div className={styles.controls}>
            {/* Misma semántica de grupo que Inventario: radiogroup real y
                navegación con flechas, no botones sueltos. */}
            <ToggleButtonGroup
              aria-label="Filtrar garantías"
              selectionMode="single"
              disallowEmptySelection
              selectedKeys={new Set<Key>([filter])}
              onSelectionChange={(keys) => {
                const next = [...keys][0] as Filter | undefined
                if (next) setFilter(next)
              }}
              className={styles.filters}
            >
              {WARRANTY_FILTERS.map((option) => (
                <FilterChip key={option.id} id={option.id} label={option.label} className={styles.chip} />
              ))}
            </ToggleButtonGroup>
          </div>
        </>
      )}

      {loading && <p className={styles.hint}>Cargando…</p>}

      {!loading && !error && warranties.length === 0 && (
        <div className={styles.empty}>
          <h3>Aún no registras ninguna garantía</h3>
          <p>Agrega la primera con su comprobante y sabrás cuándo vence — y te avisaremos antes.</p>
          <div className={styles.emptyAction}>
            <CreateWarrantyDialog onCreated={handleSaved} />
          </div>
        </div>
      )}

      {!loading && !error && warranties.length > 0 && groups.length === 0 && (
        <div className={styles.empty}>
          <h3>Ninguna garantía en este filtro</h3>
          <p>Tienes {summary.totalCount} garantías registradas. Cambia el filtro para ver el resto.</p>
          <div className={styles.emptyAction}>
            <button type="button" className={styles.primaryButton} onClick={() => setFilter('all')}>
              Ver todas
            </button>
          </div>
        </div>
      )}

      {!loading &&
        !error &&
        groups.map((group) => (
          <section key={group.id} className={styles.group}>
            <h3 className={styles.groupLabel}>{group.label}</h3>

            <div className={styles.rows}>
              {group.items.map((warranty) => {
                const state = stateOf(warranty)
                const linked = warranty.inventoryItemId ? itemNameById.get(warranty.inventoryItemId) : undefined

                return (
                  <article key={warranty.id} className={styles.row} data-state={state}>
                    <span className={styles.rowMark} aria-hidden="true">
                      {STATE_MARKS[state]}
                    </span>

                    <div className={styles.rowBody}>
                      <p className={styles.rowName}>
                        {warranty.item}
                        {state !== 'ok' && (
                          <span className={styles.tag} data-t={state}>
                            {STATE_LABELS[state]}
                          </span>
                        )}
                        {/* ADR-022: el vínculo con Inventario, visible. Sin
                            él las dos secciones se ignoraban. */}
                        {warranty.inventoryItemId && (
                          <span className={styles.tag} data-t="link">
                            {linked ? `En inventario · ${linked.name}` : 'En inventario'}
                          </span>
                        )}
                      </p>

                      <p className={styles.rowMeta}>
                        Vence: {formatDate(warranty.expiresAt)} · {fileLabel(warranty.documentContentType)}
                      </p>
                    </div>

                    <span className={styles.rowWhen}>{whenLabel(warranty, state)}</span>

                    <div className={styles.rowActions}>
                      {warranty.documentContentType && (
                        <button
                          type="button"
                          className={styles.iconButton}
                          onClick={() => setViewingWarranty(warranty)}
                          aria-label={`Ver comprobante de ${warranty.item}`}
                        >
                          <Eye width={15} height={15} />
                        </button>
                      )}

                      <button
                        type="button"
                        className={`${styles.actionButton} ${state === 'used' ? '' : styles.actionPrimary}`}
                        disabled={busyId === warranty.id}
                        onClick={() => void handleToggleUsed(warranty)}
                      >
                        {state === 'used' ? 'Volver a vigente' : 'Marcar usada'}
                      </button>

                      <button type="button" className={styles.actionButton} onClick={() => setDetailId(warranty.id)}>
                        Editar
                      </button>

                      <SimpleDeleteConfirm
                        resourceLabel="garantía"
                        itemName={warranty.item}
                        ariaLabel={`Eliminar ${warranty.item}`}
                        onConfirm={() => handleDelete(warranty.id)}
                      />
                    </div>
                  </article>
                )
              })}
            </div>
          </section>
        ))}

      {/* Decirlo en vez de recortar en silencio. */}
      {!loading && !error && hasMore && (
        <p className={styles.moreRow}>
          Mostrando {warranties.length} de {total} garantías.
        </p>
      )}

      {toast && (
        <p className={styles.toast} data-kind={toast.kind} role="status">
          {toast.text}
        </p>
      )}

      {detailWarranty && (
        <WarrantyDetailDialog
          warranty={detailWarranty}
          onClose={() => setDetailId(null)}
          onSaved={handleSaved}
        />
      )}

      <FileViewerModal
        isOpen={viewingWarranty !== null}
        onOpenChange={(open) => {
          if (!open) setViewingWarranty(null)
        }}
        title={viewingWarranty?.item ?? ''}
        contentPath={viewingWarranty ? `/warranties/${viewingWarranty.id}/content` : undefined}
        contentType={viewingWarranty?.documentContentType ?? ''}
      />
    </AppShell>
  )
}
