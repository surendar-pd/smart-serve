package com.smartserve.providerapp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.smartserve.providerapp.navigation.Routes
import com.smartserve.providerapp.ui.layouts.AuthLayout
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun AuthScreen(
    navController: NavController,
) {
    AuthLayout {
        SharedText(
            text = "Welcome to SmartServe",
            variant = SharedTextVariant.Title,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SharedText(
            text = "Sign in to continue.",
            variant = SharedTextVariant.Body,
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

