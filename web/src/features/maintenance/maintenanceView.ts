import type { MaintenanceLogEntry, MaintenanceRecord } from './api'

/**
 * ADR-021: lógica de presentación de Mantenimiento, sin dependencias de
 * React. Mismo papel —y misma estructura— que `paymentsView.ts` en Pagos:
 * el estado, la agrupación y el resumen son reglas de producto y conviene
 * poder leerlas de un tirón.
 */

export type MaintenanceState = 'overdue' | 'soon' | 'ok' | 'done' | 'closed'

/**
 * UNA sola definición de "próximo" (ADR-021). Antes eran dos: el backend
 * decía 30 días y el calendario avisaba a 7, así que el mismo registro
 * estaba "PROXIMO" en la lista y no en el calendario. Este valor coincide
 * con `PROXIMO_THRESHOLD_DAYS` del backend y con `DUE_SOON_DAYS` de Pagos.
 */
export const DUE_SOON_DAYS = 7

/**
 * DEFECTO CORREGIDO (2026-08-29, encontrado validando con un registro
 * real): todas las fechas se mostraban UN DÍA ANTES en zonas al oeste de
 * UTC. Un mantenimiento del 23 de septiembre aparecía como "22 sep", y uno
 * completado hoy 31 de agosto decía "Última vez: 30 ago".
 *
 * Causa: `new Date('2026-08-31')` interpreta la cadena como UTC, y el
 * backend guarda estas fechas a medianoche UTC. Al formatearlas en local
 * (CST = UTC-6) caían en el día anterior.
 *
 * Se toman los 10 primeros caracteres —el día tal como lo eligió el
 * usuario, sirve igual para `YYYY-MM-DD` que para un ISO completo— y se
 * construye el mediodía LOCAL, que ninguna zona horaria puede desplazar de
 * día. Es el mismo criterio que ya usaban el calendario y las notas del día
 * (`new Date(`${key}T12:00:00`)`), y por eso el calendario sí mostraba bien
 * las fechas mientras estas listas no.
 */
export function toLocalDate(iso: string): Date {
  return new Date(`${iso.slice(0, 10)}T12:00:00`)
}

function midnight(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function daysUntil(iso: string, today = new Date()): number {
  return Math.round((midnight(toLocalDate(iso)).getTime() - midnight(today).getTime()) / 86400000)
}

/** ¿Se completó hoy? Es lo que distingue "hecho" de "al día": ambos están
    activos, pero uno acaba de atenderse y merece decirlo. */
export function completedToday(log: MaintenanceLogEntry[], today = new Date()): boolean {
  // Comparación en texto sobre la fecha LOCAL de hoy: `toISOString()` la
  // convertiría a UTC y, de noche, daría el día siguiente.
  const key = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  return log.some((entry) => entry.completedDate.slice(0, 10) === key)
}

export function maintenanceState(
  record: MaintenanceRecord,
  log: MaintenanceLogEntry[],
  today = new Date(),
): MaintenanceState {
  // Un puntual completado termina de verdad: es la mitad de la regla que el
  // Product Owner aprobó (sin periodicidad, no vuelve).
  if (record.status === 'COMPLETADO') return 'closed'
  if (completedToday(log, today)) return 'done'
  const days = daysUntil(record.nextDueAt, today)
  if (days < 0) return 'overdue'
  if (days <= DUE_SOON_DAYS) return 'soon'
  return 'ok'
}

export const STATE_LABELS: Record<MaintenanceState, string> = {
  overdue: 'Vencido',
  soon: 'Próximo',
  ok: 'Al día',
  done: 'Hecho hoy',
  closed: 'Cerrado',
}

/** Marca de una letra en la fila, como en el prototipo. */
export const STATE_MARKS: Record<MaintenanceState, string> = {
  overdue: '!',
  soon: '•',
  ok: '✓',
  done: '✓',
  closed: '—',
}

export function whenLabel(record: MaintenanceRecord, state: MaintenanceState, today = new Date()): string {
  if (state === 'closed') return 'hecho'
  const days = daysUntil(record.nextDueAt, today)
  if (days < 0) return `hace ${Math.abs(days)} d`
  if (days === 0) return 'hoy'
  if (days === 1) return 'mañana'
  if (days <= DUE_SOON_DAYS) return `en ${days} d`
  return toLocalDate(record.nextDueAt).toLocaleDateString('es-MX', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/**
 * La periodicidad, en palabras. Es el dato que explica por qué un
 * mantenimiento vuelve, y hasta ahora no se mostraba en ninguna parte pese
 * a estar guardado.
 */
export function everyLabel(intervalMonths: number | undefined): string {
  if (!intervalMonths || intervalMonths < 1) return 'Sin repetición'
  if (intervalMonths === 1) return 'Cada mes'
  if (intervalMonths === 12) return 'Cada año'
  return `Cada ${intervalMonths} meses`
}

export function formatDate(iso: string): string {
  return toLocalDate(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' })
}

export interface MaintenanceGroup {
  id: 'overdue' | 'soon' | 'later' | 'done'
  label: string
  items: MaintenanceRecord[]
}

/** Agrupa por cercanía, igual que Pagos: la pregunta es "qué toca". */
export function groupRecords(
  records: MaintenanceRecord[],
  stateOf: (record: MaintenanceRecord) => MaintenanceState,
): MaintenanceGroup[] {
  const buckets: Record<MaintenanceGroup['id'], MaintenanceRecord[]> = {
    overdue: [],
    soon: [],
    later: [],
    done: [],
  }

  for (const record of records) {
    const state = stateOf(record)
    if (state === 'overdue') buckets.overdue.push(record)
    else if (state === 'soon') buckets.soon.push(record)
    else if (state === 'ok') buckets.later.push(record)
    else buckets.done.push(record)
  }

  const byDate = (a: MaintenanceRecord, b: MaintenanceRecord) =>
    a.nextDueAt.localeCompare(b.nextDueAt)

  return [
    { id: 'overdue' as const, label: 'Vencido', items: buckets.overdue.sort(byDate) },
    { id: 'soon' as const, label: 'Esta semana', items: buckets.soon.sort(byDate) },
    { id: 'later' as const, label: 'Más adelante', items: buckets.later.sort(byDate) },
    { id: 'done' as const, label: 'Hecho', items: buckets.done.sort(byDate) },
  ].filter((group) => group.items.length > 0)
}

export interface MaintenanceSummary {
  next: MaintenanceRecord | undefined
  pendingCount: number
  overdueCount: number
  recurringCount: number
  totalCount: number
}

/** Tres datos, como en el prototipo: lo siguiente, cuántos penden y
    cuántos se repiten. */
export function summarize(
  records: MaintenanceRecord[],
  stateOf: (record: MaintenanceRecord) => MaintenanceState,
  today = new Date(),
): MaintenanceSummary {
  const pending = records.filter((record) => {
    const state = stateOf(record)
    return state === 'ok' || state === 'soon' || state === 'overdue'
  })

  return {
    next: [...pending].sort((a, b) => a.nextDueAt.localeCompare(b.nextDueAt))[0],
    pendingCount: pending.length,
    overdueCount: pending.filter((record) => daysUntil(record.nextDueAt, today) < 0).length,
    recurringCount: records.filter((record) => !!record.intervalMonths).length,
    totalCount: records.length,
  }
}
