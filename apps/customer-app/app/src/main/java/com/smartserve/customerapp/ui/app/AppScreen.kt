package com.smartserve.customerapp.ui.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.customerapp.ui.layouts.AppLayout
import com.smartserve.customerapp.ui.layouts.AppTab

private data class RatingThankYouPayload(
    val serviceName: String,
    val providerName: String,
    val rating: Float,
)

@Composable
fun AppScreen(
    onLogout: () -> Unit,
) {
    hiltViewModel<CustomerNotificationsViewModel>()

    val activity = LocalContext.current as ComponentActivity
    val cartViewModel: CartViewModel = hiltViewModel(activity)
    val cartItems by cartViewModel.cartItems.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // ── Home tab navigation stack ────────────────────────────────────────────
    var selectedCategoryId    by remember { mutableStateOf("") }
    var selectedCategoryLabel by remember { mutableStateOf("") }
    var selectedProviderUid   by remember { mutableStateOf("") }
    var selectedProviderName  by remember { mutableStateOf("") }
    var selectedService       by remember { mutableStateOf<CustomerServiceListing?>(null) }
    var showProfile           by remember { mutableStateOf(false) }
    var showPrivacyData       by remember { mutableStateOf(false) }

    // ── Search tab navigation stack ──────────────────────────────────────────
    var searchProviderUid    by remember { mutableStateOf("") }
    var searchProviderName   by remember { mutableStateOf("") }
    var searchSelectedService by remember { mutableStateOf<CustomerServiceListing?>(null) }

    // ── Bookings tab navigation stack ────────────────────────────────────────
    var selectedBooking by remember { mutableStateOf<CustomerBooking?>(null) }
    var chatBookingId by remember { mutableStateOf<String?>(null) }
    var thankYouPayload by remember { mutableStateOf<RatingThankYouPayload?>(null) }

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
        showPrivacyData       = false
    }

    fun clearSearchStack() {
        searchSelectedService = null
        searchProviderUid     = ""
        searchProviderName    = ""
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
                        cartItems = cartItems,
                        categoryId = selectedCategoryId,
                        onBack   = { selectedService = null },
                        onAddToCart = { item ->
                            cartViewModel.addToCart(item) {
                                selectedService     = null
                                selectedProviderUid = ""
                                selectedTabIndex    = 2
                            }
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
                        onOpenPrivacyData = {
                            showProfile = false
                            showPrivacyData = true
                        },
                    )
                    showPrivacyData -> PrivacyDataScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onBack = { showPrivacyData = false },
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
                        cartItems = cartItems,
                        categoryId = "",
                        onBack   = { searchSelectedService = null },
                        onAddToCart = { item ->
                            cartViewModel.addToCart(item) {
                                clearSearchStack()
                                selectedTabIndex = 2
                            }
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
                    onRemoveItem = cartViewModel::removeFromCart,
                    onConfirm    = { selectedTabIndex = 3 },
                    viewModel    = cartViewModel,
                )

                // ── Bookings ─────────────────────────────────────────────────
                3 -> when {
                    thankYouPayload != null -> RatingThankYouScreen(
                        serviceName = thankYouPayload!!.serviceName,
                        providerName = thankYouPayload!!.providerName,
                        rating = thankYouPayload!!.rating,
                        onDone = {
                            thankYouPayload = null
                            selectedBooking = null
                        },
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                    chatBookingId != null -> ChatScreen(
                        bookingId = chatBookingId!!,
                        onBack = { chatBookingId = null },
                        topPadding = innerPadding.calculateTopPadding(),
                        bottomPadding = innerPadding.calculateBottomPadding(),
                    )
                    selectedBooking != null -> BookingDetailScreen(
                        booking = selectedBooking!!,
                        onBack = { selectedBooking = null },
                        onOpenChat = { id -> chatBookingId = id },
                        onRatingSubmitted = { booking, rating ->
                            thankYouPayload = RatingThankYouPayload(
                                serviceName = booking.serviceName,
                                providerName = booking.providerName,
                                rating = rating,
                            )
                        },
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                    else -> BookingsScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onSelectBooking = { selectedBooking = it },
                    )
                }
            }
        },
    )
}
