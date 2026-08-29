/**
 * Utilidades de presentación de una Tarea, tomadas del prototipo aprobado
 * ("Agenda Laboral", artifact fca1566a). Viven aquí, y no dentro de una
 * pantalla, porque la lista y el detalle deben decir exactamente lo mismo
 * sobre la misma tarea.
 */

export function daysUntil(iso: string): number {
  return Math.round((new Date(iso).getTime() - Date.now()) / 86400000)
}

/** `relativeLabel` del prototipo, literal. */
export function relativeLabel(iso?: string): string {
  if (!iso) return 'Sin fecha'
  const days = daysUntil(iso)
  if (days === 0) return 'Hoy'
  if (days === 1) return 'Mañana'
  if (days === -1) return 'Ayer · 1 día de atraso'
  if (days < -1) return `Hace ${-days} días · atrasado`
  if (days > 1 && days <= 6) return `En ${days} días`
  return new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short' })
}

/** `isOverdue` del prototipo. Una tarea sin fecha no puede estar atrasada. */
export function isOverdue(iso?: string): boolean {
  if (!iso) return false
  return daysUntil(iso) < 0
}

/** `fmtShort` del prototipo. */
export function formatShortDate(iso?: string): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short' })
}

export function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}
