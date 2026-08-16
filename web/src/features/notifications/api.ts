import { apiFetch } from '../../core/api/httpClient'

export interface DevicePushToken {
  id: string
  platform: 'ANDROID' | 'IOS' | 'WEB'
  createdAt: string
  lastSeenAt: string
}

export async function listDevices(): Promise<DevicePushToken[]> {
  const response = await apiFetch('/me/devices')
  if (!response.ok) throw new Error(`GET /me/devices failed: ${response.status}`)
  return response.json()
}

export async function registerDevice(token: string): Promise<DevicePushToken> {
  const response = await apiFetch('/me/devices', {
    method: 'POST',
    body: JSON.stringify({ platform: 'WEB', token }),
  })
  if (!response.ok) throw new Error(`POST /me/devices failed: ${response.status}`)
  return response.json()
}
