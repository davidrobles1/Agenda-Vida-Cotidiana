import { authConfig } from './config'
import { generateCodeChallenge, generateCodeVerifier } from './pkce'

// WEB-002/08c-web-architecture.md: Option 1 (the only one coherent with DEC-007/SPA) —
// the token lives in memory only (this module-level variable), never in
// localStorage/sessionStorage. sessionStorage is used below only for the PKCE
// code_verifier, which is single-use flow state, not a credential.

interface TokenSet {
  accessToken: string
  idToken: string
  refreshToken?: string
  expiresAt: number
}

let tokenSet: TokenSet | null = null
let renewTimer: number | undefined
let listeners: Array<() => void> = []

const CODE_VERIFIER_KEY = 'vc_pkce_code_verifier'
// ADR-015/FR-014: CallbackPage needs to know whether this login just came
// from Keycloak's *registration* form (→ show onboarding) vs. an ordinary
// login (→ go straight to Calendario). The backend doesn't expose a
// "brand-new account" signal on GET /me, so this session-scoped flag is the
// bridge: register() sets it right before the redirect, CallbackPage
// consumes (reads + clears) it once, right after the token exchange.
const JUST_REGISTERED_KEY = 'vc_just_registered'

function notify() {
  for (const listener of listeners) listener()
}

export function subscribe(listener: () => void): () => void {
  listeners.push(listener)
  return () => {
    listeners = listeners.filter((l) => l !== listener)
  }
}

export function getAccessToken(): string | null {
  return tokenSet?.accessToken ?? null
}

export function isAuthenticated(): boolean {
  return tokenSet !== null
}

function authorizeUrl(params: Record<string, string>): string {
  return `${authConfig.issuer}/protocol/openid-connect/auth?${new URLSearchParams(params)}`
}

export async function login(): Promise<void> {
  const verifier = generateCodeVerifier()
  const challenge = await generateCodeChallenge(verifier)
  sessionStorage.setItem(CODE_VERIFIER_KEY, verifier)

  window.location.assign(
    authorizeUrl({
      client_id: authConfig.clientId,
      response_type: 'code',
      redirect_uri: authConfig.redirectUri,
      scope: authConfig.scope,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    }),
  )
}

// WEB-008 (Task B §2): Keycloak's real self-registration form
// (protocol/openid-connect/registrations) — same client, same PKCE flow, same
// redirect_uri as login(), only the endpoint path differs. On submit, Keycloak
// creates the account and redirects back to /callback with an authorization
// code exactly like a normal login, so CallbackPage's existing exchange logic
// handles it unmodified.
function registrationUrl(params: Record<string, string>): string {
  return `${authConfig.issuer}/protocol/openid-connect/registrations?${new URLSearchParams(params)}`
}

export async function register(): Promise<void> {
  const verifier = generateCodeVerifier()
  const challenge = await generateCodeChallenge(verifier)
  sessionStorage.setItem(CODE_VERIFIER_KEY, verifier)
  sessionStorage.setItem(JUST_REGISTERED_KEY, '1')

  window.location.assign(
    registrationUrl({
      client_id: authConfig.clientId,
      response_type: 'code',
      redirect_uri: authConfig.redirectUri,
      scope: authConfig.scope,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    }),
  )
}

async function exchangeCode(code: string, verifier: string): Promise<TokenSet> {
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: authConfig.redirectUri,
    client_id: authConfig.clientId,
    code_verifier: verifier,
  })
  const response = await fetch(`${authConfig.issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!response.ok) throw new Error(`Token exchange failed: ${response.status}`)
  const json = await response.json()
  return {
    accessToken: json.access_token,
    idToken: json.id_token,
    refreshToken: json.refresh_token,
    expiresAt: Date.now() + json.expires_in * 1000,
  }
}

/** See JUST_REGISTERED_KEY above — reads and clears the flag; single-use,
    same reasoning as the PKCE code_verifier. */
export function consumeJustRegistered(): boolean {
  const value = sessionStorage.getItem(JUST_REGISTERED_KEY)
  sessionStorage.removeItem(JUST_REGISTERED_KEY)
  return value === '1'
}

export async function handleCallback(): Promise<void> {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const verifier = sessionStorage.getItem(CODE_VERIFIER_KEY)
  if (!code || !verifier) throw new Error('Missing authorization code or PKCE code_verifier')

  tokenSet = await exchangeCode(code, verifier)
  sessionStorage.removeItem(CODE_VERIFIER_KEY)
  scheduleSilentRenew()
  attachActivityListeners()
  notify()
}

function scheduleSilentRenew(): void {
  if (renewTimer) window.clearTimeout(renewTimer)
  if (!tokenSet) return
  const msUntilRenew = Math.max(tokenSet.expiresAt - Date.now() - 30_000, 5_000)
  renewTimer = window.setTimeout(silentRenew, msUntilRenew)
}

// Pedido explícito del usuario (2026-08-21): "me saca el aplicativo... que
// empiece a contar mientras haya navegación que no me saque" — el único
// timer que existía (scheduleSilentRenew, arriba) es un `setTimeout` de
// ~4.5 minutos que los navegadores retrasan o pausan por completo mientras
// la pestaña está en segundo plano o el equipo duerme (una pestaña
// "congelada" así es la causa real de "me saca" pese a haber estado
// activo: el timer simplemente no llega a dispararse a tiempo, y para
// cuando la pestaña vuelve a primer plano, el token ya expiró y la sesión
// de Keycloak puede haber superado su propio `ssoSessionIdleTimeout`
// mientras tanto — ver infra/keycloak/realm-vida-cotidiana*.json, ahora
// extendido). Esto no reemplaza ese timer — lo complementa: cualquier
// interacción real (clic, tecla, toque, scroll) o que la pestaña vuelva a
// ser visible dispara una verificación inmediata; si el token ya venció o
// está a punto de hacerlo, renueva ahí mismo en vez de esperar al
// `setTimeout` que pudo haberse retrasado. Mientras haya navegación real,
// esto mantiene la sesión viva de forma continua; solo la inactividad
// genuina (sin estos eventos, con la pestaña visible o no) deja que la
// sesión expire con normalidad.
const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'touchstart', 'scroll'] as const
let activityListenersAttached = false

function renewIfDueOrExpired(): void {
  if (!tokenSet) return
  if (Date.now() >= tokenSet.expiresAt - 30_000) void silentRenew()
}

function attachActivityListeners(): void {
  if (activityListenersAttached) return
  activityListenersAttached = true
  for (const event of ACTIVITY_EVENTS) {
    window.addEventListener(event, renewIfDueOrExpired, { passive: true })
  }
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') renewIfDueOrExpired()
  })
}

// prompt=none in a hidden iframe: renews the token without ever showing the
// login UI or touching localStorage — the refresh token stays server-side in
// the Keycloak SSO session cookie.
function silentAuthorize(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const iframe = document.createElement('iframe')
    iframe.style.display = 'none'
    const cleanup = () => iframe.remove()
    const timeoutId = window.setTimeout(() => {
      cleanup()
      reject(new Error('Silent renew timed out'))
    }, 10_000)

    iframe.onload = () => {
      try {
        const href = iframe.contentWindow?.location.href
        const code = href ? new URL(href).searchParams.get('code') : null
        window.clearTimeout(timeoutId)
        cleanup()
        if (code) resolve(code)
        else reject(new Error('Silent renew: no authorization code returned'))
      } catch (error) {
        window.clearTimeout(timeoutId)
        cleanup()
        reject(error)
      }
    }

    iframe.src = url
    document.body.appendChild(iframe)
  })
}

async function silentRenew(): Promise<void> {
  try {
    const verifier = generateCodeVerifier()
    const challenge = await generateCodeChallenge(verifier)
    const code = await silentAuthorize(
      authorizeUrl({
        client_id: authConfig.clientId,
        response_type: 'code',
        redirect_uri: authConfig.redirectUri,
        scope: authConfig.scope,
        code_challenge: challenge,
        code_challenge_method: 'S256',
        prompt: 'none',
      }),
    )
    tokenSet = await exchangeCode(code, verifier)
    scheduleSilentRenew()
    notify()
  } catch {
    // Silent renew failing (e.g. SSO session expired) means the user is
    // logged out — never surface this as an error, just drop the token.
    tokenSet = null
    notify()
  }
}

export function logout(): void {
  tokenSet = null
  if (renewTimer) window.clearTimeout(renewTimer)
  notify()
  window.location.assign(
    `${authConfig.issuer}/protocol/openid-connect/logout?${new URLSearchParams({
      client_id: authConfig.clientId,
      post_logout_redirect_uri: window.location.origin,
    })}`,
  )
}
