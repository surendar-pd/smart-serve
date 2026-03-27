package com.smartserve.providerapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedauth.AuthRoutes
import com.smartserve.sharedauth.authNavGraph
import com.smartserve.providerapp.ui.app.AppScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val startDestination by rememberSaveable {
        mutableStateOf(
            if (FirebaseAuth.getInstance().currentUser != null) Routes.App
            else Routes.Auth
        )
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
    ) {
        navigation(
            route            = Routes.Auth,
            startDestination = AuthRoutes.INTRO_PROVIDER,
        ) {
            authNavGraph(
                navController = navController,
                onNavigateToCustomerHome = {
                    navController.navigate(Routes.App) {
                        popUpTo(Routes.Auth) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToProviderHome = {
                    navController.navigate(Routes.App) {
                        popUpTo(Routes.Auth) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUpFromLogin = {
                    navController.navigate(AuthRoutes.SIGNUP_PROVIDER) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.App) {
            AppScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.Auth) {
                        popUpTo(Routes.App) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}