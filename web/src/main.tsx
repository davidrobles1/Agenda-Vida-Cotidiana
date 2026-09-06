import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MotionConfig } from 'motion/react'
import * as Sentry from '@sentry/react'
// UX-001/design-system.md §2: self-hosted via @fontsource (real font files
// bundled with the app, no request to a Google Fonts CDN on every page load).
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
// UX-008: "Agenda" identity's display/accent typeface — Fraunces, self-hosted
// the same way as Inter above. Replaces 'Brush Script MT'/'Segoe Script' from
// the original LoginPage.tsx redesign: those are Windows-only fonts that
// silently fall back to the browser's generic `cursive` (unpredictable per
// OS/browser) on Mac/Linux/Android/iOS — confirmed as a real cross-platform
// gap, not a hypothetical one. Fraunces' real italic (500-italic/600-italic)
// stands in for the script accent instead — same warm/editorial character,
// renders identically everywhere.
// El 300 lo pide la identidad «A · Tiempo» (BrandMark): el rótulo del logo va
// en Fraunces ligero, y sin este fichero caería al 500 y el arco dejaría de
// pesar más que el nombre — que es justo lo que sostiene el lock-up.
import '@fontsource/fraunces/300.css'
import '@fontsource/fraunces/500.css'
import '@fontsource/fraunces/500-italic.css'
import '@fontsource/fraunces/600.css'
import '@fontsource/fraunces/600-italic.css'
// LoginPage "Meraki" wordmark: a real connected-script face, self-hosted the
// same way as Inter/Fraunces above — this is what makes a script font safe to
// use at all (see the note above): the cross-platform gap was the OS-supplied
// cursive fallback, not script lettering itself.
import '@fontsource/alex-brush/400.css'
// UX-014: Editorial visual theme's "typewriter ledger" accent for
// timestamps/metadata only (never body text) — self-hosted the same way as
// the rest of this block, so it doesn't reintroduce the OS-fallback risk a
// bare `font-family: monospace` would (no real cross-platform "typewriter"
// generic exists the way `serif`/`sans-serif` do).
import '@fontsource/courier-prime/400.css'
import '@fontsource/courier-prime/400-italic.css'

import '@fontsource/manrope/400.css'
import '@fontsource/manrope/700.css'

import '@fontsource/playfair-display/400.css'
import '@fontsource/playfair-display/700.css'

import '@fontsource/cormorant-garamond/400.css'
import '@fontsource/cormorant-garamond/700.css'

import '@fontsource/caveat/400.css'
import '@fontsource/caveat/700.css'

import '@fontsource/lobster/400.css'
import '@fontsource/parisienne/400.css'
import '@fontsource/pacifico/400.css'

import '@fontsource/libre-baskerville/400.css'
import '@fontsource/libre-baskerville/700.css'

import '@fontsource/bodoni-moda/400.css'
import '@fontsource/bodoni-moda/700.css'

import '@fontsource/ibm-plex-mono/400.css'
import '@fontsource/ibm-plex-mono/500.css'
import '@fontsource/ibm-plex-mono/700.css'

// ADR-023 — tipografías de display de los seis temas aprobados. Cada agenda
// tiene su propia pareja; sin estos ficheros el navegador caería al genérico
// del sistema y la identidad del tema se perdería. Self-hosted igual que el
// resto del bloque, sin peticiones a un CDN.
import '@fontsource/sora/300.css' //          Aurora · display ligero del héroe
import '@fontsource/sora/400.css'
import '@fontsource/sora/600.css'
// Neo · Archivo. Hacen falta también los pesos normales: `--font-display` lo
// heredan elementos que no son titulares (rótulo de marca, subtítulos), y sin
// el 400/500/600 esos caerían al `system-ui` de reserva.
import '@fontsource/archivo/400.css'
import '@fontsource/archivo/500.css'
import '@fontsource/archivo/600.css'
import '@fontsource/archivo/700.css'
import '@fontsource/archivo/800.css'
import '@fontsource/archivo/900.css'
import '@fontsource/plus-jakarta-sans/400.css' // Calm · cuerpo y display
import '@fontsource/plus-jakarta-sans/500.css'
import '@fontsource/plus-jakarta-sans/600.css'
import '@fontsource/plus-jakarta-sans/700.css'
import '@fontsource/instrument-serif/400.css' //  Studio · titular de revista
import '@fontsource/instrument-serif/400-italic.css'
// Manrope (Lumen) necesita más pesos de los dos que ya había: el titular va a
// 200 y el cuerpo a 500.
import '@fontsource/manrope/200.css'
import '@fontsource/manrope/300.css'
import '@fontsource/manrope/500.css'
import '@fontsource/manrope/600.css'
// Caveat 500/600 — la letra a mano de Papel (nota al margen y notas del día).
import '@fontsource/caveat/500.css'
import '@fontsource/caveat/600.css'

import './index.css'
// ADR-023: después de index.css a propósito — los temas redefinen tokens que
// `:root` declara antes, y el orden de importación fija la cascada.
import './themes.css'
import App from './App.tsx'
import { initErrorTracking } from './core/errors/glitchtip'
import { restoreSession } from './core/auth/authClient'

initErrorTracking()

// Se lanza antes de renderizar para que el intento de recuperar la sesión SSO
// (ver restoreSession en authClient.ts) vaya lo más adelantado posible: la app
// arranca en estado "restoring" y solo decide si hay sesión cuando Keycloak
// contesta, en vez de mandar al login por el simple hecho de que la memoria
// esté vacía tras la recarga. No se espera el resultado: el router ya reacciona
// al cambio de estado por su cuenta.
void restoreSession()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Sentry.ErrorBoundary fallback={<p role="alert">Something went wrong. Please reload the page.</p>}>
      {/* UX-011: single global prefers-reduced-motion gate for every Motion
          animation in the app — components never need to check it themselves. */}
      <MotionConfig reducedMotion="user">
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </MotionConfig>
    </Sentry.ErrorBoundary>
  </StrictMode>,
)
