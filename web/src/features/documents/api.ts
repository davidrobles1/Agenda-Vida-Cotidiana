import { apiFetch } from '../../core/api/httpClient'

import { creationContext, withContext, type ModuleContext } from '../../core/user/moduleContext'

/** Pedido explícito del usuario (2026-08-22): las 5 categorías tal cual,
    mismo enum que el backend (document.domain.DocumentCategory). */
export type DocumentCategory = 'IDENTIFICACION' | 'COMPROBANTES' | 'SEGUROS' | 'CONTRATOS' | 'OTROS'

export const DOCUMENT_CATEGORY_LABELS: Record<DocumentCategory, string> = {
  IDENTIFICACION: 'Identificación',
  COMPROBANTES: 'Comprobantes',
  SEGUROS: 'Seguros',
  CONTRATOS: 'Contratos',
  OTROS: 'Otros',
}

export const DOCUMENT_CATEGORIES: DocumentCategory[] = ['IDENTIFICACION', 'COMPROBANTES', 'SEGUROS', 'CONTRATOS', 'OTROS']

export type DocumentVisibility = 'PRIVATE' | 'SHARED' | 'FAMILY_PUBLIC'

export interface VidaDocument {
  id: string
  ownerUserId: string
  name: string
  category: DocumentCategory
  contentType: string
  sizeBytes: number
  visibility: DocumentVisibility
  sharedWithEmail?: string
  /** ADR-016 Fase 3b/FR-030 (candidato V4, Módulo Laboral) — vínculo opcional a Persona/Proyecto. El resto de Documentos lo ignora sin problema. */
  personId?: string
  projectId?: string
  /** ADR-022: módulo propietario (el del dueño, no el de quien lo recibe
      compartido). */
  context?: ModuleContext
  version: number
  createdAt: string
  updatedAt: string
}

export interface DocumentsPageResponse {
  items: VidaDocument[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function parseErrorMessage(response: Response, fallback: string): Promise<string> {
  const body = await response.json().catch(() => null)
  return body?.message ?? fallback
}

/**
 * ADR-022: contexto, categoría y búsqueda viajan al SERVIDOR.
 *
 * El parámetro `category` ya existía en esta función y en el endpoint, y
 * **la pantalla no lo usaba**: filtraba en memoria sobre la página cargada,
 * así que con más documentos que el tamaño de página el filtro ocultaba
 * resultados en silencio. `context` y `q` se añadieron en esta fase.
 */
export async function listDocuments(
  category?: DocumentCategory | null,
  context?: ModuleContext | null,
  query?: string,
  size = 100,
): Promise<DocumentsPageResponse> {
  const params = new URLSearchParams({ size: String(size) })
  if (category) params.set('category', category)
  if (query && query.trim()) params.set('q', query.trim())
  const response = await apiFetch(withContext(`/documents?${params}`, context))
  if (!response.ok) throw new Error(`GET /documents failed: ${response.status}`)
  return response.json()
}

/**
 * ADR-022: renombrar y recategorizar. `PATCH /documents/{id}` existía en el
 * backend desde el principio y **este cliente ni siquiera tenía la
 * función**: un documento mal nombrado o mal clasificado solo se podía
 * borrar y volver a subir, perdiendo su historial de compartición.
 */
export async function updateDocument(
  id: string,
  name: string,
  category: DocumentCategory,
  version: number,
): Promise<VidaDocument> {
  const response = await apiFetch(`/documents/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ name, category, version }),
  })
  if (!response.ok) {
    if (response.status === 409) throw new Error('El documento cambió mientras lo editabas. Vuelve a abrirlo.')
    if (response.status === 400) throw new Error(await parseErrorMessage(response, 'Los datos del documento no son válidos.'))
    throw new Error(`PATCH /documents/${id} failed: ${response.status}`)
  }
  return response.json()
}

export async function uploadDocument(
  file: File,
  name: string,
  category: DocumentCategory,
  context?: ModuleContext | null,
): Promise<VidaDocument> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('name', name)
  formData.append('category', category)
  // ADR-022: el módulo queda fijado al subir y no cambia después.
  formData.append('context', creationContext(context))
  const response = await apiFetch('/documents', { method: 'POST', body: formData })
  if (!response.ok) {
    if (response.status === 400) throw new Error(await parseErrorMessage(response, 'El archivo no es válido.'))
    if (response.status === 413) throw new Error('El archivo es demasiado grande.')
    throw new Error(`POST /documents failed: ${response.status}`)
  }
  return response.json()
}

export async function shareDocument(id: string, email: string, version: number): Promise<VidaDocument> {
  const response = await apiFetch(`/documents/${id}/share`, {
    method: 'POST',
    body: JSON.stringify({ email, version }),
  })
  if (!response.ok) {
    if (response.status === 400) throw new Error(await parseErrorMessage(response, 'El correo no es válido.'))
    throw new Error(`POST /documents/${id}/share failed: ${response.status}`)
  }
  return response.json()
}

export async function makeDocumentPublic(id: string, version: number): Promise<VidaDocument> {
  const response = await apiFetch(`/documents/${id}/make-public`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /documents/${id}/make-public failed: ${response.status}`)
  return response.json()
}

export async function makeDocumentPrivate(id: string, version: number): Promise<VidaDocument> {
  const response = await apiFetch(`/documents/${id}/make-private`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /documents/${id}/make-private failed: ${response.status}`)
  return response.json()
}

export async function deleteDocument(id: string): Promise<void> {
  const response = await apiFetch(`/documents/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /documents/${id} failed: ${response.status}`)
}

/**
 * ADR-025 §7 — descarga.
 *
 * REUTILIZA EL ALMACENAMIENTO EXISTENTE: los bytes de un documento salen de
 * `GET /documents/{id}/content`, el mismo endpoint que ya alimentaba la vista
 * previa desde que existe el módulo. No se crea una segunda infraestructura de
 * archivos.
 */

/** Lanza la descarga en el navegador a partir de un blob ya obtenido. */
function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  // Sin esto el blob se queda en memoria hasta que se recarga la pestaña.
  URL.revokeObjectURL(url)
}

const EXTENSIONS: Record<string, string> = {
  'image/png': '.png',
  'image/jpeg': '.jpg',
  'image/webp': '.webp',
  'image/gif': '.gif',
  'application/pdf': '.pdf',
}

/**
 * Descarga UN documento con su nombre y su extensión.
 *
 * Pasa por `apiFetch` y no por un `<a href>` directo porque la API va
 * autenticada con un token en cabecera: un enlace normal no lo lleva y
 * devolvería 401.
 */
export async function downloadDocument(doc: VidaDocument): Promise<void> {
  const response = await apiFetch(`/documents/${doc.id}/content`)
  if (!response.ok) throw new Error('No se pudo descargar el documento.')
  const extension = EXTENSIONS[doc.contentType] ?? ''
  const name = doc.name.toLowerCase().endsWith(extension) ? doc.name : `${doc.name}${extension}`
  saveBlob(await response.blob(), name)
}

/**
 * Descarga varios documentos —o todos— en un único ZIP.
 *
 * Un ZIP y no varias descargas seguidas porque el navegador bloquea las
 * descargas múltiples automáticas: a partir de la segunda, el usuario tendría
 * que autorizar cada una. `ids` vacío significa "todos los del contexto", que
 * no puede resolverse en el cliente porque la lista está paginada.
 */
export async function downloadDocuments(ids: string[], context?: string | null): Promise<void> {
  const response = await apiFetch('/documents/download', {
    method: 'POST',
    body: JSON.stringify({ ids, context: context ?? undefined }),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.detail ?? 'No se pudieron descargar los documentos.')
  }
  saveBlob(await response.blob(), 'documentos.zip')
}
