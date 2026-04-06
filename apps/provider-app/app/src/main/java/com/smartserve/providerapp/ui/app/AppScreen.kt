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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.providerapp.ui.layouts.AppLayout
import com.smartserve.providerapp.ui.layouts.AppTab

private sealed interface HomeStack {
    object Home : HomeStack
    data class RequestDetail(val bookingId: String) : HomeStack
    data class ActiveJob(val bookingId: String) : HomeStack
    data class Chat(val bookingId: String, val returnTo: HomeStack) : HomeStack
    data class Navigation(                             // ← ADD
        val customerLat: Double,
        val customerLng: Double,
        val customerAddress: String,
        val returnTo: HomeStack,
    ) : HomeStack
}

private sealed interface ProfileStack {
    object Main : ProfileStack
    object ServicesList : ProfileStack
    object PrivacyData : ProfileStack
    data class ServiceEditor(val serviceId: String?, val sessionKey: Long) : ProfileStack
}

@Composable
fun AppScreen(onLogout: () -> Unit) {
    hiltViewModel<ProviderNotificationsViewModel>()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var homeStack by remember { mutableStateOf<HomeStack>(HomeStack.Home) }
    var profileStack by remember { mutableStateOf<ProfileStack>(ProfileStack.Main) }

    val tabs = listOf(
        AppTab(title = "Home",     icon = Icons.Filled.Home),
        AppTab(title = "Bookings", icon = Icons.Filled.DateRange),
        AppTab(title = "Profile",  icon = Icons.Filled.Person),
    )

    AppLayout(
        currentTabIndex = selectedTabIndex,
        tabs            = tabs,
        onTabSelected   = { index ->
            selectedTabIndex = index
            if (index != 0) homeStack = HomeStack.Home
            if (index != 2) profileStack = ProfileStack.Main
        },
        content = { innerPadding ->
            when (selectedTabIndex) {
                0 -> when (val stack = homeStack) {
                    is HomeStack.Home -> HomeScreen(
                        modifier                  = Modifier.fillMaxSize().padding(innerPadding),
                        onNavigateToRequestDetail = { bookingId ->
                            homeStack = HomeStack.RequestDetail(bookingId)
                        },
                    )
                    is HomeStack.RequestDetail -> RequestDetailScreen(
                        bookingId             = stack.bookingId,
                        modifier              = Modifier.fillMaxSize().padding(innerPadding),
                        onBack                = { homeStack = HomeStack.Home },
                        onNavigateToActiveJob = { bookingId ->
                            homeStack = HomeStack.ActiveJob(bookingId)
                        },
                        onNavigateToChat      = { bookingId ->
                            homeStack = HomeStack.Chat(
                                bookingId = bookingId,
                                returnTo  = stack,
                            )
                        },
                    )
                    is HomeStack.ActiveJob -> ActiveJobScreen(
                        bookingId            = stack.bookingId,
                        modifier             = Modifier.fillMaxSize().padding(innerPadding),
                        onNavigateToBookings = {
                            selectedTabIndex = 1
                            homeStack = HomeStack.Home
                        },
                        onNavigateToChat     = { bookingId ->
                            homeStack = HomeStack.Chat(
                                bookingId = bookingId,
                                returnTo  = stack,
                            )
                        },
                        onNavigateToMap      = { lat, lng, address ->      // ← ADD
                            homeStack = HomeStack.Navigation(
                            customerLat     = lat,
                            customerLng     = lng,
                            customerAddress = address,
                            returnTo        = stack,
                            )
                        },
                    )
                    is HomeStack.Chat -> ChatScreen(
                        bookingId = stack.bookingId,
                        onBack    = { homeStack = stack.returnTo },
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        topPadding     = innerPadding.calculateTopPadding(),
                    )
                    is HomeStack.Navigation -> ProviderNavigationScreen(
                        customerLat     = stack.customerLat,
                        customerLng     = stack.customerLng,
                        customerAddress = stack.customerAddress,
                        onBack          = { homeStack = stack.returnTo },
                        bottomPadding   = innerPadding.calculateBottomPadding(),
                        topPadding      = innerPadding.calculateTopPadding(),
            )
                }
                1 -> BookingsScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    onNavigateToRequestDetail = { bookingId ->
                        selectedTabIndex = 0
                        homeStack = HomeStack.RequestDetail(bookingId)
                    },
                    onNavigateToActiveJob = { bookingId ->
                        selectedTabIndex = 0
                        homeStack = HomeStack.ActiveJob(bookingId)
                    },
                    onNavigateToMap = { lat, lng, address ->
                        selectedTabIndex = 0
                        homeStack = HomeStack.Navigation(
                            customerLat = lat,
                            customerLng = lng,
                            customerAddress = address,
                            returnTo = HomeStack.Home,
                        )
                    },
                )
                2 -> when (val stack = profileStack) {
                    is ProfileStack.Main -> ProfileScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onLogout = onLogout,
                        onOpenServices = { profileStack = ProfileStack.ServicesList },
                        onOpenPrivacyData = { profileStack = ProfileStack.PrivacyData },
                    )
                    is ProfileStack.ServicesList -> MyServicesScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onBack = { profileStack = ProfileStack.Main },
                        onAddService = {
                            profileStack = ProfileStack.ServiceEditor(
                                serviceId = null,
                                sessionKey = System.currentTimeMillis(),
                            )
                        },
                        onOpenService = { serviceId ->
                            profileStack = ProfileStack.ServiceEditor(
                                serviceId = serviceId,
                                sessionKey = System.currentTimeMillis(),
                            )
                        },
                    )
                    is ProfileStack.ServiceEditor -> ServiceEditorScreen(
                        serviceId = stack.serviceId,
                        sessionKey = stack.sessionKey,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onBack = { profileStack = ProfileStack.ServicesList },
                        onFinished = { profileStack = ProfileStack.ServicesList },
                    )
                    is ProfileStack.PrivacyData -> PrivacyDataScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        onBack = { profileStack = ProfileStack.Main },
                    )
                }
            }
        },
    )
}