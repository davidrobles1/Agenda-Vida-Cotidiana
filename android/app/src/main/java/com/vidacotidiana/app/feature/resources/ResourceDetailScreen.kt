package com.vidacotidiana.app.feature.resources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.app.Dialogo
import com.vidacotidiana.app.core.app.rememberResourceAccess
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.CreateResourceSheet
import com.vidacotidiana.app.core.ui.components.VidaSmallButton
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.navigation.Routes

/**
 * LA FICHA DE UN RECURSO — una ventana, no una hoja deslizante.
 *
 * QUÉ SUSTITUYE. Tocar una fila abría una hoja que subía desde abajo con el
 * título, el subtítulo y una pila de botones. Era una tercera forma de ver un
 * recurso —ni la lista ni un formulario—: para cambiar cualquier cosa había
 * que pulsar «Editar» y saltar a OTRA hoja, y no había sitio para enseñar el
 * documento que muchos recursos llevan detrás.
 *
 * AQUÍ SE VE Y SE EDITA EN EL MISMO SITIO. Los campos son los que declara
 * `CreatableResource` —es decir, exactamente los que el endpoint acepta— y se
 * pintan con el formulario que ya existía. No hay dos maneras de editar un
 * pago según por dónde se haya entrado.
 *
 * EL RECUADRO DEL DOCUMENTO va en medio, entre el nombre y las propiedades, y
 * tiene tres estados que no son decorativos:
 *
 *  · el recurso YA tiene documento → se ve, y al tocarlo se abre en el visor
 *    interno;
 *  · el recurso PUEDE tener uno y no lo tiene → el icono de subir, que lanza
 *    el mismo protocolo de carga que el alta;
 *  · el recurso no admite documento → no se dibuja nada. Un hueco vacío
 *    prometería algo que no existe.
 */
@Composable
fun ResourceDetailScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    resource: CreatableResource,
    resourceId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val acceso = rememberResourceAccess(viewModel)
    var confirmarBorrado by remember { mutableStateOf(false) }

    // El formulario se rellena con lo que hay GUARDADO. `requestEdit` ya sabe
    // traducir cada recurso a los valores del formulario, así que la ficha no
    // repite esa traducción para trece tipos.
    //
    // `abrirFormulario = false` es lo que impide que se abran DOS cosas: esta
    // ventana y, encima, la hoja deslizante del formulario global. Con la
    // bandera puesta a true —como estaba— tocar un recurso mostraba la ficha y
    // acto seguido la hoja subiendo por encima.
    //
    // Y SE LIMPIA AL SALIR. `editing` es el blanco de la próxima escritura:
    // si la ficha lo deja puesto al cerrarse, queda apuntando a un registro
    // que ya nadie está mirando. Antes no podía pasar porque `editing` solo se
    // encendía justo antes de abrir el formulario y se apagaba al cerrarlo;
    // ahora lo enciende cada apertura de ficha, así que también tiene que
    // apagarlo cada cierre.
    DisposableEffect(resource, resourceId) {
        viewModel.requestEdit(resource, resourceId, abrirFormulario = false)
        onDispose { viewModel.cancelCreate() }
    }

    val titulo = viewModel.resourceTitle(resource, resourceId)
    val kind = resource.shareableKind
    val adjunto = viewModel.attachedDocument(resource, resourceId)

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            containerColor = c.surfaceVariant,
            title = { Text("Eliminar") },
            text = { Text("«$titulo» se borrará para siempre. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    viewModel.deleteResource(resource, resourceId)
                    navController.popBackStack()
                }) { Text("Eliminar", color = c.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }

    Surface(Modifier.fillMaxSize(), color = c.surfaceVariant) {
        // El formulario gestiona su propio desplazamiento y su hueco de
        // teclado, así que NO se mete dentro de `VidaScreen`: anidar dos
        // contenedores que desplazan lo mide con altura infinita y Compose
        // tira la aplicación.
        CreateResourceSheet(
            resource = resource,
            initialValues = state.formInitialValues,
            editing = true,
            // LA IDENTIDAD DEL REGISTRO, no solo su tipo. Sin esto, ir de la
            // ficha de un documento a la de otro conservaba el archivo elegido
            // en el primero y al guardar lo escribía en el segundo.
            formKey = "${resource.name}:$resourceId",
            headline = titulo.ifBlank { "Ficha" },
            people = state.data.people.map { it.id to it.name },
            projects = state.data.projects.map { it.id to it.name },
            inventory = state.data.inventory.map {
                it.id to (if (it.location.isNullOrBlank()) it.name else "${it.name} · ${it.location}")
            },
            saving = state.saving,
            error = state.error,
            onSubmit = { values, file ->
                viewModel.create(resource, values, file)
                navController.popBackStack()
            },
            onPickFile = viewModel::readFile,
            onQuickCreate = viewModel::quickCreate,
            documentSlot = {
                DocumentBox(
                    adjunto = adjunto,
                    puedeLlevar = resource.admiteDocumento,
                    onVer = { docId -> navController.navigate(Routes.documentRoute(docId)) },
                    // SE CUELGA DE ESTE RECURSO. Antes esto abría la subida
                    // normal y el documento quedaba suelto: se subía, y la
                    // ficha seguía diciendo «adjuntar un documento».
                    onSubir = {
                        resource.attachWire?.let { viewModel.attachDocumentTo(it, resourceId) }
                    },
                )
            },
            actionsSlot = {
                Row(
                    Modifier.fillMaxWidth().padding(top = VidaSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
                ) {
                    kind?.let {
                        VidaSmallButton(
                            "Compartir",
                            { acceso.abrir(it, resourceId, titulo) },
                            ghost = true,
                        )
                    }
                    VidaSmallButton("Eliminar", { confirmarBorrado = true }, ghost = true)
                }
            },
            onCancel = { navController.popBackStack() },
        )
    }

    acceso.Dialogo(viewModel)
}

/**
 * EL RECUADRO DEL DOCUMENTO.
 *
 * Los tres estados de arriba, en un solo sitio, para que un recurso no pueda
 * enseñar «subir» y a la vez tener uno adjunto.
 */
@Composable
private fun DocumentBox(
    adjunto: Pair<String, String>?,
    puedeLlevar: Boolean,
    onVer: (String) -> Unit,
    onSubir: () -> Unit,
) {
    if (adjunto == null && !puedeLlevar) return
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Column(
        Modifier.fillMaxWidth().padding(vertical = VidaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("DOCUMENTO", style = t.eyebrow, color = c.textTertiary)
        Box(
            Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(if (adjunto != null) c.primaryContainer else c.sunken, RoundedCornerShape(18.dp))
                .border(
                    1.dp,
                    if (adjunto != null) c.primary.copy(alpha = 0.35f) else c.border,
                    RoundedCornerShape(18.dp),
                )
                .vidaClickable(onClick = { adjunto?.let { onVer(it.first) } ?: onSubir() }),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(
                            if (adjunto != null) c.primary.copy(alpha = 0.16f) else c.surface,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (adjunto != null) Icons.Outlined.Description else Icons.Outlined.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (adjunto != null) c.primary else c.textTertiary,
                    )
                }
                Text(
                    adjunto?.second ?: "Adjuntar un documento",
                    style = t.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (adjunto != null) c.text else c.textSecondary,
                    maxLines = 1,
                )
                Text(
                    if (adjunto != null) "Toca para verlo aquí" else "PDF o imagen",
                    style = t.micro,
                    color = c.textTertiary,
                )
            }
        }
    }
}
