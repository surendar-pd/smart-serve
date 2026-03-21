package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smartserve.providerapp.ui.layouts.AppLayout
import com.smartserve.providerapp.ui.layouts.AppTab

@Composable
fun AppScreen(
    onLogout: () -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        AppTab(title = "Home", icon = Icons.Filled.Home),
        AppTab(title = "Bookings", icon = Icons.Filled.DateRange),
        AppTab(title = "Profile", icon = Icons.Filled.Person),
    )

    AppLayout(
        currentTabIndex = selectedTabIndex,
        tabs = tabs,
        onTabSelected = { selectedTabIndex = it },
        content = { innerPadding ->
            when (selectedTabIndex) {
                0 -> HomeScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
                1 -> BookingsScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
                2 -> ProfileScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    onLogout = onLogout,
                )
            }
        },
    )
}
