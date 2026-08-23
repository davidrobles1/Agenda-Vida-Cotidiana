import { test, expect } from '@playwright/test'
import { loginAsTestuserAndGoToReminders } from './helpers'

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

  await loginAsTestuserAndGoToReminders(page)
  await page.getByPlaceholder('New reminder').fill(reminderTitle)
  await page.getByRole('button', { name: 'Add' }).click()

  const row = page.locator('li', { hasText: reminderTitle })
  await expect(row).toBeVisible({ timeout: 10_000 })

  // Share button only renders once GET /me resolves and matches ownerUserId.
  await expect(row.getByRole('button', { name: 'Share' })).toBeVisible({ timeout: 15_000 })
  await row.getByRole('button', { name: 'Share' }).click()

  // UX-011 Fase 2: the share panel is now a real React Aria Popover,
  // portalled to <body> (correct behavior — floats above the list, not
  // clipped by any ancestor's overflow) — so it's no longer inside `row`'s
  // DOM subtree. Only one can be open at a time, so a page-level locator
  // is unambiguous here.
  const shareDialog = page.locator('[data-testid="share-dialog"]')
  await expect(shareDialog).toBeVisible()
  await shareDialog.locator('input[placeholder="Email or username"]').fill('userb')
  await shareDialog.getByRole('button', { name: 'Invite' }).click()

  await expect(shareDialog.getByText('userb@example.com — PENDING')).toBeVisible({ timeout: 15_000 })
})

/**
 * UX-011 Fase 2 real verification: the 3 concrete gaps the old inline
 * expand-in-place panel had — no focus trap, no Escape-to-close, no focus
 * restoration to the trigger — all closed by moving to a real React Aria
 * DialogTrigger/Popover/Dialog. Escape-to-close also exercises the Motion+
 * React Aria exit-animation integration (`motion.create(Popover)`): the
 * popover must be fully gone from the DOM after the close animation
 * settles, not stuck half-closed.
 */
test('share popover: focus moves in on open, Escape closes it, focus returns to the trigger', async ({ page }) => {
  const reminderTitle = `Share a11y test ${Math.random().toString(36).slice(2, 8)}`

  await loginAsTestuserAndGoToReminders(page)

  await page.getByPlaceholder('New reminder').fill(reminderTitle)
  await page.getByRole('button', { name: 'Add' }).click()
  const row = page.locator('li', { hasText: reminderTitle })
  await expect(row).toBeVisible({ timeout: 10_000 })

  const shareButton = row.getByRole('button', { name: 'Share' })
  await expect(shareButton).toBeVisible({ timeout: 15_000 })
  await expect(shareButton).toHaveAttribute('aria-expanded', 'false')

  await shareButton.click()
  const dialog = page.locator('[data-testid="share-dialog"]')
  await expect(dialog).toBeVisible()
  await expect(shareButton).toHaveAttribute('aria-expanded', 'true')

  const focusInsideDialog = await dialog.evaluate((el) => el.contains(document.activeElement))
  expect(focusInsideDialog).toBe(true)

  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
  await expect(shareButton).toHaveAttribute('aria-expanded', 'false')
  await expect(shareButton).toBeFocused()
})
