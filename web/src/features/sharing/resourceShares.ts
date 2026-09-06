import { apiFetch } from '../../core/api/httpClient'

/**
 * Compartidos (ADR-025 §2/§3/§4).
 *
 * Una sola API para los seis tipos de recurso. Lo que viaja es la RELACIÓN de
 * compartición —con el nombre y la fecha del recurso ya resueltos—, nunca una
 * copia del recurso: el dato sigue viviendo en su módulo.
 */

/**
 * Los tipos que se pueden compartir.
 *
 * Sin ALERTA y sin EVENTO, y ninguna de las dos ausencias es un olvido:
 *   · las ALERTAS se derivan de garantías, mantenimientos y pagos (ADR-018);
 *     no tienen fila propia. Compartir el registro de origen comparte su
 *     alerta, que es justo la propiedad por la que se decidió derivarlas.
 *   · no existe una entidad EVENTO: lo que el calendario pinta como evento es
 *     un recordatorio con fecha. Tarea y evento son la misma fila.
 */
export type SharedResourceType =
  | 'REMINDER'
  | 'MAINTENANCE'
  | 'SUBSCRIPTION'
  | 'WARRANTY'
  | 'INVENTORY_ITEM'
  | 'DOCUMENT'

export interface ResourceShare {
  id: string
  resourceType: SharedResourceType
  resourceId: string
  resourceLabel: string
  /** ISO. Ausente donde el recurso no tiene fecha (inventario, documento). */
  resourceDate?: string
  counterpartUserId?: string
  counterpartUsername?: string
  responsibility: boolean
  /** ISO. Presente = esa persona ya marcó su parte. */
  partDoneAt?: string
  version: number
}

/** Etiqueta de cada tipo, tal como se nombra la sección en el portal. */
export const RESOURCE_LABELS: Record<SharedResourceType, string> = {
  REMINDER: 'Tarea',
  MAINTENANCE: 'Mantenimiento',
  SUBSCRIPTION: 'Pago',
  WARRANTY: 'Garantía',
  INVENTORY_ITEM: 'Artículo',
  DOCUMENT: 'Documento',
}

/** A dónde lleva "ver el original". Son las rutas que ya existen. */
export const RESOURCE_ROUTES: Record<SharedResourceType, string> = {
  REMINDER: '/personal/calendar',
  MAINTENANCE: '/maintenance',
  SUBSCRIPTION: '/subscriptions',
  WARRANTY: '/warranties',
  INVENTORY_ITEM: '/inventory',
  DOCUMENT: '/documents',
}

/**
 * Qué significa "hacer mi parte" en cada recurso — analizado uno a uno, no
 * copiado de Alertas (requisito §5). Donde no significa nada, no se ofrece.
 */
export const PART_DONE_LABELS: Partial<Record<SharedResourceType, string>> = {
  REMINDER: 'Ya hice mi parte',
  MAINTENANCE: 'Ya lo hice',
  SUBSCRIPTION: 'Ya lo pagué',
  WARRANTY: 'Ya lo gestioné',
}

/**
 * Los tipos que admiten responsabilidad.
 *
 * Un artículo de INVENTARIO es algo que se posee, no algo que se hace: no
 * existe "ya hice mi parte" de una silla. Un DOCUMENTO es de solo consulta por
 * requisito explícito (§6). La misma regla vive en el backend
 * (SharedResourceType#supportsResponsibility) y en la base de datos
 * (ck_resource_shares_responsibility), para que no dependa de esta pantalla.
 */
export function supportsResponsibility(type: SharedResourceType): boolean {
  return type !== 'INVENTORY_ITEM' && type !== 'DOCUMENT'
}

async function readError(response: Response, fallback: string): Promise<never> {
  const body = await response.json().catch(() => null)
  throw new Error(body?.detail ?? body?.message ?? body?.code ?? fallback)
}

export async function listReceived(): Promise<ResourceShare[]> {
  const response = await apiFetch('/shared-resources/received')
  if (!response.ok) await readError(response, 'No se pudieron cargar los recursos compartidos contigo.')
  return response.json()
}

export async function listSent(): Promise<ResourceShare[]> {
  const response = await apiFetch('/shared-resources/sent')
  if (!response.ok) await readError(response, 'No se pudieron cargar los recursos que compartiste.')
  return response.json()
}

export async function listForResource(
  type: SharedResourceType,
  resourceId: string,
): Promise<ResourceShare[]> {
  const response = await apiFetch(`/shared-resources/${type}/${resourceId}`)
  if (!response.ok) await readError(response, 'No se pudo cargar con quién está compartido.')
  return response.json()
}

export async function shareResource(
  type: SharedResourceType,
  resourceId: string,
  collaboratorUserId: string,
  responsibility: boolean,
): Promise<ResourceShare> {
  const response = await apiFetch(`/shared-resources/${type}/${resourceId}`, {
    method: 'POST',
    body: JSON.stringify({ collaboratorUserId, responsibility }),
  })
  if (!response.ok) await readError(response, 'No se pudo compartir.')
  return response.json()
}

export async function updateResponsibility(
  shareId: string,
  responsibility: boolean,
): Promise<ResourceShare> {
  const response = await apiFetch(`/shared-resources/shares/${shareId}`, {
    method: 'PATCH',
    body: JSON.stringify({ responsibility }),
  })
  if (!response.ok) await readError(response, 'No se pudo cambiar la responsabilidad.')
  return response.json()
}

export async function revokeShare(shareId: string): Promise<void> {
  const response = await apiFetch(`/shared-resources/shares/${shareId}`, { method: 'DELETE' })
  if (!response.ok) await readError(response, 'No se pudo dejar de compartir.')
}

export async function markPartDone(shareId: string): Promise<ResourceShare> {
  const response = await apiFetch(`/shared-resources/shares/${shareId}/part-done`, { method: 'POST' })
  if (!response.ok) await readError(response, 'No se pudo marcar tu parte.')
  return response.json()
}

export async function reopenPart(shareId: string): Promise<ResourceShare> {
  const response = await apiFetch(`/shared-resources/shares/${shareId}/part-done`, { method: 'DELETE' })
  if (!response.ok) await readError(response, 'No se pudo deshacer.')
  return response.json()
}


/**
 * Lo que un formulario deja marcado antes de guardar (ADR-025 §2).
 *
 * Vive aquí y no en `ShareWithFamily.tsx` para que ese fichero exporte
 * únicamente su componente: mezclar componentes y utilidades en un mismo
 * módulo rompe el refresco en caliente de Vite.
 */
export interface PendingShare {
  userId: string
  username: string
  responsibility: boolean
}

/**
 * Lleva al servidor lo que el formulario dejó marcado.
 *
 * Relee el estado real antes de escribir, en vez de fiarse de un "inicial"
 * guardado en memoria: entre que se abrió el formulario y se pulsó Guardar,
 * la compartición pudo cambiar desde otra pantalla o desde otro dispositivo.
 *
 * NUNCA hace fallar el guardado del recurso: quien llama la ejecuta DESPUÉS de
 * crear o actualizar, y un fallo aquí se informa aparte. Perder la
 * compartición es molesto; perder el recurso recién escrito, no.
 */
export async function applyShares(
  type: SharedResourceType,
  resourceId: string,
  desired: PendingShare[],
): Promise<void> {
  const current = await listForResource(type, resourceId)

  for (const share of current) {
    const wanted = desired.find((entry) => entry.userId === share.counterpartUserId)
    if (!wanted) {
      await revokeShare(share.id)
    } else if (wanted.responsibility !== share.responsibility) {
      await updateResponsibility(share.id, wanted.responsibility)
    }
  }

  for (const entry of desired) {
    const exists = current.some((share) => share.counterpartUserId === entry.userId)
    if (!exists) {
      await shareResource(type, resourceId, entry.userId, entry.responsibility)
    }
  }
}
