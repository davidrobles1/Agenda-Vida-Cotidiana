/**
 * Alertas atendidas (2026-08-29, petición 2.3: "las alertas también deben
 * tener estado de atención: indicar si están pendientes o ya atendidas y
 * permitir marcar una alerta como atendida").
 *
 * POR QUÉ EN `localStorage` Y NO EN EL BACKEND: una alerta no es una
 * entidad — se DERIVA de una garantía, un mantenimiento o una suscripción
 * (ADR-018), y no existe ninguna tabla donde guardarle un estado. Crear una
 * sería inventar arquitectura que no se pidió. Lo que sí existe como
 * precedente en este proyecto es guardar estado ligero de cliente en
 * `localStorage` (`features/laboral/inboxStorage.ts`, la preferencia de
 * vocabulario, la marca de actividad de sesión), así que se sigue ese
 * camino.
 *
 * LIMITACIÓN DECLARADA, no escondida: al vivir en el navegador, "atendida"
 * es **por dispositivo**. Si el usuario entra desde otro equipo, verá esa
 * alerta como pendiente otra vez. Llevarlo al servidor requeriría decidir
 * dónde se guarda, y eso es una decisión de producto que no se ha tomado.
 *
 * La clave de cada alerta es su id determinista de `dateAlerts.ts`
 * (`origen:registro:fechaDelEvento:díasAntes`), así que marcar una alerta no
 * afecta a las demás del mismo registro: atender el aviso de "30 días antes"
 * no oculta el del día del vencimiento.
 */

const STORAGE_KEY = 'vc_attended_alerts'

type Listener = () => void

let listeners: Listener[] = []
let cache: Set<string> | null = null

function read(): Set<string> {
  if (cache) return cache
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? (JSON.parse(raw) as unknown) : []
    cache = new Set(Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === 'string') : [])
  } catch {
    // Modo privado, almacenamiento bloqueado o contenido corrupto: se
    // arranca en vacío en vez de romper la pantalla por un estado auxiliar.
    cache = new Set()
  }
  return cache
}

function write(ids: Set<string>): void {
  cache = ids
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...ids]))
  } catch {
    /* Se mantiene en memoria durante esta sesión. */
  }
  for (const listener of listeners) listener()
}

export function subscribeAttendedAlerts(listener: Listener): () => void {
  listeners.push(listener)
  return () => {
    listeners = listeners.filter((l) => l !== listener)
  }
}

/** Instancia estable: `useSyncExternalStore` compara por identidad y
    devolver un Set nuevo en cada llamada provocaría un bucle de render. */
export function getAttendedAlerts(): Set<string> {
  return read()
}

export function isAlertAttended(alertId: string): boolean {
  return read().has(alertId)
}

export function setAlertAttended(alertId: string, attended: boolean): void {
  const current = new Set(read())
  if (attended) current.add(alertId)
  else current.delete(alertId)
  write(current)
}
