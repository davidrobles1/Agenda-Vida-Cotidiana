package com.vidacotidiana.app.feature.documents


import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.Dialogo
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border

/**
 * EL VISOR DE DOCUMENTOS, DENTRO DE COTIDIANA.
 *
 * POR QUÉ CAMBIA EL CRITERIO ANTERIOR. Hasta ahora «ver documento» lanzaba un
 * `ACTION_VIEW` al lector del sistema, con el argumento —razonable— de que
 * ningún visor propio iba a superar al que el usuario ya eligió. Pero ver un
 * documento no es lo mismo que abrirlo con otra aplicación:
 *
 *  · El archivo SALE de la aplicación. Para entregarlo hay que escribirlo en
 *    la caché y cederlo por `FileProvider` a un tercero, que puede copiarlo,
 *    subirlo o conservarlo. Para una sección que guarda identificaciones,
 *    pólizas y contratos, eso es exactamente lo que no debe pasar sin que
 *    nadie lo pida.
 *  · Si no hay lector instalado, no pasaba NADA salvo un mensaje de error.
 *  · Volver a Cotidiana exigía salir de la otra aplicación.
 *
 * Compartir con otra aplicación SIGUE existiendo, como acción aparte y
 * explícita («Abrir en otra app»). La diferencia es que ahora es una decisión
 * y no el único camino.
 *
 * NO SE AÑADE NINGUNA DEPENDENCIA. `PdfRenderer` es parte de Android desde la
 * API 21 y las imágenes las decodifica el propio framework. El backend solo
 * admite PDF, PNG, JPEG, WEBP y GIF (`ALLOWED_CONTENT_TYPES`), así que entre
 * las dos ramas queda cubierto todo lo que puede haber guardado.
 */
@Composable
fun DocumentViewerScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    documentId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val doc = state.data.documents.firstOrNull { it.id == documentId }
    val acceso = rememberResourceAccess(viewModel)

    var paginas by remember(documentId) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var imagen by remember(documentId) { mutableStateOf<ImageBitmap?>(null) }
    var cargando by remember(documentId) { mutableStateOf(true) }
    var fallo by remember(documentId) { mutableStateOf<String?>(null) }

    LaunchedEffect(documentId) {
        cargando = true
        fallo = null
        viewModel.documentBytesFor(documentId)
            .onSuccess { bytes ->
                val nombre = doc?.name.orEmpty()
                val esPdf = bytes.size > 4 && bytes[0] == '%'.code.toByte() &&
                    bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte() &&
                    bytes[3] == 'F'.code.toByte()
                withContext(Dispatchers.IO) {
                    if (esPdf) {
                        paginas = renderPdf(bytes)
                    } else {
                        imagen = android.graphics.BitmapFactory
                            .decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                }
                if (paginas.isEmpty() && imagen == null) {
                    fallo = "No pudimos mostrar «$nombre» aquí. Puedes abrirlo con otra aplicación."
                }
                cargando = false
            }
            .onFailure { e ->
                fallo = e.message ?: "No se pudo descargar el documento."
                cargando = false
            }
    }

    VidaScreen(
        title = doc?.name ?: "Documento",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            // Compartir con la familia: la misma mecánica que el resto de los
            // recursos, que en Documentos no existía —solo estaba el selector
            // del sistema, que manda el archivo fuera.
            // SOLO EL DUEÑO decide quién tiene acceso. `doc` sale de la lista
            // PROPIA; si es nulo, el documento es uno que te compartieron y
            // aquí sólo se puede ver (ADR-025).
            if (doc != null) {
                VidaSmallButton(
                    "Acceso",
                    { acceso.abrir(ShareableKind.DOCUMENT, documentId, doc.name) },
                    ghost = true,
                )
            }
        },
    ) {
        when {
            cargando -> LoadingRows(3)
            fallo != null -> VidaError(
                title = "No se puede ver aquí",
                body = fallo.orEmpty(),
                onRetry = { navController.popBackStack() },
            )
            // UN PDF ES UNA TIRA DE PÁGINAS, no una sola imagen: se recorre en
            // vertical y cada página se dibuja a su proporción real, que es lo
            // que hace que un contrato de ocho hojas se lea como un contrato.
            //
            // COLUMNA Y NO `LazyColumn`, aunque una lista perezosa sería lo
            // natural: el contenido de `VidaScreen` YA es un `verticalScroll`,
            // y meter dentro un contenedor que también desplaza lo mide con
            // altura infinita — Compose lo rechaza y tira la aplicación al
            // abrir el documento. Lo comprobé en el teléfono. Perder el
            // reciclado no cuesta nada aquí porque las páginas ya están todas
            // rasterizadas en memoria antes de pintar la primera.
            paginas.isNotEmpty() -> Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                paginas.forEachIndexed { i, pagina ->
                    // CADA PÁGINA ES UNA HOJA, con su borde y su número.
                    //
                    // Sin esto, un documento cuyas páginas están en blanco se
                    // veía como una pantalla vacía y no había forma de saber si
                    // el archivo estaba así o si el visor no había cargado
                    // nada. Pasó de verdad. El marco dice «aquí hay una hoja» y
                    // el rótulo dice cuántas son.
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (paginas.size > 1) {
                            Text(
                                "Página ${i + 1} de ${paginas.size}",
                                style = VidaTheme.type.micro,
                                color = c.textTertiary,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(pagina.width.toFloat() / pagina.height)
                                .background(c.surface, RoundedCornerShape(10.dp))
                                .border(1.dp, c.border, RoundedCornerShape(10.dp)),
                        ) {
                            Image(
                                bitmap = pagina,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            // Y si además NO TIENE TINTA, se dice. Es la
                            // diferencia entre «tu archivo está vacío» y «esto
                            // está roto», que desde fuera se ven igual.
                            if (pagina.estaEnBlanco()) {
                                Text(
                                    "Esta página está en blanco.",
                                    style = VidaTheme.type.caption,
                                    color = c.textTertiary,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                    }
                }
            }
            // Por la misma razón, la imagen se ata a su proporción en vez de
            // llenar el alto: `fillMaxSize` dentro de un scroll es infinito.
            imagen != null -> Zoomable(
                ratio = imagen!!.width.toFloat() / imagen!!.height,
            ) {
                Image(
                    bitmap = imagen!!,
                    contentDescription = doc?.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    acceso.Dialogo(viewModel)
}

/**
 * ¿Esta página tiene algo dibujado?
 *
 * Se mira una retícula de puntos, no la página entera: leer dos millones de
 * píxeles por hoja para responder «sí» en el primero que discrepe sería tirar
 * el tiempo. Con 24×24 muestras se detecta cualquier mancha que una persona
 * vería, y una página de verdad en blanco recorre las 576 sin encontrar nada.
 */
private fun ImageBitmap.estaEnBlanco(): Boolean {
    val bmp = runCatching { asAndroidBitmap() }.getOrNull() ?: return false
    val pasoX = (bmp.width / 24).coerceAtLeast(1)
    val pasoY = (bmp.height / 24).coerceAtLeast(1)
    val referencia = bmp.getPixel(0, 0)
    var x = 0
    while (x < bmp.width) {
        var y = 0
        while (y < bmp.height) {
            if (bmp.getPixel(x, y) != referencia) return false
            y += pasoY
        }
        x += pasoX
    }
    return true
}

/** Pellizcar y arrastrar sobre una imagen. Un PDF ya se recorre en vertical. */
@Composable
private fun Zoomable(ratio: Float, content: @Composable () -> Unit) {
    var zoom by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(if (ratio.isFinite() && ratio > 0f) ratio else 1f)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                    // Con el zoom a 1 no hay nada que desplazar: dejar el
                    // arrastre libre ahí sacaría la imagen de la pantalla.
                    if (zoom > 1f) {
                        panX += pan.x
                        panY += pan.y
                    } else {
                        panX = 0f; panY = 0f
                    }
                }
            }
            .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = panX, translationY = panY),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Los bytes de un PDF, página a página.
 *
 * `PdfRenderer` necesita un descriptor de archivo SEEKABLE, así que hay que
 * pasar por disco; se escribe en la caché y se borra al terminar. El ancho de
 * render se fija en 1400 px: por encima de eso un móvil no gana nitidez y sí
 * gasta memoria de sobra, y un documento de veinte páginas empieza a pesar.
 */
private fun renderPdf(bytes: ByteArray): List<ImageBitmap> {
    val tmp = File.createTempFile("vista", ".pdf")
    return try {
        tmp.writeBytes(bytes)
        ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                (0 until renderer.pageCount).map { i ->
                    renderer.openPage(i).use { page ->
                        val ancho = minOf(1400, page.width * 2)
                        val alto = (ancho.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                        // Un PDF no trae fondo: sin esto las zonas sin tinta
                        // quedan transparentes y se ven negras sobre tema
                        // oscuro.
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp.asImageBitmap()
                    }
                }
            }
        }
    } catch (e: Exception) {
        emptyList()
    } finally {
        tmp.delete()
    }
}

/**
 * La miniatura del detalle: la PRIMERA página de un PDF o la imagen reducida.
 *
 * Mismo camino que el visor —`PdfRenderer` y el decodificador del sistema, sin
 * dependencias—, pero a 720 px de ancho: es un recuadro de previsualización, y
 * rasterizar las veinte páginas de un contrato para enseñar la portada sería
 * gastar memoria en balde.
 */
internal fun renderPreview(bytes: ByteArray): ImageBitmap? {
    val esPdf = bytes.size > 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()
    if (!esPdf) {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 720) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    }
    val tmp = File.createTempFile("previa", ".pdf")
    return try {
        tmp.writeBytes(bytes)
        ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val ancho = minOf(720, page.width * 2)
                    val alto = (ancho.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp.asImageBitmap()
                }
            }
        }
    } catch (e: Exception) {
        null
    } finally {
        tmp.delete()
    }
}
