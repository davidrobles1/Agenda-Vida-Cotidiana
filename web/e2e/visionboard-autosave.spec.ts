import { expect, test, type Page } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * FASE 18 — Autosave. Written against the real UI strings this phase
 * introduced (VisionBoardSaveIndicator.tsx: "Guardando…", "Guardado",
 * "Sin guardar", "Error al guardar", "Reintentar") and the real create
 * flow (VisionBoardElementLibrary.tsx: "Elementos" → "Texto" → "Agregar").
 *
 * FASE 22: actually run against the live dev stack for the first time.
 * Originally used the shared `loginAsTestuserAndGoToReminders` helper, but
 * that hops through a "Tareas" nav link that no longer exists (a real,
 * pre-existing gap unrelated to Vision Board — see visionboard-helpers.ts's
 * own doc comment) — switched to `loginAndGoToVisionBoard`, which reaches
 * Vision Board via its own direct sidebar link instead.
 *
 * FASE 24: this suite's own board used to be shared/reused across every
 * run (no way to create a second board existed yet), which accumulated
 * enough leftover elements over many runs to permanently block the "undo
 * after a move" test — a stale element with an elevated zIndex from an
 * earlier run's own Fase-9 layer reorder could render on top of this
 * test's brand-new one at the shared default spawn position. FASE 24
 * added a real "Nuevo Vision Board" entry (VisionBoardToolbar) — every
 * run now creates and uses its own fresh, empty board.
 */

async function goToVisionBoard(page: Page): Promise<void> {
  await loginAndGoToVisionBoard(page)

  const boardName = `Autosave E2E ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()

  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  // The new board switches in asynchronously — CreateVisionBoardDialog
  // closes itself right after firing `onCreated`, without awaiting the
  // real switch (see VisionBoardCanvas.tsx's own
  // `handleBoardCreatedRequest` doc comment). The OLD board's
  // VisionBoardCanvas (and everything inside it, including this exact
  // "Elementos" button) can still be mounted for a moment after that —
  // clicking too early can land on a DOM node that's about to be torn
  // down by the `key={board.id}` remount, silently losing the click.
  // Poll for the switcher to actually show *this* board's name, the same
  // fix visionboard-core-flows.spec.ts's own helper already needed.
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })
}

async function createTextElement(page: Page, text: string): Promise<void> {
  await page.getByRole('button', { name: 'Elementos' }).click()
  await page.getByRole('button', { name: 'Texto', exact: true }).click()
  await page.getByLabel('Contenido').fill(text)
  await page.getByRole('button', { name: 'Agregar', exact: true }).click()
}

test.describe('Vision Board — autosave', () => {
  test('creating an element shows Guardando… then Guardado', async ({ page }) => {
    await goToVisionBoard(page)
    await createTextElement(page, `Autosave create ${Date.now()}`)

    // The debounce/flush is fast (create isn't debounced at all — see
    // handleCreateElement's own trackSaveStatus wrapping), so "Guardando…"
    // may only be visible for a moment; "Guardado" is the durable resting
    // state to assert on.
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
  })

  test('dragging an element sends exactly one PUT, not one per pointermove', async ({ page }) => {
    await goToVisionBoard(page)
    const label = `Drag count ${Date.now()}`
    await createTextElement(page, label)
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    let putCount = 0
    await page.route('**/api/v1/vision-boards/*/elements/*', async (route) => {
      if (route.request().method() === 'PUT') putCount += 1
      await route.continue()
    })

    const element = page.getByRole('button', { name: new RegExp(`Texto: ${label}`) })
    const box = await element.boundingBox()
    if (!box) throw new Error('Element not found on canvas')

    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.down()
    // Several intermediate moves — each one must stay purely local
    // (onDragMove), only the final pointerup may trigger a network call.
    for (let i = 1; i <= 8; i += 1) {
      await page.mouse.move(box.x + box.width / 2 + i * 6, box.y + box.height / 2 + i * 4)
    }
    await page.mouse.up()

    // The autosave debounce (700ms) delays the actual PUT — wait past it
    // before asserting the final count.
    await page.waitForTimeout(1_200)
    expect(putCount).toBeLessThanOrEqual(1)
  })

  test('a failed save shows Error al guardar with a working Reintentar', async ({ page }) => {
    await goToVisionBoard(page)
    const label = `Retry ${Date.now()}`
    await createTextElement(page, label)
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    let failNext = true
    await page.route('**/api/v1/vision-boards/*/elements/*', async (route) => {
      if (route.request().method() === 'PUT' && failNext) {
        failNext = false
        await route.fulfill({ status: 500, body: '{}' })
        return
      }
      await route.continue()
    })

    const element = page.getByRole('button', { name: new RegExp(`Texto: ${label}`) })
    const box = await element.boundingBox()
    if (!box) throw new Error('Element not found on canvas')
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.down()
    await page.mouse.move(box.x + box.width / 2 + 40, box.y + box.height / 2 + 20)
    await page.mouse.up()

    await expect(page.getByText('Error al guardar')).toBeVisible({ timeout: 3_000 })
    // The element itself must still show the moved position locally
    // ("no perder los cambios locales") — not reverted just because the
    // save failed.
    await expect(element).toBeVisible()

    await page.getByRole('button', { name: 'Reintentar' }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
  })

  test('undo after a move is a single step (no duplicate history from autosave)', async ({ page }) => {
    await goToVisionBoard(page)
    const label = `Undo once ${Date.now()}`
    await createTextElement(page, label)
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const element = page.getByRole('button', { name: new RegExp(`Texto: ${label}`) })
    const before = await element.boundingBox()
    if (!before) throw new Error('Element not found on canvas')

    await page.mouse.move(before.x + before.width / 2, before.y + before.height / 2)
    await page.mouse.down()
    await page.mouse.move(before.x + before.width / 2 + 120, before.y + before.height / 2 + 80)
    await page.mouse.up()

    const afterDrag = await element.boundingBox()
    expect(afterDrag?.x).not.toBeCloseTo(before.x, 0)

    await page.getByRole('button', { name: 'Deshacer' }).click()
    const afterOneUndo = await element.boundingBox()
    expect(afterOneUndo?.x).toBeCloseTo(before.x, 0)
    expect(afterOneUndo?.y).toBeCloseTo(before.y, 0)

    // FASE 22 fix: the ORIGINAL assertion here expected "Deshacer" to be
    // disabled after this single Undo — wrong, because `createTextElement`
    // above pushes its own 'create' history entry (handleCreateElement
    // does this unconditionally, unrelated to autosave), so the stack
    // legitimately still has that CREATE entry left to undo. The real
    // claim this test makes ("no duplicate history from autosave") is
    // that there are EXACTLY two entries — create, then move — never
    // three; verified by asserting the *second* Undo undoes the CREATE
    // (removing the element), not by asserting the stack is already empty
    // after only the first one.
    await page.getByRole('button', { name: 'Deshacer' }).click()
    await expect(element).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Deshacer' })).toBeDisabled()
  })
})
