package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun BookingsScreen(
    modifier: Modifier = Modifier,
    viewModel: BookingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(s.bookings) { booking ->
                            BookingCard(booking = booking)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: CustomerBooking) {
    SharedCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SharedAvatar(name = booking.providerName, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SharedText(
                        text = booking.serviceName,
                        variant = SharedTextVariant.BodyStrong,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(status = booking.status)
                }
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

@Composable
private fun StatusChip(status: String) {
    val (label, containerColor, contentColor) = when (status) {
        "active"    -> Triple("Confirmed",  Color(0xFF2E7D32), Color.White)
        "completed" -> Triple("Completed",  Color(0xFF546E7A), Color.White)
        "declined"  -> Triple("Declined",   MaterialTheme.colorScheme.errorContainer,
                               MaterialTheme.colorScheme.onErrorContainer)
        else        -> Triple("Pending",    Color(0xFFF57F17), Color.White) // "pending"
    }
    AssistChip(
        onClick = {},
        label   = { SharedText(text = label, variant = SharedTextVariant.Caption, color = contentColor) },
        colors  = AssistChipDefaults.assistChipColors(containerColor = containerColor),
    )
}
