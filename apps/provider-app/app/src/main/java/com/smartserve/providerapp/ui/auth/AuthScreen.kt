package com.smartserve.providerapp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.smartserve.providerapp.navigation.Routes
import com.smartserve.providerapp.ui.layouts.AuthLayout
import com.smartserve.sharedui.SharedButton

@Composable
fun AuthScreen(
    navController: NavController,
) {
    AuthLayout {
        Text(
            text = "Welcome to SmartServe",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to continue.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SharedButton(
            text = "Continue",
            onClick = {
                navController.navigate(Routes.App) {
                    popUpTo(Routes.Auth) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

