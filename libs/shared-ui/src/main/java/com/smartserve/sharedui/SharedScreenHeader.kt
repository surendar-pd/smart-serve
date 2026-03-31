package com.smartserve.sharedui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SharedScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SharedText(
            text = title,
            variant = SharedTextVariant.ScreenTitle,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        SharedText(
            text = subtitle,
            variant = SharedTextVariant.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
