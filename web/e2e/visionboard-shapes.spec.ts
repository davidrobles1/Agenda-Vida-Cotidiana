import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * BLOQUE C (post-MVP) — expanded SHAPE catalog + inline text editing.
 */
async function createBoard(page: import('@playwright/test').Page, namePrefix: string): Promise<void> {
  const boardName = `${namePrefix} ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()
  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  // Same async board-switch race as every other spec in this suite — poll
  // the Switcher's real text, not just "non-empty" (its own fallback text,
  // "Vision Board", is already non-empty before the real board loads).
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })
}

/** Two shapes created back to back land on the exact same default spot
    (documented, expected editor convention, not a bug — see
    visionboard-core-flows.spec.ts's own `createTextElementApart` doc
    comment) — nudges the second one aside so both are independently
    clickable. */
async function nudgeAside(page: import('@playwright/test').Page, locator: import('@playwright/test').Locator) {
  const box = await locator.boundingBox()
  if (!box) throw new Error('element not found to nudge aside')
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.mouse.down()
  await page.mouse.move(box.x + box.width / 2 + 140, box.y + box.height / 2 + 140)
  await page.mouse.up()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
}

test.describe('Vision Board — shapes (BLOQUE C)', () => {
  test('creating a new-catalog shape (star), resizing, rotating and reordering it all work like any other shape', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Shapes-Basic')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Forma', exact: true }).click()
    await page.getByRole('radio', { name: 'Estrella', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const star = page.getByRole('button', { name: /^Forma: Estrella/ })
    await expect(star).toBeVisible()
    await expect(star.locator('svg path')).toHaveCount(1)

    // Resize via the SE handle — respects the generic resize mechanics,
    // nothing shape-specific to break.
    await star.click()
    const before = await star.boundingBox()
    if (!before) throw new Error('star not found')
    const handle = page.locator('[class*="resizeHandle"]').last()
    const handleBox = await handle.boundingBox()
    if (!handleBox) throw new Error('resize handle not found')
    await page.mouse.move(handleBox.x + handleBox.width / 2, handleBox.y + handleBox.height / 2)
    await page.mouse.down()
    await page.mouse.move(handleBox.x + 40, handleBox.y + 30)
    await page.mouse.up()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    const afterResize = await star.boundingBox()
    if (!afterResize) throw new Error('star not found after resize')
    expect(afterResize.width).toBeGreaterThan(before.width)

    // Layers: create a second shape, send the star to back, confirm real
    // zIndex renumbering still happens (same mechanics as every other type).
    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Forma', exact: true }).click()
    await page.getByRole('radio', { name: 'Hexágono', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    await nudgeAside(page, page.getByRole('button', { name: /^Forma: Hexágono/ }))

    await star.click()
    const [reorderResponse] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/reorder') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Enviar al fondo' }).click(),
    ])
    expect(reorderResponse.ok()).toBe(true)
  })

  test('inline text editing on a shape: double-click to edit, Enter to commit, survives undo and export', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Shapes-Text')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Forma', exact: true }).click()
    await page.getByRole('radio', { name: 'Rectángulo', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const shape = page.getByRole('button', { name: /^Forma: Rectángulo/ })
    await shape.dblclick()
    const editor = page.getByRole('textbox', { name: 'Texto de la forma' })
    await expect(editor).toBeFocused()
    await editor.fill('Meta 2026')
    await editor.press('Enter')
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText('Meta 2026')).toBeVisible()

    // Enter (while selected, not editing) also opens the editor — the
    // second entry point the phase asks for alongside double-click.
    await shape.click()
    await page.keyboard.press('Enter')
    const editor2 = page.getByRole('textbox', { name: 'Texto de la forma' })
    await expect(editor2).toBeFocused()
    await expect(editor2).toHaveValue('Meta 2026')
    await editor2.press('Escape')
    // Escape cancels — text unchanged.
    await expect(page.getByText('Meta 2026')).toBeVisible()

    // Undo removes the text edit (the original "Enter to commit" one),
    // same autosave/undo pipeline as every other edit.
    await page.getByRole('button', { name: 'Deshacer' }).click()
    await expect(page.getByText('Meta 2026')).toHaveCount(0)

    // Export still works with a SHAPE on the board (canvas render path,
    // not just the on-screen SVG one).
    await page.getByRole('button', { name: 'Exportar', exact: true }).click()
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'PNG', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.png$/)
  })
})
