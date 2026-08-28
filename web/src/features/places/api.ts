import { apiFetch } from '../../core/api/httpClient'

/**
 * ADR-016 Fase 3e3/FR-033 (Módulo Laboral). Catálogo de ubicaciones
 * reutilizables. Mismo patrón que people/api.ts — owner-only.
 *
 * No existe `REMINDER.place_id` (fuera de alcance explícito de FR-033):
 * elegir un Lugar al crear una Tarea solo copia su texto al campo
 * `location` ya existente del REMINDER (FR-024).
 */
export interface Place {
  id: string
  ownerUserId: string
  name: string
  address?: string
  personId?: string
  version: number
  createdAt: string
  updatedAt: string
}

interface PlacesPage {
  items: Place[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listPlaces(): Promise<PlacesPage> {
  const response = await apiFetch('/places?size=100')
  if (!response.ok) throw new Error(`GET /places failed: ${response.status}`)
  return response.json()
}

export interface CreatePlaceInput {
  name: string
  address?: string
  personId?: string
}

export async function createPlace(input: CreatePlaceInput): Promise<Place> {
  const body: Record<string, unknown> = { name: input.name }
  if (input.address) body.address = input.address
  if (input.personId) body.personId = input.personId
  const response = await apiFetch('/places', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /places failed: ${response.status}`)
  return response.json()
}

export async function deletePlace(id: string): Promise<void> {
  const response = await apiFetch(`/places/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /places/${id} failed: ${response.status}`)
}

/**
 * UC-26: el texto que se copia al campo `location` de la Tarea. Se prefiere
 * la dirección cuando existe (es lo accionable para llegar al sitio) y se
 * cae al nombre cuando el Lugar no tiene dirección registrada.
 */
export function placeLocationText(place: Place): string {
  return place.address?.trim() ? place.address : place.name
}
