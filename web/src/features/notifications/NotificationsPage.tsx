import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { requestWebPushToken } from '../../core/notifications/firebase'
import { listDevices, registerDevice, type DevicePushToken } from './api'
import styles from './NotificationsPage.module.css'

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
      <header className={styles.header}>
        <h1 className={styles.title}>Notifications</h1>
        <Link to="/reminders">Back</Link>
      </header>

      {error && <p role="alert">{error}</p>}

      <button type="button" className={styles.enableButton} onClick={handleEnable} disabled={registering}>
        {registering ? 'Enabling…' : 'Enable notifications'}
      </button>

      {loading ? (
        <p>Loading…</p>
      ) : devices.length === 0 ? (
        <div className="empty-state">
          <p>No devices registered yet.</p>
        </div>
      ) : (
        <ul className={styles.list}>
          {devices.map((device) => (
            <li key={device.id} className={styles.deviceRow}>
              {device.platform} — registered {device.createdAt}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
