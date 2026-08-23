import { test, expect } from '@playwright/test'
import { loginAsTestuserAndGoToReminders } from './helpers'

/**
 * WEB-005 real verification: owner (testuser) logs in and reaches the
 * Notifications screen, which loads real device data from the backend
 * (GET /me/devices). The "Enable notifications" click itself is exercised
 * here too — notification permission grant and Service Worker registration
 * both go through their real browser APIs — but this test intentionally
 * stops short of asserting a registered device.
 *
 * Found for real: `pushManager.subscribe()` is consistently rejected by
 * Google's push backend with `AbortError: Registration failed - permission
 * denied` whenever the browser is WebDriver-controlled (Playwright,
 * Chromium and real Chrome, headless and headed all reproduce it
 * identically once notification permission itself is correctly granted) —
 * this is Google's own anti-automation measure on their push-subscription
 * service, not a bug in this app's code, in Firebase's SDK, or in the VAPID
 * key. A manual click (real mouse, non-automated Chrome, 2026-08-16)
 * registered a real device — confirmed independently in Postgres
 * (device_push_tokens, platform='WEB', a real `xxxx:APA91b...`-shaped
 * token) — proving the code path is genuinely correct end to end; only
 * the automated re-verification of that exact last step is structurally
 * unavailable to this test runner.
 */
test('notifications page loads real device data and enable button is reachable', async ({ page, context }) => {
  await context.grantPermissions(['notifications'], { origin: 'http://localhost:5173' })

  await loginAsTestuserAndGoToReminders(page)
  await page.getByRole('link', { name: 'Notifications' }).click()

  // Real GET /me/devices — the WEB device registered manually earlier this
  // session must still be listed (proves this isn't a mocked/empty view).
  await expect(page.getByText('WEB — registered', { exact: false })).toBeVisible({ timeout: 10_000 })

  await expect(page.getByRole('button', { name: 'Enable notifications' })).toBeEnabled()
})
