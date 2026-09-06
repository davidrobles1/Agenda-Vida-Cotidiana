import { useEffect, useState } from 'react'

/**
 * Devuelve `value` con retardo: solo cambia cuando han pasado `delay`
 * milisegundos sin que `value` se vuelva a mover.
 *
 * Existe por el requisito de la búsqueda de personas (ADR-025 §1): "evita
 * realizar consultas innecesarias por cada carácter introducido". Sin esto,
 * escribir "roberto" son siete peticiones de las que seis ya no interesan
 * cuando llega la séptima.
 *
 * Se escribe aquí y no dentro de la pantalla porque es infraestructura, no
 * una decisión de esa pantalla: cualquier campo que consulte al servidor
 * mientras se teclea necesita exactamente esto.
 */
export function useDebouncedValue<T>(value: T, delay = 350): T {
  const [settled, setSettled] = useState(value)

  useEffect(() => {
    const timer = window.setTimeout(() => setSettled(value), delay)
    // Cada pulsación cancela el temporizador anterior: por eso solo llega a
    // dispararse el de la última.
    return () => window.clearTimeout(timer)
  }, [value, delay])

  return settled
}
