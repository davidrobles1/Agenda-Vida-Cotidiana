/**
 * FASE 9: pure geometry for Smart Guides — no React, no network, no board/
 * element persistence. VisionBoardCanvas.tsx is the only caller, at the
 * exact seam where it already owns drag/resize deltas (onDragMove/
 * onResizeMove) before applying them to local state; VisionBoardElementView
 * keeps reporting raw pointer-derived values exactly as before FASE 9,
 * unaware any of this exists.
 *
 * Rotation is deliberately out of scope: guides compare the moving
 * element's own unrotated x/y/width/height box against everyone else's,
 * same simplification VisionBoardElementView.tsx's resize handles already
 * make (they resize along the element's own local axes, not the screen's).
 */

export interface Guide {
  orientation: 'vertical' | 'horizontal'
  /** Canvas-space position (not screen pixels) — rendered inside `.canvas`,
      so it inherits that element's own zoom transform automatically. */
  position: number
}

interface Box {
  x: number
  y: number
  width: number
  height: number
}

/** 8 screen px feels grabby without snapping from across the canvas — same
    order of magnitude as the resize/rotate handles' own 18px hit target
    (VisionBoardCanvas.module.css). Divided by zoom so the *visual* snap
    distance stays constant regardless of how zoomed in/out the canvas is. */
const SNAP_THRESHOLD_SCREEN_PX = 8

export function snapThreshold(zoom: number): number {
  return SNAP_THRESHOLD_SCREEN_PX / zoom
}

interface SnapMatch {
  delta: number
  guide: Guide
}

function closestMatch(
  candidates: number[],
  targets: number[],
  threshold: number,
  orientation: Guide['orientation'],
): SnapMatch | null {
  let best: SnapMatch | null = null
  for (const candidate of candidates) {
    for (const target of targets) {
      const delta = target - candidate
      const distance = Math.abs(delta)
      if (distance <= threshold && (!best || distance < Math.abs(best.delta))) {
        best = { delta, guide: { orientation, position: target } }
      }
    }
  }
  return best
}

/** Every edge/center worth aligning to: the canvas's own edges/center, plus
    every other element's. */
function edgeTargets(boxes: Box[], canvasWidth: number, canvasHeight: number) {
  const vertical = [0, canvasWidth / 2, canvasWidth]
  const horizontal = [0, canvasHeight / 2, canvasHeight]
  for (const box of boxes) {
    vertical.push(box.x, box.x + box.width / 2, box.x + box.width)
    horizontal.push(box.y, box.y + box.height / 2, box.y + box.height)
  }
  return { vertical, horizontal }
}

/**
 * Drag: the whole box moves freely, so all three vertical positions (left/
 * center/right) and all three horizontal ones (top/center/bottom) are
 * candidates — independently per axis, so a free move that's only close to
 * a guide on one axis still snaps just that axis, matching "el usuario debe
 * poder seguir moviendo libremente cuando no hay proximidad."
 */
export function computeDragSnap(
  x: number,
  y: number,
  width: number,
  height: number,
  others: Box[],
  canvasWidth: number,
  canvasHeight: number,
  zoom: number,
): { x: number; y: number; guides: Guide[] } {
  const threshold = snapThreshold(zoom)
  const { vertical, horizontal } = edgeTargets(others, canvasWidth, canvasHeight)
  const guides: Guide[] = []

  let snappedX = x
  const vSnap = closestMatch([x, x + width / 2, x + width], vertical, threshold, 'vertical')
  if (vSnap) {
    snappedX = x + vSnap.delta
    guides.push(vSnap.guide)
  }

  let snappedY = y
  const hSnap = closestMatch([y, y + height / 2, y + height], horizontal, threshold, 'horizontal')
  if (hSnap) {
    snappedY = y + hSnap.delta
    guides.push(hSnap.guide)
  }

  return { x: snappedX, y: snappedY, guides }
}

/**
 * Resize: x/y never move (VisionBoardElementView's resize handles all grow/
 * shrink from the fixed top-left origin — see its own doc comment on why
 * only E/S/SE exist), so only the *moving* edge per active axis is a
 * candidate: the right edge when width is changing, the bottom edge when
 * height is changing. `snapWidth`/`snapHeight` let the caller only engage
 * the axis the current resize handle actually touches (an E-only resize
 * must never nudge height just because the untouched bottom edge happens to
 * be near a guide).
 */
export function computeResizeSnap(
  x: number,
  y: number,
  width: number,
  height: number,
  snapWidth: boolean,
  snapHeight: boolean,
  others: Box[],
  canvasWidth: number,
  canvasHeight: number,
  zoom: number,
  minSize: number,
): { width: number; height: number; guides: Guide[] } {
  const threshold = snapThreshold(zoom)
  const { vertical, horizontal } = edgeTargets(others, canvasWidth, canvasHeight)
  const guides: Guide[] = []

  let snappedWidth = width
  if (snapWidth) {
    const vSnap = closestMatch([x + width], vertical, threshold, 'vertical')
    if (vSnap) {
      snappedWidth = Math.max(minSize, width + vSnap.delta)
      guides.push(vSnap.guide)
    }
  }

  let snappedHeight = height
  if (snapHeight) {
    const hSnap = closestMatch([y + height], horizontal, threshold, 'horizontal')
    if (hSnap) {
      snappedHeight = Math.max(minSize, height + hSnap.delta)
      guides.push(hSnap.guide)
    }
  }

  return { width: snappedWidth, height: snappedHeight, guides }
}
