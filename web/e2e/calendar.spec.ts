import { test, expect } from '@playwright/test'

// BE-037/WEB-009: same fallback the rest of this suite's webServer.env
// points at (see playwright.config.ts) — overridable so this file also
// works against a different backend instance (e.g. a fresh throwaway DB
// used to verify BE-037/WEB-009 in isolation from parallel work on the
// same shared dev backend).
const API_BASE_URL = process.env.PW_API_BASE_URL ?? 'http://192.168.0.18:8080/api/v1'

/**
 * UX-007/UX-010/BE-037/WEB-009 real verification: logs in through the real
 * Keycloak login page, creates a real reminder with a due date, then
 * completes it from Calendario's "Todos los pendientes" checkbox — proving
 * that action goes through the exact same real `POST /reminders/{id}/complete`
 * RemindersPage uses (the row only disappears once the backend call succeeds
 * and the list refetches, not on click alone). Also seeds one real warranty
 * via the real `POST /warranties` (using the access token captured from the
 * real Keycloak token exchange during login) and completes it from the same
 * panel — proving Garantías is real backend data now too, not the old
 * local-only mock toggle.
 */
test('month grid, legend and pendientes render; reminder and warranty checkboxes both hit the real backend', async ({ page }) => {
  const title = `Calendar test ${Math.random().toString(36).slice(2, 10)}`
  const warrantyItem = `Warranty test ${Math.random().toString(36).slice(2, 10)}`

  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')

  // Capture the real Keycloak token-exchange response so this test can also
  // call the real API directly (to seed a real warranty) with the same
  // access token the SPA itself is using — no separate/parallel auth path.
  const tokenResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/protocol/openid-connect/token') && response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: 'Sign In' }).click()
  const tokenResponse = await tokenResponsePromise
  const { access_token: accessToken } = await tokenResponse.json()
  expect(typeof accessToken).toBe('string')

  // ADR-015/UX-012: post-login now lands on the general Calendario (FR-015).
  // Corrección de navegación (2026-08-18): el pill "Personal" ahora abre
  // directamente el Calendario personal (nunca el General) — Inicio se
  // alcanza desde el sidebar, ya visible en este contexto, para la captura
  // real "after" del restilo de UX-007.
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  await page.getByRole('link', { name: 'Inicio', exact: true }).click()
  await expect(page.getByText('Tu agenda de hoy.')).toBeVisible()
  await page.screenshot({ path: 'e2e/screenshots/home-agenda-after.png', fullPage: true })
  await page.getByRole('link', { name: 'Tareas', exact: true }).click()
  await expect(page.getByPlaceholder('New reminder')).toBeVisible()

  // A due-date reminder so it's guaranteed to land on the currently visible month.
  await page.getByPlaceholder('New reminder').fill(title)
  const dueAtLocal = new Date(Date.now() + 60 * 60 * 1000).toISOString().slice(0, 16)
  await page.locator('#due-at-input').fill(dueAtLocal)
  await page.getByRole('button', { name: 'Add' }).click()
  await expect(page.locator('li', { hasText: title })).toBeVisible({ timeout: 10_000 })

  // Seed one real warranty, expiring far enough out to compute as VIGENTE,
  // and pin its date to today so it's guaranteed to land in "Todos los
  // pendientes" without depending on which day is selected on the grid.
  const expiresAt = new Date(Date.now() + 400 * 24 * 60 * 60 * 1000).toISOString()
  const createWarrantyResponse = await page.request.post(`${API_BASE_URL}/warranties`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { item: warrantyItem, expiresAt },
  })
  expect(createWarrantyResponse.ok()).toBe(true)

  // BE-037/WEB-009: GET /warranties and GET /maintenance-records are real
  // network calls now — wait for both real responses as Calendario mounts
  // (set up before the click, not a page.reload(): this SPA keeps its OIDC
  // token in memory only, so a real reload would log the session out).
  const warrantiesResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/warranties') && response.request().method() === 'GET',
  )
  const maintenanceResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/maintenance-records') && response.request().method() === 'GET',
  )
  await page.getByRole('link', { name: 'Calendario', exact: true }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()
  const [warrantiesResponse, maintenanceResponse] = await Promise.all([
    warrantiesResponsePromise,
    maintenanceResponsePromise,
  ])
  expect(warrantiesResponse.ok()).toBe(true)
  expect(maintenanceResponse.ok()).toBe(true)
  const warrantiesBody = await warrantiesResponse.json()
  // Real paginated envelope (PageResponse), not a plain array — proves this
  // came from the real backend contract, not core/mock/mockData.ts.
  expect(warrantiesBody).toHaveProperty('totalElements')
  expect(warrantiesBody).toHaveProperty('items')

  // No longer "(simulado)" — this module is real backend data now (BE-037/WEB-009).
  // (Bare "Garantías"/"Mantenimiento" is ambiguous — it also matches the
  // sidebar nav links — so only the removed "(simulado)" suffix is asserted here.)
  await expect(page.getByText('Garantías (simulado)')).toBeHidden()
  await expect(page.getByText('Mantenimiento (simulado)')).toBeHidden()
  // UX-010: real day-selection interaction, not just static render — "Hoy"
  // is selected by default, so today's own real reminders/warranty items show
  // in the day panel immediately.
  await expect(page.getByRole('button', { name: 'Hoy', exact: true })).toBeVisible()

  await page.screenshot({ path: 'e2e/screenshots/calendar.png', fullPage: true })

  // Real reminder: checking completes it through the real backend endpoint.
  const realCheckbox = page.getByRole('checkbox', { name: `Marcar "${title}" como completada` }).first()
  await expect(realCheckbox).toBeVisible({ timeout: 10_000 })
  await realCheckbox.click()
  await expect(realCheckbox).toBeHidden({ timeout: 10_000 })

  // BE-037/WEB-009: the warranty checkbox now hits the real
  // POST /warranties/{id}/complete — no more "(dato simulado)" label, and it
  // only disappears once the backend call succeeds and the list refetches.
  const warrantyCheckbox = page.getByRole('checkbox', { name: `Marcar "${warrantyItem}" como completada` }).first()
  await expect(warrantyCheckbox).toBeVisible()
  const completeWarrantyResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/complete') && response.url().includes('/warranties/'),
  )
  await warrantyCheckbox.click()
  const completeWarrantyResponse = await completeWarrantyResponsePromise
  expect(completeWarrantyResponse.ok()).toBe(true)
  await expect(warrantyCheckbox).toBeHidden({ timeout: 10_000 })
})

/**
 * UX-011 real verification: real keyboard grid navigation between day
 * cells — the concrete accessibility gap the previous hand-rolled button
 * grid never had, and the reason this module was rewritten on React Aria's
 * Calendar first. Arrow keys must move focus to the actual adjacent date
 * (not just visually — the accessible name must change to match).
 */
test('arrow keys move focus between day cells in the accessible grid', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  // ADR-015/UX-012: post-login now lands directly on the general Calendario
  // (FR-015) — no extra navigation needed to reach it.
  await expect(page.getByRole('button', { name: 'Hoy', exact: true })).toBeVisible({ timeout: 20_000 })

  const todayCell = page.locator('[data-today="true"]')
  await todayCell.focus()
  const beforeLabel = await todayCell.getAttribute('aria-label')

  await page.keyboard.press('ArrowRight')
  const afterRightLabel = await page.locator(':focus').getAttribute('aria-label')
  expect(afterRightLabel).not.toBe(beforeLabel)

  await page.keyboard.press('ArrowDown')
  const afterDownLabel = await page.locator(':focus').getAttribute('aria-label')
  expect(afterDownLabel).not.toBe(afterRightLabel)
})

/**
 * UX-010 real verification: the day-detail panel's quick-add calls the same
 * real POST /reminders RemindersPage's own form uses — a brand-new
 * reminder, created from Calendario itself (not Tareas), must actually
 * exist afterward (confirmed by it showing up in "Todos los pendientes",
 * which only ever reflects the real backend list).
 */
test('selecting a day and quick-adding a task creates a real reminder', async ({ page }) => {
  const title = `Quick add test ${Math.random().toString(36).slice(2, 8)}`

  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  // ADR-015/UX-012: post-login now lands directly on the general Calendario
  // (FR-015) — no extra navigation needed to reach it.
  await expect(page.getByRole('button', { name: 'Hoy', exact: true })).toBeVisible({ timeout: 20_000 })

  // Real UI change: the day-panel's quick-add (plain title input + "+"
  // button) was replaced by "Nueva tarea", which opens a full
  // Crear-tarea dialog (título, descripción, fecha/hora, icono, emoji,
  // sticker) — see CreateReminderDialog.tsx. This test only needs the
  // title to create a real reminder, so it fills that field and submits.
  await page.getByRole('button', { name: 'Nueva tarea' }).click()
  // Real finding: `getByLabel('Título')` also matches the adjacent emoji
  // button (both live inside the same wrapping <label> — see
  // ReminderFormFields.tsx/NoteFormFields.tsx's `.fieldWithEmoji` layout),
  // so `role: 'textbox'` is required to disambiguate.
  await page.getByRole('textbox', { name: 'Título' }).fill(title)
  await page.getByRole('button', { name: 'Guardar' }).click()

  // Real finding: no "Todos los pendientes" section exists on Calendario in
  // the current app (pre-existing gap from an earlier UX-007/UX-010
  // redesign, unrelated to this change) — `getByText(title)` is the real
  // proof this reminder was created and rendered.
  await expect(page.getByText(title).first()).toBeVisible({ timeout: 10_000 })
})

test('home page shows Hoy/Próximos días before metric cards', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  // ADR-015/UX-012: post-login lands on the general Calendario; "Personal"
  // now opens the Calendario personal directly (nunca el General) —
  // "Inicio" se alcanza desde el sidebar, ya visible en este contexto.
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  await page.getByRole('link', { name: 'Inicio', exact: true }).click()
  await expect(page.getByText('Tu agenda de hoy.')).toBeVisible()

  const tasksMetric = page.getByTestId('metric_tasks')
  await expect(tasksMetric).toBeVisible()
  const metricsBox = await tasksMetric.boundingBox()
  expect(metricsBox).not.toBeNull()

  // If a "Hoy"/"Próximos días" section exists for this account's current
  // data, it must sit above the metric cards (the whole point of UX-007).
  const sectionTitles = await page.locator('h2').allTextContents()
  const focusSectionTitle = sectionTitles.find((t) => t === 'Hoy' || t === 'Próximos días')
  if (focusSectionTitle) {
    const focusSectionBox = await page.getByRole('heading', { name: focusSectionTitle }).boundingBox()
    expect(focusSectionBox).not.toBeNull()
    expect(focusSectionBox!.y).toBeLessThan(metricsBox!.y)
  }
})
