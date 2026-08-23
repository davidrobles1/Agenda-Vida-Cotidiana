import { test, expect } from '@playwright/test'

/**
 * UX-011 Fase 3 real verification: the category filter was N independent
 * `aria-pressed` buttons in a bare <div> — no grouped role, no arrow-key
 * navigation between them. Now a real React Aria `ToggleButtonGroup`
 * (selectionMode="single", disallowEmptySelection), which renders the more
 * semantically precise `role="radiogroup"`/`role="radio"` — confirmed real,
 * not assumed, while building this. Verifies both that filtering still
 * works (same real mock data, `core/mock/mockData.ts`, zero behavior
 * change) and that arrow-key navigation between chips — the concrete gap —
 * now exists.
 */
test('inventory category filter: real radiogroup semantics, filtering, and arrow-key navigation', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  // ADR-015/UX-012: post-login lands on the general Calendario; "Inventario"
  // (a legacy UX-006 scaffolding module) is no longer linked from any navbar
  // (only Inicio/Calendario/Tareas/Compartidos inside a mode — CLAUDE.md's
  // "no agregues más"). Its route is still real, but reached via a real
  // client-side link, not page.goto() — the app keeps its OIDC token in
  // memory only, and page.goto() is a real browser navigation that would
  // log the session out. "Notifications" (always rendered, any mode) lands
  // on a legacy bare route, which renders the legacy sidebar (Inventario
  // included).
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Notifications' }).click()
  await page.getByRole('link', { name: 'Inventario' }).click()
  const group = page.getByRole('radiogroup', { name: 'Filtrar por categoría' })
  await expect(group).toBeVisible()

  const allChip = group.getByRole('radio', { name: 'Todos' })
  await expect(allChip).toBeChecked()

  const electronicsChip = group.getByRole('radio', { name: 'Electrónicos' })
  await electronicsChip.click()
  await expect(electronicsChip).toBeChecked()
  await expect(allChip).not.toBeChecked()
  await expect(page.getByText('Laptop Dell XPS 13')).toBeVisible()
  await expect(page.getByText('Sofá Sala')).toBeHidden()

  // Real arrow-key navigation between chips (the concrete gap this closed).
  await electronicsChip.focus()
  await page.keyboard.press('ArrowRight')
  const focused = await page.evaluate(() => document.activeElement?.textContent)
  expect(focused).toBe('Hogar')

  // disallowEmptySelection: exactly one chip is always checked.
  const checkedCount = await group.getByRole('radio', { checked: true }).count()
  expect(checkedCount).toBe(1)
})
