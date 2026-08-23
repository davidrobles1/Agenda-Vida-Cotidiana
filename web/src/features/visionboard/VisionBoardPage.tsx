import { useEffect, useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { createVisionBoard, getVisionBoard, listVisionBoards, type VisionBoard } from './api'
import { CreateVisionBoardDialog } from './CreateVisionBoardDialog'
import { VisionBoardCanvas } from './VisionBoardCanvas'
import styles from './VisionBoardPage.module.css'

/**
 * FASE 3: real backend data only (GET /vision-boards, GET /vision-boards/{id},
 * POST /vision-boards) — no mock, no localStorage.
 *
 * FASE 17 fix (was a FASE 3 "out of this phase's scope" pendiente): this
 * now owns the *full* board list (`boards`, from `listVisionBoards` — that
 * call already existed, only its result was being discarded down to
 * `items[0]`), not just the first board, so VisionBoardCanvas's own board
 * switcher has something real to list and switch between. Still no
 * mock/localStorage — `handleSwitchBoard` always re-fetches the target via
 * the real `GET /vision-boards/{id}` (the list response omits `elements`,
 * same reason `handleCreated` already re-fetches after create).
 *
 * `key={board.id}` on `VisionBoardCanvas` is what actually satisfies "no
 * mezclar elementos entre boards / no reutilizar selectedIds / reiniciar
 * Undo/Redo al cambiar de board": remounting the whole canvas component on
 * every board switch resets *every* piece of its local state (selection,
 * history stacks, zoom, Smart Guides, saving flags) at once, by
 * construction — no manual reset code to keep in sync as that local state
 * keeps growing phase over phase.
 */
export function VisionBoardPage() {
  const [boards, setBoards] = useState<VisionBoard[]>([])
  const [board, setBoard] = useState<VisionBoard | null>(null)
  const [loading, setLoading] = useState(true)
  const [switchingBoard, setSwitchingBoard] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const page = await listVisionBoards()
        if (cancelled) return
        setBoards(page.items)
        if (page.items.length > 0) {
          const full = await getVisionBoard(page.items[0].id)
          if (!cancelled) setBoard(full)
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'No se pudo cargar el Vision Board.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  async function handleCreated(created: VisionBoard) {
    // GET (not the create response) so `elements` is populated per the
    // real contract — POST's response never includes it (VisionBoardResponse.java).
    const full = await getVisionBoard(created.id)
    setBoards((current) => [...current, full])
    setBoard(full)
  }

  /** BLOQUE A: VisionBoardCanvas already deleted the board on the backend
      by the time this runs (see its own `handleDeleteBoard`) — this only
      updates what's shown next: the next available board (re-fetched via
      GET, same "list omits elements" reasoning `handleCreated` already
      follows) if any remain, or the empty state (same as the very first
      load) if that was the last one. Removing it from `boards` first is
      what keeps the Switcher from ever listing the just-deleted board. */
  async function handleBoardDeleted(deletedId: string) {
    const remaining = boards.filter((b) => b.id !== deletedId)
    setBoards(remaining)
    if (remaining.length === 0) {
      setBoard(null)
      return
    }
    const full = await getVisionBoard(remaining[0].id)
    setBoard(full)
  }

  /** FASE 17: switching keeps the *current* board on screen until the new
      one is actually loaded (no flash to an empty/loading canvas) —
      `switchingBoard` only gates the switcher's own UI (disables it, shows
      a brief "Cargando…" state) while the fetch is in flight. */
  async function handleSwitchBoard(id: string) {
    if (!board || id === board.id || switchingBoard) return
    setSwitchingBoard(true)
    setError(null)
    try {
      const full = await getVisionBoard(id)
      setBoard(full)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cambiar de Vision Board.')
    } finally {
      setSwitchingBoard(false)
    }
  }

  return (
    <AppShell title="Vision Board" subtitle={board?.name}>
      {loading && <p>Cargando…</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && !board && (
        <div className={styles.empty}>
          <p>Aún no tienes ningún Vision Board.</p>
          <CreateVisionBoardDialog onCreate={createVisionBoard} onCreated={handleCreated} />
        </div>
      )}

      {!loading && !error && board && (
        <VisionBoardCanvas
          key={board.id}
          board={board}
          boards={boards}
          switchingBoard={switchingBoard}
          onBoardChange={setBoard}
          onSwitchBoard={handleSwitchBoard}
          // FASE 24: the real gap wasn't a missing feature — `handleCreated`
          // and `CreateVisionBoardDialog` already existed and already did
          // exactly this (add to `boards`, switch to it); they just were
          // never reachable once a board already existed. Reused as-is.
          onBoardCreated={handleCreated}
          onBoardDeleted={handleBoardDeleted}
        />
      )}
    </AppShell>
  )
}
