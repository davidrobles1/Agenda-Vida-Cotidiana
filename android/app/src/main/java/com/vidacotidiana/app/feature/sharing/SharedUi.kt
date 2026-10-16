package com.vidacotidiana.app.feature.sharing

import com.vidacotidiana.app.navigation.go
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.data.SharedResource
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.ToneColors
import com.vidacotidiana.app.feature.resources.identity
import com.vidacotidiana.app.navigation.Routes

/*
 * PIEZAS COMUNES DE COMPARTIDOS — el resumen y las colecciones leen lo mismo
 * y de la misma forma (ADR-028, extensión a Compartidos).
 */

/** Las dos direcciones, tal cual van en la ruta. */
internal object ShareDir {
    const val RECEIVED = "recibidos"
    const val SENT = "enviados"
}

/** Accesos rápidos: se navegan como una categoría más, con clave propia. */
internal object ShareCollections {
    const val ALL = "_TODOS"
    /** Recibiendo: me toca a mí. Enviando: espero la parte de otra persona. */
    const val PENDING = "_PENDIENTE"
    const val DONE = "_HECHO"
    const val VIEW_ONLY = "_SOLO_VER"

    fun title(key: String, receiving: Boolean): String = when (key) {
        ALL -> if (receiving) "Me compartieron" else "Yo compartí"
        PENDING -> if (receiving) "Te toca" else "Esperando"
        DONE -> "Hecho"
        VIEW_ONLY -> "Solo ver"
        else -> kindPlural(ShareableKind.entries.firstOrNull { it.wire == key })
    }

    fun matches(key: String, share: SharedResource): Boolean = when (key) {
        ALL -> true
        PENDING -> share.responsibility && !share.partDone
        DONE -> share.partDone
        VIEW_ONLY -> !share.responsibility
        else -> share.kind?.wire == key
    }
}

/** El nombre de la categoría en plural, como en «Mis categorías». */
internal fun kindPlural(kind: ShareableKind?): String = when (kind) {
    ShareableKind.REMINDER -> "Tareas"
    ShareableKind.SUBSCRIPTION -> "Pagos"
    ShareableKind.MAINTENANCE -> "Mantenimientos"
    ShareableKind.WARRANTY -> "Garantías"
    ShareableKind.INVENTORY_ITEM -> "Artículos"
    ShareableKind.DOCUMENT -> "Documentos"
    null -> "Otros"
}

/** El tono de una categoría: el de la sección de la que viene el recurso. */
@Composable
internal fun kindTone(kind: ShareableKind?): ToneColors {
    val tint = kind?.resource?.identity?.section?.tint ?: VidaTheme.colors.primary
    return ToneColors(tint, tint.copy(alpha = 0.14f))
}

/**
 * Un compartido como fila. Las acciones son las que ya existían: marcar tu
 * parte (quien recibe), ver el documento, y retirar el acceso (quien envía).
 * Quien recibe NUNCA recibe editar ni borrar (ADR-025).
 */
@Composable
internal fun shareEntry(
    share: SharedResource,
    receiving: Boolean,
    viewModel: AppViewModel,
    navController: NavHostController,
): ResourceEntry {
    val c = VidaTheme.colors
    val identity = share.kind?.resource?.identity
    return ResourceEntry(
        id = share.id,
        title = share.label,
        subtitle = listOfNotNull(
            if (receiving) "De ${share.counterpart}" else "Con ${share.counterpart}",
            share.dateLabel,
        ).joinToString(" · "),
        icon = identity?.section?.icon,
        tone = identity?.section?.tint,
        pill = when {
            !share.responsibility -> "Solo ver" to PillTone.QUIET
            share.partDone && receiving -> "Hiciste tu parte" to PillTone.OK
            share.partDone -> "Ya hizo su parte" to PillTone.OK
            receiving -> "Te toca" to PillTone.WARN
            else -> "Pendiente" to PillTone.WARN
        },
        onComplete = if (receiving && share.responsibility && !share.partDone) {
            { viewModel.togglePartDone(share.id, false) }
        } else null,
        completeLabel = "Ya hice mi parte",
        onRevert = if (receiving && share.responsibility && share.partDone) {
            { viewModel.togglePartDone(share.id, true) }
        } else null,
        revertLabel = "Deshacer mi parte",
        onOpen = if (share.kind == ShareableKind.DOCUMENT && share.resourceId != null) {
            { navController.go(Routes.documentRoute(share.resourceId)) }
        } else null,
        openLabel = "Ver documento",
        onDelete = if (!receiving && share.kind != null && share.resourceId != null) {
            { viewModel.revokeResourceShare(share.kind, share.resourceId, share.id) }
        } else null,
    ).let { if (share.partDone && it.tone == null) it.copy(tone = c.successText) else it }
}

/**
 * VER EL DETALLE, en las dos direcciones:
 *  · recibido → ficha de SOLO LECTURA (`SharedDetailScreen`);
 *  · enviado  → la ficha de siempre del recurso, porque el usuario es el dueño.
 */
internal fun openShare(share: SharedResource, receiving: Boolean, navController: NavHostController) {
    if (receiving) {
        navController.go(Routes.sharedDetailRoute(share.id))
        return
    }
    val id = share.resourceId ?: return
    when (share.kind) {
        ShareableKind.REMINDER -> navController.go(Routes.taskRoute(id))
        ShareableKind.SUBSCRIPTION -> navController.go(Routes.paymentRoute(id))
        ShareableKind.MAINTENANCE -> navController.go(Routes.maintenanceRoute(id))
        ShareableKind.WARRANTY -> navController.go(Routes.resourceRoute("WARRANTY", id))
        ShareableKind.INVENTORY_ITEM -> navController.go(Routes.resourceRoute("INVENTORY", id))
        ShareableKind.DOCUMENT -> navController.go(Routes.documentDetailRoute(id))
        null -> Unit
    }
}
