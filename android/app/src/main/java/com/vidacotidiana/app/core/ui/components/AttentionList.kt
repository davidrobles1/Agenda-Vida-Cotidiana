package com.vidacotidiana.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.vidacotidiana.app.core.attention.AttentionItem
import com.vidacotidiana.app.core.attention.AttentionSource
import com.vidacotidiana.app.core.attention.AttentionUrgency
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * LO QUE RECLAMA, VENGA DEL MÓDULO QUE VENGA.
 *
 * La fila lleva el MOTIVO, no el módulo: «Se pagaba hace 3 días» dice por qué
 * está en pantalla, que es lo que convierte un dato en algo accionable. El
 * importe de un pago va dentro del motivo («Se paga hoy · $349 MXN») y no como
 * dato héroe, porque aquí lo que importa no es cuánto es sino que te toca.
 * Pasarlo como `amount` convertiría la lista en una retícula de cuadrados y
 * mezclaría siete módulos en piezas que compiten entre sí.
 *
 * La píldora dice de qué módulo viene cada cosa. Sin ella, siete fuentes juntas
 * se leerían como una lista plana y se perdería justo lo que hace valiosa a la
 * sección: que la aplicación miró en todas partes.
 *
 * VIVE AQUÍ Y NO EN INICIO porque ya no la pinta sólo Inicio: el mosaico de lo
 * atrasado necesita una pantalla que enseñe EXACTAMENTE el conjunto que ese
 * mosaico cuenta, y ese conjunto cruza módulos. Duplicar la lista habría
 * garantizado dos lecturas distintas del mismo dato.
 */
@Composable
fun AttentionList(items: List<AttentionItem>, onNavigate: (String) -> Unit) {
    val c = VidaTheme.colors
    ResourceBoard(
        entries = items.map { item ->
            ResourceEntry(
                id = item.id,
                title = item.title,
                subtitle = listOfNotNull(item.reason, item.amount).joinToString(" · "),
                icon = item.source.icon(),
                // El tono codifica cuánto aprieta, no de qué módulo viene: lo
                // atrasado es lo único que está mal de verdad.
                tone = when (item.urgency) {
                    AttentionUrgency.OVERDUE -> c.error
                    AttentionUrgency.TODAY -> c.warning
                    AttentionUrgency.SOON -> null
                },
                pill = item.source.label to PillTone.NEUTRAL,
            )
        },
        onOpenDetail = { entry ->
            items.firstOrNull { it.id == entry.id }?.let { onNavigate(it.source.route) }
        },
    )
}

/** El icono de cada módulo, el mismo que ese módulo usa en su propia pantalla. */
fun AttentionSource.icon(): ImageVector = when (this) {
    AttentionSource.TASK -> Icons.AutoMirrored.Outlined.Assignment
    AttentionSource.PAYMENT -> Icons.Outlined.Autorenew
    AttentionSource.WARRANTY -> Icons.Outlined.VerifiedUser
    AttentionSource.MAINTENANCE -> Icons.Outlined.Build
    AttentionSource.COMMITMENT -> Icons.Outlined.Groups
    AttentionSource.ROUTINE -> Icons.Outlined.Repeat
    AttentionSource.SHARED -> Icons.Outlined.Share
}
