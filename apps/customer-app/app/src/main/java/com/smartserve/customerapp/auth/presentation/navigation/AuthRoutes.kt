//package com.smartserve.auth.presentation.navigation
package com.smartserve.customerapp.auth.presentation.navigation

// Matches the Routes.kt pattern already present in the project.
// Add these to your existing Routes sealed class / object.

object AuthRoutes {
    const val INTRO_CUSTOMER    = "intro_customer"
    const val INTRO_PROVIDER    = "intro_provider"
    const val LOGIN             = "login"
    const val SIGNUP_CUSTOMER   = "signup_customer"
    const val SIGNUP_PROVIDER   = "signup_provider"
    const val FORGOT_PASSWORD   = "forgot_password"
    const val CUSTOMER_PROFILE_SETUP = "customer_profile_setup/{uid}"
    const val PROVIDER_PROFILE_SETUP = "provider_profile_setup/{uid}"

    fun customerProfileSetup(uid: String) = "customer_profile_setup/$uid"
    fun providerProfileSetup(uid: String) = "provider_profile_setup/$uid"
}
