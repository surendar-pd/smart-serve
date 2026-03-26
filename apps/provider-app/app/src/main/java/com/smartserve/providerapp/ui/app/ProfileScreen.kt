package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedauth.AuthViewModel
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onUiComponents: () -> Unit,
) {
    val viewModel: AuthViewModel = hiltViewModel()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SharedText(
            text = "Profile",
            variant = SharedTextVariant.Title,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SharedButton(
            text = "UI components",
            onClick = onUiComponents,
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Outline,
        )
        Spacer(Modifier.weight(1f))
        SharedButton(
            text = "Log out",
            onClick = {
                viewModel.signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Destructive,
        )
    }
}
