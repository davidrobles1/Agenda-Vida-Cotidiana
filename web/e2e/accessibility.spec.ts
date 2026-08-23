import { test, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { loginAsTestuserAndGoToReminders } from './helpers'

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
    await expect(page.getByRole('button', { name: 'Iniciar sesión' })).toBeVisible()

    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([])
  })

  test('authenticated screens have no real WCAG 2.1 A/AA violations', async ({ page }) => {
    await loginAsTestuserAndGoToReminders(page)

    // UX-012/ADR-015: "Documentos" (a legacy UX-006 scaffolding module) is no
    // longer reachable from the Personal navbar (only Inicio/Calendario/
    // Tareas/Compartidos, CLAUDE.md's "no agregues más") — its route is
    // still real, just no longer linked from here, so it's reached directly.
    const screens: Array<[string, () => Promise<void>]> = [
      ['Tareas (Personal)', async () => {}],
      ['Inicio (Personal)', async () => {
        await page.getByRole('link', { name: 'Inicio', exact: true }).click()
        await page.waitForTimeout(1000)
      }],
      ['Calendario personal', async () => {
        await page.getByRole('link', { name: 'Calendario personal', exact: true }).click()
        await page.waitForTimeout(500)
      }],
      ['Compartidos (Personal)', async () => {
        await page.getByRole('link', { name: 'Compartidos', exact: true }).click()
        await page.waitForTimeout(500)
      }],
      ['Notificaciones', async () => {
        await page.getByRole('link', { name: 'Notifications' }).click()
        await page.waitForTimeout(500)
      }],
      ['Calendario general', async () => {
        // Disambiguate from a legacy-route navbar's own "Calendario" entry
        // (both can be on screen at once outside a mode-scoped route, e.g.
        // right after "Notificaciones") — the top-level mode selector is
        // the one place "Calendario" always resolves to the general view.
        await page
          .getByRole('navigation', { name: 'Selector de modo' })
          .getByRole('link', { name: 'Calendario', exact: true })
          .click()
        await page.waitForTimeout(500)
      }],
      ['Ajustes', async () => {
        // WEB-*: the app keeps its OIDC token in memory only — page.goto()
        // is a real browser navigation and would log the session out. The
        // gear icon in the account area is a real client-side link, always
        // rendered regardless of mode.
        await page.getByRole('link', { name: 'Ajustes' }).click()
        await page.waitForTimeout(500)
      }],
      ['Documentos (mock module)', async () => {
        // Being on /settings (not a mode route) already renders the legacy
        // sidebar (Documentos included) — a real client-side link, not
        // page.goto(), for the same in-memory-token reason as above.
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
