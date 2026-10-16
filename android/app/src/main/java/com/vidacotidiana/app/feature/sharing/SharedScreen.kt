package com.vidacotidiana.app.feature.sharing

import com.vidacotidiana.app.navigation.go
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import com.vidacotidiana.app.core.ui.components.CategoryTile
import com.vidacotidiana.app.core.ui.components.ListSearchBox
import com.vidacotidiana.app.core.ui.components.ListSectionTitle
import com.vidacotidiana.app.core.ui.components.QuickAccessTile
import com.vidacotidiana.app.core.ui.components.SummaryStatTile
import com.vidacotidiana.app.core.ui.components.ToneColors
import com.vidacotidiana.app.core.ui.components.plural
import com.vidacotidiana.app.feature.resources.identity
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
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.components.EmptyState
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.PillTone
import com.vidacotidiana.app.core.ui.components.ResourceBoard
import com.vidacotidiana.app.core.ui.components.ResourceEntry
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaIconButton
import com.vidacotidiana.app.navigation.Routes
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.VidaSegmented
import com.vidacotidiana.app.core.ui.components.VidaTileRow
import com.vidacotidiana.app.core.ui.components.VidaTileSpec
import kotlinx.coroutines.CoroutineScope
import com.vidacotidiana.app.core.data.ShareableKind

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
    var tab by rememberSaveable { mutableStateOf("Me compartieron") }
    var allKinds by rememberSaveable { mutableStateOf(false) }
    val receiving = tab == "Me compartieron"
    val list = if (receiving) state.data.sharedWithMe else state.data.sharedByMe

    com.vidacotidiana.app.core.ui.components.VidaSectionScreen(
        title = "Compartidos",
        subtitle = "Lo que tu familia comparte contigo y lo que tú compartes.",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
    ) {
        StaggeredAppear(0) {
            VidaSegmented(listOf("Me compartieron", "Yo compartí"), tab, { tab = it })
        }
        // LA RETÍCULA DEL ARTEFACTO, y no el contador que había.
        //
        // «7 recibidos» no dice lo único que importa aquí: de esos siete, ¿en
        // cuántos tengo que hacer algo? Las tres piezas separan justamente eso
        // —me toca, ya está, solo mirar— y por eso la primera pesa más.
        //
        // Cambia con la pestaña porque las dos direcciones no significan lo
        // mismo: recibiendo, «me toca» es trabajo mío; enviando, es trabajo que
        // espero de otra persona.
        //
        // ADR-018: ámbar, nunca rojo. Que me toque algo no es un error.
        when {
            state.loading && list.isEmpty() -> StaggeredAppear(2) { LoadingRows(2) }
            // ADR-021(k): si la carga falló no se afirma que no hay nada.
            state.error != null && list.isEmpty() -> StaggeredAppear(2) {
                EmptyState(
                    "No pudimos cargar lo compartido",
                    "Revisa tu conexión e inténtalo de nuevo.",
                    action = "Reintentar" to viewModel::refresh,
                    section = VidaSections.Compartidos,
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
                    body = "Compartir empieza por ahí. Después podrás elegir con quién va cada cosa desde el propio recurso.",
                    action = "Ir a Familia" to { navController.go(Routes.FAMILY) },
                    section = VidaSections.Compartidos,
                )
            }
            // Con familia, las dos pestañas significan cosas distintas y por eso
            // se dicen distinto. Ninguna lleva acción: lo que falta no se hace
            // desde aquí —se hace desde la tarea, con su botón «Compartir»— y un botón
            // llevaría a otra pantalla a hacer otra cosa.
            list.isEmpty() -> StaggeredAppear(2) {
                if (receiving) {
                    EmptyState(
                        title = "Nadie te ha compartido nada",
                        body = "Cuando alguien de tu familia comparta algo contigo, aparecerá aquí.",
                        section = VidaSections.Compartidos,
                    )
                } else {
                    EmptyState(
                        title = "Aún no has compartido nada",
                        body = "Abre cualquier tarea, pago, documento, garantía, mantenimiento o artículo y pulsa «Compartir».",
                        section = VidaSections.Compartidos,
                    )
                }
            }
        }
        if (list.isNotEmpty()) {
            val dir = if (receiving) ShareDir.RECEIVED else ShareDir.SENT
            fun go(key: String) = navController.go(Routes.sharedCollectionRoute(dir, key))

            // ── Resumen: tres cifras ─────────────────────────────────────────
            StaggeredAppear(1) {
                val people = list.map { it.counterpart }.distinct().size
                val kinds = list.mapNotNull { it.kind }.distinct().size
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryStatTile(Icons.Outlined.Share, list.size.toString(),
                        if (receiving) "recibidos" else "enviados", Modifier.weight(1f))
                    SummaryStatTile(Icons.Outlined.Groups, people.toString(),
                        if (people == 1) "persona" else "personas", Modifier.weight(1f))
                    SummaryStatTile(Icons.Outlined.Category, kinds.toString(),
                        if (kinds == 1) "categoría" else "categorías", Modifier.weight(1f))
                }
            }

            // ── Buscador: lleva a la lista completa, donde se busca ─────────
            StaggeredAppear(1) {
                ListSearchBox(
                    value = "",
                    placeholder = if (receiving) "Buscar en lo que me compartieron…" else "Buscar en lo que compartí…",
                    onClick = { go(ShareCollections.ALL) },
                )
            }

            // ── Accesos rápidos ──────────────────────────────────────────────
            // ADR-018: ámbar, nunca rojo. Que me toque algo no es un error.
            StaggeredAppear(2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ListSectionTitle("Accesos rápidos")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessTile(
                            Icons.Outlined.PendingActions,
                            ShareCollections.title(ShareCollections.PENDING, receiving),
                            list.count { ShareCollections.matches(ShareCollections.PENDING, it) },
                            ToneColors(c.warning, c.warningContainer), Modifier.weight(1f),
                        ) { go(ShareCollections.PENDING) }
                        QuickAccessTile(
                            Icons.Outlined.TaskAlt, "Hecho",
                            list.count { ShareCollections.matches(ShareCollections.DONE, it) },
                            ToneColors(c.success, c.successContainer), Modifier.weight(1f),
                        ) { go(ShareCollections.DONE) }
                        QuickAccessTile(
                            Icons.Outlined.Visibility, "Solo ver",
                            list.count { ShareCollections.matches(ShareCollections.VIEW_ONLY, it) },
                            ToneColors(c.textSecondary, c.sunken), Modifier.weight(1f),
                        ) { go(ShareCollections.VIEW_ONLY) }
                    }
                }
            }

            // ── Mis categorías: una por tipo que se puede compartir ─────────
            StaggeredAppear(3) {
                val counts = list.groupingBy { it.kind }.eachCount()
                val ordered = ShareableKind.entries.sortedByDescending { counts[it] ?: 0 }
                val visible = if (allKinds) ordered else ordered.take(4)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListSectionTitle(
                        "Por categoría",
                        action = if (allKinds) "Ver menos" else "Ver todas",
                        onAction = { allKinds = !allKinds },
                    )
                    visible.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { kind ->
                                val identity = kind.resource.identity
                                CategoryTile(
                                    identity.section.icon,
                                    kindPlural(kind),
                                    plural(counts[kind] ?: 0, "compartido", "compartidos"),
                                    kindTone(kind),
                                    Modifier.weight(1f),
                                ) { go(kind.wire) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Recientes ────────────────────────────────────────────────────
            StaggeredAppear(4) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ListSectionTitle("Recientes", action = "Ver todos") { go(ShareCollections.ALL) }
                    ResourceBoard(
                        entries = list.take(3).map { shareEntry(it, receiving, viewModel, navController) },
                        onOpenDetail = { entry ->
                            list.firstOrNull { it.id == entry.id }?.let { openShare(it, receiving, navController) }
                        },
                        pageSize = null,
                    )
                }
            }
        }
        // Nota al pie de la LISTA, no de la pantalla: cuando no hay nada, el
        // estado vacío ya dice esto mismo y verlo dos veces seguidas sobraba.
        if (list.isNotEmpty()) {
            StaggeredAppear(2 + list.size) {
                Text(
                    "Se comparte desde el propio recurso: ábrelo y pulsa «Compartir» para elegir con quién.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }
}
