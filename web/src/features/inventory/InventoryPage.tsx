import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, ToggleButtonGroup, type Key } from 'react-aria-components'
import { Pencil, Plus, Search } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { FilterChip } from '../../core/ui/components/FilterChip'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import styles from '../../core/ui/patterns/SectionList.module.css'
import { listWarranties, type Warranty } from '../warranties/api'
import { STATE_MARKS } from '../warranties/warrantiesView'
import {
  INVENTORY_CATEGORIES,
  INVENTORY_CATEGORY_LABELS,
  deleteInventoryItem,
  listInventoryItems,
  type InventoryCategory,
  type InventoryItem,
} from './api'
import {
  WARRANTY_TAG_LABELS,
  emptyReason,
  groupByLocation,
  indexWarrantiesByItem,
  summarizeInventory,
} from './inventoryView'
import { InventoryItemDialog } from './InventoryItemDialog'

/** Clave del chip "Todos" — `null` no sirve como Key de React Aria. */
const ALL_KEY = '__all__'

/**
 * ADR-022: Inventario conforme al prototipo aprobado (artifact ebd2e1a2).
 *
 * Lo que cambia de fondo:
 *
 *  1. **Aislamiento por módulo.** `inventory_items` no tenía columna
 *     `context` (migración V28) y esta pantalla no usaba `useActiveMode`:
 *     Personal y Laboral compartían inventario, en contra del ADR-019.
 *  2. **El filtro y la búsqueda se resuelven en el SERVIDOR.** Antes se
 *     filtraba en memoria sobre la página cargada, así que elegir una
 *     categoría solo miraba esos artículos y ocultaba el resto sin decirlo.
 *  3. **Vínculo con Garantías**: la fila dice si el artículo sigue cubierto.
 *  4. **El vacío distingue** "no hay nada" de "no hay nada con este filtro".
 *  5. **El estado vacío ya no aparece cuando falla la carga.**
 */
export function InventoryPage() {
  const activeMode = useActiveMode()
  const [items, setItems] = useState<InventoryItem[]>([])
  const [warranties, setWarranties] = useState<Warranty[]>([])
  const [total, setTotal] = useState(0)
  const [totalUnfiltered, setTotalUnfiltered] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [category, setCategory] = useState<InventoryCategory | null>(null)
  /** Lo que el usuario escribe; `query` es lo que ya viajó al servidor. */
  const [draft, setDraft] = useState('')
  const [query, setQuery] = useState('')

  // La búsqueda va al servidor, así que se espera a que el usuario deje de
  // escribir en vez de pedir una consulta por tecla.
  useEffect(() => {
    const timer = setTimeout(() => setQuery(draft), 300)
    return () => clearTimeout(timer)
  }, [draft])

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [page, warrantyPage] = await Promise.all([
        listInventoryItems(activeMode, category, query),
        listWarranties(activeMode).catch(() => ({ items: [] as Warranty[] })),
      ])
      setItems(page.items)
      setTotal(page.totalElements)
      setWarranties(warrantyPage.items)
      // El total sin filtrar sostiene los mensajes del estado vacío
      // ("tienes N artículos registrados"), que si no mentirían al contar
      // solo lo que el filtro dejó pasar.
      if (!category && !query.trim()) setTotalUnfiltered(page.totalElements)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los artículos.')
    } finally {
      setLoading(false)
    }
  }, [activeMode, category, query])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const warrantyByItem = useMemo(() => indexWarrantiesByItem(warranties), [warranties])
  const summary = useMemo(() => summarizeInventory(items, warrantyByItem), [items, warrantyByItem])
  const groups = useMemo(() => groupByLocation(items), [items])

  function handleSaved(saved: InventoryItem) {
    setItems((current) => {
      const exists = current.some((item) => item.id === saved.id)
      return exists ? current.map((item) => (item.id === saved.id ? saved : item)) : [saved, ...current]
    })
    setTotal((current) => (items.some((item) => item.id === saved.id) ? current : current + 1))
    setTotalUnfiltered((current) => (items.some((item) => item.id === saved.id) ? current : current + 1))
  }

  async function handleDelete(id: string) {
    await deleteInventoryItem(id)
    setItems((current) => current.filter((item) => item.id !== id))
    setTotal((current) => Math.max(0, current - 1))
    setTotalUnfiltered((current) => Math.max(0, current - 1))
  }

  function resetFilters() {
    setCategory(null)
    setDraft('')
    setQuery('')
  }

  const isFiltering = category !== null || query.trim().length > 0
  const hasMore = total > items.length
  const empty = emptyReason(totalUnfiltered, query, category ? INVENTORY_CATEGORY_LABELS[category] : null)

  return (
    <AppShell title="Inventario" subtitle="Qué tienes, dónde está y si sigue con garantía."
      actions={<><InventoryItemDialog
          onSaved={handleSaved}
          trigger={
            <Button className={styles.primaryButton}>
              <Plus width={15} height={15} /> Nuevo artículo
            </Button>
          }
        /></>}
    >
      {error && (
        <p role="alert" className={styles.error}>
          <strong>No se pudieron cargar los artículos.</strong>
          {error}
        </p>
      )}

      {!loading && !error && (totalUnfiltered > 0 || isFiltering) && (
        <>
          <section className={styles.summary}>
            <div className={`${styles.summaryCard} ${styles.summaryLead}`}>
              <span className={styles.summaryLabel}>Artículos registrados</span>
              <p className={styles.summaryValue}>{isFiltering ? total : totalUnfiltered}</p>
              <p className={styles.summaryMeta}>
                {summary.locationCount === 1 ? 'en 1 ubicación' : `en ${summary.locationCount} ubicaciones`}
                {isFiltering ? ` · de ${totalUnfiltered} en total` : ''}
              </p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Con garantía vigente</span>
              <p className={styles.summaryValue}>{summary.withWarrantyCount}</p>
              <p className={styles.summaryMeta}>enlazados a Garantías</p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Sin ubicación</span>
              <p className={styles.summaryValue}>{summary.withoutLocationCount}</p>
              <p className={styles.summaryMeta}>
                {summary.withoutLocationCount > 0 ? 'conviene completarlos' : 'todos ubicados'}
              </p>
            </div>
          </section>

          <div className={styles.controls}>
            <div className={styles.search}>
              <Search className={styles.searchIcon} width={15} height={15} aria-hidden="true" />
              <input
                className={styles.searchInput}
                type="search"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Buscar por nombre o ubicación…"
                aria-label="Buscar artículos"
              />
            </div>

            {/* ToggleButtonGroup y no botones sueltos: aporta semántica real
                de radiogroup y navegación con flechas entre chips (UX-011
                Fase 3). Cambiar la apariencia no puede costar el
                comportamiento. */}
            <ToggleButtonGroup
              aria-label="Filtrar por categoría"
              selectionMode="single"
              disallowEmptySelection
              selectedKeys={new Set<Key>([category ?? ALL_KEY])}
              onSelectionChange={(keys) => {
                const next = [...keys][0] as string | undefined
                setCategory(!next || next === ALL_KEY ? null : (next as InventoryCategory))
              }}
              className={styles.filters}
            >
              <FilterChip id={ALL_KEY} label="Todos" className={styles.chip} />
              {INVENTORY_CATEGORIES.map((option) => (
                <FilterChip
                  key={option}
                  id={option}
                  label={INVENTORY_CATEGORY_LABELS[option]}
                  className={styles.chip}
                />
              ))}
            </ToggleButtonGroup>
          </div>
        </>
      )}

      {loading && <p className={styles.hint}>Cargando…</p>}

      {!loading && !error && items.length === 0 && (
        <div className={styles.empty}>
          <h3>{empty.title}</h3>
          <p>{empty.body}</p>
          <div className={styles.emptyAction}>
            {empty.canReset ? (
              <button type="button" className={styles.primaryButton} onClick={resetFilters}>
                Ver todos
              </button>
            ) : (
              <InventoryItemDialog
                onSaved={handleSaved}
                trigger={
                  <Button className={styles.primaryButton}>
                    <Plus width={15} height={15} /> Nuevo artículo
                  </Button>
                }
              />
            )}
          </div>
        </div>
      )}

      {!loading &&
        !error &&
        groups.map((group) => (
          <section key={group.id} className={styles.group}>
            <h3 className={styles.groupLabel}>{group.label}</h3>

            <div className={styles.rows}>
              {group.items.map((item) => {
                const link = warrantyByItem.get(item.id)
                // El estado de la fila lo marca la garantía cuando la hay:
                // es la única información con urgencia que tiene un
                // artículo de inventario.
                const state = link ? link.state : 'plain'

                return (
                  <article key={item.id} className={styles.row} data-state={state}>
                    <span className={styles.rowMark} aria-hidden="true">
                      {link ? STATE_MARKS[link.state] : '•'}
                    </span>

                    <div className={styles.rowBody}>
                      <p className={styles.rowName}>
                        {item.name}
                        <span className={styles.tag}>{INVENTORY_CATEGORY_LABELS[item.category]}</span>
                        {link && (
                          <span className={styles.tag} data-t={link.state}>
                            {WARRANTY_TAG_LABELS[link.state]}
                          </span>
                        )}
                      </p>

                      <p className={styles.rowMeta}>
                        {item.location?.trim() ? `Ubicación: ${item.location}` : 'Sin ubicación registrada'}
                        {link ? ` · Garantía: ${link.warranty.item}` : ''}
                      </p>
                    </div>

                    <span className={styles.rowWhen} />

                    <div className={styles.rowActions}>
                      <InventoryItemDialog
                        item={item}
                        onSaved={handleSaved}
                        trigger={
                          <Button className={styles.iconButton} aria-label={`Editar ${item.name}`}>
                            <Pencil width={14} height={14} />
                          </Button>
                        }
                      />
                      <SimpleDeleteConfirm
                        resourceLabel="artículo"
                        itemName={item.name}
                        ariaLabel={`Eliminar ${item.name}`}
                        onConfirm={() => handleDelete(item.id)}
                      />
                    </div>
                  </article>
                )
              })}
            </div>
          </section>
        ))}

      {!loading && !error && hasMore && (
        <p className={styles.moreRow}>
          Mostrando {items.length} de {total} artículos. Afina la búsqueda para encontrar el que falta.
        </p>
      )}

    </AppShell>
  )
}
