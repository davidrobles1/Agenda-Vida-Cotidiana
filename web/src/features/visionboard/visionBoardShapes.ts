/**
 * BLOQUE C (post-MVP): the SHAPE catalog, expanded from the original 3
 * (rectangle/circle/line — kept exactly as they were, still pure CSS in
 * VisionBoardCanvas.module.css's `.elementShape*` classes, zero risk of
 * regressing them) to the full set this phase asks for.
 *
 * Every new shape is ONE SVG path (`d`), authored once in a shared 0–100
 * viewBox, and reused for both real renders instead of maintaining two
 * divergent shape implementations:
 * - on screen: an inline `<svg viewBox="0 0 100 100"><path d={d} /></svg>`
 *   (VisionBoardElementView.tsx's `ShapeElementContent`), scaled to the
 *   element's own width/height by the SVG viewport itself — rotation/
 *   resize/layers all already work because they're the same generic
 *   per-element transform/zIndex mechanics every other type already gets,
 *   nothing shape-specific to add there.
 * - export: `new Path2D(d)` filled on the off-screen canvas after scaling
 *   the 2D context to width/height (visionBoardExport.ts's `drawElement`),
 *   the exact same path data, so the exported image always matches the
 *   on-screen shape pixel-for-pixel (mod scale), never a second hand-drawn
 *   approximation of it.
 *
 * `path` is `undefined` for rectangle/circle/line/capsule —
 * VisionBoardElementView.tsx keeps rendering those exactly as before.
 *
 * 2026-08-22 (pedido explícito del usuario, "sacar el jugo de Canva"):
 * ampliado de ~30 a un catálogo real por familias, generado en su mayoría
 * (polígonos/estrellas/engranajes/orgánicas) en vez de escrito a mano
 * forma por forma — ver los generadores más abajo. NINGÚN `id` existente
 * cambió ni se eliminó: los tableros ya guardados referencian estos ids en
 * `data.shape`, así que esto es puramente aditivo. Deliberadamente NO se
 * incluyó aquí el sistema de 16 estilos de línea (guiones/doble/ondulada…)
 * que sí existe en el prototipo — eso necesitaría un renderer de trazo
 * nuevo (dasharray/marcadores), no solo datos de catálogo; queda como
 * incremento futuro si se decide construirlo.
 */
export interface VisionBoardShapeDef {
  id: string
  label: string
  /** SVG path `d`, authored in a 0–100 x 0–100 viewBox. `undefined` for
      the 4 native CSS variants (rectangle/circle/line/capsule). */
  path?: string
  /** `evenodd` for "outer shape minus inner cutout" paths (moon's
      crescent, frame's window, rings) — both renderers (SVG `fill-rule`
      and canvas `ctx.fill(path2d, 'evenodd')`) need it or the cutout
      fills solid instead of punching through. */
  fillRule?: 'evenodd'
  /** Agrupador para el picker (pestañas por familia) — puramente de UI,
      no afecta el renderizado. */
  family: ShapeFamilyId
}

export type ShapeFamilyId =
  | 'basic'
  | 'polygons'
  | 'stars'
  | 'arrows'
  | 'flowchart'
  | 'speech'
  | 'clouds'
  | 'hearts'
  | 'banners'
  | 'teardrops'
  | 'gears'
  | 'asterisks'
  | 'organic'
  | 'abstract'

export const SHAPE_FAMILIES: Array<{ id: ShapeFamilyId; label: string }> = [
  { id: 'basic', label: 'Básicas' },
  { id: 'polygons', label: 'Polígonos' },
  { id: 'stars', label: 'Estrellas' },
  { id: 'arrows', label: 'Flechas' },
  { id: 'flowchart', label: 'Diagrama de flujo' },
  { id: 'speech', label: 'Diálogo' },
  { id: 'clouds', label: 'Nubes' },
  { id: 'hearts', label: 'Corazones' },
  { id: 'banners', label: 'Banners' },
  { id: 'teardrops', label: 'Lágrimas' },
  { id: 'gears', label: 'Engranajes' },
  { id: 'asterisks', label: 'Asteriscos' },
  { id: 'organic', label: 'Orgánicas' },
  { id: 'abstract', label: 'Abstractas' },
]

/* ============ generadores paramétricos — geometría real calculada una
   vez al cargar el módulo, en vez de escribir a mano decenas de rutas SVG
   (mismo criterio que ya explica el doc comment de arriba para el resto
   del catálogo). ============ */
function polygonPath(sides: number, rot = -90): string {
  const pts: string[] = []
  for (let i = 0; i < sides; i++) {
    const a = ((rot + (360 / sides) * i) * Math.PI) / 180
    pts.push(`${(50 + 46 * Math.cos(a)).toFixed(2)} ${(50 + 46 * Math.sin(a)).toFixed(2)}`)
  }
  return `M${pts.join(' L')} Z`
}
function starPath(points: number, innerRatio: number, rot = -90): string {
  const pts: string[] = []
  for (let i = 0; i < points * 2; i++) {
    const r = i % 2 === 0 ? 46 : 46 * innerRatio
    const a = ((rot + (360 / (points * 2)) * i) * Math.PI) / 180
    pts.push(`${(50 + r * Math.cos(a)).toFixed(2)} ${(50 + r * Math.sin(a)).toFixed(2)}`)
  }
  return `M${pts.join(' L')} Z`
}
function gearPath(teeth: number, outerR: number, innerR: number, boreR: number): string {
  const pts: string[] = []
  const step = 360 / (teeth * 4)
  for (let i = 0; i < teeth * 4; i++) {
    const phase = i % 4
    const r = phase === 0 || phase === 1 ? outerR : innerR
    const a = (step * i * Math.PI) / 180
    pts.push(`${(50 + r * Math.cos(a)).toFixed(2)} ${(50 + r * Math.sin(a)).toFixed(2)}`)
  }
  return (
    `M${pts.join(' L')} Z ` +
    `M${50 + boreR} 50 A${boreR} ${boreR} 0 1 0 ${50 - boreR} 50 A${boreR} ${boreR} 0 1 0 ${50 + boreR} 50 Z`
  )
}
function blobPath(jitter: number[]): string {
  const n = jitter.length
  const pts: [number, number][] = []
  for (let i = 0; i < n; i++) {
    const r = 40 * jitter[i]
    const a = ((360 / n) * i * Math.PI) / 180
    pts.push([50 + r * Math.cos(a), 50 + r * Math.sin(a)])
  }
  let d = `M${pts[0][0].toFixed(1)} ${pts[0][1].toFixed(1)}`
  for (let j = 0; j < n; j++) {
    const p0 = pts[j]
    const p1 = pts[(j + 1) % n]
    d += ` Q${p0[0].toFixed(1)} ${p0[1].toFixed(1)} ${((p0[0] + p1[0]) / 2).toFixed(1)} ${((p0[1] + p1[1]) / 2).toFixed(1)}`
  }
  return `${d} Z`
}

export const SHAPE_CATALOG: VisionBoardShapeDef[] = [
  // ===== Básicas (nativas + path) =====
  { id: 'rectangle', label: 'Rectángulo', family: 'basic' },
  { id: 'circle', label: 'Círculo', family: 'basic' },
  { id: 'line', label: 'Línea', family: 'basic' },
  { id: 'capsule', label: 'Cápsula', family: 'basic' },
  { id: 'triangle', label: 'Triángulo', path: 'M50 5 L89 72.5 L11 72.5 Z', family: 'basic' },
  { id: 'rhombus', label: 'Rombo', path: 'M50 5 L95 50 L50 95 L5 50 Z', family: 'basic' },
  { id: 'cross', label: 'Cruz', path: 'M35 10 H65 V35 H90 V65 H65 V90 H35 V65 H10 V35 H35 Z', family: 'basic' },
  { id: 'check', label: 'Check', path: 'M8 55 L33 80 L92 15 L80 6 L33 60 L18 43 Z', family: 'basic' },
  { id: 'x', label: 'X', path: 'M15 8 L35 8 L50 35 L65 8 L85 8 L58 50 L85 92 L65 92 L50 65 L35 92 L15 92 L42 50 Z', family: 'basic' },
  { id: 'roundedRect', label: 'Rect. redondeado', path: 'M15 5 H85 A10 10 0 0 1 95 15 V85 A10 10 0 0 1 85 95 H15 A10 10 0 0 1 5 85 V15 A10 10 0 0 1 15 5 Z', family: 'basic' },
  { id: 'trapezoid', label: 'Trapecio', path: 'M20 20 H80 L95 80 H5 Z', family: 'basic' },
  { id: 'parallelogram', label: 'Paralelogramo', path: 'M25 20 H95 L75 80 H5 Z', family: 'basic' },
  { id: 'chevron', label: 'Chevrón', path: 'M10 15 L50 50 L10 85 L25 85 L65 50 L25 15 Z', family: 'basic' },
  { id: 'halfCircle', label: 'Medio círculo', path: 'M5 50 A45 45 0 0 1 95 50 Z', family: 'basic' },
  { id: 'ring', label: 'Anillo', path: 'M50 5 A45 45 0 1 0 50.01 5 Z M50 25 A25 25 0 1 1 49.99 25 Z', fillRule: 'evenodd', family: 'basic' },

  // ===== Polígonos (pentágono/hexágono/octágono ya existían) =====
  { id: 'pentagon', label: 'Pentágono', path: 'M50 5 L92.8 36.1 L76.5 86.4 L23.5 86.4 L7.2 36.1 Z', family: 'polygons' },
  { id: 'hexagon', label: 'Hexágono', path: 'M50 5 L89 27.5 L89 72.5 L50 95 L11 72.5 L11 27.5 Z', family: 'polygons' },
  { id: 'heptagon', label: 'Heptágono', path: polygonPath(7), family: 'polygons' },
  { id: 'octagon', label: 'Octágono', path: 'M67.2 8.4 L91.6 32.8 L91.6 67.2 L67.2 91.6 L32.8 91.6 L8.4 67.2 L8.4 32.8 L32.8 8.4 Z', family: 'polygons' },
  { id: 'nonagon', label: 'Eneágono', path: polygonPath(9), family: 'polygons' },
  { id: 'decagon', label: 'Decágono', path: polygonPath(10), family: 'polygons' },
  { id: 'dodecagon', label: 'Dodecágono', path: polygonPath(12), family: 'polygons' },

  // ===== Estrellas =====
  { id: 'star', label: 'Estrella', path: 'M50 5 L60.6 35.4 L92.8 36.1 L67.1 55.6 L76.5 86.4 L50 68 L23.5 86.4 L32.9 55.6 L7.2 36.1 L39.4 35.4 Z', family: 'stars' },
  { id: 'star4', label: 'Estrella 4 puntas', path: starPath(4, 0.35), family: 'stars' },
  { id: 'star6', label: 'Estrella 6 puntas', path: starPath(6, 0.55), family: 'stars' },
  { id: 'star6david', label: 'Estrella 6 (David)', path: starPath(6, 0.58), family: 'stars' },
  { id: 'star7', label: 'Estrella 7 puntas', path: starPath(7, 0.5), family: 'stars' },
  { id: 'star8', label: 'Estrella 8 puntas', path: starPath(8, 0.5), family: 'stars' },
  { id: 'star8fat', label: 'Estrella 8 gruesa', path: starPath(8, 0.72), family: 'stars' },
  { id: 'star10', label: 'Estrella 10 puntas', path: starPath(10, 0.5), family: 'stars' },
  { id: 'star12', label: 'Estrella 12 puntas', path: starPath(12, 0.55), family: 'stars' },
  { id: 'starBurst', label: 'Destello', path: starPath(16, 0.25), family: 'stars' },

  // ===== Flechas =====
  { id: 'arrowRight', label: 'Flecha derecha', path: 'M10 30 L60 30 L60 10 L95 50 L60 90 L60 70 L10 70 Z', family: 'arrows' },
  { id: 'arrowLeft', label: 'Flecha izquierda', path: 'M90 30 L40 30 L40 10 L5 50 L40 90 L40 70 L90 70 Z', family: 'arrows' },
  { id: 'arrowUp', label: 'Flecha arriba', path: 'M30 90 L30 40 L10 40 L50 5 L90 40 L70 40 L70 90 Z', family: 'arrows' },
  { id: 'arrowDown', label: 'Flecha abajo', path: 'M30 10 L30 60 L10 60 L50 95 L90 60 L70 60 L70 10 Z', family: 'arrows' },
  { id: 'arrowDoubleH', label: 'Doble flecha ↔', path: 'M5 50 L25 30 L25 42 L75 42 L75 30 L95 50 L75 70 L75 58 L25 58 L25 70 Z', family: 'arrows' },
  { id: 'arrowDoubleV', label: 'Doble flecha ↕', path: 'M50 5 L30 25 L42 25 L42 75 L30 75 L50 95 L70 75 L58 75 L58 25 L70 25 Z', family: 'arrows' },
  { id: 'arrowChevronBlock', label: 'Flecha muesca', path: 'M10 20 H55 L80 50 L55 80 H10 L35 50 Z', family: 'arrows' },

  // ===== Diagrama de flujo (nuevo) =====
  { id: 'flowDecision', label: 'Decisión', path: 'M50 5 L95 50 L50 95 L5 50 Z', family: 'flowchart' },
  { id: 'flowDocument', label: 'Documento', path: 'M10 10 H90 V70 Q70 90 50 75 Q30 60 10 80 Z', family: 'flowchart' },
  { id: 'flowPredefined', label: 'Proceso predef.', path: 'M5 10 H95 V90 H5 Z M15 10 H21 V90 H15 Z M79 10 H85 V90 H79 Z', family: 'flowchart' },
  { id: 'flowManualInput', label: 'Entrada manual', path: 'M5 30 L20 10 H95 V90 H5 Z', family: 'flowchart' },
  { id: 'flowDelay', label: 'Retraso', path: 'M10 10 H55 A40 40 0 0 1 55 90 H10 Z', family: 'flowchart' },
  { id: 'flowDisplay', label: 'Pantalla', path: 'M10 10 H55 Q95 10 95 50 Q95 90 55 90 H10 Z', family: 'flowchart' },

  // ===== Diálogo =====
  { id: 'speechBubble', label: 'Globo de diálogo', path: 'M10 12 H90 A5 5 0 0 1 95 17 V63 A5 5 0 0 1 90 68 H38 L22 88 L26 68 H10 A5 5 0 0 1 5 63 V17 A5 5 0 0 1 10 12 Z', family: 'speech' },
  { id: 'speechRight', label: 'Diálogo derecho', path: 'M90 12 H10 A5 5 0 0 0 5 17 V63 A5 5 0 0 0 10 68 H62 L78 88 L74 68 H90 A5 5 0 0 0 95 63 V17 A5 5 0 0 0 90 12 Z', family: 'speech' },
  { id: 'thoughtBubble', label: 'Nube de idea', path: 'M26 60 A16 16 0 0 1 22 30 A20 20 0 0 1 61 22 A17 17 0 0 1 82 42 A14 14 0 0 1 78 60 Z M30 72 A6 6 0 1 1 29.9 72 Z M20 88 A3 3 0 1 1 19.9 88 Z', family: 'speech' },
  { id: 'captionBox', label: 'Recuadro con guía', path: 'M8 10 H92 V65 H58 L50 88 L42 65 H8 Z', family: 'speech' },

  // ===== Nubes =====
  { id: 'cloud', label: 'Nube', path: 'M26 72 A16 16 0 0 1 22 40.5 A20 20 0 0 1 61 30 A17 17 0 0 1 82 50 A14 14 0 0 1 78 72 Z', family: 'clouds' },
  { id: 'cloud2', label: 'Nube esponjosa', path: 'M20 75 A14 14 0 0 1 18 48 A17 17 0 0 1 40 32 A15 15 0 0 1 60 28 A18 18 0 0 1 85 45 A13 13 0 0 1 80 75 Z', family: 'clouds' },
  { id: 'cloud3', label: 'Nube compacta', path: 'M30 70 A12 12 0 0 1 28 46 A15 15 0 0 1 55 35 A13 13 0 0 1 75 52 A11 11 0 0 1 72 70 Z', family: 'clouds' },

  // ===== Corazones =====
  { id: 'heart', label: 'Corazón', path: 'M50 90 C10 62 5 35 22 20 C34 10 47 15 50 28 C53 15 66 10 78 20 C95 35 90 62 50 90 Z', family: 'hearts' },
  { id: 'heart2', label: 'Corazón redondo', path: 'M50 88 C15 65 8 40 25 24 C36 14 48 18 50 30 C52 18 64 14 75 24 C92 40 85 65 50 88 Z', family: 'hearts' },
  { id: 'heartBroken', label: 'Corazón roto', path: 'M48 90 C15 65 8 40 22 24 C32 14 44 18 48 28 Z M52 90 C85 65 92 40 78 24 C68 14 56 18 52 28 Z', family: 'hearts' },

  // ===== Banners =====
  { id: 'label', label: 'Etiqueta', path: 'M12 50 L32 12 H88 V88 H32 Z', family: 'banners' },
  { id: 'ribbon', label: 'Cinta', path: 'M10 22 H90 L78 50 L90 78 H10 L22 50 Z', family: 'banners' },
  { id: 'flag', label: 'Bandera', path: 'M14 5 H20 V95 H14 Z M20 10 H86 L71 27 L86 44 H20 Z', family: 'banners' },
  { id: 'scroll', label: 'Pergamino', path: 'M10 30 Q5 20 15 18 H85 Q95 20 90 30 V70 Q95 80 85 82 H15 Q5 80 10 70 Z', family: 'banners' },
  { id: 'pennant', label: 'Banderín', path: 'M8 15 H88 L60 50 L88 85 H8 Z', family: 'banners' },

  // ===== Lágrimas =====
  { id: 'drop', label: 'Gota', path: 'M50 5 C68 32 85 53 85 68 A35 35 0 1 1 15 68 C15 53 32 32 50 5 Z', family: 'teardrops' },
  { id: 'dropWide', label: 'Gota ancha', path: 'M50 10 C72 35 88 55 88 70 A38 38 0 1 1 12 70 C12 55 28 35 50 10 Z', family: 'teardrops' },
  { id: 'dropInverted', label: 'Gota invertida', path: 'M50 95 C32 68 15 47 15 32 A35 35 0 1 1 85 32 C85 47 68 68 50 95 Z', family: 'teardrops' },

  // ===== Engranajes (nuevo, generado) =====
  { id: 'gear6', label: 'Engranaje 6', path: gearPath(6, 44, 34, 12), fillRule: 'evenodd', family: 'gears' },
  { id: 'gear8', label: 'Engranaje 8', path: gearPath(8, 44, 36, 14), fillRule: 'evenodd', family: 'gears' },
  { id: 'gear10', label: 'Engranaje 10', path: gearPath(10, 44, 37, 15), fillRule: 'evenodd', family: 'gears' },
  { id: 'gear12', label: 'Engranaje 12', path: gearPath(12, 43, 38, 16), fillRule: 'evenodd', family: 'gears' },

  // ===== Asteriscos (nuevo, generado) =====
  { id: 'asterisk4', label: 'Destello 4', path: starPath(4, 0.1), family: 'asterisks' },
  { id: 'asterisk5', label: 'Asterisco 5', path: starPath(5, 0.15), family: 'asterisks' },
  { id: 'asterisk6', label: 'Asterisco 6', path: starPath(6, 0.12), family: 'asterisks' },

  // ===== Orgánicas (nuevo, generado) =====
  { id: 'leaf', label: 'Hoja', path: 'M50 5 C85 20 90 60 50 95 C10 60 15 20 50 5 Z', family: 'organic' },
  { id: 'blob1', label: 'Orgánica 1', path: blobPath([1.0, 0.82, 1.1, 0.75, 1.05, 0.88, 0.95, 0.8]), family: 'organic' },
  { id: 'blob2', label: 'Orgánica 2', path: blobPath([0.9, 1.1, 0.85, 1.15, 0.9, 1.05, 0.8, 1.0]), family: 'organic' },
  { id: 'blob3', label: 'Orgánica 3', path: blobPath([1.1, 0.9, 1.05, 0.78, 1.12, 0.85, 1.0, 0.92]), family: 'organic' },
  { id: 'blob4', label: 'Orgánica 4', path: blobPath([0.85, 1.05, 0.95, 1.1, 0.8, 1.0, 1.08, 0.88]), family: 'organic' },

  // ===== Abstractas =====
  { id: 'diamond', label: 'Diamante', path: 'M20 35 L35 10 L65 10 L80 35 L50 95 Z', family: 'abstract' },
  { id: 'lightning', label: 'Rayo', path: 'M55 5 L20 55 H42 L35 95 L82 40 H55 Z', family: 'abstract' },
  { id: 'mountain', label: 'Montaña', path: 'M5 90 L30 40 L45 60 L65 20 L95 90 Z', family: 'abstract' },
  { id: 'sun', label: 'Sol', path: 'M28 50 A22 22 0 1 0 72 50 A22 22 0 1 0 28 50 Z M75.7 45.9 L96 50 L75.7 54.1 Z M71 65.3 L82.5 82.5 L65.3 71 Z M54.1 75.7 L50 96 L45.9 75.7 Z M34.7 71 L17.5 82.5 L29 65.3 Z M24.3 54.1 L4 50 L24.3 45.9 Z M29 34.7 L17.5 17.5 L34.7 29 Z M45.9 24.3 L50 4 L54.1 24.3 Z M65.3 29 L82.5 17.5 L71 34.7 Z', family: 'abstract' },
  { id: 'moon', label: 'Luna', path: 'M50 5 A45 45 0 1 0 50 95 A35 35 0 1 1 50 5 Z', fillRule: 'evenodd', family: 'abstract' },
  { id: 'frame', label: 'Marco', path: 'M5 5 H95 V95 H5 Z M20 20 V80 H80 V20 Z', fillRule: 'evenodd', family: 'abstract' },
  { id: 'divider', label: 'Divisor decorativo', path: 'M5 46 H38 V54 H5 Z M62 46 H95 V54 H62 Z M50 36 L60 46 L50 56 L40 46 Z', family: 'abstract' },
  { id: 'quarterCircle', label: 'Cuarto de círculo', path: 'M5 95 L5 5 A90 90 0 0 1 95 95 Z', family: 'abstract' },
  { id: 'vennCircles', label: 'Círculos superpuestos', path: 'M35 30 A25 25 0 1 0 35.01 30 Z M65 30 A25 25 0 1 0 65.01 30 Z', family: 'abstract' },
]

export function shapeDefOf(id: string): VisionBoardShapeDef {
  return SHAPE_CATALOG.find((s) => s.id === id) ?? SHAPE_CATALOG[0]
}

export function shapesByFamily(family: ShapeFamilyId): VisionBoardShapeDef[] {
  return SHAPE_CATALOG.filter((s) => s.family === family)
}
