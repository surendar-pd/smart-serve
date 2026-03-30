package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedRating
import com.smartserve.sharedui.SharedTabs
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import androidx.compose.material3.MaterialTheme

private val tabTitles = listOf("Upcoming", "Completed")

@Composable
fun BookingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: BookingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        ProviderTabHeader(
            title = "Bookings",
            subtitle = "Upcoming and completed work",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        SharedTabs(
            tabCount         = tabTitles.size,
            selectedTabIndex = selectedTab,
            onTabSelected    = { selectedTab = it },
        ) { index ->
            SharedText(text = tabTitles[index], variant = SharedTextVariant.Label)
        }

        when {
            state.isLoading -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { SharedLoading() }

            state.errorMessage != null -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { SharedText(text = state.errorMessage!!, variant = SharedTextVariant.Body) }

            selectedTab == 0 -> {
                if (state.upcomingBookings.isEmpty()) {
                    SharedEmptyState(title = "No upcoming bookings")
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.upcomingBookings, key = { it.id }) { booking ->
                            PastBookingCard(
                                booking = booking,
                                onClick = { onNavigateToRequestDetail(booking.id) },
                            )
                        }
                    }
                }
            }

            else -> {
                val completed = state.pastBookings.filter {
                    it.status == RequestStatus.COMPLETED || it.status == RequestStatus.DECLINED
                }
                if (completed.isEmpty()) {
                    SharedEmptyState(title = "No completed bookings yet")
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SharedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SharedText(text = "This Week",              variant = SharedTextVariant.Caption)
                                    SharedText(text = "$${state.weekEarnings}", variant = SharedTextVariant.Title)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SharedText(text = "This Month",              variant = SharedTextVariant.Caption)
                                    SharedText(text = "$${state.monthEarnings}", variant = SharedTextVariant.Title)
                                }
                            }
                        }
                        LazyColumn(
                            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(completed, key = { it.id }) { booking ->
                                PastBookingCard(booking = booking)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastBookingCard(
    booking: ServiceRequest,
    onClick: (() -> Unit)? = null,
) {
    SharedCard(onClick = onClick ?: {}) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                SharedAvatar(
                    name = booking.customerFirstName.ifBlank { booking.customerInitials },
                    size = 36.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SharedText(
                        text = booking.customerFirstName,
                        variant = SharedTextVariant.BodyStrong,
                    )
                    SharedText(text = booking.serviceType, variant = SharedTextVariant.Body)
                }
                val statusLabel = when (booking.status) {
                    RequestStatus.COMPLETED -> "Completed"
                    RequestStatus.DECLINED  -> "Declined"
                    RequestStatus.ACTIVE    -> "Active"
                    RequestStatus.PENDING   -> "Pending"
                    RequestStatus.NEW       -> "New"
                }
                val statusBg: Color = when (booking.status) {
                    RequestStatus.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    RequestStatus.PENDING, RequestStatus.NEW -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                    RequestStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                    RequestStatus.DECLINED -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    SharedText(
                        text = statusLabel,
                        variant = SharedTextVariant.Caption,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            SharedText(
                text    = "${booking.date} · ${booking.time} · ${booking.neighborhood}",
                variant = SharedTextVariant.Caption,
            )
            // Only show earnings for completed bookings
            if (booking.earnings > 0 && booking.status == RequestStatus.COMPLETED) {
                Spacer(Modifier.height(4.dp))
                SharedText(
                    text    = "Earned: $${booking.earnings}",
                    variant = SharedTextVariant.BodyStrong,
                )
            }
            if (booking.customerRating != null) {
                Spacer(Modifier.height(4.dp))
                SharedRating(rating = booking.customerRating)
            }
        }
    }
}