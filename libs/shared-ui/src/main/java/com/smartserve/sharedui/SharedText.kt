package com.smartserve.sharedui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

enum class SharedTextVariant {
    Title,
    Subtitle,
    Body,
    BodyStrong,
    Caption,
    Label,
}

@Composable
fun SharedText(
    text: String,
    variant: SharedTextVariant = SharedTextVariant.Body,
    modifier: Modifier = Modifier,
) {
    val baseStyle: TextStyle = when (variant) {
        SharedTextVariant.Title -> MaterialTheme.typography.headlineSmall
        SharedTextVariant.Subtitle -> MaterialTheme.typography.titleMedium
        SharedTextVariant.Body -> MaterialTheme.typography.bodyMedium
        SharedTextVariant.BodyStrong -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        SharedTextVariant.Caption -> MaterialTheme.typography.bodySmall
        SharedTextVariant.Label -> MaterialTheme.typography.labelLarge
    }

    Text(
        text = text,
        style = baseStyle,
        modifier = modifier,
    )
}

