/**
 * UX-006: mock data for the 6 scaffolding-only modules (Documentos,
 * Inventario, Garantías, Mantenimiento, Suscripciones, Familia) — zero
 * business logic, zero endpoints. Values match the reference images'
 * sample data (docs/Ejemplo Web vida Cotidiana .png). Kept in its own
 * module and imported by each page's hook, never inlined into a component —
 * mirrors android/.../core/mock/MockData.kt.
 */

export interface MockDocumentCategory {
  id: string
  name: string
  count: number
}

export interface MockDocument {
  id: string
  name: string
  category: string
  uploadedAt: string
}

export const documentCategories: MockDocumentCategory[] = [
  { id: 'identificacion', name: 'Identificación', count: 5 },
  { id: 'comprobantes', name: 'Comprobantes', count: 8 },
  { id: 'seguros', name: 'Seguros', count: 4 },
  { id: 'contratos', name: 'Contratos', count: 3 },
  { id: 'otros', name: 'Otros', count: 4 },
]

export const documents: MockDocument[] = [
  { id: 'doc-1', name: 'INE_David.pdf', category: 'Identificación', uploadedAt: 'Hace 2 horas' },
  { id: 'doc-2', name: 'Factura Netflix.pdf', category: 'Comprobantes', uploadedAt: 'Hace 5 horas' },
  { id: 'doc-3', name: 'Póliza auto 2026.pdf', category: 'Seguros', uploadedAt: 'Hace 1 día' },
]

export type WarrantyStatus = 'VIGENTE' | 'POR_VENCER' | 'VENCIDA'

export interface MockWarranty {
  id: string
  item: string
  expiresAt: string
  status: WarrantyStatus
}

export const warranties: MockWarranty[] = [
  { id: 'w-1', item: 'Laptop Dell XPS 13', expiresAt: '2027-03-10', status: 'VIGENTE' },
  { id: 'w-2', item: 'Seguro de auto', expiresAt: '2026-05-12', status: 'POR_VENCER' },
  { id: 'w-3', item: 'Lavadora Samsung', expiresAt: '2026-01-20', status: 'VENCIDA' },
]

export interface MockInventoryItem {
  id: string
  name: string
  category: string
  location: string
}

export const inventoryCategories = ['Electrónicos', 'Hogar', 'Vehículos']

export const inventoryItems: MockInventoryItem[] = [
  { id: 'inv-1', name: 'Laptop Dell XPS 13', category: 'Electrónicos', location: 'En uso' },
  { id: 'inv-2', name: 'Lavadora Samsung', category: 'Hogar', location: 'En uso' },
  { id: 'inv-3', name: 'Sofá Sala', category: 'Hogar', location: 'En uso' },
  { id: 'inv-4', name: 'Auto Toyota Corolla', category: 'Vehículos', location: 'En uso' },
]

export type MaintenanceStatus = 'AL_DIA' | 'PROXIMO' | 'VENCIDO'

export interface MockMaintenanceRecord {
  id: string
  item: string
  nextDueAt: string
  status: MaintenanceStatus
}

export const maintenanceRecords: MockMaintenanceRecord[] = [
  { id: 'm-1', item: 'Cambio de aceite — Toyota Corolla', nextDueAt: '2026-09-01', status: 'AL_DIA' },
  { id: 'm-2', item: 'Servicio de auto', nextDueAt: '2026-05-16', status: 'PROXIMO' },
  { id: 'm-3', item: 'Revisión de calentador', nextDueAt: '2026-04-01', status: 'VENCIDO' },
]

export interface MockSubscription {
  id: string
  name: string
  price: string
  cycle: string
}

export const subscriptions: MockSubscription[] = [
  { id: 's-1', name: 'Netflix', price: '$219 MXN', cycle: 'Mensual' },
  { id: 's-2', name: 'Spotify', price: '$115 MXN', cycle: 'Mensual' },
  { id: 's-3', name: 'iCloud+', price: '$29 MXN', cycle: 'Mensual' },
]

export interface MockFamilyMember {
  id: string
  name: string
  relationship: string
}

export const familyMembers: MockFamilyMember[] = [
  { id: 'fam-1', name: 'Ana García', relationship: 'Esposa' },
  { id: 'fam-2', name: 'María García', relationship: 'Hija' },
  { id: 'fam-3', name: 'Carlos García', relationship: 'Hijo' },
]

/**
 * UX-009: Home's "Resumen de actividades" — mock only, on the user's
 * explicit instruction ("genera el mock, luego implementamos la lógica").
 * There is no activity-feed endpoint today (the backend's audit log,
 * BE-029, is server-side/security-only, never exposed to the client) — this
 * is presentation scaffolding for a feature that doesn't exist yet, same
 * spirit as the other 6 mock modules. Every consumer must show these as
 * clearly simulated (subtitle ends in "· simulado"), never indistinguishable
 * from a real event.
 */
export type MockActivityKind = 'task' | 'share' | 'warranty' | 'document'

export interface MockActivity {
  id: string
  kind: MockActivityKind
  text: string
  timeLabel: string
}

export const homeActivities: MockActivity[] = [
  { id: 'act-1', kind: 'task', text: 'Completaste "Pagar servicio de luz"', timeLabel: 'Hace 2 horas' },
  { id: 'act-2', kind: 'share', text: 'Compartiste "Revisión del auto" con Ana', timeLabel: 'Ayer' },
  { id: 'act-3', kind: 'warranty', text: 'Agregaste la garantía de "Lavadora Samsung"', timeLabel: 'Hace 3 días' },
]

/**
 * UX-009: Home's third metric card, "Próximos eventos" — mock (same
 * instruction as above). "Evento" isn't a real concept in the API today
 * (reminders are "tareas", not "eventos") — this stays a static mock number
 * until a real events concept exists, not silently reinterpreted as
 * upcoming reminders.
 */
export const homeUpcomingEventsCount = 2
