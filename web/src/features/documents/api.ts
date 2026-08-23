import { apiFetch } from '../../core/api/httpClient'

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
  version: number
  createdAt: string
  updatedAt: string
}

interface DocumentsPageResponse {
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

export async function listDocuments(category?: DocumentCategory): Promise<DocumentsPageResponse> {
  const query = category ? `?category=${category}&size=100` : '?size=100'
  const response = await apiFetch(`/documents${query}`)
  if (!response.ok) throw new Error(`GET /documents failed: ${response.status}`)
  return response.json()
}

export async function uploadDocument(file: File, name: string, category: DocumentCategory): Promise<VidaDocument> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('name', name)
  formData.append('category', category)
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
