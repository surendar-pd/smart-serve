package com.smartserve.customerapp.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartserve.sharedui.SharedEmptyState

@Composable
fun CartScreen(modifier: Modifier = Modifier) {
    SharedEmptyState(
        title = "Cart",
        description = "Your cart is empty",
        icon = Icons.Filled.ShoppingCart,
        modifier = modifier,
    )
}
