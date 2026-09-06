import { test, expect } from '@playwright/test'

/**
 * BE-037/WEB-009 real verification: Garantías/Mantenimiento used to run
 * entirely on core/mock/mockData.ts (UX-006) — this confirms both pages now
 * render real backend data. Logs in through the real Keycloak login page
 * (same pattern as the rest of this suite), seeds one real warranty and one
 * real maintenance record via the real API (using the access token captured
 * from the real token exchange, same technique as calendar.spec.ts), then
 * confirms each item — with its real, server-computed status — renders on
 * its page after a real GET request.
 */
const API_BASE_URL = process.env.PW_API_BASE_URL ?? 'http://localhost:8080/api/v1'

test('Garantías and Mantenimiento pages render real backend data, not mock', async ({ page }) => {
  const warrantyItem = `WM test warranty ${Math.random().toString(36).slice(2, 10)}`
  const maintenanceItem = `WM test maintenance ${Math.random().toString(36).slice(2, 10)}`
  /** Lo que esta prueba crea en la base real, para retirarlo al terminar. */
  const sembrado: Array<[string, string]> = []

  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')

  const tokenResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/protocol/openid-connect/token') && response.request().method() === 'POST',
  )
  await page.locator('#kc-login').click()
  const tokenResponse = await tokenResponsePromise
  const { access_token: accessToken } = await tokenResponse.json()
  expect(typeof accessToken).toBe('string')

  // ADR-015/UX-012: post-login lands on the general Calendario. "Garantías"/
  // "Mantenimiento" (legacy UX-006 scaffolding modules) are no longer linked
  // from any navbar (only Inicio/Calendario/Tareas/Compartidos inside a
  // mode). Their routes are still real, reached via a real client-side
  // link, not page.goto() — the app keeps its OIDC token in memory only,
  // and page.goto() is a real browser navigation that would log the
  // session out. "Notifications" (always rendered, any mode) lands on a
  // legacy bare route, which renders the legacy sidebar (both included).
  // El destino tras iniciar sesión depende del modo del usuario
  // (ADR-015) y ya cambió; afirmar una pantalla concreta volvía roja
  // toda la suite por un cambio de producto legítimo. Basta con haber
  // vuelto a la aplicación autenticado.
  await page.waitForURL((url) => !url.href.includes('/realms/'), { timeout: 20_000 })
  await expect(page.locator('nav, header').first()).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Notifications' }).click()

  // Far enough out to compute as VIGENTE/AL_DIA (real server-side derivation,
  // see backend WarrantyResponse/MaintenanceRecordResponse — never a stored
  // temporal status).
  const farFuture = new Date(Date.now() + 400 * 24 * 60 * 60 * 1000).toISOString()

  const createWarrantyResponse = await page.request.post(`${API_BASE_URL}/warranties`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { item: warrantyItem, expiresAt: farFuture },
  })
  expect(createWarrantyResponse.ok()).toBe(true)
  const createdWarranty = await createWarrantyResponse.json()
  expect(createdWarranty.status).toBe('VIGENTE')

  const createMaintenanceResponse = await page.request.post(`${API_BASE_URL}/maintenance-records`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { item: maintenanceItem, nextDueAt: farFuture },
  })
  expect(createMaintenanceResponse.ok()).toBe(true)
  const createdMaintenance = await createMaintenanceResponse.json()
  expect(createdMaintenance.status).toBe('AL_DIA')

  sembrado.push(['warranties', createdWarranty.id], ['maintenance-records', createdMaintenance.id])

  try {
  // GET /warranties must be a real network call now (PageResponse envelope),
  // not a synchronous read from core/mock/mockData.ts. Matched against the
  // real API base, not just endsWith('/warranties') — the SPA's own
  // navigation request to the /warranties *page* also ends with that
  // suffix and would otherwise win the race.
  const warrantiesResponsePromise = page.waitForResponse(
    (response) => response.url() === `${API_BASE_URL}/warranties` && response.request().method() === 'GET',
  )
  await page.getByRole('link', { name: 'Garantías' }).click()
  const warrantiesResponse = await warrantiesResponsePromise
  expect(warrantiesResponse.ok()).toBe(true)
  const warrantiesBody = await warrantiesResponse.json()
  expect(warrantiesBody).toHaveProperty('totalElements')

  await expect(page.getByText(warrantyItem)).toBeVisible({ timeout: 10_000 })
  // Real server-computed status pill sits in the same ListItemRow as the
  // item title — [class*="_row_"] matches ListItemRow.module.css's scoped
  // "row" class regardless of its build hash.
  const warrantyRow = page.locator('[class*="_row_"]', { hasText: warrantyItem })
  await expect(warrantyRow.getByText('Vigente', { exact: true })).toBeVisible()

  const maintenanceResponsePromise = page.waitForResponse(
    (response) => response.url() === `${API_BASE_URL}/maintenance-records` && response.request().method() === 'GET',
  )
  await page.getByRole('link', { name: 'Mantenimiento' }).click()
  const maintenanceResponse = await maintenanceResponsePromise
  expect(maintenanceResponse.ok()).toBe(true)
  const maintenanceBody = await maintenanceResponse.json()
  expect(maintenanceBody).toHaveProperty('totalElements')

  await expect(page.getByText(maintenanceItem)).toBeVisible({ timeout: 10_000 })
  const maintenanceRow = page.locator('[class*="_row_"]', { hasText: maintenanceItem })
  await expect(maintenanceRow.getByText('Al día', { exact: true })).toBeVisible()
  } finally {
    // Esta prueba sembraba por API y no borraba nada: cada ejecución dejaba
    // una garantía y un mantenimiento "WM test …" en la base real del
    // usuario, mezclados con sus registros de verdad. Se acumularon ocho
    // antes de detectarlo. Va en `finally` a propósito: una prueba que
    // falla a media ejecución es justo la que más residuo deja.
    for (const [path, id] of sembrado) {
      await page.request
        .delete(`${API_BASE_URL}/${path}/${id}`, { headers: { Authorization: `Bearer ${accessToken}` } })
        .catch(() => {})
    }
  }
})
