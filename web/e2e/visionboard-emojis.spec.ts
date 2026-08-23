import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * BLOQUE D (post-MVP) — expanded sticker catalog + the new "😊 Emojis"
 * category. Emojis behave as a real STICKER-type element (data.emojiId).
 */
test.describe('Vision Board — expanded stickers & emojis (BLOQUE D)', () => {
  test('creating an emoji behaves as a real board element: drag, undo, export', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
    const boardName = `Emojis ${Date.now()}`
    await page.getByLabel('Nombre').fill(boardName)
    await page.getByRole('button', { name: 'Crear', exact: true }).click()
    await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Emojis', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Elegir emoji' })).toBeVisible()
    await page.getByRole('radio', { name: 'Avión' }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const emoji = page.getByRole('button', { name: 'Emoji: Avión', exact: true })
    await expect(emoji).toBeVisible()

    // Drag it — a plain <span> badge, never an <img>, so it was never
    // exposed to the native-image-drag bug BLOQUE D.8 fixed, but this
    // confirms it moves correctly like any other element regardless.
    const before = await emoji.boundingBox()
    if (!before) throw new Error('emoji not found')
    await page.mouse.move(before.x + before.width / 2, before.y + before.height / 2)
    await page.mouse.down()
    for (let i = 1; i <= 6; i += 1) {
      await page.mouse.move(before.x + before.width / 2 + i * 30, before.y + before.height / 2 + i * 20)
    }
    await page.mouse.up()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    const after = await emoji.boundingBox()
    if (!after) throw new Error('emoji not found after drag')
    expect(Math.abs(after.x - before.x - 180)).toBeLessThan(10)

    // Undo restores the move.
    await page.getByRole('button', { name: 'Deshacer' }).click()
    const afterUndo = await emoji.boundingBox()
    if (!afterUndo) throw new Error('emoji not found after undo')
    expect(Math.abs(afterUndo.x - before.x)).toBeLessThan(5)

    // Export still works with an emoji element on the board.
    await page.getByRole('button', { name: 'Exportar', exact: true }).click()
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'PNG', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.png$/)
  })

  test('expanded-catalog sticker (icon badge, no asset file) creates and renders correctly', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
    const boardName = `Stickers-Expanded ${Date.now()}`
    await page.getByLabel('Nombre').fill(boardName)
    await page.getByRole('button', { name: 'Crear', exact: true }).click()
    await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Sticker', exact: true }).click()
    await page.getByRole('radio', { name: 'Tecnología', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const sticker = page.getByRole('button', { name: 'Sticker: Tecnología', exact: true })
    await expect(sticker).toBeVisible()
    await expect(sticker.locator('svg')).toBeVisible()
  })

  test('editing a sticker can switch it to an emoji and back', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
    const boardName = `Sticker-Edit ${Date.now()}`
    await page.getByLabel('Nombre').fill(boardName)
    await page.getByRole('button', { name: 'Crear', exact: true }).click()
    await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Sticker', exact: true }).click()
    await page.getByRole('radio', { name: 'Celebración', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const element = page.getByRole('button', { name: 'Sticker: Celebración', exact: true })
    await element.click()
    await page.getByRole('button', { name: 'Editar', exact: true }).click()
    await page.getByRole('radio', { name: 'Emojis', exact: true }).click()
    await page.getByRole('radio', { name: 'Corazón' }).first().click()
    await page.getByRole('button', { name: 'Guardar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    await expect(page.getByRole('button', { name: /^Emoji: Corazón/ })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Sticker: Celebración', exact: true })).toHaveCount(0)
  })
})
