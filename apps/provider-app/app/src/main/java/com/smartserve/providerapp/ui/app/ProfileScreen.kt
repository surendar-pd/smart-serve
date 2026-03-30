package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedauth.AuthViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import java.util.Calendar

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onOpenServices: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val user        = FirebaseAuth.getInstance().currentUser
    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Provider"
    val memberSince = user?.metadata?.creationTimestamp?.let { ts ->
        Calendar.getInstance().also { it.timeInMillis = ts }.get(Calendar.YEAR).toString()
    } ?: "--"

    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProviderTabHeader(
            title = "Profile",
            subtitle = "Account and settings",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SharedAvatar(name = displayName, size = 64.dp)

            Spacer(Modifier.height(8.dp))

            SharedText(text = displayName, variant = SharedTextVariant.Title)
            SharedText(text = "Member since $memberSince", variant = SharedTextVariant.Body)

            Spacer(Modifier.height(28.dp))

            SharedListItem(
                title = "Services and Details",
                leadingIcon = Icons.Filled.Build,
                onClick = onOpenServices,
            )
            SharedListItem(
                title = "Serviceable Areas",
                leadingIcon = Icons.Filled.LocationOn,
                onClick = { /* TODO */ },
            )
            SharedListItem(
                title = "Availability Hours",
                leadingIcon = Icons.Filled.AccessTime,
                onClick = { /* TODO */ },
            )
            SharedListItem(
                title = "Notification Settings",
                leadingIcon = Icons.Filled.Notifications,
                onClick = { /* TODO */ },
            )
            SharedListItem(
                title = "Privacy & Data",
                leadingIcon = Icons.Filled.PrivacyTip,
                onClick = { /* TODO */ },
            )

            Spacer(Modifier.height(28.dp))

            SharedButton(
                text = "Log Out",
                onClick = { authViewModel.signOut(); onLogout() },
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Ghost,
            )
        }
    }
}