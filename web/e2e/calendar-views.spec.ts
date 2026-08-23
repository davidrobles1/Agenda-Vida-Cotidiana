import { test, expect } from '@playwright/test'

/**
 * UX-013 real verification: Semana/Día views added on top of the existing
 * Mes view (`CalendarPage.tsx`) via a real `ToggleButtonGroup` range
 * selector (same single-select pattern UX-011 Fase 3 already established
 * for `FilterChip`/Inventory — `role="radiogroup"`/`role="radio"`, not a
 * bespoke mechanism). Both new views reuse `useCalendarData()` verbatim —
 * no new data fetching, only presentation — and the exact same real
 * `POST /reminders`/`POST /reminders/{id}/complete` endpoints the Mes view
 * and RemindersPage already use.
 */

async function login(page: import('@playwright/test').Page) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  // ADR-015/UX-012: post-login now lands directly on the general Calendario
  // (FR-015) — the callers below no longer need to navigate there.
  await expect(page.getByText('Vista mensual')).toBeVisible({ timeout: 20_000 })
}

test('range selector switches between Mes/Semana/Día, each with its own real content', async ({ page }) => {
  await login(page)

  await page.getByRole('link', { name: 'Calendario' }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()

  await page.getByRole('radio', { name: 'Semana' }).click()
  await expect(page.getByText('Vista semanal')).toBeVisible()
  await expect(page.getByText('Vista mensual')).toBeHidden()
  await page.screenshot({ path: '../Documentacion/02-ux-ui/screenshots/web-calendar-ux013-week-after.png', fullPage: true })

  await page.getByRole('radio', { name: 'Día' }).click()
  await expect(page.getByText('Vista diaria')).toBeVisible()
  await expect(page.getByText('Vista semanal')).toBeHidden()
  await page.screenshot({ path: '../Documentacion/02-ux-ui/screenshots/web-calendar-ux013-day-after.png', fullPage: true })

  await page.getByRole('radio', { name: 'Mes' }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()
  await expect(page.getByText('Vista diaria')).toBeHidden()
})

test('creates a real reminder from Semana view and completes it', async ({ page }) => {
  const title = `Week view test ${Math.random().toString(36).slice(2, 10)}`

  await login(page)
  await page.getByRole('link', { name: 'Calendario' }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()

  await page.getByRole('radio', { name: 'Semana' }).click()
  await expect(page.getByText('Vista semanal')).toBeVisible()

  // Semana's 7 columns render with real day-of-week headers, today's
  // column selected by default (same "Hoy" default as Mes/Día).
  await expect(page.getByRole('button', { name: 'Semana anterior' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Semana siguiente' })).toBeVisible()

  // "Nueva tarea" opens a full Crear-tarea dialog (título, descripción,
  // fecha/hora, icono, emoji, sticker) — see CreateReminderDialog.tsx.
  await page.getByRole('button', { name: 'Nueva tarea' }).click()
  await page.getByRole('textbox', { name: 'Título' }).fill(title)
  await page.getByRole('button', { name: 'Guardar' }).click()

  await expect(page.getByText(title).first()).toBeVisible({ timeout: 10_000 })

  // Real finding: this test previously also asserted a "Todos los
  // pendientes" checkbox-based completion panel — neither the text nor a
  // `role="checkbox"` completion control exist anywhere in the current app
  // (pre-existing gap from an earlier UX-007/UX-010 redesign that replaced
  // checkboxes with the "Completar" button in DayAgenda.tsx, unrelated to
  // this change). `getByText(title)` above already proves the reminder was
  // created and rendered for real; completing it through the real current
  // button proves the same real POST /reminders/{id}/complete still works.
  const completeButton = page.getByRole('button', { name: `Completar "${title}"` }).first()
  await expect(completeButton).toBeVisible({ timeout: 10_000 })
  await completeButton.click()
  await expect(completeButton).toBeHidden({ timeout: 10_000 })
})

test('creates a real reminder from Día view', async ({ page }) => {
  const title = `Day view test ${Math.random().toString(36).slice(2, 8)}`

  await login(page)
  await page.getByRole('link', { name: 'Calendario' }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()

  await page.getByRole('radio', { name: 'Día' }).click()
  await expect(page.getByText('Vista diaria')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Ir a hoy' })).toBeVisible()

  // "Nueva tarea" opens a full Crear-tarea dialog — see CreateReminderDialog.tsx.
  await page.getByRole('button', { name: 'Nueva tarea' }).click()
  await page.getByRole('textbox', { name: 'Título' }).fill(title)
  await page.getByRole('button', { name: 'Guardar' }).click()

  // Real finding: no "Todos los pendientes" section exists on Calendario in
  // the current app (see the Semana test above for the same finding) —
  // `getByText(title)` is the real proof this reminder was created.
  await expect(page.getByText(title).first()).toBeVisible({ timeout: 10_000 })
})

test('Semana/Día stay usable on a narrow (390px) viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })

  await login(page)
  // Real finding (pre-existing, AppShell/UX-006, out of this task's scope):
  // `AppShell.module.css` hides `.navLink span:last-child` at <=900px, so
  // the sidebar becomes icon-only — its links lose "Calendario" as their
  // accessible name at this width. Routing around it via href instead of
  // name here; the underlying AppShell nav-labeling gap is reported
  // separately, not silently patched inside this unrelated test.
  await page.locator('nav a[href="/calendar"]').click()
  await expect(page.getByText('Vista mensual')).toBeVisible()

  await page.getByRole('radio', { name: 'Semana' }).click()
  await expect(page.getByText('Vista semanal')).toBeVisible()
  // The first day column's header button must be reachable/clickable at
  // this width (columns scroll horizontally instead of overflowing the
  // page), proving the range selector + week grid don't require desktop
  // width to operate.
  const firstColumnButtons = page.getByRole('button', { name: /^(lun|mar|mié|jue|vie|sáb|dom)/i })
  await expect(firstColumnButtons.first()).toBeVisible()
  await firstColumnButtons.first().click()

  await page.getByRole('radio', { name: 'Día' }).click()
  await expect(page.getByText('Vista diaria')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Ir a hoy' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Nueva tarea' })).toBeVisible()

  await page.screenshot({ path: '../Documentacion/02-ux-ui/screenshots/web-calendar-ux013-mobile-after.png', fullPage: true })
})
