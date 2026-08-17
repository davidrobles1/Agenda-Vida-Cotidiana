import { test, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

/**
 * ACC-001 (05-v2-plan.md §3, 02-ux-ui/accessibility.md): real automated
 * accessibility scan (axe-core, WCAG 2.1 A/AA ruleset) against the actual
 * running screens — real login, real Keycloak, real backend data — not a
 * claim that it "should" pass. Each screen is scanned after real navigation,
 * not a mocked/static render.
 */
test.describe('axe-core accessibility scan', () => {
  test('login page has no real WCAG 2.1 A/AA violations', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('button', { name: 'Log in' })).toBeVisible()

    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([])
  })

  test('authenticated screens have no real WCAG 2.1 A/AA violations', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Log in' }).click()
    await page.waitForURL(/realms\/vida-cotidiana/)
    await page.getByLabel('Username or email').fill('testuser')
    await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })

    const screens: Array<[string, () => Promise<void>]> = [
      ['Tareas', async () => {}],
      ['Inicio', async () => {
        await page.getByRole('link', { name: 'Inicio', exact: true }).click()
        await page.waitForTimeout(1000)
      }],
      ['Compartidos', async () => {
        await page.getByRole('link', { name: 'Compartidos', exact: true }).click()
        await page.waitForTimeout(500)
      }],
      ['Notificaciones', async () => {
        await page.getByRole('link', { name: 'Notifications' }).click()
        await page.waitForTimeout(500)
      }],
      ['Documentos (mock module)', async () => {
        await page.getByRole('link', { name: 'Documentos', exact: true }).click()
        await page.waitForTimeout(500)
      }],
    ]

    const allViolations: Record<string, unknown[]> = {}
    for (const [name, navigate] of screens) {
      await navigate()
      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
      if (results.violations.length > 0) {
        allViolations[name] = results.violations
      }
    }

    expect(allViolations, JSON.stringify(allViolations, null, 2)).toEqual({})
  })
})
