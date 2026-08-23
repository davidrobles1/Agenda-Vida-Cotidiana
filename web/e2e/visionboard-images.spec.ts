import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const TEST_IMAGE = path.join(__dirname, 'fixtures', 'test-image.png')

/**
 * BLOQUE B (post-MVP) — real image storage: upload from the file picker,
 * paste from the clipboard, and the broken-image placeholder. Drag & drop
 * from the OS file explorer shares the exact same `uploadAndPlaceImage`
 * code path as paste (VisionBoardCanvas.tsx's own doc comment) — not
 * separately covered here, since Playwright can't simulate a real OS-level
 * file drag, only synthesize the DOM drop event, which would just re-test
 * the same function paste already exercises for real.
 */
async function createBoard(page: import('@playwright/test').Page, namePrefix: string): Promise<void> {
  const boardName = `${namePrefix} ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()
  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  // Same async board-switch race as every other spec in this suite
  // (CreateVisionBoardDialog fires `onCreated` without awaiting the real
  // switch) — poll the Switcher's own text instead of proceeding the
  // instant the toolbar first appears, or the very next click can land on
  // a DOM node about to be torn down by the `key={board.id}` remount.
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })
}

test.describe('Vision Board — images (BLOQUE B)', () => {
  test('uploading an image from the file picker creates a real IMAGE element and autosaves', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Images-Upload')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Imagen', exact: true }).click()
    // "Subir archivo" is the default source.
    await page.locator('input[type="file"]').setInputFiles(TEST_IMAGE)
    // Preview appears before Agregar is even pressed.
    await expect(page.locator('img[src^="blob:"]')).toBeVisible({ timeout: 5_000 })
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const element = page.getByRole('button', { name: 'Imagen', exact: true })
    await expect(element).toBeVisible()
    // Renders the real uploaded bytes (resolved via the authenticated
    // GET /vision-board-images/{id} → blob: URL), not a broken-image state.
    await expect(element.locator('img')).toBeVisible()
    await expect(element.locator('[class*="elementImageBroken"]')).toHaveCount(0)
  })

  test('an unreachable image URL shows the broken-image placeholder, not a blank box', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Images-Broken')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Imagen', exact: true }).click()
    await page.getByRole('radio', { name: 'URL', exact: true }).click()
    await page.getByLabel('URL de la imagen').fill('https://example.invalid/this-does-not-exist.png')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    await expect(page.getByText('Imagen no disponible')).toBeVisible({ timeout: 10_000 })
  })

  test('pasting an image from the clipboard creates it centered on the last pointer position', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Images-Paste')

    const canvas = page.locator('[class*="canvas"]').first()
    const box = await canvas.boundingBox()
    if (!box) throw new Error('canvas not found')
    const pointerX = box.x + 300
    const pointerY = box.y + 200
    await page.mouse.move(pointerX, pointerY)

    // Synthesize a real clipboard paste with an image File — Playwright has
    // no `page.paste()` API, so this builds the same ClipboardEvent shape
    // the browser itself would dispatch and fires it on window, exactly
    // where VisionBoardCanvas.tsx's own paste listener is attached.
    const fs = await import('node:fs/promises')
    const bytes = await fs.readFile(TEST_IMAGE)
    const b64 = bytes.toString('base64')

    await page.evaluate((base64Data) => {
      const binary = atob(base64Data)
      const bytes = new Uint8Array(binary.length)
      for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i)
      const file = new File([bytes], 'pasted.png', { type: 'image/png' })
      const dataTransfer = new DataTransfer()
      dataTransfer.items.add(file)
      const event = new ClipboardEvent('paste', { bubbles: true, cancelable: true })
      Object.defineProperty(event, 'clipboardData', { value: dataTransfer })
      window.dispatchEvent(event)
    }, b64)

    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    const pasted = page.getByRole('button', { name: 'Imagen', exact: true })
    await expect(pasted).toBeVisible()
    const pastedBox = await pasted.boundingBox()
    if (!pastedBox) throw new Error('pasted element not found')
    // Centered on the pointer — within a few px (the element's own
    // default 240x160 size divided by 2 on each axis).
    const pastedCenterX = pastedBox.x + pastedBox.width / 2
    const pastedCenterY = pastedBox.y + pastedBox.height / 2
    expect(Math.abs(pastedCenterX - pointerX)).toBeLessThan(5)
    expect(Math.abs(pastedCenterY - pointerY)).toBeLessThan(5)
  })
})
