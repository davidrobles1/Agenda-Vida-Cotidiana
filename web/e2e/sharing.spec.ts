import { test, expect } from '@playwright/test'

/**
 * WEB-004 real verification: owner (testuser) logs in through the real
 * Keycloak login page, creates a reminder, and invites "userb" by username
 * through the actual backend. The B side (userb accepting) is verified
 * directly against the real backend per the task's own allowance ("Postman
 * para el lado B") rather than duplicating a second full browser login here.
 *
 * Requires: "userb" already provisioned in the backend's local USER table,
 * and VITE_OIDC_ISSUER/VITE_API_BASE_URL pointed at whichever host the
 * currently-running backend's OIDC_ISSUER is pinned to (see CIERRE notes —
 * both were pinned to the Mac's LAN IP for the concurrent iPad test).
 */
test('owner invites collaborator by username', async ({ page }) => {
  const reminderTitle = `Web sharing test ${Math.random().toString(36).slice(2, 10)}`

  await page.goto('/')
  await page.getByRole('button', { name: 'Log in' }).click()

  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()

  await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })
  await page.getByPlaceholder('New reminder').fill(reminderTitle)
  await page.getByRole('button', { name: 'Add' }).click()

  const row = page.locator('li', { hasText: reminderTitle })
  await expect(row).toBeVisible({ timeout: 10_000 })

  // Share button only renders once GET /me resolves and matches ownerUserId.
  await expect(row.getByRole('button', { name: 'Share' })).toBeVisible({ timeout: 15_000 })
  await row.getByRole('button', { name: 'Share' }).click()

  const shareDialog = row.locator('[data-testid="share-dialog"]')
  await expect(shareDialog).toBeVisible()
  await shareDialog.locator('input[placeholder="Email or username"]').fill('userb')
  await shareDialog.getByRole('button', { name: 'Invite' }).click()

  await expect(shareDialog.getByText('userb@example.com — PENDING')).toBeVisible({ timeout: 15_000 })
})
