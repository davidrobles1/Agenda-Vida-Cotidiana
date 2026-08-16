package com.vidacotidiana.app.core.ui

import androidx.compose.ui.graphics.Color

// UX-001/design-system.md §1. Contrast ratios computed for real against WCAG
// 2.1 (relative luminance), not eyeballed — see accessibility.md §1. Success
// and warning at their base (600) tone fail 4.5:1 for text on white
// (3.77:1/3.19:1) — they're icon/border/container colors only; the *Text
// variants (700 tone) are the ones safe for text.
object VidaColor {
    val PrimaryLight = Color(0xFF4F46E5)
    val PrimaryContainerLight = Color(0xFFE0E7FF)
    val OnPrimaryLight = Color(0xFFFFFFFF)

    val PrimaryDark = Color(0xFF818CF8)
    val PrimaryContainerDark = Color(0xFF312E81)
    val OnPrimaryDark = Color(0xFF1E1B4B)

    val SuccessLight = Color(0xFF059669)
    val SuccessTextLight = Color(0xFF047857)
    val SuccessContainerLight = Color(0xFFD1FAE5)
    val SuccessDark = Color(0xFF34D399)
    val SuccessContainerDark = Color(0xFF064E3B)

    val WarningLight = Color(0xFFD97706)
    val WarningTextLight = Color(0xFFB45309)
    val WarningContainerLight = Color(0xFFFEF3C7)
    val WarningDark = Color(0xFFFBBF24)
    val WarningContainerDark = Color(0xFF78350F)

    val ErrorLight = Color(0xFFDC2626)
    val ErrorContainerLight = Color(0xFFFEE2E2)
    val ErrorDark = Color(0xFFF87171)
    val ErrorContainerDark = Color(0xFF7F1D1D)

    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF9FAFB)
    val BorderLight = Color(0xFFE5E7EB)
    val TextLight = Color(0xFF111827)
    val TextSecondaryLight = Color(0xFF6B7280)

    val SurfaceDark = Color(0xFF16171D)
    val SurfaceVariantDark = Color(0xFF1F2028)
    val BorderDark = Color(0xFF2E303A)
    val TextDark = Color(0xFFF3F4F6)
    val TextSecondaryDark = Color(0xFF9CA3AF)
}
