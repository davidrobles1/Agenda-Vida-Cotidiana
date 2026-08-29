import { apiFetch } from '../../core/api/httpClient'
import { creationContext, withContext, type ModuleContext } from '../../core/user/moduleContext'

export type MaintenanceStatus = 'AL_DIA' | 'PROXIMO' | 'VENCIDO' | 'COMPLETADO'

export interface MaintenanceRecord {
  id: string
  ownerUserId: string
  item: string
  nextDueAt: string
  /** "¿Cada cuánto?" en meses (migración V24 / ADR-018). Ausente = una sola
      fecha, sin repetición: el calendario no proyecta ocurrencias futuras. */
  intervalMonths?: number
  status: MaintenanceStatus
  version: number
  createdAt: string
  updatedAt: string
}

interface MaintenanceRecordsPage {
  items: MaintenanceRecord[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listMaintenanceRecords(context?: ModuleContext | null): Promise<MaintenanceRecordsPage> {
  const response = await apiFetch(withContext('/maintenance-records?size=100', context))
  if (!response.ok) throw new Error(`GET /maintenance-records failed: ${response.status}`)
  return response.json()
}

export async function createMaintenanceRecord(
  item: string,
  nextDueAt: string,
  intervalMonths?: number,
  context?: ModuleContext | null,
): Promise<MaintenanceRecord> {
  const response = await apiFetch('/maintenance-records', {
    method: 'POST',
    body: JSON.stringify({ item, nextDueAt, intervalMonths, context: creationContext(context) }),
  })
  if (!response.ok) throw new Error(`POST /maintenance-records failed: ${response.status}`)
  return response.json()
}

export async function updateMaintenanceRecord(
  id: string,
  item: string,
  nextDueAt: string,
  version: number,
  intervalMonths?: number,
): Promise<MaintenanceRecord> {
  const response = await apiFetch(`/maintenance-records/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ item, nextDueAt, intervalMonths, version }),
  })
  if (!response.ok) throw new Error(`PATCH /maintenance-records/${id} failed: ${response.status}`)
  return response.json()
}

export async function completeMaintenanceRecord(id: string, version: number): Promise<MaintenanceRecord> {
  const response = await apiFetch(`/maintenance-records/${id}/complete`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /maintenance-records/${id}/complete failed: ${response.status}`)
  return response.json()
}

export async function deleteMaintenanceRecord(id: string): Promise<void> {
  const response = await apiFetch(`/maintenance-records/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /maintenance-records/${id} failed: ${response.status}`)
}
