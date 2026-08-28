import { useState } from 'react'
import { Button, Menu, MenuItem, MenuTrigger, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { Sparkles } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import toolbarStyles from './VisionBoardToolbar.module.css'
import styles from './VisionBoardAnimationAction.module.css'

const MotionMenu = motion.create(Menu)

/** 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva): 6
    presets de entrada — puramente client-side, se guardan en `data.entrance`
    (JSON libre del elemento, sin cambios de backend) y su keyframe vive en
    VisionBoardCanvas.module.css. No hay todavía un "modo presentación" que
    las reproduzca automáticamente al abrir el tablero — a propósito, ver
    VisionBoardCanvas.tsx's own doc comment en `triggerAnimationReplay`. */
export const ENTRANCE_ANIMATIONS = [
  { id: 'fadeIn', label: 'Aparecer' },
  { id: 'slideUp', label: 'Deslizar desde abajo' },
  { id: 'slideLeft', label: 'Deslizar desde la izquierda' },
  { id: 'popIn', label: 'Aparecer y crecer' },
  { id: 'bounceIn', label: 'Rebote' },
  { id: 'pulse', label: 'Pulso' },
] as const

export type EntranceAnimationId = (typeof ENTRANCE_ANIMATIONS)[number]['id']

interface VisionBoardAnimationActionProps {
  disabled: boolean
  current: EntranceAnimationId | undefined
  onApply: (animationId: EntranceAnimationId) => void
}

/**
 * Mirrors VisionBoardThemeSwitcher.tsx's exact MenuTrigger/Popover/Menu
 * shape (same "animate the Menu, never the Popover" fix that file's own
 * doc comment explains) — same family of picker, nothing new to debug.
 */
export function VisionBoardAnimationAction({ disabled, current, onApply }: VisionBoardAnimationActionProps) {
  const [open, setOpen] = useState(false)

  return (
    <MenuTrigger isOpen={open} onOpenChange={setOpen}>
      <Button className={toolbarStyles.actionButton} isDisabled={disabled} aria-label="Animar elemento">
        <Sparkles width={16} height={16} />
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.popover}>
        <MotionMenu
          className={styles.menu}
          aria-label="Animación de entrada"
          selectionMode="single"
          selectedKeys={current ? new Set([current]) : new Set()}
          onSelectionChange={(keys) => {
            if (keys === 'all') return
            const [next] = keys
            if (typeof next === 'string') onApply(next as EntranceAnimationId)
          }}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: open ? 1 : 0, scale: open ? 1 : 0.96, y: open ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {ENTRANCE_ANIMATIONS.map((a) => (
            <MenuItem key={a.id} id={a.id} className={styles.item} textValue={a.label}>
              {a.label}
            </MenuItem>
          ))}
        </MotionMenu>
      </Popover>
    </MenuTrigger>
  )
}
