import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * BLOQUE D (post-MVP) — root-cause fix for "mover una imagen/sticker
 * recién creado puede hacer que desaparezca": confirmed for real, via live
 * instrumentation counting actual `pointermove` deliveries, that dragging a
 * STICKER/IMAGE (never TEXT/NOTE/SHAPE — reproduced the identical gesture
 * against a TEXT element under the same conditions and it worked perfectly)
 * stopped receiving `pointermove` events after the very first one, every
 * time. Root cause: an `<img>` is *draggable by default* in every browser —
 * mid-gesture the browser's own native HTML5 image drag-and-drop engaged
 * and took over the pointer from this feature's `setPointerCapture()`-based
 * custom drag, starving `handlePointerMove` of further events; the eventual
 * `pointerup` that did arrive came through whatever was left of that
 * hijacked gesture, sometimes with nowhere-near-real coordinates
 * (`clientX: 0, clientY: 0` reproduced directly) — applied as if the
 * pointer had jumped to the screen's corner, flinging the element to a
 * wildly wrong, often off-board position. The element itself was never
 * deleted, it just landed somewhere nobody would look. Fixed with
 * `draggable={false}` on both `<img>` tags (VisionBoardElementView.tsx),
 * plus `lastClientX`/`lastClientY` tracking as defense in depth against any
 * other source of a bad `pointerup` reading.
 */
test('dragging a sticker across a cluttered board lands it exactly where dropped — never off-board', async ({ page }) => {
  await loginAndGoToVisionBoard(page)
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  const boardName = `Drag-Fix ${Date.now()}`
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()
  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })

  // Overlapping "clutter" — several elements at the exact same default
  // spawn position, the real condition the bug was first reproduced under
  // (though the root cause turned out to be unrelated to the clutter
  // itself — any STICKER/IMAGE drag was affected).
  for (let i = 0; i < 4; i += 1) {
    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill(`Clutter ${i}`)
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
  }

  await page.getByRole('button', { name: 'Elementos' }).click()
  await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Sticker', exact: true }).click()
  await page.getByRole('radiogroup', { name: 'Sticker' }).getByRole('radio').first().click()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

  const sticker = page.getByRole('button', { name: /^Sticker:/ })
  const before = await sticker.boundingBox()
  if (!before) throw new Error('sticker not found')

  await page.mouse.move(before.x + before.width / 2, before.y + before.height / 2)
  await page.mouse.down()
  for (let i = 1; i <= 8; i += 1) {
    await page.mouse.move(before.x + before.width / 2 + i * 35, before.y + before.height / 2 + i * 25)
  }
  await page.mouse.up()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

  await expect(sticker).toBeVisible()
  const after = await sticker.boundingBox()
  if (!after) throw new Error('sticker not found after drag')
  // Lands exactly at the intended (+280, +200) delta — before the fix this
  // landed near (-155, -270) relative to the board origin (off-board) or,
  // with only the defense-in-depth part of the fix, badly short of the
  // real drop point; a tight tolerance here is deliberate, confirming
  // *every* pointermove was actually delivered and used, not just "didn't
  // go negative."
  expect(Math.abs(after.x - before.x - 280)).toBeLessThan(10)
  expect(Math.abs(after.y - before.y - 200)).toBeLessThan(10)

  // Same repro for IMAGE — the other type the phase names, same fix, same
  // code path (VisionBoardElementView.tsx doesn't special-case type for
  // drag math, only for which `<img>` renders).
  await page.getByRole('button', { name: 'Elementos' }).click()
  await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Imagen', exact: true }).click()
  await page.getByRole('radio', { name: 'URL', exact: true }).click()
  await page.getByLabel('URL de la imagen').fill('https://picsum.photos/200')
  await page.getByRole('button', { name: 'Agregar', exact: true }).click()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

  const image = page.getByRole('button', { name: 'Imagen', exact: true })
  const ibefore = await image.boundingBox()
  if (!ibefore) throw new Error('image not found')
  await page.mouse.move(ibefore.x + ibefore.width / 2, ibefore.y + ibefore.height / 2)
  await page.mouse.down()
  for (let i = 1; i <= 8; i += 1) {
    await page.mouse.move(ibefore.x + ibefore.width / 2 + i * 30, ibefore.y + ibefore.height / 2 + i * 20)
  }
  await page.mouse.up()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

  await expect(image).toBeVisible()
  const iafter = await image.boundingBox()
  if (!iafter) throw new Error('image not found after drag')
  expect(Math.abs(iafter.x - ibefore.x - 240)).toBeLessThan(10)
  expect(Math.abs(iafter.y - ibefore.y - 160)).toBeLessThan(10)
})
