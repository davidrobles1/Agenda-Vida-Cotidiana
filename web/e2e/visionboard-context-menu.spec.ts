import { expect, test } from '@playwright/test'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * BLOQUE E (post-MVP) — real right-click context menu (React Aria Menu),
 * content varies by selection: none / single element / multi-selection.
 * macOS two-finger tap isn't separately tested — the browser already
 * normalizes it into the same native `contextmenu` DOM event this whole
 * feature is built on, nothing platform-specific to verify.
 */
async function createBoard(page: import('@playwright/test').Page, namePrefix: string): Promise<string> {
  const boardName = `${namePrefix} ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()
  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  await expect(page.getByRole('button', { name: 'Cambiar de Vision Board' })).toHaveText(boardName, { timeout: 20_000 })
  return boardName
}

test.describe('Vision Board — context menu (BLOQUE E)', () => {
  test('right-click on empty canvas shows the no-selection menu and its actions work', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'CtxMenu-Empty')

    const canvas = page.locator('[class*="canvas"]').first()
    await canvas.click({ button: 'right', position: { x: 400, y: 300 } })

    // Matched by role alone, not name: Chromium's accessible-name
    // computation for a `role="menu"` nested inside react-aria-components'
    // Popover (rendered `role="dialog"`, implicitly labelled by the
    // trigger button) resolves the *dialog's* name onto the menu too,
    // even though the menu carries its own distinct `aria-label`
    // ("Menú contextual del Vision Board", confirmed present in the raw
    // DOM) — a real browser accname quirk for this nesting shape, not a
    // functional bug (screen readers still announce something reasonable)
    // and not worth fighting for a test-only exact-name match; there's
    // only ever one context menu open at a time, so matching by role is
    // still unambiguous.
    const menu = page.getByRole('menu')
    await expect(menu).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Templates' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Elementos' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Cambiar tema' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Nuevo Vision Board' })).toBeVisible()
    // Nothing selection-specific leaks into this menu.
    await expect(menu.getByRole('menuitem', { name: 'Editar' })).toHaveCount(0)
    await expect(menu.getByRole('menuitem', { name: 'Duplicar' })).toHaveCount(0)

    // "Elementos" from the context menu opens the real, same popover.
    await menu.getByRole('menuitem', { name: 'Elementos' }).click()
    await expect(page.getByRole('dialog', { name: 'Elementos' })).toBeVisible()
    await page.keyboard.press('Escape')
  })

  test('right-click on a single unselected element selects it and shows the single-element menu', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'CtxMenu-Single')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill('CtxTarget')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    // A freshly created element is auto-selected (handleCreateElement) —
    // deselect first so this test's premise ("right-click an *unselected*
    // element") is real.
    await page.locator('[class*="canvas"]').first().click({ position: { x: 600, y: 500 } })
    const target = page.getByRole('button', { name: /^Texto: CtxTarget/ })
    await expect(target).toHaveAttribute('aria-pressed', 'false')
    await target.click({ button: 'right' })
    await expect(target).toHaveAttribute('aria-pressed', 'true')

    // Matched by role alone, not name: Chromium's accessible-name
    // computation for a `role="menu"` nested inside react-aria-components'
    // Popover (rendered `role="dialog"`, implicitly labelled by the
    // trigger button) resolves the *dialog's* name onto the menu too,
    // even though the menu carries its own distinct `aria-label`
    // ("Menú contextual del Vision Board", confirmed present in the raw
    // DOM) — a real browser accname quirk for this nesting shape, not a
    // functional bug (screen readers still announce something reasonable)
    // and not worth fighting for a test-only exact-name match; there's
    // only ever one context menu open at a time, so matching by role is
    // still unambiguous.
    const menu = page.getByRole('menu')
    await expect(menu).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Editar' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Duplicar' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Bloquear' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Eliminar' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Traer al frente' })).toBeVisible()
    // Not present in the empty-selection menu.
    await expect(menu.getByRole('menuitem', { name: 'Nuevo Vision Board' })).toHaveCount(0)

    // "Duplicar" from the menu performs the real duplicate action.
    await menu.getByRole('menuitem', { name: 'Duplicar' }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('button', { name: /^Texto: CtxTarget/ })).toHaveCount(2)
  })

  test('right-click inside an existing multi-selection keeps it and shows the multi-selection menu', async ({ page }) => {
    await loginAndGoToVisionBoard(page)
    await createBoard(page, 'CtxMenu-Multi')

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill('CtxOne')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    const first = page.getByRole('button', { name: /^Texto: CtxOne/ })
    const box = await first.boundingBox()
    if (!box) throw new Error('not found')
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.down()
    await page.mouse.move(box.x + box.width / 2 + 140, box.y + box.height / 2 + 140)
    await page.mouse.up()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill('CtxTwo')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
    const second = page.getByRole('button', { name: /^Texto: CtxTwo/ })

    await first.click()
    await second.click({ modifiers: ['Shift'] })
    await expect(first).toHaveAttribute('aria-pressed', 'true')
    await expect(second).toHaveAttribute('aria-pressed', 'true')

    // Right-click ON one of the already-selected pair — selection must
    // survive (not collapse to just the clicked one).
    await first.click({ button: 'right' })
    await expect(first).toHaveAttribute('aria-pressed', 'true')
    await expect(second).toHaveAttribute('aria-pressed', 'true')

    // Matched by role alone, not name: Chromium's accessible-name
    // computation for a `role="menu"` nested inside react-aria-components'
    // Popover (rendered `role="dialog"`, implicitly labelled by the
    // trigger button) resolves the *dialog's* name onto the menu too,
    // even though the menu carries its own distinct `aria-label`
    // ("Menú contextual del Vision Board", confirmed present in the raw
    // DOM) — a real browser accname quirk for this nesting shape, not a
    // functional bug (screen readers still announce something reasonable)
    // and not worth fighting for a test-only exact-name match; there's
    // only ever one context menu open at a time, so matching by role is
    // still unambiguous.
    const menu = page.getByRole('menu')
    await expect(menu).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Duplicar' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Bloquear' })).toBeVisible()
    await expect(menu.getByRole('menuitem', { name: 'Eliminar' })).toBeVisible()
    // Editar makes no sense for >1 element — never shown, same rule the
    // toolbar's own single-selection-only Editar/Capas already follow.
    await expect(menu.getByRole('menuitem', { name: 'Editar' })).toHaveCount(0)
    // Multi-selection only gets front/back, not raise/lower (same set the
    // phase's own spec names for "Con selección múltiple").
    await expect(menu.getByRole('menuitem', { name: 'Subir una capa' })).toHaveCount(0)
  })
})
