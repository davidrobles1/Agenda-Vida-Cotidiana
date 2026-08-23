import { useState, type RefObject } from 'react'
import { Button, Dialog, DialogTrigger, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { EmojiPicker, type Emoji } from 'frimousse'
import { motionTokens } from '../../motion/tokens'
import { EMOJI_TRIGGER_ICON } from './pickerCatalog'
import styles from './Pickers.module.css'

const MotionDialog = motion.create(Dialog)

interface EmojiPickerButtonProps {
  /** Campo real (input o textarea) donde se inserta el emoji — título o
      descripción, el mismo componente sirve para ambos. */
  targetRef: RefObject<HTMLInputElement | HTMLTextAreaElement | null>
  value: string
  onChange: (value: string) => void
  /** Nombre accesible del Dialog interno — distingue "insertar en título"
      de "insertar en descripción" para lectores de pantalla. */
  dialogLabel: string
  'aria-label'?: string
}

/**
 * Selector de emoji real, compartido por Notas y Agenda — reemplaza
 * `emoji-picker-react` por `frimousse` (Liveblocks, MIT): headless, sin
 * ningún `localStorage`/persistencia propia documentada (verificado contra
 * su API real — no expone ninguna opción de "recientes" que pudiera
 * escribir en disco, a diferencia de `emoji-picker-react`, que sí lo hacía
 * de forma incondicional sin poder desactivarse). Ver informe de esta
 * tarea para el detalle completo de la comparación.
 *
 * React Aria `DialogTrigger` + `Popover` + `Dialog`, Motion anima el
 * `Dialog` interno — no el `Popover` (bug real encontrado en vivo, pedido
 * explícito del usuario 2026-08-22: mismo anti-patrón ya diagnosticado y
 * corregido varias veces para Vision Board — el `useResizeObserver`/
 * `useLayoutEffect` interno de `Popover` se re-renderiza después del
 * montaje y pisa el estilo inline animado de Motion de vuelta a `initial`,
 * dejando el panel invisible pero seguía siendo clicable debajo).
 *
 * Genérico sobre el campo destino (`targetRef`): inserta en la posición
 * real del cursor de CUALQUIER `<input>`/`<textarea>` que se le pase — así
 * el mismo componente sirve para Título y para Descripción sin duplicar
 * lógica (pedido explícito).
 */
export function EmojiPickerButton({
  targetRef,
  value,
  onChange,
  dialogLabel,
  'aria-label': ariaLabel = 'Insertar emoji',
}: EmojiPickerButtonProps) {
  const [open, setOpen] = useState(false)

  function handleEmojiSelect(emoji: Emoji) {
    const target = targetRef.current

    if (!target) {
      onChange(`${value}${emoji.emoji}`)
      setOpen(false)
      return
    }

    const start = target.selectionStart ?? value.length
    const end = target.selectionEnd ?? value.length
    const next = `${value.slice(0, start)}${emoji.emoji}${value.slice(end)}`

    onChange(next)
    setOpen(false)

    const cursor = start + emoji.emoji.length
    requestAnimationFrame(() => {
      target.focus()
      target.setSelectionRange(cursor, cursor)
    })
  }

  return (
    <DialogTrigger isOpen={open} onOpenChange={setOpen}>
      {/* Bug real encontrado en vivo (pedido explícito del usuario,
          2026-08-22): sin `type="button"`, React Aria's `Button` deja
          pasar el `type` tal cual al `<button>` real — sin uno explícito,
          un `<button>` dentro de un `<form>` (este trigger vive dentro del
          form de título/descripción de Notas y Agenda) por defecto es
          `type="submit"`. Un clic aquí disparaba un submit implícito del
          formulario completo, guardando y cerrando el diálogo de creación
          entero ("nos regresamos al calendario") — nunca fue un problema
          de propagación de eventos ni del Popover en sí. */}
      <Button type="button" className={styles.emojiTriggerButton} aria-label={ariaLabel}>
        <EMOJI_TRIGGER_ICON width={16} height={18} />
      </Button>
      <Popover placement="bottom start" offset={6} className={styles.emojiPopover}>
        <MotionDialog
          aria-label={dialogLabel}
          className={styles.emojiDialog}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: open ? 1 : 0, scale: open ? 1 : 0.96, y: open ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          <EmojiPicker.Root
            onEmojiSelect={handleEmojiSelect}
            locale="es-mx"
            columns={8}
            className={styles.emojiRoot}
          >
            <EmojiPicker.Search className={styles.emojiSearch} placeholder="Buscar…" />
            <EmojiPicker.Viewport className={styles.emojiViewport}>
              <EmojiPicker.Loading className={styles.emojiLoading}>Cargando…</EmojiPicker.Loading>
              <EmojiPicker.Empty className={styles.emojiEmpty}>Sin resultados.</EmojiPicker.Empty>
              <EmojiPicker.List className={styles.emojiList} />
            </EmojiPicker.Viewport>
          </EmojiPicker.Root>
        </MotionDialog>
      </Popover>
    </DialogTrigger>
  )
}
