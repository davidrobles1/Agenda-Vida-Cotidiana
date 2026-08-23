import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

async function createBoard(page: import('@playwright/test').Page, namePrefix: string): Promise<string> {
  const boardName = `${namePrefix} ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()
  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })
  return boardName
}

test.describe('Vision Board — advanced UX (BLOQUE G)', () => {
  test('zoom persists per board across a switch-away-and-back', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    const boardName = await createBoard(page, 'Zoom-Persist')

    await page.getByRole('button', { name: 'Acercar' }).click()
    await page.getByRole('button', { name: 'Acercar' }).click()
    await expect(page.getByText('120%', { exact: true })).toBeVisible()

    const switcherTrigger = page.getByRole('button', { name: 'Cambiar de Vision Board' })
    await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
    const scratchName = `Zoom-Scratch ${Date.now()}`
    await page.getByLabel('Nombre').fill(scratchName)
    await page.getByRole('button', { name: 'Crear', exact: true }).click()
    await expect(switcherTrigger).toHaveText(scratchName, { timeout: 20_000 })

    await switcherTrigger.click()
    await page.getByRole('menuitemradio', { name: boardName }).click()
    await expect(switcherTrigger).toHaveText(boardName, { timeout: 20_000 })
    await expect(page.getByText('120%', { exact: true })).toBeVisible()
  })

  // BLOQUE G real bug found live: applying "focus AND click on every arrow
  // keypress" to a CREATE-flow "pick and instantly act" picker (this one —
  // picking a shape here creates it immediately and closes the popover)
  // meant arrow-key *browsing* itself created elements on every step. Fixed
  // by splitting into handleRadiogroupKeyDown (commits on arrow — true
  // persisted-value radiogroups, covered by the next test below) vs
  // handleGridKeyDown (focus-only — CREATE-flow pickers, covered here).
  test('radiogroup keyboard (create-flow grid): arrows move focus only, Enter commits', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Grid-Keyboard')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Forma', exact: true }).click()
    const grid = page.getByRole('radiogroup', { name: 'Elegir forma' })
    await expect(grid).toBeVisible()

    // Tab from the "Volver" button should land on exactly one radio
    // (roving tabindex — every other option is tabIndex=-1).
    await page.getByRole('button', { name: 'Volver' }).focus()
    await page.keyboard.press('Tab')
    const firstRadio = page.getByRole('radio', { name: 'Rectángulo', exact: true })
    await expect(firstRadio).toBeFocused()
    await expect(firstRadio).toHaveAttribute('aria-checked', 'false')

    await page.keyboard.press('ArrowRight')
    const circle = page.getByRole('radio', { name: 'Círculo', exact: true })
    await expect(circle).toBeFocused()
    // The whole point of the fix: browsing must never itself create an
    // element or check a radio — only an explicit activation may.
    await expect(circle).toHaveAttribute('aria-checked', 'false')
    // Still on the "Elegir forma" step (own Heading — see
    // VisionBoardElementLibrary.tsx's own doc comment on why the dialog's
    // accessible name switches per step) — arrow browsing hasn't closed it.
    await expect(page.getByRole('dialog', { name: 'Elegir forma' })).toBeVisible()

    await page.keyboard.press('End')
    const last = page.getByRole('radio', { name: 'Divisor decorativo', exact: true })
    await expect(last).toBeFocused()
    await expect(last).toHaveAttribute('aria-checked', 'false')

    await page.keyboard.press('Home')
    await expect(firstRadio).toBeFocused()

    // A real activation (native button behavior: Enter on a focused
    // button fires its click) does create the element and close the popover.
    await page.keyboard.press('Enter')
    await expect(page.getByRole('button', { name: /^Forma: Rectángulo/ })).toBeVisible()
  })

  test('radiogroup keyboard (persisted value): arrows move AND select, Home/End jump', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
    await page.getByLabel('Nombre').fill(`Theme-Keyboard ${Date.now()}`)

    const grid = page.getByRole('radiogroup', { name: 'Tema del Vision Board' })
    await expect(grid).toBeVisible()

    const firstRadio = page.getByRole('radio', { name: 'Claro', exact: true })
    await expect(firstRadio).toHaveAttribute('aria-checked', 'true')
    await firstRadio.focus()

    await page.keyboard.press('ArrowRight')
    const second = page.getByRole('radio', { name: 'Oscuro', exact: true })
    await expect(second).toBeFocused()
    await expect(second).toHaveAttribute('aria-checked', 'true')
    await expect(firstRadio).toHaveAttribute('aria-checked', 'false')

    await page.keyboard.press('ArrowLeft')
    await expect(firstRadio).toBeFocused()
    await expect(firstRadio).toHaveAttribute('aria-checked', 'true')

    await page.keyboard.press('End')
    const last = page.getByRole('radio', { name: 'Energía', exact: true })
    await expect(last).toBeFocused()
    await expect(last).toHaveAttribute('aria-checked', 'true')

    await page.keyboard.press('Home')
    await expect(firstRadio).toBeFocused()
    await expect(firstRadio).toHaveAttribute('aria-checked', 'true')

    // No need to actually create the board for this test.
    await page.getByRole('button', { name: 'Cancelar' }).click()
  })

  test('keyboard nudge: Arrow moves 1px, Shift+Arrow moves 10px, coalesces into one undo entry', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Keyboard-Nudge')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill('NudgeMe')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const element = page.getByRole('button', { name: /^Texto: NudgeMe/ })
    await element.click()
    const before = await element.boundingBox()
    if (!before) throw new Error('not found')

    // A fast burst of 3 fine (1px) moves coalesces into exactly one undo
    // entry. Verified as its own self-contained before/after/undo cycle —
    // NOT chained straight into the Shift+Arrow burst below — because the
    // two bursts use independent 500ms coalescing windows: if the gap
    // between the last plain ArrowRight and the following Shift+ArrowDown
    // ever exceeds 500ms (real timing jitter, not app misbehavior — a
    // genuine pause between two physical movements SHOULD start a new
    // undo entry), they'd land as two separate history entries and a
    // single Deshacer would only revert the second, leaving a false
    // failure that has nothing to do with the feature under test.
    await page.keyboard.press('ArrowRight')
    await page.keyboard.press('ArrowRight')
    await page.keyboard.press('ArrowRight')
    const afterFine = await element.boundingBox()
    if (!afterFine) throw new Error('not found')
    expect(Math.round(afterFine.x - before.x)).toBe(3)

    // Two independent debounces are in flight after a burst: the 500ms
    // history-coalescing one (VisionBoardCanvas.tsx's keyboardMoveTimerRef,
    // purely local — pushes the undo entry) and the SEPARATE 700ms
    // AUTOSAVE_DEBOUNCE_MS one (scheduleElementAutosave, a real PUT with
    // the element's optimistic-lock `version`). Clicking Deshacer too close
    // to that second boundary races Deshacer's own PUT (same base version)
    // against the autosave's — whichever the backend sees second gets
    // rejected as stale, so the loser's local state either fails to revert
    // (undo) or fails to apply. A wait comfortably past both — not just the
    // 500ms one — is what actually avoids the race.
    await page.waitForTimeout(1400)
    await page.getByRole('button', { name: 'Deshacer' }).click()
    const afterFineUndo = await element.boundingBox()
    if (!afterFineUndo) throw new Error('not found')
    expect(Math.round(afterFineUndo.x)).toBe(Math.round(before.x))
    expect(Math.round(afterFineUndo.y)).toBe(Math.round(before.y))

    // Shift+Arrow's larger 10px step, independently coalesced and
    // independently undoable.
    await page.keyboard.press('Shift+ArrowDown')
    const afterLarge = await element.boundingBox()
    if (!afterLarge) throw new Error('not found')
    expect(Math.round(afterLarge.y - before.y)).toBe(10)

    await page.waitForTimeout(1400)
    await page.getByRole('button', { name: 'Deshacer' }).click()
    const afterLargeUndo = await element.boundingBox()
    if (!afterLargeUndo) throw new Error('not found')
    expect(Math.round(afterLargeUndo.x)).toBe(Math.round(before.x))
    expect(Math.round(afterLargeUndo.y)).toBe(Math.round(before.y))
  })

  test('settle animation class applies briefly after a drag ends, never during it', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'Settle-Anim')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill('SettleMe')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const element = page.getByRole('button', { name: /^Texto: SettleMe/ })
    const content = element.locator('[class*="elementContent"]')
    const box = await element.boundingBox()
    if (!box) throw new Error('not found')

    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.down()
    await page.mouse.move(box.x + box.width / 2 + 60, box.y + box.height / 2 + 40)
    // Mid-gesture: never settling.
    await expect(content).not.toHaveClass(/settling/)
    await page.mouse.up()
    // Right after release: settling briefly applied.
    await expect(content).toHaveClass(/settling/)
    // And cleared again shortly after.
    await expect(content).not.toHaveClass(/settling/, { timeout: 2_000 })
  })
})
