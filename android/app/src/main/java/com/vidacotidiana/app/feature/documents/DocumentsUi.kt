package com.vidacotidiana.app.feature.documents

import com.vidacotidiana.app.navigation.go
import com.vidacotidiana.app.core.ui.components.ToneColors
import com.vidacotidiana.app.core.ui.components.ToneMark

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
internal fun shortDate(d: LocalDate): String = com.vidacotidiana.app.core.ui.components.shortDateEs(d)

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

internal data class DocCategoryVisual(val key: String, val label: String, val icon: ImageVector, val tone: ToneColors)

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
        "COMPROBANTES" -> DocCategoryVisual(key, label, Icons.AutoMirrored.Outlined.ReceiptLong, ToneColors(c.info, c.infoContainer))
        "SEGUROS" -> DocCategoryVisual(key, label, Icons.Outlined.Shield, ToneColors(c.success, c.successContainer))
        "IDENTIFICACION" -> DocCategoryVisual(key, label, Icons.Outlined.Badge, ToneColors(c.primary, c.primaryContainer))
        "CONTRATOS" -> DocCategoryVisual(key, label, Icons.Outlined.Handshake, ToneColors(c.warning, c.warningContainer))
        "OTROS" -> DocCategoryVisual(key, label, Icons.Outlined.MoreHoriz, ToneColors(c.textSecondary, c.sunken))
        else -> DocCategoryVisual(key, label, Icons.Outlined.Folder, ToneColors(c.textSecondary, c.sunken))
    }
}

/**
 * Dentro de UNA categoría el color de la categoría ya no distingue nada, así
 * que ahí cada fila toma el tono de su TIPO de archivo: PDF en rojo, imagen
 * en verde. Fuera de una categoría (resumen, búsqueda, detalle) manda la
 * categoría, que es lo que ayuda a ubicar el archivo.
 */
@Composable
internal fun typeTone(doc: Document): ToneColors {
    val c = VidaTheme.colors
    return when {
        doc.extension() == "pdf" -> ToneColors(c.error, c.errorContainer)
        doc.isImage() -> ToneColors(c.success, c.successContainer)
        else -> ToneColors(c.primary, c.primaryContainer)
    }
}

internal fun Document.glyph(): ImageVector = if (isImage()) Icons.Outlined.Image else Icons.Outlined.Description

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
    tone: ToneColors,
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
        ToneMark(doc.glyph(), tone)
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

    fun open(doc: Document) = navController.go(Routes.documentRoute(doc.id))
    fun details(doc: Document) = navController.go(Routes.documentDetailRoute(doc.id))
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
    // Con «Toda la familia» ya lo ven todos: compartir no tiene a quién más.
    if (doc.visibility != "FAMILY_PUBLIC") item("Compartir") { actions.share(doc) }()
    item("Abrir en otra app") { actions.openExternal(doc) }()
    item(if (familyVisible) "Ocultar a la familia" else "Ver toda la familia") { actions.setFamilyVisible(doc, !familyVisible) }()
    item("Editar") { actions.edit(doc) }()
    item("Eliminar", c.error) { actions.askDelete(doc) }()
}
