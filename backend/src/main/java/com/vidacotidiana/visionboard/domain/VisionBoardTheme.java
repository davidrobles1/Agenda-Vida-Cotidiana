package com.vidacotidiana.visionboard.domain;

/**
 * FASE 16: the six Board Themes — independent of the app's own visual
 * theme (core/theme/VisualThemeContext.tsx on the frontend; nothing
 * backend-side). Stored as the {@link VisionBoard#theme} column (renamed
 * from the previously-unused {@code background} column — see the V9
 * migration's own comment). The actual color palette per theme is defined
 * once, centrally, on the frontend (visionBoardThemes.ts) — the backend
 * only stores and validates the stable id, never a color value.
 */
public enum VisionBoardTheme {
    LIGHT,
    DARK,
    PAPER,
    NATURAL,
    CALM,
    ENERGY
}
