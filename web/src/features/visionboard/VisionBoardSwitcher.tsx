import { useState } from 'react'
import { Button, Menu, MenuItem, MenuTrigger, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { Check, ChevronDown } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import type { VisionBoard } from './api'
import toolbarStyles from './VisionBoardToolbar.module.css'
import styles from './VisionBoardSwitcher.module.css'

const MotionMenu = motion.create(Menu)

interface VisionBoardSwitcherProps {
  boards: VisionBoard[]
  currentBoardId: string
  switching: boolean
  onSwitch: (id: string) => void
}

/**
 * FASE 17: resolves the "board switcher" pendiente — mirrors
 * VisionBoardThemeSwitcher.tsx's own `MenuTrigger`/`Popover`/`MotionMenu`/
 * `MenuItem` shape (itself mirroring core/theme/ThemeSwitcher.tsx), the
 * third use of this exact pattern in the app now, rather than a new one.
 * Uses the real `GET /vision-boards` list VisionBoardPage.tsx already
 * loads — no mock, no new endpoint.
 *
 * Keyboard: entirely free from `MenuTrigger`/`Menu` — Tab reaches the
 * trigger, Enter/Space opens it, Arrow Up/Down move between boards, Enter
 * selects, Escape closes and returns focus to the trigger. Same guarantee
 * ThemeSwitcher/VisionBoardThemeSwitcher already rely on, not reimplemented.
 */
export function VisionBoardSwitcher({ boards, currentBoardId, switching, onSwitch }: VisionBoardSwitcherProps) {
  const [open, setOpen] = useState(false)
  const current = boards.find((b) => b.id === currentBoardId)

  return (
    <MenuTrigger isOpen={open} onOpenChange={setOpen}>
      <Button
        className={`${toolbarStyles.actionButton} ${styles.trigger}`}
        aria-label="Cambiar de Vision Board"
        isDisabled={switching}
      >
        <span className={styles.triggerLabel}>{switching ? 'Cargando…' : (current?.name ?? 'Vision Board')}</span>
        <ChevronDown width={14} height={14} aria-hidden="true" />
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.popover}>
        <MotionMenu
          className={styles.menu}
          aria-label="Mis Vision Boards"
          selectionMode="single"
          disabledKeys={switching ? boards.map((b) => b.id) : []}
          selectedKeys={new Set([currentBoardId])}
          onSelectionChange={(keys) => {
            if (keys === 'all') return
            const [next] = keys
            if (typeof next === 'string') onSwitch(next)
          }}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: open ? 1 : 0, scale: open ? 1 : 0.96, y: open ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {boards.map((b) => (
            <MenuItem key={b.id} id={b.id} className={styles.item} textValue={b.name}>
              <span className={styles.itemLabel}>{b.name}</span>
              {b.id === currentBoardId && <Check width={16} height={16} className={styles.itemActiveIcon} aria-hidden="true" />}
            </MenuItem>
          ))}
        </MotionMenu>
      </Popover>
    </MenuTrigger>
  )
}
