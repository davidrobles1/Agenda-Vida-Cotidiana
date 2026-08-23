import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * BLOQUE A (post-MVP) — board lifecycle: deleting a Vision Board (a real
 * `DELETE /vision-boards/{id}`, always existed on the backend, only ever
 * exercised by its own integration test until now — see api.ts's
 * `deleteVisionBoard` doc comment) and the Editar popover's
 * `motion.create(Popover)` regression (same historical bug already fixed
 * for Templates/Elementos/Exportar in FASE 23 — see
 * VisionBoardElementEditor.tsx's own doc comment).
 */
async function createBoard(page: import('@playwright/test').Page, name: string) {
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(name)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()
  const switcherTrigger = page.getByRole('button', { name: 'Cambiar de Vision Board' })
  await expect(switcherTrigger).toHaveText(name, { timeout: 20_000 })
}

test('deleting the current board switches to another remaining one, without leaving stale references in the Switcher; Editar popover opens fully visible and restores focus on Escape', async ({ page }) => {
  await loginAndGoToVisionBoard(page)

  const boardA = `BloqueA-Uno ${Date.now()}`
  await createBoard(page, boardA)

  // Create a TEXT element, select it, open Editar.
  await page.getByRole('button', { name: 'Elementos' }).click()
  await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
  await page.getByLabel('Contenido').fill('Editar spot check')
  await page.getByRole('button', { name: 'Agregar', exact: true }).click()
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

  const element = page.getByRole('button', { name: /^Texto: Editar spot check/ })
  await element.click()

  const editarTrigger = page.getByRole('button', { name: 'Editar', exact: true })
  await editarTrigger.click()
  const dialog = page.getByRole('dialog', { name: /Editar/ })
  await expect(dialog).toBeVisible()
  await expect(dialog).toHaveCSS('opacity', '1')
  // The real historical bug: dialog present in DOM but invisible. Confirm
  // it's actually interactable, not just "toBeVisible" (which only checks
  // display, not opacity).
  await expect(dialog.getByRole('heading', { name: /Editar/ })).toBeVisible()

  await page.keyboard.press('Escape')
  await expect(dialog).toHaveCount(0)
  await expect(editarTrigger).toBeFocused()

  // Create a second board, then delete the FIRST one (not the current one)
  // to confirm it switches to whatever remains — do it from board A itself
  // (deleting the board currently open).
  const boardB = `BloqueA-Dos ${Date.now()}`
  await createBoard(page, boardB)

  const deleteBoardTrigger = page.getByRole('button', { name: 'Eliminar Vision Board', exact: true })
  await deleteBoardTrigger.click()
  await expect(page.getByRole('heading', { name: 'Eliminar Vision Board' })).toBeVisible()
  await page.getByRole('alertdialog').getByRole('button', { name: 'Eliminar', exact: true }).click()

  // Board B just got deleted (it was the current one) — must switch
  // automatically to *some* remaining board (not stay referencing B, not
  // error out), and B must be gone from the Switcher for good. Doesn't
  // assert *which* board it lands on — sibling specs in this same suite
  // (autosave/export/core-flows) don't clean up their own boards, so more
  // than just A may legitimately exist at this point; asserting "some
  // valid board, never B" is what the feature actually promises.
  const switcherTrigger = page.getByRole('button', { name: 'Cambiar de Vision Board' })
  await expect(switcherTrigger).not.toHaveText(boardB, { timeout: 20_000 })
  await expect(switcherTrigger).not.toHaveText('', { timeout: 20_000 })
  await switcherTrigger.click()
  await expect(page.getByRole('menuitemradio', { name: boardB })).toHaveCount(0)
  await expect(page.getByRole('menuitemradio', { name: boardA })).toBeVisible()
  await page.keyboard.press('Escape')

  // "delete the very last board falls back to the empty state" isn't
  // covered here — this suite's sibling specs don't clean up their own
  // boards (pre-dating BLOQUE A's delete feature), so this account is
  // never reliably down to exactly one board at this point. Verified for
  // real instead as a one-time, isolated run: deleting every board an
  // account has, down to zero, does surface "Aún no tienes ningún Vision
  // Board." — the same check VisionBoardPage.tsx's own empty-state branch
  // already renders for a brand-new account.
})
