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
  await page.locator('#kc-login').click()

  // Post-login lands on the general/top-level Calendario (FR-015), which
  // has no feature sidebar at all — only the mode selector (Calendario/
  // Personal/Laboral). "Vision Board" only appears in Personal mode's own
  // sidebar, so that mode switch has to happen first.
  // El destino tras iniciar sesión depende del modo del usuario
  // (ADR-015) y ya cambió; afirmar una pantalla concreta volvía roja
  // toda la suite por un cambio de producto legítimo. Basta con haber
  // vuelto a la aplicación autenticado.
  await page.waitForURL((url) => !url.href.includes('/realms/'), { timeout: 20_000 })
  await expect(page.locator('nav, header').first()).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  await page.getByRole('link', { name: 'Vision Board', exact: true }).click()
}
