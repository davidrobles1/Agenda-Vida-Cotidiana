import type { Commitment } from '../commitments/api'
import type { Reminder } from '../reminders/api'
import type { Note } from '../calendar/notes/notesData'

/**
 * ADR-016 Fase 3c (candidato V4): "última interacción" no es un campo propio
 * de Persona — se deriva del `updatedAt` más reciente entre sus Compromisos,
 * Tareas y Notas ya vinculados. Sin endpoint nuevo, sin campo nuevo en el
 * backend.
 *
 * Vive aquí, y no dentro de `PersonDetailDialog`, desde que la tarjeta de la
 * lista también la muestra (rediseño 2026-08-28, artifact fca1566a): la
 * regla es una sola, y las dos pantallas deben coincidir.
 */
export function computeLastInteraction(
  commitments: Commitment[],
  tasks: Reminder[],
  notes: Note[],
): string | null {
  const timestamps = [
    ...commitments.map((c) => c.updatedAt),
    ...tasks.map((t) => t.updatedAt),
    ...notes.map((n) => n.updatedAt),
  ]
  if (timestamps.length === 0) return null
  return timestamps.reduce((latest, current) => (current > latest ? current : latest))
}

export function formatRelativeDate(iso: string): string {
  const then = new Date(iso)
  const days = Math.round((Date.now() - then.getTime()) / 86400000)
  if (days <= 0) return 'hoy'
  if (days === 1) return 'ayer'
  if (days < 30) return `hace ${days} días`
  const months = Math.round(days / 30)
  if (months < 12) return `hace ${months} mes${months === 1 ? '' : 'es'}`
  return iso.slice(0, 10)
}

/** Iniciales del avatar (`p.initials` del prototipo), como máximo dos. */
export function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}
