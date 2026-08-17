import { useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { FilterChip } from '../../core/ui/components/FilterChip'
import { IconInventory } from '../../core/ui/icons'
import { inventoryCategories, inventoryItems } from '../../core/mock/mockData'
import styles from './InventoryPage.module.css'

/** UX-006: scaffolding-only module — mock data (core/mock/mockData.ts), zero endpoints, zero logic. */
export function InventoryPage() {
  const [selected, setSelected] = useState<string | null>(null)
  const visibleItems = selected ? inventoryItems.filter((item) => item.category === selected) : inventoryItems

  return (
    <AppShell title="Inventario" subtitle="Un registro de tus artículos importantes.">
      <div className={styles.chipRow}>
        <FilterChip label="Todos" selected={selected === null} onClick={() => setSelected(null)} />
        {inventoryCategories.map((category) => (
          <FilterChip key={category} label={category} selected={selected === category} onClick={() => setSelected(category)} />
        ))}
      </div>

      <ListSectionCard title="Artículos">
        {visibleItems.map((item) => (
          <ListItemRow key={item.id} title={item.name} subtitle={item.category} icon={IconInventory} tone="primary" pillLabel={item.location} pillTone="info" />
        ))}
      </ListSectionCard>
    </AppShell>
  )
}
