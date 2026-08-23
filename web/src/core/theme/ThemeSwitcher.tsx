import { useState } from 'react'
import { Button, Menu, MenuItem, MenuTrigger, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../motion/tokens'
import { useVisualTheme, VISUAL_THEMES } from './VisualThemeContext'
import styles from './ThemeSwitcher.module.css'

const MotionMenu = motion.create(Menu)

/**
 * UX-014: visual-theme selector — top-right of AppShell's topbar, same
 * `MenuTrigger`/`Popover` React Aria pattern used for non-blocking floating
 * panels elsewhere.
 *
 * Real bug found in browser testing: animating `Popover` itself (as
 * `EmojiPickerButton.tsx` also does) leaves it permanently stuck at its
 * `initial` opacity — `Popover`'s own internal state (`useResizeObserver`/
 * `useLayoutEffect` for trigger width and dialog-role detection) re-renders
 * it after mount, and that re-render's `style` prop doesn't carry Motion's
 * latest animated value, so React resets the DOM node's style back to the
 * stale one. The panel ends up fully present, correctly positioned and
 * clickable (`opacity: 0` doesn't block pointer events), but 100% invisible
 * — options work when clicked, but can't be seen. Same root cause already
 * documented for `Modal` (never animate it directly — animate the `Dialog`
 * inside it): the fix here is analogous — animate the `Menu` content, leave
 * `Popover` itself un-animated.
 */
export function ThemeSwitcher() {
  const { theme, setTheme } = useVisualTheme()
  const [open, setOpen] = useState(false)
  const active = VISUAL_THEMES.find((t) => t.id === theme)!

  return (
    <MenuTrigger isOpen={open} onOpenChange={setOpen}>
      <Button
        className={styles.trigger}
        aria-label="Cambiar tema visual"
        data-variant="chrome"
      >
        <span className={styles.swatch} style={{ background: active.swatch }} aria-hidden="true" />
        {active.label}
      </Button>
      <Popover placement="bottom end" offset={8} className={styles.popover}>
        <MotionMenu
          className={styles.menu}
          aria-label="Tema visual"
          selectionMode="single"
          selectedKeys={new Set([theme])}
          onSelectionChange={(keys) => {
            if (keys === 'all') return
            const [next] = keys
            if (typeof next === 'string') setTheme(next as (typeof VISUAL_THEMES)[number]['id'])
          }}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: open ? 1 : 0, scale: open ? 1 : 0.96, y: open ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {VISUAL_THEMES.map((t) => (
            <MenuItem key={t.id} id={t.id} className={styles.item} textValue={t.label}>
              <span className={styles.dot} style={{ background: t.swatch }} aria-hidden="true" />
              <span className={styles.itemText}>
                <span className={styles.itemLabel}>{t.label}</span>
                <span className={styles.itemTagline}>{t.tagline}</span>
              </span>
            </MenuItem>
          ))}
        </MotionMenu>
      </Popover>
    </MenuTrigger>
  )
}
