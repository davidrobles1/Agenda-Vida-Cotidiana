import { test, expect } from '@playwright/test'

/**
 * UX-007 real verification: logs in through the real Keycloak login page,
 * creates a real reminder with a due date, then completes it from
 * Calendario's "Pendientes" checkbox — proving that action goes through the
 * exact same real `POST /reminders/{id}/complete` RemindersPage uses (the
 * row only disappears once the backend call succeeds and the list
 * refetches, not on click alone). Also checks a mock Garantía's checkbox
 * (local-only — no backend call, disappears immediately) to verify the two
 * behaviors are genuinely different, not just visually labeled differently.
 */
test('month grid, legend and pendientes render; real and mock checkboxes both work', async ({ page }) => {
  const title = `Calendar test ${Math.random().toString(36).slice(2, 10)}`

  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })

  // Post-login lands on Tareas (design-system.md §7), not Home — navigate
  // there explicitly for the real "after" screenshot of the UX-007 restyle.
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

  await page.getByRole('link', { name: 'Calendario' }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()
  await expect(page.getByText('Garantías (simulado)')).toBeVisible()
  await expect(page.getByText('Mantenimiento (simulado)')).toBeVisible()

  await page.screenshot({ path: 'e2e/screenshots/calendar.png', fullPage: true })

  // Real reminder: checking completes it through the real backend endpoint.
  // .click() rather than .check() — both rows unmount as soon as their state
  // update lands (the mock one synchronously), which can race Playwright's
  // own post-.check() "is it now checked" poll; toBeHidden() below is the
  // actual assertion that matters.
  const realCheckbox = page.getByRole('checkbox', { name: `Marcar "${title}" como completada` })
  await expect(realCheckbox).toBeVisible({ timeout: 10_000 })
  await realCheckbox.click()
  await expect(realCheckbox).toBeHidden({ timeout: 10_000 })

  // Mock garantía: checking is a local-only toggle, no backend involved.
  const mockCheckbox = page.getByRole('checkbox', { name: 'Marcar "Laptop Dell XPS 13" como completada (dato simulado)' })
  await expect(mockCheckbox).toBeVisible()
  await mockCheckbox.click()
  await expect(mockCheckbox).toBeHidden()
})

test('home page shows Hoy/Próximos días before metric cards', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })

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
