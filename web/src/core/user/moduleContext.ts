/**
 * ADR-019 — aislamiento de recursos por módulo.
 *
 * Pedido explícito del usuario (2026-08-28): un recurso pertenece al módulo
 * desde el que se creó y no debe aparecer ni afectar al otro, "también en la
 * lógica de negocio y en las consultas/fuentes de datos", no solo en la
 * vista.
 *
 * Este archivo es solo el puente entre el modo activo del cliente y el
 * parámetro `context` de la API: el aislamiento de verdad está en el
 * servidor (columna `context` + consultas filtradas, migración V25). Sin el
 * servidor, esto no sería más que el filtro visual que el requisito prohíbe.
 */
export type ModuleContext = 'PERSONAL' | 'LABORAL'

/**
 * Añade `?context=` cuando hay módulo activo. Sin módulo (Calendario
 * general) se omite a propósito: ese modo existe para ver Personal y
 * Laboral juntos.
 */
export function withContext(path: string, context: ModuleContext | null | undefined): string {
  if (!context) return path
  return `${path}${path.includes('?') ? '&' : '?'}context=${context}`
}

/**
 * Contexto con el que se da de alta un recurso. Sin módulo activo el
 * recurso nace PERSONAL: es la regla 4 del pedido ("todos los recursos que
 * actualmente existen... pertenecen a Personal") aplicada también a las
 * pantallas que viven fuera de /personal y /laboral, como Garantías,
 * Mantenimiento o Suscripciones, que cuelgan del menú de Personal.
 */
export function creationContext(context: ModuleContext | null | undefined): ModuleContext {
  return context ?? 'PERSONAL'
}
