import { initializeApp } from 'firebase/app'
import { getMessaging, getToken, isSupported } from 'firebase/messaging'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
}

const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY as string | undefined

/** WEB-005: real FCM Web Push token, or null if unsupported/denied/not configured. */
export async function requestWebPushToken(): Promise<string | null> {
  if (!(await isSupported())) return null
  if (!vapidKey) throw new Error('VITE_FIREBASE_VAPID_KEY is not set (Firebase Console -> Cloud Messaging -> Web Push certificates)')

  const permission = await Notification.requestPermission()
  if (permission !== 'granted') return null

  const app = initializeApp(firebaseConfig)
  const messaging = getMessaging(app)
  const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js')
  return getToken(messaging, { vapidKey, serviceWorkerRegistration: registration })
}
