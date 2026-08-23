import { useEffect, useState } from 'react'
import { Eye } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconShield } from '../../core/ui/icons'
import { FileViewerModal } from '../../core/ui/viewers/FileViewerModal'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { completeWarranty, deleteWarranty, listWarranties, type Warranty, type WarrantyStatus } from './api'
import { CreateWarrantyDialog } from './CreateWarrantyDialog'
import styles from './WarrantiesPage.module.css'

const toneByStatus: Record<WarrantyStatus, 'success' | 'warning' | 'error'> = {
  VIGENTE: 'success',
  POR_VENCER: 'warning',
  VENCIDA: 'error',
  COMPLETADO: 'success',
}

const labelByStatus: Record<WarrantyStatus, string> = {
  VIGENTE: 'Vigente',
  POR_VENCER: 'Por vencer',
  VENCIDA: 'Vencida',
  COMPLETADO: 'Completada',
}

function formatExpiresAt(iso: string): string {
  return iso.slice(0, 10)
}

/**
 * BE-037/WEB-009, ahora con CRUD completo (pedido explícito del usuario,
 * 2026-08-21): registrar (con archivo obligatorio imagen/PDF — modal
 * central, CreateWarrantyDialog), completar, y "ver garantía" con el
 * visor de imagen/PDF compartido (core/ui/viewers/FileViewerModal, mismo
 * que Documentos). Sin edición de campos en esta pasada — el pedido
 * original enumeró explícitamente "registrar... subir el archivo...
 * ver garantía," no editar; completar/eliminar sí, por ser parte del
 * ciclo de vida ya existente en el backend.
 */
export function WarrantiesPage() {
  const [warranties, setWarranties] = useState<Warranty[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [viewingWarranty, setViewingWarranty] = useState<Warranty | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listWarranties()
      setWarranties(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las garantías.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(warranty: Warranty) {
    setWarranties((current) => [warranty, ...current])
  }

  async function handleComplete(warranty: Warranty) {
    const updated = await completeWarranty(warranty.id, warranty.version)
    setWarranties((current) => current.map((w) => (w.id === updated.id ? updated : w)))
  }

  async function handleDelete(id: string) {
    await deleteWarranty(id)
    setWarranties((current) => current.filter((w) => w.id !== id))
  }

  return (
    <AppShell title="Garantías" subtitle="Da seguimiento a las garantías de tus artículos.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Todas las garantías" action={<CreateWarrantyDialog onCreated={handleCreated} />}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && warranties.length === 0 && <p className={styles.emptyHint}>Todavía no hay garantías registradas.</p>}
        {!loading &&
          warranties.map((warranty) => (
            <ListItemRow
              key={warranty.id}
              title={warranty.item}
              subtitle={`Vence: ${formatExpiresAt(warranty.expiresAt)}`}
              icon={IconShield}
              tone={toneByStatus[warranty.status]}
              pillLabel={labelByStatus[warranty.status]}
              pillTone={toneByStatus[warranty.status]}
              trailing={
                <div className={styles.rowActions}>
                  {warranty.documentContentType && (
                    <button
                      type="button"
                      className={styles.iconButton}
                      onClick={() => setViewingWarranty(warranty)}
                      aria-label="Ver garantía"
                    >
                      <Eye width={16} height={16} />
                    </button>
                  )}
                  {warranty.status !== 'COMPLETADO' && (
                    <button type="button" className={styles.completeButton} onClick={() => void handleComplete(warranty)}>
                      Completar
                    </button>
                  )}
                  <SimpleDeleteConfirm
                    resourceLabel="garantía"
                    itemName={warranty.item}
                    ariaLabel="Eliminar garantía"
                    onConfirm={() => handleDelete(warranty.id)}
                  />
                </div>
              }
            />
          ))}
      </ListSectionCard>

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
