import { useCallback, useEffect, useMemo, useState } from 'react'
import { ToggleButtonGroup, ToggleButton, type Key } from 'react-aria-components'
import { CheckSquare, Download, Eye, Search, Square } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { FileViewerModal } from '../../core/ui/viewers/FileViewerModal'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import { getCurrentUser } from '../../core/user/api'
import styles from '../../core/ui/patterns/SectionList.module.css'
import {
  DOCUMENT_CATEGORY_LABELS,
  deleteDocument,
  downloadDocument,
  downloadDocuments,
  listDocuments,
  type DocumentCategory,
  type VidaDocument,
} from './api'
import {
  VISIBILITY_LABELS,
  VISIBILITY_MARKS,
  VISIBILITY_TONES,
  categoryCounts,
  countLabel,
  emptyReason,
  formatSize,
  formatUploadedAt,
  isOwnedBy,
} from './documentsView'
import { UploadDocumentDialog } from './UploadDocumentDialog'
import { ShareDocumentDialog } from './ShareDocumentDialog'
import { DocumentDetailDialog } from './DocumentDetailDialog'

/**
 * ADR-022: Documentos conforme al prototipo aprobado (artifact ebd2e1a2).
 *
 * Lo que cambia de fondo:
 *
 *  1. **Se puede renombrar y recategorizar.** `PATCH /documents/{id}` existía
 *     y el cliente ni siquiera tenía la función.
 *  2. **Las tarjetas de categoría SON el filtro.** Antes mostraban un
 *     recuento y no eran pulsables, con una fila de chips justo debajo que sí
 *     filtraba: dos navegaciones para lo mismo y una muerta.
 *  3. **Filtro y búsqueda en el SERVIDOR.** El endpoint ya aceptaba
 *     `category` y la pantalla lo ignoraba, filtrando sobre la página
 *     cargada.
 *  4. **Aislamiento por módulo** (migración V28).
 *  5. **La visibilidad se lee**, en vez de depender de un icono sin rótulo.
 *  6. **Las acciones de dueño no se ofrecen sobre documentos ajenos**, que
 *     el backend rechaza con 404.
 */
export function DocumentsPage() {
  const activeMode = useActiveMode()
  const [documents, setDocuments] = useState<VidaDocument[]>([])
  const [total, setTotal] = useState(0)
  const [totalUnfiltered, setTotalUnfiltered] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [category, setCategory] = useState<DocumentCategory | null>(null)
  const [draft, setDraft] = useState('')
  const [query, setQuery] = useState('')
  const [userId, setUserId] = useState<string | null>(null)
  const [viewingDocument, setViewingDocument] = useState<VidaDocument | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)
  // ADR-025 §7: selección múltiple para descargar. Un `Set` y no un array
  // porque la operación que más se repite es "¿está este marcado?".
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [downloading, setDownloading] = useState(false)

  // Quién soy decide qué acciones tienen sentido: un documento que me
  // compartieron no es mío y el backend rechaza editarlo o borrarlo.
  useEffect(() => {
    let cancelled = false
    getCurrentUser()
      .then((user) => {
        if (!cancelled) setUserId(user.id)
      })
      .catch(() => {
        // Sin identidad se asume lo más restrictivo: no ofrecer acciones de
        // dueño es preferible a ofrecer las que van a fallar.
        if (!cancelled) setUserId(null)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const timer = setTimeout(() => setQuery(draft), 300)
    return () => clearTimeout(timer)
  }, [draft])

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const page = await listDocuments(category, activeMode, query)
      setDocuments(page.items)
      setTotal(page.totalElements)
      if (!category && !query.trim()) setTotalUnfiltered(page.totalElements)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los documentos.')
    } finally {
      setLoading(false)
    }
  }, [activeMode, category, query])

  useEffect(() => {
    void refresh()
  }, [refresh])

  /**
   * Los recuentos por categoría se calculan sobre la lista SIN filtrar por
   * categoría — si no, al elegir "Seguros" todas las demás tarjetas
   * marcarían cero. Por eso solo se recalculan cuando no hay filtro activo
   * y, mientras lo hay, se conserva el último recuento completo.
   */
  const [counts, setCounts] = useState<ReturnType<typeof categoryCounts>>([])
  useEffect(() => {
    if (!category && !query.trim()) setCounts(categoryCounts(documents, total))
  }, [documents, total, category, query])

  const empty = useMemo(
    () => emptyReason(totalUnfiltered, query, category ? DOCUMENT_CATEGORY_LABELS[category] : null),
    [totalUnfiltered, query, category],
  )

  function handleUploaded(document: VidaDocument) {
    setDocuments((current) => [document, ...current])
    setTotal((current) => current + 1)
    setTotalUnfiltered((current) => current + 1)
  }

  function handleChanged(updated: VidaDocument) {
    setDocuments((current) => current.map((document) => (document.id === updated.id ? updated : document)))
    setViewingDocument((current) => (current?.id === updated.id ? updated : current))
  }

  function toggleSelected(id: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (!next.delete(id)) next.add(id)
      return next
    })
  }

  /**
   * Descarga lo marcado, o TODO si no hay nada marcado.
   *
   * "Todos" no puede resolverse enviando los ids visibles: la lista está
   * paginada y solo se tienen los de la página cargada. Con `ids` vacío, el
   * servidor recorre todo el contexto.
   */
  async function handleDownload(all: boolean) {
    setDownloading(true)
    try {
      const ids = all ? [] : [...selected]
      if (!all && ids.length === 1) {
        // Un solo documento se descarga tal cual, con su nombre y su
        // extensión: meterlo en un ZIP de una entrada sería peor.
        const only = documents.find((entry) => entry.id === ids[0])
        if (only) {
          await downloadDocument(only)
          return
        }
      }
      await downloadDocuments(ids, activeMode)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron descargar los documentos.')
    } finally {
      setDownloading(false)
    }
  }

  async function handleDelete(id: string) {
    await deleteDocument(id)
    setDocuments((current) => current.filter((document) => document.id !== id))
    setTotal((current) => Math.max(0, current - 1))
    setTotalUnfiltered((current) => Math.max(0, current - 1))
  }

  function resetFilters() {
    setCategory(null)
    setDraft('')
    setQuery('')
  }

  const detailDocument = documents.find((document) => document.id === detailId)
  const hasMore = total > documents.length
  const isFiltering = category !== null || query.trim().length > 0

  return (
    <AppShell
      title="Documentos"
      subtitle="Lo importante, a mano y en su sitio."
      actionsHint={
        selected.size > 0
          ? `${selected.size} seleccionado${selected.size === 1 ? '' : 's'}`
          : undefined
      }
      actions={
        <>
          {/* §9 (ADR-025): las acciones de la sección viven arriba a la
              derecha, fuera del área con scroll. "Subir documento" es el
              mismo diálogo que estaba al final de la lista; descargar es la
              capacidad que pide §7. */}
          {selected.size > 0 && (
            <button
              type="button"
              className={styles.actionButton}
              disabled={downloading}
              onClick={() => void handleDownload(false)}
            >
              <Download width={15} height={15} aria-hidden="true" /> Descargar selección
            </button>
          )}
          <button
            type="button"
            className={styles.actionButton}
            disabled={downloading || total === 0}
            onClick={() => void handleDownload(true)}
          >
            <Download width={15} height={15} aria-hidden="true" /> Descargar todos
          </button>
          <UploadDocumentDialog onUploaded={handleUploaded} />
        </>
      }
    >
      {error && (
        <p role="alert" className={styles.error}>
          <strong>No se pudieron cargar los documentos.</strong>
          {error}
        </p>
      )}

      {!loading && !error && (totalUnfiltered > 0 || isFiltering) && (
        <>
          {/* ADR-022: la tarjeta ES el filtro. Antes era decorativa y el
              filtro real vivía en una fila de chips justo debajo. */}
          {/* ToggleButtonGroup para que las tarjetas tengan semántica real
              de grupo de selección única y navegación con flechas, como los
              chips de Inventario y Garantías. Antes eran `div` decorativos
              que ni siquiera se podían pulsar. */}
          <ToggleButtonGroup
            aria-label="Filtrar por categoría"
            selectionMode="single"
            disallowEmptySelection
            selectedKeys={new Set<Key>([category ?? 'all'])}
            onSelectionChange={(keys) => {
              const next = [...keys][0] as string | undefined
              setCategory(!next || next === 'all' ? null : (next as DocumentCategory))
            }}
            className={styles.categoryGrid}
          >
            {counts.map((entry) => (
              <ToggleButton key={entry.id} id={entry.id} className={styles.categoryCard}>
                <span className={styles.categoryName}>{entry.label}</span>
                <span className={styles.categoryCount}>{countLabel(entry.count)}</span>
              </ToggleButton>
            ))}
          </ToggleButtonGroup>

          <div className={styles.controls}>
            <div className={styles.search}>
              <Search className={styles.searchIcon} width={15} height={15} aria-hidden="true" />
              <input
                className={styles.searchInput}
                type="search"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Buscar documentos…"
                aria-label="Buscar documentos"
              />
            </div>
          </div>
        </>
      )}
      {loading && <p className={styles.hint}>Cargando…</p>}

      {!loading && !error && documents.length === 0 && (
        <div className={styles.empty}>
          <h3>{empty.title}</h3>
          <p>{empty.body}</p>
          <div className={styles.emptyAction}>
            {empty.canReset ? (
              <button type="button" className={styles.primaryButton} onClick={resetFilters}>
                Ver todos
              </button>
            ) : (
              <UploadDocumentDialog onUploaded={handleUploaded} />
            )}
          </div>
        </div>
      )}

      {!loading && !error && documents.length > 0 && (
        <div className={styles.rows}>
          {documents.map((document) => {
            const mine = isOwnedBy(document, userId)

            return (
              <article key={document.id} className={styles.row} data-state="plain" data-select>
                {/* La marca de visibilidad se convierte en la casilla de
                    selección: ocupa el mismo sitio, así que la fila no crece
                    ni se descoloca. El símbolo sigue visible al lado. */}
                <button
                  type="button"
                  className={styles.rowSelect}
                  aria-pressed={selected.has(document.id)}
                  aria-label={`Seleccionar ${document.name}`}
                  onClick={() => toggleSelected(document.id)}
                >
                  {selected.has(document.id) ? (
                    <CheckSquare width={16} height={16} aria-hidden="true" />
                  ) : (
                    <Square width={16} height={16} aria-hidden="true" />
                  )}
                </button>

                <span className={styles.rowMark} aria-hidden="true">
                  {VISIBILITY_MARKS[document.visibility]}
                </span>

                <div className={styles.rowBody}>
                  <p className={styles.rowName}>
                    {document.name}
                    <span className={styles.tag}>{DOCUMENT_CATEGORY_LABELS[document.category]}</span>
                    {/* ADR-022: quién puede ver esto es un hecho de
                        privacidad; no puede comunicarse solo con un icono. */}
                    <span className={styles.tag} data-t={VISIBILITY_TONES[document.visibility]}>
                      {VISIBILITY_LABELS[document.visibility]}
                    </span>
                    {!mine && (
                      <span className={styles.tag} data-t="link">
                        Compartido conmigo
                      </span>
                    )}
                  </p>

                  <p className={styles.rowMeta}>
                    {formatSize(document.sizeBytes)} · subido el {formatUploadedAt(document.createdAt)}
                    {document.sharedWithEmail ? ` · con ${document.sharedWithEmail}` : ''}
                  </p>
                </div>

                <span className={styles.rowWhen} />

                <div className={styles.rowActions}>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => setViewingDocument(document)}
                    aria-label={`Ver ${document.name}`}
                  >
                    <Eye width={15} height={15} />
                  </button>

                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => void downloadDocument(document)}
                    aria-label={`Descargar ${document.name}`}
                  >
                    <Download width={15} height={15} />
                  </button>

                  {/* Las acciones de dueño solo para el dueño: el backend
                      responde 404 al resto y ofrecerlas sería prometer algo
                      que va a fallar. */}
                  {mine && (
                    <>
                      <button
                        type="button"
                        className={`${styles.actionButton} ${styles.actionPrimary}`}
                        onClick={() => setDetailId(document.id)}
                      >
                        Renombrar
                      </button>

                      <ShareDocumentDialog document={document} onChanged={handleChanged} />

                      <SimpleDeleteConfirm
                        resourceLabel="documento"
                        itemName={document.name}
                        ariaLabel={`Eliminar ${document.name}`}
                        onConfirm={() => handleDelete(document.id)}
                      />
                    </>
                  )}
                </div>
              </article>
            )
          })}
        </div>
      )}

      {!loading && !error && hasMore && (
        <p className={styles.moreRow}>
          Mostrando {documents.length} de {total} documentos. Afina la búsqueda para encontrar el que falta.
        </p>
      )}

      {detailDocument && (
        <DocumentDetailDialog
          document={detailDocument}
          onClose={() => setDetailId(null)}
          onSaved={handleChanged}
        />
      )}

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
