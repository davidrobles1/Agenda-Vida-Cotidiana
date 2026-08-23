import { apiFetch } from '../../core/api/httpClient'

/**
 * BE-037/WEB-009. `status` matches the real backend contract
 * (Documentacion/openapi/openapi.yaml Warranty schema): VIGENTE/POR_VENCER/
 * VENCIDA are derived server-side from expiresAt vs "now"; COMPLETADO is
 * the explicit user-completed state — see backend's WarrantyResponse for
 * the exact computation.
 *
 * Pedido explícito del usuario (2026-08-21): "al registrar una garantía
 * subir el archivo... en formato imagen o pdf" — POST /warranties pasó de
 * JSON a multipart (mismo shape que documents/api.ts's uploadDocument),
 * `documentContentType` refleja si tiene archivo adjunto y de qué tipo.
 */
export type WarrantyStatus = 'VIGENTE' | 'POR_VENCER' | 'VENCIDA' | 'COMPLETADO'

export interface Warranty {
  id: string
  ownerUserId: string
  item: string
  expiresAt: string
  status: WarrantyStatus
  version: number
  createdAt: string
  updatedAt: string
  documentContentType?: string
}

interface WarrantiesPage {
  items: Warranty[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function parseErrorMessage(response: Response, fallback: string): Promise<string> {
  const body = await response.json().catch(() => null)
  return body?.message ?? fallback
}

export async function listWarranties(): Promise<WarrantiesPage> {
  const response = await apiFetch('/warranties?size=100')
  if (!response.ok) throw new Error(`GET /warranties failed: ${response.status}`)
  return response.json()
}

export async function createWarranty(item: string, expiresAt: string, file: File): Promise<Warranty> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('item', item)
  formData.append('expiresAt', expiresAt)
  const response = await apiFetch('/warranties', { method: 'POST', body: formData })
  if (!response.ok) {
    if (response.status === 400) throw new Error(await parseErrorMessage(response, 'Los datos de la garantía no son válidos.'))
    if (response.status === 413) throw new Error('El archivo es demasiado grande.')
    throw new Error(`POST /warranties failed: ${response.status}`)
  }
  return response.json()
}

export async function updateWarranty(id: string, item: string, expiresAt: string, version: number): Promise<Warranty> {
  const response = await apiFetch(`/warranties/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ item, expiresAt, version }),
  })
  if (!response.ok) throw new Error(`PATCH /warranties/${id} failed: ${response.status}`)
  return response.json()
}

export async function completeWarranty(id: string, version: number): Promise<Warranty> {
  const response = await apiFetch(`/warranties/${id}/complete`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /warranties/${id}/complete failed: ${response.status}`)
  return response.json()
}

export async function deleteWarranty(id: string): Promise<void> {
  const response = await apiFetch(`/warranties/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /warranties/${id} failed: ${response.status}`)
}
