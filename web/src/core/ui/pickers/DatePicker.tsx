import { useMemo } from 'react'
import {
  Button,
  Calendar,
  CalendarCell,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHeader,
  CalendarHeaderCell,
  DateInput,
  DatePicker as AriaDatePicker,
  DateSegment,
  Dialog,
  FieldError,
  Group,
  Heading,
  I18nProvider,
  Label,
  Popover,
  Text,
} from 'react-aria-components'
import {
  CalendarDate,
  CalendarDateTime,
  parseDate,
  parseDateTime,
  type DateValue,
} from '@internationalized/date'
import { motion } from 'motion/react'
import { motionTokens } from '../../motion/tokens'
import { IconCalendar, IconChevronLeft, IconChevronRight } from '../icons'
import styles from './DatePicker.module.css'

const MotionDialog = motion.create(Dialog)

interface DatePickerProps {
  label: string
  /** `YYYY-MM-DD`, o `YYYY-MM-DDTHH:mm` con `withTime`. Cadena vacía = sin
      valor, que es exactamente lo que devuelve hoy un `<input type="date">`
      sin rellenar. */
  value: string
  onChange: (value: string) => void
  /** Con hora, para los campos que hoy son `datetime-local`. */
  withTime?: boolean
  /**
   * Texto de ayuda. NO es un marcador de posición al uso: el campo se
   * compone de segmentos (día, mes, año) y React Aria ya escribe
   * "dd/mm/aaaa" cuando está vacío, así que un placeholder encima sería
   * redundante. Se muestra como descripción bajo el campo.
   */
  placeholder?: string
  isDisabled?: boolean
  isRequired?: boolean
  /** `YYYY-MM-DD`, mismo formato que los atributos `min`/`max` nativos. */
  minValue?: string
  maxValue?: string
  /** Mensaje de error del formulario. Su presencia marca el campo como
      inválido, igual que haría `aria-invalid`. */
  errorMessage?: string
  description?: string
  autoFocus?: boolean
}

/**
 * Selector de fecha propio de la aplicación.
 *
 * SUSTITUYE al `<input type="date">` nativo, cuyo panel es chrome del
 * navegador: no se puede maquetar, no respeta el tema y en cada sistema
 * operativo se ve distinto. Ni `accent-color` ni `color-scheme` arreglan
 * eso, solo lo tiñen.
 *
 * CONSTRUIDO CON LO QUE YA HABÍA — cero dependencias nuevas:
 *   · `react-aria-components` aporta la semántica y el comportamiento
 *     accesible (roles de rejilla, navegación con flechas, Enter, Escape,
 *     Tab, anuncio del mes al lector de pantalla). No se reimplementa nada
 *     de eso a mano.
 *   · `@internationalized/date` representa y manipula las fechas, igual que
 *     ya hacían `CalendarView` y `MonthSelector`.
 *   · `I18nProvider` con `es-MX`, los iconos de `core/ui/icons` y los
 *     tokens de movimiento, como el resto del portal.
 *
 * CONTRATO DE DATOS INTACTO: la propiedad `value` y el `onChange` hablan en
 * `YYYY-MM-DD` (o `YYYY-MM-DDTHH:mm`), exactamente el mismo formato que
 * emitía el input nativo. Ningún formulario cambia cómo construye ni cómo
 * envía la fecha; solo cambia la interfaz con la que se elige.
 */
export function DatePicker({
  label,
  value,
  onChange,
  withTime = false,
  placeholder,
  isDisabled,
  isRequired,
  minValue,
  maxValue,
  errorMessage,
  description,
  autoFocus,
}: DatePickerProps) {
  // Una cadena a medio escribir no debe hacer estallar la pantalla: si no
  // se puede interpretar, el campo simplemente queda vacío.
  const parsed = useMemo(() => toDateValue(value, withTime), [value, withTime])
  const min = useMemo(() => toDateValue(minValue ?? '', withTime), [minValue, withTime])
  const max = useMemo(() => toDateValue(maxValue ?? '', withTime), [maxValue, withTime])

  return (
    <I18nProvider locale="es-MX">
      <AriaDatePicker
        className={styles.picker}
        value={parsed}
        onChange={(next) => onChange(next ? next.toString() : '')}
        granularity={withTime ? 'minute' : 'day'}
        isDisabled={isDisabled}
        isRequired={isRequired}
        isInvalid={!!errorMessage}
        minValue={min ?? undefined}
        maxValue={max ?? undefined}
        shouldForceLeadingZeros
        autoFocus={autoFocus}
      >
        <Label className={styles.label}>{label}</Label>

        {/* El campo: segmentos editables con teclado (día, mes, año) más el
            botón que abre el calendario. Es un `Group`, no un `input`, para
            que cada segmento sea navegable por separado. */}
        <Group className={styles.group}>
          <DateInput className={styles.input}>
            {(segment) => (
              <DateSegment
                segment={segment}
                className={styles.segment}
                // El marcador de posición vacío del propio segmento ya dice
                // "dd/mm/aaaa"; `placeholder` se usa solo como etiqueta
                // accesible adicional cuando el formulario lo trae.
                aria-label={segment.type === 'literal' ? undefined : `${label} — ${segment.type}`}
              />
            )}
          </DateInput>
          <Button className={styles.trigger} aria-label={`Abrir calendario de ${label}`}>
            <IconCalendar width={16} height={16} aria-hidden="true" />
          </Button>
        </Group>

        {(description || placeholder) && (
          <Text slot="description" className={styles.description}>
            {description ?? placeholder}
          </Text>
        )}
        {/* `FieldError` se anuncia al lector de pantalla y se asocia al
            campo automáticamente. */}
        <FieldError className={styles.error}>{errorMessage}</FieldError>

        {/* Popover contextual anclado al campo — nunca un modal a pantalla
            completa en escritorio. En pantallas pequeñas React Aria lo
            recoloca solo para que no se salga del viewport, y el CSS le
            pone tope de altura con scroll interno. */}
        <Popover className={styles.popover} placement="bottom start" offset={6}>
          <MotionDialog
            className={styles.dialog}
            initial={{ opacity: 0, scale: 0.97, y: -4 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={motionTokens.smooth}
          >
            <Calendar className={styles.calendar}>
              <header className={styles.calendarHeader}>
                <Button slot="previous" className={styles.navButton} aria-label="Mes anterior">
                  <IconChevronLeft width={16} height={16} aria-hidden="true" />
                </Button>
                {/* `Heading` lo rellena React Aria con el mes y el año ya
                    localizados; el lector de pantalla lo anuncia al navegar. */}
                <Heading className={styles.monthLabel} />
                <Button slot="next" className={styles.navButton} aria-label="Mes siguiente">
                  <IconChevronRight width={16} height={16} aria-hidden="true" />
                </Button>
              </header>

              <CalendarGrid className={styles.grid}>
                <CalendarGridHeader>
                  {(day) => <CalendarHeaderCell className={styles.weekday}>{day}</CalendarHeaderCell>}
                </CalendarGridHeader>
                <CalendarGridBody>
                  {(date) => <CalendarCell date={date} className={styles.cell} />}
                </CalendarGridBody>
              </CalendarGrid>
            </Calendar>
          </MotionDialog>
        </Popover>
      </AriaDatePicker>
    </I18nProvider>
  )
}

/**
 * Cadena del formulario → valor de `@internationalized/date`.
 *
 * Devuelve `null` —no lanza— ante una cadena vacía o a medio escribir: un
 * formulario en blanco es un estado normal, no un error.
 */
function toDateValue(raw: string, withTime: boolean): DateValue | null {
  if (!raw) return null
  try {
    if (withTime) {
      // `datetime-local` puede venir sin segundos (`2026-09-04T10:30`), que
      // es justo lo que `parseDateTime` acepta.
      return parseDateTime(raw.length === 16 ? raw : raw.slice(0, 16))
    }
    return parseDate(raw.slice(0, 10))
  } catch {
    return null
  }
}

export type { DatePickerProps }
export { CalendarDate, CalendarDateTime }
