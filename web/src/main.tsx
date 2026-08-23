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
import './index.css'
import App from './App.tsx'
import { initErrorTracking } from './core/errors/glitchtip'

initErrorTracking()

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
