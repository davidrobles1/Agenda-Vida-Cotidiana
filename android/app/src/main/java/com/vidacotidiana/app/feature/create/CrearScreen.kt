package com.vidacotidiana.app.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaMark
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.vidaClickable
import com.vidacotidiana.app.navigation.AppContext
import com.vidacotidiana.app.navigation.createActions

/**
 * CREAR — la pantalla del artefacto, con la composición de retícula.
 *
 * POR QUÉ ESTA PANTALLA NO ES OTRO FORMULARIO.
 *
 * El formulario real ya existe y es `CreateResourceSheet`: se construye desde
 * `CreatableResource.fields`, que declara los campos que cada endpoint exige de
 * verdad, y habilita «Guardar» solo cuando lo obligatorio está completo.
 * Escribir aquí un segundo formulario daría dos interpretaciones de qué campos
 * son obligatorios, y la segunda mandaría peticiones que el backend rechaza.
 *
 * Así que esta ruta aporta lo que le faltaba al flujo —la ELECCIÓN presentada
 * con la retícula del artefacto, a pantalla completa en vez de en una hoja
 * apretada— y entrega al formulario que ya funciona. Una sola mecánica de alta.
 *
 * LO QUE SE PUEDE CREAR DEPENDE DEL CONTEXTO: `createActions` devuelve lo que
 * de verdad existe en Personal o en Laboral. No se ofrece crear algo que esa
 * mitad del producto no tiene.
 */
@Composable
fun CrearScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type

    val actions = createActions(state.context, state.profile)

    // Los tonos rotan por posición para que la retícula no sea un bloque de un
    // solo color. Salen de la paleta semántica, no de colores nuevos.
    val tones = listOf(
        c.primaryContainer to c.primaryDeep,
        c.warningContainer to c.warningText,
        c.secondContainer to c.second,
        c.successContainer to c.successText,
        c.errorContainer to c.error,
    )

    VidaScreen(
        title = "Algo nuevo",
        subtitle = "En ${if (state.context == AppContext.LABORAL) "Laboral" else "Personal"} · " +
            "solo lo que ya existe aquí",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        /* ── LA RETÍCULA DE TRES COLUMNAS ────────────────────────────────── */
        StaggeredAppear(0) {
            Column(verticalArrangement = Arrangement.spacedBy(VidaLayout.itemGap)) {
                actions.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VidaLayout.itemGap),
                    ) {
                        row.forEachIndexed { i, action ->
                            val index = actions.indexOf(action)
                            val (bg, fg) = tones[index % tones.size]
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(104.dp)
                                    .background(c.surfaceElevated, RoundedCornerShape(20.dp))
                                    .vidaClickable(
                                        onClick = {
                                            // Se vuelve ANTES de pedir el alta:
                                            // el formulario es una hoja sobre la
                                            // pantalla anterior, y dejar esta
                                            // debajo la enterraría en la pila.
                                            navController.popBackStack()
                                            viewModel.requestCreate(action.resource)
                                        },
                                    )
                                    .padding(vertical = 16.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                VidaMark(background = bg, contentColor = fg, boxSize = 38.dp) {
                                    Icon(
                                        action.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    action.label,
                                    style = t.caption.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                                    color = c.text,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        // Rellena la última fila incompleta para que las piezas
                        // conserven su anchura en vez de estirarse.
                        repeat(3 - row.size) {
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        StaggeredAppear(1) {
            Text(
                "Cada alta pide exactamente los campos que su sección necesita. " +
                    "Guardar se habilita cuando lo obligatorio está completo, no después de un error.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }
    }
}
