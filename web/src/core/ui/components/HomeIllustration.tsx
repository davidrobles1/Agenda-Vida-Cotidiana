/**
 * UX-009: decorative hero illustration for Home's greeting card — hand-built
 * from SVG primitives (circle/path shapes), styled entirely with the shared
 * color tokens via `var(--color-*)` fills. Explicitly NOT a photo or an
 * AI-generated image — same cost/license/consistency limit already applied
 * to the login theme and the notebook-texture backgrounds (design-system.md
 * §8/§9): a hand-authored vector scales crisply at any size, ships as a few
 * hundred bytes of inline markup instead of an image asset, and never drifts
 * from the app's own palette. Purely decorative (`aria-hidden`), the real
 * greeting text next to it carries the actual information.
 *
 * The sun/moon swap is the one "real" touch — driven by the same
 * `new Date().getHours()` a caller already uses for the "Buenos días/tardes/
 * noches" greeting, not a hardcoded prop, so the two never disagree.
 */
export function HomeIllustration({ isNight = false }: { isNight?: boolean }) {
  return (
    <svg
      viewBox="0 0 160 120"
      width="100%"
      height="100%"
      aria-hidden="true"
      focusable="false"
      preserveAspectRatio="xMidYMax meet"
    >
      {/* Sky glow */}
      <circle cx="80" cy="112" r="70" fill="var(--color-primary-container)" opacity="0.35" />

      {/* Sun / moon */}
      {isNight ? (
        <path
          d="M118 30a16 16 0 1 1-9-14.4A12.8 12.8 0 0 0 118 30Z"
          fill="var(--color-terracotta)"
          opacity="0.9"
        />
      ) : (
        <circle cx="122" cy="26" r="13" fill="var(--color-terracotta)" opacity="0.85" />
      )}

      {/* Birds */}
      <path d="M30 24q4-4 8 0q4-4 8 0" stroke="var(--color-primary)" strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.6" />
      <path d="M48 34q3-3 6 0q3-3 6 0" stroke="var(--color-primary)" strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.45" />

      {/* Ground */}
      <path d="M0 100q80-18 160 0v20H0Z" fill="var(--color-success-container)" opacity="0.6" />

      {/* Trees */}
      <g opacity="0.9">
        <rect x="21" y="78" width="4" height="22" rx="2" fill="var(--color-terracotta-text)" />
        <circle cx="23" cy="72" r="14" fill="var(--color-success)" opacity="0.55" />
      </g>
      <g opacity="0.9">
        <rect x="133" y="82" width="4" height="18" rx="2" fill="var(--color-terracotta-text)" />
        <circle cx="135" cy="78" r="11" fill="var(--color-success)" opacity="0.5" />
      </g>

      {/* House */}
      <g>
        <rect x="55" y="70" width="50" height="32" rx="4" fill="var(--color-surface-variant)" stroke="var(--color-border)" strokeWidth="1" />
        <path d="M50 72 80 48 110 72Z" fill="var(--color-terracotta)" />
        <rect x="76" y="82" width="10" height="20" rx="1.5" fill="var(--color-primary)" opacity="0.85" />
        <rect x="62" y="80" width="10" height="10" rx="1.5" fill="var(--color-primary-container)" stroke="var(--color-primary)" strokeWidth="1" />
        <rect x="88" y="80" width="10" height="10" rx="1.5" fill="var(--color-primary-container)" stroke="var(--color-primary)" strokeWidth="1" />
      </g>
    </svg>
  )
}
