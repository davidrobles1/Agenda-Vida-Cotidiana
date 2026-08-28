import { apiFetch } from '../../core/api/httpClient'

/**
 * ADR-016 Fase 3e4/FR-034 (Módulo Laboral). Mismo patrón que people/api.ts —
 * owner-only.
 *
 * Un Recurso guarda metadatos y una referencia de texto, NUNCA un archivo:
 * los documentos reales siguen siendo responsabilidad de `features/documents`
 * (FR-030). `reference` es un único campo de texto libre (decisión del
 * Product Owner) — una URL, la ruta de una carpeta compartida, o cualquier
 * puntero textual.
 */
export type ResourceType = 'DOCUMENTO' | 'ENLACE' | 'PLANTILLA' | 'MANUAL' | 'HERRAMIENTA' | 'OTRO'

export interface Resource {
  id: string
  ownerUserId: string
  name: string
  type: ResourceType
  reference?: string
  description?: string
  personId?: string
  projectId?: string
  version: number
  createdAt: string
  updatedAt: string
}

interface ResourcesPage {
  items: Resource[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const RESOURCE_TYPE_LABELS: Record<ResourceType, string> = {
  DOCUMENTO: 'Documento',
  ENLACE: 'Enlace',
  PLANTILLA: 'Plantilla',
  MANUAL: 'Manual',
  HERRAMIENTA: 'Herramienta',
  OTRO: 'Otro',
}

export async function listResources(): Promise<ResourcesPage> {
  const response = await apiFetch('/resources?size=100')
  if (!response.ok) throw new Error(`GET /resources failed: ${response.status}`)
  return response.json()
}

export interface CreateResourceInput {
  name: string
  type: ResourceType
  reference?: string
  description?: string
  personId?: string
  projectId?: string
}

export async function createResource(input: CreateResourceInput): Promise<Resource> {
  const body: Record<string, unknown> = { name: input.name, type: input.type }
  if (input.reference) body.reference = input.reference
  if (input.description) body.description = input.description
  if (input.personId) body.personId = input.personId
  if (input.projectId) body.projectId = input.projectId
  const response = await apiFetch('/resources', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /resources failed: ${response.status}`)
  return response.json()
}

export async function deleteResource(id: string): Promise<void> {
  const response = await apiFetch(`/resources/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /resources/${id} failed: ${response.status}`)
}
