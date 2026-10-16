package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.Dialogo
import com.vidacotidiana.app.core.app.ResourceAccessController
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.data.Document
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaVocabulary
import com.vidacotidiana.app.navigation.Routes
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * KIT DE LISTAS Y FICHAS — nació en Documentos (propuesta aprobada el
 * 2026-09-26) y se extiende a todas las secciones (ADR-028): tarjeta suave,
 * sello de tono, cabecera flotante, título de bloque, buscador, chips y
 * «Cargar más». Una sola pieza por concepto para que una garantía y un recibo
 * se lean con la misma gramática.
 */

public data class ToneColors(val fg: Color, val bg: Color)

/* ══════════════════════════════════════════════════════════════════════════
   SUPERFICIES
   ══════════════════════════════════════════════════════════════════════════ */

/** Tarjeta blanca con filete fino y sombra suave, la de todos los cuadros. */
@Composable
fun Modifier.softCard(background: Color = VidaTheme.colors.surface, corner: Dp = 18.dp): Modifier {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(corner)
    return this
        .vidaSoftShadow(corner = corner, keyBlur = 16.dp, keyOffsetY = 4.dp)
        .background(background, shape)
        .border(1.dp, c.border.copy(alpha = 0.18f), shape)
}

/** Cuadro de icono con su tono: el «sello» de cada fila y de cada mosaico. */
@Composable
fun ToneMark(icon: ImageVector, tone: ToneColors, size: Dp = 44.dp, iconSize: Dp = 22.dp, circle: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .background(tone.bg, if (circle) CircleShape else RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(iconSize))
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   CABECERA
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Cabecera de las vistas interiores: atrás, título en negrita y, debajo, la
 * cifra de la colección. `trailing` deja sitio a la ilustración de categoría o
 * a las acciones del detalle.
 */
@Composable
fun DetailTopBar(
    title: String?,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val c = VidaTheme.colors
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VidaIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = c.text, onClick = onBack)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 1)
            }
        }
        trailing()
    }
}

/** «Mis categorías · Ver todas ›». */
@Composable
fun ListSectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val c = VidaTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = c.text,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = c.primary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   BUSCADOR
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Caja de búsqueda. Editable o no: en el resumen es un ATAJO que abre la
 * búsqueda global (`onClick`), y dentro de una colección o de la búsqueda es
 * el campo de verdad.
 */
@Composable
fun ListSearchBox(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(c.sunken, shape)
            .border(1.dp, c.border.copy(alpha = 0.14f), shape)
            .then(if (onClick != null) Modifier.clip(shape).clickable(role = Role.Button, onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 50.dp)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.textTertiary, maxLines = 1)
            }
            if (onValueChange != null) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge).copy(color = c.text),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                )
            }
        }
        if (trailing != null) trailing() else Spacer(Modifier.width(10.dp))
    }
}

@Composable
fun SearchClearButton(onClick: () -> Unit) {
    VidaIconButton(Icons.Outlined.Close, "Borrar búsqueda", tint = VidaTheme.colors.primary, onClick = onClick)
}

/* ══════════════════════════════════════════════════════════════════════════
   CHIPS
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Chip de filtro de la propuesta: relleno índigo al elegirlo, filete fino en
 * reposo. `dropdown` añade la flecha del chip «Más».
 */
@Composable
fun ListChip(label: String, selected: Boolean, onClick: () -> Unit, dropdown: Boolean = false) {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) c.primary else c.surface, shape)
            .border(1.dp, if (selected) c.primary else c.border.copy(alpha = 0.22f), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .defaultMinSize(minHeight = 38.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = if (selected) c.onPrimary else c.textSecondary,
        )
        if (dropdown) {
            Icon(
                Icons.Outlined.KeyboardArrowDown, null,
                tint = if (selected) c.onPrimary else c.textSecondary,
                modifier = Modifier.padding(start = 2.dp).size(16.dp),
            )
        }
    }
}

/** «Mostrando 8 de 42 · Cargar más ↓». */
@Composable
fun LoadMoreFooter(shown: Int, total: Int, onMore: () -> Unit) {
    val c = VidaTheme.colors
    Row(
        Modifier.fillMaxWidth().softCard(c.sunken).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Mostrando $shown de $total", style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
        if (shown < total) {
            androidx.compose.material3.OutlinedButton(
                onClick = onMore,
                border = BorderStroke(1.dp, c.primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(50),
            ) {
                Text("Cargar más", style = MaterialTheme.typography.labelLarge, color = c.primary)
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = c.primary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
            }
        }
    }
}


private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * «25 sep 2026». Los meses van escritos a mano porque el formateador de Java
 * cambia según la versión de Android («sept.», «sep.»).
 */
fun shortDateEs(d: java.time.LocalDate): String = "${d.dayOfMonth} ${MESES_CORTOS[d.monthValue - 1]} ${d.year}"

/** «25 sep 2026 · 10:24 a. m.» */
fun shortDateTimeEs(t: java.time.LocalDateTime): String {
    val h12 = (t.hour % 12).let { if (it == 0) 12 else it }
    return "${shortDateEs(t.toLocalDate())} · $h12:${"%02d".format(t.minute)} ${if (t.hour < 12) "a. m." else "p. m."}"
}

/* ══ Piezas del RESUMEN de una sección (Documentos, Compartidos) ══ */

/** Una cifra del resumen: icono, número y unidad. */
@Composable
fun SummaryStatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    Column(
        modifier.softCard().padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            color = c.text,
            maxLines = 1,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1)
    }
}

/** Recientes · Favoritos · Compartidos. */
@Composable
fun QuickAccessTile(
    icon: ImageVector,
    label: String,
    count: Int,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        modifier
            .softCard(tone.bg.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(24.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("($count)", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/** Tarjeta de categoría, en el color de su categoría («Mis categorías»). */
@Composable
fun CategoryTile(
    icon: ImageVector,
    label: String,
    caption: String,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tone.bg.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToneMark(icon, ToneColors(tone.fg, c.surface.copy(alpha = 0.75f)), size = 38.dp, iconSize = 20.dp)
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = tone.fg)
        }
    }
}
