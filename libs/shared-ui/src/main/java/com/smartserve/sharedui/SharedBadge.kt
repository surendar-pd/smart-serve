package com.smartserve.sharedui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

enum class SharedBadgeVariant {
    Neutral,
    Info,
    Success,
    Warning,
    Destructive,
}

private val SuccessContainer = Color(0xFFE8F5E9)
private val SuccessContent = Color(0xFF1B5E20)
private val WarningContainer = Color(0xFFFFF8E1)
private val WarningContent = Color(0xFFF57F17)
private val InfoContainer = Color(0xFFE3F2FD)
private val InfoContent = Color(0xFF1565C0)

@Composable
fun SharedBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: SharedBadgeVariant = SharedBadgeVariant.Neutral,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    outlined: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val colors = MaterialTheme.colorScheme

    val (container, content, border) = when (variant) {
        SharedBadgeVariant.Neutral -> Triple(colors.surfaceVariant, colors.onSurfaceVariant, colors.outlineVariant)
        SharedBadgeVariant.Info -> Triple(InfoContainer, InfoContent, InfoContent.copy(alpha = 0.6f))
        SharedBadgeVariant.Success -> Triple(SuccessContainer, SuccessContent, SuccessContent.copy(alpha = 0.6f))
        SharedBadgeVariant.Warning -> Triple(WarningContainer, WarningContent, WarningContent.copy(alpha = 0.6f))
        SharedBadgeVariant.Destructive -> Triple(colors.errorContainer, colors.onErrorContainer, colors.error.copy(alpha = 0.6f))
    }

    Surface(
        modifier = modifier.wrapContentSize(),
        color = if (outlined) colors.surface else container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        border = if (outlined) BorderStroke(1.dp, border) else null,
    ) {
        SharedText(
            text = text,
            variant = SharedTextVariant.Label,
            modifier = Modifier.padding(contentPadding),
        )
    }
}

