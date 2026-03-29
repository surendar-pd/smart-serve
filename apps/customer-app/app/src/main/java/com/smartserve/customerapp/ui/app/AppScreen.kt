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

    // Category navigation
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedCategoryLabel by remember { mutableStateOf("") }

    // Provider navigation
    var selectedProviderUid by remember { mutableStateOf("") }
    var selectedProviderName by remember { mutableStateOf("") }

    // Booking navigation
    var bookingProviderName by remember { mutableStateOf("") }
    var bookingService by remember { mutableStateOf("") }
    var bookingPrice by remember { mutableStateOf("") }

    var showProfile by remember { mutableStateOf(false) }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }

    val tabs = listOf(
        AppTab(title = "Home", icon = Icons.Filled.Home),
        AppTab(title = "Search", icon = Icons.Filled.Search),
        AppTab(title = "Cart", icon = Icons.Filled.ShoppingCart),
        AppTab(title = "Bookings", icon = Icons.Filled.DateRange),
    )

    fun clearHomeStack() {
        bookingProviderName = ""
        bookingService = ""
        bookingPrice = ""
        selectedProviderUid = ""
        selectedProviderName = ""
        selectedCategoryId = ""
        selectedCategoryLabel = ""
        showProfile = false
    }

    AppLayout(
        currentTabIndex = selectedTabIndex,
        tabs = tabs,
        onTabSelected = {
            selectedTabIndex = it
            if (it != 0) clearHomeStack()
        },
        content = { innerPadding ->
            when (selectedTabIndex) {
                0 -> when {
                    bookingProviderName.isNotEmpty() -> BookingScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        providerName = bookingProviderName,
                        serviceName = bookingService,
                        price = bookingPrice,
                        onBack = {
                            bookingProviderName = ""
                            bookingService = ""
                            bookingPrice = ""
                        },
                        onAddToCart = { item ->
                            cartItems = cartItems + item
                            bookingProviderName = ""
                            bookingService = ""
                            bookingPrice = ""
                            selectedProviderUid = ""
                            selectedTabIndex = 2
                        },
                    )
                    selectedProviderUid.isNotEmpty() -> ServiceListScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        providerUid = selectedProviderUid,
                        providerName = selectedProviderName,
                        onBack = { selectedProviderUid = ""; selectedProviderName = "" },
                        onSelectService = { prov, svc, price ->
                            bookingProviderName = prov
                            bookingService = svc
                            bookingPrice = price
                        },
                    )
                    selectedCategoryId.isNotEmpty() -> CategoryListScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        categoryId = selectedCategoryId,
                        categoryLabel = selectedCategoryLabel,
                        onBack = { selectedCategoryId = ""; selectedCategoryLabel = "" },
                        onProviderClick = { uid, name ->
                            selectedProviderUid = uid
                            selectedProviderName = name
                        },
                    )
                    showProfile -> ProfileScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onBack = { showProfile = false },
                        onLogout = onLogout,
                    )
                    else -> HomeScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onNavigateToCategory = { id, label ->
                            selectedCategoryId = id
                            selectedCategoryLabel = label
                        },
                        onNavigateToProfile = { showProfile = true },
                        onNavigateToSearch = { selectedTabIndex = 1 },
                        onNavigateToProvider = { uid, name ->
                            selectedProviderUid = uid
                            selectedProviderName = name
                        },
                    )
                }
                1 -> SearchScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    onProviderClick = { uid, name ->
                        selectedProviderUid = uid
                        selectedProviderName = name
                        selectedTabIndex = 0
                    },
                )
                2 -> CartScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    cartItems = cartItems,
                    onRemoveItem = { cartItems = cartItems - it },
                    onConfirm = {
                        cartItems = emptyList()
                        selectedTabIndex = 0
                    },
                )
                3 -> BookingsScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        },
    )
}
