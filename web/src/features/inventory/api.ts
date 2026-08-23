import { apiFetch } from '../../core/api/httpClient'

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
  version: number
  createdAt: string
  updatedAt: string
}

interface InventoryItemsPageResponse {
  items: InventoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listInventoryItems(): Promise<InventoryItemsPageResponse> {
  const response = await apiFetch('/inventory-items?size=100')
  if (!response.ok) throw new Error(`GET /inventory-items failed: ${response.status}`)
  return response.json()
}

export async function createInventoryItem(name: string, category: InventoryCategory, location?: string): Promise<InventoryItem> {
  const response = await apiFetch('/inventory-items', {
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
