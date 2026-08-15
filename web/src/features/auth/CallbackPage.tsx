import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { handleCallback } from '../../core/auth/authClient'

export function CallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  // The authorization code is single-use (OAuth spec) — React 19 StrictMode
  // double-invokes this effect in dev, which would exchange it twice and
  // have Keycloak correctly reject the second attempt. Guard against that.
  const exchanged = useRef(false)

  useEffect(() => {
    if (exchanged.current) return
    exchanged.current = true

    handleCallback()
      .then(() => navigate('/reminders', { replace: true }))
      .catch((e) => setError(e instanceof Error ? e.message : 'Login failed'))
  }, [navigate])

  if (error) return <p role="alert">{error}</p>
  return <p>Signing in…</p>
}
