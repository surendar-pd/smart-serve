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

    // ── Home tab navigation stack ────────────────────────────────────────────
    var selectedCategoryId    by remember { mutableStateOf("") }
    var selectedCategoryLabel by remember { mutableStateOf("") }
    var selectedProviderUid   by remember { mutableStateOf("") }
    var selectedProviderName  by remember { mutableStateOf("") }
    var selectedService       by remember { mutableStateOf<CustomerServiceListing?>(null) }
    var showProfile           by remember { mutableStateOf(false) }

    // ── Search tab navigation stack ──────────────────────────────────────────
    var searchProviderUid    by remember { mutableStateOf("") }
    var searchProviderName   by remember { mutableStateOf("") }
    var searchSelectedService by remember { mutableStateOf<CustomerServiceListing?>(null) }

    // ── Cart ─────────────────────────────────────────────────────────────────
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }

    val tabs = listOf(
        AppTab(title = "Home",     icon = Icons.Filled.Home),
        AppTab(title = "Search",   icon = Icons.Filled.Search),
        AppTab(title = "Cart",     icon = Icons.Filled.ShoppingCart),
        AppTab(title = "Bookings", icon = Icons.Filled.DateRange),
    )

    fun clearHomeStack() {
        selectedService       = null
        selectedProviderUid   = ""
        selectedProviderName  = ""
        selectedCategoryId    = ""
        selectedCategoryLabel = ""
        showProfile           = false
    }

    fun clearSearchStack() {
        searchSelectedService = null
        searchProviderUid     = ""
        searchProviderName    = ""
    }

    /** Add item only if the same (providerUid, serviceId, date, time) isn't already in cart. */
    fun addToCart(item: CartItem) {
        val isDuplicate = cartItems.any { existing ->
            existing.providerUid == item.providerUid &&
            existing.serviceId   == item.serviceId   &&
            existing.date        == item.date         &&
            existing.time        == item.time
        }
        if (!isDuplicate) cartItems = cartItems + item
    }

    AppLayout(
        currentTabIndex = selectedTabIndex,
        tabs = tabs,
        onTabSelected = { index ->
            selectedTabIndex = index
            if (index != 0) clearHomeStack()
            if (index != 1) clearSearchStack()
        },
        content = { innerPadding ->
            when (selectedTabIndex) {

                // ── Home ─────────────────────────────────────────────────────
                0 -> when {
                    selectedService != null -> BookingScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        service  = selectedService!!,
                        onBack   = { selectedService = null },
                        onAddToCart = { item ->
                            addToCart(item)
                            selectedService     = null
                            selectedProviderUid = ""
                            selectedTabIndex    = 2
                        },
                    )
                    selectedProviderUid.isNotEmpty() -> ServiceListScreen(
                        modifier     = Modifier.fillMaxSize().padding(innerPadding),
                        providerUid  = selectedProviderUid,
                        providerName = selectedProviderName,
                        categoryId   = selectedCategoryId,
                        onBack       = { selectedProviderUid = ""; selectedProviderName = "" },
                        onSelectService = { svc -> selectedService = svc },
                    )
                    selectedCategoryId.isNotEmpty() -> CategoryListScreen(
                        modifier       = Modifier.fillMaxSize().padding(innerPadding),
                        categoryId     = selectedCategoryId,
                        categoryLabel  = selectedCategoryLabel,
                        onBack         = { selectedCategoryId = ""; selectedCategoryLabel = "" },
                        onProviderClick = { uid, name ->
                            selectedProviderUid  = uid
                            selectedProviderName = name
                        },
                    )
                    showProfile -> ProfileScreen(
                        modifier  = Modifier.fillMaxSize().padding(innerPadding),
                        onBack    = { showProfile = false },
                        onLogout  = onLogout,
                    )
                    else -> HomeScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onNavigateToCategory = { id, label ->
                            selectedCategoryId    = id
                            selectedCategoryLabel = label
                        },
                        onNavigateToProfile = { showProfile = true },
                        onNavigateToSearch  = { selectedTabIndex = 1 },
                    )
                }

                // ── Search ───────────────────────────────────────────────────
                1 -> when {
                    searchSelectedService != null -> BookingScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        service  = searchSelectedService!!,
                        onBack   = { searchSelectedService = null },
                        onAddToCart = { item ->
                            addToCart(item)
                            clearSearchStack()
                            selectedTabIndex = 2
                        },
                    )
                    searchProviderUid.isNotEmpty() -> ServiceListScreen(
                        modifier     = Modifier.fillMaxSize().padding(innerPadding),
                        providerUid  = searchProviderUid,
                        providerName = searchProviderName,
                        categoryId   = "",           // search is cross-category
                        onBack       = { searchProviderUid = ""; searchProviderName = "" },
                        onSelectService = { svc -> searchSelectedService = svc },
                    )
                    else -> SearchScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onResultClick = { service ->
                            searchProviderUid  = service.providerUid
                            searchProviderName = service.providerName
                        },
                    )
                }

                // ── Cart ─────────────────────────────────────────────────────
                2 -> CartScreen(
                    modifier     = Modifier.fillMaxSize().padding(innerPadding),
                    cartItems    = cartItems,
                    onRemoveItem = { cartItems = cartItems - it },
                    onConfirm    = {
                        cartItems        = emptyList()
                        selectedTabIndex = 3         // go to Bookings after confirming
                    },
                )

                // ── Bookings ─────────────────────────────────────────────────
                3 -> BookingsScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        },
    )
}
