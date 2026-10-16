package com.vidacotidiana.app.core.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.components.ResourceAccessDialog

/**
 * EL PUENTE ENTRE EL DIÁLOGO DE ACCESO Y EL VIEWMODEL.
 *
 * Vive aquí y no junto al diálogo a propósito: ninguna pieza de
 * `core/ui/components` conoce el `AppViewModel`, y esa separación es la que
 * permite que esos componentes se prueben y se reutilicen sin arrastrar medio
 * modelo de la aplicación. El diálogo sigue siendo puro —recibe listas y
 * devuelve eventos— y lo que sabe de dónde salen esas listas es este archivo.
 *
 * Trece pantallas de recursos tienen que ofrecer lo mismo. Si cada una montara
 * su estado, su carga y su diálogo, acabarían existiendo trece versiones que
 * se desincronizan a la primera corrección. La sección solo dice CUÁNDO abrir
 * y SOBRE QUÉ:
 *
 * ```
 * val acceso = rememberResourceAccess(viewModel)
 * …
 * onAccess = { acceso.abrir(ShareableKind.DOCUMENT, doc.id, doc.name) }
 * …
 * acceso.Dialogo()
 * ```
 */
class ResourceAccessController internal constructor(
    private val viewModel: AppViewModel,
) {
    internal var kind by mutableStateOf<ShareableKind?>(null)
    internal var resourceId by mutableStateOf<String?>(null)
    internal var title by mutableStateOf("")

    fun abrir(kind: ShareableKind, resourceId: String, title: String) {
        // ADR-019 + ADR-025: compartir con Familia es de PERSONAL. Un registro
        // de Laboral no abre este diálogo, venga de donde venga la llamada: en
        // Laboral se vincula a Personas, no a la familia.
        val resource = when (kind) {
            ShareableKind.REMINDER -> CreatableResource.TASK
            ShareableKind.SUBSCRIPTION -> CreatableResource.PAYMENT
            ShareableKind.MAINTENANCE -> CreatableResource.MAINTENANCE
            ShareableKind.WARRANTY -> CreatableResource.WARRANTY
            ShareableKind.INVENTORY_ITEM -> CreatableResource.INVENTORY
            ShareableKind.DOCUMENT -> CreatableResource.DOCUMENT
        }
        if (viewModel.moduleOf(resource, resourceId) == com.vidacotidiana.app.navigation.AppContext.LABORAL) return
        this.kind = kind
        this.resourceId = resourceId
        this.title = title
        viewModel.loadResourceShares(kind, resourceId)
    }

    fun cerrar() {
        kind = null
        resourceId = null
        // La lista no debe sobrevivir al diálogo: es de UN recurso, y dejarla
        // haría que el siguiente que se abra enseñe durante un instante los
        // permisos del anterior.
        viewModel.clearResourceShares()
    }
}

@Composable
fun rememberResourceAccess(viewModel: AppViewModel): ResourceAccessController =
    remember(viewModel) { ResourceAccessController(viewModel) }

/**
 * Se pinta UNA VEZ por pantalla, al final. Mientras no haya recurso abierto no
 * compone nada.
 */
@Composable
fun ResourceAccessController.Dialogo(viewModel: AppViewModel) {
    val abierto = kind
    val id = resourceId
    if (abierto == null || id == null) return
    val state by viewModel.state.collectAsStateWithLifecycle()
    ResourceAccessDialog(
        kind = abierto,
        resourceTitle = title,
        shares = state.resourceShares,
        loading = state.resourceSharesLoading,
        family = state.data.familyMembers,
        searchResults = state.userSearch,
        searching = state.searching,
        onSearch = viewModel::searchUsers,
        onGrant = { userId, responsibility ->
            viewModel.shareResourceWith(abierto, id, userId, responsibility)
        },
        onRevoke = { shareId -> viewModel.revokeResourceShare(abierto, id, shareId) },
        onSetResponsibility = { shareId, responsibility ->
            viewModel.setShareResponsibility(abierto, id, shareId, responsibility)
        },
        onDismiss = { cerrar() },
    )
}
