//package com.smartserve.auth.presentation.navigation
package com.smartserve.customerapp.auth.presentation.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smartserve.customerapp.auth.presentation.screens.*
import com.smartserve.customerapp.auth.presentation.viewmodel.AuthViewModel
import com.smartserve.customerapp.auth.presentation.viewmodel.CustomerProfileViewModel
import com.smartserve.customerapp.auth.presentation.viewmodel.ProviderProfileViewModel

/**
 * Call this from your AppNavigation.kt to wire up all auth screens.
 *
 *   NavHost(navController, startDestination = AuthRoutes.INTRO_CUSTOMER) {
 *       authNavGraph(navController, onCustomerHome = { ... }, onProviderHome = { ... })
 *   }
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onNavigateToCustomerHome: () -> Unit,
    onNavigateToProviderHome: () -> Unit
) {
    // ── Introduction ──────────────────────────────
    composable(AuthRoutes.INTRO_CUSTOMER) {
        val vm: AuthViewModel = hiltViewModel()
        IntroCustomerScreen(
            onGetStarted = { navController.navigate(AuthRoutes.SIGNUP_CUSTOMER) },
            onLogin      = { navController.navigate(AuthRoutes.LOGIN) }
        )
    }

    composable(AuthRoutes.INTRO_PROVIDER) {
        IntroProviderScreen(
            onJoinAsProvider = { navController.navigate(AuthRoutes.SIGNUP_PROVIDER) },
            onLogin          = { navController.navigate(AuthRoutes.LOGIN) }
        )
    }

    // ── Login ─────────────────────────────────────
    composable(AuthRoutes.LOGIN) {
        val vm: AuthViewModel = hiltViewModel()
        LoginScreen(
            viewModel         = vm,
            onForgotPassword  = { navController.navigate(AuthRoutes.FORGOT_PASSWORD) },
            onNavigateToCustomerHome = onNavigateToCustomerHome,
            onNavigateToProviderHome = onNavigateToProviderHome,
            onNavigateToCustomerSetup = { uid ->
                navController.navigate(AuthRoutes.customerProfileSetup(uid))
            },
            onNavigateToProviderSetup = { uid ->
                navController.navigate(AuthRoutes.providerProfileSetup(uid))
            }
        )
    }

    // ── Sign Up ───────────────────────────────────
    composable(AuthRoutes.SIGNUP_CUSTOMER) {
        val vm: AuthViewModel = hiltViewModel()
        SignUpCustomerScreen(
            viewModel  = vm,
            onBack     = { navController.popBackStack() },
            onNavigateToProfileSetup = { uid ->
                navController.navigate(AuthRoutes.customerProfileSetup(uid)) {
                    popUpTo(AuthRoutes.SIGNUP_CUSTOMER) { inclusive = true }
                }
            }
        )
    }

    composable(AuthRoutes.SIGNUP_PROVIDER) {
        val vm: AuthViewModel = hiltViewModel()
        SignUpProviderScreen(
            viewModel  = vm,
            onBack     = { navController.popBackStack() },
            onNavigateToProfileSetup = { uid ->
                navController.navigate(AuthRoutes.providerProfileSetup(uid)) {
                    popUpTo(AuthRoutes.SIGNUP_PROVIDER) { inclusive = true }
                }
            }
        )
    }

    // ── Forgot Password ───────────────────────────
    composable(AuthRoutes.FORGOT_PASSWORD) {
        val vm: AuthViewModel = hiltViewModel()
        ForgotPasswordScreen(
            viewModel = vm,
            onBack    = { navController.popBackStack() }
        )
    }

    // ── Profile Setup ─────────────────────────────
    composable(
        route     = AuthRoutes.CUSTOMER_PROFILE_SETUP,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })
    ) { backStack ->
        val uid = backStack.arguments?.getString("uid") ?: ""
        val vm: CustomerProfileViewModel = hiltViewModel()
        CustomerProfileSetupScreen(
            uid       = uid,
            viewModel = vm,
            onStart   = {
                onNavigateToCustomerHome()
            }
        )
    }

    composable(
        route     = AuthRoutes.PROVIDER_PROFILE_SETUP,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })
    ) { backStack ->
        val uid = backStack.arguments?.getString("uid") ?: ""
        val vm: ProviderProfileViewModel = hiltViewModel()
        ProviderProfileSetupScreen(
            uid       = uid,
            viewModel = vm,
            onStart   = {
                onNavigateToProviderHome()
            }
        )
    }
}
