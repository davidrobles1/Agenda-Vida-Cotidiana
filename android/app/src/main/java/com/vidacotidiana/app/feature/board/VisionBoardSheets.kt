package com.vidacotidiana.app.feature.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.data.BoardElement
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.VidaTextField
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Las hojas del Vision Board: una por herramienta, con el mismo recorrido
 * que la Web —una galería que lleva al paso de cada tipo, y de ahí se
 * vuelve— pero en el gesto que le corresponde a un teléfono, que es una hoja
 * que sube desde abajo y no un panel flotante.
 *
 * Ninguna decide nada por su cuenta: reciben lo que hay y devuelven lo que
 * el usuario eligió. El estado vive en `VisionBoardViewModel`.
 */

/* =========================================================
   PIEZAS COMUNES
   ========================================================= */

@Composable
fun SheetHeader(title: String, onBack: (() -> Unit)? = null, onClose: () -> Unit) {
    val c = VidaTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(bottom = VidaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconSquare(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", onBack)
            Spacer(Modifier.width(VidaSpacing.xs))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            color = c.text,
            modifier = Modifier.weight(1f),
        )
        IconSquare(Icons.Outlined.Close, "Cerrar", onClose)
    }
}

@Composable
private fun IconSquare(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .background(c.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = c.text)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
        color = VidaTheme.colors.textSecondary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/** Píldora de opción — el mismo control que la Web usa para marcos, temas y variantes. */
@Composable
fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = VidaTheme.colors
    Box(
        modifier
            .heightIn(min = 44.dp)
            .background(if (selected) c.primaryContainer else c.surface, RoundedCornerShape(999.dp))
            .border(1.dp, if (selected) c.primary else c.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = if (selected) c.primary else c.text,
        )
    }
}

/** Baldosa de catálogo: icono arriba, nombre debajo. */
@Composable
fun CatalogTile(label: String, icon: ImageVector, onClick: () -> Unit, highlighted: Boolean = false) {
    val c = VidaTheme.colors
    Column(
        Modifier
            .background(if (highlighted) c.primaryContainer else c.surface, RoundedCornerShape(17.dp))
            .border(1.dp, if (highlighted) c.primary else c.line, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (highlighted) c.primary else c.text)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
            color = if (highlighted) c.primary else c.text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    val c = VidaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SquareButton("−", "Reducir $label") { onChange((value - TEXT_FONT_SIZE_STEP).coerceAtLeast(MIN_TEXT_FONT_SIZE)) }
        Text(
            "$value pt",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = c.text,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center,
        )
        SquareButton("+", "Aumentar $label") { onChange((value + TEXT_FONT_SIZE_STEP).coerceAtMost(MAX_TEXT_FONT_SIZE)) }
    }
}

@Composable
private fun SquareButton(glyph: String, label: String, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .background(c.surface, RoundedCornerShape(15.dp))
            .border(1.dp, c.line, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = MaterialTheme.typography.titleMedium, color = c.text)
    }
}

/* =========================================================
   1 · BIBLIOTECA DE ELEMENTOS
   ========================================================= */

data class LibraryItem(val id: String, val label: String, val icon: ImageVector)

val LIBRARY_ITEMS = listOf(
    LibraryItem("TEXT", "Texto", Icons.Outlined.TextFields),
    LibraryItem("NOTE", "Nota", Icons.Outlined.StickyNote2),
    LibraryItem("STICKER", "Sticker", Icons.Outlined.Star),
    LibraryItem("EMOJI", "Emojis", Icons.Outlined.SentimentSatisfied),
    LibraryItem("IMAGE", "Imagen", Icons.Outlined.Image),
    LibraryItem("SHAPE", "Forma", Icons.Outlined.Category),
    LibraryItem("TABLE", "Tabla", Icons.Outlined.TableChart),
    LibraryItem("CHART", "Gráfica", Icons.Outlined.BarChart),
    LibraryItem("GRID", "Cuadrícula", Icons.Outlined.GridView),
)

@Composable
fun LibrarySheet(onPick: (String) -> Unit, onClose: () -> Unit) {
    val c = VidaTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Elementos", onClose = onClose)
        Text(
            "Elige qué quieres agregar al Vision Board.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = VidaSpacing.md),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            items(LIBRARY_ITEMS) { item ->
                CatalogTile(
                    item.label, item.icon, { onPick(item.id) },
                    highlighted = item.id == "IMAGE" || item.id == "TEXT",
                )
            }
        }
        Spacer(Modifier.height(VidaSpacing.lg))
    }
}

/* =========================================================
   2 · TEXTO
   ========================================================= */

@Composable
fun TextStepSheet(
    initialText: String,
    initialFont: String,
    initialSize: Int,
    confirmLabel: String,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
) {
    var value by remember { mutableStateOf(initialText) }
    var font by remember { mutableStateOf(initialFont) }
    var size by remember { mutableStateOf(initialSize) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VidaSpacing.lg),
    ) {
        SheetHeader(if (onBack != null) "Agregar texto" else "Editar texto", onBack, onClose)
        SectionLabel("Contenido")
        VidaTextField(value, { value = it }, "Escribe tu intención…")
        Spacer(Modifier.height(VidaSpacing.md))

        SectionLabel("Caligrafía · 14 opciones")
        Column(
            Modifier.heightIn(max = 190.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VISION_BOARD_FONTS.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { option ->
                        val selected = option.id == font
                        val c = VidaTheme.colors
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .background(if (selected) c.primaryContainer else c.surface, RoundedCornerShape(999.dp))
                                .border(1.dp, if (selected) c.primary else c.line, RoundedCornerShape(999.dp))
                                .clickable { font = option.id },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                option.label,
                                style = TextStyle(fontFamily = option.family, fontSize = 16.sp),
                                color = if (selected) c.primary else c.text,
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(VidaSpacing.md))

        SectionLabel("Tamaño · de $MIN_TEXT_FONT_SIZE a $MAX_TEXT_FONT_SIZE")
        StepperRow("el tamaño del texto", size) { size = it }
        Spacer(Modifier.height(VidaSpacing.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            VidaSmallButton("Cancelar", onClose, ghost = true, modifier = Modifier.weight(1f))
            VidaSmallButton(confirmLabel, { onConfirm(value, font, size) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/**
 * Solo la caligrafía — lo que abre «Fuente» de la barra de propiedades.
 *
 * Existe aparte del paso completo de texto porque en una barra al estilo de
 * Canva cada botón abre UNA cosa: pulsar «Fuente» y encontrarse además el
 * campo de contenido y el tamaño sería devolver el formulario entero por una
 * pregunta concreta.
 */
@Composable
fun FontSheet(current: String, onClose: () -> Unit, onPick: (String) -> Unit) {
    val c = VidaTheme.colors
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SheetHeader("Fuente", onClose = onClose)
        VISION_BOARD_FONTS.forEach { option ->
            val selected = option.id == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(if (selected) c.primaryContainer else c.surface, RoundedCornerShape(15.dp))
                    .border(1.dp, if (selected) c.primary else c.line, RoundedCornerShape(15.dp))
                    .clickable { onPick(option.id) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Casa en la playa",
                    style = TextStyle(fontFamily = option.family, fontSize = 19.sp),
                    color = if (selected) c.primary else c.text,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/** Solo el tamaño — lo que abre «Tamaño» de la barra de propiedades. */
@Composable
fun SizeSheet(current: Int, onClose: () -> Unit, onPick: (Int) -> Unit) {
    var size by remember { mutableStateOf(current) }
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Tamaño de la fuente", onClose = onClose)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StepperRow("el tamaño del texto", size) { size = it }
        }
        Spacer(Modifier.height(VidaSpacing.lg))
        VidaSmallButton("Aplicar", { onPick(size) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   3 · NOTA
   ========================================================= */

@Composable
fun NoteStepSheet(
    initialText: String,
    confirmLabel: String,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialText) }
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader(if (onBack != null) "Agregar nota" else "Editar nota", onBack, onClose)
        SectionLabel("Contenido")
        VidaTextField(value, { value = it }, "Lo que no quieres olvidar…", multiline = true)
        Spacer(Modifier.height(VidaSpacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            VidaSmallButton("Cancelar", onClose, ghost = true, modifier = Modifier.weight(1f))
            VidaSmallButton(confirmLabel, { onConfirm(value) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   4 · IMAGEN
   ========================================================= */

@Composable
fun ImageStepSheet(
    frame: String?,
    onFrameChange: (String?) -> Unit,
    onPickFromGallery: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val c = VidaTheme.colors
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = VidaSpacing.lg)) {
        SheetHeader(if (onBack != null) "Agregar imagen" else "Marco de la imagen", onBack, onClose)

        if (onBack != null) {
            SectionLabel("De dónde")
            Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                SourceTile("Galería", Icons.Outlined.PhotoLibrary, Modifier.weight(1f), onPickFromGallery)
                SourceTile("Pegar", Icons.Outlined.ContentPaste, Modifier.weight(1f), onPasteFromClipboard)
            }
            Text(
                "El selector del sistema no pide permisos de almacenamiento, y «Pegar» trae la imagen que hayas copiado en Chrome o en cualquier otra aplicación.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = VidaSpacing.sm, bottom = VidaSpacing.md),
            )
        }

        SectionLabel("Marco (opcional)")
        FlowChips(FRAME_OPTIONS.map { it.label }, FRAME_OPTIONS.indexOfFirst { it.id == frame }) { i ->
            onFrameChange(FRAME_OPTIONS[i].id)
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

@Composable
private fun SourceTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val c = VidaTheme.colors
    Column(
        modifier
            .background(c.primaryContainer, RoundedCornerShape(17.dp))
            .border(1.dp, c.primary, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = c.primary)
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.primary)
    }
}

/** Fila de píldoras que se envuelve en varias líneas. */
@Composable
fun FlowChips(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { i, label ->
                    val index = rowIndex * 3 + i
                    OptionChip(label, index == selectedIndex, { onSelect(index) }, Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/* =========================================================
   5 · FORMA  ·  color, contraste y las 98 variantes
   ========================================================= */

@Composable
fun ShapeStepSheet(
    initialFill: String,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
    onPick: (shapeId: String, fill: String, textColor: String) -> Unit,
) {
    val c = VidaTheme.colors
    var fill by remember { mutableStateOf(initialFill) }
    var family by remember { mutableStateOf("basic") }
    val textColor = suggestTextColor(fill)
    val ratio = contrastRatio(fill, textColor) ?: 0.0
    val legible = ratio >= 4.5

    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Elegir forma", onBack, onClose)

        SectionLabel("Fondo")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SHAPE_FILL_SWATCHES.forEach { swatch ->
                val selected = swatch.value.equals(fill, ignoreCase = true)
                Box(
                    Modifier
                        .size(44.dp)
                        .background(parseHexColor(swatch.value) ?: c.primary, CircleShape)
                        .border(if (selected) 3.dp else 1.dp, if (selected) c.primary else c.line, CircleShape)
                        .clickable { fill = swatch.value },
                )
            }
        }
        Spacer(Modifier.height(VidaSpacing.sm))

        // LA REGLA DEL CONTRASTE, visible: el texto se recalcula por
        // luminancia con cada fondo, y si aun así queda bajo, se dice.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(parseHexColor(fill) ?: c.primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Texto de ejemplo", color = parseHexColor(textColor) ?: Color.White, fontSize = 14.sp)
            }
            Text(
                String.format("%.1f:1 · %s", ratio, if (legible) "AA" else "bajo"),
                style = MaterialTheme.typography.labelMedium,
                color = if (legible) c.successText else c.warningText,
            )
        }
        Spacer(Modifier.height(VidaSpacing.md))

        SectionLabel("Familia")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(SHAPE_FAMILIES) { fam ->
                OptionChip(fam.label, fam.id == family, { family = fam.id })
            }
        }
        Spacer(Modifier.height(VidaSpacing.sm))

        val shapes = shapesByFamily(family)
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 260.dp),
        ) {
            items(shapes) { shape ->
                Box(
                    Modifier
                        .size(56.dp)
                        .background(c.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, c.line, RoundedCornerShape(14.dp))
                        .clickable { onPick(shape.id, fill, textColor) },
                    contentAlignment = Alignment.Center,
                ) {
                    ShapeThumb(shape, parseHexColor(fill) ?: c.primary)
                }
            }
        }
        Text(
            "${SHAPE_FAMILIES.first { it.id == family }.label} · ${shapes.size} de ${SHAPE_CATALOG.size} formas",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(top = VidaSpacing.sm),
        )
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

@Composable
private fun ShapeThumb(shape: VisionBoardShape, color: Color) {
    when (shape.id) {
        "rectangle" -> Box(Modifier.size(26.dp, 20.dp).background(color, RoundedCornerShape(3.dp)))
        "circle" -> Box(Modifier.size(24.dp).background(color, CircleShape))
        "capsule" -> Box(Modifier.size(28.dp, 16.dp).background(color, RoundedCornerShape(999.dp)))
        "line" -> Box(Modifier.size(26.dp, 4.dp).background(color, RoundedCornerShape(999.dp)))
        else -> {
            val path = remember(shape.id) { PathParser().parsePathString(shape.path.orEmpty()).toPath() }
            Canvas(Modifier.size(26.dp)) {
                path.fillType = if (shape.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
                withTransform({ scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) }) {
                    drawPath(path, color)
                }
            }
        }
    }
}

/* =========================================================
   6 · STICKERS Y EMOJIS
   ========================================================= */

@Composable
fun StickerStepSheet(
    startOnEmoji: Boolean,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
    onPickSticker: (String) -> Unit,
    onPickEmoji: (String) -> Unit,
) {
    val c = VidaTheme.colors
    var emojiMode by remember { mutableStateOf(startOnEmoji) }
    var category by remember { mutableStateOf(EMOJI_CATEGORIES.first()) }

    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader(if (emojiMode) "Elegir emoji" else "Elegir sticker", onBack, onClose)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OptionChip("Sticker", !emojiMode, { emojiMode = false }, Modifier.weight(1f))
            OptionChip("Emojis", emojiMode, { emojiMode = true }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(VidaSpacing.md))

        if (emojiMode) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(EMOJI_CATEGORIES) { cat -> OptionChip(cat, cat == category, { category = cat }) }
            }
            Spacer(Modifier.height(VidaSpacing.sm))
            val tiles = EMOJI_CATALOG.filter { it.category == category }
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 240.dp),
            ) {
                items(tiles) { emoji ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(c.surface, RoundedCornerShape(13.dp))
                            .border(1.dp, c.line, RoundedCornerShape(13.dp))
                            .clickable { onPickEmoji(emoji.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(emoji.icon, emoji.label, Modifier.size(22.dp), tint = emoji.color)
                    }
                }
            }
            Text(
                "$category · ${tiles.size} de ${EMOJI_CATALOG.size} emojis en ${EMOJI_CATEGORIES.size} categorías",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = VidaSpacing.sm),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 280.dp),
            ) {
                items(STICKER_CATALOG) { sticker ->
                    Column(
                        Modifier
                            .background(c.surface, RoundedCornerShape(13.dp))
                            .border(1.dp, c.line, RoundedCornerShape(13.dp))
                            .clickable { onPickSticker(sticker.id) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        StickerBadge(sticker.icon, sticker.label, sticker.color, Modifier.size(34.dp))
                        Text(
                            sticker.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = c.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   7 · GRÁFICAS Y CUADRÍCULAS
   ========================================================= */

@Composable
fun ChartStepSheet(selected: String?, onBack: (() -> Unit)?, onClose: () -> Unit, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Elegir gráfica", onBack, onClose)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.heightIn(max = 260.dp),
        ) {
            items(CHART_OPTIONS) { chart ->
                CatalogTile(chart.label, Icons.Outlined.BarChart, { onPick(chart.id) }, chart.id == selected)
            }
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

@Composable
fun GridStepSheet(selected: String?, onBack: (() -> Unit)?, onClose: () -> Unit, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Elegir cuadrícula", onBack, onClose)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.heightIn(max = 260.dp),
        ) {
            items(GRID_OPTIONS) { grid ->
                CatalogTile(grid.label, Icons.Outlined.GridView, { onPick(grid.id) }, grid.id == selected)
            }
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   8 · LISTAS SIMPLES  ·  capas, animar, plantillas, temas, tableros
   ========================================================= */

@Composable
fun SimpleListSheet(
    title: String,
    rows: List<Pair<String, String>>,
    onClose: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val c = VidaTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader(title, onClose = onClose)
        rows.forEachIndexed { index, (label, mark) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clickable { onPick(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                if (mark.isNotBlank()) {
                    Text(mark, style = MaterialTheme.typography.labelMedium, color = c.primary)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line.copy(alpha = 0.4f)))
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

@Composable
fun ThemeSheet(current: String, onClose: () -> Unit, onPick: (String) -> Unit) {
    val c = VidaTheme.colors
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SheetHeader("Tema del tablero", onClose = onClose)
        BOARD_THEMES.forEach { theme ->
            val selected = theme.id == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (selected) c.primaryContainer else c.surface, RoundedCornerShape(15.dp))
                    .border(1.dp, if (selected) c.primary else c.line, RoundedCornerShape(15.dp))
                    .clickable { onPick(theme.id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(theme.colors.background, RoundedCornerShape(10.dp))
                        .border(1.dp, c.line, RoundedCornerShape(10.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(theme.name, style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(
                        theme.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = c.textSecondary,
                    )
                }
                if (selected) Text("Activo", style = MaterialTheme.typography.labelMedium, color = c.primary)
            }
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

@Composable
fun TemplatesSheet(onClose: () -> Unit, onPick: (VisionBoardTemplate) -> Unit) {
    val c = VidaTheme.colors
    val portal = VISION_BOARD_TEMPLATES.filter { it.fromPortal }
    val basic = VISION_BOARD_TEMPLATES.filterNot { it.fromPortal }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = VidaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SheetHeader("Plantillas", onClose = onClose)

        SectionLabel("Tuyas · con fotografías")
        portal.forEach { template ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, RoundedCornerShape(15.dp))
                    .border(1.dp, c.line, RoundedCornerShape(15.dp))
                    .clickable { onPick(template) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(44.dp).background(c.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Image, null, Modifier.size(20.dp), tint = c.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = c.textSecondary,
                    )
                    Text(
                        "${template.elements.size} elementos · ${template.photoCount} fotos",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = c.primary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
        Text(
            "Las fotos se copian a tu cuenta al aplicarla, así que la plantilla sigue siendo tuya aunque cambie el tablero original. Tarda unos segundos.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = c.textSecondary,
        )

        Spacer(Modifier.height(VidaSpacing.sm))
        SectionLabel("De partida")
        basic.forEach { template ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, RoundedCornerShape(15.dp))
                    .border(1.dp, c.line, RoundedCornerShape(15.dp))
                    .clickable { onPick(template) }
                    .padding(12.dp),
            ) {
                Text(template.name, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = c.textSecondary,
                )
            }
        }
        Text(
            "Se coloca encima de lo que ya tengas, sin borrar nada.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(top = VidaSpacing.xs),
        )
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   9 · NUEVO TABLERO
   ========================================================= */

@Composable
fun NewBoardSheet(onClose: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("LIGHT") }
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Nuevo tablero", onClose = onClose)
        SectionLabel("Nombre")
        VidaTextField(name, { name = it }, "Mi tablero 2027")
        Spacer(Modifier.height(VidaSpacing.md))
        SectionLabel("Tema")
        FlowChips(BOARD_THEMES.map { it.name }, BOARD_THEMES.indexOfFirst { it.id == theme }) { i ->
            theme = BOARD_THEMES[i].id
        }
        Spacer(Modifier.height(VidaSpacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            VidaSmallButton("Cancelar", onClose, ghost = true, modifier = Modifier.weight(1f))
            VidaSmallButton("Crear", { onCreate(name, theme) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   10 · TODAS LAS HERRAMIENTAS
   ========================================================= */

data class ToolEntry(val label: String, val icon: ImageVector, val enabled: Boolean, val action: () -> Unit)

@Composable
fun AllToolsSheet(tools: List<ToolEntry>, onClose: () -> Unit) {
    val c = VidaTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Todas las herramientas", onClose = onClose)
        Text(
            "Las mismas de la barra, con su nombre. Ninguna vive solo aquí.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = VidaSpacing.sm),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            items(tools) { tool ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .background(c.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, c.line, RoundedCornerShape(14.dp))
                        .clickable(enabled = tool.enabled, onClick = tool.action)
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(
                        tool.icon, null, Modifier.size(18.dp),
                        tint = if (tool.enabled) c.text else c.textTertiary,
                    )
                    Text(
                        tool.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = if (tool.enabled) c.text else c.textTertiary,
                        maxLines = 2,
                    )
                }
            }
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}

/* =========================================================
   11 · EDITAR  ·  cada tipo con lo suyo
   ========================================================= */

@Composable
fun EditSheet(
    element: BoardElement,
    onClose: () -> Unit,
    onSave: (JsonObject) -> Unit,
    onOpenShapePicker: () -> Unit,
    onOpenStickerPicker: () -> Unit,
) {
    when (element.type) {
        "TEXT" -> TextStepSheet(
            initialText = element.text.orEmpty(),
            initialFont = element.str("font") ?: DEFAULT_VISION_BOARD_FONT,
            initialSize = fontSizeOf(element.int("fontSize")),
            confirmLabel = "Guardar",
            onBack = null,
            onClose = onClose,
        ) { text, font, size ->
            onSave(
                buildJsonObject {
                    put("text", text)
                    put("font", font)
                    put("fontSize", size)
                    element.str("entrance")?.let { put("entrance", it) }
                },
            )
        }

        "NOTE" -> NoteStepSheet(element.text.orEmpty(), "Guardar", null, onClose) { text ->
            onSave(
                buildJsonObject {
                    put("text", text)
                    element.str("entrance")?.let { put("entrance", it) }
                },
            )
        }

        "IMAGE" -> {
            var frame by remember(element.id) { mutableStateOf(element.str("frameStyle")) }
            Column {
                ImageStepSheet(
                    frame = frame,
                    onFrameChange = { next ->
                        frame = next
                        onSave(
                            buildJsonObject {
                                element.str("imageId")?.let { put("imageId", it) }
                                element.str("url")?.let { put("url", it) }
                                if (next != null) put("frameStyle", next)
                                element.str("entrance")?.let { put("entrance", it) }
                            },
                        )
                    },
                    onPickFromGallery = {},
                    onPasteFromClipboard = {},
                    onBack = null,
                    onClose = onClose,
                )
            }
        }

        "SHAPE" -> ShapeEditSheet(element, onClose, onSave, onOpenShapePicker)

        "STICKER" -> Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
            SheetHeader("Editar sticker", onClose = onClose)
            Text(
                "Elige otro sticker o emoji para sustituir este.",
                style = MaterialTheme.typography.bodySmall,
                color = VidaTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = VidaSpacing.md),
            )
            VidaSmallButton("Cambiar", onOpenStickerPicker, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(VidaSpacing.xl))
        }

        "CHART" -> ChartStepSheet(element.str("chartType"), null, onClose) { id ->
            onSave(
                buildJsonObject {
                    put("chartType", id)
                    element.str("entrance")?.let { put("entrance", it) }
                },
            )
        }

        "GRID" -> GridStepSheet(element.str("layout"), null, onClose) { id ->
            onSave(
                buildJsonObject {
                    put("layout", id)
                    element.str("entrance")?.let { put("entrance", it) }
                },
            )
        }

        else -> Column(Modifier.fillMaxWidth().padding(horizontal = VidaSpacing.lg)) {
            SheetHeader("Editar tabla", onClose = onClose)
            Text(
                "Esta tabla usa contenido de ejemplo — todavía no se puede editar celda por celda, igual que en la Web. " +
                    "Mover, redimensionar, girar y bloquear sí funcionan desde la barra.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidaTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(VidaSpacing.xl))
        }
    }
}

@Composable
private fun ShapeEditSheet(
    element: BoardElement,
    onClose: () -> Unit,
    onSave: (JsonObject) -> Unit,
    onOpenShapePicker: () -> Unit,
) {
    val c = VidaTheme.colors
    var fill by remember(element.id) { mutableStateOf(element.str("fill") ?: DEFAULT_SHAPE_FILL) }
    var label by remember(element.id) { mutableStateOf(element.text.orEmpty()) }
    val textColor = suggestTextColor(fill)
    val ratio = contrastRatio(fill, textColor) ?: 0.0
    val legible = ratio >= 4.5

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = VidaSpacing.lg)) {
        SheetHeader("Editar forma", onClose = onClose)

        SectionLabel("Texto dentro de la forma")
        VidaTextField(label, { label = it }, "Opcional")
        Spacer(Modifier.height(VidaSpacing.md))

        SectionLabel("Fondo")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SHAPE_FILL_SWATCHES.forEach { swatch ->
                val selected = swatch.value.equals(fill, ignoreCase = true)
                Box(
                    Modifier
                        .size(44.dp)
                        .background(parseHexColor(swatch.value) ?: c.primary, CircleShape)
                        .border(if (selected) 3.dp else 1.dp, if (selected) c.primary else c.line, CircleShape)
                        .clickable { fill = swatch.value },
                )
            }
        }
        Spacer(Modifier.height(VidaSpacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(parseHexColor(fill) ?: c.primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(label.ifBlank { "Texto de ejemplo" }, color = parseHexColor(textColor) ?: Color.White, fontSize = 14.sp)
            }
            Text(
                String.format("%.1f:1 · %s", ratio, if (legible) "AA" else "bajo"),
                style = MaterialTheme.typography.labelMedium,
                color = if (legible) c.successText else c.warningText,
            )
        }
        Spacer(Modifier.height(VidaSpacing.md))

        VidaSmallButton(
            "Cambiar variante · ${SHAPE_CATALOG.size} formas",
            onOpenShapePicker,
            ghost = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VidaSpacing.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm)) {
            VidaSmallButton("Cancelar", onClose, ghost = true, modifier = Modifier.weight(1f))
            VidaSmallButton(
                "Guardar",
                {
                    onSave(
                        buildJsonObject {
                            put("shape", element.shape ?: "rectangle")
                            put("fill", fill)
                            put("textColor", textColor)
                            if (label.isNotBlank()) put("text", label)
                            element.str("entrance")?.let { put("entrance", it) }
                        },
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(VidaSpacing.xl))
    }
}
