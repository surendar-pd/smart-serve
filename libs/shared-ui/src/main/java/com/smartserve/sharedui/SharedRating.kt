package com.smartserve.sharedui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Read-only star rating display (e.g. 4.5 shows 4 full stars + half).
 * [rating] is in 0..[maxStars] (e.g. 0..5).
 */
@Composable
fun SharedRating(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
) {
    val color = MaterialTheme.colorScheme.primary
    Row(modifier = modifier) {
        repeat(maxStars) { index ->
            val value = (rating - index).coerceIn(0f, 1f)
            Icon(
                imageVector = if (value >= 1f) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (value >= 1f) color else color.copy(alpha = 0.4f),
            )
        }
    }
}
