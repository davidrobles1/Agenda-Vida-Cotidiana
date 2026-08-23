import { useState } from 'react'
import { Button, Menu, MenuItem, MenuTrigger, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import type { CalendarDate } from '@internationalized/date'
import { motionTokens } from '../../motion/tokens'
import { IconChevronRight } from '../icons'
import styles from './MonthSelector.module.css'

const MotionMenu = motion.create(Menu)

const MONTH_NAMES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
]

interface MonthSelectorProps {
  focusedDate: CalendarDate
  onSelectMonth: (date: CalendarDate) => void
}

/**
 * Pedido explícito del usuario (2026-08-22): "junto a 'Vista mensual',
 * agregar selector de meses: Enero | Febrero | ... | Diciembre." Doce
 * botones en fila ocuparían demasiado espacio junto al título de la
 * sección, así que esto es un menú desplegable (mismo patrón real ya
 * usado en `core/theme/ThemeSwitcher.tsx`: `MenuTrigger` + `Button` +
 * `Popover` + `Menu`, Motion anima el `Menu` interno, no el `Popover` —
 * mismo anti-patrón `motion.create(Popover)` ya diagnosticado y evitado
 * varias veces en esta sesión). Solo cambia el mes dentro del año
 * actualmente enfocado — el pedido no menciona año, así que no se agrega
 * navegación de año aquí.
 */
export function MonthSelector({ focusedDate, onSelectMonth }: MonthSelectorProps) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <MenuTrigger isOpen={isOpen} onOpenChange={setIsOpen}>
      <Button className={styles.trigger} aria-label="Elegir mes">
        {MONTH_NAMES[focusedDate.month - 1]}
        <IconChevronRight width={14} height={14} className={styles.chevron} />
      </Button>
      <Popover placement="bottom start" offset={6} className={styles.popover}>
        <MotionMenu
          className={styles.menu}
          aria-label="Elegir mes"
          selectionMode="single"
          selectedKeys={new Set([String(focusedDate.month)])}
          onSelectionChange={(keys) => {
            if (keys === 'all') return
            const [next] = keys
            if (typeof next !== 'string') return
            onSelectMonth(focusedDate.set({ month: Number(next), day: 1 }))
          }}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {MONTH_NAMES.map((name, index) => (
            <MenuItem key={index} id={String(index + 1)} className={styles.item} textValue={name}>
              {name}
            </MenuItem>
          ))}
        </MotionMenu>
      </Popover>
    </MenuTrigger>
  )
}
