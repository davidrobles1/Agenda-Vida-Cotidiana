import { handleGridKeyDown, handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import { EMOJI_CATALOG, EMOJI_CATEGORIES } from './visionBoardEmojis'
import styles from './VisionBoardElementLibrary.module.css'

interface VisionBoardEmojiPickerProps {
  value: string | undefined
  onChange: (emojiId: string) => void
  /** BLOQUE G: see VisionBoardShapePicker.tsx's own doc comment on this
      same prop — `false` from VisionBoardElementLibrary.tsx's EmojiFields
      (creating one picks and closes immediately), default `true`
      everywhere else (Editar only stages the pick). */
  commitOnArrowKey?: boolean
}

/** BLOQUE D (post-MVP): shared by VisionBoardElementLibrary.tsx's
    "😊 Emojis" create step and VisionBoardElementEditor.tsx's Editar step —
    the exact same grouped-by-category grid either way, one definition
    instead of two (same reasoning as VisionBoardShapePicker.tsx).
    BLOQUE G: each category is its own independent radiogroup (own roving
    tabindex, own Arrow/Home/End handling) — 120 entries in one single
    group would make Home/End nearly useless and Tab still only stop once
    for the entire picker; stopping once per category (20 stops to reach
    the end) reads as the more usable, still fully APG-compliant shape for
    a grouped picker like this. */
export function VisionBoardEmojiPicker({ value, onChange, commitOnArrowKey = true }: VisionBoardEmojiPickerProps) {
  return (
    <div className={styles.emojiCategories}>
      {EMOJI_CATEGORIES.map((category) => {
        const items = EMOJI_CATALOG.filter((emoji) => emoji.category === category)
        const anyChecked = items.some((emoji) => emoji.id === value)
        return (
          <div key={category}>
            <span className={styles.emojiCategoryLabel}>{category}</span>
            <div
              className={styles.emojiGrid}
              role="radiogroup"
              aria-label={category}
              onKeyDown={commitOnArrowKey ? handleRadiogroupKeyDown : handleGridKeyDown}
            >
              {items.map((emoji, index) => (
                <button
                  key={emoji.id}
                  type="button"
                  role="radio"
                  aria-checked={value === emoji.id}
                  aria-label={emoji.label}
                  tabIndex={radioTabIndex(value === emoji.id, index === 0, anyChecked)}
                  className={`${styles.emojiTile} ${value === emoji.id ? styles.emojiTileActive : ''}`}
                  onClick={() => onChange(emoji.id)}
                >
                  <span className={styles.emojiBadge} style={{ background: emoji.color }}>
                    <emoji.Icon width={16} height={16} aria-hidden="true" />
                  </span>
                </button>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}
