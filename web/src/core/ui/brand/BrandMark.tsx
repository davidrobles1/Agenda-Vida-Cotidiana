import styles from './BrandMark.module.css'

/**
 * Identidad interna «A · Tiempo» — dirección aprobada por el Product Owner el
 * 2026-09-05 (artefacto «Tres casas de marca», dirección A).
 *
 * Cada contexto del portal tiene **nombre y logo propios**; ninguno dice «Vida
 * Cotidiana», que pasa a ser marca madre/firma corporativa y desaparece de la
 * interfaz de los módulos:
 *
 *   Portal   → Jornada     · el día completo, antes de partirlo
 *   Personal → Cotidiana   · el tramo que es tuyo
 *   Laboral  → Oficio      · la franja de trabajo
 *
 * El símbolo es UNO SOLO — el arco del día — y lo único que distingue las tres
 * identidades es **la posición del punto sobre el arco**. Esa es la lógica del
 * sistema y no debe alterarse: no hay iconos por módulo (ni casa para Personal
 * ni maletín para Laboral), no hay monograma «VC», no hay bajada de texto.
 */
export type BrandContext = 'PORTAL' | 'PERSONAL' | 'LABORAL'

/**
 * Construcción vectorial reproducida tal cual del artefacto aprobado
 * (`arcA(w, pos, color)` + `lockA(cfg, size)`), con el tamaño de barra que allí
 * se usaba: `sizes.bar = 15`. Todas las medidas se derivan de él, así que la
 * geometría es la misma que se aprobó, no una reinterpretación.
 */
const SIZE = 15 //                        cuerpo del rótulo, en px
const W = Math.round(SIZE * 2.5) //   38  ancho del arco = 2.5 × cuerpo
const H = Math.round(W * 0.42) //     16  alto del arco  = 0.42 × ancho
const RX = (W - 8) / 2 //             15  radios de la elipse del arco
const RY = H - 7 //                    9
const BASE = H - 2 //                 14  línea de apoyo (el horizonte del día)
const DOT_R = W > 110 ? 4 : 2.8 //   2.8  el punto engorda solo en tamaños grandes
const ARC_PATH = `M4 ${BASE} A ${RX} ${RY} 0 0 1 ${W - 4} ${BASE}`

interface BrandIdentity {
  /** Nombre propio del contexto. No lleva «Vida Cotidiana» debajo. */
  name: string
  /** Posición del punto sobre el arco, 0 = amanecer, 1 = anochecer. */
  pos: number
  /** Token del color de contexto. Ver `--brand-arc-*` en `index.css`. */
  accent: string
}

const IDENTITIES: Record<BrandContext, BrandIdentity> = {
  // El día entero: el punto corona el arco, en el cenit.
  PORTAL: { name: 'Jornada', pos: 0.5, accent: 'var(--brand-arc-portal)' },
  // El tramo propio: el punto cae en la tarde.
  PERSONAL: { name: 'Cotidiana', pos: 0.82, accent: 'var(--brand-arc-personal)' },
  // La franja de trabajo: el punto sube por la mañana.
  LABORAL: { name: 'Oficio', pos: 0.22, accent: 'var(--brand-arc-laboral)' },
}

/** El punto se apoya sobre el arco, no flota: misma elipse, misma parametrización. */
function dotAt(pos: number) {
  return {
    cx: (4 + (W - 8) * pos).toFixed(1),
    cy: (BASE - Math.sin(pos * Math.PI) * RY).toFixed(1),
  }
}

interface BrandMarkProps {
  context: BrandContext
  className?: string
}

/**
 * El logo es un elemento gráfico independiente: **sin fondo, sin placa, sin
 * contenedor y sin sombra**. El fondo lo pone la superficie donde se coloque
 * (`AppShell`), nunca el logo. Escala proporcionalmente desde `--brand-size`
 * — una sola construcción, no un dibujo distinto por dispositivo.
 */
export function BrandMark({ context, className }: BrandMarkProps) {
  const identity = IDENTITIES[context]
  const { cx, cy } = dotAt(identity.pos)

  return (
    <span
      className={className ? `${styles.lockup} ${className}` : styles.lockup}
      data-testid="app-brand-mark"
      data-brand-context={context}
    >
      <svg
        className={styles.arc}
        viewBox={`0 0 ${W} ${H}`}
        role="presentation"
        aria-hidden="true"
        focusable="false"
      >
        <path
          d={ARC_PATH}
          fill="none"
          stroke={identity.accent}
          strokeWidth={1.6}
          strokeLinecap="round"
          opacity={0.38}
        />
        <circle cx={cx} cy={cy} r={DOT_R} fill={identity.accent} />
      </svg>
      <span className={styles.name} data-brand="name">
        {identity.name}
      </span>
    </span>
  )
}
