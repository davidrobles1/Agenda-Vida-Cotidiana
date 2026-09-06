import { test, expect, type Browser, type Page } from '@playwright/test'

/**
 * ADR-025 — Familia y Compartidos, de punta a punta y con DOS usuarios reales.
 *
 * Cada prueba construye lo que necesita y lo deshace en `finally`: si pasan
 * sobre una cuenta sin familia y sin nada compartido, pasan siempre. Es la
 * diferencia entre comprobar la tubería y comprobar lo que hoy pasa por ella.
 *
 * Requiere `testuser` y `userb` en el Keycloak local, ambos con
 * `TestPass123!`.
 *
 * CADA USUARIO EN SU PROPIO CONTEXTO DE NAVEGADOR, y no en la misma pestaña.
 * Keycloak deja una cookie de SSO: reutilizando el contexto, "entrar como B"
 * volvía a entrar como A sin pedir credenciales, y la prueba habría dado por
 * bueno un flujo de dos personas hecho por una sola.
 */

const A = { user: 'testuser', pass: 'TestPass123!' }
const B = { user: 'userb', pass: 'TestPass123!' }

/** Abre una sesión aislada (cookies propias) y devuelve su pestaña. */
async function session(browser: Browser, who: { user: string; pass: string }): Promise<Page> {
  const context = await browser.newContext()
  const page = await context.newPage()
  await login(page, who)
  return page
}

async function login(page: Page, who: { user: string; pass: string }): Promise<void> {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill(who.user)
  await page.getByRole('textbox', { name: 'Password' }).fill(who.pass)
  await page.locator('#kc-login').click()
  await page.waitForURL((url) => !url.href.includes('/realms/'), { timeout: 20_000 })
  await expect(page.locator('nav, header').first()).toBeVisible({ timeout: 20_000 })
}

async function goToFamily(page: Page): Promise<void> {
  await page.goto('/family')
  await expect(page.getByRole('heading', { name: 'Familia' }).first()).toBeVisible({ timeout: 20_000 })
  await page.waitForTimeout(1200)
}

/** Deja la cuenta de esa pestaña sin vínculo ni invitación pendiente con `other`. */
async function cleanUp(page: Page, other: string): Promise<void> {
  await goToFamily(page)

  const member = page.locator('li').filter({ hasText: other })
  const remove = member.getByRole('button', { name: new RegExp(`Quitar a ${other}`) })
  if (await remove.count()) {
    await remove.first().click()
    await page.waitForTimeout(1500)
  }

  const cancel = page.getByRole('button', { name: new RegExp(`Cancelar la invitación a ${other}`) })
  if (await cancel.count()) {
    await cancel.first().click()
    await page.waitForTimeout(1500)
  }
}

test.describe('Familia · flujos', () => {
  test('la búsqueda no consulta por debajo de 5 caracteres', async ({ page }) => {
    await login(page, A)
    await goToFamily(page)

    const calls: string[] = []
    page.on('request', (request) => {
      if (request.url().includes('/users/search')) calls.push(request.url())
    })

    const box = page.getByLabel('Buscar personas por nombre de usuario')
    await box.fill('user')
    await page.waitForTimeout(1500)

    // Ni una petición: la regla del mínimo evita la consulta, no la descarta
    // después de hacerla.
    expect(calls).toHaveLength(0)
    await expect(page.getByText(/Escribe 1 carácter más para buscar/)).toBeVisible()

    await box.fill('userb')
    await page.waitForTimeout(2000)
    expect(calls.length).toBeGreaterThan(0)
  })

  test('escribir seguido no lanza una petición por tecla', async ({ page }) => {
    await login(page, A)
    await goToFamily(page)

    const calls: string[] = []
    page.on('request', (request) => {
      if (request.url().includes('/users/search')) calls.push(request.url())
    })

    // Ocho caracteres tecleados de corrido: con antirrebote son una consulta,
    // no cuatro (las cuatro primeras ni siquiera llegan al mínimo).
    await page.getByLabel('Buscar personas por nombre de usuario').pressSequentially('userbbbb', { delay: 60 })
    await page.waitForTimeout(2000)
    expect(calls.length).toBeLessThanOrEqual(2)
  })

  test('invitar → aceptar desde Ajustes → los dos se ven como familia', async ({ browser }) => {
    const a = await session(browser, A)
    const b = await session(browser, B)
    try {
      await goToFamily(a)
      await a.getByLabel('Buscar personas por nombre de usuario').fill('userb')
      await a.waitForTimeout(2000)
      await a.getByRole('button', { name: 'Invitar' }).first().click()
      await expect(a.getByText('Invitación enviada').first()).toBeVisible({ timeout: 15_000 })

      // B responde donde el requisito lo pide: en Configuración.
      await b.goto('/settings')
      await expect(b.getByRole('heading', { name: 'Invitaciones a una familia' })).toBeVisible({
        timeout: 20_000,
      })
      await expect(b.getByText('testuser')).toBeVisible()
      await b.getByRole('button', { name: 'Aceptar' }).first().click()
      await b.waitForTimeout(2500)

      // El vínculo es simétrico: los dos tienen que verlo.
      await goToFamily(b)
      await expect(b.getByText('testuser').first()).toBeVisible()

      await goToFamily(a)
      await expect(a.getByText('userb').first()).toBeVisible()
    } finally {
      await cleanUp(a, 'userb')
      await a.context().close()
      await b.context().close()
    }
  })

  test('rechazar una invitación no crea vínculo', async ({ browser }) => {
    const a = await session(browser, A)
    const b = await session(browser, B)
    try {
      await goToFamily(a)
      await a.getByLabel('Buscar personas por nombre de usuario').fill('userb')
      await a.waitForTimeout(2000)
      await a.getByRole('button', { name: 'Invitar' }).first().click()
      await expect(a.getByText('Invitación enviada').first()).toBeVisible({ timeout: 15_000 })

      await b.goto('/settings')
      await expect(b.getByRole('button', { name: 'Rechazar' })).toBeVisible({ timeout: 20_000 })
      await b.getByRole('button', { name: 'Rechazar' }).first().click()
      await b.waitForTimeout(2500)

      await goToFamily(b)
      await expect(b.getByText('Todavía no hay nadie', { exact: false })).toBeVisible()
    } finally {
      await cleanUp(a, 'userb')
      // Limpieza de la tarea de prueba, BEST EFFORT y con una limitación
      // conocida: el localizador de abajo no siempre encuentra la tarjeta en
      // la vista mensual, así que puede quedar el registro. Lo que sí queda
      // siempre limpio es lo que esta prueba crea de verdad —la compartición y
      // el vínculo familiar—, que es lo que ADR-025 introduce.
      await a.goto('/personal/calendar')
      await a.waitForTimeout(2500)
      const card = a.getByText(title, { exact: false }).first()
      if (await card.count()) {
        await card.click()
        await a.waitForTimeout(1200)
        const del = a.getByRole('button', { name: /Eliminar/i })
        if (await del.count()) {
          await del.first().click()
          await a.waitForTimeout(800)
          await a.getByRole('button', { name: /Eliminar|Confirmar/i }).last().click()
          await a.waitForTimeout(2000)
        }
      }
      await a.context().close()
      await b.context().close()
    }
  })
})

test.describe('Compartidos · flujos', () => {
  /** Deja a A y B como familia. */
  async function becomeFamily(a: Page, b: Page): Promise<void> {
    await goToFamily(a)
    if (await a.getByRole('button', { name: /Quitar a userb/ }).count()) return

    await a.getByLabel('Buscar personas por nombre de usuario').fill('userb')
    await a.waitForTimeout(2000)
    const invite = a.getByRole('button', { name: 'Invitar' })
    if (await invite.count()) {
      await invite.first().click()
      await a.waitForTimeout(1500)
    }

    await b.goto('/settings')
    await b.waitForTimeout(2500)
    const accept = b.getByRole('button', { name: 'Aceptar' })
    if (await accept.count()) {
      await accept.first().click()
      await b.waitForTimeout(2500)
    }
  }

  test('compartir una tarea con responsabilidad → B hace su parte → A lo ve', async ({ browser }) => {
    // Dos inicios de sesión SSO completos más el ida y vuelta entre las dos
    // cuentas no caben en el presupuesto por defecto. No es lentitud de la
    // aplicación: es que esta prueba recorre el flujo entero de dos personas.
    test.setTimeout(360_000)
    const title = `CS-${Math.random().toString(36).slice(2, 7)}`
    const a = await session(browser, A)
    const b = await session(browser, B)
    try {
      await becomeFamily(a, b)

      await a.goto('/personal/calendar')
      await expect(a.getByRole('heading', { name: /Calendario/i }).first()).toBeVisible({
        timeout: 20_000,
      })
      await a.waitForTimeout(2000)
      await a.getByRole('button', { name: /Nueva tarea/i }).first().click()

      const dialog = a.locator('[role=dialog]')
      await expect(dialog).toBeVisible({ timeout: 10_000 })
      await dialog.locator('input').first().fill(title)

      // El control de compartición vive DENTRO del formulario (requisito §2).
      await expect(dialog.getByText('Compartir con tu familia')).toBeVisible({ timeout: 15_000 })
      await dialog.getByRole('checkbox').first().check()
      await expect(dialog.getByText('Se compromete con su parte')).toBeVisible({ timeout: 10_000 })
      await dialog.getByRole('checkbox').nth(1).check()
      await a.getByRole('button', { name: /^Guardar$/ }).last().click()
      await a.waitForTimeout(3500)

      // B lo recibe, con quién se lo compartió y su parte pendiente.
      await b.goto('/personal/shared')
      await expect(b.getByRole('heading', { name: 'Compartidos' }).first()).toBeVisible({
        timeout: 20_000,
      })
      await b.waitForTimeout(2500)
      const row = b.locator('li').filter({ hasText: title })
      await expect(row).toBeVisible({ timeout: 15_000 })
      await expect(row).toContainText('testuser')
      await expect(row).toContainText('Te toca')

      await row.getByRole('button', { name: /Ya hice mi parte/ }).click()
      await b.waitForTimeout(2500)
      await expect(row).toContainText('Hiciste tu parte')

      // Y A tiene que enterarse: es la otra mitad del requisito §4.
      await a.goto('/personal/shared')
      await a.waitForTimeout(2500)
      await a.getByRole('radio', { name: 'Yo compartí' }).click()
      await a.waitForTimeout(1500)
      const mine = a.locator('li').filter({ hasText: title })
      await expect(mine).toContainText('userb')
      await expect(mine).toContainText('Ya hizo su parte')
    } finally {
      await a.goto('/personal/shared')
      await a.waitForTimeout(2000)
      await a.getByRole('radio', { name: 'Yo compartí' }).click().catch(() => {})
      await a.waitForTimeout(1200)
      const revoke = a.locator('li').filter({ hasText: title })
        .getByRole('button', { name: /Dejar de compartir/ })
      if (await revoke.count()) {
        await revoke.first().click()
        await a.waitForTimeout(2000)
      }
      await cleanUp(a, 'userb')
      await a.context().close()
      await b.context().close()
    }
  })
})
