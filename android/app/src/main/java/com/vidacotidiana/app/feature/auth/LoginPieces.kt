package com.vidacotidiana.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.vidacotidiana.app.core.ui.VidaFonts

/**
 * Las piezas del template de login de la Web, una a una.
 *
 * Cada una corresponde a un bloque real de `login.ftl` / `login.css`; ninguna
 * se inventó para rellenar. Los SVG se transcriben como `ImageVector` con los
 * mismos `path` y el mismo grosor de trazo, porque redibujarlos «parecido»
 * habría sido justamente perder el template.
 */

/** Un trazo SVG del template, dibujado tal cual sobre un lienzo escalado. */
@Composable
fun TemplateIcon(
    pathData: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 1.6f,
) {
    val path = remember(pathData) {
        androidx.compose.ui.graphics.vector.PathParser().parsePathString(pathData).toPath()
    }
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        // El viewBox del template es 0 0 24 24 en todos sus iconos.
        val scale = this.size.minDimension / 24f
        scale(scale) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/* --------------------------------------------------------------------------
   Iconos reales del template (`login.ftl`)
   -------------------------------------------------------------------------- */

/** La hoja/corazón de `.vc-brand-icon`. */
const val PATH_BRAND =
    "M12 21C12 21 5 16.5 5 10.5C5 6.91 7.69 4 11 4C11.55 4 12.09 4.08 12.6 4.23C13.32 2.9 14.69 2 16.28 2C18.65 2 20.5 3.98 20.5 6.35C20.5 12.65 12 21 12 21Z " +
        "M12 20C12 15.5 13.5 11.5 17 8"

/** Los cuatro iconos de `.vc-value-row`. */
const val PATH_HOME = "M3 10.5 12 3l9 7.5 M5.5 9.5V20h13V9.5"
const val PATH_HEART = "M12 20.5S4 15.6 4 10.2A4.2 4.2 0 0 1 12 8a4.2 4.2 0 0 1 8 2.2c0 5.4-8 10.3-8 10.3Z"
const val PATH_LEAF = "M11 20c0-6 3-10 9-11 0 6-3 10-9 11Z M11 20c-4 0-7-3-7-7 0-1 .2-2 .6-2.8"
const val PATH_SPARK = "m12 3 2.1 5.4L19.5 10l-5.4 1.6L12 17l-2.1-5.4L4.5 10l5.4-1.6L12 3Z"

/** Icono de usuario del botón principal. */
const val PATH_USER = "M12 4.5a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7Z M5.5 20C6.3 16.6 8.4 14.5 12 14.5C15.6 14.5 17.7 16.6 18.5 20"

/** Flecha de ambos botones. */
const val PATH_ARROW = "M5 12H18 M13 7L18 12L13 17"

/** Icono de «Crear una cuenta». */
const val PATH_USER_PLUS =
    "M9 4.5a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7Z M3.5 20C4.2 16.6 6.1 14.5 9 14.5C11.9 14.5 13.8 16.6 14.5 20 M18 8V15 M14.5 11.5H21.5"

/** Candado del pie, el de `.ffoot .slo` del artefacto (ADR-027). */
const val PATH_LOCK = "M7 10h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2Z M8 10V7a4 4 0 0 1 8 0v3"

/* --------------------------------------------------------------------------
   Bloques de la pantalla de inicio
   ADR-027: fuente de verdad, el artefacto aprobado «Mariposa Aprobada».
   Cada medida y color de aquí es la regla CSS que se cita, sin ajustes.
   -------------------------------------------------------------------------- */

/** El interlineado de CSS reparte el sobrante arriba y abajo por igual. */
private val CssLeading = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** `.jor` — «Jornada»: Fraunces 300, 38 px, interlineado 1, −0.02 em. */
@Composable
fun IntroWordmark(modifier: Modifier = Modifier) {
    Text(
        "Jornada",
        style = TextStyle(
            fontFamily = VidaFonts.FrauncesWordmark,
            fontWeight = FontWeight.Light,
            fontSize = 38.sp,
            lineHeight = 38.sp,
            letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
            lineHeightStyle = CssLeading,
        ),
        color = LoginTheme.introWordmark,
        modifier = modifier,
    )
}

/** `.hdiv` — filete a todo el ancho del bloque. */
@Composable
fun HeroDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LoginTheme.introDivider),
    )
}

/** `.quote` + `.meraki`. */
@Composable
fun HeroText(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "“Cada día tiene su propia historia.”",
            style = TextStyle(
                fontFamily = VidaFonts.FrauncesQuoteItalic,
                fontStyle = FontStyle.Italic,
                fontSize = 19.sp,
                lineHeight = 28.sp,
                lineHeightStyle = CssLeading,
            ),
            color = LoginTheme.introQuote,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Meraki",
            style = TextStyle(
                fontFamily = VidaFonts.AlexBrush,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                lineHeightStyle = CssLeading,
            ),
            color = LoginTheme.introScript,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

/** `.ffoot`: filete, candado y eslogan. */
@Composable
fun FormFooter(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(LoginTheme.introHairline))
        Row(
            Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TemplateIcon(PATH_LOCK, LoginTheme.introCaption, size = 13.dp, strokeWidth = 1.8f)
            Text(
                "Tu hogar, tus momentos, tu agenda.",
                style = TextStyle(fontFamily = VidaFonts.FrauncesCaption, fontSize = 13.sp),
                color = LoginTheme.introCaption,
            )
        }
    }
}

/**
 * `.vc-login-button` / `.vc-register-button`.
 *
 * Comparten caja —mismo alto, mismo radio, mismo cuerpo— y solo cambian
 * relleno y color: es lo único que debe distinguir una acción secundaria de la
 * principal, y no distinguirlas así fue el motivo real de que antes se leyeran
 * como dos botones sin relación.
 */
@Composable
fun TemplateButton(
    label: String,
    leadingPath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val content = if (primary) LoginTheme.onPrimary else LoginTheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LoginTheme.controlHeight)
            .then(
                if (primary) {
                    Modifier.background(
                        // El degradado 135° del template, de primario al
                        // primario aclarado.
                        Brush.linearGradient(
                            listOf(LoginTheme.primary, Color(0xFF7E9FBB)),
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        ),
                        shape,
                    )
                } else {
                    Modifier.background(Color.Transparent, shape)
                },
            )
            .border(1.dp, if (primary) LoginTheme.primary else LoginTheme.border, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(color = if (primary) LoginTheme.onPrimary else LoginTheme.primary),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        // `space-between`, no `center`: icono a cada lado, texto al medio.
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TemplateIcon(leadingPath, content, size = 20.dp, strokeWidth = 1.7f)
        Text(
            label,
            style = TextStyle(
                fontFamily = VidaFonts.Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (primary) 16.sp else 15.5.sp,
            ),
            color = content,
        )
        TemplateIcon(PATH_ARROW, content, size = 20.dp, strokeWidth = 1.8f)
    }
}
