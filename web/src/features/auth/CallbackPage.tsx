import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { consumeJustRegistered, consumePostLoginPath, handleCallback } from '../../core/auth/authClient'
import { getCurrentUser } from '../../core/user/api'
import { resolveModeState } from '../../core/user/modes'

/**
 * Destino tras iniciar sesión (2026-08-29, pedido explícito del usuario).
 *
 * Sustituye a la regla de ADR-015(d)/FR-015, que mandaba siempre al
 * Calendario general. Ahora se entra por la pantalla de inicio del módulo
 * que el usuario tenga activo, y Personal tiene prioridad cuando los dos lo
 * están. Es coherente con el cambio del selector de módulos del AppShell,
 * que ya lleva a Inicio y a Hoy en vez de a los calendarios.
 *
 * Sin ningún modo activo el usuario no puede usar la app (FR-014), así que
 * ahí sigue yendo a onboarding.
 */
function landingPathFor(state: { personalEnabled: boolean; laboralEnabled: boolean }): string {
  if (state.personalEnabled) return '/personal/home'
  if (state.laboralEnabled) return '/laboral/hoy'
  return '/onboarding'
}

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
      .then(async () => {
        // El destino tras el login lo decide `landingPathFor` (ver su
        // comentario). Un registro recién hecho (la marca
        // `vc_just_registered` de register(), en authClient.ts) siempre pasa
        // primero por onboarding: FR-014 exige elegir al menos un modo antes
        // de poder usar la app. Un login normal consulta los modos reales de
        // GET /me (con el respaldo documentado en modes.ts para mientras el
        // backend no los devuelva).
        if (consumeJustRegistered()) {
          navigate('/onboarding', { replace: true })
          return
        }
        // Reautenticación silenciosa a nivel de pestaña (authClient
        // escalateToTopLevelReauth): el usuario no pidió iniciar sesión, se
        // le sacó de donde estaba para revalidar. Devolverlo a Calendario
        // sería perder su sitio, así que vuelve exactamente a su pantalla.
        const back = consumePostLoginPath()
        if (back) {
          navigate(back, { replace: true })
          return
        }
        try {
          const user = await getCurrentUser()
          const state = resolveModeState(user)
          navigate(landingPathFor(state), { replace: true })
        } catch {
          // Fail open en vez de dejar al usuario atrapado en /callback —
          // misma postura que la carga del nombre en AppShell. Se elige
          // Personal/Inicio porque `resolveModeState` también trata Personal
          // como activo por omisión cuando el backend no informa modos.
          navigate('/personal/home', { replace: true })
        }
      })
      .catch((e) => {
        // Un fallo aquí no debe dejar al usuario atrapado en /callback
        // mirando un mensaje técnico: authClient ya dejó escrito el motivo
        // legible (getSessionNotice) y el login lo muestra.
        setError(e instanceof Error ? e.message : 'Login failed')
        navigate('/', { replace: true })
      })
  }, [navigate])

  if (error) return <p role="alert">{error}</p>
  return <p>Signing in…</p>
}
