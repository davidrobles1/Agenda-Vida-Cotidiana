package com.vidacotidiana.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vidacotidiana.app.core.app.AppViewModel
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.VidaCotidianaTheme
import com.vidacotidiana.app.core.ui.components.CreateResourceSheet
import com.vidacotidiana.app.core.ui.components.CreateSheet
import com.vidacotidiana.app.core.ui.components.VidaBottomNav
import com.vidacotidiana.app.core.ui.components.VidaDrawerContent
import com.vidacotidiana.app.feature.auth.AuthManager
import com.vidacotidiana.app.feature.auth.IntroScreen
import com.vidacotidiana.app.feature.board.VisionBoardScreen
import com.vidacotidiana.app.feature.calendar.CalendarScreen
import com.vidacotidiana.app.feature.documents.DocumentsScreen
import com.vidacotidiana.app.feature.family.FamilyScreen
import com.vidacotidiana.app.feature.home.HomeScreen
import com.vidacotidiana.app.feature.inventory.InventoryScreen
import com.vidacotidiana.app.feature.laboral.AgendaScreen
import com.vidacotidiana.app.feature.laboral.HoyScreen
import com.vidacotidiana.app.feature.laboral.InboxScreen
import com.vidacotidiana.app.feature.laboral.LaboralTasksScreen
import com.vidacotidiana.app.feature.laboral.PeopleScreen
import com.vidacotidiana.app.feature.laboral.ProjectsScreen
import com.vidacotidiana.app.feature.laboral.CommitmentsScreen
import com.vidacotidiana.app.feature.objectives.ObjectivesScreen
import com.vidacotidiana.app.feature.places.PlacesScreen
import com.vidacotidiana.app.feature.resources.WorkResourcesScreen
import com.vidacotidiana.app.feature.routines.RoutinesScreen
import com.vidacotidiana.app.feature.maintenance.MaintenanceScreen
import com.vidacotidiana.app.feature.notifications.NotificationsScreen
import com.vidacotidiana.app.feature.settings.AppearanceScreen
import com.vidacotidiana.app.feature.settings.SettingsScreen
import com.vidacotidiana.app.feature.sharing.SharedScreen
import com.vidacotidiana.app.feature.subscriptions.PaymentsScreen
import com.vidacotidiana.app.feature.tasks.TasksScreen
import com.vidacotidiana.app.feature.warranties.WarrantiesScreen
import kotlinx.coroutines.launch

/**
 * El armazón de la aplicación: tema, cajón, barra inferior por contexto,
 * hoja de creación y grafo de navegación.
 *
 * ARQUITECTURA DE ACCESO (aprobada en el artefacto):
 *   · barra inferior = destinos prioritarios del contexto activo;
 *   · menú lateral   = el resto de las secciones + cuenta.
 *
 * Las pestañas REINICIAN la pila; el menú APILA sobre el destino actual, de
 * modo que "atrás" desde una sección secundaria devuelve al sitio del que se
 * salió en vez de dejar la pila vacía.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    authManager: AuthManager,
    /** Destino pedido por un intent entrante: notificación o acceso directo. */
    pendingRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
) {
    val appViewModel: AppViewModel = hiltViewModel()
    val state by appViewModel.state.collectAsStateWithLifecycle()

    VidaCotidianaTheme(theme = state.theme) {
        val navController: NavHostController = rememberNavController()
        // UX-006, error real encontrado en dispositivo: esto debe calcularse
        // UNA sola vez. Un `val` corriente se reevalúa en cada recomposición
        // —incluida la que dispara el propio `navigate` tras el login— y el
        // `NavHost` reiniciaría la pila pisando la navegación recién pedida.
        // Con sesión guardada no hay presentación que hacer: se entra. La
        // introducción firma la puerta, y a quien ya tiene llave no se le
        // enseña la puerta.
        val startDestination = remember { if (authManager.isLoggedIn()) Routes.HOME else Routes.INTRO }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var createSheetOpen by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val bottom = bottomDestinations(state.context, state.profile)
        val showChrome = currentRoute != null && currentRoute != Routes.INTRO

        /** La raíz del módulo activo: a donde significa «volver al principio». */
        val moduleRoot = rootRouteFor(state.context)

        // Portal no tiene barra inferior: su navegación es una sola sección y
        // una barra de un solo destino no es navegación, es un adorno.
        val showBottomNav = showChrome && state.context != AppContext.PORTAL

        /**
         * Saltar a un destino principal del módulo.
         *
         * LA RAÍZ NUNCA SE SACA DE LA PILA. Es el cambio que arregla el
         * regreso a Inicio y a Hoy: antes, al tocar la propia raíz se hacía
         * `popUpTo(root) { inclusive = true }`, es decir se eliminaba la raíz
         * para volver a insertarla, y encima con `restoreState` intentando
         * reponer un estado que la navegación anterior había guardado. Ese par
         * dejaba la pila en un estado que ya no correspondía a ninguna
         * navegación real.
         *
         * `saveState`/`restoreState` tampoco vuelven: son la herramienta de los
         * grafos ANIDADOS con una pila por pestaña. Este grafo es plano y de
         * una sola pila, así que ahí no guardaban estado de nadie — solo
         * introducían entradas fantasma.
         *
         * Con `inclusive = false` la operación es siempre la misma y siempre
         * predecible: vaciar por encima de la raíz y dejar arriba el destino
         * pedido. Si el pedido ES la raíz, no queda nada por encima y
         * `launchSingleTop` reutiliza la instancia que ya estaba. Sin
         * duplicados, sin pilas inconsistentes y sin tocar el «atrás» del
         * sistema, que sigue recorriendo lo que quede.
         */
        fun openTab(route: String) {
            navController.navigate(route) {
                popUpTo(moduleRoot) { inclusive = false }
                launchSingleTop = true
            }
        }

        fun pushSection(route: String) {
            navController.navigate(route) { launchSingleTop = true }
        }

        /**
         * Cambiar de módulo REINICIA la navegación en su raíz.
         *
         * Sin esto, pasar de Personal a Laboral dejaba al usuario en una
         * pantalla de Personal con la barra de Laboral: el contexto es estado
         * de la aplicación y la pila no se enteraba. Se hace aquí, en un solo
         * sitio, y no en cada pantalla que ofrece el selector.
         */
        /**
         * Atender el destino que llega por notificación o acceso directo.
         *
         * SOLO CON SESIÓN ABIERTA, y esto es deliberado: un enlace no puede
         * saltarse el login. Si todavía no la hay, el destino NO se consume —
         * queda pendiente y se aplica en cuanto el usuario entra, en vez de
         * perderse por haber tocado la notificación con la sesión caducada.
         */
        val loggedIn by authManager.isLoggedInFlow.collectAsStateWithLifecycle()
        LaunchedEffect(pendingRoute, loggedIn) {
            val target = pendingRoute ?: return@LaunchedEffect
            if (!loggedIn) return@LaunchedEffect

            if (target == DeepLinks.ACTION_CREATE_TASK) {
                // El acceso directo de crear no es un destino: lleva a la raíz
                // del módulo y abre el MISMO formulario de alta que el «+».
                navController.navigate(rootRouteFor(state.context)) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
                appViewModel.requestCreate(CreatableResource.TASK)
            } else {
                navController.navigate(target) {
                    // Se apila sobre la raíz del módulo, no sobre lo que
                    // hubiera: «atrás» desde una notificación debe dejar al
                    // usuario en un sitio con sentido, no en su navegación previa.
                    popUpTo(rootRouteFor(state.context)) { inclusive = false }
                    launchSingleTop = true
                }
            }
            onRouteConsumed()
        }

        var lastContext by remember { mutableStateOf(state.context) }
        LaunchedEffect(state.context) {
            if (state.context != lastContext) {
                lastContext = state.context
                if (showChrome) {
                    navController.navigate(rootRouteFor(state.context)) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showChrome,
            drawerContent = {
                // #6: el menu se ceñia a la pantalla entera porque su contenido
                // pedia `fillMaxSize`. Ahora tiene un ancho propio —el estandar
                // de Material para un cajon— acotado ademas al 86 % de pantalla,
                // para que en un telefono estrecho siga dejando ver la aplicacion
                // detras y se entienda como menu y no como otra pantalla.
                BoxWithConstraints {
                    val drawerWidth = minOf(320.dp, this@BoxWithConstraints.maxWidth * 0.86f)
                    ModalDrawerSheet(
                        modifier = Modifier.width(drawerWidth),
                        drawerContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    ) {
                    VidaDrawerContent(
                        context = state.context,
                        sections = drawerDestinations(state.context, state.profile),
                        account = accountDestinations,
                        currentRoute = currentRoute,
                        themeLabel = state.theme.label,
                        onDestination = { d ->
                            scope.launch { drawerState.close() }
                            pushSection(d.route)
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            authManager.logout()
                            // Cerrar sesión devuelve al PRINCIPIO del flujo, no
                            // al formulario. Saltar a `LOGIN` era lo que hacía
                            // reaparecer el comportamiento anterior: quien cierra
                            // sesión vuelve a ser alguien sin llave, y a esa
                            // persona la aplicación se le presenta.
                            navController.navigate(Routes.INTRO) { popUpTo(0) { inclusive = true } }
                        },
                    )
                    }
                }
            },
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        // Transiciones entre pantallas: el artefacto no corta,
                        // desliza. Navigation 2.8 lo permite por destino.
                        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                        exitTransition = { fadeOut(tween(160)) },
                        popEnterTransition = { fadeIn(tween(220)) },
                        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 12 } },
                    ) {
                        // Único destino antes de la sesión. El formulario real
                        // lo abre él mismo en el Custom Tab de Keycloak; ya no
                        // hay pantalla de login propia por medio.
                        composable(Routes.INTRO) {
                            IntroScreen(
                                authManager = authManager,
                                onLoggedIn = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.INTRO) { inclusive = true }
                                    }
                                },
                            )
                        }

                        // ---- Personal ----
                        composable(Routes.HOME) { HomeScreen(appViewModel, navController::navigateSafely, drawerState, scope) }
                        composable(Routes.CALENDAR) { CalendarScreen(appViewModel, navController::navigateSafely, drawerState, scope) }
                        composable(Routes.BOARD) { VisionBoardScreen(appViewModel, drawerState, scope) }
                        composable(Routes.PAYMENTS) { PaymentsScreen(appViewModel, drawerState, scope) }
                        composable(Routes.SHARED) { SharedScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.DOCUMENTS) { DocumentsScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.INVENTORY) { InventoryScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.WARRANTIES) { WarrantiesScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.MAINTENANCE) { MaintenanceScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.FAMILY) { FamilyScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.TASKS) { TasksScreen(appViewModel, drawerState, scope, navController) }

                        // ---- Laboral ----
                        composable(Routes.HOY) { HoyScreen(appViewModel, drawerState, scope) }
                        composable(Routes.AGENDA) { AgendaScreen(appViewModel, drawerState, scope) }
                        composable(Routes.TASKS_LAB) { LaboralTasksScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.PEOPLE) { PeopleScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.PROJECTS) { ProjectsScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.COMMITMENTS) { CommitmentsScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.OBJECTIVES) { ObjectivesScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.ROUTINES) { RoutinesScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.WORK_RESOURCES) { WorkResourcesScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.PLACES) { PlacesScreen(appViewModel, drawerState, scope, navController) }
                        composable(Routes.INBOX) { InboxScreen(appViewModel, drawerState, scope) }

                        // ---- Cuenta ----
                        composable(Routes.NOTIFICATIONS) { NotificationsScreen(navController) }
                        composable(Routes.SETTINGS) { SettingsScreen(appViewModel, navController) }
                        composable(Routes.APPEARANCE) { AppearanceScreen(appViewModel, navController) }
                    }
                }

                if (showBottomNav) {
                    VidaBottomNav(
                        destinations = bottom,
                        currentRoute = currentRoute,
                        onSelect = { openTab(it.route) },
                        // #8: dentro de una seccion el «+» crea SU recurso; en
                        // las raices del modulo conserva el selector de siempre.
                        onCreate = {
                            val direct = createResourceForRoute(currentRoute)
                            if (direct != null) appViewModel.requestCreate(direct) else createSheetOpen = true
                        },
                    )
                }
            }
        }

        // El formulario de alta vive AQUI, uno solo. Las dos entradas —el «+»
        // de la barra y el «+» de cada seccion— piden lo mismo al ViewModel y
        // acaban en esta hoja, asi que no pueden divergir.
        state.pendingCreate?.let { resource ->
            ModalBottomSheet(
                onDismissRequest = appViewModel::cancelCreate,
                sheetState = sheetState,
                containerColor = com.vidacotidiana.app.core.ui.VidaTheme.colors.surfaceVariant,
            ) {
                CreateResourceSheet(
                    resource = resource,
                    initialValues = state.formInitialValues,
                    editing = state.editing != null,
                    people = state.data.people.map { it.id to it.name },
                    projects = state.data.projects.map { it.id to it.name },
                    // El nombre solo no basta para reconocer un articulo: dos
                    // pueden llamarse igual en dos sitios distintos.
                    inventory = state.data.inventory.map {
                        it.id to (if (it.location.isNullOrBlank()) it.name else "${it.name} · ${it.location}")
                    },
                    saving = state.saving,
                    error = state.error,
                    onSubmit = { values, file -> appViewModel.create(resource, values, file) },
                    onPickFile = appViewModel::readFile,
                    onQuickCreate = appViewModel::quickCreate,
                    onCancel = appViewModel::cancelCreate,
                )
            }
        }

        if (createSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { createSheetOpen = false },
                sheetState = sheetState,
                containerColor = com.vidacotidiana.app.core.ui.VidaTheme.colors.surfaceVariant,
            ) {
                CreateSheet(
                    context = state.context,
                    actions = createActions(state.context, state.profile),
                    onPick = { resource ->
                        createSheetOpen = false
                        appViewModel.requestCreate(resource)
                    },
                )
            }
        }
    }
}

/** Navegación desde una pantalla: apila, para que "atrás" devuelva. */
private fun NavHostController.navigateSafely(route: String) {
    navigate(route) { launchSingleTop = true }
}
