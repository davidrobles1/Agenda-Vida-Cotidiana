import { useEffect, useState } from 'react'
import { apiFetch } from './httpClient'

/**
 * Genérico, para cualquier endpoint de bytes protegido por Bearer JWT (un
 * `<img src="...">`/`<iframe src="...">` plano no puede mandar ese header,
 * así que necesita fetch autenticado + blob URL local) — mismo patrón que
 * visionboard/visionBoardImages.ts ya estableció para las imágenes de
 * Vision Board, generalizado aquí para que Documentos/Garantías (y
 * cualquier otro módulo con archivos protegidos) lo reutilicen en vez de
 * duplicarlo. Cache por path, compartida entre cualquier componente que
 * pida el mismo archivo.
 */
const objectUrlCache = new Map<string, Promise<string>>()

function fetchObjectUrl(path: string): Promise<string> {
  let cached = objectUrlCache.get(path)
  if (cached) return cached
  cached = (async () => {
    const response = await apiFetch(path)
    if (!response.ok) throw new Error(`GET ${path} failed: ${response.status}`)
    const blob = await response.blob()
    return URL.createObjectURL(blob)
  })()
  objectUrlCache.set(path, cached)
  return cached
}

export interface AuthenticatedFileSrcResult {
  src: string | undefined
  loading: boolean
  error: boolean
}

/** `path` puede ser `undefined`/vacío para desactivar el fetch (ej. mientras
    un modal de visor todavía no tiene un id real que mostrar). */
export function useAuthenticatedFileSrc(path: string | undefined): AuthenticatedFileSrcResult {
  const [state, setState] = useState<AuthenticatedFileSrcResult>(
    path ? { src: undefined, loading: true, error: false } : { src: undefined, loading: false, error: false },
  )

  useEffect(() => {
    if (!path) {
      setState({ src: undefined, loading: false, error: false })
      return
    }
    let cancelled = false
    setState({ src: undefined, loading: true, error: false })
    fetchObjectUrl(path)
      .then((objectUrl) => {
        if (!cancelled) setState({ src: objectUrl, loading: false, error: false })
      })
      .catch(() => {
        objectUrlCache.delete(path)
        if (!cancelled) setState({ src: undefined, loading: false, error: true })
      })
    return () => {
      cancelled = true
    }
  }, [path])

  return state
}
