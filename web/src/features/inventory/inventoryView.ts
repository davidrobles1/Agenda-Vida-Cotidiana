import type { Warranty } from '../warranties/api'
import { warrantyState, type WarrantyViewState } from '../warranties/warrantiesView'
import type { InventoryItem } from './api'

/**
 * ADR-022 — lógica de presentación de Inventario.
 *
 * Lo nuevo aquí es la relación con Garantías: el mismo objeto ("Refrigerador
 * Mabe") vivía en dos listas que se ignoraban, así que no se podía responder
 * "¿este artículo todavía tiene garantía?", que es justo lo que hace útil
 * tener las dos secciones.
 */

/** Etiqueta del grupo cuando el artículo no tiene ubicación registrada. */
export const NO_LOCATION = 'Sin ubicación'

export interface WarrantyLink {
  warranty: Warranty
  state: WarrantyViewState
}

/**
 * Índice artículo → garantía que lo cubre.
 *
 * El vínculo se guarda en `warranties.inventory_item_id` (ADR-022), así que
 * la pantalla de Inventario carga también las garantías del módulo activo y
 * las indexa aquí — el mismo patrón con el que `MaintenancePage` carga su
 * historial. Si un artículo acumula varias (garantía de fábrica y extensión
 * contratada aparte), gana la que vence más tarde: es la que sigue
 * cubriendo.
 */
export function indexWarrantiesByItem(warranties: Warranty[]): Map<string, WarrantyLink> {
  const index = new Map<string, WarrantyLink>()
  for (const warranty of warranties) {
    if (!warranty.inventoryItemId) continue
    const current = index.get(warranty.inventoryItemId)
    if (!current || warranty.expiresAt > current.warranty.expiresAt) {
      index.set(warranty.inventoryItemId, { warranty, state: warrantyState(warranty) })
    }
  }
  return index
}

export const WARRANTY_TAG_LABELS: Record<WarrantyViewState, string> = {
  ok: 'Con garantía',
  soon: 'Garantía por vencer',
  over: 'Garantía vencida',
  used: 'Garantía usada',
}

export interface InventoryGroup {
  id: string
  label: string
  items: InventoryItem[]
}

/**
 * Agrupa por ubicación. "¿Qué tengo en la cochera?" es la pregunta que se le
 * hace a un inventario y la lista plana anterior no podía responderla.
 * Los artículos sin ubicación van al final, no ocultos: que falte el dato es
 * justamente lo que hay que ver para completarlo.
 */
export function groupByLocation(items: InventoryItem[]): InventoryGroup[] {
  const buckets = new Map<string, InventoryItem[]>()
  for (const item of items) {
    const key = item.location?.trim() || NO_LOCATION
    const list = buckets.get(key) ?? []
    list.push(item)
    buckets.set(key, list)
  }
  return [...buckets.entries()]
    .sort(([a], [b]) => {
      if (a === NO_LOCATION) return 1
      if (b === NO_LOCATION) return -1
      return a.localeCompare(b, 'es')
    })
    .map(([label, list]) => ({
      id: label,
      label,
      items: list.sort((x, y) => x.name.localeCompare(y.name, 'es')),
    }))
}

export interface InventorySummary {
  totalCount: number
  locationCount: number
  withWarrantyCount: number
  withoutLocationCount: number
}

export function summarizeInventory(
  items: InventoryItem[],
  warrantyByItem: Map<string, WarrantyLink>,
): InventorySummary {
  const locations = new Set(items.map((item) => item.location?.trim()).filter((value): value is string => !!value))
  return {
    totalCount: items.length,
    locationCount: locations.size,
    // Solo cuenta la cobertura que sigue viva: una garantía vencida o ya
    // usada no responde "¿esto todavía tiene garantía?".
    withWarrantyCount: items.filter((item) => {
      const link = warrantyByItem.get(item.id)
      return link ? link.state === 'ok' || link.state === 'soon' : false
    }).length,
    withoutLocationCount: items.filter((item) => !item.location?.trim()).length,
  }
}

/**
 * Mensaje del estado vacío, que hasta ahora era siempre el mismo
 * ("Todavía no hay artículos aquí") tanto con el inventario vacío como con
 * un filtro sin resultados — y parecía que no había nada registrado.
 */
export function emptyReason(
  totalLoaded: number,
  query: string,
  categoryLabel: string | null,
): { title: string; body: string; canReset: boolean } {
  if (query.trim()) {
    return {
      title: `Ningún artículo coincide con “${query.trim()}”`,
      body: `Tienes ${totalLoaded} artículos registrados.`,
      canReset: true,
    }
  }
  if (categoryLabel) {
    return {
      title: `No tienes artículos en ${categoryLabel}`,
      body: `Tienes ${totalLoaded} artículos registrados en otras categorías.`,
      canReset: true,
    }
  }
  return {
    title: 'Aún no registras ningún artículo',
    body: 'Registra lo importante y sabrás qué tienes, dónde está y si aún tiene garantía.',
    canReset: false,
  }
}
