package com.vidacotidiana.app.feature.documents

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
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.core.ui.components.vidaSoftShadow
import com.vidacotidiana.app.navigation.Routes
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * PIEZAS COMUNES DEL MÓDULO DOCUMENTOS — propuesta aprobada el 2026-09-26.
 *
 * Las cuatro vistas (resumen, colección, búsqueda y detalle) comparten la
 * misma fila de documento, el mismo menú de acciones y los mismos tonos por
 * categoría. Vivir aquí es lo que evita que la fila de «Recientes» en el
 * resumen y la de una búsqueda acaben diciendo cosas distintas del mismo
 * archivo.
 */

/* ══════════════════════════════════════════════════════════════════════════
   COLECCIONES
   ══════════════════════════════════════════════════════════════════════════ */

/** Accesos rápidos: se navegan como una categoría más, con su clave propia. */
internal object DocCollections {
    const val ALL = "_TODOS"
    const val RECENT = "_RECIENTES"
    const val FAVORITES = "_FAVORITOS"
    const val SHARED = "_COMPARTIDOS"

    /**
     * «Reciente» = subido en los últimos 30 días.
     *
     * RECOMMENDATION, no requisito: la propuesta muestra el acceso sin fijar la
     * ventana. Treinta días cubre el ciclo de un recibo mensual, que es el caso
     * típico de la sección.
     */
    const val RECENT_DAYS = 30L

    fun title(key: String): String = when (key) {
        ALL -> "Todos los documentos"
        RECENT -> "Recientes"
        FAVORITES -> "Favoritos"
        SHARED -> "Compartidos"
        else -> VidaVocabulary.human(key)
    }
}

internal fun Document.isRecent(): Boolean {
    val d = createdAt?.toLocalDate() ?: return false
    return ChronoUnit.DAYS.between(d, LocalDate.now()) <= DocCollections.RECENT_DAYS
}

/** Visible a alguien más que yo: la familia entera o una persona concreta. */
internal fun Document.isShared(sharedCount: Int): Boolean = visibility != "PRIVATE" || sharedCount > 0

/** Más nuevo primero; los que no traen fecha, al final. */
internal val NewestFirst: Comparator<Document> =
    compareByDescending<Document> { it.createdAt ?: LocalDateTime.MIN }.thenBy { it.name.lowercase() }

/* ══════════════════════════════════════════════════════════════════════════
   FECHAS Y TAMAÑOS
   ══════════════════════════════════════════════════════════════════════════ */

private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * «25 sep 2026». Los meses se escriben a mano porque el formateador de Java
 * cambia según la versión de Android («sept.», «sep.»), y la propuesta los
 * quiere cortos y sin punto.
 */
internal fun shortDate(d: LocalDate): String = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"

internal fun Document.dayLabel(): String = createdAt?.toLocalDate()?.let(::shortDate) ?: dateLabel

/** «Hoy», «Ayer», «3 días» o la fecha: lo que la lista de Recientes enseña. */
internal fun Document.relativeLabel(): String {
    val d = createdAt?.toLocalDate() ?: return dateLabel
    val days = ChronoUnit.DAYS.between(d, LocalDate.now())
    return when {
        days <= 0L -> "Hoy"
        days == 1L -> "Ayer"
        days < 7L -> "$days días"
        else -> shortDate(d)
    }
}

/** «25 sep 2026 · 10:24 a. m.» para el detalle. */
internal fun Document.dateTimeLabel(): String {
    val t = createdAt ?: return dateLabel
    val h12 = (t.hour % 12).let { if (it == 0) 12 else it }
    val suffix = if (t.hour < 12) "a. m." else "p. m."
    return "${shortDate(t.toLocalDate())} · $h12:${"%02d".format(t.minute)} $suffix"
}

/** El total de la sección puede pasar del giga, cosa que una fila nunca hace. */
internal fun totalSizeParts(bytes: Long): Pair<String, String> = when {
    bytes >= 1_073_741_824L -> String.format(Locale.US, "%.1f", bytes / 1_073_741_824.0) to "GB"
    bytes >= 1_048_576L -> String.format(Locale.US, "%.1f", bytes / 1_048_576.0) to "MB"
    bytes >= 1024L -> String.format(Locale.US, "%.0f", bytes / 1024.0) to "KB"
    else -> "$bytes" to "B"
}

/** Sin acentos y en minúsculas: «recibo» encuentra «Recibó» y viceversa. */
internal fun String.searchable(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

internal fun Document.extension(): String = name.substringAfterLast('.', "").lowercase()

internal fun Document.isImage(): Boolean = extension() in setOf("png", "jpg", "jpeg", "webp", "gif")

/* ══════════════════════════════════════════════════════════════════════════
   TONOS E ICONOS
   ══════════════════════════════════════════════════════════════════════════ */

internal data class DocTone(val fg: Color, val bg: Color)

internal data class DocCategoryVisual(val key: String, val label: String, val icon: ImageVector, val tone: DocTone)

/**
 * Cada categoría con su color, como en «Mis categorías» de la propuesta.
 *
 * SON LAS CINCO QUE EXISTEN (`documents.category`, CHECK de V12). La propuesta
 * enseña además Casa, Auto y Salud: no existen en el backend y no se inventan
 * aquí — añadirlas es una decisión de producto con migración (TBD).
 */
@Composable
internal fun categoryVisual(key: String): DocCategoryVisual {
    val c = VidaTheme.colors
    val label = VidaVocabulary.human(key)
    return when (key) {
        "COMPROBANTES" -> DocCategoryVisual(key, label, Icons.AutoMirrored.Outlined.ReceiptLong, DocTone(c.info, c.infoContainer))
        "SEGUROS" -> DocCategoryVisual(key, label, Icons.Outlined.Shield, DocTone(c.success, c.successContainer))
        "IDENTIFICACION" -> DocCategoryVisual(key, label, Icons.Outlined.Badge, DocTone(c.primary, c.primaryContainer))
        "CONTRATOS" -> DocCategoryVisual(key, label, Icons.Outlined.Handshake, DocTone(c.warning, c.warningContainer))
        "OTROS" -> DocCategoryVisual(key, label, Icons.Outlined.MoreHoriz, DocTone(c.textSecondary, c.sunken))
        else -> DocCategoryVisual(key, label, Icons.Outlined.Folder, DocTone(c.textSecondary, c.sunken))
    }
}

/**
 * Dentro de UNA categoría el color de la categoría ya no distingue nada, así
 * que ahí cada fila toma el tono de su TIPO de archivo: PDF en rojo, imagen
 * en verde. Fuera de una categoría (resumen, búsqueda, detalle) manda la
 * categoría, que es lo que ayuda a ubicar el archivo.
 */
@Composable
internal fun typeTone(doc: Document): DocTone {
    val c = VidaTheme.colors
    return when {
        doc.extension() == "pdf" -> DocTone(c.error, c.errorContainer)
        doc.isImage() -> DocTone(c.success, c.successContainer)
        else -> DocTone(c.primary, c.primaryContainer)
    }
}

internal fun Document.glyph(): ImageVector = if (isImage()) Icons.Outlined.Image else Icons.Outlined.Description

/* ══════════════════════════════════════════════════════════════════════════
   SUPERFICIES
   ══════════════════════════════════════════════════════════════════════════ */

/** Tarjeta blanca con filete fino y sombra suave, la de todos los cuadros. */
@Composable
internal fun Modifier.docCard(background: Color = VidaTheme.colors.surface, corner: Dp = 18.dp): Modifier {
    val c = VidaTheme.colors
    val shape = RoundedCornerShape(corner)
    return this
        .vidaSoftShadow(corner = corner, keyBlur = 16.dp, keyOffsetY = 4.dp)
        .background(background, shape)
        .border(1.dp, c.border.copy(alpha = 0.18f), shape)
}

/** Cuadro de icono con su tono: el «sello» de cada fila y de cada mosaico. */
@Composable
internal fun DocMark(icon: ImageVector, tone: DocTone, size: Dp = 44.dp, iconSize: Dp = 22.dp, circle: Boolean = false) {
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
internal fun DocTopBar(
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
internal fun DocSectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
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
internal fun DocSearchBox(
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
internal fun ClearButton(onClick: () -> Unit) {
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
internal fun DocChip(label: String, selected: Boolean, onClick: () -> Unit, dropdown: Boolean = false) {
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

/* ══════════════════════════════════════════════════════════════════════════
   FILA DE DOCUMENTO
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Una fila de documento: sello, nombre, metadatos, estrella si es favorito y
 * el menú «⋮». Sin tarjeta propia: la propuesta alinea las filas sobre el
 * fondo, y el aire entre ellas hace de separador.
 */
@Composable
internal fun DocumentRow(
    doc: Document,
    meta: String,
    tone: DocTone,
    favorite: Boolean,
    actions: DocumentActions,
    onClick: () -> Unit,
) {
    val c = VidaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocMark(doc.glyph(), tone)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                doc.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (favorite) {
            Icon(Icons.Filled.Star, contentDescription = "Favorito", tint = c.warning, modifier = Modifier.size(18.dp))
        }
        DocumentMenu(doc, favorite, actions)
    }
}

/** «Mostrando 8 de 42 · Cargar más ↓». */
@Composable
internal fun LoadMoreFooter(shown: Int, total: Int, onMore: () -> Unit) {
    val c = VidaTheme.colors
    Row(
        Modifier.fillMaxWidth().docCard(c.sunken).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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

/* ══════════════════════════════════════════════════════════════════════════
   ACCIONES
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Todo lo que se puede hacer con un documento, en un solo sitio.
 *
 * Son las MISMAS acciones que ya tenía la sección (y la Web): ver, compartir
 * con la familia, abrir en otra app, editar, visibilidad y eliminar. Lo único
 * nuevo es el favorito, que es local (`AppPreferences`).
 */
internal class DocumentActions(
    private val viewModel: AppViewModel,
    private val navController: NavHostController,
    private val context: android.content.Context,
    val access: ResourceAccessController,
) {
    var pendingDelete by mutableStateOf<Document?>(null)

    fun open(doc: Document) = navController.navigate(Routes.documentRoute(doc.id))
    fun details(doc: Document) = navController.navigate(Routes.documentDetailRoute(doc.id))
    fun toggleFavorite(doc: Document) = viewModel.toggleDocumentFavorite(doc.id)
    fun share(doc: Document) = access.abrir(ShareableKind.DOCUMENT, doc.id, doc.name)
    fun openExternal(doc: Document) = viewModel.shareDocumentFile(context, doc.id, doc.name)
    fun edit(doc: Document) = viewModel.requestEdit(CreatableResource.DOCUMENT, doc.id)
    fun setFamilyVisible(doc: Document, visible: Boolean) = viewModel.setDocumentFamilyVisible(doc.id, visible)
    fun askDelete(doc: Document) { pendingDelete = doc }
    fun confirmDelete(onDeleted: () -> Unit = {}) {
        val doc = pendingDelete ?: return
        pendingDelete = null
        viewModel.toggleDocumentFavoriteOff(doc.id)
        viewModel.deleteResource(CreatableResource.DOCUMENT, doc.id)
        onDeleted()
    }
}

private fun AppViewModel.toggleDocumentFavoriteOff(id: String) {
    if (id in favoriteDocuments.value) toggleDocumentFavorite(id)
}

@Composable
internal fun rememberDocumentActions(viewModel: AppViewModel, navController: NavHostController): DocumentActions {
    val context = LocalContext.current
    val access = rememberResourceAccess(viewModel)
    return remember(viewModel, navController, access) { DocumentActions(viewModel, navController, context, access) }
}

/**
 * Los diálogos que cuelgan de las acciones: quién tiene acceso y la
 * confirmación de borrado. Borrar un documento no tiene vuelta atrás en el
 * backend, así que se pregunta siempre.
 */
@Composable
internal fun DocumentActions.Dialogs(viewModel: AppViewModel, onDeleted: () -> Unit = {}) {
    val c = VidaTheme.colors
    access.Dialogo(viewModel)
    pendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar documento?") },
            text = { Text("«${doc.name}» se eliminará para siempre, también para quienes lo tengan compartido.") },
            confirmButton = { TextButton(onClick = { confirmDelete(onDeleted) }) { Text("Eliminar", color = c.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }
}

/** El «⋮» de cada fila. */
@Composable
internal fun DocumentMenu(doc: Document, favorite: Boolean, actions: DocumentActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        VidaIconButton(Icons.Outlined.MoreVert, "Más acciones de ${doc.name}", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DocumentMenuItems(doc, favorite, actions, includeView = true, includeDetails = true) { open = false }
        }
    }
}

@Composable
internal fun DocumentMenuItems(
    doc: Document,
    favorite: Boolean,
    actions: DocumentActions,
    includeView: Boolean,
    includeDetails: Boolean,
    dismiss: () -> Unit,
) {
    val c = VidaTheme.colors
    val familyVisible = doc.visibility != "PRIVATE"
    fun item(label: String, color: Color? = null, run: () -> Unit): @Composable () -> Unit = {
        DropdownMenuItem(
            text = { Text(label, color = color ?: c.text) },
            onClick = { dismiss(); run() },
        )
    }
    if (includeView) item("Ver documento") { actions.open(doc) }()
    if (includeDetails) item("Detalles") { actions.details(doc) }()
    item(if (favorite) "Quitar de favoritos" else "Añadir a favoritos") { actions.toggleFavorite(doc) }()
    item("Compartir") { actions.share(doc) }()
    item("Abrir en otra app") { actions.openExternal(doc) }()
    item(if (familyVisible) "Ocultar a la familia" else "Ver toda la familia") { actions.setFamilyVisible(doc, !familyVisible) }()
    item("Editar") { actions.edit(doc) }()
    item("Eliminar", c.error) { actions.askDelete(doc) }()
}
