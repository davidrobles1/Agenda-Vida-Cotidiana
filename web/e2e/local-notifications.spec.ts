import { test, expect } from '@playwright/test'
import { loginAsTestuserAndGoToReminders } from './helpers'

/**
 * WEB-007 real verification: creates a reminder with a due date a few
 * seconds in the future, waits past it, and confirms `new Notification(...)`
 * was actually invoked with this reminder's data — not just that
 * scheduleLocalReminder() was called, and not mocked.
 *
 * `new Notification(...)` opens a real OS-level notification once granted;
 * Playwright doesn't expose the OS notification center directly, so the
 * standard, still-real way to assert on it is to wrap the constructor via
 * page.addInitScript() (runs before any app code on every navigation) and
 * record real invocations — this observes the actual browser Notification
 * API being called by the actual app code, not a mock swapped in for it.
 */
test('a reminder with a near-future due date fires a real browser Notification', async ({ page, context }) => {
  test.setTimeout(180_000)
  await context.grantPermissions(['notifications'], { origin: 'http://localhost:5173' })

  await page.addInitScript(() => {
    ;(window as unknown as { __notificationCalls: Array<{ title: string; body?: string }> }).__notificationCalls = []
    const OriginalNotification = window.Notification
    class SpyNotification extends OriginalNotification {
      constructor(title: string, options?: NotificationOptions) {
        super(title, options)
        ;(window as unknown as { __notificationCalls: Array<{ title: string; body?: string }> }).__notificationCalls.push({
          title,
          body: options?.body,
        })
      }
    }
    // @ts-expect-error - overriding the global constructor for real, observable invocation capture
    window.Notification = SpyNotification
  })

  const reminderTitle = `WEB-007 local notification test ${Math.random().toString(36).slice(2, 8)}`

  await loginAsTestuserAndGoToReminders(page)

  // datetime-local has minute precision only (no seconds field) — the value
  // gets truncated to its minute. A 6s buffer looked safe on paper but was
  // eaten by that truncation for real: if "now" is already a few seconds
  // into the current minute, truncating down could put the picked minute at
  // or before the actual submit time, and scheduleLocalReminder() correctly
  // no-ops on any non-positive delay (found for real — the reminder was
  // created fine with the right due date shown, just never scheduled).
  // +130s truncated down leaves at least ~70s of real margin.
  const dueAt = new Date(Date.now() + 130_000)
  const localValue = new Date(dueAt.getTime() - dueAt.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)

  await page.getByPlaceholder('New reminder').fill(reminderTitle)
  await page.locator('#due-at-input').fill(localValue)
  await page.getByRole('button', { name: 'Add' }).click()

  const row = page.locator('li', { hasText: reminderTitle })
  await expect(row).toBeVisible({ timeout: 10_000 })
  await expect(row.getByText('Due:', { exact: false })).toBeVisible()

  await expect
    .poll(
      async () =>
        page.evaluate((title) => {
          const calls = (window as unknown as { __notificationCalls: Array<{ title: string; body?: string }> })
            .__notificationCalls
          return calls.some((c) => c.title === 'Reminder due' && c.body === title)
        }, reminderTitle),
      { timeout: 150_000, intervals: [2_000], message: 'expected a real Notification("Reminder due", {body: reminderTitle}) call' },
    )
    .toBeTruthy()
})
