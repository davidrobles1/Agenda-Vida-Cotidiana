import type { VisionBoardThemeId } from './api'

/**
 * FASE 16: Board Themes — deliberately independent of the app's own
 * global visual theme (core/theme/VisualThemeContext.tsx). That system
 * applies a `theme-*` class to AppShell's own wrapper, overriding the
 * app-wide `--color-*` custom properties; reusing those same names here
 * would make a Board Theme bleed into (or fight with) the app theme the
 * instant both were active at once (e.g. App Theme "Oscuro" + Board Theme
 * "Papel"). Every Board Theme token below uses its own `--vb-*` prefix, and
 * (see VisionBoardCanvas.tsx) is applied via inline style scoped to the
 * `.canvas` element alone — CSS custom properties only inherit to
 * descendants, so the toolbar/dialogs/popovers around the canvas never see
 * these at all and keep using the app's own tokens untouched.
 *
 * This is the ONE place the six palettes are defined — VisionBoardCanvas's
 * inline style, VisionBoardThemeSwitcher's preview swatches, and
 * visionBoardExport.ts's PNG/PDF rendering all read from here, never
 * duplicate a color.
 */
export interface VisionBoardThemeColors {
  /** Outer/page-level tone — used for preview swatches only; `.canvas`
      itself fills with `surfaceVariant` (see below). */
  background: string
  /** `.canvas`'s own fill. */
  surfaceVariant: string
  /** Element background (TEXT/STICKER/IMAGE/SHAPE's own `.element` box) —
      a distinct shade from `surfaceVariant`, same "surface vs.
      surface-variant" two-tone pairing the app's own tokens already use,
      so an element still reads as a card sitting *on* the canvas rather
      than blending into it. */
  surface: string
  text: string
  textSecondary: string
  border: string
  /** Secondary/accent — Smart Guides, SHAPE (rectangle/circle/line) fill. */
  accent: string
  /** Selection outline + resize/rotate handle ring. */
  selection: string
  /** Selection glow (the soft box-shadow around a selected element) —
      paired with `selection` the same way the app's own tokens pair
      `--color-primary` with `--color-primary-container`. */
  selectionContainer: string
  /** NOTE element fill. */
  noteBackground: string
  /** SHAPE element fill. */
  shapeBackground: string
  /** BLOQUE F (post-MVP): opacity (0–1) of `.canvas`'s own paper-grain
      texture overlay (VisionBoardCanvas.module.css's `.canvas::before`) —
      a material property, not a new color, so it lives alongside the
      other tokens here rather than as a 7th CSS custom property computed
      elsewhere. Deliberately NOT the same value for every theme: PAPER is
      the one theme whose whole identity is "physical corkboard," so it
      gets a visibly stronger grain than the others — every theme still
      gets *some* texture (a flat, textureless canvas reads as a CRUD
      form, exactly what this block exists to move away from), just not
      all at PAPER's intensity. */
  textureOpacity: number
}

export interface VisionBoardThemeDefinition {
  id: VisionBoardThemeId
  name: string
  description: string
  colors: VisionBoardThemeColors
}

export const DEFAULT_VISION_BOARD_THEME: VisionBoardThemeId = 'LIGHT'

export const VISION_BOARD_THEMES: Record<VisionBoardThemeId, VisionBoardThemeDefinition> = {
  LIGHT: {
    id: 'LIGHT',
    name: 'Claro',
    description: 'El tono neutro por defecto — mismo look que el resto de Vida Cotidiana.',
    colors: {
      background: '#fffcf6',
      surfaceVariant: '#fffcf6',
      surface: '#f5eedf',
      text: '#2c425b',
      // FASE 22 audit fix: a real axe-core run surfaced a contrast gap the
      // Fase 20 audit missed — it only checked textSecondary against
      // surfaceVariant, never against `surface` (what an element's own
      // placeholder text actually sits on). Failed at 4.43:1 there; this
      // clears both.
      textSecondary: '#596d83',
      border: '#6e8192',
      accent: '#2c5f8c',
      selection: '#2c5f8c',
      selectionContainer: '#e4eef6',
      noteBackground: '#f3e4c4',
      shapeBackground: '#a9cbe8',
      textureOpacity: 0.035,
    },
  },
  DARK: {
    id: 'DARK',
    name: 'Oscuro',
    description: 'Fondo profundo con acentos claros — ideal para trabajar de noche.',
    colors: {
      background: '#1c2430',
      surfaceVariant: '#232c3a',
      surface: '#2b3547',
      text: '#e8edf3',
      textSecondary: '#a8b6c6',
      border: '#3c4a5c',
      accent: '#6fa8dc',
      selection: '#6fa8dc',
      selectionContainer: '#2a3a4a',
      noteBackground: '#3a3320',
      shapeBackground: '#1f4f73',
      textureOpacity: 0.05,
    },
  },
  PAPER: {
    id: 'PAPER',
    name: 'Papel',
    description: 'Crema cálido y tinta oscura — el corcho físico de siempre.',
    colors: {
      background: '#f4ecd8',
      surfaceVariant: '#f9f2e2',
      surface: '#f4ead2',
      text: '#4a3f2e',
      // FASE 22 audit fix: same real, axe-found gap against `surface`
      // specifically (4.28:1) — see LIGHT's own doc comment above.
      textSecondary: '#746750',
      border: '#cdbfa0',
      accent: '#a9762c',
      selection: '#a9762c',
      selectionContainer: '#ecdcb8',
      noteBackground: '#f0d9a6',
      shapeBackground: '#e0b06a',
      textureOpacity: 0.12,
    },
  },
  NATURAL: {
    id: 'NATURAL',
    name: 'Natural',
    description: 'Verdes y tonos de tierra — para tableros de bienestar y hábitos.',
    colors: {
      background: '#eef2e6',
      surfaceVariant: '#f4f7ee',
      surface: '#eaf0e0',
      text: '#33422c',
      // FASE 22 audit fix: a real axe-core run found this failing contrast
      // (4.42:1, needs 4.5:1) against `surface` specifically — e.g. a
      // STICKER placeholder's text sits on `surface`, not `surfaceVariant`
      // (the only pairing the Fase 20 audit had checked). Darkened just
      // enough to clear both.
      textSecondary: '#616f54',
      border: '#b8c8a8',
      accent: '#5c8a4a',
      selection: '#5c8a4a',
      selectionContainer: '#dcebd0',
      noteBackground: '#e2ecd0',
      shapeBackground: '#9cc687',
      textureOpacity: 0.04,
    },
  },
  CALM: {
    id: 'CALM',
    name: 'Calm',
    description: 'Azules y lavandas suaves, baja saturación — para enfocarse sin ruido visual.',
    colors: {
      background: '#eef1f7',
      surfaceVariant: '#f5f7fb',
      surface: '#eceef7',
      text: '#3b4256',
      // FASE 20 audit fix: both of these failed WCAG contrast against
      // surfaceVariant (#6b7488 → 4.37:1, needs 4.5:1 for text; #7c8fc0 →
      // 2.99:1, needs 3:1 for UI components) by a hair — darkened just
      // enough to clear the threshold without changing the theme's
      // low-saturation lavender-blue identity.
      // FASE 22 audit fix: a real axe-core run found the Fase 20 fix still
      // wasn't enough against `surface` specifically (4.17:1, needs
      // 4.5:1) — Fase 20 only ever checked surfaceVariant. Darkened once
      // more to clear both.
      textSecondary: '#626b7c',
      border: '#c3cbdc',
      accent: '#798cbe',
      selection: '#798cbe',
      selectionContainer: '#e1e5f3',
      noteBackground: '#e6e9f5',
      shapeBackground: '#aab6dd',
      textureOpacity: 0.03,
    },
  },
  ENERGY: {
    id: 'ENERGY',
    name: 'Energía',
    description: 'Naranjas vibrantes sobre un fondo cálido — para metas y motivación.',
    colors: {
      background: '#fff5ec',
      surfaceVariant: '#fffaf4',
      surface: '#fff1e4',
      text: '#402313',
      textSecondary: '#7a4a2a',
      border: '#f0b285',
      accent: '#e8622c',
      selection: '#e8622c',
      selectionContainer: '#ffd9b8',
      noteBackground: '#ffe0b8',
      shapeBackground: '#ffb37a',
      textureOpacity: 0.045,
    },
  },
}

export const VISION_BOARD_THEME_LIST: VisionBoardThemeDefinition[] = Object.values(VISION_BOARD_THEMES)

export function visionBoardThemeOf(id: string | null | undefined): VisionBoardThemeDefinition {
  return (id && VISION_BOARD_THEMES[id as VisionBoardThemeId]) || VISION_BOARD_THEMES[DEFAULT_VISION_BOARD_THEME]
}

/** The `--vb-*` custom properties for one theme, ready to spread into a
    React inline `style` object (cast to `CSSProperties` at the call site —
    custom properties aren't part of that type). Scoping is purely a matter
    of *where* this gets applied (VisionBoardCanvas.tsx applies it to
    `.canvas` alone), not anything special done here. */
export function visionBoardThemeStyleVars(id: string | null | undefined): Record<string, string> {
  const { colors } = visionBoardThemeOf(id)
  return {
    '--vb-background': colors.background,
    '--vb-surface-variant': colors.surfaceVariant,
    '--vb-surface': colors.surface,
    '--vb-text': colors.text,
    '--vb-text-secondary': colors.textSecondary,
    '--vb-border': colors.border,
    '--vb-accent': colors.accent,
    '--vb-selection': colors.selection,
    '--vb-selection-container': colors.selectionContainer,
    '--vb-note-bg': colors.noteBackground,
    '--vb-shape-bg': colors.shapeBackground,
    '--vb-texture-opacity': String(colors.textureOpacity),
  }
}
