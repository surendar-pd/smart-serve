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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onRemoveItem: (CartItem) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val isConfirming by viewModel.isConfirming.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        CustomerTabHeader(
            title = "My Cart (${cartItems.size} item${if (cartItems.size == 1) "" else "s"})",
            subtitle = "Review items before you confirm",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        )

        if (cartItems.isEmpty()) {
            SharedEmptyState(
                title = "Your cart is empty",
                description = "Browse services and add items to get started",
                icon = Icons.Filled.ShoppingCart,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cartItems) { item ->
                    SharedCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            SharedAvatar(name = item.providerName, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                SharedText(
                                    text = item.serviceName,
                                    variant = SharedTextVariant.BodyStrong,
                                )
                                SharedText(
                                    text = "${item.providerName} · ${item.price}",
                                    variant = SharedTextVariant.Body,
                                )
                                if (item.date.isNotBlank() || item.time.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dateTime = buildString {
                                        if (item.date.isNotBlank()) append(item.date)
                                        if (item.time.isNotBlank()) {
                                            if (isNotEmpty()) append(" · ")
                                            append(item.time)
                                        }
                                    }
                                    SharedText(
                                        text = dateTime,
                                        variant = SharedTextVariant.Caption,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            SharedIconButton(
                                onClick = { onRemoveItem(item) },
                                icon = Icons.Filled.Close,
                                contentDescription = "Remove",
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            SharedButton(
                text = if (isConfirming) "Confirming…" else "Confirm Bookings",
                onClick = { viewModel.confirm(cartItems, onConfirm) },
                enabled = !isConfirming,
                loading = isConfirming,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}
