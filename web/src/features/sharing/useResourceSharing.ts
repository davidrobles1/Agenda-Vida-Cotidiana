import { useCallback, useState } from 'react'
import { applyShares, type PendingShare, type SharedResourceType } from './resourceShares'

/**
 * Cablea el control de compartición dentro de un formulario de recurso
 * (ADR-025 §2) con las mismas cuatro líneas en los seis diálogos.
 *
 * Existe para que integrar la compartición en un módulo NO signifique repetir
 * en cada uno el estado, el reinicio al abrir y el manejo de errores — que es
 * justo donde aparecen las diferencias de comportamiento entre pantallas que
 * deberían comportarse igual.
 */
export function useResourceSharing(type: SharedResourceType) {
  const [shares, setShares] = useState<PendingShare[]>([])

  /** Al abrir el diálogo: el control vuelve a sembrarse desde el servidor. */
  const reset = useCallback(() => setShares([]), [])

  /**
   * Se ejecuta DESPUÉS de que el recurso se guardó.
   *
   * Devuelve el mensaje de error o `null`. Nunca lanza: el recurso ya está
   * escrito y una compartición que falla no puede presentarse como si el
   * guardado hubiera fallado.
   */
  const commit = useCallback(
    async (resourceId: string): Promise<string | null> => {
      try {
        await applyShares(type, resourceId, shares)
        return null
      } catch (e) {
        return e instanceof Error
          ? `Se guardó, pero no se pudo actualizar con quién está compartido: ${e.message}`
          : 'Se guardó, pero no se pudo actualizar con quién está compartido.'
      }
    },
    [type, shares],
  )

  return { shares, setShares, reset, commit }
}
