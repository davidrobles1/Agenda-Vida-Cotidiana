import { test, expect, type Locator, type Page } from '@playwright/test'

/**
 * ADR-021 — MANTENIMIENTO: verificación por FLUJO, no por datos.
 *
 * Mismo criterio que `payments-flows.spec.ts`: cada prueba siembra lo suyo,
 * lo ejerce entero y lo retira. Lo que se comprueba es el comportamiento de
 * la sección, no el estado de una fila que hoy existe.
 *
 * Las reglas que aquí se fijan y que NO deben "unificarse" con las de
 * Pagos: un mantenimiento vencido avanza desde HOY (el intervalo mide uso,
 * no calendario) y uno sin periodicidad se cierra y no vuelve.
 */

function dayKey(offsetDays = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Mismo cálculo que aplica el dominio al avanzar: hoy + N meses. */
function monthsFromToday(months: number): Date {
  const d = new Date()
  d.setHours(12, 0, 0, 0)
  d.setMonth(d.getMonth() + months)
  return d
}

function esDate(d: Date): string {
  return d.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' })
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

async function goToMaintenance(page: Page): Promise<void> {
  await page.goto('/maintenance')
  await expect(page.getByRole('heading', { name: /Mantenimiento/i }).first()).toBeVisible({
    timeout: 20_000,
  })
  await page.waitForTimeout(1500)
}

function row(page: Page, name: string) {
  return page.locator('article').filter({ hasText: name }).first()
}

async function createMaintenance(
  page: Page,
  name: string,
  dueDate: string,
  everyLabel?: RegExp,
): Promise<void> {
  await page.getByRole('button', { name: /Agregar|Nuevo mantenimiento/i }).first().click()
  const dialog = page.locator('[role=dialog]')
  await expect(dialog).toBeVisible({ timeout: 10_000 })
  await dialog.locator('input').first().fill(name)
  await fillDate(page, dialog, dueDate)
  // Los intervalos del alta son un grupo de radio, no botones.
  if (everyLabel) await dialog.getByRole('radio', { name: everyLabel }).click()
  await page.getByRole('button', { name: /Guardar|Crear|Agregar/i }).last().click()
  await expect(row(page, name)).toBeVisible({ timeout: 15_000 })
}

async function deleteMaintenance(page: Page, name: string): Promise<void> {
  const target = row(page, name)
  if ((await target.count()) === 0) return
  await target.getByRole('button', { name: /Eliminar/i }).click()
  await page.getByRole('button', { name: /Eliminar|Confirmar/i }).last().click()
  await expect(row(page, name)).toHaveCount(0, { timeout: 15_000 })
}

const unique = () => `MF-${Math.random().toString(36).slice(2, 8)}`

test.describe('Mantenimiento · flujos', () => {
  test('alta con periodicidad → persiste tras recargar → baja', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(30), /3 meses/i)
      await goToMaintenance(page)
      await expect(row(page, name)).toContainText(/3 meses/i)
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('completar uno recurrente avanza a la siguiente ocurrencia y lo deja activo', async ({
    page,
  }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(30), /3 meses/i)
      await row(page, name).getByRole('button', { name: /^Hecho$/ }).click()
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })

      await goToMaintenance(page)
      // Sigue existiendo y volverá: lo contrario —darlo por terminado para
      // siempre— era el defecto que ADR-021 corrige.
      await expect(row(page, name)).toContainText(/3 meses/i)
      await expect(row(page, name)).toContainText(/Última vez/i)
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('un mantenimiento vencido avanza desde HOY, no desde la fecha que se pasó', async ({
    page,
  }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(-113), /3 meses/i)
      await expect(row(page, name)).toContainText(/Vencid/i)

      await row(page, name).getByRole('button', { name: /^Hecho$/ }).click()
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })

      await goToMaintenance(page)
      // El intervalo mide uso: si se cambió el aceite hoy, el siguiente
      // cambio toca dentro de tres meses contados desde hoy. Avanzar desde
      // la fecha vencida programaría una revisión ya pasada.
      await expect(row(page, name)).toContainText(esDate(monthsFromToday(3)))
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('sin periodicidad se cierra al completarlo y no vuelve', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(15))
      await expect(row(page, name)).toContainText(/Sin repetición/i)

      await row(page, name).getByRole('button', { name: /^Hecho$/ }).click()
      await expect(page.locator('[role=status]').first()).toContainText(/no vuelve/i, {
        timeout: 15_000,
      })

      await goToMaintenance(page)
      await expect(row(page, name)).toContainText(/Se hizo el/i)
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible()
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('asignar periodicidad a uno que no la tenía conserva su fecha', async ({ page }) => {
    const name = unique()
    const due = dayKey(60)
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, due)
      await expect(row(page, name)).toContainText(/Sin repetición/i)
      const fecha = esDate(new Date(`${due}T12:00:00`))

      await row(page, name).getByRole('button', { name: /Detalle/i }).click()
      const dialog = page.locator('[role=dialog]')
      await dialog.locator('select').selectOption({ label: 'Cada 3 meses' })
      await page.getByRole('button', { name: /Guardar cambios/i }).click()

      await goToMaintenance(page)
      // Asignar periodicidad no debe mover la fecha: el usuario dijo cada
      // cuánto se repite, no cuándo toca la próxima.
      await expect(row(page, name)).toContainText(/3 meses/i)
      await expect(row(page, name)).toContainText(fecha)
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('quitar la periodicidad lo devuelve a puntual', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(45), /3 meses/i)

      await row(page, name).getByRole('button', { name: /Detalle/i }).click()
      const dialog = page.locator('[role=dialog]')
      await dialog.locator('select').selectOption({ label: 'Sin repetición' })
      await page.getByRole('button', { name: /Guardar cambios/i }).click()

      await goToMaintenance(page)
      // Quitar es distinto de no mandar: sin un indicador explícito el
      // backend interpretaba el hueco como "no tocar" y la periodicidad
      // no se podía retirar nunca.
      await expect(row(page, name)).toContainText(/Sin repetición/i)
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('deshacer devuelve el mantenimiento a la fecha que estaba programada', async ({ page }) => {
    const name = unique()
    const due = dayKey(20)
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, due, /3 meses/i)
      const fecha = esDate(new Date(`${due}T12:00:00`))

      await row(page, name).getByRole('button', { name: /^Hecho$/ }).click()
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })
      await row(page, name).getByRole('button', { name: /Deshacer/i }).click()
      await expect(row(page, name).getByRole('button', { name: /^Hecho$/ })).toBeVisible({
        timeout: 15_000,
      })

      await goToMaintenance(page)
      await expect(row(page, name)).toContainText(fecha)
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('marcar dos veces seguidas no salta dos ciclos', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(-5), /3 meses/i)

      const done = row(page, name).getByRole('button', { name: /^Hecho$/ })
      await done.click()
      await done.click({ timeout: 2000 }).catch(() => {})
      await expect(row(page, name).getByRole('button', { name: /Deshacer/i })).toBeVisible({
        timeout: 15_000,
      })

      await goToMaintenance(page)
      // Un doble clic no puede costar un intervalo entero: la siguiente
      // revisión sigue siendo hoy + 3 meses, no hoy + 6.
      await expect(row(page, name)).toContainText(esDate(monthsFromToday(3)))
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('ninguna fila ofrece a la vez completar y deshacer', async ({ page }) => {
    const name = unique()
    await login(page)
    await goToMaintenance(page)
    try {
      await createMaintenance(page, name, dayKey(10), /3 meses/i)
      const contradictorias = await page.locator('article').evaluateAll((articles) =>
        articles.filter((a) => {
          const labels = [...a.querySelectorAll('button')].map((b) => (b.textContent ?? '').trim())
          return labels.some((t) => /^Hecho$/.test(t)) && labels.some((t) => /Deshacer/.test(t))
        }).length,
      )
      expect(contradictorias).toBe(0)
    } finally {
      await goToMaintenance(page)
      await deleteMaintenance(page, name)
    }
  })

  test('si la carga falla, avisa del error en vez de afirmar que no hay mantenimientos', async ({
    page,
  }) => {
    await login(page)
    await page.route('**/api/v1/**', (route) =>
      route.fulfill({ status: 500, contentType: 'application/problem+json', body: '{"detail":"boom"}' }),
    )
    await page.goto('/maintenance')
    await page.waitForTimeout(3000)

    await expect(page.getByText('Aún no registras ningún mantenimiento')).toHaveCount(0)
    await expect(page.locator('[role=alert]').first()).toBeVisible()

    await page.unroute('**/api/v1/**')
    await goToMaintenance(page)
    await expect(page.locator('[role=alert]')).toHaveCount(0)
  })

  test('el estado vacío aparece solo cuando de verdad no hay registros', async ({ page }) => {
    await login(page)
    await goToMaintenance(page)

    const hayRegistros = (await page.locator('article').count()) > 0
    if (hayRegistros) {
      await expect(page.getByText('Aún no registras ningún mantenimiento')).toHaveCount(0)
    } else {
      await expect(page.getByText('Aún no registras ningún mantenimiento')).toBeVisible()
      // El vacío tiene que ofrecer la salida, no solo constatar la ausencia.
      await expect(page.getByRole('button', { name: /Agregar|Nuevo mantenimiento/i }).first()).toBeVisible()
    }
  })
})
