package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smartserve.customerapp.ui.layouts.AppLayout
import com.smartserve.customerapp.ui.layouts.AppTab

@Composable
fun AppScreen(
    onLogout: () -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("") }
    var showProfile by remember { mutableStateOf(false) }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    var checkoutRequested by remember { mutableStateOf(false) }

    val tabs = listOf(
        AppTab(title = "Home", icon = Icons.Filled.Home),
        AppTab(title = "Search", icon = Icons.Filled.Search),
        AppTab(title = "Cart", icon = Icons.Filled.ShoppingCart),
        AppTab(title = "Bookings", icon = Icons.Filled.DateRange),
    )

    AppLayout(
        currentTabIndex = selectedTabIndex,
        tabs = tabs,
        onTabSelected = {
            selectedTabIndex = it
            if (it != 0) {
                selectedCategory = ""
                selectedProvider = ""
                showProfile = false
            }
            if (it != 2) {
                checkoutRequested = false
            }
        },
        content = { innerPadding ->
            when (selectedTabIndex) {
                0 -> when {
                    selectedProvider.isNotEmpty() -> ServiceListScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        providerName = selectedProvider,
                        onBack = { selectedProvider = "" },
                        onAddToCart = { cartItems = cartItems + it },
                    )
                    selectedCategory.isNotEmpty() -> CategoryListScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        categoryName = selectedCategory,
                        onBack = { selectedCategory = "" },
                        onProviderClick = { selectedProvider = it },
                    )
                    showProfile -> ProfileScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onBack = { showProfile = false },
                        onLogout = onLogout,
                    )
                    else -> HomeScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onNavigateToCategory = { selectedCategory = it },
                        onNavigateToProfile = { showProfile = true },
                        onNavigateToSearch = { selectedTabIndex = 1 },
                    )
                }
                1 -> SearchScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    onProviderClick = {
                        selectedProvider = it
                        selectedTabIndex = 0
                    },
                )
                2 -> if (checkoutRequested && cartItems.isNotEmpty()) {
                    BookingScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        providerName = cartItems.first().providerName,
                        serviceName = cartItems.joinToString(", ") { it.serviceName },
                        onBack = { checkoutRequested = false },
                        onAddToCart = { checkoutRequested = false },
                        onConfirm = {
                            cartItems = emptyList()
                            checkoutRequested = false
                            selectedTabIndex = 0
                        },
                    )
                } else {
                    CartScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        cartItems = cartItems,
                        onRemoveItem = { cartItems = cartItems - it },
                        onCheckout = { checkoutRequested = true },
                    )
                }
                3 -> BookingsScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        },
    )
}
