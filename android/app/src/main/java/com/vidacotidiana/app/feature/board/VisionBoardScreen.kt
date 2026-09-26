package com.vidacotidiana.app.feature.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.data.BoardElement
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.navigation.Routes
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Vision Board — un EDITOR DE LIENZO, con la forma que tiene un editor de
 * lienzo en un teléfono.
 *
 * VENTANA PROPIA. No lleva la barra de secciones de la aplicación (ver
 * `NavGraph`, donde `showBottomNav` la excluye): esa franja de abajo la
 * necesita el editor entera, porque ahí viven las herramientas y, en cuanto
 * seleccionas algo, las propiedades de ESE elemento. Dos barras apiladas
 * dejarían el lienzo sin sitio. La vuelta está en la cabecera: el botón de
 * casa lleva a Inicio.
 *
 * TRES CAPAS, y cada una con su trabajo:
 *  1. El lienzo es una PÁGINA —del tamaño real del tablero— centrada sobre
 *     un fondo neutro, no un fondo infinito. Se ve dónde acaba lo que estás
 *     componiendo, que es justo lo que un tablero necesita.
 *  2. Sobre el elemento seleccionado flota su burbuja de acciones rápidas
 *     (duplicar, bloquear, eliminar, más), y alrededor sus tiradores: las
 *     cuatro esquinas, los cuatro lados, y debajo los botones de girar y de
 *     mover.
 *  3. Abajo, la barra: categorías cuando no hay nada seleccionado,
 *     propiedades del elemento cuando sí, con su «✓» para soltar.
 *
 * EL ÁRBITRO DEL GESTO es la jerarquía de punteros, no una heurística: el
 * elemento recibe primero el toque y, si arrastra, CONSUME el evento, así
 * que el `detectTransformGestures` de la página no mueve nada por debajo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionBoardScreen(
    navController: NavHostController,
    boardViewModel: VisionBoardViewModel = hiltViewModel(),
) {
    val c = VidaTheme.colors
    val state by boardViewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val theme = boardThemeOf(state.board?.theme)

    var sheet by remember { mutableStateOf<BoardSheet?>(null) }
    var confirmDeleteElement by remember { mutableStateOf(false) }
    var confirmDeleteBoard by remember { mutableStateOf(false) }
    var pendingFrame by remember { mutableStateOf<String?>(null) }
    var replacingImage by remember { mutableStateOf(false) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val boardW = (state.board?.width ?: 1200).toFloat()
    val boardH = (state.board?.height ?: 800).toFloat()
    val pageWpx = with(density) { boardW.dp.toPx() }
    val pageHpx = with(density) { boardH.dp.toPx() }

    // La página entra encuadrada, como cualquier editor al abrir un diseño:
    // se ve entera y desde ahí se acerca con dos dedos.
    fun fitZoom(): Float =
        if (canvasSize.width == 0) 1f
        else min((canvasSize.width - 48f) / pageWpx, (canvasSize.height - 48f) / pageHpx).coerceIn(0.05f, 4f)

    fun centerPage(z: Float) {
        pan = Offset((canvasSize.width - pageWpx * z) / 2f, (canvasSize.height - pageHpx * z) / 2f)
    }

    // UNA VEZ POR TABLERO, no cada vez que cambia el tamaño del lienzo: el
    // teclado al abrirse encoge la ventana, y re-encuadrar ahí le borraba al
    // usuario el zoom y el encuadre que acababa de elegir.
    var fittedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(canvasSize, state.board?.id) {
        val id = state.board?.id
        if (canvasSize.width > 0 && id != null && fittedFor != id) {
            zoom = fitZoom()
            centerPage(zoom)
            fittedFor = id
        }
    }

    /** El centro de lo que se está viendo, en coordenadas del tablero. */
    fun viewCenter(): Pair<Float, Float> {
        val cx = (-pan.x + canvasSize.width / 2f) / zoom
        val cy = (-pan.y + canvasSize.height / 2f) / zoom
        return with(density) { cx.toDp().value to cy.toDp().value }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            if (replacingImage) {
                replacingImage = false
                state.selected?.let { boardViewModel.replaceImage(it.id, uri) }
            } else {
                val (x, y) = viewCenter()
                boardViewModel.addImage(uri, x, y, pendingFrame)
            }
        } else {
            replacingImage = false
        }
    }

    val selected = state.selected

    Column(Modifier.fillMaxSize().background(c.surfaceVariant)) {

        /* ---------------- cabecera ---------------- */
        EditorHeader(
            title = state.board?.name ?: "Vision Board",
            save = state.save,
            onHome = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onMore = { sheet = BoardSheet.AllTools },
            onRetry = boardViewModel::retrySave,
        )

        /* ---------------- lienzo ---------------- */
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(c.sunken)
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) { detectTapGestures { boardViewModel.select(null) } }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.1f, 4f)
                        pan += panChange
                    }
                },
        ) {
            // LA PÁGINA Y LO QUE LLEVA DENTRO.
            //
            // Ni un solo `graphicsLayer` escalando el tablero entero, y ese es
            // el cambio de fondo. Escalar un layout de 1200×800 dp obliga a
            // Compose a crear una capa de hardware de ~3600×2400 px: son
            // treinta y pico MB de textura, roza el límite que el dispositivo
            // admite y, cuando lo pasa, NO DIBUJA NADA. Era exactamente lo que
            // pasaba: la plantilla se aplicaba y el lienzo se quedaba vacío.
            //
            // Ahora el zoom no deforma nada: se usa para CALCULAR dónde y de
            // qué tamaño va cada elemento en pantalla. Cada uno es un nodo
            // pequeño, el texto se compone a su tamaño real —así que se lee
            // nítido a cualquier zoom, no ampliado y borroso— y la memoria no
            // depende de lo grande que sea el tablero.
            val pageLeft = pan.x
            val pageTop = pan.y
            val pageW = pageWpx * zoom
            val pageH = pageHpx * zoom

            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Color.Black.copy(alpha = 0.10f),
                    topLeft = Offset(pageLeft + 5f, pageTop + 8f),
                    size = Size(pageW, pageH),
                )
                drawRect(theme.colors.background, topLeft = Offset(pageLeft, pageTop), size = Size(pageW, pageH))
            }

            Box(
                Modifier
                    .fillMaxSize()
                    // Recorta al papel, como la Web: el tablero tiene bordes y
                    // se tienen que notar. Se hace al DIBUJAR y no con un
                    // contenedor, para no meter otro nodo que mida de más.
                    .drawWithContent {
                        clipRect(pageLeft, pageTop, pageLeft + pageW, pageTop + pageH) {
                            this@drawWithContent.drawContent()
                        }
                    },
            ) {
                state.elements.forEach { element ->
                    // SOLO LO QUE SE VE.
                    //
                    // Un tablero puede tener cien elementos y la pantalla
                    // enseña unos pocos. Componer los que caen fuera cuesta
                    // medida, dibujo y memoria a cambio de nada. El margen
                    // deja entrar los que asoman por el borde para que no
                    // aparezcan de golpe al desplazar.
                    val elLeft = pan.x + with(density) { element.x.dp.toPx() } * zoom
                    val elTop = pan.y + with(density) { element.y.dp.toPx() } * zoom
                    val elRight = elLeft + with(density) { element.width.dp.toPx() } * zoom
                    val elBottom = elTop + with(density) { element.height.dp.toPx() } * zoom
                    val margin = 240f
                    val visible = elRight > -margin && elLeft < canvasSize.width + margin &&
                        elBottom > -margin && elTop < canvasSize.height + margin

                    if (visible || element.id == state.selectedId) {
                        // La clave deja que Compose REUTILICE el nodo de cada
                        // elemento entre recomposiciones en vez de tirarlo y
                        // rehacerlo cada vez que cambia cualquier otra cosa.
                        key(element.id) {
                            CanvasElement(
                                element = element,
                                theme = theme,
                                image = element.imageId?.let { state.images[it] },
                                selected = element.id == state.selectedId,
                                zoom = zoom,
                                pan = pan,
                                onSelect = { boardViewModel.select(element.id) },
                                onOpenProperties = {
                                    boardViewModel.select(element.id)
                                    sheet = primarySheetFor(element)
                                },
                                onCommit = { x, y, w, h, r -> boardViewModel.commitGeometry(element, x, y, w, h, r) },
                            )
                        }
                    }
                }
            }

            if (state.loading) {
                Text(
                    "Abriendo tu tablero…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (state.elements.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = VidaSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Tu tablero está en blanco", style = MaterialTheme.typography.titleMedium, color = theme.colors.text)
                    Text(
                        "Empieza por «Elementos» o «Plantillas», ahí abajo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.colors.textSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // LA BURBUJA de acciones rápidas, encima del elemento.
            if (selected != null && canvasSize.width > 0) {
                val bubbleW = 196.dp
                val bubbleWpx = with(density) { bubbleW.toPx() }
                val elCenterX = pan.x + with(density) { (selected.x + selected.width / 2f).dp.toPx() } * zoom
                val elTop = pan.y + with(density) { selected.y.dp.toPx() } * zoom
                val rawX = elCenterX - bubbleWpx / 2f
                val x = rawX.coerceIn(8f, (canvasSize.width - bubbleWpx - 8f).coerceAtLeast(8f))
                val above = elTop - with(density) { 56.dp.toPx() }
                val y = if (above < 8f) elTop + with(density) { selected.height.dp.toPx() } * zoom + 12f else above

                QuickBubble(
                    width = bubbleW,
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) },
                    locked = selected.locked,
                    onDuplicate = { boardViewModel.duplicate(selected.id) },
                    onLock = { boardViewModel.toggleLock(selected.id) },
                    onDelete = { confirmDeleteElement = true },
                    onMore = { sheet = BoardSheet.AllTools },
                )
            }

            /* aviso */
            val notice = state.notice ?: state.error
            if (notice != null) {
                Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 12.dp)) {
                    NoticeBar(
                        message = notice,
                        undoable = state.noticeUndoable,
                        onUndo = boardViewModel::undoNotice,
                        onDismiss = boardViewModel::dismissNotice,
                    )
                }
            }
        }

        /* ---------------- barra de abajo ---------------- */
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Box(Modifier.fillMaxWidth().background(c.surface).navigationBarsPadding()) {
            if (selected != null) {
                PropertyBar(
                    element = selected,
                    onAction = { action ->
                        when (action) {
                            PropertyAction.Edit -> sheet = BoardSheet.Edit
                            PropertyAction.Font -> sheet = BoardSheet.FontPicker
                            PropertyAction.Size -> sheet = BoardSheet.SizePicker
                            PropertyAction.Replace -> { replacingImage = true; picker.launch(imageRequest()) }
                            PropertyAction.Frame -> sheet = BoardSheet.FramePicker
                            PropertyAction.Variant -> sheet = BoardSheet.ShapeVariant
                            PropertyAction.Swap -> sheet = BoardSheet.StickerReplace
                            PropertyAction.Layers -> sheet = BoardSheet.Layers
                            PropertyAction.Animate -> sheet = BoardSheet.Animate
                            PropertyAction.Done -> boardViewModel.select(null)
                        }
                    },
                )
            } else {
                CategoryBar(
                    items = buildList {
                        add(BarEntry(Icons.Outlined.ViewQuilt, "Plantillas", true) { sheet = BoardSheet.Templates })
                        add(BarEntry(Icons.Outlined.Widgets, "Elementos", true) { sheet = BoardSheet.Library })
                        add(BarEntry(Icons.Outlined.TextFields, "Texto", true) { sheet = BoardSheet.TextStep(false) })
                        add(BarEntry(Icons.Outlined.StickyNote2, "Nota", true) { sheet = BoardSheet.NoteStep })
                        add(
                            BarEntry(Icons.Outlined.PhotoLibrary, "Galería", true) {
                                pendingFrame = null
                                picker.launch(imageRequest())
                            },
                        )
                        add(
                            BarEntry(Icons.Outlined.ContentPaste, "Pegar", true) {
                                val (x, y) = viewCenter()
                                boardViewModel.pasteFromClipboard(x, y)
                            },
                        )
                        add(BarEntry(Icons.Outlined.Category, "Formas", true) { sheet = BoardSheet.ShapeStep })
                        add(BarEntry(Icons.Outlined.Star, "Stickers", true) { sheet = BoardSheet.StickerStep(false) })
                        add(BarEntry(Icons.Outlined.SentimentSatisfied, "Emojis", true) { sheet = BoardSheet.StickerStep(true) })
                        add(
                            BarEntry(Icons.Outlined.TableChart, "Tabla", true) {
                                val (x, y) = viewCenter()
                                boardViewModel.addTable(x, y)
                            },
                        )
                        add(BarEntry(Icons.Outlined.BarChart, "Gráfica", true) { sheet = BoardSheet.ChartStep })
                        add(BarEntry(Icons.Outlined.GridView, "Cuadrícula", true) { sheet = BoardSheet.GridStep })
                        add(BarEntry(Icons.Outlined.Palette, "Tema", true) { sheet = BoardSheet.Theme })
                        add(BarEntry(Icons.Outlined.Dashboard, "Tableros", true) { sheet = BoardSheet.Boards })
                        add(BarEntry(Icons.AutoMirrored.Outlined.Undo, "Deshacer", state.canUndo, boardViewModel::undo))
                        add(BarEntry(Icons.AutoMirrored.Outlined.Redo, "Rehacer", state.canRedo, boardViewModel::redo))
                        add(
                            BarEntry(Icons.Outlined.CenterFocusStrong, "Ajustar", true) {
                                zoom = fitZoom()
                                centerPage(zoom)
                            },
                        )
                        add(BarEntry(Icons.Outlined.Download, "Exportar", false) {})
                    },
                )
            }
        }
    }

    /* ---------------- hojas ---------------- */

    if (sheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = sheetState,
            containerColor = c.surfaceVariant,
        ) {
            val close = { sheet = null }
            val toLibrary = { sheet = BoardSheet.Library }

            when (val open = sheet) {
                BoardSheet.Library -> LibrarySheet(
                    onPick = { type ->
                        val (x, y) = viewCenter()
                        when (type) {
                            "TEXT" -> sheet = BoardSheet.TextStep(true)
                            "NOTE" -> sheet = BoardSheet.NoteStep
                            "IMAGE" -> sheet = BoardSheet.ImageStep
                            "SHAPE" -> sheet = BoardSheet.ShapeStep
                            "STICKER" -> sheet = BoardSheet.StickerStep(false)
                            "EMOJI" -> sheet = BoardSheet.StickerStep(true)
                            "CHART" -> sheet = BoardSheet.ChartStep
                            "GRID" -> sheet = BoardSheet.GridStep
                            "TABLE" -> { boardViewModel.addTable(x, y); close() }
                        }
                    },
                    onClose = close,
                )

                is BoardSheet.TextStep -> TextStepSheet(
                    "", DEFAULT_VISION_BOARD_FONT, DEFAULT_TEXT_FONT_SIZE, "Agregar",
                    if (open.fromLibrary) toLibrary else null, close,
                ) { text, font, size ->
                    val (x, y) = viewCenter()
                    boardViewModel.addText(text, x, y, font, size)
                    close()
                }

                BoardSheet.NoteStep -> NoteStepSheet("", "Agregar", toLibrary, close) { text ->
                    val (x, y) = viewCenter()
                    boardViewModel.addNote(text, x, y)
                    close()
                }

                BoardSheet.ImageStep -> ImageStepSheet(
                    frame = pendingFrame,
                    onFrameChange = { pendingFrame = it },
                    onPickFromGallery = { picker.launch(imageRequest()); close() },
                    onPasteFromClipboard = {
                        val (x, y) = viewCenter()
                        boardViewModel.pasteFromClipboard(x, y)
                        close()
                    },
                    onBack = toLibrary,
                    onClose = close,
                )

                BoardSheet.FramePicker -> selected?.let { element ->
                    ImageStepSheet(
                        frame = element.str("frameStyle"),
                        onFrameChange = { next ->
                            boardViewModel.editData(
                                element.id,
                                buildJsonObject {
                                    element.str("imageId")?.let { put("imageId", it) }
                                    element.str("url")?.let { put("url", it) }
                                    if (next != null) put("frameStyle", next)
                                    element.str("entrance")?.let { put("entrance", it) }
                                },
                            )
                            close()
                        },
                        onPickFromGallery = {},
                        onPasteFromClipboard = {},
                        onBack = null,
                        onClose = close,
                    )
                } ?: Box(Modifier.height(1.dp))

                BoardSheet.ShapeStep -> ShapeStepSheet(DEFAULT_SHAPE_FILL, toLibrary, close) { id, fill, textColor ->
                    val (x, y) = viewCenter()
                    boardViewModel.addShape(id, fill, textColor, x, y)
                    close()
                }

                is BoardSheet.StickerStep -> StickerStepSheet(
                    startOnEmoji = open.emoji,
                    onBack = toLibrary,
                    onClose = close,
                    onPickSticker = { id -> val (x, y) = viewCenter(); boardViewModel.addSticker(id, x, y); close() },
                    onPickEmoji = { id -> val (x, y) = viewCenter(); boardViewModel.addEmoji(id, x, y); close() },
                )

                BoardSheet.ChartStep -> ChartStepSheet(null, toLibrary, close) { id ->
                    val (x, y) = viewCenter()
                    boardViewModel.addChart(id, x, y)
                    close()
                }

                BoardSheet.GridStep -> GridStepSheet(null, toLibrary, close) { id ->
                    val (x, y) = viewCenter()
                    boardViewModel.addGrid(id, x, y)
                    close()
                }

                BoardSheet.Edit -> selected?.let { element ->
                    EditSheet(
                        element = element,
                        onClose = close,
                        onSave = { data -> boardViewModel.editData(element.id, data); close() },
                        onOpenShapePicker = { sheet = BoardSheet.ShapeVariant },
                        onOpenStickerPicker = { sheet = BoardSheet.StickerReplace },
                    )
                } ?: Box(Modifier.height(1.dp))

                BoardSheet.FontPicker -> selected?.let { element ->
                    FontSheet(element.str("font") ?: DEFAULT_VISION_BOARD_FONT, close) { id ->
                        boardViewModel.editData(
                            element.id,
                            buildJsonObject {
                                put("text", element.text.orEmpty())
                                put("font", id)
                                put("fontSize", fontSizeOf(element.int("fontSize")))
                                element.str("entrance")?.let { put("entrance", it) }
                            },
                        )
                        close()
                    }
                } ?: Box(Modifier.height(1.dp))

                BoardSheet.SizePicker -> selected?.let { element ->
                    SizeSheet(fontSizeOf(element.int("fontSize")), close) { size ->
                        boardViewModel.editData(
                            element.id,
                            buildJsonObject {
                                put("text", element.text.orEmpty())
                                put("font", element.str("font") ?: DEFAULT_VISION_BOARD_FONT)
                                put("fontSize", size)
                                element.str("entrance")?.let { put("entrance", it) }
                            },
                        )
                        close()
                    }
                } ?: Box(Modifier.height(1.dp))

                BoardSheet.ShapeVariant -> selected?.let { element ->
                    ShapeStepSheet(element.str("fill") ?: DEFAULT_SHAPE_FILL, null, close) { id, fill, textColor ->
                        boardViewModel.editData(
                            element.id,
                            buildJsonObject {
                                put("shape", id)
                                put("fill", fill)
                                put("textColor", textColor)
                                element.text?.let { put("text", it) }
                            },
                        )
                        close()
                    }
                } ?: Box(Modifier.height(1.dp))

                BoardSheet.StickerReplace -> selected?.let { element ->
                    StickerStepSheet(
                        startOnEmoji = element.str("emojiId") != null,
                        onBack = null,
                        onClose = close,
                        onPickSticker = { id ->
                            boardViewModel.editData(element.id, buildJsonObject { put("stickerId", id) })
                            close()
                        },
                        onPickEmoji = { id ->
                            boardViewModel.editData(element.id, buildJsonObject { put("emojiId", id) })
                            close()
                        },
                    )
                } ?: Box(Modifier.height(1.dp))

                BoardSheet.Layers -> SimpleListSheet(
                    "Capa",
                    listOf("Traer al frente" to "", "Subir una capa" to "", "Bajar una capa" to "", "Enviar al fondo" to ""),
                    close,
                ) { index ->
                    val direction = listOf("FRONT", "RAISE", "LOWER", "BACK")[index]
                    selected?.let { boardViewModel.changeLayer(it.id, direction) }
                    close()
                }

                BoardSheet.Animate -> SimpleListSheet(
                    "Animación de entrada",
                    ENTRANCE_ANIMATIONS.map { anim ->
                        anim.label to if (selected?.str("entrance") == anim.id) "Activa" else ""
                    },
                    close,
                ) { index ->
                    selected?.let { element ->
                        val merged = buildJsonObject {
                            element.data.forEach { (k, v) -> put(k, v) }
                            put("entrance", ENTRANCE_ANIMATIONS[index].id)
                        }
                        boardViewModel.editData(element.id, merged)
                    }
                    close()
                }

                BoardSheet.Templates -> TemplatesSheet(close) { template ->
                    boardViewModel.applyTemplate(template)
                    close()
                }

                BoardSheet.Theme -> ThemeSheet(state.board?.theme ?: "LIGHT", close) { id ->
                    boardViewModel.changeTheme(id)
                    close()
                }

                BoardSheet.Boards -> SimpleListSheet(
                    "Tus tableros",
                    state.boards.map { it.name to if (it.id == state.board?.id) "Actual" else "" } +
                        listOf("＋ Nuevo tablero" to "", "Eliminar este tablero" to ""),
                    close,
                ) { index ->
                    when (index) {
                        state.boards.size -> sheet = BoardSheet.NewBoard
                        state.boards.size + 1 -> { close(); confirmDeleteBoard = true }
                        else -> { boardViewModel.switchBoard(state.boards[index].id); close() }
                    }
                }

                BoardSheet.NewBoard -> NewBoardSheet(close) { name, themeId ->
                    boardViewModel.createBoard(name, themeId)
                    close()
                }

                BoardSheet.Tools -> SimpleListSheet(
                    "Herramientas",
                    listOf(
                        "Deshacer" to if (state.canUndo) "" else "nada que deshacer",
                        "Rehacer" to if (state.canRedo) "" else "nada que rehacer",
                        "Ajustar el encuadre" to "${(zoom * 100).roundToInt()}%",
                        "Exportar" to "por decidir",
                    ),
                    close,
                ) { index ->
                    when (index) {
                        0 -> boardViewModel.undo()
                        1 -> boardViewModel.redo()
                        2 -> { zoom = fitZoom(); centerPage(zoom) }
                        3 -> Unit
                    }
                    if (index != 3) close()
                }

                BoardSheet.AllTools -> AllToolsSheet(
                    tools = allTools(
                        hasSelection = selected != null,
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        open = { next -> sheet = next },
                        onUndo = { boardViewModel.undo(); close() },
                        onRedo = { boardViewModel.redo(); close() },
                        onDuplicate = { selected?.let { boardViewModel.duplicate(it.id) }; close() },
                        onLock = { selected?.let { boardViewModel.toggleLock(it.id) }; close() },
                        onDeleteElement = { close(); confirmDeleteElement = true },
                        onDeleteBoard = { close(); confirmDeleteBoard = true },
                        onResetView = { zoom = fitZoom(); centerPage(zoom); close() },
                    ),
                    onClose = close,
                )

                null -> Box(Modifier.height(1.dp))
            }
        }
    }

    /* ---------------- confirmaciones ---------------- */

    if (confirmDeleteElement) {
        AlertDialog(
            onDismissRequest = { confirmDeleteElement = false },
            title = { Text("Eliminar 1 elemento") },
            text = { Text("Se quitará del tablero. Podrás deshacerlo desde el aviso, durante unos segundos.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteElement = false
                    selected?.let { boardViewModel.deleteElement(it.id) }
                }) { Text("Eliminar", color = c.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteElement = false }) { Text("Cancelar") } },
            containerColor = c.surfaceVariant,
        )
    }

    if (confirmDeleteBoard) {
        AlertDialog(
            onDismissRequest = { confirmDeleteBoard = false },
            title = { Text("Eliminar este Vision Board") },
            text = {
                Text(
                    "«${state.board?.name.orEmpty()}» se eliminará con todos sus elementos. " +
                        "Si era el único, se abrirá uno nuevo en blanco.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteBoard = false
                    boardViewModel.deleteCurrentBoard()
                }) { Text("Eliminar", color = c.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteBoard = false }) { Text("Cancelar") } },
            containerColor = c.surfaceVariant,
        )
    }
}

private fun imageRequest() =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

/** Qué abre un toque largo sobre cada tipo: su propiedad principal. */
private fun primarySheetFor(element: BoardElement): BoardSheet = when (element.type) {
    "TEXT", "NOTE" -> BoardSheet.Edit
    "IMAGE" -> BoardSheet.FramePicker
    "SHAPE" -> BoardSheet.Edit
    "STICKER" -> BoardSheet.StickerReplace
    "CHART", "GRID" -> BoardSheet.Edit
    else -> BoardSheet.Layers
}

/* =========================================================
   QUÉ HOJA ESTÁ ABIERTA
   ========================================================= */

sealed interface BoardSheet {
    data object Library : BoardSheet
    data class TextStep(val fromLibrary: Boolean) : BoardSheet
    data object NoteStep : BoardSheet
    data object ImageStep : BoardSheet
    data object ShapeStep : BoardSheet
    data class StickerStep(val emoji: Boolean) : BoardSheet
    data object ChartStep : BoardSheet
    data object GridStep : BoardSheet
    data object Edit : BoardSheet
    data object FontPicker : BoardSheet
    data object SizePicker : BoardSheet
    data object FramePicker : BoardSheet
    data object ShapeVariant : BoardSheet
    data object StickerReplace : BoardSheet
    data object Layers : BoardSheet
    data object Animate : BoardSheet
    data object Templates : BoardSheet
    data object Theme : BoardSheet
    data object Boards : BoardSheet
    data object NewBoard : BoardSheet
    data object Tools : BoardSheet
    data object AllTools : BoardSheet
}

/* =========================================================
   CABECERA
   ========================================================= */

@Composable
private fun EditorHeader(
    title: String,
    save: SaveStatus,
    onHome: () -> Unit,
    onMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val c = VidaTheme.colors
    // EL RELOJ Y LA BATERÍA NO SON NUESTROS. `MainActivity` dibuja de borde a
    // borde, así que sin este hueco la cabecera se pinta DEBAJO de la barra
    // de estado y el título acaba bajo la hora. `VidaAppBar` ya lo hacía; al
    // escribir una cabecera propia había que traerse también esto.
    Column(Modifier.background(c.surface).statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // La casa: la salida del editor. Sin ella, una ventana sin barra
            // de secciones sería una ventana sin puerta.
            Box(
                Modifier
                    .size(44.dp)
                    .background(c.primaryContainer, RoundedCornerShape(14.dp))
                    .clickable(onClick = onHome),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Home, "Volver a Inicio", Modifier.size(20.dp), tint = c.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = c.text,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (save) {
                        SaveStatus.Saved -> {
                            Icon(Icons.Outlined.Check, null, Modifier.size(12.dp), tint = c.successText)
                            Text("Guardado", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = c.successText)
                        }
                        SaveStatus.Saving -> Text(
                            "Guardando…",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = c.textSecondary,
                        )
                        is SaveStatus.Failed -> {
                            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(12.dp), tint = c.error)
                            Text(
                                "Sin guardar · Reintentar",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = c.error,
                                modifier = Modifier.clickable(onClick = onRetry),
                            )
                        }
                    }
                }
            }
            Box(Modifier.size(44.dp).clickable(onClick = onMore), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MoreVert, "Todas las herramientas", Modifier.size(21.dp), tint = c.text)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

/* =========================================================
   UN ELEMENTO EN LA PÁGINA
   ========================================================= */

@Composable
private fun CanvasElement(
    element: BoardElement,
    theme: BoardTheme,
    image: androidx.compose.ui.graphics.ImageBitmap?,
    selected: Boolean,
    zoom: Float,
    pan: Offset,
    onSelect: () -> Unit,
    onOpenProperties: () -> Unit,
    onCommit: (Float, Float, Float, Float, Float) -> Unit,
) {
    // La posición vive en la composición durante el gesto y solo sube al
    // servidor al soltar: así el dedo nunca espera a la red. Siempre en
    // coordenadas DEL TABLERO; la pantalla se calcula al colocar.
    var x by remember(element.id, element.x, element.version) { mutableFloatStateOf(element.x) }
    var y by remember(element.id, element.y, element.version) { mutableFloatStateOf(element.y) }
    var w by remember(element.id, element.width, element.version) { mutableFloatStateOf(element.width) }
    var h by remember(element.id, element.height, element.version) { mutableFloatStateOf(element.height) }
    var rotation by remember(element.id, element.rotation, element.version) { mutableFloatStateOf(element.rotation) }

    val density = LocalDensity.current
    /** Un arrastre en píxeles de pantalla, en puntos del tablero. */
    fun toBoard(drag: Offset): Pair<Float, Float> =
        with(density) { (drag.x / zoom).toDp().value to (drag.y / zoom).toDp().value }

    Box(
        Modifier
            .offset {
                IntOffset(
                    (pan.x + with(density) { x.dp.toPx() } * zoom).roundToInt(),
                    (pan.y + with(density) { y.dp.toPx() } * zoom).roundToInt(),
                )
            }
            .size((w * zoom).dp, (h * zoom).dp)
            // Solo se pide capa cuando hay giro de verdad. `graphicsLayer`
            // crea una capa de render por elemento, y en un tablero de veinte
            // imágenes eso son veinte capas con su memoria para algo que casi
            // ninguna usa: la mayoría están a cero grados.
            .then(if (rotation != 0f) Modifier.graphicsLayer(rotationZ = rotation) else Modifier)
            .pointerInput(element.id) {
                detectTapGestures(onTap = { onSelect() }, onLongPress = { onOpenProperties() })
            }
            .pointerInput(element.id, element.locked, zoom) {
                if (element.locked) return@pointerInput
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDragEnd = { onCommit(x, y, w, h, rotation) },
                ) { change, drag ->
                    // Consumir es lo que impide que este arrastre mueva además
                    // la página entera por debajo.
                    change.consume()
                    val (dx, dy) = toBoard(drag)
                    x += dx
                    y += dy
                }
            },
    ) {
        BoardElementContent(element, theme, image, zoom, Modifier.fillMaxSize())

        if (selected) {
            val accent = theme.colors.accent
            // Los tiradores miden lo mismo pase lo que pase con el zoom: son
            // objetivos para un dedo, no parte del dibujo.
            Box(Modifier.fillMaxSize().border(1.5.dp, accent, RoundedCornerShape(1.dp)))

            if (!element.locked) {
                SideHandle(Alignment.CenterStart, 5.dp, 22.dp, accent, vertical = true, onEnd = { onCommit(x, y, w, h, rotation) }) { dx, _ ->
                    val (bx, _) = toBoard(Offset(dx, 0f))
                    val nw = (w - bx).coerceAtLeast(24f); x += (w - nw); w = nw
                }
                SideHandle(Alignment.CenterEnd, 5.dp, 22.dp, accent, vertical = true, onEnd = { onCommit(x, y, w, h, rotation) }) { dx, _ ->
                    val (bx, _) = toBoard(Offset(dx, 0f))
                    w = (w + bx).coerceAtLeast(24f)
                }
                SideHandle(Alignment.TopCenter, 22.dp, 5.dp, accent, vertical = false, onEnd = { onCommit(x, y, w, h, rotation) }) { _, dy ->
                    val (_, by) = toBoard(Offset(0f, dy))
                    val nh = (h - by).coerceAtLeast(24f); y += (h - nh); h = nh
                }
                SideHandle(Alignment.BottomCenter, 22.dp, 5.dp, accent, vertical = false, onEnd = { onCommit(x, y, w, h, rotation) }) { _, dy ->
                    val (_, by) = toBoard(Offset(0f, dy))
                    h = (h + by).coerceAtLeast(24f)
                }

                CornerHandle(Alignment.TopStart, accent, { onCommit(x, y, w, h, rotation) }) { d ->
                    val (dx, dy) = toBoard(d)
                    val nw = (w - dx).coerceAtLeast(24f); val nh = (h - dy).coerceAtLeast(24f)
                    x += (w - nw); y += (h - nh); w = nw; h = nh
                }
                CornerHandle(Alignment.TopEnd, accent, { onCommit(x, y, w, h, rotation) }) { d ->
                    val (dx, dy) = toBoard(d)
                    val nh = (h - dy).coerceAtLeast(24f); y += (h - nh); h = nh
                    w = (w + dx).coerceAtLeast(24f)
                }
                CornerHandle(Alignment.BottomStart, accent, { onCommit(x, y, w, h, rotation) }) { d ->
                    val (dx, dy) = toBoard(d)
                    val nw = (w - dx).coerceAtLeast(24f); x += (w - nw); w = nw
                    h = (h + dy).coerceAtLeast(24f)
                }
                CornerHandle(Alignment.BottomEnd, accent, { onCommit(x, y, w, h, rotation) }) { d ->
                    val (dx, dy) = toBoard(d)
                    w = (w + dx).coerceAtLeast(24f); h = (h + dy).coerceAtLeast(24f)
                }

                // GIRAR Y MOVER, debajo del elemento: mover con su propio
                // botón permite colocar un elemento pequeño sin tener que
                // acertarle al contenido con el dedo.
                Row(
                    Modifier.align(Alignment.BottomCenter).offset(y = 38.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RoundHandle(
                        Icons.Outlined.Rotate90DegreesCcw, "Girar", accent,
                        onEnd = { onCommit(x, y, w, h, rotation) },
                    ) { drag -> rotation = (rotation + drag.x / 3.5f) % 360f }

                    RoundHandle(
                        Icons.Outlined.OpenWith, "Mover", accent,
                        onEnd = { onCommit(x, y, w, h, rotation) },
                    ) { drag ->
                        val (dx, dy) = toBoard(drag)
                        x += dx
                        y += dy
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SideHandle(
    alignment: Alignment,
    width: Dp,
    height: Dp,
    accent: Color,
    vertical: Boolean,
    onEnd: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    Box(
        Modifier
            .align(alignment)
            .size(if (vertical) 30.dp else 46.dp, if (vertical) 46.dp else 30.dp)
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = onEnd) { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width, height)
                .background(Color.White, RoundedCornerShape(999.dp))
                .border(1.5.dp, accent, RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun BoxScope.CornerHandle(
    alignment: Alignment,
    accent: Color,
    onEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    Box(
        Modifier
            .align(alignment)
            .size(34.dp)
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = onEnd) { change, drag ->
                    change.consume()
                    onDrag(drag)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(14.dp).background(Color.White, CircleShape).border(1.5.dp, accent, CircleShape))
    }
}

@Composable
private fun RoundHandle(
    icon: ImageVector,
    label: String,
    accent: Color,
    onEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    Box(
        Modifier
            .size(34.dp)
            .background(Color.White, CircleShape)
            .border(1.5.dp, accent, CircleShape)
            .pointerInput(label) {
                detectDragGestures(onDragEnd = onEnd) { change, drag ->
                    change.consume()
                    onDrag(drag)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(18.dp), tint = accent)
    }
}

/* =========================================================
   BURBUJA DE ACCIONES RÁPIDAS
   ========================================================= */

@Composable
private fun QuickBubble(
    width: Dp,
    modifier: Modifier,
    locked: Boolean,
    onDuplicate: () -> Unit,
    onLock: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
) {
    val c = VidaTheme.colors
    Row(
        modifier
            .width(width)
            .height(46.dp)
            .shadow(8.dp, RoundedCornerShape(23.dp))
            .background(c.surface, RoundedCornerShape(23.dp))
            .border(1.dp, c.line, RoundedCornerShape(23.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BubbleIcon(Icons.Outlined.ContentCopy, "Duplicar", onDuplicate)
        BubbleIcon(
            if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            if (locked) "Desbloquear" else "Bloquear",
            onLock,
            tint = if (locked) c.primary else null,
        )
        BubbleIcon(Icons.Outlined.Delete, "Eliminar", onDelete, tint = c.error, enabled = !locked)
        BubbleIcon(Icons.Outlined.MoreHoriz, "Más", onMore)
    }
}

@Composable
private fun BubbleIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val c = VidaTheme.colors
    Box(Modifier.size(44.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(
            icon, label, Modifier.size(19.dp),
            tint = if (!enabled) c.textTertiary.copy(alpha = 0.4f) else tint ?: c.text,
        )
    }
}

/* =========================================================
   BARRA DE ABAJO  ·  categorías
   ========================================================= */

/** Una entrada de la barra de abajo. */
private data class BarEntry(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean,
    val action: () -> Unit,
)

/**
 * La barra de categorías: TODO lo que la Web ofrece, a un solo toque.
 *
 * Se desplaza en horizontal porque son dieciocho entradas y ninguna merece
 * quedar escondida en un menú de segundo nivel — en la Web tampoco lo están.
 */
@Composable
private fun CategoryBar(items: List<BarEntry>) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { entry ->
            BarItem(entry.icon, entry.label, entry.action, entry.enabled)
        }
    }
}

/* =========================================================
   BARRA DE ABAJO  ·  propiedades del elemento
   ========================================================= */

enum class PropertyAction { Edit, Font, Size, Replace, Frame, Variant, Swap, Layers, Animate, Done }

@Composable
private fun PropertyBar(element: BoardElement, onAction: (PropertyAction) -> Unit) {
    val c = VidaTheme.colors
    val items: List<Triple<ImageVector, String, PropertyAction>> = when (element.type) {
        "TEXT" -> listOf(
            Triple(Icons.Outlined.Edit, "Editar", PropertyAction.Edit),
            Triple(Icons.Outlined.TextFields, "Fuente", PropertyAction.Font),
            Triple(Icons.Outlined.FormatSize, "Tamaño", PropertyAction.Size),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        "NOTE" -> listOf(
            Triple(Icons.Outlined.Edit, "Editar", PropertyAction.Edit),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        "IMAGE" -> listOf(
            Triple(Icons.Outlined.SwapHoriz, "Reemplazar", PropertyAction.Replace),
            Triple(Icons.Outlined.Crop, "Marco", PropertyAction.Frame),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        "SHAPE" -> listOf(
            Triple(Icons.Outlined.Palette, "Color", PropertyAction.Edit),
            Triple(Icons.Outlined.Widgets, "Variante", PropertyAction.Variant),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        "STICKER" -> listOf(
            Triple(Icons.Outlined.SwapHoriz, "Cambiar", PropertyAction.Swap),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        "CHART" -> listOf(
            Triple(Icons.Outlined.Edit, "Tipo", PropertyAction.Edit),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        "GRID" -> listOf(
            Triple(Icons.Outlined.Edit, "Diseño", PropertyAction.Edit),
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
        else -> listOf(
            Triple(Icons.Outlined.Layers, "Capa", PropertyAction.Layers),
            Triple(Icons.Outlined.AutoAwesome, "Animar", PropertyAction.Animate),
        )
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Un elemento bloqueado conserva sus botones, deshabilitados: la
            // fila no cambia de ancho y el dedo no acaba pulsando otro.
            items.forEach { (icon, label, action) ->
                BarItem(icon, label, { onAction(action) }, enabled = !element.locked)
            }
        }
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .size(46.dp)
                .background(VidaTheme.colors.primary, CircleShape)
                .clickable { onAction(PropertyAction.Done) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, "Listo", Modifier.size(22.dp), tint = c.onPrimary)
        }
    }
}

@Composable
private fun BarItem(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val c = VidaTheme.colors
    Column(
        Modifier
            .width(72.dp)
            .height(58.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val color = if (enabled) c.text else c.textTertiary.copy(alpha = 0.45f)
        Icon(icon, null, Modifier.size(21.dp), tint = color)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/* =========================================================
   AVISO
   ========================================================= */

@Composable
private fun NoticeBar(message: String, undoable: Boolean, onUndo: () -> Unit, onDismiss: () -> Unit) {
    val c = VidaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.text, RoundedCornerShape(16.dp))
            // Tocar el aviso lo cierra SIEMPRE, también tras un borrado: si el
            // único toque disponible fuera «Deshacer», cerrar el mensaje
            // significaría recuperar lo que se acaba de tirar.
            .clickable(onClick = onDismiss)
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = c.surface,
            modifier = Modifier.weight(1f),
        )
        if (undoable) {
            Box(
                Modifier
                    .height(34.dp)
                    .background(c.surface.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onUndo)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Deshacer",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                    color = c.primaryContainer,
                )
            }
        }
    }
}

/* =========================================================
   TODAS LAS HERRAMIENTAS
   ========================================================= */

private fun allTools(
    hasSelection: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    open: (BoardSheet) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDuplicate: () -> Unit,
    onLock: () -> Unit,
    onDeleteElement: () -> Unit,
    onDeleteBoard: () -> Unit,
    onResetView: () -> Unit,
): List<ToolEntry> = listOf(
    ToolEntry("Elementos", Icons.Outlined.Widgets, true) { open(BoardSheet.Library) },
    ToolEntry("Plantillas", Icons.Outlined.ViewQuilt, true) { open(BoardSheet.Templates) },
    ToolEntry("Texto", Icons.Outlined.TextFields, true) { open(BoardSheet.TextStep(false)) },
    ToolEntry("Tema del tablero", Icons.Outlined.Palette, true) { open(BoardSheet.Theme) },
    ToolEntry("Exportar (por decidir)", Icons.Outlined.Download, false) {},
    ToolEntry("Cambiar de tablero", Icons.Outlined.SwapHoriz, true) { open(BoardSheet.Boards) },
    ToolEntry("Nuevo tablero", Icons.Outlined.NoteAdd, true) { open(BoardSheet.NewBoard) },
    ToolEntry("Eliminar tablero", Icons.Outlined.DeleteForever, true, onDeleteBoard),
    ToolEntry("Deshacer", Icons.AutoMirrored.Outlined.Undo, canUndo, onUndo),
    ToolEntry("Rehacer", Icons.AutoMirrored.Outlined.Redo, canRedo, onRedo),
    ToolEntry("Ajustar el encuadre", Icons.Outlined.CenterFocusStrong, true, onResetView),
    ToolEntry("Editar elemento", Icons.Outlined.Edit, hasSelection) { open(BoardSheet.Edit) },
    ToolEntry("Capas", Icons.Outlined.Layers, hasSelection) { open(BoardSheet.Layers) },
    ToolEntry("Animar", Icons.Outlined.AutoAwesome, hasSelection) { open(BoardSheet.Animate) },
    ToolEntry("Duplicar", Icons.Outlined.ContentCopy, hasSelection, onDuplicate),
    ToolEntry("Bloquear", Icons.Outlined.Lock, hasSelection, onLock),
    ToolEntry("Eliminar elemento", Icons.Outlined.Delete, hasSelection, onDeleteElement),
)
