/**
 * ADR-016/FR-028 — Inbox de captura rápida. El backend NOTE no se extendió
 * con vínculos opcionales a Persona/Proyecto/Reminder en la Fase 1
 * (deliberado, para no exceder el alcance del backend ya implementado —
 * ver `docs/development/08-laboral-module-plan.md`), así que no hay forma
 * de distinguir server-side "nota sin clasificar" de cualquier otra nota.
 * Para no fingir un filtro que no existe, el Inbox de V3 es un capture
 * ligero **solo de cliente** (localStorage) — efímero por diseño, por
 * dispositivo, no sincronizado. "Convertir" crea una Tarea/Compromiso real
 * vía la API y solo entonces borra el ítem local.
 */
const STORAGE_KEY = 'vc_laboral_inbox'

export interface InboxItem {
  id: string
  text: string
  createdAt: string
}

export function loadInboxItems(): InboxItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as InboxItem[]) : []
  } catch {
    return []
  }
}

function saveInboxItems(items: InboxItem[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  } catch {
    // best-effort only — un Inbox que no persiste en este navegador no es fatal
  }
}

export function addInboxItem(text: string): InboxItem[] {
  const next = [{ id: crypto.randomUUID(), text, createdAt: new Date().toISOString() }, ...loadInboxItems()]
  saveInboxItems(next)
  return next
}

export function removeInboxItem(id: string): InboxItem[] {
  const next = loadInboxItems().filter((item) => item.id !== id)
  saveInboxItems(next)
  return next
}
