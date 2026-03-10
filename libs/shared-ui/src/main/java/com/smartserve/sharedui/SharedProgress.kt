package com.smartserve.sharedui

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Linear progress. Use [progress] in 0f..1f for determinate, or null for indeterminate.
 */
@Composable
fun SharedProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
