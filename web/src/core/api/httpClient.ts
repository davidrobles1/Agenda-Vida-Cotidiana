import { getAccessToken } from '../auth/authClient'
import { apiBaseUrl } from '../auth/config'

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = getAccessToken()
  const headers = new Headers(init.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  return fetch(`${apiBaseUrl}${path}`, { ...init, headers })
}
