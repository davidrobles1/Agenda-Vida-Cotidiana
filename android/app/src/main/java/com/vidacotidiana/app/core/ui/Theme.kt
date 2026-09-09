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
        // Las dos reglas derivadas del spec. Se calculan aquí una vez por tema
        // en vez de en cada composable que las lee.
        LocalVidaType provides typeScaleFor(spec),
        LocalVidaRoles provides rolesFor(animated),
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
 * Los roles de Material 3, SERVIDOS DESDE LA ESCALA SEMÁNTICA.
 *
 * Ya no hay dos escalas. `VidaTypeScale` es la única fuente y esto es el puente
 * hacia M3, igual que `ColorScheme` lo es para el color: los componentes de
 * serie (`TextField`, `ModalBottomSheet`, `DatePicker`) y las pantallas que aún
 * escriben `MaterialTheme.typography.titleMedium` reciben exactamente el mismo
 * estilo que recibe quien pide `VidaTheme.type.cardTitle`.
 *
 * El efecto práctico: corregir la jerarquía aquí la corrige en las diecisiete
 * secciones a la vez, sin tocarlas. `titleMedium` pasa de 15 sp a 16 sp y deja
 * de medir lo mismo que `bodyLarge`, que era el fallo de fondo.
 *
 * Antes existía además `VidaTypography` en `Type.kt`, una TERCERA escala (18/16/
 * 14 sp, fuente del sistema) que no se usaba en ningún sitio pero que cualquiera
 * habría tomado por la buena. Borrada.
 */
private fun typographyFor(spec: VidaThemeSpec): Typography {
    val t = typeScaleFor(spec)
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
        displaySmall = t.heroFigure,
        // Barra superior y cabeceras de bloque.
        headlineMedium = t.screenTitle,
        headlineSmall = t.sectionTitle,
        titleLarge = display.copy(fontSize = 17.sp, lineHeight = 22.sp),
        // Contenido — los tres primeros niveles de la jerarquía.
        titleMedium = t.cardTitle,
        bodyLarge = t.body,
        bodyMedium = t.metadata,
        bodySmall = t.caption,
        // Etiquetas — acción, antetítulo, rótulo mínimo.
        labelLarge = t.action,
        labelMedium = t.eyebrow,
        labelSmall = t.micro,
    )
}

private val Double.em: androidx.compose.ui.unit.TextUnit get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

/**
 * `VidaTheme.colors.textSecondary`, `VidaTheme.spec.radii.card` — el acceso a
 * los tokens desde cualquier composable.
 *
 * `type` y `role` se leen igual: `VidaTheme.type.cardTitle`,
 * `VidaTheme.role.completeFg`. Preferir estos dos a
 * `MaterialTheme.typography.*` y a `VidaTheme.colors.*` en cuanto lo que se
 * quiere expresar tiene un nombre en el sistema — que es casi siempre.
 */
object VidaTheme {
    val colors: VidaColors
        @Composable get() = LocalVidaColors.current
    val spec: VidaThemeSpec
        @Composable get() = LocalVidaThemeSpec.current

    /** La escala tipográfica por significado. Ver `VidaType.kt`. */
    val type: VidaTypeScale
        @Composable get() = LocalVidaType.current

    /** Qué significa cada color. Ver `VidaRoles.kt`. */
    val role: VidaRoles
        @Composable get() = LocalVidaRoles.current
}

/** Tachado de una tarea hecha, en un solo sitio. */
val StrikeThrough = TextDecoration.LineThrough
