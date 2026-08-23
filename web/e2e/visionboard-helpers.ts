import { expect, type Page } from '@playwright/test'

/**
 * FASE 22 finding: the shared `loginAsTestuserAndGoToReminders` (helpers.ts)
 * hops through Personal mode's "Tareas" link on its way to Reminders — that
 * link no longer exists in the current sidebar (verified via a real
 * Playwright run against the live dev stack: the nav only lists Inicio,
 * Calendario personal, Vision Board, Compartidos, Documentos, Inventario,
 * Garantías, Mantenimiento, Suscripciones, Familia). That's a real,
 * pre-existing gap in shared test infrastructure — not something this
 * Vision Board testing phase introduced or is scoped to fix (it affects
 * Reminders/navigation, not Vision Board), so `helpers.ts` itself is left
 * untouched here.
 *
 * Vision Board doesn't need that detour at all — it has its own direct
 * sidebar link ("Vision Board" → /personal/vision-board), reachable right
 * after login. This is the login path the 3 Vision Board specs use instead.
 */
export async function loginAndGoToVisionBoard(page: Page): Promise<void> {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()

  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()

  // Post-login lands on the general/top-level Calendario (FR-015), which
  // has no feature sidebar at all — only the mode selector (Calendario/
  // Personal/Laboral). "Vision Board" only appears in Personal mode's own
  // sidebar, so that mode switch has to happen first.
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  await page.getByRole('link', { name: 'Vision Board', exact: true }).click()
}
