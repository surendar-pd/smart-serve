package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun BookingsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        CustomerTabHeader(
            title = "Bookings",
            subtitle = "Your upcoming activity",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, fill = true),
            contentAlignment = Alignment.Center,
        ) {
            SharedText(
                text = "No bookings yet",
                variant = SharedTextVariant.Body,
            )
        }
    }
}
