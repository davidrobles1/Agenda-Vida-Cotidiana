/**
 * Caligrafías para los elementos de TEXTO del Vision Board (2026-08-29,
 * pedido explícito del usuario: "al agregar y al editar texto, permitir
 * seleccionar entre varias caligrafías/tipografías").
 *
 * Los identificadores son EXACTAMENTE los mismos que ya usaba la unión
 * `font` de las notas del día (`features/calendar/daynotes/api.ts`), para no
 * inventar un segundo vocabulario tipográfico en la misma aplicación.
 *
 * Cada opción apunta a una familia que `main.tsx` **ya carga** vía
 * @fontsource. No se añade ninguna fuente nueva: si una familia no
 * estuviera cargada, el navegador caería en silencio a la de respaldo y el
 * usuario elegiría una caligrafía que no vería, que es justo lo que hay que
 * evitar. Por eso la lista se derivó de los imports reales de `main.tsx`.
 *
 * `undefined` se trata como 'sans', el aspecto que tenían los textos antes
 * de este cambio — así ningún elemento existente cambia de apariencia.
 */
export interface VisionBoardFontOption {
  id: string
  label: string
  /** Pila CSS completa, con respaldo real. */
  stack: string
}

export const VISION_BOARD_FONTS: VisionBoardFontOption[] = [
  { id: 'sans', label: 'Sans', stack: "'Inter', system-ui, sans-serif" },
  { id: 'serif', label: 'Serif', stack: "'Fraunces', Georgia, serif" },
  { id: 'modern', label: 'Moderna', stack: "'Manrope', 'Inter', sans-serif" },
  { id: 'elegant', label: 'Elegante', stack: "'Cormorant Garamond', Georgia, serif" },
  { id: 'editorial', label: 'Editorial', stack: "'Playfair Display', Georgia, serif" },
  { id: 'classic', label: 'Clásica', stack: "'Libre Baskerville', Georgia, serif" },
  { id: 'fashion', label: 'Fashion', stack: "'Bodoni Moda', Didot, Georgia, serif" },
  { id: 'script', label: 'Caligráfica', stack: "'Alex Brush', cursive" },
  { id: 'handwritten', label: 'Manuscrita', stack: "'Caveat', cursive" },
  { id: 'parisienne', label: 'Parisienne', stack: "'Parisienne', cursive" },
  { id: 'extravagant', label: 'Extravagante', stack: "'Lobster', cursive" },
  { id: 'playful', label: 'Divertida', stack: "'Pacifico', cursive" },
  { id: 'mono', label: 'Mono', stack: "'Courier Prime', monospace" },
  { id: 'typewriter', label: 'Máquina', stack: "'IBM Plex Mono', monospace" },
]

export const DEFAULT_VISION_BOARD_FONT = 'sans'

export function fontStackOf(fontId: unknown): string {
  const id = typeof fontId === 'string' ? fontId : DEFAULT_VISION_BOARD_FONT
  return (
    VISION_BOARD_FONTS.find((font) => font.id === id)?.stack ??
    VISION_BOARD_FONTS[0].stack
  )
}

/**
 * Tamaños de letra, al estilo de un procesador de texto. Se guardan en
 * `data.fontSize` (px). `undefined` = 15 px, que es exactamente
 * lo que `.elementText` valía antes de esto (0.9375rem), para que ningún
 * texto ya existente cambie de aspecto.
 */
export const DEFAULT_TEXT_FONT_SIZE = 15
export const MIN_TEXT_FONT_SIZE = 10
export const MAX_TEXT_FONT_SIZE = 160
export const TEXT_FONT_SIZE_STEP = 2

export function fontSizeOf(value: unknown): number {
  const size = typeof value === 'number' && Number.isFinite(value) ? value : DEFAULT_TEXT_FONT_SIZE
  return Math.min(MAX_TEXT_FONT_SIZE, Math.max(MIN_TEXT_FONT_SIZE, size))
}

/**
 * Alto mínimo que necesita un texto para no quedar cortado (petición 1.4:
 * "el contenedor debe adaptarse al contenido para que nunca se oculte parte
 * del texto"). Es una estimación deliberadamente conservadora —cuenta los
 * saltos de línea explícitos y estima el ajuste automático por ancho—
 * porque el alto real solo lo sabe el navegador tras maquetar, y aquí hace
 * falta un número ANTES de guardar el elemento.
 *
 * Redondea siempre hacia arriba: prefiere que sobre espacio a esconder
 * una línea.
 */
export function estimateTextHeight(text: string, width: number, fontSize: number): number {
  const lineHeight = fontSize * 1.35
  // Ancho medio de un carácter ≈ 0.55 em en las familias de este catálogo.
  const charsPerLine = Math.max(1, Math.floor((width - 16) / (fontSize * 0.55)))
  const lines = text.split('\n').reduce((total, paragraph) => {
    return total + Math.max(1, Math.ceil(paragraph.length / charsPerLine))
  }, 0)
  return Math.ceil(lines * lineHeight + 16)
}
