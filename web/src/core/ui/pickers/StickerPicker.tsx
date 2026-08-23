import { handleGridKeyDown, handleRadiogroupKeyDown, radioTabIndex } from '../keyboard/radiogroupKeyboard'
import { STICKER_OPTIONS } from './pickerCatalog'
import styles from './Pickers.module.css'

interface StickerPickerProps {
  value: string | undefined
  onChange: (value: string | undefined) => void
  /** BLOQUE G: see VisionBoardShapePicker.tsx's own doc comment on this
      same prop — `false` from VisionBoardElementLibrary.tsx's StickerFields
      (creating one picks and closes immediately), default `true` for every
      other caller (Notas, and VisionBoardElementEditor.tsx's Editar step,
      which only stage the pick). */
  commitOnArrowKey?: boolean
}

/**
 * Grilla de selección de sticker (Fluent Emoji) — compartida por Notas y
 * Agenda. Catálogo de ~12 elementos: grilla inline directa, sin buscador,
 * sin tabs, sin Popover — esa complejidad no aporta valor a este tamaño de
 * catálogo (ver design doc de la tarea de Notas).
 */
export function StickerPicker({ value, onChange, commitOnArrowKey = true }: StickerPickerProps) {
  const anyChecked = STICKER_OPTIONS.some((sticker) => sticker.id === value)
  return (
    <div
      className={styles.stickerGrid}
      role="radiogroup"
      aria-label="Sticker"
      onKeyDown={commitOnArrowKey ? handleRadiogroupKeyDown : handleGridKeyDown}
    >
      {STICKER_OPTIONS.map((sticker, index) => (
        <button
          key={sticker.id}
          type="button"
          role="radio"
          aria-checked={value === sticker.id}
          aria-label={sticker.label}
          tabIndex={radioTabIndex(value === sticker.id, index === 0, anyChecked)}
          className={`${styles.stickerButton} ${value === sticker.id ? styles.stickerButtonActive : ''}`}
          onClick={() => onChange(value === sticker.id ? undefined : sticker.id)}
        >
          {sticker.asset ? (
            <img src={sticker.asset} alt="" className={styles.stickerImage} />
          ) : (
            sticker.Icon && (
              <span className={styles.stickerBadge} style={{ background: sticker.color }}>
                <sticker.Icon width={18} height={18} aria-hidden="true" />
              </span>
            )
          )}
        </button>
      ))}
    </div>
  )
}
