package com.smartserve.providerapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartserve.providerapp.ui.app.AppScreen
import com.smartserve.providerapp.ui.auth.AuthScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Auth,
    ) {
        composable(Routes.Auth) {
            AuthScreen(navController = navController)
        }
        composable(Routes.App) {
            AppScreen()
        }
    }
}

