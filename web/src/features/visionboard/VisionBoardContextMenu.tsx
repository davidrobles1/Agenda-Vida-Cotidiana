import { Button, Menu, MenuItem, MenuTrigger, Popover, Separator } from 'react-aria-components'
import { motion } from 'motion/react'
import {
  ArrowDownToLine,
  ArrowUpToLine,
  ChevronDown,
  ChevronUp,
  Copy,
  LayoutTemplate,
  LibraryBig,
  Lock,
  LockOpen,
  Palette,
  PencilLine,
  Plus,
  Trash2,
} from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import styles from './VisionBoardContextMenu.module.css'

const MotionMenu = motion.create(Menu)

type ContextMenuAction =
  | 'edit'
  | 'duplicate'
  | 'toggleLock'
  | 'delete'
  | 'front'
  | 'back'
  | 'raise'
  | 'lower'
  | 'templates'
  | 'elements'
  | 'theme'
  | 'newBoard'

interface VisionBoardContextMenuProps {
  /** `null` closes the menu — same "position is the open signal" shape a
      screen-space point naturally gives, no separate boolean needed. */
  position: { x: number; y: number } | null
  onOpenChange: (open: boolean) => void
  selectedCount: number
  singleSelectedLocked: boolean
  allSelectedLocked: boolean
  hasDeletable: boolean
  onEdit: () => void
  onDuplicate: () => void
  onToggleLock: () => void
  onDelete: () => void
  onLayerChange: (kind: 'front' | 'back' | 'raise' | 'lower') => void
  onOpenTemplates: () => void
  onOpenElements: () => void
  onOpenTheme: () => void
  onNewBoard: () => void
}

/**
 * BLOQUE E (post-MVP) — right-click (Windows) / two-finger tap (macOS
 * trackpad, already translated by the OS/browser into a native
 * `contextmenu` DOM event — nothing macOS-specific to handle here) context
 * menu. Positioned at the real click point via a 1×1, invisible,
 * `pointer-events: none` `Button` used purely as `MenuTrigger`'s anchor
 * (React Aria Components has no first-class "open at this screen point"
 * API — this is the standard workaround) — everything else is the exact
 * same `MenuTrigger`/`Popover`/`Menu` shape, and the exact same "animate
 * `Menu`, never `Popover`" fix, every other menu in this feature already
 * uses (VisionBoardThemeSwitcher.tsx, VisionBoardSwitcher.tsx).
 *
 * Every action here calls back into VisionBoardCanvas.tsx's own existing
 * handlers (or opens the exact same popover its toolbar button opens, via
 * the lifted isOpen/onOpenChange props those components gained this same
 * phase) — nothing is reimplemented, this is purely a second way to reach
 * actions that already exist.
 */
export function VisionBoardContextMenu({
  position,
  onOpenChange,
  selectedCount,
  singleSelectedLocked,
  allSelectedLocked,
  hasDeletable,
  onEdit,
  onDuplicate,
  onToggleLock,
  onDelete,
  onLayerChange,
  onOpenTemplates,
  onOpenElements,
  onOpenTheme,
  onNewBoard,
}: VisionBoardContextMenuProps) {
  const isOpen = position !== null

  function handleAction(action: ContextMenuAction) {
    onOpenChange(false)
    switch (action) {
      case 'edit':
        onEdit()
        return
      case 'duplicate':
        onDuplicate()
        return
      case 'toggleLock':
        onToggleLock()
        return
      case 'delete':
        onDelete()
        return
      case 'front':
      case 'back':
      case 'raise':
      case 'lower':
        onLayerChange(action)
        return
      case 'templates':
        onOpenTemplates()
        return
      case 'elements':
        onOpenElements()
        return
      case 'theme':
        onOpenTheme()
        return
      case 'newBoard':
        onNewBoard()
    }
  }

  return (
    <MenuTrigger isOpen={isOpen} onOpenChange={onOpenChange}>
      {/* BLOQUE E fix (real axe-core finding, not a false positive): React
          Aria's `Button` doesn't forward a plain `aria-hidden` prop (it
          conflicts with the `aria-haspopup`/`aria-expanded` MenuTrigger
          already sets on its trigger), so this anchor rendered as a
          nameless, zero-size button — a real WCAG 4.1.2 (button-name)
          violation the accessibility scan below caught for real.
          `excludeFromTabOrder` already keeps it out of the Tab sequence;
          `aria-label` gives it a real accessible name for the rare
          assistive-tech path that isn't Tab-based (e.g. touch
          exploration). */}
      <Button
        aria-label="Menú contextual"
        excludeFromTabOrder
        className={styles.anchor}
        style={{ left: position?.x ?? 0, top: position?.y ?? 0 }}
      />
      <Popover placement="bottom start" offset={2} className={styles.popover}>
        <MotionMenu
          className={styles.menu}
          aria-label="Menú contextual del Vision Board"
          onAction={(key) => handleAction(key as ContextMenuAction)}
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96 }}
          transition={motionTokens.smooth}
        >
          {selectedCount === 0 ? (
            <>
              <MenuItem id="templates" className={styles.item} textValue="Templates">
                <LayoutTemplate width={16} height={16} aria-hidden="true" />
                Templates
              </MenuItem>
              <MenuItem id="elements" className={styles.item} textValue="Elementos">
                <LibraryBig width={16} height={16} aria-hidden="true" />
                Elementos
              </MenuItem>
              <MenuItem id="theme" className={styles.item} textValue="Cambiar tema">
                <Palette width={16} height={16} aria-hidden="true" />
                Cambiar tema
              </MenuItem>
              <Separator className={styles.separator} />
              <MenuItem id="newBoard" className={styles.item} textValue="Nuevo Vision Board">
                <Plus width={16} height={16} aria-hidden="true" />
                Nuevo Vision Board
              </MenuItem>
            </>
          ) : selectedCount === 1 ? (
            <>
              <MenuItem id="edit" className={styles.item} textValue="Editar" isDisabled={singleSelectedLocked}>
                <PencilLine width={16} height={16} aria-hidden="true" />
                Editar
              </MenuItem>
              <MenuItem id="duplicate" className={styles.item} textValue="Duplicar">
                <Copy width={16} height={16} aria-hidden="true" />
                Duplicar
              </MenuItem>
              <MenuItem id="toggleLock" className={styles.item} textValue={allSelectedLocked ? 'Desbloquear' : 'Bloquear'}>
                {allSelectedLocked ? <LockOpen width={16} height={16} aria-hidden="true" /> : <Lock width={16} height={16} aria-hidden="true" />}
                {allSelectedLocked ? 'Desbloquear' : 'Bloquear'}
              </MenuItem>
              <MenuItem id="delete" className={styles.item} textValue="Eliminar" isDisabled={!hasDeletable}>
                <Trash2 width={16} height={16} aria-hidden="true" />
                Eliminar
              </MenuItem>
              <Separator className={styles.separator} />
              <MenuItem id="front" className={styles.item} textValue="Traer al frente" isDisabled={singleSelectedLocked}>
                <ArrowUpToLine width={16} height={16} aria-hidden="true" />
                Traer al frente
              </MenuItem>
              <MenuItem id="raise" className={styles.item} textValue="Subir una capa" isDisabled={singleSelectedLocked}>
                <ChevronUp width={16} height={16} aria-hidden="true" />
                Subir una capa
              </MenuItem>
              <MenuItem id="lower" className={styles.item} textValue="Bajar una capa" isDisabled={singleSelectedLocked}>
                <ChevronDown width={16} height={16} aria-hidden="true" />
                Bajar una capa
              </MenuItem>
              <MenuItem id="back" className={styles.item} textValue="Enviar al fondo" isDisabled={singleSelectedLocked}>
                <ArrowDownToLine width={16} height={16} aria-hidden="true" />
                Enviar al fondo
              </MenuItem>
            </>
          ) : (
            <>
              <MenuItem id="duplicate" className={styles.item} textValue="Duplicar">
                <Copy width={16} height={16} aria-hidden="true" />
                Duplicar
              </MenuItem>
              <MenuItem id="toggleLock" className={styles.item} textValue={allSelectedLocked ? 'Desbloquear' : 'Bloquear'}>
                {allSelectedLocked ? <LockOpen width={16} height={16} aria-hidden="true" /> : <Lock width={16} height={16} aria-hidden="true" />}
                {allSelectedLocked ? 'Desbloquear' : 'Bloquear'}
              </MenuItem>
              <MenuItem id="delete" className={styles.item} textValue="Eliminar" isDisabled={!hasDeletable}>
                <Trash2 width={16} height={16} aria-hidden="true" />
                Eliminar
              </MenuItem>
              <Separator className={styles.separator} />
              <MenuItem id="front" className={styles.item} textValue="Traer al frente">
                <ArrowUpToLine width={16} height={16} aria-hidden="true" />
                Traer al frente
              </MenuItem>
              <MenuItem id="back" className={styles.item} textValue="Enviar al fondo">
                <ArrowDownToLine width={16} height={16} aria-hidden="true" />
                Enviar al fondo
              </MenuItem>
            </>
          )}
        </MotionMenu>
      </Popover>
    </MenuTrigger>
  )
}
