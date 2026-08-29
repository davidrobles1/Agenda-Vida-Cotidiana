import type { Tone } from '../../../core/ui/components/MetricCard'
import type { Warranty } from '../../warranties/api'
import type { MaintenanceRecord } from '../../maintenance/api'
import type { Subscription } from '../../subscriptions/api'

/**
 * ALERTAS DE FECHA — Garantías, Mantenimiento y Suscripciones alimentan el
 * Calendario (pedido explícito del usuario, 2026-08-28; ADR-018).
 *
 * DECISIÓN DE DISEÑO, y la razón de que este archivo sea una función pura y
 * no una entidad nueva: **las alertas se DERIVAN, no se almacenan**.
 *
 * El usuario pidió cuatro cosas que, con alertas guardadas en base de datos,
 * serían cuatro problemas distintos que resolver a mano:
 *   - "si cambia la fecha del registro original, las alertas deben
 *     actualizarse" → derivar lo da gratis: no hay copia que sincronizar;
 *   - "evita duplicar alertas para el mismo evento y fecha" → imposible
 *     duplicar algo que se calcula cada vez, y además cada alerta lleva un
 *     id determinista (`origen:registro:fecha:díasAntes`);
 *   - "deben quedar vinculadas a su módulo y registro de origen" → la
 *     alerta ES una vista del registro, lleva su id y su ruta;
 *   - "no deben convertirse en REMINDER/tarea" → nunca tocan el módulo de
 *     recordatorios; no hay endpoint, no hay tabla, no hay forma de que
 *     acaben en la lista de tareas del usuario.
 *
 * También es la opción coherente con el proyecto: `useCalendarData` ya
 * cargaba garantías y mantenimientos para el calendario. Aquí se suma
 * Suscripciones y se interpretan los tres, sin arquitectura paralela ni
 * migraciones para datos que ya existen.
 */

/** Importancia de la alerta. El usuario las nombró así: baja/media/alta. */
export type AlertSeverity = 'low' | 'medium' | 'high'

export type AlertSource = 'warranty' | 'maintenance' | 'subscription'

export interface DateAlert {
  /** Determinista: mismo evento + misma fecha ⇒ mismo id ⇒ nunca duplica. */
  id: string
  /** `YYYY-MM-DD`, el día en que la alerta debe aparecer en el calendario. */
  dateKey: string
  severity: AlertSeverity
  source: AlertSource
  /** Id del registro de origen, para volver a él. */
  sourceId: string
  /** Nombre del registro tal cual lo escribió el usuario. */
  sourceLabel: string
  /** Texto que se muestra ("Vence en 15 días", "Pago hoy"…). */
  message: string
  /** Días que faltan para el hecho real. 0 = es hoy. */
  daysBefore: number
  /** `YYYY-MM-DD` del hecho real (vencimiento, mantenimiento o pago). */
  eventDateKey: string
  /** Ruta del módulo de origen. */
  href: string
}

export const SEVERITY_LABELS: Record<AlertSeverity, string> = {
  low: 'Baja',
  medium: 'Media',
  high: 'Alta',
}

/** Orden de prioridad para listar "lo más importante primero". */
export const SEVERITY_RANK: Record<AlertSeverity, number> = {
  high: 0,
  medium: 1,
  low: 2,
}

/** Traducción al vocabulario de tonos que ya usa el calendario. */
export const SEVERITY_TONE: Record<AlertSeverity, Tone> = {
  high: 'error',
  medium: 'warning',
  low: 'info',
}

export const SOURCE_LABELS: Record<AlertSource, string> = {
  warranty: 'Garantía',
  maintenance: 'Mantenimiento',
  subscription: 'Suscripción',
}

/**
 * Los tres calendarios de aviso, exactamente como los pidió el usuario.
 * Están juntos y en un solo sitio a propósito: es la regla de negocio de
 * esta funcionalidad y debe poder leerse de un vistazo.
 */
const WARRANTY_OFFSETS: Array<{ days: number; severity: AlertSeverity }> = [
  { days: 30, severity: 'medium' },
  { days: 15, severity: 'medium' },
  { days: 0, severity: 'high' },
]

const MAINTENANCE_OFFSETS: Array<{ days: number; severity: AlertSeverity }> = [
  { days: 7, severity: 'low' },
  { days: 3, severity: 'medium' },
  { days: 0, severity: 'high' },
]

const SUBSCRIPTION_OFFSETS: Array<{ days: number; severity: AlertSeverity }> = [
  { days: 5, severity: 'low' },
  { days: 2, severity: 'medium' },
  { days: 0, severity: 'high' },
]

/** Cuántas ocurrencias futuras se proyectan de un mantenimiento periódico o
    de una suscripción. Suficiente para cubrir un calendario navegable sin
    generar series infinitas. */
const MAX_PROJECTED_OCCURRENCES = 12

function toDateKey(value: Date): string {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** Las fechas del backend llegan como ISO; el calendario razona en días
    locales, así que se recorta a `YYYY-MM-DD` sin convertir zonas horarias
    (el mismo criterio que ya usa `isoDateKey` en calendarHelpers). */
function eventDateKeyOf(iso: string): string {
  return iso.slice(0, 10)
}

function parseDateKey(key: string): Date {
  const [year, month, day] = key.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function shiftDays(key: string, deltaDays: number): string {
  const date = parseDateKey(key)
  date.setDate(date.getDate() + deltaDays)
  return toDateKey(date)
}

function shiftMonths(key: string, deltaMonths: number): string {
  const date = parseDateKey(key)
  const targetDay = date.getDate()
  date.setMonth(date.getMonth() + deltaMonths)
  // `setMonth` desborda cuando el mes destino es más corto (31 de enero
  // + 1 mes = 3 de marzo). Se corrige al último día del mes esperado.
  if (date.getDate() !== targetDay) date.setDate(0)
  return toDateKey(date)
}

function messageFor(daysBefore: number, kind: 'expira' | 'mantenimiento' | 'pago'): string {
  if (daysBefore === 0) {
    if (kind === 'expira') return 'Vence hoy'
    if (kind === 'pago') return 'Se cobra hoy'
    return 'Mantenimiento hoy'
  }
  const unit = daysBefore === 1 ? 'día' : 'días'
  if (kind === 'expira') return `Vence en ${daysBefore} ${unit}`
  if (kind === 'pago') return `Se cobra en ${daysBefore} ${unit}`
  return `Mantenimiento en ${daysBefore} ${unit}`
}

interface BuildAlertsInput {
  warranties: Warranty[]
  maintenanceRecords: MaintenanceRecord[]
  subscriptions: Subscription[]
}

/**
 * Genera todas las alertas derivadas. Puro: mismas entradas, mismas
 * salidas, sin fechas "ahora" implícitas salvo para descartar el pasado.
 */
export function buildDateAlerts({
  warranties,
  maintenanceRecords,
  subscriptions,
}: BuildAlertsInput): DateAlert[] {
  // Clave = id determinista. Es el mecanismo antiduplicados: dos reglas que
  // caigan en el mismo día para el mismo evento colapsan en una sola.
  const byId = new Map<string, DateAlert>()

  const push = (alert: DateAlert) => {
    const existing = byId.get(alert.id)
    // Si por cualquier motivo coincidieran dos, gana la más importante.
    if (existing && SEVERITY_RANK[existing.severity] <= SEVERITY_RANK[alert.severity]) return
    byId.set(alert.id, alert)
  }

  const emit = (
    source: AlertSource,
    sourceId: string,
    sourceLabel: string,
    href: string,
    eventKey: string,
    offsets: Array<{ days: number; severity: AlertSeverity }>,
    kind: 'expira' | 'mantenimiento' | 'pago',
  ) => {
    for (const offset of offsets) {
      const dateKey = shiftDays(eventKey, -offset.days)
      push({
        id: `${source}:${sourceId}:${eventKey}:${offset.days}`,
        dateKey,
        severity: offset.severity,
        source,
        sourceId,
        sourceLabel,
        message: messageFor(offset.days, kind),
        daysBefore: offset.days,
        eventDateKey: eventKey,
        href,
      })
    }
  }

  for (const warranty of warranties) {
    // Una garantía ya cerrada por el usuario no debe seguir avisando.
    if (warranty.status === 'COMPLETADO') continue
    emit(
      'warranty',
      warranty.id,
      warranty.item,
      '/warranties',
      eventDateKeyOf(warranty.expiresAt),
      WARRANTY_OFFSETS,
      'expira',
    )
  }

  for (const record of maintenanceRecords) {
    if (record.status === 'COMPLETADO') continue

    const firstKey = eventDateKeyOf(record.nextDueAt)
    emit('maintenance', record.id, record.item, '/maintenance', firstKey, MAINTENANCE_OFFSETS, 'mantenimiento')

    // "La frecuencia de '¿Cada cuándo?' debe utilizarse para calcular las
    // próximas fechas": con intervalo, el mantenimiento no es una fecha
    // suelta sino una serie, y el calendario debe mostrarla.
    const interval = record.intervalMonths
    if (!interval || interval < 1) continue
    for (let occurrence = 1; occurrence <= MAX_PROJECTED_OCCURRENCES; occurrence += 1) {
      const key = shiftMonths(firstKey, interval * occurrence)
      emit('maintenance', record.id, record.item, '/maintenance', key, MAINTENANCE_OFFSETS, 'mantenimiento')
    }
  }

  for (const subscription of subscriptions) {
    const firstKey = eventDateKeyOf(subscription.nextPaymentDate)
    emit(
      'subscription',
      subscription.id,
      subscription.service,
      '/subscriptions',
      firstKey,
      SUBSCRIPTION_OFFSETS,
      'pago',
    )

    // El ciclo de facturación ya es la periodicidad del pago: proyectarla
    // es lo mismo que hace "¿Cada cuánto?" en Mantenimiento.
    for (let occurrence = 1; occurrence <= MAX_PROJECTED_OCCURRENCES; occurrence += 1) {
      const key =
        subscription.billingCycle === 'WEEKLY'
          ? shiftDays(firstKey, 7 * occurrence)
          : shiftMonths(firstKey, (subscription.billingCycle === 'YEARLY' ? 12 : 1) * occurrence)
      emit('subscription', subscription.id, subscription.service, '/subscriptions', key, SUBSCRIPTION_OFFSETS, 'pago')
    }
  }

  return [...byId.values()]
}

/** Agrupa por día, que es como las consume el calendario. */
export function groupAlertsByDay(alerts: DateAlert[]): Record<string, DateAlert[]> {
  const map: Record<string, DateAlert[]> = {}
  for (const alert of alerts) {
    if (!map[alert.dateKey]) map[alert.dateKey] = []
    map[alert.dateKey].push(alert)
  }
  for (const key of Object.keys(map)) {
    map[key].sort(bySeverityThenDate)
  }
  return map
}

export function bySeverityThenDate(a: DateAlert, b: DateAlert): number {
  return (
    SEVERITY_RANK[a.severity] - SEVERITY_RANK[b.severity] ||
    a.dateKey.localeCompare(b.dateKey) ||
    a.sourceLabel.localeCompare(b.sourceLabel)
  )
}

/**
 * Las alertas "próximas": las que caen entre hoy y `horizonDays`, ordenadas
 * por importancia. Es lo que se muestra al iniciar sesión.
 */
export function upcomingAlerts(alerts: DateAlert[], horizonDays = 30, today = new Date()): DateAlert[] {
  const todayKey = toDateKey(today)
  const limitKey = shiftDays(todayKey, horizonDays)
  return alerts
    .filter((alert) => alert.dateKey >= todayKey && alert.dateKey <= limitKey)
    .sort(bySeverityThenDate)
}
