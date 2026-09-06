import { DOCUMENT_CATEGORY_LABELS, type DocumentCategory, type DocumentVisibility, type VidaDocument } from './api'

/**
 * ADR-022 — lógica de presentación de Documentos.
 */

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * `createdAt` es un instante real (momento de la subida), no una fecha de
 * calendario: aquí `new Date(iso)` es correcto y NO aplica la regla del
 * mediodía local del ADR-021(j), que existe para fechas guardadas a
 * medianoche UTC.
 */
export function formatUploadedAt(iso: string): string {
  return new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' })
}

/**
 * ADR-022: la visibilidad pasa a ser una etiqueta con texto.
 *
 * Antes se comunicaba únicamente con un icono de 18 px (candado / globo /
 * personas) sin rótulo: que un documento esté a la vista de toda la familia
 * es un hecho de privacidad y no puede depender de que el usuario reconozca
 * un glifo.
 */
export const VISIBILITY_LABELS: Record<DocumentVisibility, string> = {
  PRIVATE: 'Solo yo',
  SHARED: 'Compartido',
  FAMILY_PUBLIC: 'Familia',
}

export const VISIBILITY_MARKS: Record<DocumentVisibility, string> = {
  PRIVATE: '🔒',
  SHARED: '🔗',
  FAMILY_PUBLIC: '👪',
}

/** Tono de la etiqueta: lo más expuesto, más visible. */
export const VISIBILITY_TONES: Record<DocumentVisibility, 'ok' | 'soon' | 'link'> = {
  PRIVATE: 'ok',
  SHARED: 'soon',
  FAMILY_PUBLIC: 'link',
}

export interface CategoryCount {
  id: DocumentCategory | 'all'
  label: string
  count: number
}

/**
 * Recuentos por categoría para las tarjetas.
 *
 * `total` viene del servidor (`totalElements`) y no de la longitud de la
 * lista: con más documentos que el tamaño de página, contar lo cargado daría
 * un número menor que el real.
 */
export function categoryCounts(documents: VidaDocument[], total: number): CategoryCount[] {
  return [
    { id: 'all' as const, label: 'Todos', count: total },
    ...(Object.keys(DOCUMENT_CATEGORY_LABELS) as DocumentCategory[]).map((id) => ({
      id,
      label: DOCUMENT_CATEGORY_LABELS[id],
      count: documents.filter((document) => document.category === id).length,
    })),
  ]
}

/** "1 documentos" era el texto que salía antes de esto. */
export function countLabel(count: number): string {
  return `${count} ${count === 1 ? 'documento' : 'documentos'}`
}

export function emptyReason(
  totalLoaded: number,
  query: string,
  categoryLabel: string | null,
): { title: string; body: string; canReset: boolean } {
  if (query.trim()) {
    return {
      title: `Ningún documento coincide con “${query.trim()}”`,
      body: `Tienes ${countLabel(totalLoaded)} guardados.`,
      canReset: true,
    }
  }
  if (categoryLabel) {
    return {
      title: `No tienes documentos en ${categoryLabel}`,
      body: `Tienes ${countLabel(totalLoaded)} guardados en otras categorías.`,
      canReset: true,
    }
  }
  return {
    title: 'Aún no guardas ningún documento',
    body: 'Sube el primero y tenlo a mano cuando haga falta — identificaciones, pólizas, contratos.',
    canReset: false,
  }
}

/**
 * Un documento que me compartieron no es mío: no puedo renombrarlo, moverlo
 * de categoría ni borrarlo. El backend ya lo impide (`getOwnedOrThrow` en
 * `DocumentService`), y la interfaz debe dejar de ofrecer acciones que van a
 * fallar con un 404.
 */
export function isOwnedBy(document: VidaDocument, userId: string | null | undefined): boolean {
  return !!userId && document.ownerUserId === userId
}
