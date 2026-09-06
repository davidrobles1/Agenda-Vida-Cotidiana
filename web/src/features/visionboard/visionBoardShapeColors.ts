/**
 * Color de fondo y de texto por forma, con contraste automático.
 *
 * Hasta ahora una forma tomaba su relleno del TEMA DEL TABLERO
 * (`--vb-shape-bg`) y su texto de `--vb-text`: todas las formas de un
 * tablero eran del mismo color y no había manera de cambiarlo. Esto añade
 * un color propio por forma, guardado en su `data` (`fill` y `textColor`),
 * sin tocar el contrato del backend — `data` es JSON libre.
 *
 * Una forma SIN color propio sigue heredando el del tema, que es el
 * comportamiento actual y el que deben conservar los tableros existentes.
 */

export interface ShapeSwatch {
  /** Valor CSS que se guarda en `data.fill` / `data.textColor`. */
  value: string
  label: string
}

/**
 * Paleta de fondos. Los tonos salen de la identidad de la aplicación —los
 * mismos azules, terracotas, verdes y arenas del sistema de diseño— en vez
 * de un arcoíris genérico: una forma con un fucsia puro desentonaría con
 * cualquiera de los temas del tablero.
 *
 * Se guardan como HEX literal y no como `var(--token)` a propósito: el
 * color de una forma es una decisión del usuario sobre ESA forma, y debe
 * sobrevivir a que cambie el tema del tablero o el tema del portal. Los
 * tokens sí gobiernan el resto de la interfaz.
 */
export const SHAPE_FILL_SWATCHES: ShapeSwatch[] = [
  { value: '#2f6188', label: 'Azul tinta' },
  { value: '#3f6b7c', label: 'Azul niebla' },
  { value: '#4f7a5b', label: 'Verde salvia' },
  { value: '#9aab68', label: 'Verde oliva' },
  { value: '#a85636', label: 'Terracota' },
  { value: '#c9a227', label: 'Ocre' },
  { value: '#8a5a7c', label: 'Ciruela' },
  { value: '#e8dcc8', label: 'Arena' },
  { value: '#f4f1ea', label: 'Marfil' },
  { value: '#2c3038', label: 'Carbón' },
]

/* NOTA SOBRE ESTOS DIEZ VALORES — no son un tono cualquiera de cada familia.
   Cada uno se comprobó contra los dos tonos de tinta y se ajustó hasta que
   el mejor de los dos alcanza 4.5:1 (AA de WCAG para texto normal), para
   que la sugerencia automática nunca tenga que recurrir a blanco o negro
   puros, que es lo que el pedido quiere evitar por armonía visual:

     azul tinta  #2f6188 → marfil 6.2:1     verde oliva #9aab68 → carbón 5.3:1
     azul niebla #3f6b7c → marfil 5.4:1     terracota   #a85636 → marfil 4.9:1
     verde salvia#4f7a5b → marfil 4.6:1     ocre        #c9a227 → carbón 5.5:1
     ciruela     #8a5a7c → marfil 5.1:1     arena       #e8dcc8 → carbón 9.8:1
     marfil      #f4f1ea → carbón 11.7:1    carbón      #2c3038 → marfil 12.4:1

   Los tonos originales de azul niebla (#4a7a8c, 4.4:1), verde oliva
   (#8a9a5b, 4.3:1), terracota (#bb6440, 3.9:1) y ciruela (#9c6b8e, 4.0:1)
   se oscurecieron o aclararon lo justo para cruzar el umbral. */

/**
 * Paleta de textos. Deliberadamente corta y sin saturación: el texto de una
 * forma se lee, no decora. Incluye los tonos claros y oscuros que la
 * sugerencia automática puede proponer, para que el usuario reconozca de
 * dónde salió lo que ve.
 */
export const SHAPE_TEXT_SWATCHES: ShapeSwatch[] = [
  { value: '#faf7f0', label: 'Marfil' },
  { value: '#ffffff', label: 'Blanco' },
  { value: '#e8dcc8', label: 'Arena clara' },
  { value: '#2c3038', label: 'Carbón' },
  { value: '#24303c', label: 'Azul marino' },
  { value: '#3d2f24', label: 'Café oscuro' },
  { value: '#1a1a1a', label: 'Negro suave' },
]

/** Los dos extremos que propone el cálculo automático. Marfil y carbón en
    vez de blanco y negro puros: mantienen la calidez de la aplicación y
    siguen dando contraste de sobra. */
const LIGHT_INK = '#faf7f0'
const DARK_INK = '#2c3038'

/** Fallback si `data.fill` no es un color reconocible. */
export const DEFAULT_SHAPE_FILL = '#2f6188'

/* ─────────────────────── Contraste real (WCAG 2.1) ─────────────────────── */

function parseHex(color: string): [number, number, number] | null {
  const hex = color.trim().replace(/^#/, '')
  const full =
    hex.length === 3
      ? hex
          .split('')
          .map((c) => c + c)
          .join('')
      : hex
  if (!/^[0-9a-fA-F]{6}$/.test(full)) return null
  return [
    parseInt(full.slice(0, 2), 16),
    parseInt(full.slice(2, 4), 16),
    parseInt(full.slice(4, 6), 16),
  ]
}

/** Luminancia relativa, fórmula WCAG 2.1 — no un promedio de canales ni el
    nombre del color, que es justo lo que el requisito prohíbe. */
export function relativeLuminance(color: string): number | null {
  const rgb = parseHex(color)
  if (!rgb) return null
  const [r, g, b] = rgb.map((channel) => {
    const c = channel / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** Razón de contraste entre dos colores (1 a 21). */
export function contrastRatio(a: string, b: string): number | null {
  const la = relativeLuminance(a)
  const lb = relativeLuminance(b)
  if (la === null || lb === null) return null
  const [hi, lo] = la > lb ? [la, lb] : [lb, la]
  return (hi + 0.05) / (lo + 0.05)
}

/**
 * Color de texto sugerido para un fondo dado.
 *
 * Se calcula el contraste REAL contra los dos candidatos y gana el mayor,
 * no una regla del tipo "si el color se llama oscuro, texto blanco". El
 * ocre (#c9a227) lo ilustra: es un amarillo, que suena "claro", pero el
 * marfil sobre él da 2.3:1 y el carbón 5.5:1 — solo el cálculo distingue
 * eso.
 *
 * Marfil y carbón antes que blanco y negro puros, por armonía visual; si
 * ninguno de los dos llegara a 4.5:1 (AA para texto normal) se recurre al
 * extremo puro que corresponda, porque la legibilidad manda sobre la
 * estética.
 */
export function suggestTextColor(fill: string): string {
  const withLight = contrastRatio(fill, LIGHT_INK)
  const withDark = contrastRatio(fill, DARK_INK)

  // Color no interpretable (p. ej. un `var()` heredado): se deja el oscuro,
  // que es legible sobre la mayoría de los fondos de tablero.
  if (withLight === null || withDark === null) return DARK_INK

  const best = withLight >= withDark ? LIGHT_INK : DARK_INK
  const bestRatio = Math.max(withLight, withDark)
  if (bestRatio >= 4.5) return best

  // Ningún tono cálido llega a AA: se cae al extremo puro del mismo lado.
  return best === LIGHT_INK ? '#ffffff' : '#000000'
}

/** ¿La pareja alcanza el AA de WCAG para texto normal? Lo usa la interfaz
    para avisar cuando el usuario elige a mano una combinación ilegible. */
export function meetsAA(fill: string, text: string): boolean {
  const ratio = contrastRatio(fill, text)
  return ratio !== null && ratio >= 4.5
}

/* ─────────────────────────── Lectura de `data` ─────────────────────────── */

/** Un color válido guardado en `data`, o `undefined` si no hay ninguno —
    en ese caso la forma hereda el color del tema del tablero, como siempre. */
export function shapeColorOf(data: Record<string, unknown>, key: 'fill' | 'textColor'): string | undefined {
  const value = data[key]
  if (typeof value !== 'string') return undefined
  return parseHex(value) ? value : undefined
}

/** Normaliza lo que devuelve un `<input type="color">`, que siempre da
    `#rrggbb` en minúsculas pero puede llegar vacío si se cancela. */
export function normalizeColor(value: string): string | null {
  return parseHex(value) ? value.toLowerCase() : null
}
