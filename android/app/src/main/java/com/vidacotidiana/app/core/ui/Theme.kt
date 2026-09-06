package com.vidacotidiana.app.core.ui

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * ADR-023 — el tema de la aplicación, servido desde un `VidaThemeSpec`.
 *
 * DOS VOCABULARIOS, UNA FUENTE. El spec es la verdad; de él se DERIVA el
 * `ColorScheme` de Material 3 para que `Card`, `ModalBottomSheet`,
 * `TextField` y compañía sigan funcionando sin recolorearlos uno a uno. Es el
 * mismo puente que la Web mantiene entre `--ink` y `--color-text`.
 *
 * Los colores se ANIMAN al cambiar de tema (`animateColorAsState`): en el
 * artefacto el cambio de agenda es una transición, no un parpadeo, y aquí se
 * consigue gratis porque todo pasa por estos tokens.
 */
@Composable
fun VidaCotidianaTheme(
    theme: VisualTheme = VisualTheme.DEFAULT,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    val spec = VidaThemes.of(theme)
    val c = spec.colors
    val d = tween<androidx.compose.ui.graphics.Color>(320)

    // Solo se animan los que pintan superficie grande; animar los veinte
    // dispararía otras tantas animaciones por recomposición sin que se note.
    val surface by animateColorAsState(c.surface, if (animate) d else tween(0), label = "surface")
    val surfaceVariant by animateColorAsState(c.surfaceVariant, if (animate) d else tween(0), label = "surfaceVariant")
    val text by animateColorAsState(c.text, if (animate) d else tween(0), label = "text")
    val primary by animateColorAsState(c.primary, if (animate) d else tween(0), label = "primary")

    val animated = c.copy(surface = surface, surfaceVariant = surfaceVariant, text = text, primary = primary)

    val scheme = if (spec.isDark) {
        darkColorScheme(
            primary = animated.primary, onPrimary = c.onPrimary, primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.primary, secondary = c.second, onSecondary = c.onPrimary,
            secondaryContainer = c.secondContainer, onSecondaryContainer = c.second,
            error = c.error, errorContainer = c.errorContainer, onError = c.onPrimary,
            background = animated.surface, onBackground = animated.text,
            surface = animated.surface, onSurface = animated.text,
            surfaceVariant = animated.surfaceVariant, onSurfaceVariant = c.textSecondary,
            surfaceContainer = animated.surfaceVariant, surfaceContainerHigh = c.surfaceElevated,
            surfaceContainerLow = c.sunken, surfaceContainerLowest = c.sunken,
            outline = c.border, outlineVariant = c.line, scrim = androidx.compose.ui.graphics.Color(0x75080810),
        )
    } else {
        lightColorScheme(
            primary = animated.primary, onPrimary = c.onPrimary, primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.primary, secondary = c.second, onSecondary = c.onPrimary,
            secondaryContainer = c.secondContainer, onSecondaryContainer = c.second,
            error = c.error, errorContainer = c.errorContainer, onError = c.onPrimary,
            background = animated.surface, onBackground = animated.text,
            surface = animated.surface, onSurface = animated.text,
            surfaceVariant = animated.surfaceVariant, onSurfaceVariant = c.textSecondary,
            surfaceContainer = animated.surfaceVariant, surfaceContainerHigh = c.surfaceElevated,
            surfaceContainerLow = c.sunken, surfaceContainerLowest = c.sunken,
            outline = c.border, outlineVariant = c.line, scrim = androidx.compose.ui.graphics.Color(0x75080810),
        )
    }

    // La barra de estado sigue al tema: con Aurora (fondo oscuro) los iconos
    // del sistema pasan a claros. Sin esto, en Aurora la hora y la batería
    // quedaban en negro sobre negro — invisibles.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !spec.isDark
                isAppearanceLightNavigationBars = !spec.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalVidaColors provides animated,
        LocalVidaThemeSpec provides spec,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typographyFor(spec),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(spec.radii.control / 2),
                small = RoundedCornerShape(spec.radii.control),
                medium = RoundedCornerShape(spec.radii.card),
                large = RoundedCornerShape(spec.radii.card),
                extraLarge = RoundedCornerShape(spec.radii.card * 1.5f),
            ),
            content = content,
        )
    }
}

/**
 * La escala tipográfica del artefacto, mapeada a los roles de Material 3.
 *
 * Las medidas vienen en `sp`, no en `dp`: así respetan el ajuste de tamaño de
 * letra del sistema, algo que la Web no tiene y que en Android es un requisito
 * de accesibilidad real.
 */
private fun typographyFor(spec: VidaThemeSpec): Typography {
    val f = spec.fonts
    val display = TextStyle(
        fontFamily = f.display,
        fontWeight = f.displayWeight,
        fontStyle = if (f.displayItalic) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = (-0.02).em,
    )
    return Typography(
        // Titulares — el héroe de Inicio y el número grande del día.
        displayLarge = display.copy(fontSize = 40.sp, lineHeight = 44.sp),
        displayMedium = display.copy(fontSize = 32.sp, lineHeight = 36.sp),
        displaySmall = display.copy(fontSize = 26.sp, lineHeight = 30.sp),
        // Barra superior y títulos de tarjeta.
        headlineMedium = display.copy(fontSize = 22.sp, lineHeight = 27.sp),
        headlineSmall = display.copy(fontSize = 19.sp, lineHeight = 24.sp),
        titleLarge = display.copy(fontSize = 17.sp, lineHeight = 22.sp),
        // Cuerpo.
        titleMedium = TextStyle(fontFamily = f.body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = f.body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = spec.bodyLineHeight),
        bodyMedium = TextStyle(fontFamily = f.body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = TextStyle(fontFamily = f.body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        // Etiquetas — antetítulos, píldoras, chips.
        labelLarge = TextStyle(fontFamily = f.body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
        labelMedium = TextStyle(fontFamily = f.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.14.em),
        labelSmall = TextStyle(fontFamily = f.label, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.1.em),
    )
}

private val Double.em: androidx.compose.ui.unit.TextUnit get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

/**
 * `VidaTheme.colors.textSecondary`, `VidaTheme.spec.radii.card` — el acceso a
 * los tokens desde cualquier composable.
 */
object VidaTheme {
    val colors: VidaColors
        @Composable get() = LocalVidaColors.current
    val spec: VidaThemeSpec
        @Composable get() = LocalVidaThemeSpec.current
}

/** Tachado de una tarea hecha, en un solo sitio. */
val StrikeThrough = TextDecoration.LineThrough
