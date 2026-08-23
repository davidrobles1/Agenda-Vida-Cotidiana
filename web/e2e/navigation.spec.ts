import { test, expect } from '@playwright/test'

/**
 * UX-011 real verification: the sidebar hamburger toggle (web-only,
 * AppShell.tsx) — plain state + CSS transition (no gap here hard enough to
 * justify Motion or React Aria; see AppShell.tsx's own comment on why).
 * Confirms it's a real toggle, not just a visual one: `inert` genuinely
 * removes the collapsed sidebar from the accessibility tree/tab order, and
 * the collapse direction correctly differs by breakpoint (width on the
 * desktop column layout, height on the ≤900px horizontal layout).
 */
test('sidebar hamburger toggle: collapses/expands, real inert when closed, correct axis per breakpoint', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  // ADR-015/UX-012: post-login lands on the general Calendario, cuyo
  // sidebar está oculto/bloqueado por completo (Corrección de navegación,
  // 2026-08-18) — "Personal" (modo grandfathered de testuser) abre su
  // propio Calendario y con él, el sidebar real que este test necesita.
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Personal', exact: true }).click()

  const sidebar = page.locator('#app-sidebar')
  const menuButton = page.getByRole('button', { name: /navegación/ })
  await expect(sidebar).toBeVisible()
  await expect(menuButton).toHaveAttribute('aria-expanded', 'true')
  await expect(sidebar.getByRole('link', { name: 'Tareas' })).toBeVisible()

  const openWidth = await sidebar.evaluate((el) => el.getBoundingClientRect().width)
  expect(openWidth).toBeGreaterThan(100)

  await menuButton.click()
  await expect(menuButton).toHaveAttribute('aria-expanded', 'false')
  await expect.poll(() => sidebar.evaluate((el) => el.getBoundingClientRect().width)).toBeLessThan(5)
  expect(await sidebar.evaluate((el) => el.inert)).toBe(true)

  await menuButton.click()
  await expect(menuButton).toHaveAttribute('aria-expanded', 'true')
  await expect.poll(() => sidebar.evaluate((el) => el.getBoundingClientRect().width)).toBeGreaterThan(100)
  expect(await sidebar.evaluate((el) => el.inert)).toBe(false)

  // ≤900px: the sidebar becomes a horizontal bar — toggle collapses height.
  await page.setViewportSize({ width: 390, height: 844 })
  const mobileOpenHeight = await sidebar.evaluate((el) => el.getBoundingClientRect().height)
  expect(mobileOpenHeight).toBeGreaterThan(30)

  await menuButton.click()
  await expect.poll(() => sidebar.evaluate((el) => el.getBoundingClientRect().height)).toBeLessThan(5)
})

/**
 * Corrección de navegación (2026-08-18, pedido explícito del usuario):
 * verifica las 3 reglas reales de separación entre contextos — Calendario
 * General oculta el sidebar y bloquea cualquier mecanismo para reabrirlo
 * (el botón hamburguesa ni siquiera existe en el DOM); Personal lo muestra
 * de nuevo, con **todas** las opciones que antes vivían en el sidebar
 * plano (nada se perdió al reorganizar); y Ajustes/Logout siguen
 * alcanzables desde General pese a que el sidebar está oculto (no son
 * parte del "TabNav lateral").
 */
test('Calendario General oculta y bloquea el sidebar; Personal lo restaura con todas sus opciones', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })

  // Calendario General: sin sidebar, sin botón para reabrirlo.
  await expect(page.locator('#app-sidebar')).toHaveCount(0)
  await expect(page.getByRole('button', { name: /navegación/ })).toHaveCount(0)
  // Pero Ajustes/Logout siguen alcanzables — no son parte del TabNav.
  await expect(page.getByRole('link', { name: 'Ajustes' })).toBeVisible()

  // Personal: el sidebar reaparece, sin ningún bloqueo, con las 10
  // opciones reales (4 mode-scoped + los 6 módulos de maqueta de UX-006).
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  const sidebar = page.locator('#app-sidebar')
  await expect(sidebar).toBeVisible()
  await expect(page.getByRole('button', { name: /navegación/ })).toBeVisible()
  for (const label of [
    'Inicio',
    'Calendario personal',
    'Tareas',
    'Compartidos',
    'Documentos',
    'Inventario',
    'Garantías',
    'Mantenimiento',
    'Suscripciones',
    'Familia',
  ]) {
    await expect(sidebar.getByRole('link', { name: label, exact: true })).toBeVisible()
  }

  // De vuelta a General: el sidebar se oculta otra vez, sin arrastrar
  // ningún estado (ni abierto ni cerrado) de la visita anterior a Personal.
  await page
    .getByRole('navigation', { name: 'Selector de modo' })
    .getByRole('link', { name: 'Calendario', exact: true })
    .click()
  await expect(page.locator('#app-sidebar')).toHaveCount(0)
  await expect(page.getByRole('button', { name: /navegación/ })).toHaveCount(0)
})
