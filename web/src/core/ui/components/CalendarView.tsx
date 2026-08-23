import {
  useRef,
  useState,
} from 'react'

import {
  Button,
  Calendar,
  CalendarCell,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHeader,
  CalendarHeaderCell,
  CalendarHeading,
  I18nProvider,
} from 'react-aria-components'

import type { CalendarDate } from '@internationalized/date'

import {
  getLocalTimeZone,
  parseDate,
  today,
} from '@internationalized/date'

import {
  AnimatePresence,
  motion,
  type PanInfo,
} from 'motion/react'

import { motionTokens } from '../../motion/tokens'

import {
  IconChevronLeft,
  IconChevronRight,
} from '../icons'

import styles from './CalendarView.module.css'

import type { Tone } from './MetricCard'

export interface CalendarMarker {
  tone: Tone

  /**
   * Se mantiene para compatibilidad con
   * el modelo existente.
   *
   * La vista mensual no depende de este
   * color para representar la tarea.
   */
  contextColor?: string

  /**
   * Título general de la actividad.
   */
  label: string
}

export interface CalendarLegendEntry {
  label: string
  tone: Tone
}

interface CalendarViewProps {
  markersByDay: Record<
    string,
    CalendarMarker[]
  >

  selectedDateKey: string

  onSelectDate: (
    dateKey: string,
  ) => void

  /** Pedido explícito del usuario (2026-08-22): selector de mes junto a
      "Vista mensual" — mismo patrón "optional-controlled" ya usado en
      Vision Board (ej. VisionBoardDeleteConfirm.tsx): si el padre pasa
      ambos, controla qué mes se muestra (saltar directo a un mes
      elegido); si no, el componente sigue manejando su propio estado
      interno exactamente como antes, sin ningún cambio de comportamiento. */
  focusedDate?: CalendarDate
  onFocusedDateChange?: (date: CalendarDate) => void
}

const SWIPE_THRESHOLD = 60

/**
 * Cantidad máxima de actividades
 * visibles directamente dentro de
 * cada día.
 */
const MAX_VISIBLE_TASKS_PER_DAY = 3

const monthVariants = {
  enter: (
    direction: number,
  ) => ({
    opacity: 0,
    x:
      direction >= 0
        ? 28
        : -28,
  }),

  center: {
    opacity: 1,
    x: 0,
  },

  exit: (
    direction: number,
  ) => ({
    opacity: 0,
    x:
      direction >= 0
        ? -28
        : 28,
  }),
}

function absoluteMonth(
  date: CalendarDate,
): number {
  return (
    date.year * 12 +
    date.month
  )
}

export function CalendarView({
  markersByDay,
  selectedDateKey,
  onSelectDate,
  focusedDate: focusedDateProp,
  onFocusedDateChange,
}: CalendarViewProps) {
  const [
    internalFocusedDate,
    setInternalFocusedDate,
  ] = useState(
    () =>
      today(
        getLocalTimeZone(),
      ),
  )

  const focusedDate = focusedDateProp ?? internalFocusedDate
  const setFocusedDate = onFocusedDateChange ?? setInternalFocusedDate

  const [
    direction,
    setDirection,
  ] = useState(0)

  const prevAbsMonthRef =
    useRef(
      absoluteMonth(
        focusedDate,
      ),
    )

  function handleFocusChange(
    date: CalendarDate,
  ) {
    const nextAbs =
      absoluteMonth(date)

    const prevAbs =
      prevAbsMonthRef.current

    if (
      nextAbs !==
      prevAbs
    ) {
      setDirection(
        nextAbs > prevAbs
          ? 1
          : -1,
      )

      prevAbsMonthRef.current =
        nextAbs
    }

    setFocusedDate(date)
  }

  function handleToday() {
    const now =
      today(
        getLocalTimeZone(),
      )

    handleFocusChange(now)

    onSelectDate(
      now.toString(),
    )
  }

  function handleDragEnd(
    _event:
      | PointerEvent
      | MouseEvent
      | TouchEvent,
    info: PanInfo,
  ) {
    if (
      info.offset.x <=
      -SWIPE_THRESHOLD
    ) {
      handleFocusChange(
        focusedDate.add({
          months: 1,
        }),
      )
    } else if (
      info.offset.x >=
      SWIPE_THRESHOLD
    ) {
      handleFocusChange(
        focusedDate.add({
          months: -1,
        }),
      )
    }
  }

  const monthKey =
    `${focusedDate.year}-${focusedDate.month}`

  return (
    <I18nProvider locale="es-MX">
      <Calendar
        aria-label="Calendario"
        firstDayOfWeek="mon"
        value={parseDate(
          selectedDateKey,
        )}
        onChange={(date) =>
          onSelectDate(
            date.toString(),
          )
        }
        focusedValue={
          focusedDate
        }
        onFocusChange={
          handleFocusChange
        }
        className={
          styles.calendar
        }
      >
        {/* =====================================================
            CALENDAR HEADER
            ===================================================== */}

        <div
          className={
            styles.header
          }
        >
          <button
            type="button"
            className={
              styles.todayButton
            }
            onClick={
              handleToday
            }
          >
            <span>
              Hoy
            </span>
          </button>

          <div
            className={
              styles.monthNav
            }
          >
            <Button
              slot="previous"
              className={
                styles.navButton
              }
              aria-label="Mes anterior"
            >
              <IconChevronLeft
                width={18}
                height={18}
              />
            </Button>

            <CalendarHeading
              className={
                styles.monthLabel
              }
            />

            <Button
              slot="next"
              className={
                styles.navButton
              }
              aria-label="Mes siguiente"
            >
              <IconChevronRight
                width={18}
                height={18}
              />
            </Button>
          </div>

          <div
            className={
              styles.headerDecoration
            }
            aria-hidden="true"
          >
            <span />
            <span />
            <span />
          </div>
        </div>

        {/* =====================================================
            MONTH GRID
            ===================================================== */}

        <div
          className={
            styles.gridViewport
          }
        >
          <AnimatePresence
            mode="popLayout"
            initial={false}
            custom={
              direction
            }
          >
            <motion.div
              key={monthKey}
              custom={
                direction
              }
              variants={
                monthVariants
              }
              initial="enter"
              animate="center"
              exit="exit"
              transition={
                motionTokens.spatial
              }
              drag="x"
              dragConstraints={{
                left: 0,
                right: 0,
              }}
              dragElastic={0.4}
              onDragEnd={
                handleDragEnd
              }
              className={
                styles.monthMotion
              }
            >
              <CalendarGrid
                weekdayStyle="narrow"
                className={
                  styles.grid
                }
              >
                <CalendarGridHeader>
                  {(day) => (
                    <CalendarHeaderCell
                      className={
                        styles.weekday
                      }
                    >
                      {day}
                    </CalendarHeaderCell>
                  )}
                </CalendarGridHeader>

                <CalendarGridBody>
                  {(date) => {
                    const markers =
                      markersByDay[
                        date.toString()
                      ] ?? []

                    return (
                      <CalendarCell
                        date={date}
                        className={
                          styles.dayCell
                        }
                      >
                        {({
                          formattedDate,
                          isOutsideMonth,
                        }) => {
                          const visibleMarkers =
                            markers.slice(
                              0,
                              MAX_VISIBLE_TASKS_PER_DAY,
                            )

                          const hasOverflow =
                            markers.length >
                            MAX_VISIBLE_TASKS_PER_DAY

                          return (
                            <>
                              <div
                                className={
                                  styles.dayTop
                                }
                              >
                                <span
                                  className={
                                    styles.dayNumber
                                  }
                                >
                                  {
                                    formattedDate
                                  }
                                </span>

                                {!isOutsideMonth &&
                                  markers.length >
                                    0 && (
                                    <span
                                      className={
                                        styles.dayActivityCount
                                      }
                                      aria-hidden="true"
                                    >
                                      {
                                        markers.length
                                      }
                                    </span>
                                  )}
                              </div>

                              {!isOutsideMonth &&
                                markers.length >
                                  0 && (
                                  <div
                                    className={
                                      styles.taskList
                                    }
                                  >
                                    {visibleMarkers.map(
                                      (
                                        marker,
                                        index,
                                      ) => (
                                        <span
                                          key={`${marker.label}-${index}`}
                                          className={
                                            styles.taskLabel
                                          }
                                          title={
                                            marker.label
                                          }
                                        >
                                          {
                                            marker.label
                                          }
                                        </span>
                                      ),
                                    )}

                                    {hasOverflow && (
                                      <span
                                        className={
                                          styles.taskOverflow
                                        }
                                      >
                                        (...)
                                      </span>
                                    )}
                                  </div>
                                )}
                            </>
                          )
                        }}
                      </CalendarCell>
                    )
                  }}
                </CalendarGridBody>
              </CalendarGrid>
            </motion.div>
          </AnimatePresence>
        </div>
      </Calendar>
    </I18nProvider>
  )
}