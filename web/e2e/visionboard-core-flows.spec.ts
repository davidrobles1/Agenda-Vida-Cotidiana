import { expect, test, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { loginAndGoToVisionBoard } from './visionboard-helpers'

/**
 * FASE 22 — Testing. Covers flows the two existing Vision Board specs
 * (visionboard-autosave.spec.ts, visionboard-export.spec.ts) don't touch:
 * NOTE/STICKER/SHAPE/IMAGE creation, multi-selection + lock enforcement,
 * layers, duplicate, delete, Undo/Redo entry-count discipline across a
 * combo of operations, Templates, Board Themes, and a full create →
 * mutate → reload → verify-persistence → export cycle (the exact flow
 * FASE 22's own spec asks for under "PERSISTENCIA E2E COMPLETA").
 *
 * Unlike every previous Vision Board e2e spec written in this project, this
 * one was actually run against a live dev stack (frontend :5173, backend
 * :8080, Keycloak :8081, all already running) — see the FASE 22 final
 * report for the real pass/fail results, not a "could not verify" note.
 */

/**
 * FASE 22 finding (RESOLVED in FASE 24): VisionBoardPage.tsx used to only
 * render CreateVisionBoardDialog in its empty state — no way to create a
 * second board once testuser already had one, so every test in this file
 * had to reuse the same shared, ever-growing board, which accumulated
 * enough leftover elements across many runs to permanently block 6 tests
 * (documented in the FASE 22/23 reports). FASE 24 added a real "Nuevo
 * Vision Board" entry to VisionBoardToolbar (reusing CreateVisionBoardDialog
 * as-is) — every test now creates its own fresh, isolated, empty board
 * instead of reusing whatever testuser already has. Labels below no longer
 * need the defensive per-run timestamp uniqueness the old version required
 * (a fresh board has nothing else on it), but it's kept anyway — cheap
 * insurance, and it makes failures easier to tell apart across runs.
 */
async function goToVisionBoard(page: Page, namePrefix: string): Promise<string> {
  await loginAndGoToVisionBoard(page)

  const boardName = `${namePrefix} ${Date.now()}`
  await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
  await page.getByLabel('Nombre').fill(boardName)
  await page.getByRole('button', { name: 'Crear', exact: true }).click()

  await expect(page.getByRole('toolbar', { name: 'Herramientas del Vision Board' })).toBeVisible({ timeout: 20_000 })
  // The new board switches in asynchronously (CreateVisionBoardDialog
  // closes itself right after firing `onCreated`, without awaiting the
  // actual switch — see VisionBoardCanvas.tsx's own `handleBoardCreatedRequest`
  // doc comment) — poll for the switcher to actually show this board's
  // name instead of reading it once.
  const switcherTrigger = page.getByRole('button', { name: 'Cambiar de Vision Board' })
  await expect(switcherTrigger).toHaveText(boardName, { timeout: 20_000 })
  return boardName
}

async function waitForSaved(page: Page): Promise<void> {
  await expect(page.getByText('Guardado', { exact: true })).toBeVisible({ timeout: 10_000 })
}

/**
 * FASE 22 finding: every new element spawns at the same fixed default
 * canvas position (confirmed for real — creating two TEXT elements back
 * to back renders the second exactly on top of the first). Not an app bug
 * worth fixing (a single default spot for new elements is a reasonable,
 * common editor convention — Figma/Miro do the same — the user is
 * expected to drag it into place), but it breaks a test creating more
 * than one element and expecting each to be independently clickable.
 *
 * FASE 24 fix: every test now creates and uses its own fresh board (see
 * goToVisionBoard's own doc comment), so the FASE 22 version of this
 * helper's large `Date.now()`-derived offset — needed back when this
 * board was shared and accumulated debris across many runs — is no longer
 * needed, and turned out to actively cause a *different* problem on a
 * clean board: an offset that large can push an element outside the
 * viewport's initially-visible area, and a raw `page.mouse` drag ending
 * near/past that edge can trigger the browser's own native
 * scroll-into-view-while-dragging behavior — which shifts every other
 * on-screen element's `boundingBox()` (viewport-relative) even though
 * nothing about their real canvas-space x/y changed, confirmed for real:
 * a "locked element didn't move" assertion failed by exactly the same
 * margin a mid-gesture scroll would produce, and a controlled single-
 * element repro with no such offset showed the *identical* boundingBox()
 * before/after (proving lock enforcement itself was never the problem).
 * A small, fixed offset is enough to keep two elements visually apart
 * without ever leaving the default viewport.
 */
async function createTextElementApart(page: Page, label: string, index: number): Promise<void> {
  await page.getByRole('button', { name: 'Elementos' }).click()
  await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
  await page.getByLabel('Contenido').fill(label)
  await page.getByRole('button', { name: 'Agregar', exact: true }).click()
  await waitForSaved(page)

  if (index === 0) return
  const element = page.getByRole('button', { name: new RegExp(`^Texto: ${label}`) })
  const box = await element.boundingBox()
  if (!box) throw new Error(`Element "${label}" not found right after creation`)
  const offset = index * 140
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.mouse.down()
  await page.mouse.move(box.x + box.width / 2 + offset, box.y + box.height / 2 + offset)
  await page.mouse.up()
  await waitForSaved(page)
}

test.describe('Vision Board — core flows (real run)', () => {
  test('multi-selection, lock enforcement, and duplicate', async ({ page }) => {
    await goToVisionBoard(page, 'MultiSelect')
    // Unique per run — the board may already carry leftovers from a
    // previous run of this same spec (see goToVisionBoard's own doc
    // comment on why a fresh board isn't guaranteed), so a literal label
    // like "Uno" alone could match more than the 2 elements this test
    // itself creates.
    const runId = Date.now()
    const uno = `Uno ${runId}`
    const dos = `Dos ${runId}`

    // Two TEXT elements so there's something real to multi-select — moved
    // apart right after creation, see createTextElementApart's own doc
    // comment on why (both would otherwise spawn stacked exactly on top
    // of each other).
    await createTextElementApart(page, uno, 0)
    await createTextElementApart(page, dos, 1)

    const first = page.getByRole('button', { name: new RegExp(`^Texto: ${uno}`) })
    const second = page.getByRole('button', { name: new RegExp(`^Texto: ${dos}`) })
    await expect(first).toBeVisible()
    await expect(second).toBeVisible()

    await first.click()
    await expect(first).toHaveAttribute('aria-pressed', 'true')
    await second.click({ modifiers: ['Shift'] })
    await expect(second).toHaveAttribute('aria-pressed', 'true')
    await expect(first).toHaveAttribute('aria-pressed', 'true')

    // Lock both via the toolbar's single toggle.
    await page.getByRole('button', { name: 'Bloquear', exact: true }).click()
    await waitForSaved(page)
    await expect(first).toHaveAttribute('aria-label', /bloqueado/)
    await expect(second).toHaveAttribute('aria-label', /bloqueado/)
    // Button label flips once the whole selection is locked.
    await expect(page.getByRole('button', { name: 'Desbloquear', exact: true })).toBeVisible()

    // A locked element must not move: attempt a drag on the first one.
    // Offset kept small and *inside* the element's own box on purpose —
    // since a locked element never actually moves, the cursor ends up
    // exactly where this delta says (unlike an unlocked drag, where the
    // element following the cursor keeps it "inside"); a large offset
    // (confirmed for real) can land the final `mouseup` past the
    // element's edge, on the empty canvas behind it, which clears the
    // whole selection as a side effect — a bug in this simulated gesture,
    // not in the app's real (mouse-follows-element) drag.
    const box = await first.boundingBox()
    if (!box) throw new Error('Element not found')
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.down()
    await page.mouse.move(box.x + box.width / 2 + 15, box.y + box.height / 2 + 10)
    await page.mouse.up()
    const boxAfter = await first.boundingBox()
    expect(boxAfter?.x).toBeCloseTo(box.x, 0)
    expect(boxAfter?.y).toBeCloseTo(box.y, 0)

    // Unlock, then duplicate the still-selected pair.
    await page.getByRole('button', { name: 'Desbloquear', exact: true }).click()
    await waitForSaved(page)
    await page.getByRole('button', { name: 'Duplicar', exact: true }).click()
    await waitForSaved(page)
    await expect(page.getByRole('button', { name: new RegExp(`^Texto: ${uno}`) })).toHaveCount(2)
    await expect(page.getByRole('button', { name: new RegExp(`^Texto: ${dos}`) })).toHaveCount(2)
  })

  test('Undo/Redo entry-count discipline across a combo', async ({ page }) => {
    await goToVisionBoard(page, 'UndoRedo')
    const combo = `Combo ${Date.now()}`

    await page.getByRole('button', { name: 'Elementos' }).click()
    await page.getByRole('dialog', { name: 'Elementos' }).getByRole('button', { name: 'Texto', exact: true }).click()
    await page.getByLabel('Contenido').fill(combo)
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await waitForSaved(page)

    const element = page.getByRole('button', { name: new RegExp(`^Texto: ${combo}`) })
    const before = await element.boundingBox()
    if (!before) throw new Error('Element not found')

    // MOVE — a large, `Date.now()`-derived offset (not a small fixed one),
    // same reasoning as createTextElementApart's own doc comment: this
    // shared, never-cleaned board has accumulated debris at small fixed
    // offsets from repeated past runs too, some of it with an elevated
    // zIndex a fresh element can't render above.
    const moveX = 300 + (Date.now() % 800)
    const moveY = 200 + ((Date.now() * 3) % 600)
    await page.mouse.move(before.x + before.width / 2, before.y + before.height / 2)
    await page.mouse.down()
    await page.mouse.move(before.x + before.width / 2 + moveX, before.y + before.height / 2 + moveY)
    await page.mouse.up()
    await waitForSaved(page)

    // DUPLICATE (selection is still the moved element)
    await page.getByRole('button', { name: 'Duplicar', exact: true }).click()
    await waitForSaved(page)
    await expect(page.getByRole('button', { name: new RegExp(`^Texto: ${combo}`) })).toHaveCount(2)

    const undo = page.getByRole('button', { name: 'Deshacer' })
    const redo = page.getByRole('button', { name: 'Rehacer' })

    // One Undo must undo ONLY the duplicate (one logical operation = one entry).
    await undo.click()
    await expect(page.getByRole('button', { name: new RegExp(`^Texto: ${combo}`) })).toHaveCount(1)

    // A second Undo must undo ONLY the move, landing back at the original position.
    await undo.click()
    const afterTwoUndos = await element.boundingBox()
    expect(afterTwoUndos?.x).toBeCloseTo(before.x, 0)
    expect(afterTwoUndos?.y).toBeCloseTo(before.y, 0)

    // Redo replays the move, then the duplicate, in the same order.
    await redo.click()
    const afterRedoMove = await element.boundingBox()
    expect(afterRedoMove?.x).not.toBeCloseTo(before.x, 0)
    await redo.click()
    await expect(page.getByRole('button', { name: new RegExp(`^Texto: ${combo}`) })).toHaveCount(2)
    await expect(redo).toBeDisabled()
  })

  test('layers: front/back/raise/lower change paint order', async ({ page }) => {
    await goToVisionBoard(page, 'Layers')
    const runId = Date.now()
    const labelA = `LayerA ${runId}`
    const labelB = `LayerB ${runId}`

    await createTextElementApart(page, labelA, 0)
    await createTextElementApart(page, labelB, 1)
    const a = page.getByRole('button', { name: new RegExp(`^Texto: ${labelA}`) })
    const b = page.getByRole('button', { name: new RegExp(`^Texto: ${labelB}`) })
    const zIndexOf = async (locator: typeof a) =>
      Number(await locator.evaluate((el) => window.getComputedStyle(el).zIndex))

    // FASE 22 finding: every new element is created with zIndex: 0
    // (VisionBoardElement's backend constructor never increments it — see
    // the Fase 21 audit) — A and B are BOTH zIndex 0 at this point, so
    // there's nothing meaningful to assert about their raw CSS zIndex yet.
    // "B renders on top by default" is real (confirmed by code, Fase 21),
    // but it's DOM order deciding equal-zIndex stacking, not the zIndex
    // value itself — verified here via compareDocumentPosition instead of
    // (wrongly) expecting zIndexOf(b) > zIndexOf(a).
    const bIsAfterAInDom = await a.evaluate(
      (elA, elB) => !!(elA.compareDocumentPosition(elB as Node) & Node.DOCUMENT_POSITION_FOLLOWING),
      await b.elementHandle(),
    )
    expect(bIsAfterAInDom).toBe(true)

    // Send B to back — the backend renumbers both to real, distinct
    // zIndex values (Fase 9's reorder), so *this* comparison is meaningful.
    // FASE 24 fix: `waitForSaved` alone raced with this — "Guardado" was
    // already showing (from creating A/B moments earlier), so that
    // assertion could pass *before* the reorder's own request-response
    // cycle even finished, reading a stale zIndex. Confirmed for real via
    // a direct network-response check (POST .../reorder correctly
    // returned zIndex 1/0) — the backend/frontend reorder logic itself
    // was never the problem. Waiting for the specific reorder response
    // removes the race without weakening what's actually asserted.
    await b.click()
    const [reorderResponse] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/reorder') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Enviar al fondo' }).click(),
    ])
    expect(reorderResponse.ok()).toBe(true)
    await waitForSaved(page)
    expect(await zIndexOf(a)).toBeGreaterThan(await zIndexOf(b))
  })

  test('delete: confirmation dialog, cancel, then real delete with Undo', async ({ page }) => {
    await goToVisionBoard(page, 'Delete')
    const borrame = `Borrame ${Date.now()}`
    // Moved off the shared default spawn position defensively — see
    // createTextElementApart's own doc comment: any element left there by
    // an earlier test/run (this board is reused, never fresh) could end
    // up rendering on top and stealing this click.
    await createTextElementApart(page, borrame, 0)

    const element = page.getByRole('button', { name: new RegExp(`^Texto: ${borrame}`) })
    await element.click()

    // Cancel must NOT delete.
    await page.getByRole('button', { name: 'Eliminar', exact: true }).first().click()
    await expect(page.getByRole('heading', { name: 'Eliminar elemento' })).toBeVisible()
    await page.getByRole('button', { name: 'Cancelar' }).click()
    await expect(element).toBeVisible()

    // Confirm deletes for real.
    await page.getByRole('button', { name: 'Eliminar', exact: true }).first().click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Eliminar', exact: true }).click()
    await expect(element).toHaveCount(0)
    await waitForSaved(page)

    // Undo restores it.
    await page.getByRole('button', { name: 'Deshacer' }).click()
    await expect(page.getByRole('button', { name: new RegExp(`^Texto: ${borrame}`) })).toBeVisible()
  })

  test('full persistence cycle: create, mutate, reload, verify, export', async ({ page }) => {
    const boardName = await goToVisionBoard(page, 'Persistencia')

    // FASE 22 finding: some pre-existing STICKER/IMAGE elements on this
    // (reused, never-fresh) board have generic fallback accessible names —
    // "Sticker" when their stickerId doesn't resolve, "Imagen" always
    // (VisionBoardElementView's elementLabel) — identical, `exact: true`
    // and all, to the Element Library's own tile buttons. Scoping every
    // tile click to the "Elementos" dialog itself (only open, unambiguous
    // context at click time) avoids that collision entirely.
    const elementsDialog = page.getByRole('dialog', { name: 'Elementos' })

    // NOTE
    await page.getByRole('button', { name: 'Elementos' }).click()
    await elementsDialog.getByRole('button', { name: 'Nota', exact: true }).click()
    await page.getByLabel('Contenido').fill('Comprar materiales')
    await page.getByRole('button', { name: 'Agregar', exact: true }).click()
    await waitForSaved(page)

    // SHAPE (rectangle, the default variant)
    await page.getByRole('button', { name: 'Elementos' }).click()
    await elementsDialog.getByRole('button', { name: 'Forma', exact: true }).click()
    // BLOQUE C (post-MVP): the shape-variant picker moved to a real ARIA
    // radiogroup (VisionBoardShapePicker) as the catalog grew from 3 to
    // ~30 — was a plain `role="button"` grid before.
    await page.getByRole('radio', { name: 'Rectángulo', exact: true }).click()
    await waitForSaved(page)

    // STICKER
    await page.getByRole('button', { name: 'Elementos' }).click()
    await elementsDialog.getByRole('button', { name: 'Sticker', exact: true }).click()
    await page.getByRole('radiogroup', { name: 'Sticker' }).getByRole('radio').first().click()
    await waitForSaved(page)

    // Board Theme change. FASE 24 finding: changing the theme used to wipe
    // every element from the canvas (a real app bug — see
    // VisionBoardCanvas.tsx's own `handleThemeChange` doc comment — the PUT
    // response has no `elements`, and passing it straight through re-fired
    // the elements-sync effect with `undefined`). This is the regression
    // check for that fix: elements must still be present and countable
    // right after the theme change, not just after a later reload/switch.
    await page.getByRole('button', { name: 'Cambiar tema del Vision Board' }).click()
    await page.getByRole('menuitemradio', { name: /Natural/ }).click()
    await waitForSaved(page)

    // Explicit non-zero checks (not just "same count before/after switch"
    // below) — a weakened version of this test could pass at 0/0/0 if the
    // theme-change wipe bug ever regressed, since "before" and "after"
    // would still match each other.
    const noteCountBefore = await page.getByRole('button', { name: /^Nota:/ }).count()
    const shapeCountBefore = await page.getByRole('button', { name: /^Forma:/ }).count()
    const stickerCountBefore = await page.getByRole('button', { name: /^Sticker:/ }).count()
    expect(noteCountBefore).toBe(1)
    expect(shapeCountBefore).toBe(1)
    expect(stickerCountBefore).toBe(1)

    // FASE 24 fix: `page.reload()` doesn't reliably return to an
    // authenticated Vision Board view in this test environment — confirmed
    // for real (a full hard reload lands back on `/`, a session/routing
    // characteristic unrelated to Vision Board's own persistence, already
    // documented in the Fase 22 report). This checks the *same real thing*
    // ("did this data actually reach the backend, not just live in local
    // React state") a different, equally real way: switching to another
    // board and back both go through the *actual* `GET /vision-boards/{id}`
    // fetch (VisionBoardPage.tsx's `handleSwitchBoard`) and a full
    // `key={board.id}` remount — nothing here reads from any local/cached
    // copy of this board's data.
    // "Nuevo Vision Board" is its own separate toolbar trigger, unrelated
    // to the Switcher's own dropdown — no need to open that first.
    const switcherTrigger = page.getByRole('button', { name: 'Cambiar de Vision Board' })
    await page.getByRole('button', { name: 'Nuevo Vision Board' }).click()
    const scratchBoardName = `Scratch ${Date.now()}`
    await page.getByLabel('Nombre').fill(scratchBoardName)
    await page.getByRole('button', { name: 'Crear', exact: true }).click()
    await expect(switcherTrigger).toHaveText(scratchBoardName, { timeout: 20_000 })

    await switcherTrigger.click()
    await page.getByRole('menuitemradio', { name: boardName }).click()
    await expect(switcherTrigger).toHaveText(boardName, { timeout: 20_000 })

    await expect(page.getByRole('button', { name: /^Nota:/ })).toHaveCount(noteCountBefore)
    await expect(page.getByRole('button', { name: /^Forma:/ })).toHaveCount(shapeCountBefore)
    await expect(page.getByRole('button', { name: /^Sticker:/ })).toHaveCount(stickerCountBefore)
    // Theme persisted too — the switcher's *visible text* shows the active
    // theme's name (its accessible name is a fixed aria-label, "Cambiar
    // tema del Vision Board", so matching by accessible name here would
    // never see "Natural" — this checks rendered text instead).
    await expect(page.getByRole('button', { name: 'Cambiar tema del Vision Board' })).toHaveText(/Natural/)

    // Export still works after a reload + real persisted data.
    await page.getByRole('button', { name: 'Exportar', exact: true }).click()
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'PNG', exact: true }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/\.png$/)
  })

  // FASE 22: no existing spec ever ran an axe-core scan against Vision
  // Board (accessibility.spec.ts's own screen list stops at Notificaciones/
  // Calendario general) despite this being the single most
  // accessibility-audited feature in the app (Fase 20). Reuses the same
  // `@axe-core/playwright` + WCAG 2.1 A/AA tag pattern that file already
  // established — no new dependency.
  //
  // Excludes the shared `<aside>` sidebar and `<header>` mode-switcher
  // (AppShell.tsx) — both have their own real, pre-existing contrast
  // violations (confirmed via a live run: `.modePill`/`.topBar` text at
  // 4.04:1, needs 4.5:1) that belong to AppShell/core theming, not Vision
  // Board. Scanning them here would make this test fail for something
  // outside this feature's own code — exactly what FASE 22's own "no
  // modifiques código ajeno únicamente para conseguir una suite verde"
  // rule warns against. `exclude` (not `include('main')`) on purpose:
  // React Aria's Popover/Modal portal as siblings of the app root, not
  // necessarily inside `<main>` — excluding the two known-bad landmarks
  // still lets every scan pick up wherever an overlay actually renders.
  test('Vision Board canvas and Element Library popover have no real WCAG 2.1 A/AA violations', async ({ page }) => {
    await goToVisionBoard(page, 'A11yScan')

    const canvasResults = await new AxeBuilder({ page })
      .exclude('aside')
      .exclude('header')
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze()
    expect(canvasResults.violations, JSON.stringify(canvasResults.violations, null, 2)).toEqual([])

    await page.getByRole('button', { name: 'Elementos' }).click()
    const libraryDialog = page.getByRole('dialog', { name: 'Elementos' })
    await expect(libraryDialog).toBeVisible()
    // FASE 23 fix: `toBeVisible()` only requires the dialog to be
    // *displayed*, not fully opaque — it can resolve true mid-fade, while
    // the popover's own `MotionDialog` is still animating in from
    // `initial={{ opacity: 0 }}`. Scanning color-contrast against
    // partially-transparent text produces meaningless low-contrast
    // readings that have nothing to do with the real, settled colors (the
    // ones already verified in the Fase 20/22 audits). Wait for the
    // opacity transition to actually finish before analyzing.
    await expect(libraryDialog).toHaveCSS('opacity', '1')
    const libraryResults = await new AxeBuilder({ page })
      .exclude('aside')
      .exclude('header')
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze()
    expect(libraryResults.violations, JSON.stringify(libraryResults.violations, null, 2)).toEqual([])
  })
})
