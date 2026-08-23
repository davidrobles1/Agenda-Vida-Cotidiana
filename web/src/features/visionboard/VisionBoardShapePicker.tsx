import { useState } from 'react'
import { handleGridKeyDown, handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import { SHAPE_FAMILIES, shapesByFamily, shapeDefOf, type ShapeFamilyId, type VisionBoardShapeDef } from './visionBoardShapes'
import styles from './VisionBoardShapePicker.module.css'

interface VisionBoardShapePickerProps {
  value: string
  onChange: (id: string) => void
  label: string
  /** BLOQUE G: `true` (default) for Editar — a real staged value, arrow
      keys select same as a native radio. `false` for creating one
      (VisionBoardElementLibrary.tsx's ShapeFields) — picking a shape
      there creates it immediately and closes the popover, so arrow keys
      must only browse, never themselves trigger that action (see
      radiogroupKeyboard.ts's own doc comment on `handleGridKeyDown`). */
  commitOnArrowKey?: boolean
}

/**
 * BLOQUE C (post-MVP): shared by VisionBoardElementEditor.tsx (editing an
 * existing SHAPE) and VisionBoardElementLibrary.tsx (creating one) — the
 * exact same "which of the ~60 shapes" picker either way, one definition
 * instead of two.
 *
 * 2026-08-22: once the catalog grew past ~30 flat tiles, a single scrolling
 * grid stopped being scannable — split into family tabs (visionBoardShapes.ts's
 * `SHAPE_FAMILIES`), same idea as the "Elementos" library's own step
 * pattern. Opens on the family that already contains `value` (editing an
 * existing shape shouldn't dump you back on "Básicas").
 */
export function VisionBoardShapePicker({ value, onChange, label, commitOnArrowKey = true }: VisionBoardShapePickerProps) {
  const [family, setFamily] = useState<ShapeFamilyId>(() => (value ? shapeDefOf(value).family : 'basic'))
  const shapes = shapesByFamily(family)
  const anyChecked = shapes.some((shape) => shape.id === value)
  return (
    <div>
      <div className={styles.familyTabs} role="tablist" aria-label={`${label} — familia`}>
        {SHAPE_FAMILIES.map((fam) => (
          <button
            key={fam.id}
            type="button"
            role="tab"
            aria-selected={family === fam.id}
            className={`${styles.familyTab} ${family === fam.id ? styles.familyTabActive : ''}`}
            onClick={() => setFamily(fam.id)}
          >
            {fam.label}
          </button>
        ))}
      </div>
      <div
        className={styles.grid}
        role="radiogroup"
        aria-label={label}
        onKeyDown={commitOnArrowKey ? handleRadiogroupKeyDown : handleGridKeyDown}
      >
        {shapes.map((shape, index) => (
          <button
            key={shape.id}
            type="button"
            role="radio"
            aria-checked={value === shape.id}
            tabIndex={radioTabIndex(value === shape.id, index === 0, anyChecked)}
            className={`${styles.tile} ${value === shape.id ? styles.tileActive : ''}`}
            onClick={() => onChange(shape.id)}
          >
            <ShapePreview shape={shape} />
            {shape.label}
          </button>
        ))}
      </div>
    </div>
  )
}

function ShapePreview({ shape }: { shape: VisionBoardShapeDef }) {
  if (shape.id === 'rectangle') return <span className={styles.previewRect} aria-hidden="true" />
  if (shape.id === 'circle') return <span className={styles.previewCircle} aria-hidden="true" />
  if (shape.id === 'line') return <span className={styles.previewLine} aria-hidden="true" />
  if (shape.id === 'capsule') return <span className={styles.previewCapsule} aria-hidden="true" />
  return (
    <svg viewBox="0 0 100 100" className={styles.previewSvg} aria-hidden="true">
      <path d={shape.path} fillRule={shape.fillRule} />
    </svg>
  )
}
