package com.smartserve.providerapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.smartserve.sharedauth.AuthRoutes
import com.smartserve.sharedauth.SessionBootstrapRoute
import com.smartserve.sharedauth.UserRole
import com.smartserve.sharedauth.authNavGraph
import com.smartserve.providerapp.ui.app.AppScreen

@Composable
fun AppNavigation() {
  val navController = rememberNavController()

  NavHost(
    navController    = navController,
    startDestination = Routes.Bootstrap
  ) {
    composable(Routes.Bootstrap) {
      SessionBootstrapRoute(
        navController = navController,
        bootstrapRoute = Routes.Bootstrap,
        appRoute = Routes.App,
        authRoute = Routes.Auth,
        expectedAppRole = UserRole.PROVIDER.value,
      )
    }

    // ── Auth graph ─────────────────────────────────
    navigation(
      route            = Routes.Auth,
      startDestination = AuthRoutes.INTRO_PROVIDER
    ) {
      authNavGraph(
        navController = navController,
        onNavigateToCustomerHome = {
          navController.navigate(Routes.App) {
            popUpTo(Routes.Auth) { inclusive = true }
          }
        },
        onNavigateToProviderHome = {
          navController.navigate(Routes.App) {
            popUpTo(Routes.Auth) { inclusive = true }
          }
        },
        onNavigateToSignUpFromLogin = {
          navController.navigate(AuthRoutes.SIGNUP_PROVIDER)
        },
      )
    }

    // ── App graph ──────────────────────────────────
    composable(Routes.App) {
      AppScreen(
        onLogout = {
          navController.navigate(Routes.Auth) {
            popUpTo(Routes.App) { inclusive = true }
          }
        },
      )
    }
  }
}

