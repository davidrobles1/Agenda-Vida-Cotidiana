import { apiFetch } from '../../core/api/httpClient'
import { creationContext, withContext, type ModuleContext } from '../../core/user/moduleContext'

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
  /** ADR-019: módulo propietario del recurso. */
  context?: 'PERSONAL' | 'LABORAL'
  /** ADR-022: artículo del inventario que cubre esta garantía. */
  inventoryItemId?: string | null
}

export interface WarrantiesPage {
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

export async function listWarranties(context?: ModuleContext | null): Promise<WarrantiesPage> {
  // ADR-019: el filtro viaja al servidor; sin contexto se devuelve todo
  // (Calendario general).
  const response = await apiFetch(withContext('/warranties?size=100', context))
  if (!response.ok) throw new Error(`GET /warranties failed: ${response.status}`)
  return response.json()
}

export async function createWarranty(
  item: string,
  expiresAt: string,
  file: File,
  context?: ModuleContext | null,
): Promise<Warranty> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('item', item)
  formData.append('expiresAt', expiresAt)
  // ADR-019: el módulo queda fijado en el alta y no cambia después.
  formData.append('context', creationContext(context))
  const response = await apiFetch('/warranties', { method: 'POST', body: formData })
  if (!response.ok) {
    if (response.status === 400) throw new Error(await parseErrorMessage(response, 'Los datos de la garantía no son válidos.'))
    if (response.status === 413) throw new Error('El archivo es demasiado grande.')
    throw new Error(`POST /warranties failed: ${response.status}`)
  }
  return response.json()
}

/**
 * ADR-022: la edición existía en el backend y en este cliente desde el
 * principio, y **ninguna pantalla la llamaba**: corregir una fecha obligaba
 * a borrar la garantía y volver a subir el archivo. Ahora la usa
 * `WarrantyDetailDialog`.
 *
 * `linkInventoryItem` distingue "no tocar el enlace" de "cambiarlo": sin
 * ese indicador, mandar `null` sería indistinguible de omitirlo y
 * desenlazar un artículo sería imposible. Es la misma solución que
 * `clearInterval` en Mantenimiento (ADR-021(i)).
 */
export async function updateWarranty(
  id: string,
  item: string,
  expiresAt: string,
  version: number,
  inventoryItemId?: string | null,
  linkInventoryItem = false,
): Promise<Warranty> {
  const response = await apiFetch(`/warranties/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({
      item,
      expiresAt,
      version,
      ...(linkInventoryItem ? { inventoryItemId: inventoryItemId ?? null, linkInventoryItem: true } : {}),
    }),
  })
  if (!response.ok) {
    if (response.status === 409) throw new Error('La garantía cambió mientras la editabas. Vuelve a abrirla.')
    throw new Error(`PATCH /warranties/${id} failed: ${response.status}`)
  }
  return response.json()
}

/**
 * ADR-022: en la interfaz esto se llama **"Marcar como usada"**. El valor
 * del contrato sigue siendo `COMPLETADO` y el endpoint sigue siendo
 * `/complete`: cambiar el contrato obligaría a tocar `dateAlerts`, los
 * tests y la especificación OpenAPI sin ganar nada. Lo que cambia es la
 * palabra que ve el usuario — una garantía no se "completa", se usa.
 */
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
