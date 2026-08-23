import { useEffect, useState } from 'react'
import { Button, ToggleButtonGroup, type Key } from 'react-aria-components'
import { Pencil, Plus } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { FilterChip } from '../../core/ui/components/FilterChip'
import { IconInventory } from '../../core/ui/icons'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import {
  INVENTORY_CATEGORIES,
  INVENTORY_CATEGORY_LABELS,
  deleteInventoryItem,
  listInventoryItems,
  type InventoryCategory,
  type InventoryItem,
} from './api'
import { InventoryItemDialog } from './InventoryItemDialog'
import styles from './InventoryPage.module.css'

const ALL_KEY = '__all__'

/** Módulo Inventario real (pedido explícito del usuario, 2026-08-22) —
    reemplaza el scaffolding UX-006 por backend real
    (inventory.domain.InventoryItem): registrar/actualizar/borrar
    artículos, filtrado por categoría (Todos/Electrónicos/Hogar/Vehículos). */
export function InventoryPage() {
  const [items, setItems] = useState<InventoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedKeys, setSelectedKeys] = useState<Set<Key>>(new Set([ALL_KEY]))

  const selected = selectedKeys.has(ALL_KEY) ? null : ((selectedKeys.values().next().value as string) as InventoryCategory)
  const visibleItems = selected ? items.filter((item) => item.category === selected) : items

  useEffect(() => {
    void refresh()
  }, [])

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listInventoryItems()
      setItems(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los artículos.')
    } finally {
      setLoading(false)
    }
  }

  function handleSaved(saved: InventoryItem) {
    setItems((current) => {
      const exists = current.some((item) => item.id === saved.id)
      return exists ? current.map((item) => (item.id === saved.id ? saved : item)) : [saved, ...current]
    })
  }

  async function handleDelete(id: string) {
    await deleteInventoryItem(id)
    setItems((current) => current.filter((item) => item.id !== id))
  }

  return (
    <AppShell title="Inventario" subtitle="Un registro de tus artículos importantes.">
      <ToggleButtonGroup
        aria-label="Filtrar por categoría"
        selectionMode="single"
        disallowEmptySelection
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
        className={styles.chipRow}
      >
        <FilterChip id={ALL_KEY} label="Todos" />
        {INVENTORY_CATEGORIES.map((category) => (
          <FilterChip key={category} id={category} label={INVENTORY_CATEGORY_LABELS[category]} />
        ))}
      </ToggleButtonGroup>

      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard
        title="Artículos"
        action={
          <InventoryItemDialog
            onSaved={handleSaved}
            trigger={
              <Button className={styles.addButton}>
                <Plus width={16} height={16} /> Nuevo artículo
              </Button>
            }
          />
        }
      >
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && visibleItems.length === 0 && <p className={styles.emptyHint}>Todavía no hay artículos aquí.</p>}
        {!loading &&
          visibleItems.map((item) => (
            <ListItemRow
              key={item.id}
              title={item.name}
              subtitle={item.category ? INVENTORY_CATEGORY_LABELS[item.category] : undefined}
              icon={IconInventory}
              tone="primary"
              pillLabel={item.location}
              pillTone="info"
              trailing={
                <div className={styles.rowActions}>
                  <InventoryItemDialog
                    item={item}
                    onSaved={handleSaved}
                    trigger={
                      <Button className={styles.iconButton} aria-label="Editar artículo">
                        <Pencil width={16} height={16} />
                      </Button>
                    }
                  />
                  <SimpleDeleteConfirm
                    resourceLabel="artículo"
                    itemName={item.name}
                    ariaLabel="Eliminar artículo"
                    onConfirm={() => handleDelete(item.id)}
                  />
                </div>
              }
            />
          ))}
      </ListSectionCard>
    </AppShell>
  )
}
