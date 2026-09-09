package com.vidacotidiana.app.feature.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.Eyebrow
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSegmented
import kotlinx.coroutines.CoroutineScope

/**
 * Compartidos (ADR-025 §3/§4) — las dos direcciones separadas por pestaña,
 * porque confundir recibido con enviado cambia quién tiene que actuar.
 *
 * "Ya hice mi parte" NO toca el recurso: registra que ESA persona cumplió lo
 * suyo. El estado de un recordatorio sigue siendo único y global (DEC-001).
 */
@Composable
fun SharedScreen(
    viewModel: AppViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    val c = VidaTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Me compartieron") }
    val receiving = tab == "Me compartieron"
    val list = if (receiving) state.data.sharedWithMe else state.data.sharedByMe

    VidaScreen(
        title = "Compartidos",
        subtitle = "Lo que tu familia comparte contigo y lo que tú compartes.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) {
            VidaSegmented(listOf("Me compartieron", "Yo compartí"), tab, { tab = it })
        }
        StaggeredAppear(1) {
            Eyebrow("${list.size} ${if (receiving) "recibidos" else "compartidos"}")
        }
        when {
            state.loading && list.isEmpty() -> StaggeredAppear(2) { LoadingRows(2) }
            // ADR-021(k): si la carga falló no se afirma que no hay nada.
            state.error != null && list.isEmpty() -> StaggeredAppear(2) {
                EmptyState(
                    "No pudimos cargar lo compartido",
                    "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to viewModel::refresh,
                )
            }
            // TRES SITUACIONES QUE ANTES COMPARTÍAN UN SOLO TEXTO.
            //
            // Sin familia es FIRST_USE, no vacío: decirle «puedes elegir con
            // quién compartirlo» a quien no tiene con quién es un callejón sin
            // salida disfrazado de instrucción. Aquí sí hay siguiente paso, y
            // es una pantalla que ya existe.
            list.isEmpty() && state.data.familyMembers.isEmpty() -> StaggeredAppear(2) {
                // El título NO puede ser «Todavía no compartes con nadie»:
                // es literalmente la frase con la que Familia abre su propio
                // vacío, y dos módulos distintos diciendo lo mismo se leen como
                // el mismo sitio. Aquí lo que falta no es compartir: es tener
                // con quién.
                EmptyState(
                    title = "Todavía no tienes a nadie en tu familia",
                    body = "Compartir empieza por ahí. Después podrás elegir con quién va cada cosa al crearla o editarla.",
                    action = "Ir a Familia" to { navController.navigate(Routes.FAMILY) },
                )
            }
            // Con familia, las dos pestañas significan cosas distintas y por eso
            // se dicen distinto. Ninguna lleva acción: lo que falta no se hace
            // desde aquí —se elige al crear o editar el recurso— y un botón
            // llevaría a otra pantalla a hacer otra cosa.
            list.isEmpty() -> StaggeredAppear(2) {
                if (receiving) {
                    EmptyState(
                        title = "Nadie te ha compartido nada",
                        body = "Cuando alguien de tu familia comparta algo contigo, aparecerá aquí.",
                    )
                } else {
                    EmptyState(
                        title = "Aún no has compartido nada",
                        body = "Al crear o editar un recurso puedes elegir con quién compartirlo.",
                    )
                }
            }
        }
        if (list.isNotEmpty()) {
            StaggeredAppear(2) {
                // Cuadricula: lo compartido son recursos, y verlos distribuidos
                // deja leer de un vistazo cuantos piden accion.
                ResourceBoard(
                    entries = list.map { share ->
                        ResourceEntry(
                            id = share.id,
                            title = share.label,
                            subtitle = if (receiving) "De ${share.counterpart}" else "Con ${share.counterpart}",
                            highlight = share.dateLabel,
                            typeTag = share.type,
                            tone = if (share.partDone) c.successText else if (share.responsibility) c.warning else null,
                            pill = when {
                                !share.responsibility -> "Solo ver" to PillTone.QUIET
                                share.partDone && receiving -> "Hiciste tu parte" to PillTone.OK
                                share.partDone -> "Ya hizo su parte" to PillTone.OK
                                receiving -> "Te toca" to PillTone.WARN
                                else -> "Pendiente" to PillTone.WARN
                            },
                            // Solo quien recibe marca SU parte: el emisor ve el
                            // estado del otro, no lo cambia.
                            onComplete = if (receiving && share.responsibility && !share.partDone) {
                                { viewModel.togglePartDone(share.id, false) }
                            } else null,
                            completeLabel = "Ya hice mi parte",
                        )
                    },
                    onOpenDetail = {},
                )
            }
        }
        // Nota al pie de la LISTA, no de la pantalla: cuando no hay nada, el
        // estado vacío ya dice esto mismo y verlo dos veces seguidas sobraba.
        if (list.isNotEmpty()) {
            StaggeredAppear(2 + list.size) {
                Text(
                    "Compartir se elige al crear o editar el recurso: con quién, y si se compromete con su parte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }
}
