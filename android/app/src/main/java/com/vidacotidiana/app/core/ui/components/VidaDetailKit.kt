package com.vidacotidiana.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.FormField
import com.vidacotidiana.app.core.app.ReferenceSource
import com.vidacotidiana.app.core.ui.SectionVisual
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.VidaTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/*
 * ════════════════════════════════════════════════════════════════════════════
 *  EL DETALLE DE UN REGISTRO — «ver todo, editar en el sitio»
 *  (artefacto «Inicio en cinco zonas», pestaña Detalles, aprobado 2026-09-27)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Antes cada detalle ponía sus botones en un sitio distinto —en la barra de
 * arriba, al final bajo los adjuntos, o en baldosas— y «Editar» abría OTRA
 * pantalla con el formulario. Ahora todos los detalles se arman con estas
 * piezas:
 *
 *  · `VidaDetailPage`       — barra de arriba, contenido con scroll y la barra
 *                             de acción FIJA ABAJO: el scroll nunca la pierde.
 *  · `VidaDetailHeroCard`   — baldosa del módulo, antetítulo, cifra, estado y
 *                             el título con su lápiz (se edita ahí mismo).
 *  · `VidaDetailDock`       — acción principal en la primera celda (azul
 *                             tinta), dos frecuentes y «Más».
 *  · `VidaInfoSection`      — TODOS los campos del registro, cada valor con su
 *                             lápiz. Fecha y hora abren el calendario y el
 *                             reloj de siempre (`VidaDateDialog`/`VidaTimeDialog`).
 *  · `InlineSaver`          — guarda un valor por el camino de edición de
 *                             siempre y confirma con «X actualizado · Deshacer».
 *
 * No hay modo de edición ni botón general de guardar: cada cambio se guarda al
 * confirmarlo y se puede deshacer.
 */

private val HeroInk = Color(0xFF2B2870)
private val HeroInkNight = Color(0xFF4A4DA0)
private val SnackBg = Color(0xFF1E1B4B)
private val SnackAccent = Color(0xFFCFEAFB)

@Composable
private fun actionFill(): Color = if (VidaTheme.spec.isDark) HeroInkNight else HeroInk

private val uiFont = VidaFonts.Sora

/* ══════════════════════════════════════════════════════════════════════════
   AVISO CON «DESHACER»
   ══════════════════════════════════════════════════════════════════════════ */

class DetailSnack(val message: String, val undo: (() -> Unit)?, val key: Long)

class DetailSnackState {
    var current by mutableStateOf<DetailSnack?>(null)
        private set

    fun show(message: String, undo: (() -> Unit)? = null) {
        current = DetailSnack(message, undo, System.nanoTime())
    }

    fun dismiss() { current = null }
}

@Composable
fun rememberDetailSnack(): DetailSnackState = remember { DetailSnackState() }

@Composable
private fun DetailSnackHost(state: DetailSnackState, modifier: Modifier) {
    val snack = state.current
    LaunchedEffect(snack?.key) {
        if (snack != null) {
            delay(3600)
            if (state.current?.key == snack.key) state.dismiss()
        }
    }
    AnimatedVisibility(
        visible = snack != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(160)),
        modifier = modifier,
    ) {
        val s = snack ?: return@AnimatedVisibility
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(16.dp))
                .background(SnackBg, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                .heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                s.message,
                style = TextStyle(fontFamily = VidaFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (s.undo != null) {
                Text(
                    "Deshacer",
                    style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = SnackAccent,
                    modifier = Modifier
                        .vidaClickable(onClick = { state.dismiss(); s.undo.invoke() })
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   LA PÁGINA
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * El esqueleto de todo detalle. La barra de acción va FIJA ABAJO, fuera del
 * scroll —sobre una tarjeta con sombra y un velo del fondo para que el
 * contenido pase por debajo sin chocar—, así que la acción principal está a
 * un toque desde cualquier punto de la pantalla. Con el teclado abierto se
 * aparta para no tapar el campo que se está escribiendo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VidaDetailPage(
    title: String,
    onBack: () -> Unit,
    snack: DetailSnackState,
    dock: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = VidaTheme.colors
    val keyboard = WindowInsets.isImeVisible
    val showDock = dock != null && !keyboard
    Box(Modifier.fillMaxSize().background(c.surface)) {
        Column(Modifier.fillMaxSize()) {
            VidaAppBar(title = title, showBack = true, onNavigationClick = onBack, actions = actions)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                content()
                // Aire para que lo último no quede bajo la barra fija.
                Spacer(Modifier.height(if (dock != null) 112.dp else 32.dp))
            }
        }
        if (showDock) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.28f to c.surface))
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 14.dp),
            ) { dock?.invoke() }
        }
        DetailSnackHost(
            snack,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = if (showDock) 96.dp else 18.dp),
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   LÁPIZ
   ══════════════════════════════════════════════════════════════════════════ */

@Composable
fun DetailPen(
    icon: ImageVector,
    contentDescription: String,
    size: Dp = 40.dp,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(if (size < 40.dp) 12.dp else 14.dp)
    Box(
        Modifier
            .size(size)
            .background(if (filled) actionFill() else c.surfaceVariant, shape)
            .border(1.dp, if (filled) actionFill() else c.line, shape)
            .vidaClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (filled) Color.White else c.textSecondary,
            modifier = Modifier.size(if (size < 40.dp) 17.dp else 18.dp),
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   CABECERA
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Baldosa del módulo, antetítulo y cifra; debajo, el título con su lápiz y el
 * estado. El lápiz vuelve el título un campo EN EL MISMO SITIO: la palomita (o
 * «Listo» del teclado) guarda y la equis cancela. Vacío no se guarda.
 */
@Composable
fun VidaDetailHeroCard(
    section: SectionVisual,
    eyebrow: String,
    title: String,
    onRename: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    tile: (@Composable () -> Unit)? = null,
    figure: String? = null,
    figureCaption: String? = null,
    figureTone: Color? = null,
    status: String? = null,
    statusColor: Color? = null,
    onEmptyTitle: () -> Unit = {},
) {
    val c = VidaTheme.colors
    var editing by remember { mutableStateOf(false) }
    var draft by remember(title) { mutableStateOf(title) }
    val focus = remember { FocusRequester() }

    fun save() {
        val clean = draft.trim()
        editing = false
        if (clean.isEmpty()) { draft = title; onEmptyTitle(); return }
        if (clean != title) onRename?.invoke(clean)
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(c.surfaceVariant, RoundedCornerShape(24.dp))
            .border(1.dp, if (editing) actionFill() else c.line, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tile != null) tile() else SectionTile(section, size = 48.dp)
            Text(
                eyebrow.uppercase(),
                style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.12.em),
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (figure != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        figure,
                        style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.03).em),
                        color = figureTone ?: c.text,
                        maxLines = 1,
                    )
                    if (figureCaption != null) {
                        Text(
                            figureCaption,
                            style = TextStyle(fontFamily = VidaFonts.Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                            color = figureTone ?: c.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val titleStyle = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em)
            if (editing) {
                LaunchedEffect(Unit) { delay(80); runCatching { focus.requestFocus() } }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = titleStyle.copy(color = c.text),
                    cursorBrush = SolidColor(c.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier
                        .weight(1f)
                        .background(c.surface, RoundedCornerShape(14.dp))
                        .border(1.5.dp, actionFill(), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .focusRequester(focus),
                )
                DetailPen(Icons.Outlined.Check, "Guardar título", filled = true) { save() }
                DetailPen(Icons.Outlined.Close, "Cancelar") { draft = title; editing = false }
            } else {
                Text(title, style = titleStyle, color = c.text, modifier = Modifier.weight(1f))
                if (onRename != null) {
                    DetailPen(Icons.Outlined.Edit, "Editar el título") { draft = title; editing = true }
                }
            }
        }

        if (status != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(statusColor ?: c.primary, CircleShape))
                Text(
                    status,
                    style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.text,
                )
            }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   BARRA DE ACCIÓN
   ══════════════════════════════════════════════════════════════════════════ */

data class DockAction(
    val label: String,
    val icon: ImageVector,
    val done: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

data class MoreAction(
    val label: String,
    val icon: ImageVector,
    val caption: String? = null,
    val danger: Boolean = false,
    /** Si no es nulo, se pide confirmación con este texto antes de hacerlo. */
    val confirm: String? = null,
    val onClick: () -> Unit,
)

/**
 * La barra de acción: la principal SIEMPRE en la primera celda y más ancha;
 * luego hasta dos frecuentes y «Más» con el resto (lo destructivo, siempre
 * ahí y siempre con confirmación).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VidaDetailDock(primary: DockAction, secondary: List<DockAction> = emptyList(), more: List<MoreAction> = emptyList()) {
    val c = VidaTheme.colors
    var sheet by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = Color(0x1F101322), spotColor = Color(0x1F101322))
            .background(c.surfaceVariant, shape)
            .border(1.dp, c.line, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val sec = secondary.take(2)
        val primaryWeight = if (sec.size + (if (more.isNotEmpty()) 1 else 0) >= 3) 2.1f else 2f
        PrimaryCell(primary, Modifier.weight(primaryWeight))
        sec.forEach { SecondaryCell(it.label, it.icon, it.enabled, Modifier.weight(1f), it.onClick) }
        if (more.isNotEmpty()) SecondaryCell("Más", Icons.Outlined.MoreHoriz, true, Modifier.weight(1f)) { sheet = true }
    }

    if (sheet) {
        var confirming by remember { mutableStateOf<MoreAction?>(null) }
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceVariant,
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 22.dp)) {
                val pending = confirming
                if (pending == null) {
                    Text(
                        "Más acciones",
                        style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = c.text,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    more.forEachIndexed { i, a ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                        MoreRow(a) {
                            if (a.confirm != null) confirming = a else { sheet = false; a.onClick() }
                        }
                    }
                } else {
                    Text(
                        pending.label,
                        style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = c.text,
                    )
                    Text(
                        pending.confirm.orEmpty(),
                        style = TextStyle(fontFamily = VidaFonts.Inter, fontSize = 13.5.sp, lineHeight = 19.sp),
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SheetButton("Cancelar", Modifier.weight(1f), fill = null) { confirming = null }
                        SheetButton(
                            pending.label.substringBefore(' '),
                            Modifier.weight(1f),
                            fill = if (pending.danger) c.error else actionFill(),
                        ) { sheet = false; pending.onClick() }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryCell(a: DockAction, modifier: Modifier) {
    val c = VidaTheme.colors
    val bg = if (a.done) c.successContainer else actionFill()
    val fg = if (a.done) c.successText else Color.White
    val iconTint = if (a.done) c.successText else SnackAccent
    Row(
        modifier
            .height(58.dp)
            .alpha(if (a.enabled) 1f else 0.5f)
            .background(bg, RoundedCornerShape(20.dp))
            .then(if (a.enabled) Modifier.vidaClickable(onClick = a.onClick) else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(a.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(
            a.label,
            style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SecondaryCell(label: String, icon: ImageVector, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Column(
        modifier
            .height(58.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .then(if (enabled) Modifier.vidaClickable(onClick = onClick) else Modifier)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = TextStyle(fontFamily = VidaFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoreRow(a: MoreAction, onClick: () -> Unit) {
    val c = VidaTheme.colors
    val tint = if (a.danger) c.error else c.textSecondary
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).vidaClickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).background(if (a.danger) c.errorContainer else c.sunken, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(a.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(
                a.label,
                style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = if (a.danger) c.error else c.text,
            )
            a.caption?.let {
                Text(it, style = TextStyle(fontFamily = VidaFonts.Inter, fontSize = 12.sp), color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun SheetButton(label: String, modifier: Modifier, fill: Color?, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Box(
        modifier
            .height(48.dp)
            .background(fill ?: c.surfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, fill ?: c.line, RoundedCornerShape(16.dp))
            .vidaClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
            color = if (fill != null) Color.White else c.text,
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   TÍTULO DE ZONA (el mismo de Inicio)
   ══════════════════════════════════════════════════════════════════════════ */

@Composable
fun DetailZoneTitle(title: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    val c = VidaTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em),
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                trailing,
                style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = if (onTrailing != null) 12.5.sp else 11.sp),
                color = if (onTrailing != null) c.primary else c.textSecondary,
                modifier = if (onTrailing != null) Modifier.vidaClickable(onClick = onTrailing).padding(vertical = 4.dp) else Modifier,
            )
        }
    }
}

/** La tarjeta de zona: radio 24, filete y margen de filas. */
@Composable
fun DetailZoneCard(content: @Composable ColumnScope.() -> Unit) {
    val c = VidaTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surfaceVariant, RoundedCornerShape(24.dp))
            .border(1.dp, c.line, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 2.dp),
        content = content,
    )
}

/* ══════════════════════════════════════════════════════════════════════════
   INFORMACIÓN — un lápiz por valor
   ══════════════════════════════════════════════════════════════════════════ */

sealed interface InfoEdit {
    /** Se edita en la fila con las piezas del formulario de alta (opciones, texto, número, vínculo). */
    data class Field(val field: FormField, val raw: String) : InfoEdit

    /** Fecha: el lápiz abre el calendario de siempre. `raw` en ISO. */
    data class Date(val raw: String) : InfoEdit

    /** El DÍA de una fecha con hora: conserva la hora; sin hora, pide también la hora. `raw` = `2026-05-12T14:30`. */
    data class DateOfMoment(val raw: String) : InfoEdit

    /** La HORA de una fecha con hora: el reloj de siempre. Sin día, cuenta hoy. */
    data class TimeOfMoment(val raw: String) : InfoEdit

    /** El lápiz hace algo propio (elegir otro archivo, por ejemplo). */
    data class Action(val icon: ImageVector, val onClick: () -> Unit) : InfoEdit

    /** Sin lápiz; en su lugar, de dónde sale el dato. */
    data class ReadOnly(val note: String? = null) : InfoEdit
}

data class InfoItem(
    val key: String,
    val label: String,
    val value: String?,
    val edit: InfoEdit,
    val onSave: (String) -> Unit = {},
)

/** De dónde salen las opciones de los vínculos (personas, proyectos, artículos). */
data class InfoRefs(
    val people: List<Pair<String, String>> = emptyList(),
    val projects: List<Pair<String, String>> = emptyList(),
    val inventory: List<Pair<String, String>> = emptyList(),
    val onQuickCreate: (ReferenceSource, String, String?, (String?) -> Unit) -> Unit = { _, _, _, done -> done(null) },
)

/**
 * Abrir una fila desde fuera —la barra de acción—, con la sección a la vista.
 * Es lo que hace «Persona» en Laboral: el vínculo se elige en su propia fila,
 * no en otra ventana.
 */
class InfoOpener {
    var request by mutableStateOf<Pair<String, Long>?>(null)
        private set

    fun open(key: String) { request = key to System.nanoTime() }
}

@Composable
fun rememberInfoOpener(): InfoOpener = remember { InfoOpener() }

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VidaInfoSection(
    items: List<InfoItem>,
    refs: InfoRefs = InfoRefs(),
    flash: Pair<String, Long>? = null,
    title: String = "Información",
    opener: InfoOpener? = null,
) {
    val c = VidaTheme.colors
    var open by remember { mutableStateOf<String?>(null) }
    val inView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    LaunchedEffect(opener?.request) {
        val key = opener?.request?.first ?: return@LaunchedEffect
        open = key
        delay(220)
        runCatching { inView.bringIntoView() }
    }
    var dateFor by remember { mutableStateOf<InfoItem?>(null) }
    var timeFor by remember { mutableStateOf<InfoItem?>(null) }
    /** Día elegido para una fecha con hora que todavía no tenía hora. */
    var pendingDay by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.bringIntoViewRequester(inView),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailZoneTitle(title, "Toca el lápiz para cambiar")
        DetailZoneCard {
            items.forEachIndexed { i, item ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                val isOpen = open == item.key
                InfoRowView(
                    item = item,
                    open = isOpen,
                    flashToken = flash?.takeIf { it.first == item.key }?.second,
                    onPen = {
                        when (val e = item.edit) {
                            is InfoEdit.Field -> open = if (isOpen) null else item.key
                            is InfoEdit.Date, is InfoEdit.DateOfMoment -> { open = null; dateFor = item }
                            is InfoEdit.TimeOfMoment -> { open = null; timeFor = item }
                            is InfoEdit.Action -> { open = null; e.onClick() }
                            is InfoEdit.ReadOnly -> Unit
                        }
                    },
                )
                val edit = item.edit
                if (isOpen && edit is InfoEdit.Field) {
                    InlineDrawer(edit, refs, onSave = { v -> open = null; if (v != edit.raw) item.onSave(v) }, onCancel = { open = null })
                }
            }
        }
    }

    dateFor?.let { item ->
        val e = item.edit
        val raw = when (e) { is InfoEdit.Date -> e.raw; is InfoEdit.DateOfMoment -> e.raw; else -> "" }
        VidaDateDialog(
            initial = raw.substringBefore('T'),
            onDismiss = { dateFor = null },
            onPick = { day ->
                dateFor = null
                if (e is InfoEdit.DateOfMoment) {
                    val hora = e.raw.substringAfter('T', "")
                    if (hora.isBlank()) {
                        // Una fecha con hora no se guarda a medias: se pide la hora ya.
                        pendingDay = day
                        timeFor = item
                    } else {
                        item.onSave(day + "T" + hora)
                    }
                } else {
                    item.onSave(day)
                }
            },
        )
    }

    timeFor?.let { item ->
        val raw = when (val e = item.edit) { is InfoEdit.TimeOfMoment -> e.raw; is InfoEdit.DateOfMoment -> e.raw; else -> "" }
        VidaTimeDialog(
            initial = raw.substringAfter('T', "").ifBlank { null },
            onDismiss = { timeFor = null; pendingDay = null },
            onPick = { hhmm ->
                val day = pendingDay ?: raw.substringBefore('T').ifBlank { LocalDate.now().toString() }
                timeFor = null
                pendingDay = null
                item.onSave(day + "T" + hhmm)
            },
        )
    }
}

@Composable
private fun InfoRowView(item: InfoItem, open: Boolean, flashToken: Long?, onPen: () -> Unit) {
    val c = VidaTheme.colors
    val glow = remember { Animatable(0f) }
    val motion = motionEnabled()
    LaunchedEffect(flashToken) {
        if (flashToken != null && motion) {
            glow.snapTo(0.35f)
            glow.animateTo(0f, tween(1200))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .background(Color(0xFF7DC2ED).copy(alpha = glow.value), RoundedCornerShape(10.dp))
            .then(if (item.edit !is InfoEdit.ReadOnly) Modifier.vidaClickable(onClick = onPen) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.label,
                style = TextStyle(
                    fontFamily = VidaFonts.Inter,
                    fontWeight = if (open) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                color = if (open) c.primary else c.textSecondary,
            )
            val v = item.value?.takeIf { it.isNotBlank() }
            if (v != null) {
                Text(
                    v,
                    style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 19.sp),
                    color = c.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    "Sin " + item.label.lowercase(),
                    style = TextStyle(fontFamily = VidaFonts.Inter, fontWeight = FontWeight.Medium, fontSize = 13.5.sp),
                    color = c.textTertiary,
                )
            }
        }
        when (val e = item.edit) {
            is InfoEdit.ReadOnly -> e.note?.let {
                Text(
                    it,
                    style = TextStyle(fontFamily = VidaFonts.Inter, fontSize = 11.sp),
                    color = c.textTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 110.dp),
                )
            }
            else -> DetailPen(
                icon = when (e) {
                    is InfoEdit.Date, is InfoEdit.DateOfMoment -> Icons.Outlined.CalendarMonth
                    is InfoEdit.TimeOfMoment -> Icons.Outlined.Schedule
                    is InfoEdit.Action -> e.icon
                    else -> Icons.Outlined.Edit
                },
                contentDescription = "Cambiar " + item.label.lowercase(),
                size = 36.dp,
                filled = open,
                onClick = onPen,
            )
        }
    }
}

/** El cajón de una fila: las MISMAS piezas del formulario de alta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineDrawer(edit: InfoEdit.Field, refs: InfoRefs, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val values = remember(edit) { mutableStateMapOf(edit.field.key to edit.raw) }
    val typed = edit.field is FormField.Text || edit.field is FormField.Number
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldDrawer(
            field = edit.field,
            values = values,
            people = refs.people,
            projects = refs.projects,
            inventory = refs.inventory,
            // Elegir una opción ya ES la respuesta: se guarda sin más botones.
            onAnswer = { _, v -> if (typed) values[edit.field.key] = v else onSave(v) },
            onPartial = { k, v -> values[k] = v },
            onOpenCalendar = {},
            onOpenClock = {},
            onQuickCreate = refs.onQuickCreate,
        )
        if (typed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                RowButton("Cancelar", filled = false, onClick = onCancel)
                RowButton("Guardar", filled = true) { onSave(values[edit.field.key].orEmpty().trim()) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                RowButton("Cancelar", filled = false, onClick = onCancel)
            }
        }
    }
}

@Composable
private fun RowButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Box(
        Modifier
            .height(38.dp)
            .background(if (filled) actionFill() else c.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, if (filled) actionFill() else c.line, RoundedCornerShape(12.dp))
            .vidaClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = uiFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = if (filled) Color.White else c.text,
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   DE LA DECLARACIÓN DEL FORMULARIO A LAS FILAS
   ══════════════════════════════════════════════════════════════════════════ */

private val ES = Locale("es", "MX")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", ES)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", ES)

/** «Hoy · 27 sep», «Mañana · 28 sep», «vie 3 oct», «vie 3 oct 2027». */
fun infoDate(day: LocalDate, today: LocalDate = LocalDate.now()): String {
    val short = day.format(DAY_MONTH).trimEnd('.')
    return when (day) {
        today -> "Hoy · $short"
        today.plusDays(1) -> "Mañana · $short"
        today.minusDays(1) -> "Ayer · $short"
        else -> day.dayOfWeek.getDisplayName(DateTextStyle.SHORT, ES).trimEnd('.') + " " +
            (if (day.year == today.year) short else day.format(DAY_MONTH_YEAR).replace(".", ""))
    }
}

/** El rótulo de la fila: el del formulario sin la pregunta («¿Dónde?» → «Dónde»). */
fun FormField.infoLabel(): String = when (key) {
    "inventoryItemId" -> "Artículo"
    "personId" -> "Persona"
    "projectId" -> "Proyecto"
    "direction" -> "Depende de"
    else -> label.trim('¿', '?', ' ').replaceFirstChar { it.uppercase() }
}

/** El valor como lo lee una persona. Nulo = vacío. */
fun infoValue(field: FormField, raw: String?, refs: InfoRefs): String? {
    val v = raw?.trim().orEmpty()
    if (v.isEmpty()) return null
    return when (field) {
        is FormField.Choice -> field.options.firstOrNull { it.first == v }?.second ?: v
        is FormField.Date -> runCatching { infoDate(LocalDate.parse(v.take(10))) }.getOrDefault(v)
        is FormField.DateTime -> runCatching { infoDate(LocalDate.parse(v.take(10))) }.getOrDefault(v)
        is FormField.Reference -> when (field.source) {
            ReferenceSource.PEOPLE -> refs.people
            ReferenceSource.PROJECTS -> refs.projects
            ReferenceSource.INVENTORY -> refs.inventory
        }.firstOrNull { it.first == v }?.second ?: v
        else -> v
    }
}

/**
 * Las filas de TODOS los campos del formulario de alta, menos el título (que
 * tiene su lápiz en la cabecera) y el archivo (que cada detalle trata aparte).
 * Una fecha con hora se parte en dos filas —Día y Hora— porque cada una abre
 * su selector.
 */
fun infoItemsFor(
    resource: CreatableResource,
    values: Map<String, String>,
    refs: InfoRefs,
    saver: InlineSaver,
    format: (FormField, String) -> String? = { f, raw -> infoValue(f, raw, refs) },
    /** El módulo DEL REGISTRO: en Personal no hay Persona ni Proyecto. */
    laboral: Boolean = true,
): List<InfoItem> {
    val head = resource.headField?.key
    return resource.fieldsFor(laboral).filter { it.key != head && it !is FormField.File }.flatMap { f ->
        val raw = values[f.key].orEmpty()
        val label = f.infoLabel()
        when (f) {
            is FormField.DateTime -> {
                val hora = raw.substringAfter('T', "").takeIf { it.isNotBlank() }
                listOf(
                    InfoItem(f.key + "#dia", "Día", infoValue(f, raw, refs), InfoEdit.DateOfMoment(raw)) { saver.save(f.key, "Día", it, raw, f.key + "#dia") },
                    InfoItem(f.key + "#hora", "Hora", hora, InfoEdit.TimeOfMoment(raw)) { saver.save(f.key, "Hora", it, raw, f.key + "#hora") },
                )
            }
            is FormField.Date -> listOf(InfoItem(f.key, label, format(f, raw), InfoEdit.Date(raw)) { saver.save(f.key, label, it, raw) })
            else -> listOf(InfoItem(f.key, label, format(f, raw), InfoEdit.Field(f, raw)) { saver.save(f.key, label, it, raw) })
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   GUARDAR UN VALOR
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Guarda un valor por el camino de edición de siempre (`saveField`) y lo
 * confirma: la fila parpadea y aparece «Prioridad actualizada · Deshacer».
 * Deshacer vuelve a guardar el valor anterior por el mismo camino.
 */
class InlineSaver(
    private val viewModel: AppViewModel,
    private val resource: CreatableResource,
    private val id: String,
    private val snack: DetailSnackState,
) {
    var flash by mutableStateOf<Pair<String, Long>?>(null)
        private set

    fun save(key: String, label: String, new: String, old: String, flashKey: String = key) {
        if (new == old) return
        viewModel.saveField(resource, id, key, new) { ok ->
            if (ok) {
                flash = flashKey to System.nanoTime()
                snack.show("$label ${actualizado(label)}", undo = {
                    viewModel.saveField(resource, id, key, old) { back ->
                        if (back) { flash = flashKey to System.nanoTime(); snack.show("Cambio deshecho") }
                        else snack.show("No se pudo deshacer")
                    }
                })
            } else {
                snack.show("No se pudo guardar. Revisa tu conexión.")
            }
        }
    }
}

@Composable
fun rememberInlineSaver(
    viewModel: AppViewModel,
    resource: CreatableResource,
    id: String,
    snack: DetailSnackState,
): InlineSaver = remember(resource, id) { InlineSaver(viewModel, resource, id, snack) }

/** «Prioridad actualizada», «Importe actualizado». Concuerda con el sustantivo que manda. */
internal fun actualizado(label: String): String {
    val head = label.lowercase().substringBefore(' ')
    val masc = setOf("día", "cada", "detalle", "título", "nombre", "importe")
    val fem = head !in masc && (head.endsWith("a") || head.endsWith("ad") || head.endsWith("ión") || head == "vez" || head == "hora")
    return if (fem) "actualizada" else "actualizado"
}

/** Las opciones de los vínculos, sacadas de los datos que ya están cargados. */
fun infoRefsOf(state: com.vidacotidiana.app.core.app.AppUiState, viewModel: AppViewModel): InfoRefs = InfoRefs(
    people = state.data.people.map { it.id to it.name },
    projects = state.data.projects.map { it.id to it.name },
    // El nombre solo no basta: dos artículos pueden llamarse igual en dos sitios.
    inventory = state.data.inventory.map {
        it.id to (if (it.location.isNullOrBlank()) it.name else "${it.name} · ${it.location}")
    },
    onQuickCreate = viewModel::quickCreate,
)

/** La sección de adjuntos (V37) con el título de zona, para todos los detalles. */
@Composable
fun DetailAttachments(
    state: com.vidacotidiana.app.core.app.AppUiState,
    viewModel: AppViewModel,
    to: com.vidacotidiana.app.core.data.AttachTo,
    id: String,
    modifier: Modifier = Modifier,
) {
    val files = if (state.attachmentsFor == id) state.attachments else emptyList()
    val attachedIds = files.map { it.id }.toSet()
    val docs = state.data.documents.filterNot { it.id in attachedIds }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailZoneTitle("Adjuntos", files.size.takeIf { it > 0 }?.toString())
        VidaAttachments(
            attached = files.map {
                VidaFile(
                    it.id, it.name,
                    "${it.contentType.substringAfterLast('/').uppercase()} · ${it.sizeBytes / 1024} KB",
                    it.isImage,
                )
            },
            loose = docs.filter { it.resourceType == null }.map {
                VidaFile(it.id, it.name, "${it.category} · ${it.sizeLabel}", false)
            },
            hangingElsewhere = docs.count { it.resourceType != null },
            loading = state.attachmentsLoading,
            onAttach = { viewModel.attachDocument(it, to, id) },
            onDetach = { viewModel.detachDocument(it, to, id) },
        )
    }
}

/** Sin permiso de edición: las mismas filas, sin lápiz. */
fun List<InfoItem>.readOnly(note: String? = null): List<InfoItem> = map { it.copy(edit = InfoEdit.ReadOnly(note)) }
