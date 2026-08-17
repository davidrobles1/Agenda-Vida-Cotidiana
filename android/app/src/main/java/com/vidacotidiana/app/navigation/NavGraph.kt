package com.vidacotidiana.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vidacotidiana.app.core.ui.components.BottomNavDestination
import com.vidacotidiana.app.core.ui.components.VidaBottomNav
import com.vidacotidiana.app.feature.auth.AuthManager
import com.vidacotidiana.app.feature.auth.LoginScreen
import com.vidacotidiana.app.feature.documents.DocumentsScreen
import com.vidacotidiana.app.feature.family.FamilyScreen
import com.vidacotidiana.app.feature.home.HomeScreen
import com.vidacotidiana.app.feature.inventory.InventoryScreen
import com.vidacotidiana.app.feature.maintenance.MaintenanceScreen
import com.vidacotidiana.app.feature.more.MoreScreen
import com.vidacotidiana.app.feature.notifications.NotificationsScreen
import com.vidacotidiana.app.feature.reminders.RemindersScreen
import com.vidacotidiana.app.feature.sharing.InvitationsScreen
import com.vidacotidiana.app.feature.subscriptions.SubscriptionsScreen
import com.vidacotidiana.app.feature.warranties.WarrantiesScreen

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val REMINDERS = "reminders"
    const val INVITATIONS = "invitations"
    const val MORE = "more"
    const val NOTIFICATIONS = "notifications"
    const val DOCUMENTS = "documents"
    const val WARRANTIES = "warranties"
    const val INVENTORY = "inventory"
    const val MAINTENANCE = "maintenance"
    const val SUBSCRIPTIONS = "subscriptions"
    const val FAMILY = "family"
}

// UX-006: the 4 real bottom-nav destinations — everything else (Notifications,
// the 6 mock modules) is reached through "Más" and shown without the bottom
// bar, same as any pushed detail screen.
private val bottomNavRoutes = setOf(Routes.HOME, Routes.REMINDERS, Routes.INVITATIONS, Routes.MORE)

@Composable
fun AppNavGraph(authManager: AuthManager) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (authManager.isLoggedIn()) Routes.HOME else Routes.LOGIN

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull { it.route in bottomNavRoutes }?.route
    val showBottomNav = currentRoute != null

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                val current = when (currentRoute) {
                    Routes.HOME -> BottomNavDestination.Home
                    Routes.REMINDERS -> BottomNavDestination.Tasks
                    Routes.INVITATIONS -> BottomNavDestination.Shared
                    else -> BottomNavDestination.More
                }
                VidaBottomNav(
                    current = current,
                    onSelect = { destination ->
                        val route = when (destination) {
                            BottomNavDestination.Home -> Routes.HOME
                            BottomNavDestination.Tasks -> Routes.REMINDERS
                            BottomNavDestination.Shared -> Routes.INVITATIONS
                            BottomNavDestination.More -> Routes.MORE
                        }
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateClick = {
                        navController.navigate(Routes.REMINDERS) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authManager = authManager,
                    onLoggedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToTasks = { navController.navigate(Routes.REMINDERS) },
                    onNavigateToShared = { navController.navigate(Routes.INVITATIONS) },
                )
            }
            composable(Routes.REMINDERS) {
                RemindersScreen(
                    onNavigateToInvitations = { navController.navigate(Routes.INVITATIONS) },
                    onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                )
            }
            composable(Routes.INVITATIONS) {
                InvitationsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onNavigateToDocuments = { navController.navigate(Routes.DOCUMENTS) },
                    onNavigateToWarranties = { navController.navigate(Routes.WARRANTIES) },
                    onNavigateToInventory = { navController.navigate(Routes.INVENTORY) },
                    onNavigateToMaintenance = { navController.navigate(Routes.MAINTENANCE) },
                    onNavigateToSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                    onNavigateToFamily = { navController.navigate(Routes.FAMILY) },
                    onLogout = {
                        authManager.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DOCUMENTS) { DocumentsScreen() }
            composable(Routes.WARRANTIES) { WarrantiesScreen() }
            composable(Routes.INVENTORY) { InventoryScreen() }
            composable(Routes.MAINTENANCE) { MaintenanceScreen() }
            composable(Routes.SUBSCRIPTIONS) { SubscriptionsScreen() }
            composable(Routes.FAMILY) { FamilyScreen() }
        }
    }
}
