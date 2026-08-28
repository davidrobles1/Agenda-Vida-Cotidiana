import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './GlobalIconTooltip.module.css'

/**
 * Pedido explícito del usuario (2026-08-23): tooltip para TODO control que
 * se muestre solo con icono, en todos los módulos, "de forma
 * reutilizable/global, evitando implementarlo manualmente botón por botón".
 *
 * Por eso es UN solo componente montado una vez en la raíz (App.tsx) que
 * escucha en `document`, en vez de envolver cada botón:
 *
 * - Cero cambios en los ~cientos de botones que ya existen. Un botón nuevo
 *   que se agregue mañana con `aria-label` y solo un icono adentro ya
 *   queda cubierto sin tocar nada.
 * - Reutiliza el `aria-label` que ya tiene cada control (requisito
 *   explícito: no duplicar textos).
 * - Se renderiza en un portal sobre `document.body`, así NO lo recorta
 *   ningún contenedor con `overflow: hidden/auto`. Eso descartó la
 *   alternativa CSS-only (`::after` con `attr(aria-label)`): la barra
 *   lateral del Vision Board tiene `overflow-y: auto` y habría cortado el
 *   tooltip justo donde más falta hace.
 * - También descartó envolver cada botón en el `TooltipTrigger` de React
 *   Aria: además del trabajo botón por botón, habría que anidarlo dentro
 *   de los `DialogTrigger`/`MenuTrigger` que ya envuelven a muchos de
 *   estos triggers.
 *
 * REGLA DE "SOLO ICONO" (requisito explícito: si el control ya muestra
 * texto, no debe llevar tooltip): se decide en tiempo de ejecución con
 * `textContent`. Si el control tiene cualquier texto visible, se ignora.
 * Eso hace que funcione solo en los temas/modos que muestran solo el
 * icono, sin necesidad de saber cuáles son: en un tema que muestre
 * icono + texto, el mismo botón simplemente deja de calificar.
 */

/** Texto dentro de un nodo visualmente oculto (patrón `VisuallyHidden` de
    React Aria: 1×1 px recortado) NO cuenta como "texto visible" — si
    contara, un botón icon-only con su etiqueta para lectores de pantalla
    quedaría sin tooltip. */
function isVisuallyHidden(el: Element): boolean {
  const rect = el.getBoundingClientRect()
  if (rect.width <= 1 && rect.height <= 1) return true
  const style = getComputedStyle(el)
  return style.clipPath === 'inset(50%)' || style.clip === 'rect(0px, 0px, 0px, 0px)'
}

function hasVisibleText(el: Element): boolean {
  for (const node of Array.from(el.childNodes)) {
    if (node.nodeType === Node.TEXT_NODE) {
      if ((node.textContent ?? '').trim() !== '') return true
      continue
    }
    if (node.nodeType !== Node.ELEMENT_NODE) continue
    const child = node as Element
    // Un icono nunca aporta texto visible.
    if (child.tagName === 'SVG' || child.tagName === 'IMG') continue
    if ((child.textContent ?? '').trim() === '') continue
    if (isVisuallyHidden(child)) continue
    return true
  }
  return false
}

/** Elementos que cuentan como "control": lo que un usuario puede accionar
    y que por tanto merece explicación cuando solo muestra un icono. */
const CONTROL_SELECTOR = 'button, [role="button"], a[href], [role="menuitem"], [role="radio"], [role="tab"]'

interface TooltipState {
  label: string
  x: number
  y: number
  placement: 'top' | 'bottom'
}

const GAP = 8

/** Margen mínimo entre el tooltip y el borde de la ventana. */
const VIEWPORT_MARGIN = 8

export function GlobalIconTooltip() {
  const [tip, setTip] = useState<TooltipState | null>(null)
  const activeRef = useRef<Element | null>(null)
  const tooltipRef = useRef<HTMLDivElement | null>(null)

  /**
   * El tooltip se centra sobre el control (`translateX(-50%)`), así que en
   * un botón pegado al borde izquierdo o derecho la mitad del texto se
   * salía de la ventana y quedaba cortada. No se puede resolver solo con
   * CSS porque el ancho depende del texto: hay que medirlo ya renderizado
   * y recolocarlo. `useLayoutEffect` (no `useEffect`) para que el ajuste
   * ocurra antes del paint y nunca se vea el salto.
   */
  useLayoutEffect(() => {
    const node = tooltipRef.current
    if (!node || !tip) return

    // Se parte siempre de la posición centrada: sin esto, al pasar de un
    // botón a otro se acumularía la corrección del anterior.
    node.style.transform = tip.placement === 'bottom' ? 'translate(-50%, 0)' : 'translate(-50%, -100%)'

    const rect = node.getBoundingClientRect()

    let shiftX = 0
    if (rect.left < VIEWPORT_MARGIN) {
      shiftX = VIEWPORT_MARGIN - rect.left
    } else if (rect.right > window.innerWidth - VIEWPORT_MARGIN) {
      shiftX = window.innerWidth - VIEWPORT_MARGIN - rect.right
    }

    // Si tampoco cabe abajo (control muy pegado al pie de la ventana), se
    // voltea hacia arriba; el caso contrario ya lo cubre `placement`.
    let shiftY = 0
    if (tip.placement === 'bottom' && rect.bottom > window.innerHeight - VIEWPORT_MARGIN) {
      shiftY = -(rect.height + 2 * GAP)
    }

    if (shiftX !== 0 || shiftY !== 0) {
      node.style.transform =
        `translate(calc(-50% + ${shiftX}px), ${tip.placement === 'bottom' ? '0px' : '-100%'}) ` +
        `translateY(${shiftY}px)`
    }
  }, [tip])

  useEffect(() => {
    function show(target: Element) {
      const label = target.getAttribute('aria-label')?.trim()
      if (!label) return
      // Requisito explícito: si ya muestra texto, sin tooltip.
      if (hasVisibleText(target)) return

      const rect = target.getBoundingClientRect()
      if (rect.width === 0 && rect.height === 0) return

      // Debajo cuando no hay espacio arriba — mismo criterio que el
      // toolbar de Notas del día, para no salirse de la ventana.
      const placement: TooltipState['placement'] = rect.top < 48 ? 'bottom' : 'top'
      activeRef.current = target
      setTip({
        label,
        x: rect.left + rect.width / 2,
        y: placement === 'top' ? rect.top - GAP : rect.bottom + GAP,
        placement,
      })
    }

    function hide() {
      activeRef.current = null
      setTip(null)
    }

    function findControl(raw: EventTarget | null): Element | null {
      if (!(raw instanceof Element)) return null
      const control = raw.closest(CONTROL_SELECTOR)
      if (!control) return null
      if (control.getAttribute('aria-hidden') === 'true') return null
      return control
    }

    function handlePointerOver(event: PointerEvent) {
      const control = findControl(event.target)
      if (!control) {
        if (activeRef.current) hide()
        return
      }
      if (control === activeRef.current) return
      show(control)
    }

    function handleFocusIn(event: FocusEvent) {
      const control = findControl(event.target)
      if (control) show(control)
      else if (activeRef.current) hide()
    }

    // Cualquier interacción real esconde el tooltip: ya no aporta nada y
    // estorbaría sobre el popover/menú que se acaba de abrir.
    document.addEventListener('pointerover', handlePointerOver, true)
    document.addEventListener('pointerdown', hide, true)
    document.addEventListener('focusin', handleFocusIn, true)
    document.addEventListener('focusout', hide, true)
    document.addEventListener('keydown', hide, true)
    window.addEventListener('scroll', hide, true)
    window.addEventListener('resize', hide)
    window.addEventListener('blur', hide)

    return () => {
      document.removeEventListener('pointerover', handlePointerOver, true)
      document.removeEventListener('pointerdown', hide, true)
      document.removeEventListener('focusin', handleFocusIn, true)
      document.removeEventListener('focusout', hide, true)
      document.removeEventListener('keydown', hide, true)
      window.removeEventListener('scroll', hide, true)
      window.removeEventListener('resize', hide)
      window.removeEventListener('blur', hide)
    }
  }, [])

  if (!tip) return null

  return createPortal(
    // `aria-hidden`: el nombre accesible ya lo da el propio `aria-label`
    // del control — anunciarlo otra vez aquí lo duplicaría.
    <div
      ref={tooltipRef}
      className={`${styles.tooltip} ${tip.placement === 'bottom' ? styles.tooltipBottom : ''}`}
      style={{ left: tip.x, top: tip.y }}
      aria-hidden="true"
    >
      {tip.label}
    </div>,
    document.body,
  )
}
