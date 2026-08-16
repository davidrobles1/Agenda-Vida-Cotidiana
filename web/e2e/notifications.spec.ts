import { test, expect } from '@playwright/test'

/**
 * WEB-005 real verification: the Notifications page renders against the
 * real backend (a real GET /me/devices call, not mocked) and correctly
 * declares the Firebase-config-blocked state — no Firebase Web config
 * (apiKey/messagingSenderId/vapidKey) exists in this checkout, so there's no
 * real FCM token to register yet (see CIERRE notes).
 */
test('notifications page loads and declares blocked state', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Log in' }).click()

  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()

  await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Notifications' }).click()

  await expect(page.getByText("Push notifications require Firebase configuration")).toBeVisible()
  await expect(page.getByRole('button', { name: 'Enable notifications' })).toBeDisabled()
})
