import { useEffect, useState } from 'react'
import { apiFetch } from '../../core/api/httpClient'

/**
 * BLOQUE B (post-MVP): resolves an IMAGE element's `data` into a real
 * `<img src>`. Two independent sources can live in `data`:
 * - `data.imageId`: an internally-stored upload (BLOQUE A/B — file picker,
 *   drag & drop, paste). `GET /vision-board-images/{id}` requires the same
 *   bearer JWT as every other endpoint (VisionBoardImageController's own
 *   doc comment), so a plain `<img src="/api/.../vision-board-images/{id}">`
 *   would 401 — this fetches it with the real auth header and hands back a
 *   local `URL.createObjectURL` blob URL instead.
 * - `data.url`: the original external-URL path (unchanged since FASE 6) —
 *   used directly, no fetch needed.
 *
 * One module-level cache (by imageId), shared by every caller — the canvas
 * element view, the editor's preview, and the export renderer all resolve
 * through this same function instead of each doing their own fetch, so an
 * image already on screen never gets re-fetched just because a second
 * component (e.g. the Editar popover) also wants to render it.
 */
const objectUrlCache = new Map<string, Promise<string>>()

function fetchImageObjectUrl(imageId: string): Promise<string> {
  let cached = objectUrlCache.get(imageId)
  if (cached) return cached
  cached = (async () => {
    const response = await apiFetch(`/vision-board-images/${imageId}`)
    if (!response.ok) throw new Error(`GET /vision-board-images/${imageId} failed: ${response.status}`)
    const blob = await response.blob()
    return URL.createObjectURL(blob)
  })()
  objectUrlCache.set(imageId, cached)
  return cached
}

/** BLOQUE B: the same resolve-and-cache logic `useVisionBoardImageSrc`
    exposes as a hook, for the one caller outside a React render —
    visionBoardExport.ts's `drawElement`, which awaits it directly inside a
    plain async function. Shares the same module-level cache, so an image
    already resolved for on-screen rendering is never re-fetched just to
    export it. */
export async function resolveVisionBoardImageSrc(imageId: string): Promise<string> {
  return fetchImageObjectUrl(imageId)
}

/**
 * 2026-08-23 (pedido explícito del usuario): "al agregar imagen la corta
 * en base a la forma, necesito que la forma se acomode en base a la
 * imagen" — antes toda IMAGE se creaba con el mismo tamaño fijo
 * (visionBoardElementDefaults.ts's `DEFAULT_SIZE.IMAGE`, 240×160) y
 * `object-fit: cover` recortaba lo que no encajara en esa proporción fija.
 * VisionBoardElementLibrary.tsx's `ImageEntryFields` usa esto para medir
 * la imagen real ANTES de crear el elemento y le pasa un tamaño con la
 * proporción real de la imagen — `cover` sigue sin recortar nada porque
 * la caja ya tiene la misma proporción que la imagen.
 *
 * No necesita `crossOrigin` (a diferencia de visionBoardExport.ts's propio
 * `loadImage`): leer `naturalWidth`/`naturalHeight` no toca los píxeles de
 * la imagen, así que no lo bloquea CORS aunque la imagen sea de otro
 * origen — solo dibujarla en un `<canvas>` (como sí hace la exportación)
 * lo necesitaría.
 */
export function loadImageNaturalSize(src: string): Promise<{ width: number; height: number } | null> {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve({ width: img.naturalWidth, height: img.naturalHeight })
    img.onerror = () => resolve(null)
    img.src = src
  })
}

/** Misma proporción real de la imagen, escalada para que el lado más
    largo mida `maxSide` (con un piso de `minSide` para que una imagen muy
    apaisada/alargada no termine con el lado corto ilegiblemente pequeño). */
export function fitImageElementSize(natural: { width: number; height: number }, maxSide = 260, minSide = 90): { width: number; height: number } {
  if (natural.width <= 0 || natural.height <= 0) return { width: maxSide, height: maxSide * (160 / 240) }
  const ratio = natural.width / natural.height
  if (ratio >= 1) {
    return { width: maxSide, height: Math.max(minSide, Math.round(maxSide / ratio)) }
  }
  return { height: maxSide, width: Math.max(minSide, Math.round(maxSide * ratio)) }
}

export interface VisionBoardImageSrcResult {
  src: string | undefined
  loading: boolean
  /** True once resolution is settled and there's nothing renderable — an
      internal fetch failed, or there was neither an `imageId` nor a `url`
      to begin with. Callers show the broken-image placeholder on this,
      not only on the `<img>` tag's own `onError` (which only catches a
      resolved-but-unloadable `src`, not "never had one"). */
  error: boolean
}

export function useVisionBoardImageSrc(data: Record<string, unknown>): VisionBoardImageSrcResult {
  const imageId = typeof data.imageId === 'string' ? data.imageId : undefined
  const externalUrl = typeof data.url === 'string' && data.url.length > 0 ? data.url : undefined

  const [state, setState] = useState<VisionBoardImageSrcResult>(() =>
    imageId
      ? { src: undefined, loading: true, error: false }
      : { src: externalUrl, loading: false, error: !externalUrl },
  )

  useEffect(() => {
    if (!imageId) {
      setState({ src: externalUrl, loading: false, error: !externalUrl })
      return
    }
    let cancelled = false
    setState({ src: undefined, loading: true, error: false })
    fetchImageObjectUrl(imageId)
      .then((objectUrl) => {
        if (!cancelled) setState({ src: objectUrl, loading: false, error: false })
      })
      .catch(() => {
        // Allow a retry on the next mount (e.g. a transient network error)
        // instead of caching the failure forever.
        objectUrlCache.delete(imageId)
        if (!cancelled) setState({ src: undefined, loading: false, error: true })
      })
    return () => {
      cancelled = true
    }
  }, [imageId, externalUrl])

  return state
}
