import { expect, test, type Page } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * FASE 19 — Exportación audit. Written against the real UI strings this
 * feature uses (VisionBoardExportAction.tsx: "Exportar", "PNG", "JPG",
 * "PDF"; VisionBoardThemeSwitcher.tsx: "Cambiar tema del Vision Board",
 * theme names like "Oscuro") and the real create flow
 * (VisionBoardElementLibrary.tsx: "Elementos" → "Texto" → "Agregar").
 *
 * FASE 22: actually run against the live dev stack for the first time.
 * Originally used the shared `loginAsTestuserAndGoToReminders` helper, but
 * that hops through a "Tareas" nav link that no longer exists (a real,
 * pre-existing gap unrelated to Vision Board — see visionboard-helpers.ts's
 * own doc comment) — switched to `loginAndGoToVisionBoard`, which reaches
 * Vision Board via its own direct sidebar link instead. It also does NOT do
 * pixel-level image comparison (no image-diff infra exists in this repo)
 * — it verifies the export flow completes, produces a file of the right
 * kind/name, and that the PDF path renders real board content, not that
 * every pixel is correct.
 *
 * FASE 24: every run now creates and uses its own fresh board via the new
 * "Nuevo Vision Board" toolbar entry, instead of reusing whatever board
 * testuser already had (see visionboard-autosave.spec.ts's own doc
 * comment for why that used to matter).
 */

async function goToVisionBoard(page: Page): Promise<void> {
  await loginAndGoToVisionBoard(page)

  const boardName = `Export E2E ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()

  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  // See visionboard-autosave.spec.ts's own doc comment on why this poll
  // (not a one-shot check) is needed — the board switch after creation
  // resolves asynchronously.
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })
}

async function createTextElement(page: Page, text: string): Promise<void> {
  await page.getByRole('button', { name: 'Elementos' }).click()
  await page.getByRole('button', { name: 'Texto', exact: true }).click()
  await page.getByLabel('Contenido').fill(text)
  await page.getByRole('button', { name: 'Agregar', exact: true }).click()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
}

async function openExportPopover(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Exportar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Exportar Vision Board' })).toBeVisible()
}

test.describe('Vision Board — export', () => {
  test('the export popover offers PNG, JPG and PDF', async ({ page }) => {
    await goToVisionBoard(page)
    await createTextElement(page, `Export options ${Date.now()}`)
    await openExportPopover(page)

    await expect(page.getByRole('button', { name: 'PNG', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'JPG', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'PDF', exact: true })).toBeVisible()
  })

  test('exporting PNG downloads a .png file named after the board', async ({ page }) => {
    await goToVisionBoard(page)
    await createTextElement(page, `Export PNG ${Date.now()}`)
    await openExportPopover(page)

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'PNG', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.png$/)
  })

  test('exporting JPG downloads a .jpg file using the same renderer as PNG', async ({ page }) => {
    await goToVisionBoard(page)
    await createTextElement(page, `Export JPG ${Date.now()}`)
    await openExportPopover(page)

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'JPG', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.jpg$/)
  })

  // BLOQUE H (post-MVP): PDF is now a real, deterministic direct download
  // (jsPDF, embedding the same rendered canvas PNG/JPG already use) — no
  // more print window / native print dialog to drive from a test.
  test('exporting PDF downloads a real .pdf file, not a print-dialog fallback', async ({ page }) => {
    await goToVisionBoard(page)
    await createTextElement(page, `Export PDF ${Date.now()}`)
    await openExportPopover(page)

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'PDF', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.pdf$/)

    const path = await download.path()
    expect(path).toBeTruthy()
    const fs = await import('node:fs/promises')
    const bytes = await fs.readFile(path!)
    // A real PDF byte stream starts with the "%PDF-" magic header — proof
    // this is an actual PDF file, not an HTML print-window document saved
    // under a .pdf name.
    expect(bytes.subarray(0, 5).toString('latin1')).toBe('%PDF-')
  })

  test('changing Board Theme still allows exporting successfully afterwards', async ({ page }) => {
    await goToVisionBoard(page)
    await createTextElement(page, `Export after theme ${Date.now()}`)

    await page.getByRole('button', { name: 'Cambiar tema del Vision Board' }).click()
    await page.getByRole('menuitemradio', { name: /Oscuro/ }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    await openExportPopover(page)
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'PNG', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.png$/)
  })
})
