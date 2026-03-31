package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

private fun CustomerBooking.normalizedStatus(): String =
    status.lowercase().trim().ifBlank { "pending" }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BookingsScreen(
    modifier: Modifier = Modifier,
    viewModel: BookingsViewModel = hiltViewModel(),
    onSelectBooking: (CustomerBooking) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    // If the NavHost keeps this composable instance alive, we still need to re-fetch
    // when the user returns to the bookings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val refreshing = state is BookingsUiState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { if (!refreshing) viewModel.load() },
    )

    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomerTabHeader(
                title    = "Bookings",
                subtitle = "Your upcoming activity",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )

            when (val s = state) {
                is BookingsUiState.Loading -> SharedLoading(modifier = Modifier.fillMaxSize())

                is BookingsUiState.Error -> SharedErrorState(
                    title       = "Couldn't load bookings",
                    description = s.message,
                    modifier    = Modifier.fillMaxSize(),
                )

                is BookingsUiState.Success -> {
                    if (s.bookings.isEmpty()) {
                        SharedEmptyState(
                            title       = "No bookings yet",
                            description = "Confirm a cart to create your first booking",
                            icon        = Icons.Filled.DateRange,
                            modifier    = Modifier.fillMaxSize(),
                        )
                    } else {
                        val pending = s.bookings.filter { it.normalizedStatus() == "pending" }
                        val active = s.bookings.filter { it.normalizedStatus() == "active" }
                        val completed = s.bookings.filter { it.normalizedStatus() == "completed" }
                        val declined = s.bookings.filter { it.normalizedStatus() == "declined" }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            section(title = "Pending", bookings = pending, onSelectBooking = onSelectBooking)
                            section(title = "Active", bookings = active, onSelectBooking = onSelectBooking)
                            section(title = "Completed", bookings = completed, onSelectBooking = onSelectBooking)
                            section(title = "Declined", bookings = declined, onSelectBooking = onSelectBooking)
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    bookings: List<CustomerBooking>,
    onSelectBooking: (CustomerBooking) -> Unit,
) {
    if (bookings.isEmpty()) return

    item(key = "section_$title") {
        Spacer(Modifier.height(6.dp))
        SharedText(
            text = title,
            variant = SharedTextVariant.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
    }

    items(bookings, key = { it.id }) { booking ->
        BookingCard(booking = booking, onClick = { onSelectBooking(booking) })
    }
}

@Composable
private fun BookingCard(booking: CustomerBooking, onClick: () -> Unit) {
    SharedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SharedAvatar(name = booking.providerName, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SharedText(
                    text = booking.serviceName,
                    variant = SharedTextVariant.BodyStrong,
                )
                SharedText(
                    text = booking.providerName,
                    variant = SharedTextVariant.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SharedText(
                    text = booking.price,
                    variant = SharedTextVariant.Body,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (booking.date.isNotBlank() || booking.time.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    val dateTime = buildString {
                        if (booking.date.isNotBlank()) append(booking.date)
                        if (booking.time.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(booking.time)
                        }
                    }
                    SharedText(
                        text = dateTime,
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
