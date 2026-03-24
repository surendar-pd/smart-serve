package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onRemoveItem: (CartItem) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var promoCode by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        SharedText(
            text = "My Cart (${cartItems.size} item${if (cartItems.size == 1) "" else "s"})",
            variant = SharedTextVariant.Title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )

        if (cartItems.isEmpty()) {
            SharedEmptyState(
                title = "Your cart is empty",
                description = "Browse services and add items to get started",
                icon = Icons.Filled.ShoppingCart,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(cartItems) { index, item ->
                    SharedListItem(
                        title = item.serviceName,
                        supportingText = "with ${item.providerName} · ${item.price}",
                        leadingAvatar = { SharedAvatar(name = item.providerName, size = 40.dp) },
                        trailing = {
                            SharedIconButton(
                                onClick = { onRemoveItem(item) },
                                icon = Icons.Filled.Close,
                                contentDescription = "Remove",
                            )
                        },
                        showDivider = index > 0,
                    )
                }
            }

            HorizontalDivider()

            SharedTextField(
                value = promoCode,
                onValueChange = { promoCode = it },
                label = "Promo code",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            SharedButton(
                text = "Proceed to Checkout",
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            )
        }
    }
}
