package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedSwitchRow
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) {
            delay(2000)
            viewModel.clearSaved()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SharedTopAppBar(title = "Profile", onBack = onBack)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SharedLoading()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SharedAvatar(name = state.name.ifBlank { "User" }, size = 80.dp)

            Spacer(modifier = Modifier.height(8.dp))

            SharedText(text = state.name.ifBlank { "User" }, variant = SharedTextVariant.Title)
            SharedText(text = state.email, variant = SharedTextVariant.Body)

            Spacer(modifier = Modifier.height(28.dp))

            SharedText(
                text = "Personal Info",
                variant = SharedTextVariant.Title,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            SharedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = "Name",
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedTextField(
                value = state.email,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Email",
                readOnly = true,
                enabled = false,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                label = "Phone",
                placeholder = "e.g. +1 613 555 0100",
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedTextField(
                value = state.homeAddress,
                onValueChange = viewModel::onAddressChange,
                modifier = Modifier.fillMaxWidth(),
                label = "Home Address",
                placeholder = "e.g. 123 Main St, Ottawa",
            )

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(16.dp))

            SharedText(
                text = "Preferences",
                variant = SharedTextVariant.Title,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            SharedSwitchRow(
                checked = state.locationAwareness,
                onCheckedChange = viewModel::onLocationToggle,
                title = "Location Awareness",
                description = "Use your location for better service matches",
            )

            SharedSwitchRow(
                checked = state.pushNotifications,
                onCheckedChange = viewModel::onNotifToggle,
                title = "Push Notifications",
                description = "Get notified about bookings and updates",
            )

            Spacer(modifier = Modifier.height(28.dp))

            SharedButton(
                text = if (state.savedOk) "Saved!" else "Save Changes",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedButton(
                text = "Log Out",
                onClick = {
                    viewModel.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Ghost,
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SharedText(
                    text = state.errorMessage!!,
                    variant = SharedTextVariant.Caption,
                )
            }
        }
    }
}
