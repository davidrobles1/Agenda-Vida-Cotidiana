import { useState } from 'react'
import { Button, Menu, MenuItem, MenuTrigger, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import type { VisionBoardThemeId } from './api'
import { VISION_BOARD_THEME_LIST, visionBoardThemeOf } from './visionBoardThemes'
import toolbarStyles from './VisionBoardToolbar.module.css'
import styles from './VisionBoardThemeSwitcher.module.css'

const MotionMenu = motion.create(Menu)

interface VisionBoardThemeSwitcherProps {
  theme: VisionBoardThemeId
  saving: boolean
  onChange: (theme: VisionBoardThemeId) => void
  /** BLOQUE E (post-MVP): same optional-controlled shape as
      VisionBoardDeleteConfirm.tsx's own isOpen/onOpenChange — the context
      menu's "Cambiar tema" entry needs to open this popover too. */
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
}

/**
 * FASE 16: Board Theme picker — deliberately mirrors
 * core/theme/ThemeSwitcher.tsx's own `MenuTrigger`/`Popover`/`Menu` shape
 * (same component family, same `selectionMode="single"` + swatch-dot
 * pattern) rather than inventing a second picker pattern for what is,
 * structurally, the same kind of choice. Also inherits that file's own
 * documented real bug fix: animate the `Menu` inside the `Popover`, never
 * the `Popover` itself (its internal `useResizeObserver`/layout effects
 * clobber Motion's inline style on remount, leaving the panel invisible
 * but still clickable).
 *
 * This is a *different* picker from the app's own ThemeSwitcher — separate
 * component, separate state, separate persistence (PUT /vision-boards/{id}
 * vs. the app theme's own localStorage) — by design, since Board Theme and
 * App Theme must stay fully independent (see visionBoardThemes.ts's own
 * doc comment).
 */
export function VisionBoardThemeSwitcher({
  theme,
  saving,
  onChange,
  isOpen: isOpenProp,
  onOpenChange: onOpenChangeProp,
}: VisionBoardThemeSwitcherProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const open = isOpenProp ?? internalOpen
  const setOpen = onOpenChangeProp ?? setInternalOpen
  const active = visionBoardThemeOf(theme)

  return (
    <MenuTrigger isOpen={open} onOpenChange={setOpen}>
      {/* Barra icon-only (2026-08-23): el nombre del tema se quitó; el
          swatch de color ya comunica cuál está activo y el `aria-label`
          (que ya incluía el nombre) sigue dándolo por accesibilidad. */}
      <Button
        className={toolbarStyles.actionButton}
        aria-label={`Cambiar tema del Vision Board (actual: ${active.name})`}
      >
        <span className={styles.swatch} style={{ background: active.colors.accent }} aria-hidden="true" />
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.popover}>
        <MotionMenu
          className={styles.menu}
          aria-label="Tema del Vision Board"
          selectionMode="single"
          disabledKeys={saving ? VISION_BOARD_THEME_LIST.map((t) => t.id) : []}
          selectedKeys={new Set([theme])}
          onSelectionChange={(keys) => {
            if (keys === 'all') return
            const [next] = keys
            if (typeof next === 'string') onChange(next as VisionBoardThemeId)
          }}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: open ? 1 : 0, scale: open ? 1 : 0.96, y: open ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {VISION_BOARD_THEME_LIST.map((t) => (
            <MenuItem key={t.id} id={t.id} className={styles.item} textValue={t.name}>
              <span className={styles.dot} style={{ background: t.colors.accent }} aria-hidden="true" />
              <span className={styles.itemText}>
                <span className={styles.itemLabel}>{t.name}</span>
                <span className={styles.itemTagline}>{t.description}</span>
              </span>
            </MenuItem>
          ))}
        </MotionMenu>
      </Popover>
    </MenuTrigger>
  )
}
