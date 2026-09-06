import type { Warranty, WarrantyStatus } from './api'

/**
 * ADR-022 — lógica de presentación de Garantías, separada de la pantalla
 * por el mismo motivo que `maintenanceView.ts` y `paymentsView.ts`: son
 * reglas con casos límite (fechas, umbrales, agrupación) y conviene poder
 * leerlas y probarlas sin montar un árbol de React.
 */

/**
 * Fecha de calendario a la hora local del mediodía.
 *
 * REGLA DEL PROYECTO (ADR-021(j)): el backend guarda las fechas de
 * calendario a medianoche UTC y `new Date('YYYY-MM-DD')` las interpreta
 * como UTC, así que en UTC−6 se pintaban **el día anterior**. Garantías
 * arrastraba además su propio problema: mostraba la fecha en crudo
 * (`iso.slice(0, 10)` → "2027-03-12"), sin formato y sin esta corrección.
 */
export function toLocalDate(iso: string): Date {
  return new Date(`${iso.slice(0, 10)}T12:00:00`)
}

export function formatDate(iso: string): string {
  const date = toLocalDate(iso)
  return date.toLocaleDateString('es-MX', {
    day: 'numeric',
    month: 'short',
    // El año solo cuando no es el corriente, misma regla que Pagos
    // (ADR-020(8)): una garantía a tres años vista no puede mostrarse
    // igual que una de este año.
    ...(date.getFullYear() === new Date().getFullYear() ? {} : { year: 'numeric' }),
  })
}

/** Siempre con año — para textos donde la fecha va sola y sin contexto. */
export function formatFullDate(iso: string): string {
  return toLocalDate(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function daysUntil(iso: string, today = new Date()): number {
  const start = new Date(today)
  start.setHours(12, 0, 0, 0)
  return Math.round((toLocalDate(iso).getTime() - start.getTime()) / 86_400_000)
}

/**
 * Estado de presentación. Deriva del `status` que calcula el servidor
 * (VIGENTE/POR_VENCER/VENCIDA/COMPLETADO), no lo recalcula: el umbral de
 * "por vencer" son 30 días y vive en `WarrantyResponse`, que es su único
 * dueño. Aquí solo se traduce a las cuatro casillas visuales del prototipo.
 */
export type WarrantyViewState = 'ok' | 'soon' | 'over' | 'used'

export function warrantyState(warranty: Warranty): WarrantyViewState {
  switch (warranty.status) {
    case 'COMPLETADO':
      return 'used'
    case 'VENCIDA':
      return 'over'
    case 'POR_VENCER':
      return 'soon'
    default:
      return 'ok'
  }
}

/**
 * ADR-022: "Completar" una garantía no significaba nada — una garantía no
 * se completa, se **usa** (se hizo válida la reclamación) o vence sola. El
 * valor del contrato sigue siendo `COMPLETADO`; lo que cambia es la palabra
 * que lee el usuario. Aprobado por el Product Owner el 2026-09-01.
 */
export const STATE_LABELS: Record<WarrantyViewState, string> = {
  ok: 'Vigente',
  soon: 'Por vencer',
  over: 'Vencida',
  used: 'Usada',
}

export const STATE_MARKS: Record<WarrantyViewState, string> = {
  ok: '✓',
  soon: '!',
  over: '✕',
  used: '—',
}

/** Etiqueta de cuándo. Relativa cerca y absoluta lejos, igual que Pagos. */
export function whenLabel(warranty: Warranty, state: WarrantyViewState, today = new Date()): string {
  if (state === 'used') return 'usada'
  const days = daysUntil(warranty.expiresAt, today)
  if (days < -1) return `hace ${Math.abs(days)} días`
  if (days === -1) return 'ayer'
  if (days === 0) return 'hoy'
  if (days === 1) return 'mañana'
  if (days <= 30) return `en ${days} días`
  return formatDate(warranty.expiresAt)
}

export interface WarrantyGroup {
  id: WarrantyViewState
  label: string
  items: Warranty[]
}

/** Orden del prototipo: lo urgente arriba, lo cerrado al final. */
export function groupWarranties(
  warranties: Warranty[],
  stateOf: (warranty: Warranty) => WarrantyViewState,
): WarrantyGroup[] {
  const order: Array<{ id: WarrantyViewState; label: string }> = [
    { id: 'over', label: 'Vencidas' },
    { id: 'soon', label: 'Por vencer' },
    { id: 'ok', label: 'Vigentes' },
    { id: 'used', label: 'Ya usadas' },
  ]
  return order
    .map(({ id, label }) => ({
      id,
      label,
      items: warranties
        .filter((warranty) => stateOf(warranty) === id)
        .sort((a, b) => a.expiresAt.localeCompare(b.expiresAt)),
    }))
    .filter((group) => group.items.length > 0)
}

export interface WarrantySummary {
  /** La que vence antes de entre las que siguen contando. */
  next: Warranty | null
  soonCount: number
  activeCount: number
  totalCount: number
}

export function summarizeWarranties(
  warranties: Warranty[],
  stateOf: (warranty: Warranty) => WarrantyViewState,
): WarrantySummary {
  // Una garantía usada o ya vencida no es "lo siguiente en vencer": no hay
  // nada que anticipar en ella.
  const pending = warranties
    .filter((warranty) => ['ok', 'soon'].includes(stateOf(warranty)))
    .sort((a, b) => a.expiresAt.localeCompare(b.expiresAt))

  return {
    next: pending[0] ?? null,
    soonCount: warranties.filter((warranty) => stateOf(warranty) === 'soon').length,
    activeCount: pending.length,
    totalCount: warranties.length,
  }
}

export const WARRANTY_FILTERS: Array<{ id: 'all' | WarrantyViewState; label: string }> = [
  { id: 'all', label: 'Todas' },
  { id: 'ok', label: 'Vigentes' },
  { id: 'soon', label: 'Por vencer' },
  { id: 'over', label: 'Vencidas' },
  { id: 'used', label: 'Usadas' },
]

/** Nombre legible del tipo de archivo adjunto, para la línea de detalle. */
export function fileLabel(contentType: string | undefined): string {
  if (!contentType) return 'Sin comprobante'
  if (contentType === 'application/pdf') return 'Comprobante PDF'
  if (contentType.startsWith('image/')) return 'Comprobante (imagen)'
  return 'Comprobante adjunto'
}

export type { WarrantyStatus }
