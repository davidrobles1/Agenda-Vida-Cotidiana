/**
 * BLOQUE G (post-MVP) — proper ARIA APG radiogroup keyboard pattern
 * (https://www.w3.org/WAI/ARIA/apg/patterns/radio/), applied uniformly to
 * every `role="radiogroup"` this feature has grown (Board Theme,
 * SHAPE/sticker/emoji pickers, the image/sticker "source" toggles): Arrow
 * Right/Down moves to the next option, Arrow Left/Up to the previous one
 * (wrapping), Home/End jump to the first/last — and roving tabindex
 * (`radioTabIndex` below) is what keeps Tab moving in/out of the whole
 * group in *one* stop instead of visiting every option individually (the
 * exact gap FASE 24's own report flagged: "Tab recorre cada opción
 * individualmente... no una parada, per ARIA APG").
 *
 * One shared implementation instead of six near-identical copies — every
 * radiogroup container just adds `onKeyDown={handleRadiogroupKeyDown}`
 * (zero other wiring: it works by focusing *and clicking* the target
 * option, reusing whatever onClick handler that button already has,
 * whatever catalog/state it belongs to) and gives each of its own
 * `role="radio"` buttons a `tabIndex` from `radioTabIndex` below.
 */
/** Shared by both exported handlers below — finds the next/previous/
    first/last option for a `role="radio"` (or, for the grid-only variant,
    plain button) container, or `null` if the pressed key isn't one this
    pattern handles. */
function resolveNextTarget(event: React.KeyboardEvent<HTMLElement>, selector: string): HTMLButtonElement | null {
  const key = event.key
  if (key !== 'ArrowRight' && key !== 'ArrowDown' && key !== 'ArrowLeft' && key !== 'ArrowUp' && key !== 'Home' && key !== 'End') {
    return null
  }
  const options = Array.from(event.currentTarget.querySelectorAll<HTMLButtonElement>(selector))
  if (options.length === 0) return null
  const currentIndex = options.indexOf(document.activeElement as HTMLButtonElement)

  let nextIndex: number
  if (key === 'Home') {
    nextIndex = 0
  } else if (key === 'End') {
    nextIndex = options.length - 1
  } else if (key === 'ArrowRight' || key === 'ArrowDown') {
    nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % options.length
  } else {
    nextIndex = currentIndex < 0 ? options.length - 1 : (currentIndex - 1 + options.length) % options.length
  }
  event.preventDefault()
  return options[nextIndex]
}

/** For a *real* radiogroup — one whose `value` is a genuine setting
    (Board Theme, the image/sticker "source" toggles, and a SHAPE/sticker/
    emoji picker's own Editar step, where picking one only stages a draft
    until Guardar). Arrow keys both move focus *and* select, same as a
    native `<input type="radio">` group — nothing to separately "commit,"
    since nothing here is an irreversible action by itself. */
export function handleRadiogroupKeyDown(event: React.KeyboardEvent<HTMLElement>): void {
  const next = resolveNextTarget(event, '[role="radio"]:not(:disabled)')
  if (!next) return
  next.focus()
  // Reuses whatever onClick this specific radio already has — selecting
  // via keyboard and via mouse are the same action everywhere in this
  // feature, never two separate code paths.
  next.click()
}

/** BLOQUE G fix (real functional bug, caught by this feature's own live
    testing, not assumed): SHAPE/sticker/emoji *creation* — VisionBoardElementLibrary.tsx's
    ShapeFields/StickerFields/EmojiFields — isn't a settable value at all;
    picking one creates a real board element and closes the popover
    immediately (the exact same "pick and go" shape Sticker's own create
    step already had). `role="radio"`/`role="radiogroup"` was reused here
    anyway for visual/structural consistency with the real radiogroups
    above, but arrow-key browsing must never itself perform that
    immediate, irreversible action on every step it passes through —
    confirmed live: it does, and the popover closes and a real element
    gets created just from arrowing past it. This variant only moves
    focus; the actual pick still happens on Enter/Space (native `<button>`
    behavior, nothing extra to wire) or a real click, exactly once, on
    the option the user actually stops on. */
export function handleGridKeyDown(event: React.KeyboardEvent<HTMLElement>): void {
  const next = resolveNextTarget(event, '[role="radio"]:not(:disabled)')
  if (!next) return
  next.focus()
}

/** `checked` wins outright; otherwise only `isFirst` gets `0` (and only
    when nothing in the group is checked yet — a freshly-opened "pick one"
    step, e.g. Forma/Emoji at creation time, where nothing is selected
    until the user picks). Every other option is `-1` — out of the Tab
    order, reachable only via the arrow keys above, which is the whole
    point of roving tabindex. */
export function radioTabIndex(checked: boolean, isFirst: boolean, anyChecked: boolean): 0 | -1 {
  if (checked) return 0
  if (!anyChecked && isFirst) return 0
  return -1
}
