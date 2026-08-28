/**
 * UX-014/UX-015 — Módulo Laboral: vocabulario adaptable por perfil
 * (`Documentacion/02-ux-ui/design-system.md` §12, ADR-016(d)).
 *
 * **Capa de presentación pura.** El esquema y la API son idénticos para
 * cualquier perfil: `PROJECT`/`PERSON` son las mismas tablas y los mismos
 * endpoints, y nada de este archivo viaja al backend. Es una tabla de
 * strings, deliberadamente sin lógica condicional de negocio — cambiar de
 * perfil solo cambia cómo se nombran dos conceptos en la interfaz.
 *
 * Los cuatro perfiles y sus términos son exactamente los de la tabla de
 * `design-system.md` §12; no se inventó ninguno. "Consultor tecnológico" es
 * el default porque usa los términos neutros ("Proyecto"/"Persona") que la
 * app ya venía mostrando — así, un usuario que nunca toca esta preferencia
 * no percibe ningún cambio.
 *
 * **Persistencia:** `localStorage`, por dispositivo. No hay columna nueva en
 * `USER` ni endpoint nuevo: FR/ADR-016(d) define esto como capa de cliente,
 * y añadir esquema para una preferencia de presentación excedería ese
 * alcance. Limitación aceptada y declarada: no sincroniza entre
 * dispositivos (mismo criterio y mismo precedente que el Inbox de FR-028).
 */

export type ProfessionalProfile = 'CONSULTOR' | 'ARQUITECTO' | 'ABOGADO' | 'VENDEDOR'

/**
 * Género gramatical del término. Necesario porque el vocabulario cambia el
 * género entre perfiles ("las Obras" vs. "los Casos", "las Personas" vs.
 * "los Contactos") y los textos de la interfaz llevan artículos. Se declara
 * explícitamente en vez de deducirlo de la terminación: "-o/-a" acierta en
 * estos ocho términos por casualidad, pero es una regla falsa en español
 * (p. ej. "el día") y rompería en cuanto se añadiera un perfil nuevo.
 */
export type Gender = 'f' | 'm'

export interface ProfileVocabulary {
  /** Etiqueta del perfil en Ajustes. */
  label: string
  /** Término para PROJECT, singular y plural. */
  project: string
  projectPlural: string
  projectGender: Gender
  /** Término para PERSON, singular y plural. */
  person: string
  personPlural: string
  personGender: Gender
}

/** "la"/"el" · "las"/"los" · "ninguna"/"ningún", según el género del término. */
export const article = {
  definiteSingular: (g: Gender) => (g === 'f' ? 'la' : 'el'),
  definitePlural: (g: Gender) => (g === 'f' ? 'las' : 'los'),
  none: (g: Gender) => (g === 'f' ? 'ninguna' : 'ningún'),
}

/** Tabla de `design-system.md` §12, literal. Ningún término inventado. */
export const PROFILE_VOCABULARY: Record<ProfessionalProfile, ProfileVocabulary> = {
  CONSULTOR: {
    label: 'Consultor tecnológico',
    project: 'Proyecto',
    projectPlural: 'Proyectos',
    projectGender: 'm',
    person: 'Persona',
    personPlural: 'Personas',
    personGender: 'f',
  },
  ARQUITECTO: {
    label: 'Arquitecto',
    project: 'Obra',
    projectPlural: 'Obras',
    projectGender: 'f',
    person: 'Contacto',
    personPlural: 'Contactos',
    personGender: 'm',
  },
  ABOGADO: {
    label: 'Abogado',
    project: 'Caso',
    projectPlural: 'Casos',
    projectGender: 'm',
    // La tabla de §12 mantiene "Persona" para Abogado — no se cambió.
    person: 'Persona',
    personPlural: 'Personas',
    personGender: 'f',
  },
  VENDEDOR: {
    label: 'Vendedor',
    project: 'Oportunidad',
    projectPlural: 'Oportunidades',
    projectGender: 'f',
    person: 'Prospecto',
    personPlural: 'Prospectos',
    personGender: 'm',
  },
}

export const DEFAULT_PROFILE: ProfessionalProfile = 'CONSULTOR'

export const PROFILE_ORDER: ProfessionalProfile[] = ['CONSULTOR', 'ARQUITECTO', 'ABOGADO', 'VENDEDOR']

const STORAGE_KEY = 'vc.laboral.profile'

function isProfile(value: string | null): value is ProfessionalProfile {
  return value !== null && value in PROFILE_VOCABULARY
}

export function readProfile(): ProfessionalProfile {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return isProfile(stored) ? stored : DEFAULT_PROFILE
  } catch {
    // Modo privado o storage bloqueado: el default neutro sigue siendo correcto.
    return DEFAULT_PROFILE
  }
}

export function writeProfile(profile: ProfessionalProfile): void {
  try {
    localStorage.setItem(STORAGE_KEY, profile)
  } catch {
    // Sin persistencia el vocabulario simplemente vuelve al default; no es
    // un error que deba interrumpir al usuario.
  }
  listeners.forEach((listener) => listener())
}

/**
 * Suscripción mínima para `useSyncExternalStore` — sin Context ni provider
 * nuevo: el vocabulario es un valor global de solo lectura para casi toda
 * la app, y el único punto que lo escribe es Ajustes.
 */
const listeners = new Set<() => void>()

export function subscribeProfile(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}
