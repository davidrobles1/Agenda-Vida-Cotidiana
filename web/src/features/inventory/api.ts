import { apiFetch } from '../../core/api/httpClient'

import { creationContext, withContext, type ModuleContext } from '../../core/user/moduleContext'

export type InventoryCategory = 'ELECTRONICOS' | 'HOGAR' | 'VEHICULOS'

export const INVENTORY_CATEGORY_LABELS: Record<InventoryCategory, string> = {
  ELECTRONICOS: 'Electrónicos',
  HOGAR: 'Hogar',
  VEHICULOS: 'Vehículos',
}

export const INVENTORY_CATEGORIES: InventoryCategory[] = ['ELECTRONICOS', 'HOGAR', 'VEHICULOS']

export interface InventoryItem {
  id: string
  ownerUserId: string
  name: string
  category: InventoryCategory
  location?: string
  /** ADR-022: módulo propietario del recurso. */
  context?: ModuleContext
  version: number
  createdAt: string
  updatedAt: string
}

export interface InventoryItemsPageResponse {
  items: InventoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * ADR-022: contexto, categoría y búsqueda viajan al SERVIDOR.
 *
 * Antes esta función no aceptaba nada y la pantalla filtraba en memoria
 * sobre la página ya cargada: con más de 100 artículos, elegir una
 * categoría o buscar ocultaba resultados sin decirlo. El endpoint ya
 * aceptaba `category`; `context` y `q` se añadieron en esta fase.
 */
export async function listInventoryItems(
  context?: ModuleContext | null,
  category?: InventoryCategory | null,
  query?: string,
  size = 100,
): Promise<InventoryItemsPageResponse> {
  const params = new URLSearchParams({ size: String(size) })
  if (category) params.set('category', category)
  if (query && query.trim()) params.set('q', query.trim())
  const response = await apiFetch(withContext(`/inventory-items?${params}`, context))
  if (!response.ok) throw new Error(`GET /inventory-items failed: ${response.status}`)
  return response.json()
}

export async function createInventoryItem(
  name: string,
  category: InventoryCategory,
  location?: string,
  context?: ModuleContext | null,
): Promise<InventoryItem> {
  // ADR-022: el módulo queda fijado en el alta y no cambia después.
  const response = await apiFetch(withContext('/inventory-items', creationContext(context)), {
    method: 'POST',
    body: JSON.stringify({ name, category, location: location || undefined }),
  })
  if (!response.ok) throw new Error(`POST /inventory-items failed: ${response.status}`)
  return response.json()
}

export async function updateInventoryItem(
  id: string,
  name: string,
  category: InventoryCategory,
  location: string | undefined,
  version: number,
): Promise<InventoryItem> {
  const response = await apiFetch(`/inventory-items/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ name, category, location: location || undefined, version }),
  })
  if (!response.ok) throw new Error(`PATCH /inventory-items/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteInventoryItem(id: string): Promise<void> {
  const response = await apiFetch(`/inventory-items/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /inventory-items/${id} failed: ${response.status}`)
}
