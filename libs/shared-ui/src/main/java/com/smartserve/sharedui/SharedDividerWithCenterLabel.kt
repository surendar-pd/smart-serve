package com.smartserve.sharedui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Horizontal rule with centered label (e.g. "or" between SSO and password flows).
 */
@Composable
fun SharedDividerWithCenterLabel(
    label: String,
    modifier: Modifier = Modifier,
    textVariant: SharedTextVariant = SharedTextVariant.Caption,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SharedSeparator(modifier = Modifier.weight(1f))
        SharedText(
            text = label,
            variant = textVariant,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        SharedSeparator(modifier = Modifier.weight(1f))
    }
}
