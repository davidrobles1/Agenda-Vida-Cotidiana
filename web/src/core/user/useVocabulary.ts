import { useSyncExternalStore } from 'react'
import {
  PROFILE_VOCABULARY,
  readProfile,
  subscribeProfile,
  type ProfessionalProfile,
  type ProfileVocabulary,
} from './vocabulary'

/**
 * UX-014/UX-015 (`design-system.md` §12, ADR-016(d)). Devuelve los términos
 * del perfil activo. Capa de presentación: nada de esto cambia el esquema,
 * la API ni el comportamiento — solo cómo se nombran `PROJECT` y `PERSON`.
 *
 * Se usa `useSyncExternalStore` (mismo patrón que `isAuthenticated` en
 * `AppRouter.tsx`) para que cambiar el perfil en Ajustes se refleje de
 * inmediato en el resto de la app sin recargar.
 */
export function useProfile(): ProfessionalProfile {
  return useSyncExternalStore(subscribeProfile, readProfile, () => 'CONSULTOR' as const)
}

export function useVocabulary(): ProfileVocabulary {
  return PROFILE_VOCABULARY[useProfile()]
}
