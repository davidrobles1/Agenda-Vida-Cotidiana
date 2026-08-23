import { useEffect, useState } from 'react'
import { Wrench } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconWrench } from '../../core/ui/icons'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { completeMaintenanceRecord, deleteMaintenanceRecord, listMaintenanceRecords, type MaintenanceRecord, type MaintenanceStatus } from './api'
import { CreateMaintenanceDialog } from './CreateMaintenanceDialog'
import styles from './MaintenancePage.module.css'

const toneByStatus: Record<MaintenanceStatus, 'success' | 'warning' | 'error'> = {
  AL_DIA: 'success',
  PROXIMO: 'warning',
  VENCIDO: 'error',
  COMPLETADO: 'success',
}

const labelByStatus: Record<MaintenanceStatus, string> = {
  AL_DIA: 'Al día',
  PROXIMO: 'Próximo',
  VENCIDO: 'Vencido',
  COMPLETADO: 'Completado',
}

function formatNextDueAt(iso: string): string {
  return iso.slice(0, 10)
}

/**
 * BE-037/WEB-009, ahora con CRUD completo (pedido explícito del usuario,
 * 2026-08-21: "aunque esta parte es simple tenemos que ser sofisticados...
 * necesitamos que la gente lo utilice"). El backend sigue siendo el mismo
 * CRUD simple — la sofisticación vive en quitarle fricción: el modal de
 * registro (CreateMaintenanceDialog) ofrece intervalos comunes con un
 * clic, y este empty state explica *por qué* vale la pena registrar un
 * mantenimiento en vez de solo mostrar una lista vacía.
 */
export function MaintenancePage() {
  const [records, setRecords] = useState<MaintenanceRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listMaintenanceRecords()
      setRecords(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los mantenimientos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(record: MaintenanceRecord) {
    setRecords((current) => [record, ...current])
  }

  async function handleComplete(record: MaintenanceRecord) {
    const updated = await completeMaintenanceRecord(record.id, record.version)
    setRecords((current) => current.map((r) => (r.id === updated.id ? updated : r)))
  }

  async function handleDelete(id: string) {
    await deleteMaintenanceRecord(id)
    setRecords((current) => current.filter((r) => r.id !== id))
  }

  return (
    <AppShell title="Mantenimiento" subtitle="Seguimiento al mantenimiento de tus artículos.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Próximos mantenimientos" action={<CreateMaintenanceDialog onCreated={handleCreated} />}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && records.length === 0 && (
          <div className={styles.emptyState}>
            <Wrench width={28} height={28} aria-hidden="true" />
            <p className={styles.emptyStateTitle}>Un mantenimiento a tiempo evita gastos grandes después.</p>
            <p className={styles.emptyStateBody}>
              Registra el filtro, el aire acondicionado o cualquier cosa que necesite atención periódica — te avisamos
              cuándo toca.
            </p>
          </div>
        )}
        {!loading &&
          records.map((record) => (
            <ListItemRow
              key={record.id}
              title={record.item}
              subtitle={`Próximo: ${formatNextDueAt(record.nextDueAt)}`}
              icon={IconWrench}
              tone={toneByStatus[record.status]}
              pillLabel={labelByStatus[record.status]}
              pillTone={toneByStatus[record.status]}
              trailing={
                <div className={styles.rowActions}>
                  {record.status !== 'COMPLETADO' && (
                    <button type="button" className={styles.completeButton} onClick={() => void handleComplete(record)}>
                      Completar
                    </button>
                  )}
                  <SimpleDeleteConfirm
                    resourceLabel="mantenimiento"
                    itemName={record.item}
                    ariaLabel="Eliminar mantenimiento"
                    onConfirm={() => handleDelete(record.id)}
                  />
                </div>
              }
            />
          ))}
      </ListSectionCard>
    </AppShell>
  )
}
