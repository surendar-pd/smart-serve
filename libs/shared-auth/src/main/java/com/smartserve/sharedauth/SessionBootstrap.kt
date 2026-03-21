package com.smartserve.sharedauth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

/**
 * Cold-start gate: navigates once to [appRoute] if Firebase has a persisted user session,
 * otherwise to [authRoute] (intro / sign-in flow). Pops the bootstrap entry from the back stack.
 */
@Composable
fun SessionBootstrapRoute(
    navController: NavController,
    bootstrapRoute: String,
    appRoute: String,
    authRoute: String,
) {
    LaunchedEffect(Unit) {
        val hasSession = FirebaseAuth.getInstance().currentUser != null
        val destination = if (hasSession) appRoute else authRoute
        navController.navigate(destination) {
            popUpTo(bootstrapRoute) { inclusive = true }
        }
    }
    Box(Modifier.fillMaxSize())
}
