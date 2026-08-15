package com.vidacotidiana.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vidacotidiana.app.feature.auth.AuthManager
import com.vidacotidiana.app.feature.auth.LoginScreen
import com.vidacotidiana.app.feature.reminders.RemindersScreen

private object Routes {
    const val LOGIN = "login"
    const val REMINDERS = "reminders"
}

@Composable
fun AppNavGraph(authManager: AuthManager) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (authManager.isLoggedIn()) Routes.REMINDERS else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                authManager = authManager,
                onLoggedIn = {
                    navController.navigate(Routes.REMINDERS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.REMINDERS) {
            RemindersScreen()
        }
    }
}
