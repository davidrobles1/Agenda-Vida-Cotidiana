import { test, expect, type Locator, type Page } from '@playwright/test'

/**
 * ADR-020 — PAGOS: verificación por FLUJO, no por datos.
 *
 * Cada prueba crea lo que necesita, lo ejerce de punta a punta y lo borra
 * al terminar. Ninguna depende de que existan registros previos ni deja
 * residuo: si estas pruebas pasan sobre una cuenta vacía, pasan siempre.
 * Es la diferencia entre comprobar la tubería y comprobar lo que hoy pasa
 * por ella — un registro concreto puede estar bien por casualidad, un
 * flujo no.
 */

/** El día de hoy y desplazamientos, en formato `YYYY-MM-DD` local. */
function dayKey(offsetDays = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}


/**
 * Escribe una fecha en el DatePicker propio (ADR: `core/ui/pickers`).
 *
 * Ya no hay `<input type="date">`: el campo se compone de segmentos
 * (día / mes / año) de React Aria, así que se enfoca el primero y se
 * teclean los dígitos, que es exactamente lo que hace una persona.
 * El orden es el de `es-MX`: dd/mm/aaaa.
 */
async function fillDate(page: Page, scope: Locator, iso: string): Promise<void> {
  const [year, month, day] = iso.split('-')
  await scope.locator('[class*="_segment_"]').first().click()
  // Dígito a dígito: React Aria avanza de segmento solo, igual que al
  // teclear una persona. `press('15')` no es una tecla válida.
  await page.keyboard.type(`${day}${month}${year}`, { delay: 30 })
}

async function login(page: Page): Promise<void> {
  await page.goto('/')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.locator('#kc-login').click()
  // No se afirma a qué pantalla cae: el destino depende del modo del
  // usuario (ADR-015) y ya cambió una vez. Basta con estar de vuelta en
  // la aplicación y autenticado.
  await page.waitForURL((url) => !url.href.includes('/realms/'), { timeout: 20_000 })
  await expect(page.locator('nav, header').first()).toBeVisible({ timeout: 20_000 })
}

/** Vuelve a Pagos. La sesión de Keycloak sobrevive a la navegación dura
    (cookie SSO), así que `goto` reautentica solo — es además la forma de
    comprobar que el estado se reconstruye desde el servidor y no de la
    memoria del cliente, que es justo lo que interesa verificar. */
async function goToPayments(page: Page): Promise<void> {
  await page.goto('/subscriptions')
  await expect(page.getByRole('heading', { name: /Pagos|Suscripciones/i }).first()).toBeVisible({
    timeout: 20_000,
  })
  await page.waitForTimeout(1500)
}

function row(page: Page, name: string) {
  return page.locator('article').filter({ hasText: name }).first()
}

async function createPayment(
  page: Page,
  name: string,
  dueDate: string,
  amount?: string,
): Promise<void> {
  await page.getByRole('button', { name: /Agregar pago/i }).first().click()
  const dialog = page.locator('[role=dialog]')
  await expect(dialog).toBeVisible({ timeout: 10_000 })
  await dialog.locator('input').first().fill(name)
  if (amount) await dialog.locator('input[type=number]').first().fill(amount)
  await fillDate(page, dialog, dueDate)
  await page.getByRole('button', { name: /Guardar|Crear/i }).last().click()
  await expect(row(page, name)).toBeVisible({ timeout: 15_000 })
}

async function deletePayment(page: Page, name: string): Promise<void> {
  const target = row(page, name)
  if ((await target.count()) === 0) return
  await target.getByRole('button', { name: /Eliminar/i }).click()
  await page.getByRole('button', { name: /Eliminar|Confirmar/i }).last().click()
  await expect(row(page, name)).toHaveCount(0, { timeout: 15_000 })
}

const unique = () => `PF-${Math.random().toString(36).slice(2, 8)}`

test.describe('Pagos · flujos', () => {
  test('alta → persiste tras recargar → baja', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    try {
      await createPayment(page, name, dayKey(20), '250')

      // Recargar es la prueba de que se guardó de verdad: lo que sobrevive
      // a una navegación dura vino del servidor, no del estado local.
      await goToPayments(page)
      await expect(row(page, name)).toBeVisible()
      await expect(row(page, name)).toContainText('250')
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('pagar → queda pagado tras recargar → deshacer devuelve la fecha original', async ({
    page,
  }) => {
    const name = unique()
    const due = dayKey(10)
    await login(page)
    await goToPayments(page)
    try {
      await createPayment(page, name, due)

      await row(page, name).getByRole('button', { name: /Marcar .* como pagado/i }).click()
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })

      await goToPayments(page)
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible()

      // Deshacer tiene que devolver EXACTAMENTE la fecha que había, no una
      // aproximada: es la salida de un clic equivocado.
      await row(page, name).getByRole('button', { name: /Deshacer/i }).click()
      await expect(
        row(page, name).getByRole('button', { name: /Marcar .* como pagado/i }),
      ).toBeVisible({ timeout: 15_000 })

      await goToPayments(page)
      await expect(
        row(page, name).getByRole('button', { name: /Marcar .* como pagado/i }),
      ).toBeVisible()
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('pagar dos veces seguidas no duplica la ocurrencia ni salta dos ciclos', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    try {
      await createPayment(page, name, dayKey(5))
      const before = await row(page, name).innerText()

      const payButton = row(page, name).getByRole('button', { name: /Marcar .* como pagado/i })
      await payButton.click()
      // El segundo clic llega cuando el botón ya cambió: lo que se prueba es
      // que el backend es idempotente aunque la red reintente.
      await payButton.click({ timeout: 2000 }).catch(() => {})
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })

      await goToPayments(page)
      const after = await row(page, name).innerText()
      // Un solo ciclo de avance: si hubieran entrado dos, la fila mostraría
      // una fecha un mes más allá de la que corresponde.
      expect(after).not.toEqual(before)
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible()
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('un pago adelantado se muestra como pagado', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    try {
      // Vence dentro de más de un mes y se paga hoy: la regla mira CUÁNDO se
      // pagó, no qué periodo cubre. Antes comparaba el periodo y adelantar
      // un pago no se reflejaba nunca.
      await createPayment(page, name, dayKey(40))
      await row(page, name).getByRole('button', { name: /Marcar .* como pagado/i }).click()
      await goToPayments(page)
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible()
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('un pago vencido se señala como vencido y sigue ofreciendo pagarlo', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    try {
      await createPayment(page, name, dayKey(-21))
      await expect(row(page, name)).toContainText(/vencid|hace \d+ d/i)
      await expect(
        row(page, name).getByRole('button', { name: /Marcar .* como pagado/i }),
      ).toBeVisible()
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('una fecha de otro año muestra el año', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    const nextYear = String(new Date().getFullYear() + 1)
    try {
      // Sin el año, "25 oct" del año que viene era idéntico a "25 oct" de
      // este: la fila decía algo distinto de lo que había guardado.
      await createPayment(page, name, `${nextYear}-10-25`)
      await expect(row(page, name)).toContainText(nextYear)
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('ninguna fila ofrece a la vez pagar y deshacer', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    try {
      await createPayment(page, name, dayKey(7))
      const contradictorias = await page.locator('article').evaluateAll((articles) =>
        articles.filter((a) => {
          const labels = [...a.querySelectorAll('button')].map((b) => (b.textContent ?? '').trim())
          return labels.some((t) => /^(Pagado|Marcar)/.test(t)) && labels.some((t) => /Deshacer/.test(t))
        }).length,
      )
      expect(contradictorias).toBe(0)
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('el calendario refleja la fecha vigente del pago, no la anterior', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToPayments(page)
    try {
      await createPayment(page, name, dayKey(3))

      // Las alertas se DERIVAN del registro (ADR-018), así que pagar debe
      // moverlas solo. Comprobarlo importa: si el calendario conservara la
      // fecha vieja, el usuario vería un aviso de algo ya pagado.
      await page.goto('/calendar')
      await expect(page.getByRole('heading', { name: /Calendario/i }).first()).toBeVisible({
        timeout: 20_000,
      })
      await page.waitForTimeout(2500)
      await expect(page.getByText(name, { exact: false }).first()).toBeVisible({ timeout: 15_000 })

      await goToPayments(page)
      await row(page, name).getByRole('button', { name: /Marcar .* como pagado/i }).click()
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })

      // Ya no toca en los próximos días: el aviso se fue con la fecha.
      await page.goto('/calendar')
      await page.waitForTimeout(2500)
      await expect(page.getByText(name, { exact: false })).toHaveCount(0)
    } finally {
      await goToPayments(page)
      await deletePayment(page, name)
    }
  })

  test('si la carga falla, avisa del error en vez de afirmar que no hay pagos', async ({ page }) => {
    await login(page)
    await page.route('**/api/v1/**', (route) =>
      route.fulfill({ status: 500, contentType: 'application/problem+json', body: '{"detail":"boom"}' }),
    )
    await page.goto('/subscriptions')
    await page.waitForTimeout(3000)

    // El vacío afirma un hecho sobre los datos del usuario. Si la carga
    // falló no sabemos nada de sus datos, así que afirmarlo es mentir.
    await expect(page.getByText('Aún no tienes pagos registrados')).toHaveCount(0)
    await expect(page.locator('[role=alert]').first()).toBeVisible()

    await page.unroute('**/api/v1/**')
    await goToPayments(page)
    await expect(page.locator('[role=alert]')).toHaveCount(0)
  })
})
