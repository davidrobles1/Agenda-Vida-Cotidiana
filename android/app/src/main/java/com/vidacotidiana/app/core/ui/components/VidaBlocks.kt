package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.data.DayNote
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import kotlin.math.roundToInt

/**
 * Los bloques compuestos del artefacto: el héroe, las tarjetas de tira, el
 * estado vacío, la libreta de notas, la tarjeta de tema, la fila deslizable y
 * la entrada escalonada.
 */

/**
 * Héroe de Inicio: una sola cosa a la que atender, con salida clara.
 *
 * Lleva el arco de la marca al fondo, muy tenue, igual que en el artefacto —
 * no es decoración aleatoria: es la identidad «A · Tiempo» usada como textura.
 */
@Composable
fun HeroCard(
    eyebrow: String,
    title: String,
    body: String,
    primaryAction: Pair<String, () -> Unit>?,
    secondaryAction: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.card)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (spec.elevation.isHard) Modifier.hardShadow(spec.elevation.hardOffset, spec.radii.card, c.text)
                else Modifier,
            )
            .background(c.primary, shape)
            .then(if (spec.borderWidth > 1.dp) Modifier.border(spec.borderWidth, c.text, shape) else Modifier)
            .drawBehind {
                // El arco de la marca, al 15 %: la misma figura del logo.
                val stroke = 14.dp.toPx()
                val r = size.height * 1.1f
                drawArc(
                    color = c.onPrimary.copy(alpha = 0.14f),
                    startAngle = 195f, sweepAngle = 110f, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width - r * 0.9f, -r * 0.35f),
                    size = androidx.compose.ui.geometry.Size(r, r),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
            }
            .padding(VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = c.onPrimary.copy(alpha = 0.75f),
        )
        Text(
            if (spec.fonts.displayUppercase) title.uppercase() else title,
            style = MaterialTheme.typography.headlineMedium,
            color = c.onPrimary,
        )
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.onPrimary.copy(alpha = 0.86f))
        if (primaryAction != null || secondaryAction != null) {
            Row(
                Modifier.padding(top = VidaSpacing.sm).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                primaryAction?.let { HeroButton(it.first, solid = true, onClick = it.second, modifier = Modifier.weight(1f)) }
                secondaryAction?.let { HeroButton(it.first, solid = false, onClick = it.second, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HeroButton(text: String, solid: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.pill)
    Box(
        modifier = modifier
            .pressScale(interaction, 0.95f)
            .background(if (solid) c.onPrimary else Color.Transparent, shape)
            .border(1.dp, c.onPrimary.copy(alpha = if (solid) 1f else 0.45f), shape)
            .clickable(interactionSource = interaction, indication = ripple(color = c.onPrimary), onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (solid) c.primary else c.onPrimary,
        )
    }
}

/**
 * Tarjeta de una tira horizontal: métrica compacta que además navega.
 *
 * ANCHO MÍNIMO, NO ANCHO FIJO. Con `width(150.dp)` la cifra se rompía: en Pagos
 * se leía «MÁS…», «27 20…» e «Inter…» cortados, y «$3,729 MXN» partido en dos
 * líneas — siendo la primera cifra de la pantalla. Una métrica que no se puede
 * leer entera no es una métrica. Ahora la pieza parte de 150 dp y crece con su
 * contenido; la tira sigue desplazándose en horizontal, así que crecer no le
 * quita sitio a nadie.
 *
 * La cifra usa `heroFigure`: es el display del tema con SU peso, no una copia
 * en negrita. En Lumen sale fina y aireada, en Neo pesada — que es la
 * diferencia entre las dos agendas.
 */
@Composable
fun StripCard(
    kicker: String,
    value: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.card)
    Column(
        modifier = modifier
            .widthIn(min = 150.dp)
            .pressScale(interaction, 0.97f)
            .vidaSurface(shape, spec.radii.card, c.surfaceVariant, c.line)
            .clickable(interactionSource = interaction, indication = ripple(color = c.primary), onClick = onClick)
            .padding(VidaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VidaLayout.textGap),
    ) {
        Text(kicker.uppercase(), style = t.micro, color = c.textTertiary)
        Text(value, style = t.heroFigure, color = c.text, maxLines = 1)
        Text(caption, style = t.caption, color = c.textSecondary, maxLines = 1)
    }
}

/**
 * Estado vacío: nunca una lista en blanco, siempre una frase que explique.
 *
 * NO ES UN CENTRO DE PANTALLA. Antes se centraba con 32 dp arriba y abajo, y en
 * una sección sin datos el resultado era un título flotando sobre mil doscientos
 * píxeles de nada — Inbox, Agenda y Compartidos se ven así en las capturas. Un
 * vacío centrado en el eje vertical de un espacio vacío no compone nada; solo
 * subraya lo que falta.
 *
 * Ahora el bloque se alinea a la izquierda con el resto del contenido y ocupa el
 * alto que necesita. La sección sigue empezando donde empiezan todas, el texto
 * cae en la misma columna que el de una lista con datos, y el hueco de abajo
 * pasa a ser margen en vez de agujero.
 *
 * `action` es la parte que faltaba: de once estados vacíos solo uno ofrecía la
 * acción siguiente. Un vacío que dice qué NO hay y no dice qué hacer es un
 * callejón sin salida.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: Pair<String, () -> Unit>? = null,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Column(
        modifier = modifier.fillMaxWidth().padding(top = VidaLayout.blockGap, bottom = VidaLayout.sectionGap),
        verticalArrangement = Arrangement.spacedBy(VidaLayout.textGap),
    ) {
        Text(title, style = t.sectionTitle, color = c.text)
        Text(
            body,
            style = t.body,
            color = c.textSecondary,
        )
        action?.let {
            Spacer(Modifier.height(VidaSpacing.md))
            VidaSmallButton(it.first, it.second)
        }
    }
}

/**
 * Filas fantasma mientras llegan los datos.
 *
 * Tienen la altura y el ritmo de una `ResourceRow` real para que la lista no
 * dé un salto al llegar el contenido, y respiran despacio (1.2 s) en lugar de
 * parpadear — a `prefers-reduced-motion` no llega Compose, así que la
 * animación se mantiene deliberadamente suave.
 */
@Composable
fun LoadingRows(count: Int = 3, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.card)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
        repeat(count) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(c.surfaceVariant.copy(alpha = alpha), shape)
                    .border(spec.borderWidth, c.line, shape)
                    .padding(horizontal = VidaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                Box(Modifier.size(28.dp).background(c.sunken, RoundedCornerShape(spec.radii.control)))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(150.dp).height(11.dp).background(c.sunken, RoundedCornerShape(4.dp)))
                    Box(Modifier.width(96.dp).height(9.dp).background(c.sunken, RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

/**
 * La libreta de notas del día. El rayado se dibuja con `drawBehind` —el
 * `NotebookBackground` que el proyecto ya tenía— y el texto va en la
 * manuscrita del tema cuando existe (solo Papel).
 */
@Composable
fun NotebookNotes(
    notes: List<DayNote>,
    placeholder: String,
    onAdd: (String) -> Unit,
    onEdit: (DayNote, String) -> Unit,
    onDelete: (DayNote) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val lineHeight = 30.dp
    val shape = RoundedCornerShape(spec.radii.card)
    var editing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var composing by remember { mutableStateOf("") }

    // Un solo estilo para leer, editar y escribir. Es una invariante de la
    // Web («si divergen, el texto salta al editar») y aquí vale igual: la
    // línea escrita debe ocupar exactamente lo mismo que la línea guardada.
    val style = if (spec.fonts.hand != null) {
        MaterialTheme.typography.titleLarge.copy(
            fontFamily = spec.fonts.hand, fontSize = 19.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp,
        )
    } else {
        MaterialTheme.typography.bodyLarge
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceVariant, shape)
            .border(spec.borderWidth, c.line, shape)
            .drawBehind {
                var y = lineHeight.toPx()
                while (y < size.height) {
                    drawLine(
                        color = c.line,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                    y += lineHeight.toPx()
                }
            }
            .padding(horizontal = VidaSpacing.md, vertical = 4.dp),
    ) {
        if (loading && notes.isEmpty()) {
            Box(Modifier.height(lineHeight), contentAlignment = Alignment.CenterStart) {
                Text("Cargando tus notas…", style = style, color = c.textTertiary)
            }
        }

        notes.forEach { note ->
            val isEditing = editing == note.id
            Row(
                Modifier.fillMaxWidth().height(lineHeight).subeSobreElTeclado(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditing) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = style.copy(color = c.text),
                        cursorBrush = SolidColor(c.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
    imeAction = ImeAction.Done,
    // La misma regla que `VidaTextField`: una nota es una frase.
    capitalization = KeyboardCapitalization.Sentences,
),
                        // Guardar con vacío borra la nota: es lo que significa
                        // dejar una línea en blanco en una libreta.
                        keyboardActions = KeyboardActions(onDone = {
                            onEdit(note, draft)
                            editing = null
                        }),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Borrar",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textSecondary,
                        modifier = Modifier
                            .clickable {
                                onDelete(note)
                                editing = null
                            }
                            .padding(horizontal = VidaSpacing.xs),
                    )
                } else {
                    Text(
                        note.text,
                        style = style,
                        color = c.text,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                editing = note.id
                                draft = note.text
                            },
                    )
                }
            }
        }

        // El compositor está SIEMPRE listo, como en la Web: escribir una nota
        // no exige abrir nada antes.
        Row(Modifier.fillMaxWidth().height(lineHeight).subeSobreElTeclado(), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = composing,
                onValueChange = { composing = it },
                textStyle = style.copy(color = c.text),
                cursorBrush = SolidColor(c.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
    imeAction = ImeAction.Done,
    // La misma regla que `VidaTextField`: una nota es una frase.
    capitalization = KeyboardCapitalization.Sentences,
),
                keyboardActions = KeyboardActions(onDone = {
                    onAdd(composing)
                    composing = ""
                }),
                decorationBox = { inner ->
                    if (composing.isEmpty()) {
                        Text(placeholder, style = style, color = c.textTertiary)
                    }
                    inner()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Tarjeta de tema del selector de apariencia, con su muestra y su vista previa. */
@Composable
fun ThemeCard(
    label: String,
    tagline: String,
    swatch: Color,
    preview: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.radii.card)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .vidaSurface(shape, spec.radii.card, c.surfaceVariant, if (selected) c.primary else c.line)
            .clickable(interactionSource = interaction, indication = ripple(color = c.primary), onClick = onClick)
            .padding(VidaSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VidaSpacing.md),
    ) {
        Box(Modifier.size(32.dp).background(swatch, RoundedCornerShape(10.dp)).border(1.dp, c.line, RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(tagline, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            preview.forEach { p ->
                Box(Modifier.width(10.dp).height(24.dp).background(p, RoundedCornerShape(3.dp)).border(1.dp, c.line, RoundedCornerShape(3.dp)))
            }
        }
        if (selected) {
            Spacer(Modifier.width(VidaSpacing.xs))
            Icon(Icons.Filled.Check, contentDescription = "Tema activo", tint = c.primary, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Fila que se desliza a la izquierda para completar.
 *
 * Gesto real sobre una acción real —completar una tarea—, no un adorno. Se
 * implementa con `detectHorizontalDragGestures` en lugar de `SwipeToDismissBox`
 * porque aquí la fila NO se descarta: vuelve a su sitio y cambia de estado, que
 * es lo que hace el artefacto.
 */
@Composable
fun SwipeToCompleteRow(
    done: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val shape = RoundedCornerShape(spec.radii.card)
    var offset by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(offset, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "swipe")

    Box(modifier = modifier.fillMaxWidth()) {
        // Fondo revelado: dice exactamente qué va a pasar al soltar.
        Row(
            Modifier
                .matchParentSizeCompat()
                .background(c.successContainer, shape)
                .padding(end = VidaSpacing.lg),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.successText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (done) "Reabrir" else "Hecha",
                style = MaterialTheme.typography.labelLarge,
                color = c.successText,
            )
        }
        Box(
            Modifier
                .offsetX { animated.roundToInt() }
                .pointerInput(done) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offset < -180f) {
                                onToggle()
                            }
                            offset = 0f
                        },
                        onDragCancel = { offset = 0f },
                    ) { _, delta ->
                        offset = (offset + delta).coerceIn(-260f, 0f)
                    }
                },
        ) { content() }
    }
}

/** Desplazamiento horizontal del gesto, sin remedir el contenido. */
private fun Modifier.offsetX(x: () -> Int) = this.offset { IntOffset(x(), 0) }

@Composable
private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxWidth().height(72.dp)

/**
 * Entrada escalonada de una pantalla: cada bloque entra un poco después que el
 * anterior. Es lo que hace que un cambio de pantalla se lea como una llegada y
 * no como un corte.
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, delayMillis = index * 35)) +
            slideInVertically(tween(320, delayMillis = index * 35)) { it / 6 },
        modifier = modifier,
    ) { content() }
}

/** Fila de icono + título + subtítulo + acción, el patrón más repetido. */
@Composable
fun ResourceRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    tone: Color? = null,
    markBackground: Color? = null,
    markTint: Color? = null,
    typeTag: String? = null,
    amount: String? = null,
    pill: Pair<String, PillTone>? = null,
    trailing: (@Composable () -> Unit)? = null,
    strikeThrough: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    VidaRow(modifier = modifier, tone = tone, onClick = onClick) {
        typeTag?.let { VidaTypeTag(it) }
        icon?.let {
            VidaMark(background = markBackground ?: c.primaryContainer, contentColor = markTint ?: c.primary) {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (strikeThrough) c.textTertiary else c.text,
                textDecoration = if (strikeThrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        amount?.let {
            Text(it, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.text)
        }
        pill?.let { VidaPill(it.first, it.second) }
        trailing?.invoke()
    }
}

/**
 * El envoltorio de una hoja modal: título a la izquierda y una X arriba a la
 * derecha.
 *
 * La X sustituye a los botones «Cerrar» de texto. En una hoja, cerrar no es una
 * de las acciones —compite con Editar, Eliminar o Guardar, que sí lo son—, y
 * ponerla arriba a la derecha la saca de esa fila y la deja donde el sistema
 * la pone en todas partes. Lleva `contentDescription`, así que para un lector
 * de pantalla sigue anunciándose como «Cerrar».
 */
@Composable
fun SheetSurface(
    onClose: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = VidaSpacing.lg)
            .padding(bottom = VidaSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = c.text,
                modifier = Modifier.weight(1f).padding(end = VidaSpacing.sm),
            )
            VidaIconButton(Icons.Filled.Close, "Cerrar", onClick = onClose)
        }
        content()
    }
}
