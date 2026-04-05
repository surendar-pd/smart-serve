package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
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
import com.smartserve.sharedui.SharedTabs
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import androidx.compose.material3.MaterialTheme

private val tabTitles = listOf("Pending", "Active", "Completed")

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

        Spacer(Modifier.height(12.dp))

        val pending = state.upcomingBookings.filter { it.status == RequestStatus.PENDING }
        val active = state.upcomingBookings.filter { it.status == RequestStatus.ACTIVE }
        val completed = state.pastBookings.filter { it.status == RequestStatus.COMPLETED }

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
                if (pending.isEmpty()) {
                    SharedEmptyState(title = "No pending bookings")
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(pending, key = { it.id }) { booking ->
                            PastBookingCard(
                                booking = booking,
                                onClick = { onNavigateToRequestDetail(booking.id) },
                            )
                        }
                    }
                }
            }

            selectedTab == 1 -> {
                if (active.isEmpty()) {
                    SharedEmptyState(title = "No active bookings")
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(active, key = { it.id }) { booking ->
                            PastBookingCard(
                                booking = booking,
                                onClick = { onNavigateToRequestDetail(booking.id) },
                            )
                        }
                    }
                }
            }

            else -> {
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
                                CompletedBookingCard(
                                    booking = booking,
                                    onClick = { onNavigateToRequestDetail(booking.id) },
                                )
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SharedAvatar(
                name = booking.customerFirstName.ifBlank { booking.customerInitials },
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SharedText(
                    text = booking.customerFirstName.ifBlank { "Customer" },
                    variant = SharedTextVariant.BodyStrong,
                )
                SharedText(text = booking.serviceType, variant = SharedTextVariant.Body)
                SharedText(
                    text    = "${booking.date} · ${booking.time}",
                    variant = SharedTextVariant.Caption,
                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompletedBookingCard(
    booking: ServiceRequest,
    onClick: (() -> Unit)? = null,
) {
    SharedCard(onClick = onClick ?: {}) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SharedAvatar(
                name = booking.customerFirstName.ifBlank { booking.customerInitials },
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SharedText(
                            text = booking.customerFirstName.ifBlank { "Customer" },
                            variant = SharedTextVariant.BodyStrong,
                        )
                        SharedText(
                            text = booking.serviceType.ifBlank { booking.categoryLabel.ifBlank { "Service" } },
                            variant = SharedTextVariant.Body,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        SharedText(
                            text = "$${booking.earnings}",
                            variant = SharedTextVariant.BodyStrong,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        SharedText(
                            text = "Completed",
                            variant = SharedTextVariant.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                SharedText(
                    text = "${booking.date} · ${booking.time}",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (booking.customerRating != null) {
                    Spacer(Modifier.height(4.dp))
                    SharedText(
                        text = "Your rating: ${booking.customerRating}",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}