package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.tileFigure
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.style.TextOverflow

/**
 * LAS PIEZAS DEL ARTEFACTO MAESTRO QUE NO EXISTÍAN.
 *
 * Fuente de verdad:
 * https://claude.ai/code/artifact/b31707e4-2719-4a26-b7e8-b870ded5a0ec
 *
 * Cada medida de este archivo está transcrita de su CSS, no interpretada. Donde
 * el artefacto dice 560 ms, aquí dice 560 ms; donde dice 23 dp, dice 23 dp. Las
 * piezas viven aquí juntas —y no repartidas por las pantallas— porque es lo que
 * garantiza que Inicio, Pagos y Proyectos sigan pareciéndose dentro de un año.
 */

/** El easing del artefacto: `cubic-bezier(.22, 1, .36, 1)`. */
val VidaEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** El muelle del artefacto: `cubic-bezier(.34, 1.5, .64, 1)`. */
val VidaSpring = CubicBezierEasing(0.34f, 1.5f, 0.64f, 1f)

/**
 * La sombra de tarjeta del artefacto, que son DOS capas:
 *
 *   box-shadow: 0 1px 2px rgba(16,24,40,.04), 0 8px 22px rgba(16,24,40,.05)
 *
 * `Modifier.shadow` de Compose solo dibuja una, y con un desenfoque que no es
 * el mismo. Dos capas importan: la corta ancla la tarjeta al papel, la larga le
 * da altura. Con una sola, o flota sin apoyo o se pega sin aire.
 *
 * Se dibuja con `setShadowLayer` sobre un pincel transparente — el mismo
 * recurso que ya usaba `hardShadow` para la sombra dura de Neo.
 */
fun Modifier.vidaSoftShadow(
    corner: Dp,
    color: Color = Color(0xFF101828),
    keyBlur: Dp = 22.dp,
    keyOffsetY: Dp = 8.dp,
    keyAlpha: Float = 0.05f,
    ambientBlur: Dp = 2.dp,
    ambientOffsetY: Dp = 1.dp,
    ambientAlpha: Float = 0.04f,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
        }
        val r = corner.toPx()
        // Capa larga primero: la corta va encima para que no la lave.
        paint.setShadowLayer(keyBlur.toPx(), 0f, keyOffsetY.toPx(), color.copy(alpha = keyAlpha).toArgb())
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        paint.setShadowLayer(ambientBlur.toPx(), 0f, ambientOffsetY.toPx(), color.copy(alpha = ambientAlpha).toArgb())
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        paint.clearShadowLayer()
    }
}

/**
 * ANILLO DE AVANCE (`C.ring`).
 *
 * El artefacto anima `stroke-dasharray` en 560 ms con el easing de la casa, y
 * arranca el arco en las doce (`rotate(-90deg)`). La cifra va centrada, con la
 * tipografía de display del tema y el tono oscuro del módulo — nunca el tono
 * vivo, que sobre blanco no llega a 4,5:1.
 *
 * El porcentaje SE DERIVA de los pasos; este componente solo lo pinta.
 */
@Composable
fun VidaRing(
    percent: Int,
    tone: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 50.dp,
    stroke: Dp = 5.dp,
    label: String? = null,
    labelColor: Color = tone,
) {
    val c = VidaTheme.colors
    val target = (percent.coerceIn(0, 100)) / 100f
    val swept by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 560, easing = VidaEase),
        label = "ringSweep",
    )
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val w = stroke.toPx()
            val inset = w / 2f
            val arcSize = Size(size.width - w, size.height - w)
            drawArc(
                color = c.line,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = w),
            )
            if (swept > 0f) {
                drawArc(
                    color = tone,
                    startAngle = -90f, sweepAngle = 360f * swept, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = w, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
        val txt = label ?: "${percent.coerceIn(0, 100)}%"
        Text(
            txt,
            style = VidaTheme.type.tileFigure(txt, hero = false)
                .copy(fontSize = if (diameter > 60.dp) 13.sp else 12.sp, lineHeight = 16.sp),
            color = labelColor,
        )
    }
}

/**
 * CASILLA REDONDA (`.tick`).
 *
 * 23 dp dibujados, 2 dp de filete, y al marcarse crece a 1,08 en 240 ms con el
 * muelle. Lo que se PULSA nunca baja de `VidaLayout.touchTarget`: el blanco
 * táctil lo pone quien la coloca, no ella.
 */
@Composable
fun VidaTick(
    checked: Boolean,
    tone: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp,
    onClick: (() -> Unit)? = null,
) {
    val c = VidaTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.08f else 1f,
        animationSpec = tween(240, easing = VidaSpring),
        label = "tickScale",
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(if (checked) tone else Color.Transparent, RoundedCornerShape(50))
            .border(2.dp, if (checked) tone else c.border, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = c.onPrimary,
                modifier = Modifier.size(size * 0.57f),
            )
        }
    }
}

/**
 * PIEZA DE CIFRA (`.tile`).
 *
 * La retícula de Inicio es `1.32f / 1f × 120 dp / 100 dp` y esa proporción es
 * la que hace que el «5» domine sin aplastar a los otros tres. La pieza pone
 * rótulo arriba, cifra abajo y un pie; el hueco elástico va en medio, así que
 * la cifra se apoya siempre en la misma línea aunque el rótulo ocupe dos.
 */
@Composable
fun VidaTile(
    kicker: String,
    figure: String,
    caption: String,
    background: Brush,
    figureColor: Color,
    kickerColor: Color,
    captionColor: Color,
    modifier: Modifier = Modifier,
    figureSize: Dp = 38.dp,
    chevron: Boolean = false,
    decoration: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val t = VidaTheme.type
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier),
    ) {
        decoration?.invoke()
        // LA LEYENDA SE MEDÍA DESPUÉS DE REPARTIR TODO EL ALTO.
        //
        // El `Spacer(weight(1f))` se quedaba con lo que sobraba entre el
        // antetítulo y la cifra, y sólo DESPUÉS se medían cifra y leyenda. Con
        // una cifra de 54 sp en una caja de 120 dp, lo que sobraba era menos
        // que lo que faltaba: la leyenda se salía por abajo y `clip(shape)` la
        // cortaba. Tres de los cuatro mosaicos de Inicio perdían así la línea
        // que da sentido a su número —«2 tareas · 1 pago…», «en 7 secciones»—
        // y sólo se salvaba el que tenía la cifra más pequeña.
        //
        // La leyenda va ahora anclada al fondo y el hueco elástico queda entre
        // ella y la cifra, de modo que el reparto empieza por lo que hay que
        // leer y no por el espacio decorativo. La caja conserva su alto: no se
        // toca la retícula del artefacto.
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            // El antetítulo arriba, la cifra y su leyenda abajo, y el aire
            // libre en medio. Sin `Spacer(weight)`: un espaciador elástico
            // compite por el alto con lo que hay que leer, y en una caja de
            // alto fijo esa competencia la pierde siempre el contenido.
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kicker,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = kickerColor,
                    modifier = Modifier.weight(1f),
                    // Una sola línea: cuando el antetítulo se partía en dos
                    // («Con garantía»), empujaba cifra y leyenda 16 dp hacia
                    // abajo y la leyenda se salía de la caja.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chevron) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = kickerColor,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            // La cifra y su leyenda son UNA pieza, anclada abajo: la leyenda
            // explica el número y separarlas dejaba a la segunda fuera de la
            // caja. Ninguna de las dos lleva peso, así que las dos se miden
            // por lo que necesitan.
            Column {
                Text(
                    figure,
                    style = t.heroFigure.copy(fontSize = figureSize.value.sp, lineHeight = figureSize.value.sp),
                    color = figureColor,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                // UNA línea, que es lo que la caja sostiene de verdad.
                //
                // La cuenta es la que es: 120 dp menos 32 de relleno dejan 88;
                // el antetítulo pide 16, la cifra su tamaño y la leyenda 15 por
                // línea. Con dos líneas no cabe ninguna combinación, y por eso
                // la leyenda desaparecía. Admitir dos líneas y confiar en que
                // quepan era justamente el error: lo que no cabe no se recorta
                // solo, se sale y lo corta el redondeo de la tarjeta.
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = captionColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * FILA DE PROPIEDAD (`.prop`).
 *
 * Nombre a la izquierda, valor pulsable a la derecha dentro de una píldora con
 * su tono. Es el patrón de los detalles: Estado, Cuánto aprieta, Cada cuánto,
 * Dónde. El valor ES el control — no hay un lápiz aparte, porque tocar el dato
 * es lo que el usuario intenta primero.
 */
@Composable
fun VidaProp(
    name: String,
    value: String,
    valueBackground: Color,
    valueForeground: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = VidaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceElevated, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .background(valueBackground, RoundedCornerShape(50))
                .then(if (onClick != null) Modifier.vidaClickable(onClick) else Modifier)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = valueForeground,
                maxLines = 1,
            )
            if (onClick != null) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = valueForeground,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/**
 * ESTADO DE ERROR (`C.error`).
 *
 * Dice qué pasó y cómo salir. Nunca un código, nunca una traza, nunca una
 * disculpa. Era el único de los tres estados que no existía: había vacío y
 * cargando, y un fallo de red caía en una lista en blanco indistinguible de
 * «no tienes nada».
 */
@Composable
fun VidaError(
    title: String,
    body: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Reintentar",
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VidaMark(
            background = c.errorContainer,
            contentColor = c.error,
            boxSize = 56.dp,
        ) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(title, style = t.sectionTitle, color = c.text, textAlign = TextAlign.Center)
        Text(body, style = t.body, color = c.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        VidaSmallButton(retryLabel, onRetry)
    }
}
