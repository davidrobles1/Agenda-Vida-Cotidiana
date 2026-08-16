import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listDevices, type DevicePushToken } from './api'

// WEB-005 gated on Firebase config (CIERRE): no Firebase Web config (apiKey/
// messagingSenderId/vapidKey) exists in this checkout, so the `firebase`
// SDK isn't installed and no real FCM token can be obtained. registerDevice()
// in ./api.ts is real and callable the moment a real token is available.
const BLOCKED_ON_FIREBASE_CONFIG = true

export function NotificationsPage() {
  const [devices, setDevices] = useState<DevicePushToken[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listDevices()
      .then(setDevices)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load devices'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div>
      <header>
        <h1>Notifications</h1>
        <Link to="/reminders">Back</Link>
      </header>

      {error && <p role="alert">{error}</p>}

      {BLOCKED_ON_FIREBASE_CONFIG ? (
        <>
          <p>Push notifications require Firebase configuration, which isn't set up in this build yet.</p>
          <button type="button" disabled>
            Enable notifications
          </button>
        </>
      ) : (
        <button type="button">Enable notifications</button>
      )}

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
