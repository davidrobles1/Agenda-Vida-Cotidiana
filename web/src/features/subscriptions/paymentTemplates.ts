import type { BillingCycle, PaymentKind } from './api'

/**
 * ADR-020: plantillas de alta.
 *
 * Azúcar de interfaz sobre el tipo, con **coste cero en el modelo**: no son
 * una entidad, ni una categoría, ni se guardan. Solo prellenan el
 * formulario. Es lo que convierte un alta de siete campos en una de dos.
 *
 * La lista es deliberadamente corta y de casos reales del producto
 * (streaming, servicios del hogar, membresía, renta, tarjeta). Añadir
 * cincuenta marcas la volvería un catálogo que hay que mantener y que
 * envejece; "Otro" cubre el resto sin prometer nada.
 */
export interface PaymentTemplate {
  id: string
  label: string
  kind: PaymentKind
  billingCycle: BillingCycle
  /** Nombre sugerido. Vacío en las genéricas, que solo fijan el tipo. */
  service: string
}

export const PAYMENT_TEMPLATES: PaymentTemplate[] = [
  { id: 'netflix', label: 'Netflix', kind: 'SUBSCRIPTION', billingCycle: 'MONTHLY', service: 'Netflix' },
  { id: 'spotify', label: 'Spotify', kind: 'SUBSCRIPTION', billingCycle: 'MONTHLY', service: 'Spotify' },
  { id: 'internet', label: 'Internet', kind: 'SERVICE', billingCycle: 'MONTHLY', service: 'Internet' },
  { id: 'telefono', label: 'Teléfono', kind: 'SERVICE', billingCycle: 'MONTHLY', service: 'Teléfono' },
  { id: 'renta', label: 'Renta', kind: 'CUSTOM', billingCycle: 'MONTHLY', service: 'Renta' },
  { id: 'gimnasio', label: 'Gimnasio', kind: 'MEMBERSHIP', billingCycle: 'MONTHLY', service: 'Gimnasio' },
  { id: 'seguro', label: 'Seguro', kind: 'SERVICE', billingCycle: 'MONTHLY', service: 'Seguro' },
  { id: 'tarjeta', label: 'Tarjeta de crédito', kind: 'CARD', billingCycle: 'MONTHLY', service: '' },
  { id: 'credito', label: 'Crédito', kind: 'CREDIT', billingCycle: 'MONTHLY', service: '' },
  { id: 'otro', label: 'Otro', kind: 'CUSTOM', billingCycle: 'MONTHLY', service: '' },
]
