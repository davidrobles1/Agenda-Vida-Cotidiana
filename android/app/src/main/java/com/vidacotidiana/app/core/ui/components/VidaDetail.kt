package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import androidx.compose.ui.graphics.Brush

/**
 * LA COMPOSICIÓN DE DETALLE DEL ARTEFACTO MAESTRO.
 *
 * Sus cuatro pantallas de detalle —pago, mantenimiento, persona, proyecto—
 * comparten exactamente la misma estructura, y por eso vive aquí una sola vez:
 *
 *   1. HÉROE de 30 dp de radio en degradado del tono del registro, con la cifra
 *      o el avance a la izquierda y el nombre a la derecha.
 *   2. PROPIEDADES: filas hundidas donde el VALOR es el control. No hay un
 *      lápiz aparte, porque tocar el dato es lo que el usuario intenta primero.
 *   3. SECCIONES con antetítulo en versalita.
 *
 * Escribirla cuatro veces habría dado cuatro interpretaciones del mismo héroe:
 * es exactamente así como una aplicación acaba con cuatro detalles que se
 * parecen pero no encajan.
 */

/**
 * El héroe del detalle.
 *
 * `figure` es la pieza de la izquierda: un anillo de avance, un importe, una
 * inicial. Se recibe como composable porque cambia por pantalla —en Pagos es el
 * importe, en Mantenimiento el porcentaje de pasos— mientras el marco no.
 */
@Composable
fun VidaDetailHero(
    tone: Color,
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    figure: (@Composable () -> Unit)? = null,
) {
    val t = VidaTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .vidaSoftShadow(30.dp, color = tone, keyBlur = 32.dp, keyOffsetY = 14.dp, keyAlpha = 0.30f)
            .background(tone, RoundedCornerShape(30.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        figure?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                eyebrow.uppercase(),
                style = t.eyebrow,
                color = Color.White.copy(alpha = 0.78f),
            )
            Text(
                title,
                style = t.sectionTitle.copy(fontSize = 21.sp, lineHeight = 27.sp),
                color = Color.White,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }
}

/**
 * Una cifra grande dentro del héroe, en blanco sobre el tono.
 *
 * En blanco y no en el tono: el fondo YA lleva el color, y repetirlo haría
 * desaparecer la cifra. Es el mismo criterio del anillo del detalle de tarea.
 */
@Composable
fun VidaHeroFigure(text: String, modifier: Modifier = Modifier) {
    Box(modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = VidaTheme.type.tileFigureFor(text),
            color = Color.White,
        )
    }
}

/** La cifra del héroe cede tamaño antes que carácteres, como en la retícula. */
private fun com.vidacotidiana.app.core.ui.VidaTypeScale.tileFigureFor(text: String) =
    heroFigure.copy(
        fontSize = when {
            text.length <= 4 -> 30.sp
            text.length <= 7 -> 22.sp
            else -> 17.sp
        },
        lineHeight = 34.sp,
    )

/** Antetítulo de sección dentro de un detalle. */
@Composable
fun VidaSectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = t.eyebrow, color = c.textTertiary, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = t.caption, color = c.textTertiary)
        }
    }
}

/**
 * Un bloque de propiedades. Cada par es nombre + valor con su tono.
 *
 * Recibe la lista ya resuelta y no un modelo: qué propiedades tiene un pago y
 * cuáles una persona es cosa de cada pantalla, no de este componente.
 */
@Composable
fun VidaPropertyBlock(
    props: List<VidaProperty>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
        props.forEach { p ->
            VidaProp(
                name = p.name,
                value = p.value,
                valueBackground = p.background,
                valueForeground = p.foreground,
                onClick = p.onClick,
            )
        }
    }
}

/** Una propiedad del detalle: nombre, valor, tono y qué hace al tocarla. */
data class VidaProperty(
    val name: String,
    val value: String,
    val background: Color,
    val foreground: Color,
    val onClick: (() -> Unit)? = null,
)

/**
 * Dos piezas de fecha, lado a lado.
 *
 * El artefacto las usa en Mantenimiento («última vez» / «tocaba») y en Pagos
 * («último cobro» / «próximo»). Son una retícula de dos, no dos filas: puestas
 * una debajo de otra se pierde la comparación, que es justo para lo que están.
 */
@Composable
fun VidaDatePair(
    leftLabel: String,
    leftValue: String,
    leftCaption: String,
    rightLabel: String,
    rightValue: String,
    rightCaption: String,
    rightTone: Color,
    rightBackground: Color,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
        Column(
            Modifier
                .weight(1f)
                .background(c.surfaceElevated, RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Text(leftLabel.uppercase(), style = t.eyebrow, color = c.textTertiary)
            Text(
                leftValue,
                style = t.sectionTitle.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = c.text,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(leftCaption, style = t.micro, color = c.textTertiary, modifier = Modifier.padding(top = 3.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .background(rightBackground, RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Text(rightLabel.uppercase(), style = t.eyebrow, color = rightTone.copy(alpha = 0.7f))
            Text(
                rightValue,
                style = t.sectionTitle.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = rightTone,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                rightCaption,
                style = t.micro,
                color = rightTone.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/**
 * LA RETÍCULA DE PIEZAS DE CABECERA.
 *
 * Es la composición con la que el artefacto abre sus secciones: dos, tres o
 * cuatro cifras a lo ancho, leídas de un vistazo antes de leer una sola fila de
 * la lista. Sustituye a la tira de `StripCard` que se desplazaba en horizontal
 * —donde la cuarta cifra quedaba fuera de pantalla y por tanto no existía.
 *
 * La PROPORCIÓN la decide quien llama: en Inicio la primera pieza pesa 1,32
 * porque «lo de hoy» domina; en Garantías las tres severidades pesan igual
 * porque ninguna manda sobre las otras. Forzar aquí una proporción única
 * obligaría a las secciones a mentir sobre su jerarquía.
 */
@Composable
fun VidaTileRow(
    tiles: List<VidaTileSpec>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 92.dp,
) {
    if (tiles.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap),
    ) {
        tiles.forEach { spec ->
            VidaTile(
                kicker = spec.kicker,
                figure = spec.figure,
                caption = spec.caption,
                background = spec.backgroundBrush ?: androidx.compose.ui.graphics.SolidColor(spec.background),
                figureColor = spec.figureColor,
                kickerColor = spec.foreground,
                captionColor = spec.foreground.copy(alpha = 0.75f),
                figureSize = spec.figureSize,
                chevron = spec.onClick != null,
                onClick = spec.onClick,
                modifier = Modifier.weight(spec.weight).fillMaxHeight(),
            )
        }
    }
}

/**
 * LOS ADJUNTOS DE UN DETALLE, CON SUS DOS MITADES.
 *
 * LA BRECHA QUE CIERRA. El mecanismo genérico de V37 —`resource_type` /
 * `resource_id` en `documents`, con `POST /documents/{id}/link`— estaba
 * completo de extremo a extremo, pero desde la pantalla solo se podía SOLTAR.
 * Colgar era imposible: no había ningún gesto que llamara a `attach`. Media
 * función implementada es, en la práctica, ninguna.
 *
 * POR QUÉ NO ES UNA HOJA. El selector se abre AQUÍ MISMO, debajo del gesto que
 * lo pidió. Una hoja modal taparía el detalle entero para elegir un archivo y
 * volvería a la pantalla sin que se vea dónde quedó colgado; en línea, el
 * documento aparece en la lista de arriba en el sitio donde va a vivir.
 *
 * QUÉ SE OFRECE Y QUÉ NO. Solo documentos SUELTOS. Colgar uno que ya cuelga de
 * otro registro lo MOVERÍA —`link` es uno a uno, no una lista— y hacerlo desde
 * un selector, sin decirlo, dejaría al otro registro sin su comprobante sin que
 * nadie se entere. Los que ya cuelgan se cuentan al pie, para que el usuario
 * sepa que existen y por qué no están en la lista.
 *
 * SOLTAR NO ES BORRAR: el documento vuelve a Documentos, donde no ha dejado de
 * estar. El texto lo dice porque es lo primero que se teme al pulsarlo.
 */
@Composable
fun VidaAttachments(
    attached: List<VidaFile>,
    loose: List<VidaFile>,
    hangingElsewhere: Int,
    loading: Boolean,
    onAttach: (String) -> Unit,
    onDetach: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    var picking by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
        when {
            loading && attached.isEmpty() -> LoadingRows(1)
            attached.isEmpty() -> Text(
                "Sin archivos. Un documento de la sección Documentos puede colgarse aquí " +
                    "sin dejar de estar allí.",
                style = t.body,
                color = c.textTertiary,
            )
            else -> attached.forEach { file ->
                VidaFileRow(file) {
                    Text(
                        "Soltar",
                        style = t.caption,
                        color = c.textSecondary,
                        modifier = Modifier.vidaClickable(onClick = { onDetach(file.id) }),
                    )
                }
            }
        }

        // El gesto que faltaba.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (picking) c.primaryContainer else c.surfaceElevated,
                    RoundedCornerShape(50),
                )
                .vidaClickable(onClick = { picking = !picking })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (picking) "Elige cuál" else "Colgar un documento",
                style = t.caption,
                color = if (picking) c.primaryDeep else c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (picking) "Cancelar" else "+",
                style = t.caption,
                color = if (picking) c.primaryDeep else c.textSecondary,
            )
        }

        if (picking) {
            if (loose.isEmpty()) {
                Text(
                    if (hangingElsewhere > 0) {
                        "No tienes documentos sueltos. Los $hangingElsewhere que hay ya cuelgan de " +
                            "otro registro, y colgarlos aquí los quitaría de allí."
                    } else {
                        "Todavía no has subido ningún documento. Se suben en la sección Documentos."
                    },
                    style = t.body,
                    color = c.textTertiary,
                )
            } else {
                loose.forEach { file ->
                    VidaFileRow(file) {
                        Text(
                            "Colgar",
                            style = t.caption,
                            color = c.primaryDeep,
                            modifier = Modifier.vidaClickable(
                                onClick = {
                                    picking = false
                                    onAttach(file.id)
                                },
                            ),
                        )
                    }
                }
                if (hangingElsewhere > 0) {
                    Text(
                        "No se listan $hangingElsewhere que ya cuelgan de otro registro: " +
                            "colgarlos aquí los quitaría de allí.",
                        style = t.micro,
                        color = c.textTertiary,
                    )
                }
            }
        }
    }
}

/** Una fila de archivo, con su acción a la derecha. Igual colgado que por colgar. */
@Composable
private fun VidaFileRow(file: VidaFile, action: @Composable () -> Unit) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceElevated, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        VidaMark(
            background = if (file.isImage) c.primaryContainer else c.errorContainer,
            contentColor = if (file.isImage) c.primary else c.error,
            boxSize = 34.dp,
        ) {
            Icon(
                if (file.isImage) Icons.Outlined.Image else Icons.Outlined.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(file.name, style = t.cardTitle.copy(fontSize = 13.sp), color = c.text, maxLines = 1)
            Text(file.meta, style = t.micro, color = c.textTertiary, maxLines = 1)
        }
        action()
    }
}

/**
 * Un archivo, como lo necesita la vista.
 *
 * NO es `Attachment` ni `Document`: son dos modelos de datos distintos —uno
 * viene de `/documents/attachments`, el otro de `/documents`— y esta sección
 * pinta los dos en la misma fila. Traducirlos aquí evita que un componente de
 * `core/ui` dependa de `core/data`, y evita también escribir la fila dos veces.
 */
data class VidaFile(
    val id: String,
    val name: String,
    val meta: String,
    val isImage: Boolean,
)

/** Una pieza de la retícula de cabecera. */
data class VidaTileSpec(
    val kicker: String,
    val figure: String,
    val caption: String,
    val background: Color,
    val figureColor: Color,
    val foreground: Color,
    val weight: Float = 1f,
    /**
     * Relleno con DEGRADADO, cuando el artefacto lo pide.
     *
     * La pieza «Este mes» de Pagos es `linear-gradient(145deg, --indigo-l,
     * --indigo-d)` con sombra índigo y texto blanco; aquí era un relleno plano
     * y pálido, y por eso la pantalla se veía mucho menos viva que el
     * artefacto. Nulo en el resto: una pieza plana sigue siendo plana.
     */
    val backgroundBrush: Brush? = null,
    /** 22 dp y no 26: en la tira de 92 dp quedan 60 utiles y el contenido
        pedia los 60 exactos, asi que la leyenda se cortaba por abajo. */
    val figureSize: androidx.compose.ui.unit.Dp = 22.dp,
    val onClick: (() -> Unit)? = null,
)
