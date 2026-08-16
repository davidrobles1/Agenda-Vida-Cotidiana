import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { requestWebPushToken } from '../../core/notifications/firebase'
import { listDevices, registerDevice, type DevicePushToken } from './api'

export function NotificationsPage() {
  const [devices, setDevices] = useState<DevicePushToken[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [registering, setRegistering] = useState(false)

  function refresh() {
    listDevices()
      .then(setDevices)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load devices'))
      .finally(() => setLoading(false))
  }

  useEffect(refresh, [])

  async function handleEnable() {
    setRegistering(true)
    setError(null)
    try {
      const token = await requestWebPushToken()
      if (!token) {
        setError('Notification permission was not granted, or Web Push is not supported in this browser.')
        return
      }
      await registerDevice(token)
      refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to enable notifications')
    } finally {
      setRegistering(false)
    }
  }

  return (
    <div>
      <header>
        <h1>Notifications</h1>
        <Link to="/reminders">Back</Link>
      </header>

      {error && <p role="alert">{error}</p>}

      <button type="button" onClick={handleEnable} disabled={registering}>
        {registering ? 'Enabling…' : 'Enable notifications'}
      </button>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <ul>
          {devices.map((device) => (
            <li key={device.id}>
              {device.platform} — registered {device.createdAt}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
