// Verificación real de 3e1 (Objetivos) contra el backend/Keycloak reales.
// Temporal — se borra al terminar. Regla del proyecto: NUNCA page.goto()
// después del login (el token vive solo en memoria); solo navegación in-app.
import { chromium } from '@playwright/test'

const WEB = 'http://localhost:5173'
const suffix = Math.random().toString(36).slice(2, 8)
const objectiveTitle = `Cerrar 3 proyectos ${suffix}`

const browser = await chromium.launch()
const page = await browser.newPage()
const consoleErrors = []
page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()) })
page.on('pageerror', (e) => consoleErrors.push(`pageerror: ${e.message}`))

try {
  await page.goto(WEB)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await page.waitForLoadState('networkidle')
  console.log('1. login OK')

  // Entrar al modo Laboral y a "Hoy" — solo clicks in-app.
  await page.getByRole('link', { name: 'Laboral', exact: true }).click()
  await page.getByRole('link', { name: 'Hoy', exact: true }).click()
  await page.waitForLoadState('networkidle')

  // La tarjeta "Objetivos" debe existir en Hoy (FR-031).
  await page.getByRole('heading', { name: 'Objetivos' }).waitFor({ timeout: 10_000 })
  console.log('2. tarjeta "Objetivos" visible en Hoy OK')

  // "Ver todas" navega a la página dedicada (sin entrada en el navbar).
  await page.locator('section', { has: page.getByRole('heading', { name: 'Objetivos' }) })
    .getByRole('button', { name: 'Ver todas' }).click()
  await page.waitForURL(/\/laboral\/objectives/)
  await page.getByText('Las metas que le dan sentido').waitFor({ timeout: 10_000 })
  console.log('3. navegación Hoy → /laboral/objectives OK (url:', page.url(), ')')

  // Crear un objetivo real con meta numérica.
  await page.getByRole('button', { name: 'Nuevo objetivo' }).click()
  await page.getByRole('textbox', { name: 'Objetivo' }).fill(objectiveTitle)
  await page.getByRole('spinbutton', { name: 'Meta numérica (opcional)' }).fill('3')
  await page.getByRole('button', { name: 'Guardar' }).click()
  await page.getByText(objectiveTitle).first().waitFor({ timeout: 10_000 })
  await page.getByText('0/3').first().waitFor({ timeout: 10_000 })
  console.log('4. objetivo creado contra el backend real, pill "0/3" OK')

  // Progreso manual: +1 dos veces → 2/3, y NO debe autocompletarse.
  await page.getByRole('button', { name: `Sumar progreso a ${objectiveTitle}` }).click()
  await page.getByText('1/3').first().waitFor({ timeout: 10_000 })
  await page.getByRole('button', { name: `Sumar progreso a ${objectiveTitle}` }).click()
  await page.getByText('2/3').first().waitFor({ timeout: 10_000 })
  console.log('5. progreso manual 0→1→2 vía PATCH real OK')

  // Marcar cumplido explícitamente (PATCH completed=true, nunca derivado).
  await page.getByRole('button', { name: 'Cumplido' }).first().click()
  await page.getByText('Cumplido', { exact: true }).first().waitFor({ timeout: 10_000 })
  console.log('6. "Cumplido" (PATCH completed=true) OK')

  // Un objetivo cumplido debe dejar de aparecer en Hoy (AC-018).
  await page.getByRole('link', { name: 'Hoy', exact: true }).click()
  await page.waitForLoadState('networkidle')
  const stillInHoy = await page.getByText(objectiveTitle).count()
  console.log(`7. objetivo cumplido visible en Hoy: ${stillInHoy} (esperado 0)`)

  await page.screenshot({ path: 'e2e/screenshots/_scratch-3e1-hoy.png', fullPage: true })
  console.log('\nERRORES DE CONSOLA:', consoleErrors.length === 0 ? 'ninguno' : consoleErrors)
} catch (e) {
  console.error('FALLO:', e.message)
  await page.screenshot({ path: 'e2e/screenshots/_scratch-3e1-fail.png', fullPage: true })
  console.error('consola:', consoleErrors)
  process.exitCode = 1
} finally {
  await browser.close()
}
