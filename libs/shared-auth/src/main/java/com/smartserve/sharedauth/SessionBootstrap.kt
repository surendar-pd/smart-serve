package com.smartserve.sharedauth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.EntryPointAccessors

/**
 * Cold-start gate: navigates to [appRoute] only if Firebase has a session **and**
 * the user’s Firestore `activeRole` matches this APK’s [expectedAppRole] ([AppRoleGate]).
 * Otherwise signs out (when session exists but wrong app) and sends the user to [authRoute].
 */
@Composable
fun SessionBootstrapRoute(
    navController: NavController,
    bootstrapRoute: String,
    appRoute: String,
    authRoute: String,
    expectedAppRole: String,
) {
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navController.navigate(authRoute) {
                popUpTo(bootstrapRoute) { inclusive = true }
            }
            return@LaunchedEffect
        }
        val repo = EntryPointAccessors.fromApplication(
            appContext,
            AuthRepositoryEntryPoint::class.java,
        ).authRepository()
        val role = repo.getUserRole(user.uid)
        if (!AppRoleGate.isAllowed(expectedAppRole, role)) {
            FirebaseAuth.getInstance().signOut()
            navController.navigate(authRoute) {
                popUpTo(bootstrapRoute) { inclusive = true }
            }
        } else {
            navController.navigate(appRoute) {
                popUpTo(bootstrapRoute) { inclusive = true }
            }
        }
    }
    Box(Modifier.fillMaxSize())
}
