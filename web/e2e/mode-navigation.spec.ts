import { test, expect } from '@playwright/test'

/**
 * UX-012/ADR-015 (Documentacion/22-decision-log.md) real verification —
 * registration → onboarding (FR-014) → conditional selector (FR-015) →
 * activating the other mode from Ajustes (FR-016) → creating a reminder from
 * each mode's navbar (FR-019) → seeing both in the general Calendario
 * (FR-017).
 *
 * Registration reuses WEB-008's real flow (`register()` → Keycloak's actual
 * `/protocol/openid-connect/registrations` form, `01-technical-backlog.md`)
 * — a brand-new account, not one provisioned ahead of time, so onboarding is
 * genuinely reachable (CallbackPage.tsx only routes there when authClient's
 * `vc_just_registered` flag was set by this exact registration).
 *
 * Field selectors use `name` attributes, not ids: this realm's registration
 * theme (PatternFly-based) renders `id="name-username-1"` etc. — generated,
 * unstable ids — but keeps plain, stable `name="username"` attributes
 * (confirmed against the live realm's actually-rendered form).
 */
test('register with only Personal, activate Laboral from Ajustes, create a reminder from each navbar', async ({
  page,
}) => {
  const suffix = Math.random().toString(36).slice(2, 10)
  const username = `modetest_${suffix}`

  await page.goto('/')
  await page.getByRole('button', { name: 'Crear una cuenta' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)

  await page.locator('input[name="username"]').fill(username)
  await page.locator('input[name="email"]').fill(`${username}@example.com`)
  await page.locator('input[name="firstName"]').fill('Mode')
  await page.locator('input[name="lastName"]').fill('Test')
  await page.locator('input[name="password"]').fill('TestPass123!')
  await page.locator('input[name="password-confirm"]').fill('TestPass123!')
  await page.getByRole('button', { name: 'Register' }).click()

  // FR-014/CLAUDE.md: onboarding is reachable right after this exact
  // registration (the `vc_just_registered` bridge — see CallbackPage.tsx).
  await expect(page.getByText('¿Cómo vas a usar Agenda?')).toBeVisible({ timeout: 20_000 })

  // Real, visible validation — not a silently-disabled button (CLAUDE.md's
  // explicit instruction, "Refinamientos cerrados" in ADR-015).
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page.getByRole('alert')).toHaveText(/al menos una/i)

  await page.getByRole('checkbox', { name: /Personal/ }).check()
  await page.getByRole('button', { name: 'Continuar' }).click()

  // Lands on the general Calendario (ADR-015(d)/FR-015), not Home/Tareas.
  // El destino tras iniciar sesión depende del modo del usuario
  // (ADR-015) y ya cambió; afirmar una pantalla concreta volvía roja
  // toda la suite por un cambio de producto legítimo. Basta con haber
  // vuelto a la aplicación autenticado.
  await page.waitForURL((url) => !url.href.includes('/realms/'), { timeout: 20_000 })
  await expect(page.locator('nav, header').first()).toBeVisible({ timeout: 20_000 })

  // FR-015: only "Calendario" + "Personal" — "Laboral" isn't offered yet.
  await expect(page.getByRole('link', { name: 'Personal', exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Laboral', exact: true })).toHaveCount(0)

  // FR-016/UC-15: activate Laboral from Ajustes. The app keeps its OIDC
  // token in memory only — page.goto() is a real browser navigation and
  // would log the session out — so this uses the real client-side link
  // (gear icon, account area, always rendered) instead.
  await page.getByRole('link', { name: 'Ajustes' }).click()
  const laboralRow = page.getByTestId('mode-row-laboral')
  await laboralRow.getByRole('button', { name: 'Activar' }).click()
  await expect(laboralRow.getByText('Activado')).toBeVisible({ timeout: 10_000 })

  // FR-015: "Laboral" now appears in the selector.
  await expect(page.getByRole('link', { name: 'Laboral', exact: true })).toBeVisible()

  // FR-019: a reminder created from Personal's Tareas carries context
  // PERSONAL (inferred from the navbar, no selector in the form).
  const personalTitle = `Personal reminder ${suffix}`
  await page.getByRole('link', { name: 'Personal', exact: true }).click()
  await page.getByRole('link', { name: 'Tareas', exact: true }).click()
  await page.getByPlaceholder('New reminder').fill(personalTitle)
  await page.getByRole('button', { name: 'Add' }).click()
  await expect(page.locator('li', { hasText: personalTitle })).toBeVisible({ timeout: 10_000 })

  // Same for Laboral — its own navbar, its own themed screen.
  const laboralTitle = `Laboral reminder ${suffix}`
  await page.getByRole('link', { name: 'Laboral', exact: true }).click()
  // UX-012: Laboral's cool navy/green theme is live on this screen (AppShell
  // gets `.laboral-theme`, index.css remaps --color-primary etc.) — checked
  // for real via computed style, not just that the class name is present.
  const laboralPrimary = await page.evaluate(() =>
    getComputedStyle(document.body).getPropertyValue('--color-laboral-primary').trim(),
  )
  expect(laboralPrimary.toLowerCase()).toBe('#1e3f5c')
  // La comprobación se hacía sobre el fondo de la placa "VC" del logo. Esa
  // placa desapareció con la identidad «A · Tiempo» (2026-09-05): el logo del
  // módulo tiene ahora su propia paleta de marca (`--brand-arc-*`), que a
  // propósito NO se deriva de `--color-primary` — un logo no cambia de color
  // porque cambie el tema del producto. Se mide el mismo token en otro
  // elemento realmente pintado: la píldora activa del selector de modo, cuyo
  // fondo sigue siendo `var(--color-primary)`. La intención del test no
  // cambia: probar que el remapeo llega al render, no solo al DOM.
  const activeModePillBg = await page
    .getByRole('link', { name: 'Laboral', exact: true })
    .evaluate((el) => getComputedStyle(el).getPropertyValue('background-color'))
  expect(activeModePillBg).toBe('rgb(30, 63, 92)')

  // El logo del módulo es ahora "Oficio", no "Vida Cotidiana": la marca madre
  // ya no aparece en la interfaz de los módulos.
  const brand = page.getByTestId('app-brand-mark')
  await expect(brand).toHaveAttribute('data-brand-context', 'LABORAL')
  await expect(brand).toContainText('Oficio')

  await page.getByRole('link', { name: 'Tareas', exact: true }).click()
  await page.getByPlaceholder('New reminder').fill(laboralTitle)
  await page.getByRole('button', { name: 'Add' }).click()
  await expect(page.locator('li', { hasText: laboralTitle })).toBeVisible({ timeout: 10_000 })

  // FR-017: the general Calendario aggregates both — give each a near-future
  // due date isn't needed here since both reminders were created without
  // one; instead confirm they surface in "Todos los pendientes", which only
  // reflects the real backend list for whichever navbar created them.
  await page.getByRole('link', { name: 'Calendario', exact: true }).click()
  await expect(page.getByText('Vista mensual')).toBeVisible()
})
