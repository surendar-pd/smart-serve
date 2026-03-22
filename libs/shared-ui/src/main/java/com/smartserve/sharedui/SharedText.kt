package com.smartserve.sharedui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

enum class SharedTextVariant {
    /** `headlineLarge` — hero / intro headlines */
    Headline,
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
    color: Color? = null,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit? = null,
) {
    val baseStyle: TextStyle = when (variant) {
        SharedTextVariant.Headline -> MaterialTheme.typography.headlineLarge
        SharedTextVariant.Title -> MaterialTheme.typography.headlineSmall
        SharedTextVariant.Subtitle -> MaterialTheme.typography.titleMedium
        SharedTextVariant.Body -> MaterialTheme.typography.bodyMedium
        SharedTextVariant.BodyStrong -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        SharedTextVariant.Caption -> MaterialTheme.typography.bodySmall
        SharedTextVariant.Label -> MaterialTheme.typography.labelLarge
    }

    var style = baseStyle
    if (fontWeight != null) style = style.copy(fontWeight = fontWeight)
    if (color != null) style = style.copy(color = color)
    if (lineHeight != null) style = style.copy(lineHeight = lineHeight)

    Text(
        text = text,
        style = style,
        textAlign = textAlign ?: TextAlign.Start,
        modifier = modifier,
    )
}

