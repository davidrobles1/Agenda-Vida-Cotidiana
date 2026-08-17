import { useEffect, useState } from 'react'
import { requestWebPushToken } from '../../core/notifications/firebase'
import { listDevices, registerDevice, type DevicePushToken } from './api'
import { AppShell } from '../../core/ui/layout/AppShell'
import { IconBell } from '../../core/ui/icons'
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
    <AppShell title="Notificaciones" subtitle="Dispositivos registrados para recibir avisos.">
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
              <span className={styles.icon}>
                <IconBell width={16} height={16} />
              </span>
              <span className={styles.deviceText}>
                {device.platform} — registered {device.createdAt}
              </span>
              <span className="badge badge-success">Active</span>
            </li>
          ))}
        </ul>
      )}
    </AppShell>
  )
}
