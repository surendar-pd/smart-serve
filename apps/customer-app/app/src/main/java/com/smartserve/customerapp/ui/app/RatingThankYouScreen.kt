package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun RatingThankYouScreen(
    serviceName: String,
    providerName: String,
    rating: Float,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                SharedText(text = "Thank You", variant = SharedTextVariant.Title)
                SharedText(
                    text = "You rated $providerName $rating stars for $serviceName.",
                    variant = SharedTextVariant.Body,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SharedText(
                    text = "We appreciate you choosing SmartServe and sharing your feedback. 🌟",
                    variant = SharedTextVariant.Caption,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SharedButton(
                    text = "Back to Bookings",
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    variant = SharedButtonVariant.Secondary,
                )
            }
        }
    }
}
