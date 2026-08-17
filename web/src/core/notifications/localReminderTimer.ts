/**
 * WEB-007 (ADR-007: "las notificaciones locales ... se mantienen resueltas
 * en el cliente"). Best-effort only — a real, documented platform limit, not
 * a defect: this only works while the tab is open (even backgrounded), since
 * there is no persistent OS-level timer available to a web page. If the tab
 * or browser is closed, the timer is gone and no notification fires — see
 * Documentacion/08c-web-architecture.md for the explicit limitation writeup.
 * No Service Worker periodicSync is used here on purpose: browser support is
 * inconsistent enough that promising it would be a false promise.
 *
 * Reuses the Notification permission already requested by WEB-005's "Enable
 * notifications" flow (core/notifications/firebase.ts) — this module never
 * prompts on its own; if permission isn't granted yet, it just no-ops.
 */
const timers = new Map<string, ReturnType<typeof setTimeout>>()

export function scheduleLocalReminder(reminderId: string, title: string, dueAtMillis: number): void {
  cancelLocalReminder(reminderId)

  const delay = dueAtMillis - Date.now()
  if (delay <= 0) return
  if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return

  const timer = setTimeout(() => {
    new Notification('Reminder due', { body: title, tag: reminderId })
    timers.delete(reminderId)
  }, delay)
  timers.set(reminderId, timer)
}

export function cancelLocalReminder(reminderId: string): void {
  const timer = timers.get(reminderId)
  if (timer !== undefined) {
    clearTimeout(timer)
    timers.delete(reminderId)
  }
}
