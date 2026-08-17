import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconWrench } from '../../core/ui/icons'
import { maintenanceRecords, type MaintenanceStatus } from '../../core/mock/mockData'

const toneByStatus: Record<MaintenanceStatus, 'success' | 'warning' | 'error'> = {
  AL_DIA: 'success',
  PROXIMO: 'warning',
  VENCIDO: 'error',
}

const labelByStatus: Record<MaintenanceStatus, string> = {
  AL_DIA: 'Al día',
  PROXIMO: 'Próximo',
  VENCIDO: 'Vencido',
}

/** UX-006: scaffolding-only module — mock data (core/mock/mockData.ts), zero endpoints, zero logic. */
export function MaintenancePage() {
  return (
    <AppShell title="Mantenimiento" subtitle="Seguimiento al mantenimiento de tus artículos.">
      <ListSectionCard title="Próximos mantenimientos">
        {maintenanceRecords.map((record) => (
          <ListItemRow
            key={record.id}
            title={record.item}
            subtitle={`Próximo: ${record.nextDueAt}`}
            icon={IconWrench}
            tone={toneByStatus[record.status]}
            pillLabel={labelByStatus[record.status]}
            pillTone={toneByStatus[record.status]}
          />
        ))}
      </ListSectionCard>
    </AppShell>
  )
}
