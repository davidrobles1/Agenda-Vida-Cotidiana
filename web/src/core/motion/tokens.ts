import type { Transition } from 'motion/react'

/**
 * UX-011 Motion Design System — named duration/easing/spring categories so
 * motion stays consistent app-wide instead of ad-hoc per-component choices.
 * See design-system.md §10. Every entry exists because a real interaction
 * in the app needs it; do not add speculative categories.
 *
 * Fixed rules (apply at every call site, not encoded in the types below):
 * - hover/focus/press micro-feedback stays plain CSS transitions, never
 *   Motion — cheaper, and already correct in every `.module.css` file.
 * - an exit should read as faster than its matching entrance: pass a
 *   stiffer/shorter transition to `exit` than to `animate` at the call
 *   site (e.g. `{ ...motionTokens.spatial, stiffness: 320 }`).
 * - every non-decorative animation must respect `prefers-reduced-motion`;
 *   `<MotionConfig reducedMotion="user">` at the app root (main.tsx)
 *   handles this globally, so components don't check it individually.
 */
export const motionTokens = {
  /** Tap/press feedback, checkbox toggles, small state flips. */
  quick: { duration: 0.15, ease: 'easeOut' } satisfies Transition,
  /** Content enter/exit, list items appearing, panel cross-fade. */
  base: { type: 'spring', stiffness: 380, damping: 32 } satisfies Transition,
  /** Dialog/Popover open-close, day-panel swap. */
  smooth: { type: 'spring', stiffness: 300, damping: 30 } satisfies Transition,
  /** The one deliberately-longest category (~300-320ms): month change,
      swipe/snap-back — anything that must read as real spatial
      displacement, not a generic fade. */
  spatial: { type: 'spring', stiffness: 260, damping: 28 } satisfies Transition,
  /** Task-complete / success confirmation pop. */
  feedbackSuccess: { type: 'spring', stiffness: 500, damping: 25 } satisfies Transition,
  /** Validation-error shake — the one non-spring category. */
  feedbackError: { duration: 0.4, ease: [0.36, 0.07, 0.19, 0.97] } satisfies Transition,
} as const
