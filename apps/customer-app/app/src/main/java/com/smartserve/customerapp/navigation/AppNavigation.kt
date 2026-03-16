/*package com.smartserve.customerapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartserve.customerapp.ui.app.AppScreen
import com.smartserve.customerapp.ui.auth.AuthScreen

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
}*/
package com.smartserve.customerapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.smartserve.customerapp.auth.presentation.navigation.AuthRoutes
import com.smartserve.customerapp.auth.presentation.navigation.authNavGraph
import com.smartserve.customerapp.ui.app.AppScreen

@Composable
fun AppNavigation() {
  val navController = rememberNavController()

  NavHost(
    navController    = navController,
    startDestination = Routes.Auth
  ) {

    // ── Auth graph ─────────────────────────────────
    navigation(
      route            = Routes.Auth,
      startDestination = AuthRoutes.INTRO_CUSTOMER
    ) {
      authNavGraph(
        navController            = navController,
        onNavigateToCustomerHome = {
          navController.navigate(Routes.App) {
            popUpTo(Routes.Auth) { inclusive = true }
          }
        },
        onNavigateToProviderHome = {
          navController.navigate(Routes.App) {
            popUpTo(Routes.Auth) { inclusive = true }
          }
        }
      )
    }

    // ── App graph ──────────────────────────────────
    composable(Routes.App) {
      AppScreen()
    }
  }
}
