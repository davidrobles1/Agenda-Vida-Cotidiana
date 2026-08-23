import { useEffect, useState } from 'react'
import { ToggleButtonGroup, type Key } from 'react-aria-components'
import { Eye, Globe, Lock, Users } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { FilterChip } from '../../core/ui/components/FilterChip'
import { IconFolder } from '../../core/ui/icons'
import { FileViewerModal } from '../../core/ui/viewers/FileViewerModal'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import {
  DOCUMENT_CATEGORIES,
  DOCUMENT_CATEGORY_LABELS,
  deleteDocument,
  listDocuments,
  type DocumentCategory,
  type VidaDocument,
} from './api'
import { UploadDocumentDialog } from './UploadDocumentDialog'
import { ShareDocumentDialog } from './ShareDocumentDialog'
import styles from './DocumentsPage.module.css'

const ALL_KEY = '__all__'

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-MX', { day: '2-digit', month: 'short', year: 'numeric' })
}

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function visibilityIcon(document: VidaDocument) {
  if (document.visibility === 'FAMILY_PUBLIC') return Globe
  if (document.visibility === 'SHARED') return Users
  return Lock
}

/**
 * Módulo Documentos real (pedido explícito del usuario, 2026-08-22) —
 * reemplaza el scaffolding UX-006 (mock data) por backend real
 * (document.domain.Document): subir/actualizar/borrar/compartir, categorías
 * Identificación/Comprobantes/Seguros/Contratos/Otros, visor de imagen/PDF
 * (core/ui/viewers/FileViewerModal, compartido con Garantías).
 */
export function DocumentsPage() {
  const [documents, setDocuments] = useState<VidaDocument[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedKeys, setSelectedKeys] = useState<Set<Key>>(new Set([ALL_KEY]))
  const [viewingDocument, setViewingDocument] = useState<VidaDocument | null>(null)

  const selectedCategory = selectedKeys.has(ALL_KEY) ? null : ((selectedKeys.values().next().value as string) as DocumentCategory)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listDocuments()
      setDocuments(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los documentos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  const visibleDocuments = selectedCategory ? documents.filter((doc) => doc.category === selectedCategory) : documents

  function handleUploaded(document: VidaDocument) {
    setDocuments((current) => [document, ...current])
  }

  function handleChanged(updated: VidaDocument) {
    setDocuments((current) => current.map((doc) => (doc.id === updated.id ? updated : doc)))
    setViewingDocument((current) => (current?.id === updated.id ? updated : current))
  }

  async function handleDelete(id: string) {
    await deleteDocument(id)
    setDocuments((current) => current.filter((doc) => doc.id !== id))
  }

  return (
    <AppShell title="Documentos" subtitle="Organiza tus documentos importantes.">
      <div className={styles.categoryGrid}>
        {DOCUMENT_CATEGORIES.map((category) => {
          const count = documents.filter((doc) => doc.category === category).length
          return (
            <div key={category} className={styles.categoryCard}>
              <span className={styles.categoryIcon}>
                <IconFolder width={18} height={18} />
              </span>
              <span className={styles.categoryName}>{DOCUMENT_CATEGORY_LABELS[category]}</span>
              <span className={styles.categoryCount}>{count} documentos</span>
            </div>
          )
        })}
      </div>

      <ToggleButtonGroup
        aria-label="Filtrar por categoría"
        selectionMode="single"
        disallowEmptySelection
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
        className={styles.chipRow}
      >
        <FilterChip id={ALL_KEY} label="Todos" />
        {DOCUMENT_CATEGORIES.map((category) => (
          <FilterChip key={category} id={category} label={DOCUMENT_CATEGORY_LABELS[category]} />
        ))}
      </ToggleButtonGroup>

      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Documentos" action={<UploadDocumentDialog onUploaded={handleUploaded} />}>
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && visibleDocuments.length === 0 && <p className={styles.emptyHint}>Todavía no hay documentos aquí.</p>}
        {!loading &&
          visibleDocuments.map((document) => (
            <ListItemRow
              key={document.id}
              title={document.name}
              subtitle={`${DOCUMENT_CATEGORY_LABELS[document.category]} · ${formatSize(document.sizeBytes)} · ${formatDate(document.createdAt)}`}
              icon={visibilityIcon(document)}
              tone="info"
              trailing={
                <div className={styles.rowActions}>
                  <button type="button" className={styles.iconButton} onClick={() => setViewingDocument(document)} aria-label="Ver documento">
                    <Eye width={16} height={16} />
                  </button>
                  <ShareDocumentDialog document={document} onChanged={handleChanged} />
                  <SimpleDeleteConfirm
                    resourceLabel="documento"
                    itemName={document.name}
                    ariaLabel="Eliminar documento"
                    onConfirm={() => handleDelete(document.id)}
                  />
                </div>
              }
            />
          ))}
      </ListSectionCard>

      <FileViewerModal
        isOpen={viewingDocument !== null}
        onOpenChange={(open) => {
          if (!open) setViewingDocument(null)
        }}
        title={viewingDocument?.name ?? ''}
        contentPath={viewingDocument ? `/documents/${viewingDocument.id}/content` : undefined}
        contentType={viewingDocument?.contentType ?? ''}
      />
    </AppShell>
  )
}
