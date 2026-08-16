import * as Sentry from '@sentry/react'

// WEB-006: GlitchTip is self-hosted and speaks the Sentry ingestion protocol,
// so @sentry/react works against it unmodified — if this ever moves to
// Sentry-hosted or elsewhere, only the DSN changes, not this client code.
const dsn = import.meta.env.VITE_GLITCHTIP_DSN as string | undefined

export function initErrorTracking() {
  if (!dsn) return // local dev without error tracking configured — not an error
  Sentry.init({ dsn })
}
