import { expect, type Page } from '@playwright/test'

/**
 * ADR-015/UX-012 real verification note: post-login now lands on the
 * general Calendario (FR-015), not Tareas — this replaces every spec's old
 * "log in, wait for the reminders placeholder" opening. `testuser` is a
 * pre-ADR-015 account, so `core/user/modes.ts`'s documented fallback treats
 * it as personalEnabled=true (mirroring the real backfill migration the
 * backend block is applying to existing rows) — the mode switcher offers
 * "Personal", and its navbar's "Tareas" link reaches the same
 * `/personal/reminders` screen these specs actually exercise.
 *
 * Kept in one place so the many specs written before ADR-015 (sharing,
 * notifications, local-notifications, error-tracking, accessibility) only
 * needed their login boilerplate swapped for a call to this, not a
 * navigation rewrite each.
 */
export async function loginAsTestuserAndGoToReminders(page: Page): Promise<void> {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()

  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()

  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  await page.getByRole('link', { name: 'Tareas', exact: true }).click()
  await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })
}
