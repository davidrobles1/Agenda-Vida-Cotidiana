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

export async function handleCallback(): Promise<void> {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const verifier = sessionStorage.getItem(CODE_VERIFIER_KEY)
  if (!code || !verifier) throw new Error('Missing authorization code or PKCE code_verifier')

  tokenSet = await exchangeCode(code, verifier)
  sessionStorage.removeItem(CODE_VERIFIER_KEY)
  scheduleSilentRenew()
  notify()
}

function scheduleSilentRenew(): void {
  if (renewTimer) window.clearTimeout(renewTimer)
  if (!tokenSet) return
  const msUntilRenew = Math.max(tokenSet.expiresAt - Date.now() - 30_000, 5_000)
  renewTimer = window.setTimeout(silentRenew, msUntilRenew)
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
