package com.vidacotidiana.app.feature.sharing

import com.vidacotidiana.app.navigation.go
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.DetailTopBar
import com.vidacotidiana.app.core.ui.components.ToneColors
import com.vidacotidiana.app.core.ui.components.ToneMark
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaHeroArt
import com.vidacotidiana.app.core.ui.components.softCard
import com.vidacotidiana.app.feature.resources.ActionTile
import com.vidacotidiana.app.feature.resources.InfoRow
import com.vidacotidiana.app.feature.resources.MetaItem
import com.vidacotidiana.app.feature.resources.identity
import com.vidacotidiana.app.navigation.Routes

/** El tipo de alta que corresponde a cada tipo compartible (misma lista que ADR-025). */
internal val ShareableKind.resource: CreatableResource
    get() = when (this) {
        ShareableKind.REMINDER -> CreatableResource.TASK
        ShareableKind.MAINTENANCE -> CreatableResource.MAINTENANCE
        ShareableKind.SUBSCRIPTION -> CreatableResource.PAYMENT
        ShareableKind.WARRANTY -> CreatableResource.WARRANTY
        ShareableKind.INVENTORY_ITEM -> CreatableResource.INVENTORY
        ShareableKind.DOCUMENT -> CreatableResource.DOCUMENT
    }

/**
 * LO QUE TE COMPARTIERON, EN SOLO LECTURA (ADR-025).
 *
 * Quien recibe un recurso lo VE, no lo edita: aquí no hay «Editar», ni
 * «Eliminar», ni «Compartir». La única acción propia es «Ya hice mi parte»
 * cuando el dueño le asignó una parte, que registra lo de ESA persona sin
 * tocar el recurso (DEC-001).
 *
 * QUÉ SE ENSEÑA Y POR QUÉ SOLO ESO. La ficha se arma con lo que la propia
 * compartición trae (`GET /shared-resources/received`): nombre, tipo, fecha,
 * quién lo compartió y el estado de tu parte. El backend no deja leer el resto
 * de campos de una tarea, un pago, una garantía, un mantenimiento o un
 * artículo ajenos —sus servicios sólo autorizan al dueño—; la excepción es el
 * documento (`DocumentService.getVisibleOrThrow`), que por eso sí se abre.
 * Enseñar más exigiría un endpoint de lectura para colaboradores: TBD.
 */
@Composable
fun SharedDetailScreen(
    viewModel: AppViewModel,
    navController: NavHostController,
    shareId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val share = state.data.sharedWithMe.firstOrNull { it.id == shareId }
    val scroll = rememberScrollState()
    val fadeDistance = with(LocalDensity.current) { 140.dp.toPx() }

    if (share == null) {
        Column(Modifier.fillMaxSize().background(c.surface)) {
            DetailTopBar(title = null, onBack = { navController.popBackStack() })
            VidaError(
                title = "Ya no tienes acceso",
                body = "Puede que quien lo compartió haya retirado el acceso o lo haya borrado.",
                onRetry = { navController.popBackStack() },
                retryLabel = "Volver",
            )
        }
        return
    }

    val identity = share.kind?.resource?.identity
    val tint = identity?.section?.tint ?: c.primary
    val tone = ToneColors(tint, tint.copy(alpha = 0.14f))
    val label = identity?.label ?: share.type

    Box(Modifier.fillMaxSize().background(c.surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth()) {
                identity?.let { VidaHeroArt(it.motif, tone, Modifier.matchParentSize()) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 60.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ToneMark(identity?.section?.icon ?: Icons.Outlined.Visibility, tone, size = 76.dp, iconSize = 36.dp, circle = true)
                    Text(
                        share.label,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                        color = c.text,
                        modifier = Modifier.fillMaxWidth(0.62f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(label, tone.fg, tone.bg)
                        Chip("Solo lectura", c.textSecondary, c.sunken)
                    }
                    Spacer(Modifier.height(6.dp))
                    MetaItem(Icons.Outlined.Person, "Te lo compartió ${share.counterpart}")
                    share.dateLabel?.let { MetaItem(Icons.Outlined.CalendarToday, it) }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HorizontalDivider(color = c.border.copy(alpha = 0.18f))

                // ── Acciones: solo las que corresponden a quien recibe ───────
                val isDocument = share.kind == ShareableKind.DOCUMENT && share.resourceId != null
                if (isDocument || share.responsibility) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isDocument) {
                            ActionTile(Icons.Outlined.Visibility, "Ver documento", Modifier.weight(1f)) {
                                navController.go(Routes.documentRoute(share.resourceId!!))
                            }
                        }
                        if (share.responsibility) {
                            ActionTile(
                                if (share.partDone) Icons.Outlined.Undo else Icons.Outlined.CheckCircle,
                                if (share.partDone) "Deshacer mi parte" else "Ya hice mi parte",
                                Modifier.weight(1f),
                                tint = if (share.partDone) null else c.success,
                            ) { viewModel.togglePartDone(share.id, share.partDone) }
                        }
                    }
                }

                Text("Información", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.text)
                Column(Modifier.fillMaxWidth().softCard().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    InfoRow("Tipo", label)
                    InfoRow("Compartido por", share.counterpart)
                    share.dateLabel?.let { InfoRow("Fecha", it) }
                    InfoRow(
                        "Tu parte",
                        when {
                            !share.responsibility -> "Solo ver"
                            share.partDone -> "Hecha"
                            else -> "Te toca"
                        },
                        last = true,
                    )
                }

                // Por qué no hay «Editar»: dicho, no escondido.
                Row(
                    Modifier.fillMaxWidth().softCard(c.sunken).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                    Text(
                        "Solo puedes verlo. Solo ${share.counterpart} puede editarlo, borrarlo o compartirlo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Box(Modifier.fillMaxWidth().background(c.surface.copy(alpha = (scroll.value / fadeDistance).coerceIn(0f, 1f)))) {
            DetailTopBar(title = null, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun Chip(text: String, fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
