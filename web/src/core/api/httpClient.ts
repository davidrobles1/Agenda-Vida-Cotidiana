import { getAccessToken } from '../auth/authClient'
import { apiBaseUrl } from '../auth/config'

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = getAccessToken()
  const headers = new Headers(init.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  // BLOQUE B: a FormData body (Vision Board image upload) must never get a
  // manual Content-Type — the browser sets its own `multipart/form-data;
  // boundary=...` only when the header is left unset; forcing `application/
  // json` here (the previous unconditional default) would send a body the
  // server can't parse as either.
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  return fetch(`${apiBaseUrl}${path}`, { ...init, headers })
}
