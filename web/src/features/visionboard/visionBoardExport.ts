import { findStickerOption } from '../../core/ui/pickers/pickerCatalog'
import { shapeVariantOf, type VisionBoard, type VisionBoardElement, type VisionBoardThemeId } from './api'
import { buildChart } from './visionBoardCharts'
import { findEmojiOption } from './visionBoardEmojis'
import { frameStyleOf, type VisionBoardFrameStyle } from './visionBoardFrames'
import { GRID_CELL_COLORS, gridLayoutOf } from './visionBoardGrids'
import { resolveVisionBoardImageSrc } from './visionBoardImages'
import { shapeDefOf } from './visionBoardShapes'
import { visionBoardThemeOf } from './visionBoardThemes'

/**
 * FASE 14: PNG/PDF export, built entirely on native browser APIs — no
 * html2canvas/jspdf/etc (none were already installed, see package.json,
 * and the phase's own rule is to only add a dependency if this genuinely
 * can't be done without one). Every element's real x/y/width/height/
 * rotation/zIndex/data is already known client-side (the same `elements`
 * VisionBoardCanvas.tsx renders), so rather than rasterizing the live,
 * interactive DOM (which would also have to somehow hide the toolbar/
 * handles/guides and undo the zoom transform), this draws the board
 * straight onto an off-screen `<canvas>` from that same data — the
 * export is a second, independent renderer of the same model, not a
 * screenshot of the UI. That's what keeps it free of toolbar/selection/
 * handles/Smart Guides/zoom by construction, and independent of the
 * user's current zoom/pan.
 *
 * BLOQUE H (post-MVP): PDF used to mean "open the rendered canvas in a
 * dedicated print-only window and invoke the browser's native print
 * dialog — 'Guardar como PDF' from there is the actual export." That was
 * non-deterministic by construction (a human had to notice the right
 * print destination, actually choose it, and its output filename/margins/
 * scale all fell out of the OS print pipeline, not this code) and
 * impossible to script — it needed a real, undismissable browser dialog.
 * Replaced with a genuine direct download: the same rendered canvas is
 * embedded as an image into a real PDF byte stream, built with `jspdf`
 * (the one new dependency this file has ever needed — hand-rolling PDF
 * bytes by hand was correctly ruled out before, and remains ruled out;
 * `jspdf` does that encoding, not this code) and saved straight to disk
 * via `doc.save()`, same one-click, no-dialog shape PNG/JPG already have.
 *
 * FASE 19 audit: PNG/PDF/rotation/zIndex/theme/locked-doesn't-block-export
 * were all already correct against the real model (verified against
 * VisionBoardElementEditor.tsx/VisionBoardElementView.tsx/CSS — TEXT/NOTE/
 * SHAPE have no per-element color/font/border/opacity fields to lose in
 * the first place, so the fixed styling here already matches 100% of what
 * the editor exposes). The one real gap was JPG — the original plan asked
 * for PNG/JPG/PDF but only PNG/PDF existed. Added `canvasToJpegBlob` below,
 * reusing `renderVisionBoardToCanvas` unchanged — same renderer, same
 * `<canvas>`, `toBlob('image/jpeg', quality)` instead of `'image/png'`.
 */

const EXPORT_FONT_FAMILY = "'Inter', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
const STICKER_FIT_SCALE = 0.7
const MAX_EXPORT_SCALE = 2
/** Matches `--radius-control`'s own default (index.css) — Board Themes
    (FASE 16) only ever define colors, not shape, so this stays a plain
    constant rather than something read from a theme. */
const EXPORT_RADIUS_CONTROL = 8

export interface VisionBoardExportColors {
  surface: string
  primaryContainer: string
  warningContainer: string
  text: string
  radiusControl: number
}

/**
 * FASE 16: colors now come from the board's own Board Theme
 * (visionBoardThemes.ts), not from reading the live DOM's computed
 * `--color-*` custom properties — those belong to the *app* theme, which
 * is explicitly independent of the Board Theme a board is exported with.
 * This also means export no longer needs any DOM reference at all.
 */
export function exportColorsForTheme(themeId: VisionBoardThemeId): VisionBoardExportColors {
  const { colors } = visionBoardThemeOf(themeId)
  return {
    // The whole canvas's own fill — matches `.canvas`'s `--vb-surface-variant`
    // on screen, not the (slightly different) per-element `--vb-surface` card tone.
    surface: colors.surfaceVariant,
    primaryContainer: colors.shapeBackground,
    warningContainer: colors.noteBackground,
    text: colors.text,
    radiusControl: EXPORT_RADIUS_CONTROL,
  }
}

function loadImage(url: string, crossOrigin?: boolean): Promise<HTMLImageElement | null> {
  return new Promise((resolve) => {
    const img = new Image()
    // Only set for the external IMAGE type (VisionBoardExportAction never
    // sets it for same-origin sticker assets): with `crossorigin` set and
    // no CORS grant from the remote host, the browser fails the *load*
    // outright (caught by onerror below) instead of silently tainting the
    // canvas — a taint would only surface later, as a SecurityError on
    // the *whole* canvas at toBlob/toDataURL time, by which point there's
    // no way to tell which image caused it or to recover just that one
    // element.
    if (crossOrigin) img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = () => resolve(null)
    img.src = url
  })
}

function textOf(data: Record<string, unknown>): string {
  return typeof data.text === 'string' ? data.text : ''
}

function drawRoundedRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  radius: number,
  fillStyle: string,
) {
  const r = Math.max(0, Math.min(radius, w / 2, h / 2))
  ctx.fillStyle = fillStyle
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.arcTo(x + w, y, x + w, y + r, r)
  ctx.lineTo(x + w, y + h - r)
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r)
  ctx.lineTo(x + r, y + h)
  ctx.arcTo(x, y + h, x, y + h - r, r)
  ctx.lineTo(x, y + r)
  ctx.arcTo(x, y, x + r, y, r)
  ctx.closePath()
  ctx.fill()
}

/** BLOQUE B (post-MVP): same editorial "broken image" treatment as
    VisionBoardCanvas.module.css's `.elementImageBroken` (dashed tinted box
    + a discreet message), reimplemented in canvas primitives since the
    export is a second, independent renderer of the same model, not a DOM
    screenshot (see this file's own top-of-file doc comment) — no lucide
    icon here (canvas can't draw an SVG icon component directly), just the
    box + message, which alone already satisfies "no dejar cuadros vacíos"
    in the exported image. */
function drawBrokenImagePlaceholder(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  colors: VisionBoardExportColors,
) {
  drawRoundedRect(ctx, 0, 0, width, height, colors.radiusControl, colors.surface)
  ctx.save()
  ctx.strokeStyle = colors.text
  ctx.globalAlpha = 0.25
  ctx.setLineDash([5, 4])
  ctx.lineWidth = 1
  ctx.strokeRect(1, 1, Math.max(0, width - 2), Math.max(0, height - 2))
  ctx.restore()
  drawTextBlock(ctx, 'Imagen no disponible', width, height, { color: colors.text, fontSize: 12, padding: 8 })
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
  const lines: string[] = []
  for (const paragraph of text.split('\n')) {
    const words = paragraph.split(' ')
    let current = ''
    for (const word of words) {
      const candidate = current ? `${current} ${word}` : word
      if (current && ctx.measureText(candidate).width > maxWidth) {
        lines.push(current)
        current = word
      } else {
        current = candidate
      }
    }
    lines.push(current)
  }
  return lines
}

function drawTextBlock(
  ctx: CanvasRenderingContext2D,
  text: string,
  boxWidth: number,
  boxHeight: number,
  options: { color: string; fontSize: number; padding: number },
) {
  if (!text) return
  ctx.save()
  ctx.beginPath()
  ctx.rect(0, 0, boxWidth, boxHeight)
  ctx.clip()
  ctx.fillStyle = options.color
  ctx.font = `${options.fontSize}px ${EXPORT_FONT_FAMILY}`
  ctx.textBaseline = 'top'
  const maxWidth = Math.max(0, boxWidth - options.padding * 2)
  const lineHeight = options.fontSize * 1.4
  let y = options.padding
  for (const line of wrapText(ctx, text, maxWidth)) {
    if (y > boxHeight) break
    ctx.fillText(line, options.padding, y, maxWidth)
    y += lineHeight
  }
  ctx.restore()
}

function drawContainFit(ctx: CanvasRenderingContext2D, img: HTMLImageElement, x: number, y: number, w: number, h: number) {
  const iw = img.naturalWidth || img.width
  const ih = img.naturalHeight || img.height
  if (!iw || !ih) return
  const scale = Math.min(w / iw, h / ih)
  const dw = iw * scale
  const dh = ih * scale
  ctx.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh)
}

function drawCoverFit(ctx: CanvasRenderingContext2D, img: HTMLImageElement, x: number, y: number, w: number, h: number) {
  const iw = img.naturalWidth || img.width
  const ih = img.naturalHeight || img.height
  if (!iw || !ih) return
  const scale = Math.max(w / iw, h / ih)
  const sw = w / scale
  const sh = h / scale
  ctx.drawImage(img, (iw - sw) / 2, (ih - sh) / 2, sw, sh, x, y, w, h)
}

/** BLOQUE D (post-MVP): the flat-color circle every icon-badge sticker/
    emoji renders as in the export (see the SHAPE-adjacent case's own doc
    comment on why the icon glyph itself isn't reproduced here) — same
    `STICKER_FIT_SCALE` proportion the real asset-based stickers use, so
    both kinds occupy a visually consistent footprint. */
function drawStickerBadge(ctx: CanvasRenderingContext2D, width: number, height: number, color: string) {
  const size = Math.min(width, height) * STICKER_FIT_SCALE
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.ellipse(width / 2, height / 2, size / 2, size / 2, 0, 0, Math.PI * 2)
  ctx.fill()
}

async function drawElement(ctx: CanvasRenderingContext2D, element: VisionBoardElement, colors: VisionBoardExportColors) {
  const { width, height } = element
  switch (element.type) {
    case 'TEXT':
      drawTextBlock(ctx, textOf(element.data), width, height, { color: colors.text, fontSize: 15, padding: 8 })
      return
    case 'NOTE':
      drawRoundedRect(ctx, 0, 0, width, height, colors.radiusControl, colors.warningContainer)
      drawTextBlock(ctx, textOf(element.data), width, height, { color: colors.text, fontSize: 14, padding: 8 })
      return
    case 'SHAPE': {
      const variant = shapeVariantOf(element.data)
      if (variant === 'circle') {
        ctx.fillStyle = colors.primaryContainer
        ctx.beginPath()
        ctx.ellipse(width / 2, height / 2, width / 2, height / 2, 0, 0, Math.PI * 2)
        ctx.fill()
      } else if (variant === 'line') {
        // Matches VisionBoardCanvas.module.css's .elementShapeLine: always
        // a 4px bar centered in the box, regardless of the element's own
        // stored height — the same visual quirk the real board renders.
        const barHeight = 4
        drawRoundedRect(ctx, 0, (height - barHeight) / 2, width, barHeight, barHeight / 2, colors.primaryContainer)
      } else if (variant === 'capsule') {
        drawRoundedRect(ctx, 0, 0, width, height, Math.min(width, height) / 2, colors.primaryContainer)
      } else if (variant === 'rectangle') {
        drawRoundedRect(ctx, 0, 0, width, height, colors.radiusControl, colors.primaryContainer)
      } else {
        // BLOQUE C (post-MVP): every other variant is one path from the
        // same catalog VisionBoardElementView.tsx's on-screen SVG reads
        // (visionBoardShapes.ts) — `Path2D` accepts an SVG path `d` string
        // directly, so this is the *same* path data, not a second
        // hand-drawn approximation of it. Scaling the context (not the
        // path coordinates) to width/height before filling matches the
        // on-screen SVG's `preserveAspectRatio="none"` non-uniform stretch.
        const def = shapeDefOf(variant)
        if (def.path) {
          ctx.save()
          ctx.scale(width / 100, height / 100)
          ctx.fillStyle = colors.primaryContainer
          ctx.fill(new Path2D(def.path), def.fillRule ?? 'nonzero')
          ctx.restore()
        }
      }
      // BLOQUE C: matches VisionBoardCanvas.module.css's `.elementShapeText`
      // (centered on top of the fill) — `drawTextBlock` alone is
      // top-aligned (right for TEXT/NOTE, which fill their own box edge to
      // edge) and uses one `padding` value for both axes, so centering
      // vertically needs its own small block instead of reusing it as-is.
      const shapeText = typeof element.data.text === 'string' ? element.data.text : ''
      if (shapeText) {
        const fontSize = 13
        const padding = 8
        const lineHeight = fontSize * 1.2
        ctx.save()
        ctx.beginPath()
        ctx.rect(0, 0, width, height)
        ctx.clip()
        ctx.font = `${fontSize}px ${EXPORT_FONT_FAMILY}`
        ctx.fillStyle = colors.text
        ctx.textBaseline = 'top'
        const maxWidth = Math.max(0, width - padding * 2)
        const lines = wrapText(ctx, shapeText, maxWidth)
        let y = Math.max(padding, (height - lines.length * lineHeight) / 2)
        for (const line of lines) {
          if (y > height) break
          const lineWidth = ctx.measureText(line).width
          ctx.fillText(line, (width - lineWidth) / 2, y, maxWidth)
          y += lineHeight
        }
        ctx.restore()
      }
      return
    }
    case 'STICKER': {
      // BLOQUE D (post-MVP): "😊 Emojis" is a STICKER element with
      // `data.emojiId` instead of `data.stickerId` — see
      // visionBoardEmojis.ts's own doc comment on why no new element type
      // was added for it.
      const emojiId = typeof element.data.emojiId === 'string' ? element.data.emojiId : undefined
      if (emojiId) {
        const emoji = findEmojiOption(emojiId)
        if (emoji) drawStickerBadge(ctx, width, height, emoji.color)
        return
      }
      const sticker = findStickerOption(typeof element.data.stickerId === 'string' ? element.data.stickerId : undefined)
      if (!sticker) return
      // BLOQUE D: the expanded catalog's icon-badge entries (no `asset`
      // file) export as their badge's flat color — reproducing the
      // `lucide-react` glyph itself on an offscreen canvas would need
      // rasterizing a whole second icon-rendering path this decorative
      // catalog content doesn't warrant; flagged as a Mejora Futura, not
      // implemented here. The original 12 Fluent Emoji stickers (real SVG
      // assets) still export exactly as before, unaffected.
      if (!sticker.asset) {
        if (sticker.color) drawStickerBadge(ctx, width, height, sticker.color)
        return
      }
      const img = await loadImage(sticker.asset)
      if (!img) return
      const w = width * STICKER_FIT_SCALE
      const h = height * STICKER_FIT_SCALE
      drawContainFit(ctx, img, (width - w) / 2, (height - h) / 2, w, h)
      return
    }
    case 'IMAGE': {
      // BLOQUE B: an internal upload (`data.imageId`) resolves through the
      // same authenticated fetch → blob URL as the on-screen canvas
      // (visionBoardImages.ts's shared cache — an image already showing on
      // screen is never re-fetched just to export it). A same-origin
      // `blob:` URL needs no `crossOrigin` (unlike the external `data.url`
      // path below, which does — see loadImage's own doc comment).
      const frameStyle = frameStyleOf(typeof element.data.frameStyle === 'string' ? element.data.frameStyle : undefined)
      const imageId = typeof element.data.imageId === 'string' ? element.data.imageId : undefined
      if (imageId) {
        try {
          const objectUrl = await resolveVisionBoardImageSrc(imageId)
          const img = await loadImage(objectUrl, false)
          if (img) {
            const clipped = clipImageFrame(ctx, frameStyle, width, height)
            drawCoverFit(ctx, img, 0, 0, width, height)
            if (clipped) ctx.restore()
            return
          }
        } catch {
          // falls through to the broken-image placeholder below
        }
        drawBrokenImagePlaceholder(ctx, width, height, colors)
        return
      }
      const url = typeof element.data.url === 'string' ? element.data.url : undefined
      if (!url) return
      const img = await loadImage(url, true)
      if (!img) {
        drawBrokenImagePlaceholder(ctx, width, height, colors)
        return
      }
      const clipped = clipImageFrame(ctx, frameStyle, width, height)
      drawCoverFit(ctx, img, 0, 0, width, height)
      if (clipped) ctx.restore()
      return
    }

    // 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva):
    // TABLE/CHART — mismo criterio "reusa los mismos datos, no un segundo
    // dibujo a mano" que SHAPE ya sigue arriba.
    case 'TABLE': {
      drawRoundedRect(ctx, 0, 0, width, height, colors.radiusControl, colors.surface)
      const raw = element.data.rows
      const rows = Array.isArray(raw) ? (raw as unknown[]).filter((row): row is string[] => Array.isArray(row)) : []
      const safeRows = rows.length > 0 ? rows : [['Columna 1', 'Columna 2'], ['', '']]
      const rowHeight = height / safeRows.length
      const colCount = Math.max(...safeRows.map((row) => row.length), 1)
      const colWidth = width / colCount
      ctx.strokeStyle = colors.primaryContainer
      ctx.lineWidth = 1
      ctx.font = `12px ${EXPORT_FONT_FAMILY}`
      ctx.textBaseline = 'top'
      safeRows.forEach((row, rowIndex) => {
        const y = rowIndex * rowHeight
        if (rowIndex === 0) {
          ctx.fillStyle = colors.primaryContainer
          ctx.fillRect(0, y, width, rowHeight)
        }
        ctx.fillStyle = colors.text
        row.forEach((cell, cellIndex) => {
          const x = cellIndex * colWidth
          ctx.strokeRect(x, y, colWidth, rowHeight)
          ctx.fillText(cell, x + 6, y + Math.max(4, (rowHeight - 12) / 2), Math.max(0, colWidth - 12))
        })
      })
      return
    }

    case 'CHART': {
      // Reutiliza el mismo markup SVG que el canvas real muestra
      // (visionBoardCharts.ts) — cargado como imagen en vez de un segundo
      // dibujo a mano, mismo espíritu que el `Path2D` de SHAPE arriba.
      const { viewBox, markup } = buildChart(typeof element.data.chartType === 'string' ? element.data.chartType : undefined)
      const [, , vbWidth, vbHeight] = viewBox.split(' ').map(Number)
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}" width="${vbWidth}" height="${vbHeight}">${markup}</svg>`
      const dataUrl = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
      const img = await loadImage(dataUrl, false)
      if (!img) return
      drawRoundedRect(ctx, 0, 0, width, height, colors.radiusControl, colors.surface)
      drawContainFit(ctx, img, 0, 0, width, height)
      return
    }

    case 'GRID': {
      // 2026-08-23 (pedido explícito del usuario, catálogo estilo Canva):
      // mismas celdas de color decorativas que el canvas real (ver
      // visionBoardGrids.ts's propio doc comment sobre el alcance).
      const layout = gridLayoutOf(typeof element.data.layout === 'string' ? element.data.layout : undefined)
      const gap = 4
      const cols = layout.gridTemplateColumns.match(/repeat\((\d+)/)?.[1]
      const rows = layout.gridTemplateRows.match(/repeat\((\d+)/)?.[1]
      const colCount = cols ? Number(cols) : 1
      const rowCount = rows ? Number(rows) : 1
      const cellW = (width - gap * (colCount - 1)) / colCount
      const cellH = (height - gap * (rowCount - 1)) / rowCount
      for (let i = 0; i < layout.cells; i++) {
        const col = i % colCount
        const row = Math.floor(i / colCount)
        const spans = layout.spanCell === i
        const x = col * (cellW + gap)
        const y = row * (cellH + gap)
        const h = spans ? cellH * 2 + gap : cellH
        ctx.fillStyle = GRID_CELL_COLORS[i % GRID_CELL_COLORS.length]
        ctx.fillRect(x, y, cellW, h)
      }
      return
    }
  }
}

/** "Marcos" (2026-08-23) — circle/hexagon/rounded se resuelven recortando
    la región de dibujo antes de pintar la imagen, mismo espíritu que
    SHAPE's `Path2D` arriba: la exportación reutiliza la misma geometría
    que el recorte CSS del canvas real (visionBoardFrames.ts), no una
    aproximación aparte. polaroid/film (chrome de borde, no una forma) se
    dejan sin aproximar en la exportación — gap menor, documentado, no
    bloquea el resto. Devuelve `true` si dejó un `ctx.save()` pendiente de
    `ctx.restore()`. */
function clipImageFrame(ctx: CanvasRenderingContext2D, frameStyle: VisionBoardFrameStyle | undefined, width: number, height: number): boolean {
  if (!frameStyle) return false
  if (frameStyle.id === 'circle') {
    ctx.save()
    ctx.beginPath()
    ctx.ellipse(width / 2, height / 2, width / 2, height / 2, 0, 0, Math.PI * 2)
    ctx.clip()
    return true
  }
  if (frameStyle.id === 'hexagon') {
    // Mismos puntos (en fracción 0–1) que el `clip-path: polygon(...)` de
    // visionBoardFrames.ts.
    const points: Array<[number, number]> = [
      [0.5, 0.02],
      [0.93, 0.26],
      [0.93, 0.74],
      [0.5, 0.98],
      [0.07, 0.74],
      [0.07, 0.26],
    ]
    ctx.save()
    ctx.beginPath()
    points.forEach(([px, py], index) => {
      const x = px * width
      const y = py * height
      if (index === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.closePath()
    ctx.clip()
    return true
  }
  if (frameStyle.id === 'rounded') {
    const r = Math.min(24, width / 2, height / 2)
    ctx.save()
    ctx.beginPath()
    ctx.moveTo(r, 0)
    ctx.arcTo(width, 0, width, height, r)
    ctx.arcTo(width, height, 0, height, r)
    ctx.arcTo(0, height, 0, 0, r)
    ctx.arcTo(0, 0, width, 0, r)
    ctx.closePath()
    ctx.clip()
    return true
  }
  return false
}

/** Draws the board (background + every visible element, in zIndex/paint
    order) onto a fresh off-screen canvas sized to the board's own
    width/height — never the current zoom, never touched by pan. Scaled up
    to devicePixelRatio (capped at 2x) purely for output sharpness; the
    logical coordinate system used for every draw call stays board-space
    px throughout. */
export async function renderVisionBoardToCanvas(
  board: VisionBoard,
  elements: VisionBoardElement[],
  colors: VisionBoardExportColors,
): Promise<HTMLCanvasElement> {
  const scale = Math.min(MAX_EXPORT_SCALE, window.devicePixelRatio || 1)
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(board.width * scale))
  canvas.height = Math.max(1, Math.round(board.height * scale))
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('No se pudo preparar el lienzo de exportación.')
  ctx.scale(scale, scale)

  ctx.fillStyle = colors.surface
  ctx.fillRect(0, 0, board.width, board.height)

  const ordered = elements.filter((element) => element.visible).sort((a, b) => a.zIndex - b.zIndex)
  for (const element of ordered) {
    ctx.save()
    const cx = element.x + element.width / 2
    const cy = element.y + element.height / 2
    ctx.translate(cx, cy)
    ctx.rotate((element.rotation * Math.PI) / 180)
    ctx.translate(-element.width / 2, -element.height / 2)
    // Sequential on purpose — paint order must stay strictly sequential
    // (later elements draw over earlier ones); loading images in parallel
    // would race the canvas draw order.
    await drawElement(ctx, element, colors)
    ctx.restore()
  }

  return canvas
}

export function canvasToPngBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob)
      else reject(new Error('No se pudo generar el PNG.'))
    }, 'image/png')
  })
}

/** JPEG has no alpha channel, but `renderVisionBoardToCanvas` always fills
    the board's own theme background first (see `ctx.fillRect` above), so
    there's never a transparent area for JPEG to flatten to black. 0.92 is
    a standard "visually lossless for UI/graphics content" quality — high
    enough that theme colors and text edges don't show compression
    artifacts, well short of 1.0's disproportionate file-size cost. */
const JPEG_EXPORT_QUALITY = 0.92

export function canvasToJpegBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) resolve(blob)
        else reject(new Error('No se pudo generar el JPG.'))
      },
      'image/jpeg',
      JPEG_EXPORT_QUALITY,
    )
  })
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** BLOQUE H (post-MVP): the rendered canvas embedded as a full-page image
    into a real single-page PDF, sized to the board's own aspect ratio, and
    saved straight to disk — no print dialog, no intermediate window, no
    dependency on what print destination a human happens to pick. `unit:
    'pt'` + an explicit `[width, height]` page format is what makes the
    page exactly the board's own size rather than a fixed A4/Letter with
    the image shrunk to fit; 72pt/in is the PDF spec's own fixed unit,
    96px/in is the standard CSS px reference `renderVisionBoardToCanvas`
    already uses for `board.width`/`board.height` — same conversion the old
    print-window `@page size` used, just expressed in `jspdf`'s own unit
    instead of CSS inches. */
export async function downloadVisionBoardPdf(
  canvas: HTMLCanvasElement,
  boardWidth: number,
  boardHeight: number,
  filename: string,
): Promise<void> {
  // Dynamic import: `jspdf`'s only browser entry point statically bundles
  // html2canvas + dompurify (for its unused-here `.html()` plugin, ~55kB
  // gzipped) — loading it only when a user actually clicks "PDF" keeps
  // that weight out of every Vision Board visitor's initial bundle.
  const { jsPDF } = await import('jspdf')
  const dataUrl = canvas.toDataURL('image/png')
  const widthPt = (boardWidth / 96) * 72
  const heightPt = (boardHeight / 96) * 72
  const doc = new jsPDF({
    orientation: widthPt >= heightPt ? 'landscape' : 'portrait',
    unit: 'pt',
    format: [widthPt, heightPt],
  })
  doc.addImage(dataUrl, 'PNG', 0, 0, widthPt, heightPt)
  doc.save(filename)
}

const COMBINING_DIACRITICS = /[\u0300-\u036f]/g

export function slugifyForFilename(name: string): string {
  const slug = name
    .toLowerCase()
    .normalize('NFD')
    .replace(COMBINING_DIACRITICS, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return slug || 'vision-board'
}
