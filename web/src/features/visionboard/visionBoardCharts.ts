/**
 * 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva): CHART
 * es puramente decorativo por ahora — datos de ejemplo fijos (no editables
 * todavía, mismo alcance que STICKER: se elige un tipo y se coloca), igual
 * que visionBoardShapes.ts's `path`, el "dibujo" real vive en un solo lugar
 * (`buildChartSvg`) y VisionBoardElementView.tsx solo lo pega dentro de un
 * `<svg viewBox="0 0 W H">`. Deliberadamente 6 tipos, no los 9 originalmente
 * mencionados — dispersión/jerarquía/barras-animadas/infografía quedan
 * fuera de este incremento (más costo de implementación real por menos
 * valor claro que este set); ver VisionBoardElementType.java's propio doc
 * comment sobre qué sí quedó fuera de esta fase y por qué.
 */
export type VisionBoardChartType = 'line' | 'bar' | 'pie' | 'donut' | 'area' | 'gauge'

export const CHART_TYPE_OPTIONS: Array<{ id: VisionBoardChartType; label: string }> = [
  { id: 'bar', label: 'Barras' },
  { id: 'line', label: 'Línea' },
  { id: 'area', label: 'Área' },
  { id: 'pie', label: 'Pastel' },
  { id: 'donut', label: 'Anillos' },
  { id: 'gauge', label: 'Medidor' },
]

const SAMPLE_SERIES = [12, 19, 14, 24, 18, 30, 22]
const SAMPLE_SLICES = [30, 25, 20, 15, 10]
const SLICE_COLORS = ['#2c5f8c', '#b47b48', '#4d6b46', '#8a5a12', '#2f5f66']

function scaleY(v: number, max: number, h: number): number {
  return h - (v / max) * h
}
function arcPoint(cx: number, cy: number, r: number, angle: number): [number, number] {
  return [cx + r * Math.cos(angle), cy + r * Math.sin(angle)]
}
function arcPath(cx: number, cy: number, r: number, a0: number, a1: number): string {
  const [x0, y0] = arcPoint(cx, cy, r, a0)
  const [x1, y1] = arcPoint(cx, cy, r, a1)
  const large = a1 - a0 > Math.PI ? 1 : 0
  return `M${x0.toFixed(1)} ${y0.toFixed(1)} A${r} ${r} 0 ${large} 1 ${x1.toFixed(1)} ${y1.toFixed(1)}`
}
function pieSlicePath(cx: number, cy: number, r: number, a0: number, a1: number): string {
  return `M${cx} ${cy} L${arcPath(cx, cy, r, a0, a1).slice(1)} Z`
}

function lineOrArea(kind: 'line' | 'area'): { viewBox: string; markup: string } {
  const w = 200
  const h = 100
  const max = Math.max(...SAMPLE_SERIES) * 1.15
  const step = w / (SAMPLE_SERIES.length - 1)
  const points = SAMPLE_SERIES.map((v, i) => `${(i * step).toFixed(1)},${scaleY(v, max, h).toFixed(1)}`)
  if (kind === 'area') {
    const d = `M0,${h} L${points.join(' L')} L${w},${h} Z`
    return {
      viewBox: `0 0 ${w} ${h}`,
      markup: `<path d="${d}" fill="#2c5f8c" fill-opacity="0.18" /><polyline points="${points.join(' ')}" fill="none" stroke="#2c5f8c" stroke-width="2.5" />`,
    }
  }
  const dots = points
    .map((p) => {
      const [x, y] = p.split(',')
      return `<circle cx="${x}" cy="${y}" r="3" fill="#2c5f8c" />`
    })
    .join('')
  return {
    viewBox: `0 0 ${w} ${h}`,
    markup: `<polyline points="${points.join(' ')}" fill="none" stroke="#2c5f8c" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />${dots}`,
  }
}

function bar(): { viewBox: string; markup: string } {
  const w = 200
  const h = 100
  const max = Math.max(...SAMPLE_SERIES) * 1.15
  const gap = 6
  const bw = (w - gap * (SAMPLE_SERIES.length + 1)) / SAMPLE_SERIES.length
  const bars = SAMPLE_SERIES.map((v, i) => {
    const bh = (v / max) * h
    const x = gap + i * (bw + gap)
    const y = h - bh
    return `<rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${bw.toFixed(1)}" height="${bh.toFixed(1)}" rx="2" fill="#b47b48" />`
  }).join('')
  return { viewBox: `0 0 ${w} ${h}`, markup: bars }
}

function pieOrDonut(kind: 'pie' | 'donut'): { viewBox: string; markup: string } {
  const cx = 55
  const cy = 55
  const r = 48
  const total = SAMPLE_SLICES.reduce((a, b) => a + b, 0)
  let angle = -Math.PI / 2
  const slices = SAMPLE_SLICES.map((v, i) => {
    const next = angle + (v / total) * Math.PI * 2
    const d = pieSlicePath(cx, cy, r, angle, next)
    angle = next
    return `<path d="${d}" fill="${SLICE_COLORS[i % SLICE_COLORS.length]}" />`
  }).join('')
  const hole = kind === 'donut' ? `<circle cx="${cx}" cy="${cy}" r="24" fill="var(--vb-surface, #fff)" />` : ''
  return { viewBox: '0 0 110 110', markup: slices + hole }
}

function gauge(): { viewBox: string; markup: string } {
  const cx = 55
  const cy = 55
  const r = 44
  const pct = 0.68
  const full = arcPath(cx, cy, r, Math.PI, Math.PI * 2)
  const value = arcPath(cx, cy, r, Math.PI, Math.PI + pct * Math.PI)
  return {
    viewBox: '0 0 110 65',
    markup: `<path d="${full}" fill="none" stroke="#e4eef6" stroke-width="10" stroke-linecap="round" /><path d="${value}" fill="none" stroke="#2c5f8c" stroke-width="10" stroke-linecap="round" />`,
  }
}

/** Devuelve el `viewBox` correcto y el markup SVG interno (sin la etiqueta
    `<svg>` — VisionBoardElementView.tsx la envuelve) para el tipo pedido.
    `chartType` inválido/`undefined` cae a 'bar' (mismo "valor por defecto
    razonable" que shapeDefOf ya usa para `shape` inválido). */
export function buildChart(chartType: string | undefined): { viewBox: string; markup: string } {
  switch (chartType) {
    case 'line':
      return lineOrArea('line')
    case 'area':
      return lineOrArea('area')
    case 'pie':
      return pieOrDonut('pie')
    case 'donut':
      return pieOrDonut('donut')
    case 'gauge':
      return gauge()
    case 'bar':
    default:
      return bar()
  }
}
