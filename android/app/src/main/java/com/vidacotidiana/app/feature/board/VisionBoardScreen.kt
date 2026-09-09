package com.vidacotidiana.app.feature.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.data.BoardElement
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaChipRow
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSearchField
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.openDrawerAction
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

/**
 * Vision Board — un LIENZO, no una lista de imágenes.
 *
 * Los elementos se arrastran de verdad (`detectDragGestures`) y su posición se
 * GUARDA al soltarlos, que es la naturaleza de esta sección: la relación
 * espacial entre intenciones es el contenido, no un adorno.
 *
 * Las imágenes entran por dos caminos, los dos del sistema: pegar lo que haya
 * en el portapapeles —una imagen copiada en Chrome— o elegirla en el selector
 * de fotos. Ninguno pide permisos de almacenamiento.
 */
@Composable
fun VisionBoardScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    boardViewModel: VisionBoardViewModel = hiltViewModel(),
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    val state by boardViewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    var selected by remember { mutableStateOf<String?>(null) }
    var composing by remember { mutableStateOf("") }
    // Dónde cae lo próximo que se añada: en cascada, para que dos elementos
    // seguidos no queden uno encima del otro.
    var dropIndex by remember { mutableStateOf(0) }
    fun nextDrop(): Pair<Float, Float> {
        val step = (dropIndex % 5) * 22f
        dropIndex++
        return 24f + step to 24f + step
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val (x, y) = nextDrop()
            boardViewModel.addImage(uri, x, y)
        }
    }

    VidaScreen(
        title = "Vision Board",
        subtitle = "Tu tablero de intenciones.",
        onNavigationClick = openDrawerAction(drawerState, scope),
    ) {
        // Las tres formas de traer algo al lienzo, delante y no escondidas.
        StaggeredAppear(0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                VidaSmallButton(
                    if (state.uploading) "Subiendo…" else "Pegar imagen",
                    {
                        val (x, y) = nextDrop()
                        boardViewModel.pasteFromClipboard(x, y)
                    },
                )
                VidaSmallButton(
                    "Elegir foto",
                    {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    ghost = true,
                )
                VidaSmallButton(
                    "Forma",
                    {
                        val (x, y) = nextDrop()
                        boardViewModel.addShape("rectangle", x, y)
                    },
                    ghost = true,
                )
            }
        }

        (state.notice ?: state.error)?.let { message ->
            StaggeredAppear(1) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) c.error else c.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures { boardViewModel.dismissNotice() } },
                )
            }
        }

        StaggeredAppear(2) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(c.sunken, RoundedCornerShape(spec.radii.card))
                    .border(spec.borderWidth, c.line, RoundedCornerShape(spec.radii.card))
                    .clip(RoundedCornerShape(spec.radii.card))
                    // Tocar el fondo deselecciona: es cómo se cierra una
                    // selección en cualquier lienzo.
                    .pointerInput(Unit) { detectTapGestures { selected = null } },
            ) {
                val board = state.board
                when {
                    state.loading && board == null -> Text(
                        "Abriendo tu tablero…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    // FIRST_USE. Sin acción propia a propósito: «Pegar imagen» y
                    // «Elegir foto» están justo encima del lienzo, y repetirlas
                    // aquí sería el tercer botón para lo mismo.
                    //
                    // Ancla arriba a la izquierda, no al centro: el envoltorio
                    // centrado venía de cuando `EmptyState` se centraba solo, y
                    // devolvía a esta pantalla el comportamiento que el sistema
                    // ya no tiene. Así el texto arranca en el mismo eje que el
                    // resto del contenido.
                    board == null || board.elements.isEmpty() -> Box(
                        Modifier.align(Alignment.TopStart).padding(VidaSpacing.lg),
                    ) {
                        EmptyState(
                            title = "Tu tablero está en blanco",
                            body = "Pega una imagen que hayas copiado o elige una foto: los dos botones están justo arriba.",
                        )
                    }
                    else -> board.elements.forEach { element ->
                        DraggableElement(
                            element = element,
                            image = element.imageId?.let { state.images[it] },
                            selected = selected == element.id,
                            onSelect = { selected = element.id },
                            onDropped = { x, y -> boardViewModel.onDropped(element, x, y) },
                            onDelete = {
                                boardViewModel.delete(element)
                                selected = null
                            },
                        )
                    }
                }
            }
        }

        // Escribir una intención: el mismo gesto que en la libreta del día.
        StaggeredAppear(3) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                Box(Modifier.weight(1f)) {
                    VidaSearchField(composing, { composing = it }, "Escribe una intención…")
                }
                VidaSmallButton(
                    "Añadir",
                    {
                        val (x, y) = nextDrop()
                        boardViewModel.addText(composing, x, y)
                        composing = ""
                    },
                )
            }
        }

        StaggeredAppear(4) {
            Text(
                "Arrastra un elemento para moverlo; al soltarlo se guarda su sitio. " +
                    "Tócalo para seleccionarlo y poder quitarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

/**
 * Un elemento del lienzo, arrastrable.
 *
 * La posición se lleva en estado local durante el arrastre y solo sube al
 * servidor al soltar: así el dedo nunca espera a la red.
 */
@Composable
private fun DraggableElement(
    element: BoardElement,
    image: androidx.compose.ui.graphics.ImageBitmap?,
    selected: Boolean,
    onSelect: () -> Unit,
    onDropped: (Float, Float) -> Unit,
    onDelete: () -> Unit,
) {
    val c = VidaTheme.colors
    val spec = VidaTheme.spec
    // Se re-siembra cuando el servidor confirma una posición nueva.
    var x by remember(element.id, element.x) { mutableStateOf(element.x) }
    var y by remember(element.id, element.y) { mutableStateOf(element.y) }
    val shape = when (element.shape) {
        "circle" -> CircleShape
        else -> RoundedCornerShape(spec.radii.card)
    }

    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(element.width.dp, element.height.dp)
            .then(if (selected) Modifier.border(2.dp, c.primary, shape) else Modifier)
            .pointerInput(element.id) {
                detectTapGestures(onTap = { onSelect() })
            }
            .pointerInput(element.id, element.version) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDragEnd = { onDropped(x, y) },
                ) { change, drag ->
                    change.consume()
                    // Los elementos no se salen del lienzo por arriba ni por la
                    // izquierda: recuperarlos de ahí sería imposible.
                    x = (x + drag.x).coerceAtLeast(0f)
                    y = (y + drag.y).coerceAtLeast(0f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = "Imagen del tablero",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(element.width.dp, element.height.dp).clip(shape),
            )
            element.type == "IMAGE" -> Box(
                Modifier.size(element.width.dp, element.height.dp).background(c.surfaceVariant, shape),
                contentAlignment = Alignment.Center,
            ) {
                // La imagen existe pero sus bytes aún no han llegado.
                Text("…", style = MaterialTheme.typography.bodyMedium, color = c.textTertiary)
            }
            element.type == "SHAPE" -> Box(
                Modifier
                    .size(element.width.dp, element.height.dp)
                    .background(c.primaryContainer, shape)
                    .border(1.dp, c.primary, shape),
            )
            else -> Box(
                Modifier
                    .background(
                        if (spec.fonts.hand != null) Color.Transparent else c.secondContainer,
                        shape,
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    element.text.orEmpty(),
                    style = if (spec.fonts.hand != null) {
                        MaterialTheme.typography.titleLarge.copy(
                            fontFamily = spec.fonts.hand, fontSize = 21.sp, letterSpacing = 0.sp,
                        )
                    } else {
                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    },
                    color = if (spec.fonts.hand != null) c.textSecondary else c.second,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(c.error, CircleShape)
                    .pointerInput(element.id) { detectTapGestures { onDelete() } },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", style = MaterialTheme.typography.labelLarge, color = c.onPrimary)
            }
        }
    }
}
