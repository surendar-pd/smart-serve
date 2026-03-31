package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.Star
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedBottomSheet
import com.smartserve.sharedui.SharedRating
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import java.util.concurrent.TimeUnit

@Composable
fun BookingDetailScreen(
    booking: CustomerBooking,
    onBack: () -> Unit,
    onOpenChat: (bookingId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingDetailViewModel = hiltViewModel(),
) {
    val isDeleting by viewModel.isDeleting.collectAsState()
    val isRating by viewModel.isRating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val now = System.currentTimeMillis()
    val cutoffMillis = booking.scheduledAtMillis - TimeUnit.HOURS.toMillis(2)
    val isPending = booking.status.lowercase().trim() == "pending"
    val canDelete = isPending && booking.scheduledAtMillis > 0L && now < cutoffMillis
    val isCompleted = booking.status.lowercase().trim() == "completed"
    val hasRatedProvider = booking.providerRating != null

    var showRateSheet by remember { mutableStateOf(false) }
    var rating by remember { mutableFloatStateOf(booking.providerRating ?: 0f) }

    SharedBottomSheet(
        isOpen = showRateSheet,
        onOpenChange = { showRateSheet = it },
        skipPartiallyExpanded = true,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SharedText(text = "Rate provider", variant = SharedTextVariant.Subtitle)
                SharedText(
                    text = "Share your experience. This helps improve service quality.",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SharedText(text = "Rating", variant = SharedTextVariant.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Star $star",
                            tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(enabled = !isRating) { rating = star.toFloat() },
                        )
                    }
                }

                SharedButton(
                    text = if (isRating) "Submitting…" else "Submit rating",
                    onClick = {
                        viewModel.clearError()
                        viewModel.rateProvider(booking.id, rating) { showRateSheet = false }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = rating > 0f && !isRating,
                    loading = isRating,
                )

                SharedButton(
                    text = "Cancel",
                    onClick = { showRateSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    variant = SharedButtonVariant.Ghost,
                    enabled = !isRating,
                )
            }
        },
        content = { _ ->
            Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = "Booking details",
            subtitle = "View and manage your booking",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            DetailRow(label = "Type", value = booking.typeLabel)
            DetailRow(label = "Service", value = booking.serviceName)
            DetailRow(label = "Provider", value = booking.providerName)
            DetailRow(
                label = "When",
                value = listOf(booking.date, booking.time).filter { it.isNotBlank() }.joinToString(" · "),
            )
            DetailRow(label = "Price", value = booking.price)
            DetailRow(label = "Status", value = booking.status)
            if (booking.address.isNotBlank()) {
                DetailRow(label = "Address", value = booking.address)
            }

            Spacer(Modifier.height(24.dp))

            if (!isCompleted) {
                SharedButton(
                    text = "Open Chat",
                    onClick = { onOpenChat(booking.id) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = SharedButtonVariant.Outline,
                    leadingIcon = Icons.Outlined.ChatBubbleOutline,
                )
            }

            if (isCompleted) {
                if (hasRatedProvider) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SharedText(
                            text = "Your rating",
                            variant = SharedTextVariant.Subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SharedRating(rating = booking.providerRating ?: 0f, starSize = 24.dp)
                    }
                } else {
                    SharedButton(
                        text = "Rate provider",
                        onClick = { showRateSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        variant = SharedButtonVariant.Secondary,
                        enabled = !isRating,
                    )
                }
            }

            if (!isCompleted) {
                SharedButton(
                    text = if (isDeleting) "Deleting…" else "Delete booking",
                    onClick = {
                        viewModel.clearError()
                        viewModel.deleteBooking(booking.id) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canDelete && !isDeleting,
                    loading = isDeleting,
                    variant = SharedButtonVariant.Destructive,
                )

                SharedText(
                    text = "You can delete this booking up to 2 hours before the booking time.",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val err = errorMessage
            if (err != null) {
                Spacer(Modifier.height(8.dp))
                SharedText(
                    text = err,
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SharedText(
            text = label,
            variant = SharedTextVariant.Subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        SharedText(text = value, variant = SharedTextVariant.Body)
    }
}

