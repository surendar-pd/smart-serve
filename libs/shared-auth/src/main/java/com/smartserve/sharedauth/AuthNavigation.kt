package com.smartserve.sharedauth

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object AuthRoutes {
    const val INTRO_CUSTOMER = "intro_customer"
    const val INTRO_PROVIDER = "intro_provider"
    const val LOGIN = "login"
    const val SIGNUP_CUSTOMER = "signup_customer"
    const val SIGNUP_PROVIDER = "signup_provider"
    const val FORGOT_PASSWORD = "forgot_password"
    const val CUSTOMER_PROFILE_SETUP = "customer_profile_setup/{uid}"
    const val PROVIDER_PROFILE_SETUP = "provider_profile_setup/{uid}"

    fun customerProfileSetup(uid: String) = "customer_profile_setup/$uid"
    fun providerProfileSetup(uid: String) = "provider_profile_setup/$uid"
}

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onNavigateToCustomerHome: () -> Unit,
    onNavigateToProviderHome: () -> Unit,
    onNavigateToSignUpFromLogin: () -> Unit,
) {
    composable(AuthRoutes.INTRO_CUSTOMER) {
        IntroCustomerScreen(
            onGetStarted = { navController.navigate(AuthRoutes.LOGIN) }
        )
    }

    composable(AuthRoutes.INTRO_PROVIDER) {
        IntroProviderScreen(
            onGetStarted = { navController.navigate(AuthRoutes.LOGIN) }
        )
    }

    composable(AuthRoutes.LOGIN) {
        val vm: AuthViewModel = hiltViewModel()
        LoginScreen(
            viewModel = vm,
            onBack = { navController.popBackStack() },
            onForgotPassword = { navController.navigate(AuthRoutes.FORGOT_PASSWORD) },
            onNavigateToSignUp = onNavigateToSignUpFromLogin,
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

    composable(AuthRoutes.SIGNUP_CUSTOMER) {
        val vm: AuthViewModel = hiltViewModel()
        SignUpCustomerScreen(
            viewModel = vm,
            onBack = { navController.popBackStack() },
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
            viewModel = vm,
            onBack = { navController.popBackStack() },
            onNavigateToProfileSetup = { uid ->
                navController.navigate(AuthRoutes.providerProfileSetup(uid)) {
                    popUpTo(AuthRoutes.SIGNUP_PROVIDER) { inclusive = true }
                }
            }
        )
    }

    composable(AuthRoutes.FORGOT_PASSWORD) {
        val vm: AuthViewModel = hiltViewModel()
        ForgotPasswordScreen(
            viewModel = vm,
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = AuthRoutes.CUSTOMER_PROFILE_SETUP,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })
    ) { backStack ->
        val uid = backStack.arguments?.getString("uid") ?: ""
        val vm: CustomerProfileViewModel = hiltViewModel()
        CustomerProfileSetupScreen(
            uid = uid,
            viewModel = vm,
            onStart = { onNavigateToCustomerHome() }
        )
    }

    composable(
        route = AuthRoutes.PROVIDER_PROFILE_SETUP,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })
    ) { backStack ->
        val uid = backStack.arguments?.getString("uid") ?: ""
        val vm: ProviderProfileViewModel = hiltViewModel()
        ProviderProfileSetupScreen(
            uid = uid,
            viewModel = vm,
            onStart = { onNavigateToProviderHome() }
        )
    }
}
