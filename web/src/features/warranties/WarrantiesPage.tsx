import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconShield } from '../../core/ui/icons'
import { warranties, type WarrantyStatus } from '../../core/mock/mockData'

const toneByStatus: Record<WarrantyStatus, 'success' | 'warning' | 'error'> = {
  VIGENTE: 'success',
  POR_VENCER: 'warning',
  VENCIDA: 'error',
}

const labelByStatus: Record<WarrantyStatus, string> = {
  VIGENTE: 'Vigente',
  POR_VENCER: 'Por vencer',
  VENCIDA: 'Vencida',
}

/** UX-006: scaffolding-only module — mock data (core/mock/mockData.ts), zero endpoints, zero logic. */
export function WarrantiesPage() {
  return (
    <AppShell title="Garantías" subtitle="Da seguimiento a las garantías de tus artículos.">
      <ListSectionCard title="Todas las garantías">
        {warranties.map((warranty) => (
          <ListItemRow
            key={warranty.id}
            title={warranty.item}
            subtitle={`Vence: ${warranty.expiresAt}`}
            icon={IconShield}
            tone={toneByStatus[warranty.status]}
            pillLabel={labelByStatus[warranty.status]}
            pillTone={toneByStatus[warranty.status]}
          />
        ))}
      </ListSectionCard>
    </AppShell>
  )
}
